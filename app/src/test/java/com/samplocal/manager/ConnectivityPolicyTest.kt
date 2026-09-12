package com.samplocal.manager

import com.samplocal.manager.net.CandidateType
import com.samplocal.manager.net.ConnectivityRepository
import com.samplocal.manager.net.IceCandidate
import com.samplocal.manager.net.LinkState
import com.samplocal.manager.net.PublishMethod
import com.samplocal.manager.net.PublishState
import com.samplocal.manager.net.SignalingSession
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class ConnectivityPolicyTest {

    @Test
    fun localEndpointUsesLoopback() {
        val st = PublishState(method = PublishMethod.LOCAL, sampPort = 7777)
        assertEquals("127.0.0.1:7777", st.playerEndpoint())
    }

    @Test
    fun directEndpointOnlyWhenTested() {
        assertEquals(
            "200.1.2.3:7777",
            PublishState(method = PublishMethod.DIRECT, publicIp = "200.1.2.3", publicPort = 7777)
                .playerEndpoint()
        )

        assertNull(
            PublishState(method = PublishMethod.DIRECT).playerEndpoint()
        )
        assertNull(PublishState(method = PublishMethod.NONE).playerEndpoint())
    }

    @Test
    fun serverStopDropsPublication() {
        val on = PublishState(method = PublishMethod.DIRECT, linkState = LinkState.CONNECTED, players = 3)
        val off = ConnectivityRepository.serverRunningTransition(on, running = false)
        assertEquals(LinkState.OFFLINE, off.linkState)
        assertEquals(PublishMethod.NONE, off.method)
        assertNull(off.players)
    }

    @Test
    fun serverRunningKeepsState() {
        val on = PublishState(method = PublishMethod.DIRECT, linkState = LinkState.CONNECTED)
        assertEquals(on, ConnectivityRepository.serverRunningTransition(on, running = true))
    }

    @Test
    fun selectionPolicy() {
        assertEquals(
            PublishMethod.DIRECT,
            ConnectivityRepository.selectMethod(directProven = true, relayActive = true)
        )
        assertEquals(
            PublishMethod.RELAY,
            ConnectivityRepository.selectMethod(directProven = false, relayActive = true)
        )
        assertEquals(
            PublishMethod.NONE,
            ConnectivityRepository.selectMethod(directProven = false, relayActive = false)
        )
    }

    @Test
    fun networkLostClearsReflexive() {
        val st = PublishState(
            method = PublishMethod.DIRECT, linkState = LinkState.CONNECTED,
            publicIp = "200.1.2.3", publicPort = 45000, directAvailable = true
        )
        val next = ConnectivityRepository.networkLostTransition(st)
        assertEquals(LinkState.RECONNECTING, next.linkState)
        assertNull(next.publicIp)
        assertNull(next.directAvailable)
        assertEquals(PublishMethod.NONE, next.method)
    }

    @Test
    fun loopbackUdpRoundtrip() {
        val server = DatagramSocket(0)
        try {
            val payload = "SAMP".toByteArray()
            server.soTimeout = 3000
            DatagramSocket().use { cli ->
                cli.send(DatagramPacket(payload, payload.size, InetAddress.getByName("127.0.0.1"), server.localPort))
                val buf = ByteArray(16)
                val pkt = DatagramPacket(buf, buf.size)
                server.receive(pkt)
                assertArrayEquals(payload, pkt.data.copyOf(pkt.length))
            }
        } finally {
            server.close()
        }
    }

    @Test
    fun stunTimeoutSurfacesFailure() = runTest {
        val client = com.samplocal.manager.net.stun.StunClient(
            transport = object : com.samplocal.manager.net.stun.UdpTransport {
                override fun exchange(host: String, port: Int, req: ByteArray, timeoutMs: Int): ByteArray {
                    throw java.net.SocketTimeoutException("timed out")
                }
            }
        )
        val r = client.discover("127.0.0.1", 9, timeoutMs = 100)
        assertTrue(r.isFailure)
    }
}

class SessionAuthTest {

    @Test
    fun candidateOfferCarriesSession() {
        val c = IceCandidate.host(InetAddress.getByName("192.168.0.2"), 7777)
        val json = IceCandidate.listToJson(listOf(c))
        assertTrue(json.contains("192.168.0.2"))
        val back = IceCandidate.listFromJson(json)
        assertEquals(1, back.size)
        assertEquals(CandidateType.HOST, back[0].type)
    }

    @Test
    fun md5KeyIsStable() {
        val k1 = SignalingSession.md5key("u", "r", "p")
        val k2 = SignalingSession.md5key("u", "r", "p")
        assertArrayEquals(k1, k2)
        assertEquals(16, k1.size)
        assertFalse(k1.contentEquals(SignalingSession.md5key("u", "r", "x")))
    }
}
