package dev.rj.sendspincast

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var url: TextView
    private lateinit var button: Button
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshTask = object : Runnable {
        override fun run() { refresh(); refreshHandler.postDelayed(this, 1500) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dp = resources.displayMetrics.density
        val pad = (22 * dp).toInt()
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, pad)
            setBackgroundColor(0xff11111b.toInt())
        }
        fun heading(label: String, size: Float, color: Int): TextView = TextView(this).apply {
            text = label; textSize = size; setTextColor(color)
            setPadding(0, 0, 0, (12 * dp).toInt())
        }
        container.addView(heading("SENDSPIN CAST", 28f, 0xffd2bcff.toInt()))
        container.addView(heading("Phone audio → Music Assistant → speaker group", 15f, 0xffc9c6d4.toInt()))
        status = heading("Not broadcasting", 17f, 0xffffffff.toInt())
        container.addView(status)
        button = Button(this).apply { text = "Start audio cast"; setOnClickListener { toggle() } }
        container.addView(button, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        container.addView(heading("Your private live stream URL", 17f, 0xffd2bcff.toInt()))
        url = heading("", 13f, 0xffffffff.toInt())
        url.setTextIsSelectable(true)
        container.addView(url)
        container.addView(Button(this).apply {
            text = "Copy stream URL"
            setOnClickListener {
                val manager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                manager.setPrimaryClip(ClipData.newPlainText("Sendspin Cast URL", CastConfig.url(this@MainActivity)))
                android.widget.Toast.makeText(this@MainActivity, "URL copied", android.widget.Toast.LENGTH_SHORT).show()
            }
        })
        container.addView(heading("1. Start the cast and allow audio capture.\n2. In Music Assistant → Radio → Add Item from URL, paste the link.\n3. Play that station on your Sendspin group.\n\nKeep your phone and Music Assistant reachable on the same network. Protected/DRM apps may be silent.\n\nThe link includes a private key. Anyone with that URL on your network can listen while you're broadcasting.", 15f, 0xffd7d4de.toInt()))
        setContentView(ScrollView(this).apply { addView(container) })
    }

    override fun onResume() { super.onResume(); refreshHandler.post(refreshTask) }
    override fun onPause() { refreshHandler.removeCallbacks(refreshTask); super.onPause() }
    private fun refresh() {
        url.text = CastConfig.url(this)
        button.text = if (CastService.isRunning) "Stop audio cast" else "Start audio cast"
        status.text = when {
            CastService.isRunning -> "● Broadcasting AAC at 48 kHz stereo"
            CastService.lastError != null -> "Stopped: ${CastService.lastError}"
            else -> "Not broadcasting"
        }
    }
    private fun toggle() {
        if (CastService.isRunning) {
            stopService(Intent(this, CastService::class.java))
        } else if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD)
        } else {
            requestCapture()
        }
    }
    private fun requestCapture() {
        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mgr.createScreenCaptureIntent(), REQUEST_CAPTURE)
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) requestCapture()
    }
    @Deprecated("Activity result callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CAPTURE || resultCode != RESULT_OK || data == null) return
        val service = Intent(this, CastService::class.java).apply {
            putExtra(CastService.EXTRA_RESULT_CODE, resultCode)
            putExtra(CastService.EXTRA_RESULT_DATA, data)
        }
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(service) else startService(service)
    }
    companion object { private const val REQUEST_RECORD = 91; private const val REQUEST_CAPTURE = 92 }
}
