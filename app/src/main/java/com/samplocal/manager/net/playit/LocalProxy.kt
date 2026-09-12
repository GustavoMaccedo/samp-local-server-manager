package com.samplocal.manager.net.playit

import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class HttpConnectProxy {
    private var server: ServerSocket? = null
    private var acceptThread: Thread? = null
    private val running = AtomicBoolean(false)

    @Synchronized
    fun start(): Int {
        server?.takeIf { it.isBound && !it.isClosed }?.let { return it.localPort }
        val s = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
        server = s
        running.set(true)
        acceptThread = Thread({
            while (running.get()) {
                try {
                    val client = s.accept()
                    Thread({ handle(client) }, "proxy-conn").apply {
                        isDaemon = true
                        start()
                    }
                } catch (_: Exception) {
                    if (!running.get()) break
                }
            }
        }, "proxy-accept").apply { isDaemon = true; start() }
        return s.localPort
    }

    @Synchronized
    fun stop() {
        running.set(false)
        try { server?.close() } catch (_: Exception) { }
        server = null
    }

    fun isRunning(): Boolean = running.get() && server?.isClosed == false

    fun env(port: Int): Map<String, String> {
        val v = "http://127.0.0.1:$port"
        return mapOf(
            "HTTPS_PROXY" to v,
            "HTTP_PROXY" to v,
            "https_proxy" to v,
            "http_proxy" to v
        )
    }

    private fun handle(client: Socket) {
        try {
            client.soTimeout = 15000
            val req = readHead(client.getInputStream())
            val first = req.lineSequence().firstOrNull().orEmpty()

            val parts = first.split(" ")
            if (parts.size < 2 || !parts[0].equals("CONNECT", ignoreCase = true)) {
                client.getOutputStream().write("HTTP/1.1 400 Bad Request\r\n\r\n".toByteArray())
                return
            }
            val hostPort = parts[1]
            val host = hostPort.substringBeforeLast(":")
            val port = hostPort.substringAfterLast(":").toIntOrNull() ?: 443

            val addr = InetAddress.getByName(host)
            val upstream = Socket()
            try {
                upstream.connect(java.net.InetSocketAddress(addr, port), 15000)
                upstream.soTimeout = 0
                client.getOutputStream().write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())
                client.getOutputStream().flush()
                val t1 = relay(client.getInputStream(), upstream.getOutputStream())
                val t2 = relay(upstream.getInputStream(), client.getOutputStream())
                t1.join()
                t2.join()
            } finally {
                try { upstream.close() } catch (_: Exception) { }
            }
        } catch (_: Exception) {
        } finally {
            try { client.close() } catch (_: Exception) { }
        }
    }

    private fun relay(ins: InputStream, out: OutputStream): Thread {
        return Thread({
            try {
                val buf = ByteArray(16384)
                while (true) {
                    val r = ins.read(buf)
                    if (r <= 0) break
                    out.write(buf, 0, r)
                    out.flush()
                }
            } catch (_: Exception) { }
            try { out.flush() } catch (_: Exception) { }
        }, "proxy-relay").apply { isDaemon = true; start() }
    }

    private fun readHead(ins: InputStream): String {
        val sb = StringBuilder()
        val tail = ArrayDeque<Int>()
        while (sb.length < 8192) {
            val b = try { ins.read() } catch (_: Exception) { -1 }
            if (b < 0) break
            sb.append(b.toChar())
            tail.addLast(b)
            while (tail.size > 4) tail.removeFirst()
            if (tail == listOf('\r'.code, '\n'.code, '\r'.code, '\n'.code)) break
        }
        return sb.toString()
    }
}
