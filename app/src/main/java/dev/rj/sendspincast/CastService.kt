package dev.rj.sendspincast

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

/** Captures permitted playback audio and broadcasts AAC-LC / ADTS over HTTP. */
class CastService : Service() {
    private val running = AtomicBoolean(false)
    private var worker: Thread? = null
    @Volatile private var audioRecord: AudioRecord? = null
    @Volatile private var projection: MediaProjection? = null
    private var broadcaster: AacStreamServer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!running.compareAndSet(false, true)) return START_NOT_STICKY
        lastError = null
        createChannel()
        val notification = Notification.Builder(this, NOTIFICATION_CHANNEL)
            .setContentTitle("Sendspin Cast")
            .setContentText("Broadcasting phone audio to Music Assistant")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else startForeground(NOTIFICATION_ID, notification)
        isRunning = true

        val code = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        @Suppress("DEPRECATION")
        val captureData = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        if (code == 0 || captureData == null) {
            lastError = "Screen capture permission was not supplied"
            stopSelf()
            return START_NOT_STICKY
        }
        worker = Thread({ runCapture(code, captureData) }, "cast-capture").apply { start() }
        return START_NOT_STICKY
    }

    private fun runCapture(code: Int, captureData: Intent) {
        var encoder: MediaCodec? = null
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SendspinCast:AudioStream").apply { acquire() }
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val captureProjection = manager.getMediaProjection(code, captureData)
            projection = captureProjection
            captureProjection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() { stopSelf() }
            }, Handler(Looper.getMainLooper()))

            // Android only permits capture from apps that allow playback capture.
            val captureConfig = AudioPlaybackCaptureConfiguration.Builder(captureProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()
            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                .build()
            val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT)
            check(minBuffer > 0) { "Audio input is unsupported" }
            val record = AudioRecord.Builder()
                .setAudioFormat(format)
                .setBufferSizeInBytes(max(minBuffer * 2, 32_768))
                .setAudioPlaybackCaptureConfig(captureConfig)
                .build()
            check(record.state == AudioRecord.STATE_INITIALIZED) { "AudioRecord could not initialize" }
            audioRecord = record

            val aac = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, CHANNELS).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, 192_000)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16_384)
            }
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
                configure(aac, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                start()
            }
            val server = AacStreamServer(CastConfig.secret(this)).apply { start() }
            broadcaster = server
            record.startRecording()
            val pcm = ByteArray(4096)
            val info = MediaCodec.BufferInfo()
            var capturedFrames = 0L
            while (running.get() && !Thread.currentThread().isInterrupted) {
                val count = record.read(pcm, 0, pcm.size, AudioRecord.READ_BLOCKING)
                if (count < 0) error("AudioRecord read failed: $count")
                if (count == 0) continue
                val inputIdx = encoder.dequeueInputBuffer(10_000)
                if (inputIdx >= 0) {
                    val input = encoder.getInputBuffer(inputIdx) ?: error("Encoder lost input buffer")
                    input.clear()
                    val accepted = minOf(count, input.remaining())
                    input.put(pcm, 0, accepted)
                    encoder.queueInputBuffer(inputIdx, 0, accepted, capturedFrames * 1_000_000L / SAMPLE_RATE, 0)
                    capturedFrames += accepted / (CHANNELS * 2)
                }
                while (true) {
                    val outputIdx = encoder.dequeueOutputBuffer(info, 0)
                    if (outputIdx < 0) break
                    try {
                        if (info.size > 0 && (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                            val output = encoder.getOutputBuffer(outputIdx) ?: continue
                            output.position(info.offset)
                            output.limit(info.offset + info.size)
                            val packet = ByteArray(7 + info.size)
                            makeAdtsHeader(packet, info.size)
                            output.get(packet, 7, info.size)
                            server.publish(packet)
                        }
                    } finally { encoder.releaseOutputBuffer(outputIdx, false) }
                }
            }
        } catch (e: Exception) {
            if (running.get()) lastError = "${e.javaClass.simpleName}: ${e.message ?: "unknown error"}"
        } finally {
            running.set(false)
            try { audioRecord?.stop() } catch (_: Exception) {}
            try { audioRecord?.release() } catch (_: Exception) {}
            audioRecord = null
            try { encoder?.stop() } catch (_: Exception) {}
            try { encoder?.release() } catch (_: Exception) {}
            broadcaster?.stop(); broadcaster = null
            stopSelf()
        }
    }

    override fun onDestroy() {
        running.set(false)
        isRunning = false
        try { audioRecord?.stop() } catch (_: Exception) {}
        try { projection?.stop() } catch (_: Exception) {}
        broadcaster?.stop()
        try { wakeLock?.takeIf { it.isHeld }?.release() } catch (_: Exception) {}
        super.onDestroy()
    }

    private fun createChannel() {
        val notifications = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notifications.createNotificationChannel(NotificationChannel(NOTIFICATION_CHANNEL, "Audio broadcast", NotificationManager.IMPORTANCE_LOW))
    }

    private fun makeAdtsHeader(packet: ByteArray, aacLength: Int) {
        // AAC LC, MPEG-4, 48 kHz (frequency index 3), stereo (channel config 2).
        val length = aacLength + 7
        packet[0] = 0xff.toByte()
        packet[1] = 0xf1.toByte()
        packet[2] = 0x4c.toByte()
        packet[3] = (0x80 or (length shr 11)).toByte()
        packet[4] = ((length shr 3) and 0xff).toByte()
        packet[5] = (((length and 7) shl 5) or 0x1f).toByte()
        packet[6] = 0xfc.toByte()
    }

    companion object {
        const val EXTRA_RESULT_CODE = "projection_result_code"
        const val EXTRA_RESULT_DATA = "projection_data"
        private const val SAMPLE_RATE = 48_000
        private const val CHANNELS = 2
        private const val NOTIFICATION_CHANNEL = "sendspin_cast_channel"
        private const val NOTIFICATION_ID = 1319
        @Volatile var isRunning: Boolean = false
        @Volatile var lastError: String? = null
    }
}
