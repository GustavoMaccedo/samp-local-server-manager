package com.samplocal.manager.net.relay

import com.samplocal.manager.samp.ServerQueryManager

class DirectProvider(
    private val query: ServerQueryManager = ServerQueryManager(),
    private val localIp: String,
    private val publicIp: String,
    private val publicPort: Int,
    private val sampPort: Int
) : RelayProvider {
    override val id = "direct"
    override val kind = ProviderKind.DIRECT
    override val region = "local"
    override val priority = 0
    override val transport = "udp"
    override val supportsVanillaSamp = true

    override suspend fun healthCheck(): ProviderHealth {
        return try {
            val info = query.queryInfo("127.0.0.1", sampPort, 2000).getOrNull()
                ?: return ProviderHealth(false, null, "servidor nao responde local")
            val ping = query.ping("127.0.0.1", sampPort, 2000).getOrNull()
            ProviderHealth(true, ping, "direto: ${info.players} players, ping ${ping ?: "?"}ms")
        } catch (e: Exception) {
            ProviderHealth(false, null, e.message ?: "falha direta")
        }
    }

    override suspend fun connect(params: ConnectParams): Result<ProviderSession> {
        val h = healthCheck()
        if (!h.ok) return Result.failure(IllegalStateException(h.detail))
        return Result.success(
            DirectSession(publicIp, publicPort.ifZero(sampPort), h.latencyMs)
        )
    }

    private fun Int.ifZero(fb: Int) = if (this == 0) fb else this

    override fun close() { }

    private class DirectSession(ip: String, port: Int, rtt: Long?) : ProviderSession {
        override val providerId = "direct"
        override val endpoint = ProviderEndpoint(ip, port)
        private var rtt = rtt
        override suspend fun heartbeat(): Boolean = true
        override fun stats(): SessionStats = SessionStats(rttMs = rtt)
        override fun close() { }
    }
}
