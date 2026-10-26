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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(12, 12, 18)
        window.navigationBarColor = android.graphics.Color.rgb(12, 12, 18)
        setContent {
            CastDashboard(
                onToggle = { toggleBroadcast() },
                onCopy = { copyStreamUrl() },
                onShare = { shareStreamUrl() },
            )
        }
    }

    private fun toggleBroadcast() {
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

    private fun copyStreamUrl() {
        val manager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText("Sendspin Cast stream", CastConfig.url(this)))
        android.widget.Toast.makeText(this, "Private stream link copied", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun shareStreamUrl() {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, CastConfig.url(this@MainActivity))
        }
        startActivity(Intent.createChooser(send, "Share private stream URL"))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            requestCapture()
        }
    }

    @Deprecated("Legacy activity result for MediaProjection")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CAPTURE || resultCode != Activity.RESULT_OK || data == null) return
        val service = Intent(this, CastService::class.java).apply {
            putExtra(CastService.EXTRA_RESULT_CODE, resultCode)
            putExtra(CastService.EXTRA_RESULT_DATA, data)
        }
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(service) else startService(service)
    }

    companion object {
        private const val REQUEST_RECORD = 91
        private const val REQUEST_CAPTURE = 92
    }
}
