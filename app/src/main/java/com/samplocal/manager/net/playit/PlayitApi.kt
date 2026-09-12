package com.samplocal.manager.net.playit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

const val AGENT_VERSION = "playit 1.0.10"

class PlayitApi(
    private val transport: HttpTransport = RealHttpTransport,
    private val baseUrl: String = "https://api.playit.gg"
) {
    interface HttpTransport {
        fun post(path: String, body: JSONObject, headers: Map<String, String> = emptyMap()): JSONObject
    }

    object RealHttpTransport : HttpTransport {
        override fun post(path: String, body: JSONObject, headers: Map<String, String>): JSONObject {
            val conn = URL("https://api.playit.gg" + path).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Accept", "application/json")
                headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.use { it.readBytes().toString(Charsets.UTF_8) } ?: "{}"
                if (code !in 200..299) throw ApiException(code, text.take(500))
                return JSONObject(text)
            } finally {
                conn.disconnect()
            }
        }
    }

    class ApiException(val code: Int, message: String) : Exception("HTTP $code: $message")

    companion object {

        fun redact(raw: String): String {
            var s = raw
            for (key in listOf("secret", "agent_key", "api_key", "password", "token", "credential")) {
                s = s.replace(
                    Regex("(\"$key[^\\\"]*\"\\s*:\\s*\")[^\"]+(\")", RegexOption.IGNORE_CASE),
                    "$1***$2"
                )
            }
            return s.take(800)
        }
    }

    private fun authHeaders(agentKey: String? = null, apiKey: String? = null): Map<String, String> {

        val h = mutableMapOf<String, String>()
        if (agentKey != null) h["Agent-Key"] = agentKey
        if (apiKey != null) h["Authorization"] = "ApiKey $apiKey"
        return h
    }

    suspend fun claimSetup(
        code: String,
        agentType: String = "self-managed",
        version: String = AGENT_VERSION
    ): Result<SetupState> = withContext(Dispatchers.IO) {
        try {
            val r = transport.post(
                "/claim/setup",
                JSONObject()
                    .put("code", code)
                    .put("agent_type", agentType)
                    .put("version", version)
            )
            if (r.optString("status", "") != "success") {
                return@withContext Result.failure(
                    IllegalStateException("setup: ${r.opt("data")}")
                )
            }
            val state = SetupState.of(r.optString("data", ""))
                ?: return@withContext Result.failure(
                    IllegalStateException("setup: resposta desconhecida ${r.opt("data")}")
                )
            Result.success(state)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pollClaim(
        code: String,
        tries: Int = 60,
        intervalMs: Long = 2000,
        onEvent: (ClaimEvent) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            onEvent(ClaimEvent.Setup)
            repeat(tries) {
                val setup = claimSetup(code).getOrNull()
                if (setup == null) {
                    onEvent(ClaimEvent.SetupRetry)
                } else when (setup) {
                    SetupState.USER_ACCEPTED -> {
                        onEvent(ClaimEvent.Approved)
                        val ex = claimExchange(code)
                        val v = ex.getOrNull()
                        if (v is ClaimResult.Approved) {
                            onEvent(ClaimEvent.SecretReceived)
                            return@withContext Result.success(v.secret)
                        }
                        onEvent(ClaimEvent.Poll(describeExchange(ex)))
                    }
                    SetupState.USER_REJECTED ->
                        return@withContext Result.failure(
                            IllegalStateException("Aprovação recusada no navegador")
                        )
                    SetupState.WAITING_FOR_USER_VISIT, SetupState.WAITING_FOR_USER ->
                        onEvent(ClaimEvent.Poll(setup.name))
                }
                delay(intervalMs)
            }
            Result.failure(IllegalStateException("Tempo esgotado aguardando aprovação"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun describeExchange(ex: Result<ClaimResult>): String {
        val v = ex.getOrNull()
        if (v is ClaimResult.Pending) return "exchange pendente (${v.detail.take(60)})"
        return "exchange: ${ex.exceptionOrNull()?.message?.take(80) ?: "?"}"
    }

    enum class SetupState {
        WAITING_FOR_USER_VISIT, WAITING_FOR_USER, USER_ACCEPTED, USER_REJECTED;

        companion object {
            fun of(raw: String): SetupState? = when (raw.trim()) {
                "WaitingForUserVisit" -> WAITING_FOR_USER_VISIT
                "WaitingForUser" -> WAITING_FOR_USER
                "UserAccepted" -> USER_ACCEPTED
                "UserRejected" -> USER_REJECTED
                else -> null
            }
        }
    }

    sealed interface ClaimEvent {
        data object Setup : ClaimEvent
        data object SetupRetry : ClaimEvent
        data class Poll(val status: String) : ClaimEvent
        data object Approved : ClaimEvent
        data object SecretReceived : ClaimEvent
    }

    suspend fun claimExchange(code: String): Result<ClaimResult> = withContext(Dispatchers.IO) {
        try {
            val r = transport.post("/claim/exchange", JSONObject().put("code", code))
            val status = r.optString("status", "")
            if (status == "success") {
                val data = r.optJSONObject("data") ?: JSONObject()
                val secret = data.optString("secret_key",
                    data.optString("secret", data.optString("key", "")))
                if (secret.isBlank()) {
                    return@withContext Result.failure(IllegalStateException("resposta sem secret"))
                }
                Result.success(ClaimResult.Approved(secret))
            } else {
                Result.success(ClaimResult.Pending(r.opt("data")?.toString() ?: status))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun tunnelsList(agentKey: String): Result<List<PlayitTunnel>> = withContext(Dispatchers.IO) {
        try {
            val r = transport.post("/tunnels/list", JSONObject(), authHeaders(agentKey = agentKey))
            Result.success(parseTunnelList(r))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun tunnelsCreate(
        agentKey: String,
        name: String,
        localIp: String,
        localPort: Int,
        agentId: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val origin = if (agentId != null) {
                JSONObject()
                    .put("type", "agent")
                    .put("data", JSONObject()
                        .put("agent_id", agentId)
                        .put("local_ip", localIp)
                        .put("local_port", localPort))
            } else {
                JSONObject()
                    .put("type", "default")
                    .put("data", JSONObject()
                        .put("local_ip", localIp)
                        .put("local_port", localPort))
            }
            val body = JSONObject()
                .put("name", name)
                .put("port_type", "udp")
                .put("port_count", 1)
                .put("origin", origin)
                .put("enabled", true)
            val r = transport.post("/tunnels/create", body, authHeaders(agentKey = agentKey))
            val status = r.optString("status", "")
            if (status != "success") {
                return@withContext Result.failure(
                    IllegalStateException("create falhou: ${r.opt("data")}")
                )
            }
            val data = r.optJSONObject("data") ?: JSONObject()
            val id = data.optString("id", data.optString("tunnel_id", ""))
            if (id.isBlank()) return@withContext Result.failure(IllegalStateException("sem id"))
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun tunnelsDelete(agentKey: String, tunnelId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                transport.post("/tunnels/delete", JSONObject().put("tunnel_id", tunnelId),
                    authHeaders(agentKey = agentKey))
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun agentsRundata(agentKey: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        runCatching { transport.post("/agents/rundata", JSONObject(), authHeaders(agentKey = agentKey)) }
    }

    fun extractEndpoint(tunnel: JSONObject): Pair<String, Int>? {
        fun obj(vararg keys: String): JSONObject? {
            var cur: JSONObject? = tunnel
            for (k in keys) cur = cur?.optJSONObject(k) ?: return null
            return cur
        }

        val alloc = tunnel.optJSONObject("alloc")
        val candidates = listOf(
            alloc?.optJSONObject("port_alloc"),
            alloc?.optJSONObject("dedicated_ip"),
            tunnel.optJSONObject("allocation"),
            tunnel
        ).filterNotNull()
        for (c in candidates) {
            val ip = c.optString("ip", c.optString("hostname", c.optString("address", "")))
            val port = c.optInt("port", c.optInt("public_port", 0))
            if (ip.isNotBlank() && port in 1..65535) return ip to port
        }
        val addrs = tunnel.optJSONArray("connect_addresses")
        if (addrs != null) {
            for (i in 0 until addrs.length()) {
                val a = addrs.optJSONObject(i) ?: continue
                val ip = a.optString("ip", a.optString("hostname", ""))
                val port = a.optInt("port", 0)
                if (ip.isNotBlank() && port in 1..65535) return ip to port
            }
        }
        return null
    }

    private fun parseTunnelList(r: JSONObject): List<PlayitTunnel> {
        val out = mutableListOf<PlayitTunnel>()
        val data = r.optJSONObject("data") ?: return out
        val arr = data.optJSONArray("tunnels") ?: data.optJSONArray("items") ?: return out
        for (i in 0 until arr.length()) {
            val t = arr.optJSONObject(i) ?: continue
            out.add(
                PlayitTunnel(
                    id = t.optString("id", ""),
                    name = t.optString("name", ""),
                    portType = t.optString("port_type", ""),
                    active = t.optBoolean("active", true),
                    endpoint = extractEndpoint(t),
                    region = t.optString("region", ""),
                    raw = t
                )
            )
        }
        return out
    }

    sealed interface ClaimResult {
        data class Approved(val secret: String) : ClaimResult
        data class Pending(val detail: String) : ClaimResult
    }

    data class PlayitTunnel(
        val id: String,
        val name: String,
        val portType: String,
        val active: Boolean,
        val endpoint: Pair<String, Int>?,
        val region: String,
        val raw: JSONObject = JSONObject()
    )
}
