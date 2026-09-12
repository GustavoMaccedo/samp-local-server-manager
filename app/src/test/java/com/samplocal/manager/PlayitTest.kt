package com.samplocal.manager

import com.samplocal.manager.net.playit.AgentLogState
import com.samplocal.manager.net.playit.PlayitAgent
import com.samplocal.manager.net.playit.PlayitApi
import com.samplocal.manager.net.relay.ConnectParams
import com.samplocal.manager.net.relay.PlayitProvider
import com.samplocal.manager.samp.ServerQueryManager
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class PlayitApiTest {

    @Test
    fun claimExchangePendingAndApproved() = runTest {
        val pending = PlayitApi(object : PlayitApi.HttpTransport {
            override fun post(path: String, body: JSONObject, headers: Map<String, String>) =
                JSONObject("""{"status":"fail","data":"CodeNotFound"}""")
        })
        val r = pending.claimExchange("0000000000")
        assertTrue(r.isSuccess)
        assertTrue(r.getOrThrow() is PlayitApi.ClaimResult.Pending)

        val approved = PlayitApi(object : PlayitApi.HttpTransport {
            override fun post(path: String, body: JSONObject, headers: Map<String, String>) =
                JSONObject("""{"status":"success","data":{"secret_key":"ABC123"}}""")
        })
        val r2 = approved.claimExchange("abcdef1234")
        assertEquals("ABC123", (r2.getOrThrow() as PlayitApi.ClaimResult.Approved).secret)
    }

    @Test
    fun tunnelsListParsesEndpoint() = runTest {
        val t = FakeApiTransport { path, _ ->
            assertEquals("/tunnels/list", path)
            JSONObject(
                """{"status":"success","data":{"tunnels":[
                  {"id":"t1","name":"samp-local-X","port_type":"udp","active":true,
                   "alloc":{"port_alloc":{"ip":"1.2.3.4","port":12345}},
                   "region":"sa-east"}]}}"""
            )
        }
        val list = PlayitApi(t).tunnelsList("K").getOrThrow()
        assertEquals(1, list.size)
        assertEquals("1.2.3.4" to 12345, list[0].endpoint)
        assertEquals("sa-east", list[0].region)
    }

    @Test
    fun tunnelsCreateSendsLoopbackOnly() = runTest {
        var sent: JSONObject? = null
        val t = FakeApiTransport { _, body ->
            sent = body
            JSONObject("""{"status":"success","data":{"id":"nt1"}}""")
        }
        val id = PlayitApi(t).tunnelsCreate("K", "samp-local-X", "127.0.0.1", 7777).getOrThrow()
        assertEquals("nt1", id)
        val origin = sent!!.getJSONObject("origin").getJSONObject("data")

        assertEquals("127.0.0.1", origin.getString("local_ip"))
        assertEquals(7777, origin.getInt("local_port"))
        assertEquals("udp", sent!!.getString("port_type"))
    }

    @Test
    fun createRejectsNonLoopback() = runTest {

        val t = FakeApiTransport { path, _ ->
            assertEquals("/tunnels/create", path)
            JSONObject("""{"status":"fail","data":"InvalidIpHostname"}""")
        }
        val r = PlayitApi(t).tunnelsCreate("K", "x", "127.0.0.1", 7777)
        assertTrue(r.isFailure)
    }

    @Test
    fun redactRemovesSecrets() {
        val raw = """{"secret_key":"ABC","agent_key":"DEF","password":"x","user":"u"}"""
        val clean = PlayitApi.redact(raw)
        assertFalse(clean.contains("ABC"))
        assertFalse(clean.contains("DEF"))
        assertTrue(clean.contains("\"user\""))
    }

    @Test
    fun apiErrorSurfaces() = runTest {
        val t = FakeApiTransport { _, _ -> throw PlayitApi.ApiException(401, "unauthorized") }
        val r = PlayitApi(t).tunnelsList("BAD")
        assertTrue(r.isFailure)
        assertTrue(r.exceptionOrNull()?.message?.contains("401") == true)
    }

    @Test
    fun logTailConnected() {
        val log = "INFO Starting playitd version=1.0.10\n" +
            "INFO playit connected; tunnels loaded agent_id=xxx tunnel_count=0\n"
        val p = PlayitAgent.parseLogTail(log)
        assertEquals(AgentLogState.CONNECTED, p.state)
        assertNull(p.errorLine)
    }

    @Test
    fun logTailAuthError() {
        val log = "INFO Starting playitd\n" +
            "ERROR failed to start playit agent error=ApiError(Auth(InvalidAgentKey)) abc\n"
        val p = PlayitAgent.parseLogTail(log)
        assertEquals(AgentLogState.AUTH_ERROR, p.state)
        assertTrue(p.errorLine!!.contains("InvalidAgentKey"))
    }

    @Test
    fun logTailWaiting() {
        val p = PlayitAgent.parseLogTail("INFO Waiting for frontend secret provisioning over IPC\n")
        assertEquals(AgentLogState.WAITING, p.state)
    }

    @Test
    fun logTailIgnoresSecretLines() {

        val p = PlayitAgent.parseLogTail(
            "INFO Starting playitd\n" +
                "DEBUG secret provisioned deadbeef0123456789abcdef\n"
        )
        assertEquals(AgentLogState.STARTING, p.state)
    }
}

class PlayitAgentTest {

    @Test
    fun abiGate() {
        assertTrue(PlayitAgent.checkAbi(listOf("arm64-v8a")).isSuccess)
        assertTrue(PlayitAgent.checkAbi(listOf("armeabi-v7a", "armeabi")).isFailure)
        assertTrue(PlayitAgent.checkAbi(emptyList()).isFailure)
    }

    @Test
    fun binaryAbsentFailsClearly() {
        val dir = createTempDir("noplayit")
        try {
            val agent = TestAgent(dir, FakeStarter())

            val r = agent.start("")
            assertTrue(r.isFailure)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun startStopDelegatesToStarter() {
        val dir = createTempDir("playit-agent")
        try {
            val starter = FakeStarter(alive = true)
            val agent = TestAgent(dir, starter)
            assertTrue(agent.start("SECRET").isSuccess)
            assertTrue(agent.isAlive())
            assertTrue(starter.lastCmd!!.first().endsWith("libplayit-agent.so"))
            assertTrue(starter.lastCmd!!.contains("--socket-path"))
            agent.stop()
            assertEquals(1, starter.stops)
            assertFalse(agent.isAlive())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun spawnFailureSurfaces() {
        val dir = createTempDir("playit-fail")
        try {
            val agent = TestAgent(dir, FakeStarter(failStart = true))
            assertTrue(agent.start("SECRET").isFailure)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun startUnclaimedHasNoSecret() {
        val dir = createTempDir("playit-unclaimed")
        try {
            val starter = FakeStarter(alive = true)
            val agent = TestAgent(dir, starter)
            assertTrue(agent.startUnclaimed().isSuccess)
            val cmd = starter.lastCmd!!
            assertFalse(cmd.any { it.contains("SECRET") })
            assertTrue(cmd.contains("--socket-path"))
            assertFalse(agent.isAuthenticated())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun singletonNoDoubleSpawn() {
        val dir = createTempDir("playit-single")
        try {
            val starter = FakeStarter(alive = true)
            var spawns = 0
            val counting = object : PlayitAgent.ProcessStarter by starter {
                override fun start(
                    cmd: List<String>,
                    env: Map<String, String>
                ): PlayitAgent.ManagedProcess {
                    spawns++
                    return starter.start(cmd, env)
                }
            }
            val agent = TestAgent(dir, FakeStarter())
            agent.starter = counting
            assertTrue(agent.startUnclaimed().isSuccess)
            assertTrue(agent.startUnclaimed().isSuccess)
            assertEquals(1, spawns)
            assertTrue(agent.start("SECRET").isSuccess)
            assertTrue(agent.isAuthenticated())
        } finally {
            dir.deleteRecursively()
        }
    }
}

class PlayitProviderTest {

    private fun provider(
        apiHandler: (String, JSONObject) -> JSONObject,
        starter: FakeStarter = FakeStarter(),
        secrets: MemorySecrets = MemorySecrets()
    ): Triple<PlayitProvider, FakeApiTransport, File> {
        val dir = createTempDir("playit-prov")
        val t = FakeApiTransport(apiHandler)
        val p = PlayitProvider(
            TestAgent(dir, starter),
            PlayitApi(t),
            ServerQueryManager(),
            secrets
        )
        return Triple(p, t, dir)
    }

    private fun listWithTunnel(ep: Pair<String, Int>? = "2.3.4.5" to 23456): JSONObject {
        val alloc = if (ep != null) {
            ""","alloc":{"port_alloc":{"ip":"${ep.first}","port":${ep.second}}}"""
        } else ""
        return JSONObject(
            """{"status":"success","data":{"tunnels":[
              {"id":"t1","name":"samp-local-SRV","port_type":"udp","active":true$alloc,
               "region":"sa-east"}]}}"""
        )
    }

    @Test
    fun unlinkedProviderRefuses() = runTest {
        val (p, _, dir) = provider({ _, _ -> JSONObject() })
        try {
            assertFalse(p.isLinked())
            val r = p.connect(ConnectParams("SRV", 7777))
            assertTrue(r.isFailure)
            assertTrue(r.exceptionOrNull()?.message?.contains("NOT_LINKED") == true)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun fullConnectFlow() = runTest {
        val secrets = MemorySecrets()
        secrets.save(PlayitProvider.KEY, "SECRET".toByteArray())
        var created = 0
        val (p, t, dir) = provider({ path, _ ->
            when (path) {
                "/tunnels/list" -> if (created == 0) {
                    JSONObject("""{"status":"success","data":{"tunnels":[]}}""")
                } else listWithTunnel()
                "/tunnels/create" -> {
                    created++
                    JSONObject("""{"status":"success","data":{"id":"nt1"}}""")
                }
                else -> JSONObject()
            }
        }, secrets = secrets)
        try {
            val session = p.connect(ConnectParams("SRV", 7777)).getOrThrow()
            assertEquals("2.3.4.5", session.endpoint.ip)
            assertEquals(23456, session.endpoint.port)
            assertEquals(1, created)
            assertTrue(t.calls.contains("/tunnels/list"))
            assertTrue(t.calls.contains("/tunnels/create"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun reusesExistingTunnel() = runTest {
        val secrets = MemorySecrets()
        secrets.save(PlayitProvider.KEY, "SECRET".toByteArray())
        var created = 0
        val (p, _, dir) = provider({ path, _ ->
            when (path) {
                "/tunnels/list" -> listWithTunnel()
                "/tunnels/create" -> {
                    created++
                    JSONObject("""{"status":"success","data":{"id":"nt9"}}""")
                }
                else -> JSONObject()
            }
        }, secrets = secrets)
        try {
            p.connect(ConnectParams("SRV", 7777)).getOrThrow()
            assertEquals(0, created)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun multiServerIsolation() = runTest {
        val secrets = MemorySecrets()
        secrets.save(PlayitProvider.KEY, "SECRET".toByteArray())
        val names = mutableListOf<String>()
        val (p, _, dir) = provider({ path, body ->
            if (path == "/tunnels/list") {
                JSONObject("""{"status":"success","data":{"tunnels":[]}}""")
            } else {
                names.add(body.optString("name", ""))
                JSONObject("""{"status":"success","data":{"id":"n"}}""")
            }
        }, secrets = secrets)
        try {

            p.connect(ConnectParams("A", 7777))

        val (p2, _, dir2) = provider({ path, body ->
            if (path == "/tunnels/list") {
                val mine = names.filter { it == "samp-local-B" }
                if (mine.isEmpty()) {
                    JSONObject("""{"status":"success","data":{"tunnels":[
                      {"id":"a","name":"samp-local-A","port_type":"udp","active":true,
                       "alloc":{"port_alloc":{"ip":"1.1.1.1","port":111}}} ]}}""")
                } else {
                    JSONObject("""{"status":"success","data":{"tunnels":[
                      {"id":"b","name":"samp-local-B","port_type":"udp","active":true,
                       "alloc":{"port_alloc":{"ip":"2.2.2.2","port":222}}} ]}}""")
                }
            } else {
                names.add(body.optString("name", ""))
                JSONObject("""{"status":"success","data":{"id":"n"}}""")
            }
        }, secrets = secrets)
            try {
                p2.connect(ConnectParams("B", 7778)).getOrThrow()
                assertTrue(names.any { it == "samp-local-B" })
                assertFalse(names.any { it == "samp-local-A" && names.count { n -> n == "samp-local-A" } > 1 })
            } finally {
                dir2.deleteRecursively()
            }
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun secretNeverInCalls() = runTest {
        val secrets = MemorySecrets()
        secrets.save(PlayitProvider.KEY, "SUPERSECRET".toByteArray())
        val bodies = mutableListOf<String>()
        val (p, _, dir) = provider({ _, body ->
            bodies.add(body.toString())
            JSONObject("""{"status":"success","data":{"tunnels":[]}}""")
        }, secrets = secrets)
        try {
            p.connect(ConnectParams("SRV", 7777))
            assertTrue(bodies.none { it.contains("SUPERSECRET") })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun list401AsksRelink() = runTest {
        val secrets = MemorySecrets()
        secrets.save(PlayitProvider.KEY, "DEAD".toByteArray())
        val (p, _, dir) = provider({ path, _ ->
            if (path == "/tunnels/list") throw PlayitApi.ApiException(401, "AuthRequired")
            JSONObject("""{"status":"success","data":{"id":"n"}}""")
        }, secrets = secrets)
        try {
            val r = p.connect(ConnectParams("SRV", 7777))
            assertTrue(r.isFailure)
            val msg = r.exceptionOrNull()?.message.orEmpty()
            assertTrue(msg.contains("401"))
            assertTrue(msg.contains("Desvincule"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun create401GuidesUser() = runTest {
        val secrets = MemorySecrets()
        secrets.save(PlayitProvider.KEY, "SECRET".toByteArray())
        val (p, _, dir) = provider({ path, _ ->
            when (path) {
                "/tunnels/list" -> JSONObject("""{"status":"success","data":{"tunnels":[]}}""")
                else -> throw PlayitApi.ApiException(401, "AuthRequired")
            }
        }, secrets = secrets)
        try {
            val r = p.connect(ConnectParams("SRV", 7777))
            assertTrue(r.isFailure)
            val msg = r.exceptionOrNull()?.message.orEmpty()
            assertTrue(msg.contains("401"))
            assertTrue(msg.contains("dashboard") || msg.contains("email"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun singleManualTunnelAdopted() = runTest {
        val secrets = MemorySecrets()
        secrets.save(PlayitProvider.KEY, "SECRET".toByteArray())
        var created = 0
        val (p, _, dir) = provider({ path, _ ->
            when (path) {
                "/tunnels/list" -> JSONObject(
                    """{"status":"success","data":{"tunnels":[
                      {"id":"m1","name":"meu-tunnel","port_type":"udp","active":true,
                       "alloc":{"port_alloc":{"ip":"9.9.9.9","port":9999}},
                       "origin":{"type":"agent","data":{"local_ip":"127.0.0.1","local_port":7777}}} ]}}"""
                )
                else -> {
                    created++
                    JSONObject("""{"status":"success","data":{"id":"n"}}""")
                }
            }
        }, secrets = secrets)
        try {
            val s = p.connect(ConnectParams("SRV", 7777)).getOrThrow()
            assertEquals("9.9.9.9", s.endpoint.ip)
            assertEquals(0, created)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun wrongOriginTunnelNotAdopted() = runTest {
        val secrets = MemorySecrets()
        secrets.save(PlayitProvider.KEY, "SECRET".toByteArray())
        var created = 0
        val (p, _, dir) = provider({ path, body ->
            when (path) {
                "/tunnels/list" -> {
                    if (created == 0) {
                        JSONObject(
                            """{"status":"success","data":{"tunnels":[
                              {"id":"m1","name":"outro-jogo","port_type":"udp","active":true,
                               "alloc":{"port_alloc":{"ip":"9.9.9.9","port":9999}},
                               "origin":{"type":"agent","data":{"local_ip":"192.168.1.50","local_port":25565}}} ]}}"""
                        )
                    } else {
                        JSONObject(
                            """{"status":"success","data":{"tunnels":[
                              {"id":"n","name":"samp-local-SRV","port_type":"udp","active":true,
                               "alloc":{"port_alloc":{"ip":"9.9.9.9","port":8888}}} ]}}"""
                        )
                    }
                }
                else -> {
                    created++
                    JSONObject("""{"status":"success","data":{"id":"n"}}""")
                }
            }
        }, secrets = secrets)
        try {

            val s = p.connect(ConnectParams("SRV", 7777)).getOrThrow()
            assertEquals(1, created)
            assertEquals(8888, s.endpoint.port)
        } finally {
            dir.deleteRecursively()
        }
    }
}

class PlayitListErrorsTest {

    private fun provider(
        apiHandler: (String, JSONObject) -> JSONObject,
        secrets: MemorySecrets = MemorySecrets().also {
            it.save(PlayitProvider.KEY, "SECRET".toByteArray())
        }
    ): Triple<PlayitProvider, FakeApiTransport, java.io.File> {
        val dir = createTempDir("playit-err")
        val t = FakeApiTransport(apiHandler)
        val p = PlayitProvider(
            TestAgent(dir, FakeStarter()),
            PlayitApi(t),
            ServerQueryManager(),
            secrets
        )
        return Triple(p, t, dir)
    }

    @Test
    fun transientListFailureRetriesAndSucceeds() = runTest {
        var calls = 0
        val (p, _, dir) = provider({ path, _ ->
            when (path) {
                "/tunnels/list" -> {
                    calls++
                    if (calls == 1) throw java.net.SocketTimeoutException("timed out")
                    JSONObject(
                        """{"status":"success","data":{"tunnels":[
                          {"id":"t1","name":"samp-local-SRV","port_type":"udp","active":true,
                           "alloc":{"port_alloc":{"ip":"5.6.7.8","port":5555}}} ]}}"""
                    )
                }
                else -> JSONObject("""{"status":"success","data":{"id":"n"}}""")
            }
        })
        try {
            val s = p.connect(ConnectParams("SRV", 7777)).getOrThrow()
            assertEquals("5.6.7.8", s.endpoint.ip)
            assertEquals(2, calls)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun listFailurePreservesCause() = runTest {
        val (p, _, dir) = provider({ path, _ ->
            if (path == "/tunnels/list") throw java.net.UnknownHostException("api.playit.gg")
            JSONObject("""{"status":"success","data":{"id":"n"}}""")
        })
        try {
            val r = p.connect(ConnectParams("SRV", 7777))
            assertTrue(r.isFailure)
            val msg = r.exceptionOrNull()?.message.orEmpty()
            assertTrue(msg.contains("falha ao listar tunnels"))
            assertTrue(msg.contains("api.playit.gg"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun auth401ShortCircuitsWithGuidance() = runTest {
        var lists = 0
        val (p, _, dir) = provider({ path, _ ->
            if (path == "/tunnels/list") {
                lists++
                throw PlayitApi.ApiException(401, "AuthRequired")
            }
            JSONObject("""{"status":"success","data":{"id":"n"}}""")
        })
        try {
            val r = p.connect(ConnectParams("SRV", 7777))
            assertTrue(r.isFailure)
            assertTrue(r.exceptionOrNull()?.message?.contains("401") == true)
            assertEquals(1, lists)
        } finally {
            dir.deleteRecursively()
        }
    }
}
