package com.samplocal.manager

import com.samplocal.manager.net.playit.PlayitApi
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ClaimFlowTest {

    private fun setupResp(state: String) =
        JSONObject("""{"status":"success","data":"$state"}""")

    private fun approvedResp(secret: String) =
        JSONObject("""{"status":"success","data":{"secret_key":"$secret"}}""")

    @Test
    fun setupRegistersBeforeExchange() = runTest {
        val calls = mutableListOf<String>()
        var polls = 0
        val api = PlayitApi(object : PlayitApi.HttpTransport {
            override fun post(path: String, body: JSONObject, headers: Map<String, String>): JSONObject {
                calls.add(path)
                return when (path) {
                    "/claim/setup" -> {
                        polls++
                        if (polls < 3) setupResp("WaitingForUser")
                        else setupResp("UserAccepted")
                    }
                    "/claim/exchange" -> approvedResp("SECRET123")
                    else -> JSONObject()
                }
            }
        })
        val events = mutableListOf<PlayitApi.ClaimEvent>()
        val secret = api.pollClaim("abcdef1234", tries = 10, intervalMs = 10, onEvent = events::add)
            .getOrThrow()
        assertEquals("SECRET123", secret)

        assertTrue(calls.indexOf("/claim/setup") < calls.indexOf("/claim/exchange"))
        assertTrue(events.any { it is PlayitApi.ClaimEvent.Setup })
        assertTrue(events.any { it is PlayitApi.ClaimEvent.Approved })
        assertTrue(events.any { it is PlayitApi.ClaimEvent.SecretReceived })
    }

    @Test
    fun rejectionFailsFast() = runTest {
        val api = PlayitApi(object : PlayitApi.HttpTransport {
            var n = 0
            override fun post(path: String, body: JSONObject, headers: Map<String, String>): JSONObject {
                n++
                return if (path == "/claim/setup" && n > 2) setupResp("UserRejected")
                else setupResp("WaitingForUser")
            }
        })
        val r = api.pollClaim("abcdef1234", tries = 10, intervalMs = 10)
        assertTrue(r.isFailure)
        assertTrue(r.exceptionOrNull()?.message?.contains("recusada") == true)
    }

    @Test
    fun setupErrorRetriesThenSucceeds() = runTest {
        var n = 0
        val api = PlayitApi(object : PlayitApi.HttpTransport {
            override fun post(path: String, body: JSONObject, headers: Map<String, String>): JSONObject {
                n++
                if (path == "/claim/setup" && n == 1) throw PlayitApi.ApiException(500, "boom")
                if (path == "/claim/setup") return setupResp("UserAccepted")
                return approvedResp("S")
            }
        })
        assertEquals("S", api.pollClaim("abcdef1234", tries = 5, intervalMs = 10).getOrThrow())
    }

    @Test
    fun setupSendsAgentTypeAndVersion() = runTest {
        var sent: JSONObject? = null
        val api = PlayitApi(object : PlayitApi.HttpTransport {
            override fun post(path: String, body: JSONObject, headers: Map<String, String>): JSONObject {
                if (path == "/claim/setup") sent = body
                return setupResp("WaitingForUserVisit")
            }
        })
        api.claimSetup("abcdef1234")
        assertEquals("abcdef1234", sent!!.getString("code"))
        assertEquals("self-managed", sent!!.getString("agent_type"))
        assertTrue(sent!!.getString("version").startsWith("playit "))
    }

    @Test
    fun unknownSetupStateFailsClearly() = runTest {
        val api = PlayitApi(object : PlayitApi.HttpTransport {
            override fun post(path: String, body: JSONObject, headers: Map<String, String>): JSONObject {
                return if (path == "/claim/setup") setupResp("AlgoNovo")
                else approvedResp("S")
            }
        })

        val r = api.pollClaim("abcdef1234", tries = 2, intervalMs = 10)
        assertTrue(r.isFailure)
    }

    @Test
    fun timeoutWithoutApproval() = runTest {
        val api = PlayitApi(object : PlayitApi.HttpTransport {
            override fun post(path: String, body: JSONObject, headers: Map<String, String>): JSONObject {
                return setupResp("WaitingForUser")
            }
        })
        val r = api.pollClaim("abcdef1234", tries = 3, intervalMs = 10)
        assertTrue(r.isFailure)
        assertTrue(r.exceptionOrNull()?.message?.contains("Tempo esgotado") == true)
    }
}
