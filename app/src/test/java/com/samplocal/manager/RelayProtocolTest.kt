package com.samplocal.manager

import com.samplocal.manager.net.NatClassifier
import com.samplocal.manager.net.relay.HeartbeatStats
import com.samplocal.manager.net.relay.RelayDirectory
import com.samplocal.manager.net.relay.RelayProtocol
import com.samplocal.manager.net.relay.UdpBridge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class RelayProtocolTest {

    private val sid = ByteArray(16) { it.toByte() }
    private val token = ByteArray(32) { (it * 3).toByte() }

    @Test
    fun idsAreUniqueAndSized() {
        val a = RelayProtocol.newSessionId()
        val b = RelayProtocol.newSessionId()
        assertEquals(16, a.size)
        assertFalse(a.contentEquals(b))
        assertEquals(32, RelayProtocol.newToken().size)
    }

    @Test
    fun registerRoundtrip() {
        val req = RelayProtocol.buildRegister(sid, token, 7777)
        assertEquals(0x01, req[4].toInt())
        assertTrue(RelayProtocol.verify(token, req))
        assertFalse(RelayProtocol.verify(ByteArray(32), req))
    }

    @Test
    fun registeredParses() {
        val body2 = "SLR1".toByteArray() + byteArrayOf(0x02) + sid +
            byteArrayOf(0x75, 0x30, 0, 90)
        val pkt = body2 + RelayProtocol.hmac(token, body2)
        val r = RelayProtocol.parseRegistered(pkt, sid, token)!!
        assertEquals(30000, r.sessionPort)
        assertEquals(90, r.expiresSec)
        assertNull(RelayProtocol.parseRegistered(pkt, ByteArray(16), token))
    }

    @Test
    fun dataR2AWithPlayer() {
        val pkt = RelayProtocol.buildDataR2A(
            sid, token, 9, InetAddress.getByName("1.2.3.4"), 5123, "hi".toByteArray()
        )
        val m = RelayProtocol.parseDataR2A(pkt, sid, token)!!
        assertEquals(9, m.seq)
        assertEquals("1.2.3.4", m.playerIp.hostAddress)
        assertEquals(5123, m.playerPort)
        assertArrayEquals("hi".toByteArray(), m.payload)
    }

    @Test
    fun tamperedFails() {
        val pkt = RelayProtocol.buildDataR2A(
            sid, token, 1, InetAddress.getByName("1.1.1.1"), 1, "x".toByteArray()
        )
        pkt[30] = (pkt[30] + 1).toByte()
        assertNull(RelayProtocol.parseDataR2A(pkt, sid, token))
    }

    @Test
    fun pongRoundtrip() {
        val req = RelayProtocol.buildPing(sid, token, 5, 123456L)

        val body = "SLR1".toByteArray() + byteArrayOf(0x21) + sid +
            req.copyOfRange(21, 21 + 4 + 8) + ByteArray(8)
        val pkt = body + RelayProtocol.hmac(token, body)
        val p = RelayProtocol.parsePong(pkt, sid, token)!!
        assertEquals(5, p.seq)
        assertEquals(123456L, p.tSendMs)
    }

    @Test
    fun heartbeatStatsMath() {
        val s = HeartbeatStats.compute(listOf(10L, 20L, 30L), sent = 4)
        assertEquals(30L, s.lastRttMs)
        assertEquals(25f, s.lossPct!!, 0.01f)
        assertEquals(10f, s.jitterMs!!, 0.01f)
        val empty = HeartbeatStats.compute(emptyList(), 2)
        assertNull(empty.lastRttMs)
    }

    @Test
    fun directoryParses() {
        val cfg = RelayDirectory.parse(
            """{"version":1,"relay":{"host":"r.exemplo.com","port":7779},
               "turn":{"host":"t.exemplo.com","port":3478,"user":"u","pass":"p"}}"""
        )
        assertEquals("r.exemplo.com", cfg.relayHost)
        assertEquals(7779, cfg.relayPort)
        assertEquals("t.exemplo.com", cfg.turn!!.host)
    }

    @Test(expected = IllegalArgumentException::class)
    fun directoryRejectsGarbage() {
        RelayDirectory.parse("""{"version":9}""")
    }

    @Test
    fun traversalCompare() {
        assertEquals(
            NatClassifier.Mapping.PORT_PRESERVED,
            NatClassifier.compareMappings(45000, 45000)
        )
        assertEquals(
            NatClassifier.Mapping.SYMMETRIC,
            NatClassifier.compareMappings(45000, 45001)
        )
        assertEquals(NatClassifier.Mapping.UNKNOWN, NatClassifier.compareMappings(null, 1))
        assertTrue(NatClassifier.cgnatSuspect("Mobile", NatClassifier.Mapping.SYMMETRIC,
            com.samplocal.manager.net.NatState.NAT))
        assertFalse(NatClassifier.cgnatSuspect("Wi-Fi", NatClassifier.Mapping.SYMMETRIC,
            com.samplocal.manager.net.NatState.NAT))
    }
}

class UdpBridgeTest {

    private val sid = ByteArray(16) { 7 }
    private val token = ByteArray(32) { 9 }

    @Test
    fun forwardsBothDirections() = runTest {
        val relay = DatagramSocket(0)
        val relayPort = relay.localPort
        val samp = DatagramSocket(0, InetAddress.getByName("127.0.0.1"))
        val sampPort = samp.localPort
        val tunnel = DatagramSocket(0, InetAddress.getByName("127.0.0.1"))
        val bridge = UdpBridge("127.0.0.1", relayPort, token, sid, sampPort)
        val job = launch(kotlinx.coroutines.Dispatchers.IO) {

            val buf = ByteArray(2048)
            try {
                while (true) {
                    val p = DatagramPacket(buf, buf.size)
                    samp.receive(p)
                    samp.send(DatagramPacket(p.data, p.length, p.address, p.port))
                }
            } catch (_: Exception) { }
        }
        try {
            bridge.start(this, tunnel)

            val r2a = RelayProtocol.buildDataR2A(
                sid, token, 1, InetAddress.getByName("9.9.9.9"), 4000, "PING".toByteArray()
            )
            relay.send(DatagramPacket(r2a, r2a.size, InetAddress.getByName("127.0.0.1"),
                tunnel.localPort))

            relay.soTimeout = 5000
            val buf = ByteArray(2048)
            val back = DatagramPacket(buf, buf.size)
            relay.receive(back)
            val raw = back.data.copyOf(back.length)
            assertEquals(0x10, raw[4].toInt())
            assertTrue(RelayProtocol.verify(token, raw))

            val pay = raw.copyOfRange(31, raw.size - 16)
            assertArrayEquals("PING".toByteArray(), pay)
            assertEquals("9.9.9.9", InetAddress.getByAddress(raw.copyOfRange(25, 29)).hostAddress)
            assertTrue(bridge.pktsDown.get() >= 1)
            assertTrue(bridge.pktsUp.get() >= 1)
        } finally {
            bridge.stop()
            job.cancel()
            relay.close(); samp.close(); tunnel.close()
        }
    }

    @Test
    fun foreignSourceDropped() = runTest {
        val relay = DatagramSocket(0, InetAddress.getByName("127.0.0.2"))
        val samp = DatagramSocket(0, InetAddress.getByName("127.0.0.1"))
        val tunnel = DatagramSocket(0, InetAddress.getByName("127.0.0.1"))

        val bridge = UdpBridge("127.0.0.2", 9999, token, sid, samp.localPort)
        try {
            bridge.start(this, tunnel)
            val alien = DatagramSocket(0, InetAddress.getByName("127.0.0.1"))
            try {
                val r2a = RelayProtocol.buildDataR2A(
                    sid, token, 1, InetAddress.getByName("9.9.9.9"), 4000, "X".toByteArray()
                )
                alien.send(DatagramPacket(r2a, r2a.size, InetAddress.getByName("127.0.0.1"),
                    tunnel.localPort))
                delay(700)
                assertEquals(0, bridge.pktsDown.get())
            } finally {
                alien.close()
            }
        } finally {
            bridge.stop()
            relay.close(); samp.close(); tunnel.close()
        }
    }

    @Test
    fun rateLimitCaps() = runTest {
        val relay = DatagramSocket(0, InetAddress.getByName("127.0.0.1"))
        val samp = DatagramSocket(0, InetAddress.getByName("127.0.0.1"))

        val drain = launch(kotlinx.coroutines.Dispatchers.IO) {
            val b = ByteArray(2048)
            try {
                while (true) samp.receive(DatagramPacket(b, b.size))
            } catch (_: Exception) { }
        }
        val tunnel = DatagramSocket(0, InetAddress.getByName("127.0.0.1"))
        val bridge = UdpBridge("127.0.0.1", relay.localPort, token, sid, samp.localPort,
            maxPps = 3, maxBps = Long.MAX_VALUE)
        try {
            bridge.start(this, tunnel)
            repeat(10) { i ->
                val r2a = RelayProtocol.buildDataR2A(
                    sid, token, i, InetAddress.getByName("9.9.9.9"), 4000, "Z".toByteArray()
                )
                relay.send(DatagramPacket(r2a, r2a.size, InetAddress.getByName("127.0.0.1"),
                    tunnel.localPort))
            }
            delay(800)
            assertTrue(bridge.pktsDown.get() <= 3)
            assertTrue(bridge.pktsDown.get() >= 1)
        } finally {
            bridge.stop()
            drain.cancel()
            relay.close(); samp.close(); tunnel.close()
        }
    }

    @Test
    fun sessionsIsolatedById() {
        val a = RelayProtocol.newSessionId()
        val b = RelayProtocol.newSessionId()
        assertFalse(a.contentEquals(b))
        val ta = RelayProtocol.newToken()
        val tb = RelayProtocol.newToken()
        assertFalse(ta.contentEquals(tb))

        val pkt = RelayProtocol.buildDataR2A(
            a, ta, 1, InetAddress.getByName("1.1.1.1"), 1, "q".toByteArray()
        )
        assertNull(RelayProtocol.parseDataR2A(pkt, b, ta))
        assertNull(RelayProtocol.parseDataR2A(pkt, a, tb))
    }
}
