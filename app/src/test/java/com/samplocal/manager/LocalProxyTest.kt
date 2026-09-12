package com.samplocal.manager

import com.samplocal.manager.net.playit.HttpConnectProxy
import com.samplocal.manager.net.playit.PlayitAgent
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.net.DatagramSocket
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.ServerSocket

class LocalProxyTest {

    @Test
    fun forwardsConnectTunnel() {

        val echo = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
        val echoPort = echo.localPort
        val echoJob = Thread {
            try {
                while (true) {
                    val c = echo.accept()
                    Thread {
                        try {
                            val buf = ByteArray(4096)
                            val ins = c.getInputStream()
                            val out = c.getOutputStream()
                            while (true) {
                                val r = ins.read(buf)
                                if (r <= 0) break
                                out.write(buf, 0, r)
                                out.flush()
                            }
                        } catch (_: Exception) { } finally {
                            try { c.close() } catch (_: Exception) { }
                        }
                    }.apply { isDaemon = true; start() }
                }
            } catch (_: Exception) { }
        }.apply { isDaemon = true; start() }
        val proxy = HttpConnectProxy()
        try {
            val port = proxy.start()
            assertTrue(port > 0)
            assertTrue(proxy.isRunning())

            java.net.Socket("127.0.0.1", port).use { s ->
                s.soTimeout = 8000
                val out = s.getOutputStream()
                out.write("CONNECT 127.0.0.1:$echoPort HTTP/1.1\r\nhost: x\r\n\r\n".toByteArray())
                out.flush()
                val ins = s.getInputStream()
                val head = ByteArray(39)
                var n = 0
                while (n < head.size) {
                    val r = ins.read(head, n, head.size - n)
                    if (r <= 0) break
                    n += r
                }
                val status = String(head, 0, n)
                assertTrue(status.startsWith("HTTP/1.1 200"))

                out.write("abc123".toByteArray())
                out.flush()
                val back = ByteArray(6)
                var m = 0
                while (m < 6) {
                    val r = ins.read(back, m, 6 - m)
                    if (r <= 0) break
                    m += r
                }
                assertArrayEquals("abc123".toByteArray(), back)
            }
        } finally {
            proxy.stop()
            assertFalse(proxy.isRunning())
            echo.close()
            echoJob.interrupt()
        }
    }

    @Test
    fun badRequestRejected() {
        val proxy = HttpConnectProxy()
        try {
            val port = proxy.start()
            java.net.Socket("127.0.0.1", port).use { s ->
                s.soTimeout = 5000
                s.getOutputStream().write("GET / HTTP/1.0\r\n\r\n".toByteArray())
                s.getOutputStream().flush()
                val buf = ByteArray(64)
                val r = s.getInputStream().read(buf)
                assertTrue(r > 0)
                assertTrue(String(buf, 0, r).contains("400"))
            }
        } finally {
            proxy.stop()
        }
    }

    @Test
    fun agentPassesProxyEnv() {
        val dir = createTempDir("proxy-env")
        try {
            val starter = FakeStarter(alive = true)
            val agent = TestAgent(dir, starter)
            assertTrue(agent.start("SECRET", proxyPort = 18080).isSuccess)
            val env = starter.lastEnv!!
            assertEquals("http://127.0.0.1:18080", env["HTTPS_PROXY"])
            assertEquals("http://127.0.0.1:18080", env["http_proxy"])
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun agentWithoutProxyHasEmptyEnv() {
        val dir = createTempDir("proxy-env2")
        try {
            val starter = FakeStarter(alive = true)
            val agent = TestAgent(dir, starter)
            assertTrue(agent.startUnclaimed().isSuccess)
            assertTrue(starter.lastEnv!!.isEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }
}
