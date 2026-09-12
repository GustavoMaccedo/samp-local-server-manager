package com.samplocal.manager

import com.samplocal.manager.net.turn.AllocateOutcome
import com.samplocal.manager.net.turn.TurnClient
import com.samplocal.manager.net.turn.TurnConfig
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import org.junit.Assert.*
import org.junit.Test
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TurnCodecTest {

    private val txid = ByteArray(12) { (it + 1).toByte() }

    @Test
    fun allocateRequestLayout() {
        val req = TurnClient.buildAllocate(txid)
        val b = ByteBuffer.wrap(req).order(ByteOrder.BIG_ENDIAN)
        assertEquals(0x0003, b.short.toInt() and 0xFFFF)
        assertEquals(req.size - 20, b.short.toInt() and 0xFFFF)

        val at = ByteBuffer.wrap(req, 20, 2).order(ByteOrder.BIG_ENDIAN).short.toInt() and 0xFFFF
        assertEquals(0x0019, at)
        assertEquals(17, req[24].toInt() and 0xFF)
    }

    @Test
    fun messageIntegrityVerifies() {
        val key = TurnClient.md5key("user", "realm", "pass")
        assertEquals(16, key.size)
        val msg = "abc".toByteArray()
        val mac1 = TurnClient.hmacSha1(key, msg)
        val mac2 = TurnClient.hmacSha1(key, msg)
        assertArrayEquals(mac1, mac2)
        assertEquals(20, mac1.size)
        assertFalse(mac1.contentEquals(TurnClient.hmacSha1(key, "abd".toByteArray())))
    }

    private fun error401(realm: String, nonce: ByteArray): ByteArray {
        val err = byteArrayOf(0, 0, 4, 1) + "Unauthorized".toByteArray()
        fun attr(t: Int, v: ByteArray): ByteArray {
            val pad = (-v.size % 4 + 4) % 4
            val b = ByteBuffer.allocate(4 + v.size + pad).order(ByteOrder.BIG_ENDIAN)
            b.putShort(t.toShort()); b.putShort(v.size.toShort()); b.put(v)
            repeat(pad) { b.put(0) }
            return b.array()
        }
        val attrs = attr(0x0009, err) + attr(0x0014, realm.toByteArray()) + attr(0x0015, nonce)
        val h = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN)
        h.putShort(0x0113); h.putShort(attrs.size.toShort()); h.putInt(0x2112A442); h.put(txid)
        return h.array() + attrs
    }

    @Test
    fun parses401Challenge() {
        val out = TurnClient.parseAllocateResponse(error401("t.realm", byteArrayOf(1, 2, 3)), txid)
        assertTrue(out is AllocateOutcome.NeedAuth)
        out as AllocateOutcome.NeedAuth
        assertEquals("t.realm", out.realm)
        assertArrayEquals(byteArrayOf(1, 2, 3), out.nonce)
    }

    @Test
    fun parsesRelayedAddress() {

        val mask = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(0x2112A442).array()
        val ip = byteArrayOf(203.toByte(), 0, 113, 9)
        val v = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
        v.put(0); v.put(0x01)
        v.putShort((60000 xor (0x2112A442 ushr 16)).toShort())
        v.put(ByteArray(4) { i -> (ip[i].toInt() xor (mask[i].toInt() and 0xFF)).toByte() })
        val h = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN)
        h.putShort(0x0016); h.putShort(8)
        val attrs = h.array() + v.array()
        val head = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN)
        head.putShort(0x0103); head.putShort(attrs.size.toShort()); head.putInt(0x2112A442); head.put(txid)
        val out = TurnClient.parseAllocateResponse(head.array() + attrs, txid)
        assertTrue(out is AllocateOutcome.Allocated)
        out as AllocateOutcome.Allocated
        assertEquals("203.0.113.9", out.allocation.relayedIp.hostAddress)
        assertEquals(60000, out.allocation.relayedPort)
    }

    @Test
    fun allocateAgainstFakeTurn() = runTest {
        val server = DatagramSocket(0)
        val serverPort = server.localPort
        val job = launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                repeat(2) {
                    val buf = ByteArray(2048)
                    val pkt = DatagramPacket(buf, buf.size)
                    server.soTimeout = 8000
                    server.receive(pkt)
                    val req = pkt.data.copyOf(pkt.length)
                    val rTx = req.copyOfRange(8, 20)
                    val hasUser = req.size > 20 && containsAttr(req, 0x0006)
                    val resp = if (!hasUser) {
                        error401For(rTx, "fake", byteArrayOf(7, 7))
                    } else {
                        successFor(rTx)
                    }
                    server.send(DatagramPacket(resp, resp.size, pkt.address, pkt.port))
                }
            } catch (_: Exception) { }
        }
        try {
            val client = TurnClient(
                transport = object : com.samplocal.manager.net.stun.UdpTransport {
                    override fun exchange(host: String, port: Int, req: ByteArray, timeoutMs: Int): ByteArray {
                        java.net.DatagramSocket().use { s ->
                            s.soTimeout = timeoutMs
                            val a = InetAddress.getByName(host)
                            s.send(DatagramPacket(req, req.size, a, port))
                            val b = ByteArray(2048)
                            val p = DatagramPacket(b, b.size)
                            s.receive(p)
                            return p.data.copyOf(p.length)
                        }
                    }
                }
            )
            val alloc = client.allocate(TurnConfig("127.0.0.1", serverPort, "u", "p", "fake"), 4000)
                .getOrThrow()
            assertEquals("203.0.113.9", alloc.relayedIp.hostAddress)
            assertEquals(60000, alloc.relayedPort)
        } finally {
            job.cancel()
            server.close()
        }
    }

    @Test
    fun unavailableRelayFails() = runTest {
        val client = TurnClient(
            transport = object : com.samplocal.manager.net.stun.UdpTransport {
                override fun exchange(host: String, port: Int, req: ByteArray, timeoutMs: Int): ByteArray {
                    throw java.net.SocketTimeoutException("timed out")
                }
            }
        )

        val r = client.allocate(TurnConfig("127.0.0.1", 9, "u", "p"), 200)
        assertTrue(r.isFailure)
    }

    private fun containsAttr(req: ByteArray, type: Int): Boolean {
        var off = 20
        while (off + 4 <= req.size) {
            val b = ByteBuffer.wrap(req, off, req.size - off).order(ByteOrder.BIG_ENDIAN)
            val at = b.short.toInt() and 0xFFFF
            val al = b.short.toInt() and 0xFFFF
            if (at == type) return true
            off += 4 + al + (-al % 4 + 4) % 4
        }
        return false
    }

    private fun error401For(tx: ByteArray, realm: String, nonce: ByteArray): ByteArray {
        val err = byteArrayOf(0, 0, 4, 1) + "Unauthorized".toByteArray()
        fun attr(t: Int, v: ByteArray): ByteArray {
            val pad = (-v.size % 4 + 4) % 4
            val b = ByteBuffer.allocate(4 + v.size + pad).order(ByteOrder.BIG_ENDIAN)
            b.putShort(t.toShort()); b.putShort(v.size.toShort()); b.put(v)
            repeat(pad) { b.put(0) }
            return b.array()
        }
        val attrs = attr(0x0009, err) + attr(0x0014, realm.toByteArray()) + attr(0x0015, nonce)
        val h = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN)
        h.putShort(0x0113); h.putShort(attrs.size.toShort()); h.putInt(0x2112A442); h.put(tx)
        return h.array() + attrs
    }

    private fun successFor(tx: ByteArray): ByteArray {
        val mask = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(0x2112A442).array()
        val ip = byteArrayOf(203.toByte(), 0, 113, 9)
        val v = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
        v.put(0); v.put(0x01)
        v.putShort((60000 xor (0x2112A442 ushr 16)).toShort())
        v.put(ByteArray(4) { i -> (ip[i].toInt() xor (mask[i].toInt() and 0xFF)).toByte() })
        val h = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN)
        h.putShort(0x0016); h.putShort(8)
        val attrs = h.array() + v.array()
        val head = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN)
        head.putShort(0x0103); head.putShort(attrs.size.toShort()); head.putInt(0x2112A442); head.put(tx)
        return head.array() + attrs
    }
}
