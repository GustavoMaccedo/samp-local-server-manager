package com.samplocal.manager.net.relay

interface RelayProvider {
    val id: String
    val kind: ProviderKind
    val region: String
    val priority: Int
    val transport: String

    val supportsVanillaSamp: Boolean

    suspend fun healthCheck(): ProviderHealth
    suspend fun connect(params: ConnectParams): Result<ProviderSession>
    fun close()
}

enum class ProviderKind { DIRECT, OWN_UDP, TURN, PLAYIT }

data class ConnectParams(val serverId: String, val sampPort: Int)

data class ProviderHealth(
    val ok: Boolean,
    val latencyMs: Long?,
    val detail: String
)

data class ProviderEndpoint(val ip: String, val port: Int) {
    override fun toString(): String = "$ip:$port"
}

interface ProviderSession {
    val providerId: String
    val endpoint: ProviderEndpoint
    suspend fun heartbeat(): Boolean
    fun stats(): SessionStats
    fun close()
}

data class SessionStats(
    val rttMs: Long? = null,
    val lossPct: Float? = null,
    val jitterMs: Float? = null,
    val bytesUp: Long = 0L,
    val bytesDown: Long = 0L,
    val pktsUp: Long = 0L,
    val pktsDown: Long = 0L,
    val startedMs: Long = System.currentTimeMillis()
)
