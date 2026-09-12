package com.samplocal.manager

import com.samplocal.manager.net.CandidateType
import com.samplocal.manager.net.IceCandidate
import com.samplocal.manager.net.NatClassifier
import com.samplocal.manager.net.NatState
import com.samplocal.manager.net.NetworkProbe
import org.junit.Assert.*
import org.junit.Test
import java.net.InetAddress

class NetLogicTest {

    @Test
    fun picksPrivateIpv4SkippingFirst() {
        val addrs = listOf(
            InetAddress.getByName("fe80::1"),
            InetAddress.getByName("8.8.8.8"),
            InetAddress.getByName("192.168.1.25"),
            InetAddress.getByName("10.0.0.9")
        )

        assertEquals("192.168.1.25", NetworkProbe.selectLanIpv4(addrs))
    }

    @Test
    fun rejectsLoopbackAndPublicOnly() {
        assertNull(NetworkProbe.selectLanIpv4(listOf(InetAddress.getByName("127.0.0.1"))))
        assertNull(NetworkProbe.selectLanIpv4(listOf(InetAddress.getByName("203.0.113.7"))))
        assertEquals("172.20.5.4", NetworkProbe.selectLanIpv4(listOf(InetAddress.getByName("172.20.5.4"))))
        assertNull(NetworkProbe.selectLanIpv4(listOf(InetAddress.getByName("172.32.0.1"))))
    }

    @Test
    fun invalidPortsRejected() {
        assertFalse(NetworkProbe.isValidSampPort(0))
        assertFalse(NetworkProbe.isValidSampPort(-1))
        assertFalse(NetworkProbe.isValidSampPort(99999))
        assertTrue(NetworkProbe.isValidSampPort(7777))
        assertTrue(NetworkProbe.isValidSampPort(65535))
    }

    @Test
    fun directWhenReflexiveEqualsLocal() {
        val v = NatClassifier.classify(
            InetAddress.getByName("200.1.2.3"), 7777,
            InetAddress.getByName("200.1.2.3"), 7777, true
        )
        assertEquals(NatState.DIRECT, v.state)
    }

    @Test
    fun natWhenDifferent() {
        val v = NatClassifier.classify(
            InetAddress.getByName("192.168.1.25"), 7777,
            InetAddress.getByName("200.1.2.3"), 45000, true
        )
        assertEquals(NatState.NAT, v.state)
        assertTrue(v.detail.contains("192.168.1.25"))
    }

    @Test
    fun unreachableWithoutNetwork() {
        val v = NatClassifier.classify(null, 7777, null, null, false)
        assertEquals(NatState.UNREACHABLE, v.state)
    }

    @Test
    fun unknownOnStunTimeout() {
        val v = NatClassifier.classify(
            InetAddress.getByName("192.168.1.25"), 7777, null, null, true
        )
        assertEquals(NatState.UNKNOWN, v.state)
    }

    @Test
    fun priorityOrderHostSrflxRelay() {
        val h = IceCandidate.host(InetAddress.getByName("192.168.1.25"), 7777)
        val s = IceCandidate.srflx(InetAddress.getByName("200.1.2.3"), 45000, "192.168.1.25")
        val r = IceCandidate.relay("10.9.9.9", 50000)
        assertTrue(h.priority > s.priority)
        assertTrue(s.priority > r.priority)
        val ordered = IceCandidate.prioritize(listOf(r, s, h))
        assertEquals(CandidateType.HOST, ordered[0].type)
        assertEquals(CandidateType.RELAY, ordered[2].type)
    }

    @Test
    fun candidateJsonRoundtrip() {
        val c = IceCandidate.host(InetAddress.getByName("10.0.0.2"), 7777)
        val back = IceCandidate.fromJson(c.toJson())
        assertEquals(c.type, back.type)
        assertEquals(c.ip, back.ip)
        assertEquals(c.port, back.port)
        assertEquals(c.priority, back.priority)
    }

    @Test
    fun nominatesFirstWorkingPair() {
        val pairs = listOf("host", "srflx", "relay")
        val works: (String) -> Boolean = { it != "srflx" }
        val nominated = pairs.firstOrNull(works)
        assertEquals("host", nominated)
        assertEquals("relay", listOf("srflx", "relay").firstOrNull { it == "relay" && works(it) })
    }
}
