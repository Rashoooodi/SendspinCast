package dev.rj.sendspincast

import android.content.Context
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.SecureRandom

object CastConfig {
    const val PORT = 8765
    fun secret(context: Context): String {
        val prefs = context.getSharedPreferences("cast_config", Context.MODE_PRIVATE)
        val stored = prefs.getString("stream_key", null)
        if (stored != null) return stored
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        val newKey = bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }
        prefs.edit().putString("stream_key", newKey).apply()
        return newKey
    }
    fun localIp(): String? {
        val all = NetworkInterface.getNetworkInterfaces()?.toList() ?: return null
        val candidates = all.filter { it.isUp && !it.isLoopback }
        val preferred = candidates.sortedBy { iface ->
            when { iface.name.startsWith("wlan") -> 0; iface.name.startsWith("eth") -> 1; else -> 2 }
        }
        return preferred.flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress && it.isSiteLocalAddress }?.hostAddress
    }
    fun url(context: Context): String {
        val ip = localIp() ?: "PHONE_IP"
        return "http://$ip:$PORT/live.aac?key=${secret(context)}"
    }
}
