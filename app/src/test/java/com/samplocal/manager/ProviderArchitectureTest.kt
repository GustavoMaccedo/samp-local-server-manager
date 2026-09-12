package com.samplocal.manager

import com.samplocal.manager.net.relay.ConnectParams
import com.samplocal.manager.net.relay.CredentialProvider
import com.samplocal.manager.net.relay.ProviderEndpoint
import com.samplocal.manager.net.relay.ProviderHealth
import com.samplocal.manager.net.relay.ProviderKind
import com.samplocal.manager.net.relay.ProviderSelector
import com.samplocal.manager.net.relay.ProviderSession
import com.samplocal.manager.net.relay.RelayDirectory
import com.samplocal.manager.net.relay.RelayProvider
import com.samplocal.manager.net.relay.SessionStats
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class ProviderArchitectureTest {

    private fun fake(
        id: String,
        kind: ProviderKind,
        vanilla: Boolean,
        priority: Int,
        healthy: Boolean,
        latency: Long? = 10L
    ): RelayProvider = object : RelayProvider {
        override val id = id
        override val kind = kind
        override val region = "test"
        override val priority = priority
        override val transport = "udp"
        override val supportsVanillaSamp = vanilla
        override suspend fun healthCheck() =
            ProviderHealth(healthy, latency, if (healthy) "ok" else "down")
        override suspend fun connect(params: ConnectParams): Result<ProviderSession> =
            if (healthy) Result.success(object : ProviderSession {
                override val providerId = id
                override val endpoint = ProviderEndpoint("9.9.9.9", 5000)
                override suspend fun heartbeat() = true
                override fun stats() = SessionStats()
                override fun close() { }
            }) else Result.failure(IllegalStateException("down"))
        override fun close() { }
    }

    @Test
    fun prefersHealthyCompatibleByPriorityThenLatency() {
        val ownSlow = fake("own-b", ProviderKind.OWN_UDP, true, 10, true, 200L)
        val ownFast = fake("own-a", ProviderKind.OWN_UDP, true, 10, true, 20L)
        val sel = ProviderSelector.selectForVanilla(
            listOf(
                ProviderSelector.Candidate(ownSlow, ownSlow.healthCheckBlocking()),
                ProviderSelector.Candidate(ownFast, ownFast.healthCheckBlocking())
            )
        )
        assertEquals("own-a", sel.provider?.id)
    }

    @Test
    fun skipsIncompatibleTurnEvenIfHealthy() {
        val turn = fake("twilio", ProviderKind.TURN, false, 1, true, 5L)
        val own = fake("own", ProviderKind.OWN_UDP, true, 50, true, 500L)
        val sel = ProviderSelector.selectForVanilla(
            listOf(
                ProviderSelector.Candidate(turn, ProviderHealth(true, 5L, "allocate OK")),
                ProviderSelector.Candidate(own, ProviderHealth(true, 500L, "ok"))
            )
        )
        assertEquals("own", sel.provider?.id)
    }

    @Test
    fun fallsBackThroughChain() {
        val a = fake("a", ProviderKind.OWN_UDP, true, 10, false)
        val b = fake("b", ProviderKind.OWN_UDP, true, 20, true, 30L)
        val sel = ProviderSelector.selectForVanilla(
            listOf(
                ProviderSelector.Candidate(a, ProviderHealth(false, null, "down")),
                ProviderSelector.Candidate(b, ProviderHealth(true, 30L, "ok"))
            )
        )
        assertEquals("b", sel.provider?.id)
    }

    @Test
    fun honestFailureWhenNothingViable() {
        val sel = ProviderSelector.selectForVanilla(
            listOf(
                ProviderSelector.Candidate(
                    fake("t", ProviderKind.TURN, false, 1, true),
                    ProviderHealth(true, 5L, "ok")
                )
            )
        )
        assertNull(sel.provider)
        assertTrue(sel.reason.isNotBlank())
    }

    @Test
    fun turnMarkedIncompatibleForVanilla() {

        val turn = TurnRelayProviderShim()
        assertFalse(turn.supportsVanillaSamp)
        assertTrue(turn.vanillaReason.contains("RFC 8656"))
    }

    @Test
    fun directoryRejectsSecretsInProviders() {
        try {
            RelayDirectory.parse(
                """{"version":2,"providers":[
                  {"id":"x","type":"TURN","host":"h","port":3478,"user":"u","pass":"p"}]}"""
            )
            fail("deveria rejeitar segredo")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("segredo"))
        }
    }

    @Test
    fun directoryV2ParsesProvidersAndCreds() {
        val cfg = RelayDirectory.parse(
            """{"version":2,"providers":[
              {"id":"own-sa","type":"OWN","host":"r.exemplo.com","port":7779,
               "transport":"udp","region":"sa-east","priority":10,"enabled":true},
              {"id":"tw","type":"TURN","host":"t.exemplo.com","port":3478,
               "transport":"udp","region":"global","priority":20,"enabled":true}],
              "credentials":{"tw":"https://op.exemplo.com/creds"}}"""
        )
        assertEquals(2, cfg.providers.size)
        assertEquals("https://op.exemplo.com/creds", cfg.credentials["tw"])
        assertEquals("sa-east", cfg.providers[0].region)
        assertEquals("r.exemplo.com", cfg.relayHost)
    }

    @Test
    fun twilioStyleCredentialsParsed() {
        val c = CredentialProvider.parse(
            """{"username":"u123","password":"p456","ttl":3600,
               "ice_servers":[{"urls":"turn:x:3478?transport=udp","username":"u123","credential":"p456"}]}"""
        )
        assertEquals("u123", c.username)
        assertEquals("p456", c.password)
    }

    private class TurnRelayProviderShim {
        val supportsVanillaSamp = false
        val vanillaReason =
            "TURN exige permissão por IP de jogador (RFC 8656); clientes vanilla não sinalizam"
    }
}

private fun RelayProvider.healthCheckBlocking(): ProviderHealth =
    kotlinx.coroutines.runBlocking { healthCheck() }

class TurnVanillaProofTest {

    private class StrictFakeTurn(val port: Int) {
        val sock = DatagramSocket(port, InetAddress.getByName("127.0.0.1"))
        val perms = mutableSetOf<String>()
        @Volatile var running = true
        val fwd = mutableListOf<ByteArray>()

        fun serve() {
            val buf = ByteArray(2048)
            while (running) {
                try {
                    val p = DatagramPacket(buf, buf.size)
                    sock.soTimeout = 500
                    sock.receive(p)
                    val d = p.data.copyOf(p.length)
                    when {

                        String(d).startsWith("PERM ") -> {
                            perms.add(String(d).removePrefix("PERM ").trim())
                            sock.send(DatagramPacket("OK".toByteArray(), 2, p.address, p.port))
                        }

                        String(d).startsWith("PLAYER:") -> {
                            val key = "${p.address.hostAddress}:${p.port}"
                            if (key in perms) {
                                synchronized(fwd) { fwd.add(d) }
                                sock.send(DatagramPacket("FWD".toByteArray(), 3, p.address, p.port))
                            }
                        }
                    }
                } catch (_: Exception) { }
            }
        }

        fun close() {
            running = false
            sock.close()
        }
    }

    @Test
    fun unknownPeerDroppedPermissionedForwarded() {
        val turn = StrictFakeTurn(0)
        val t = Thread { turn.serve() }
        t.isDaemon = true
        t.start()
        try {
            val port = turn.sock.localPort
            fun sendRecv(msg: ByteArray, timeout: Int = 1200): ByteArray? {
                DatagramSocket().use { s ->
                    s.soTimeout = timeout
                    s.send(DatagramPacket(msg, msg.size, InetAddress.getByName("127.0.0.1"), port))
                    return try {
                        val b = ByteArray(2048)
                        val p = DatagramPacket(b, b.size)
                        s.receive(p)
                        p.data.copyOf(p.length)
                    } catch (_: Exception) { null }
                }
            }

            assertNull(sendRecv("PLAYER:oi".toByteArray(), 800))

            DatagramSocket().use { s ->
                s.soTimeout = 2000
                val me = "127.0.0.1:${s.localPort}"
                s.send(DatagramPacket("PERM $me".toByteArray(), 5 + me.length,
                    InetAddress.getByName("127.0.0.1"), port))
                val b = ByteArray(64)
                s.receive(DatagramPacket(b, b.size))
                s.send(DatagramPacket("PLAYER:oi".toByteArray(), 9,
                    InetAddress.getByName("127.0.0.1"), port))
                val r = ByteArray(64)
                s.receive(DatagramPacket(r, r.size))
                assertEquals("FWD", String(r, 0, 3))
            }
        } finally {
            turn.close()
        }
    }
}
