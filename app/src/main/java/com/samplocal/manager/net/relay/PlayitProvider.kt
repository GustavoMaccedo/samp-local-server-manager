package com.samplocal.manager.net.relay

import android.content.Context
import com.samplocal.manager.net.playit.PlayitAgent
import com.samplocal.manager.net.playit.PlayitApi
import com.samplocal.manager.samp.ServerQueryManager
import com.samplocal.manager.util.KeystoreStore
import com.samplocal.manager.util.SecretStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.security.SecureRandom

class PlayitProvider(
    private val agent: PlayitAgent,
    private val api: PlayitApi = PlayitApi(),
    private val query: ServerQueryManager = ServerQueryManager(),
    private val store: SecretStore
) : RelayProvider {

    constructor(context: Context) : this(
        PlayitAgent(context), PlayitApi(), ServerQueryManager(), KeystoreStore(context)
    )

    override val id = "playit"
    override val kind = ProviderKind.PLAYIT
    override val region = "auto"
    override val priority = 5
    override val transport = "udp"
    override val supportsVanillaSamp = true

    private var active: PlayitSession? = null

    fun agent(): PlayitAgent = agent

    fun playitApi(): PlayitApi = api

    fun isLinked(): Boolean = try {
        !loadSecret().isNullOrBlank()
    } catch (_: Exception) { false }

    fun clearLink() {
        try { store.remove(KEY) } catch (_: Exception) { }
        close()
    }

    fun saveSecret(secret: String) {
        require(secret.isNotBlank())
        store.save(KEY, secret.trim().toByteArray(Charsets.UTF_8))
    }

    fun loadSecret(): String? = try {
        store.load(KEY)?.toString(Charsets.UTF_8)?.takeIf { it.isNotBlank() }
    } catch (_: Exception) { null }

    fun newClaimCode(): String {
        val b = ByteArray(5)
        SecureRandom().nextBytes(b)
        return b.joinToString("") { "%02x".format(it) }
    }

    fun claimUrl(code: String): String = "https://playit.gg/claim/$code"

    suspend fun pollClaim(
        code: String,
        tries: Int = 60,
        intervalMs: Long = 2000,
        onEvent: (PlayitApi.ClaimEvent) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        val res = api.pollClaim(code, tries, intervalMs, onEvent)
        val secret = res.getOrNull()
        if (secret != null) saveSecret(secret)
        res
    }

    override suspend fun healthCheck(): ProviderHealth = withContext(Dispatchers.IO) {
        if (!isLinked()) return@withContext ProviderHealth(false, null, "conta não vinculada")
        val chk = agent.checkBinary()
        if (chk.isFailure) return@withContext ProviderHealth(false, null, chk.exceptionOrNull()?.message ?: "binário inválido")
        ProviderHealth(true, null, "agent ok, conta vinculada")
    }

    override suspend fun connect(params: ConnectParams): Result<ProviderSession> =
        withContext(Dispatchers.IO) {
            try {
                close()
                val secret = loadSecret()
                    ?: return@withContext Result.failure(IllegalStateException("PLAYIT_NOT_LINKED"))
                val chk = agent.checkBinary()
                if (chk.isFailure) {
                    return@withContext Result.failure(
                        IllegalStateException("PLAYIT_AGENT_START_FAILED: ${chk.exceptionOrNull()?.message}")
                    )
                }
                val started = agent.start(secret)
                if (started.isFailure || !agent.isAlive()) {
                    return@withContext Result.failure(
                        IllegalStateException("PLAYIT_AGENT_START_FAILED")
                    )
                }

                delay(4000)
                if (!agent.isAlive()) {
                    return@withContext Result.failure(IllegalStateException("PLAYIT_AUTH_FAILED"))
                }

                val tunnelRes = ensureTunnel(secret, params)
                val tunnel = tunnelRes.getOrNull()
                    ?: return@withContext Result.failure(
                        IllegalStateException(
                            tunnelRes.exceptionOrNull()?.message
                                ?: "PLAYIT_TUNNEL_CREATE_FAILED"
                        )
                    )
                val ep = tunnel.endpoint
                    ?: return@withContext Result.failure(
                        IllegalStateException("PLAYIT_ENDPOINT_UNAVAILABLE")
                    )
                val session = PlayitSession(params.serverId, ep.first, ep.second, tunnel.region,
                    RelayProtocol.newSessionId(), alive = { agent.isAlive() })
                active = session
                Result.success(session)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun validate(params: ConnectParams, endpoint: ProviderEndpoint): ValidateLevel =
        withContext(Dispatchers.IO) {
            if (!agent.isAlive()) return@withContext ValidateLevel.AGENT_DOWN
            val secret = loadSecret() ?: return@withContext ValidateLevel.AGENT_DOWN
            val tunnels = api.tunnelsList(secret).getOrNull() ?: return@withContext ValidateLevel.AGENT_UP
            val t = tunnels.firstOrNull { it.name == tunnelName(params.serverId) && it.active }
                ?: tunnels.filter { it.portType.lowercase().contains("udp") && it.active }
                    .singleOrNull()
                    ?.takeIf { originMatches(it, params.sampPort) }
                ?: return@withContext ValidateLevel.AGENT_UP
            if (t.endpoint == null) return@withContext ValidateLevel.TUNNEL_UP
            val q = query.queryInfo("127.0.0.1", params.sampPort, 2000)
            if (q.isFailure) return@withContext ValidateLevel.TUNNEL_UP
            ValidateLevel.SAMP_REACHABLE
        }

    private suspend fun ensureTunnel(secret: String, params: ConnectParams): Result<PlayitApi.PlayitTunnel> {

        var lastErr: Exception? = null
        repeat(2) { attempt ->
            try {
                val list = api.tunnelsList(secret).getOrThrow()
                return foundOrCreate(secret, params, list)
            } catch (e: Exception) {
                lastErr = e
                if (isAuthError(e)) {
                    return Result.failure(
                        IllegalStateException(
                            "Playit rejeitou a credencial (HTTP 401). " +
                                "Desvincule e vincule de novo em Conectividade > PLAYIT."
                        )
                    )
                }
                delay(2000)
            }
        }
        val detail = (lastErr?.message ?: "falha de rede").take(160)
        return Result.failure(IllegalStateException("falha ao listar tunnels: $detail"))
    }

    private fun isAuthError(e: Exception): Boolean =
        e is PlayitApi.ApiException && e.code == 401 ||
            "401" in (e.message.orEmpty())

    private suspend fun foundOrCreate(
        secret: String,
        params: ConnectParams,
        list: List<PlayitApi.PlayitTunnel>
    ): Result<PlayitApi.PlayitTunnel> {
        val name = tunnelName(params.serverId)
        list.firstOrNull {
            it.name == name && it.portType.lowercase().contains("udp")
        }?.let { return Result.success(it) }

        list.filter { it.portType.lowercase().contains("udp") && it.active }
            .singleOrNull()
            ?.takeIf { originMatches(it, params.sampPort) }
            ?.let { return Result.success(it) }

        val wantIp = "127.0.0.1"
        val created = api.tunnelsCreate(secret, name, wantIp, params.sampPort)
        if (created.isFailure) {
            val msg = created.exceptionOrNull()?.message.orEmpty()
            return if ("401" in msg) Result.failure(
                IllegalStateException(
                    "Playit recusou criar o tunnel (HTTP 401). Verifique o email da " +
                        "conta Playit ou crie um tunnel UDP manual no dashboard apontando " +
                        "127.0.0.1:${params.sampPort} que o app adota sozinho."
                )
            ) else Result.failure(
                IllegalStateException("falha ao criar tunnel: ${msg.take(120)}")
            )
        }
        delay(3000)

        return api.tunnelsList(secret).getOrNull()?.firstOrNull {
            it.name == name && it.portType.lowercase().contains("udp")
        }?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("tunnel criado mas não localizado"))
    }

    override fun close() {
        try { active?.close() } catch (_: Exception) { }
        active = null

    }

    suspend fun awaitAuthenticated(timeoutMs: Long = 30000): Boolean =
        withContext(Dispatchers.IO) {
            val secret = loadSecret() ?: return@withContext false
            val end = System.currentTimeMillis() + timeoutMs
            while (System.currentTimeMillis() < end) {
                try {
                    val r = api.agentsRundata(secret).getOrNull()
                    if (r != null && r.optString("status", "") == "success") return@withContext true
                } catch (_: Exception) { }
                delay(2000)
            }
            false
        }

    fun stopAgent() {
        try { agent.stop() } catch (_: Exception) { }
    }

    fun agentAlive(): Boolean = agent.isAlive()

    enum class ValidateLevel { AGENT_DOWN, AGENT_UP, TUNNEL_UP, SAMP_REACHABLE }

    class PlayitSession(
        serverId: String,
        ip: String,
        port: Int,
        val region: String,
        val sid: ByteArray,
        private val alive: () -> Boolean = { true }
    ) : ProviderSession {
        override val providerId = "playit"
        override val endpoint = ProviderEndpoint(ip, port)
        override suspend fun heartbeat(): Boolean = withContext(Dispatchers.IO) {
            try { alive() } catch (_: Exception) { false }
        }
        override fun stats(): SessionStats = SessionStats()
        override fun close() { }
    }

    companion object {
        const val KEY = "playit:agent-secret"
        fun tunnelName(serverId: String): String = "samp-local-$serverId".take(48)

        fun originMatches(t: PlayitApi.PlayitTunnel, sampPort: Int): Boolean {
            return try {
                val origin = t.raw.optJSONObject("origin") ?: return false
                val data = origin.optJSONObject("data") ?: return false
                val ip = data.optString("local_ip", "")
                val port = data.optInt("local_port", -1)
                if (ip.isBlank() || port < 0) return false
                (ip == "127.0.0.1" || ip == "localhost") && port == sampPort
            } catch (_: Exception) {
                false
            }
        }
    }
}
