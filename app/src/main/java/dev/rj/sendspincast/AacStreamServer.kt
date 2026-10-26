package dev.rj.sendspincast

import android.net.Uri
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/** A tiny LAN-only HTTP AAC live-stream server. No external libraries. */
class AacStreamServer(private val secret: String) {
    private val started = AtomicBoolean(false)
    private val clients = ConcurrentHashMap.newKeySet<Client>()
    private var server: ServerSocket? = null
    private var acceptThread: Thread? = null

    fun start() {
        if (!started.compareAndSet(false, true)) return
        try {
            server = ServerSocket(CastConfig.PORT).apply { reuseAddress = true }
        } catch (e: Exception) {
            started.set(false); throw e
        }
        acceptThread = Thread({
            while (started.get()) {
                try {
                    val socket = server?.accept() ?: break
                    Thread({ handle(socket) }, "aac-http-client").start()
                } catch (_: SocketException) { break }
                catch (_: Exception) { if (!started.get()) break }
            }
        }, "aac-http-accept").apply { isDaemon = true; start() }
    }

    fun publish(frame: ByteArray) {
        if (!started.get()) return
        for (client in clients) {
            // Never block the audio encoder on a slow network listener.
            if (!client.queue.offer(frame)) client.close()
        }
    }

    fun stop() {
        started.set(false)
        try { server?.close() } catch (_: Exception) {}
        clients.toList().forEach { it.close() }
    }

    private fun handle(socket: Socket) {
        socket.soTimeout = 10_000
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.US_ASCII))
            val request = reader.readLine() ?: return
            val parts = request.split(' ')
            if (parts.size < 2) return
            val method = parts[0]
            val path = Uri.parse(parts[1])
            while (true) {
                val header = reader.readLine() ?: break
                if (header.isEmpty()) break
            }
            val out = BufferedOutputStream(socket.getOutputStream(), 32_768)
            if (path.path == "/health") {
                reply(out, "200 OK", "text/plain", "Sendspin Cast is running\n")
                return
            }
            if (path.path != "/live.aac") {
                reply(out, "404 Not Found", "text/plain", "Not found\n")
                return
            }
            if (path.getQueryParameter("key") != secret) {
                reply(out, "403 Forbidden", "text/plain", "Invalid stream key\n")
                return
            }
            if (method != "GET" && method != "HEAD") {
                reply(out, "405 Method Not Allowed", "text/plain", "Use GET or HEAD\n")
                return
            }
            out.write((
                "HTTP/1.1 200 OK\r\n" +
                "Content-Type: audio/aac\r\n" +
                "Cache-Control: no-store, no-cache, must-revalidate\r\n" +
                "Pragma: no-cache\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
            ).toByteArray(Charsets.US_ASCII))
            out.flush()
            if (method == "HEAD") return
            socket.soTimeout = 0
            val client = Client(socket)
            clients.add(client)
            try {
                while (started.get() && client.active.get()) {
                    val frame = client.queue.take()
                    out.write(frame)
                    out.flush()
                }
            } catch (_: Exception) {
                // Client stopped listening.
            } finally { client.close() }
        } catch (_: Exception) {
            // Probe clients may disconnect immediately.
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun reply(out: BufferedOutputStream, status: String, type: String, body: String) {
        val content = body.toByteArray(Charsets.UTF_8)
        out.write("HTTP/1.1 $status\r\nContent-Type: $type\r\nContent-Length: ${content.size}\r\nConnection: close\r\n\r\n".toByteArray(Charsets.US_ASCII))
        out.write(content)
        out.flush()
    }

    private inner class Client(val socket: Socket) {
        val queue = ArrayBlockingQueue<ByteArray>(96)
        val active = AtomicBoolean(true)
        fun close() {
            if (!active.compareAndSet(true, false)) return
            clients.remove(this)
            try { socket.close() } catch (_: Exception) {}
        }
    }
}
