package com.samplocal.manager.net

enum class ConnectivityMode { LOCAL, LAN, INTERNET }

enum class NatState { DIRECT, NAT, UNKNOWN, UNREACHABLE }

enum class PublishMethod { LOCAL, DIRECT, PLAYIT, RELAY, NONE }

enum class LinkState { OFFLINE, CHECKING, CONNECTED, DEGRADED, RECONNECTING, RELAYING }

enum class RelayState { NOT_CONFIGURED, STANDBY, ACTIVE, FAILED }

data class RelayInfo(
    val state: RelayState = RelayState.NOT_CONFIGURED,
    val endpoint: String? = null,
    val latencyMs: Long? = null,
    val lossPct: Float? = null,
    val jitterMs: Float? = null,
    val sessionStartMs: Long = 0L,
    val bytesUp: Long = 0L,
    val bytesDown: Long = 0L,
    val pktsUp: Long = 0L,
    val pktsDown: Long = 0L
)

data class PublishState(
    val mode: ConnectivityMode = ConnectivityMode.LOCAL,
    val linkState: LinkState = LinkState.OFFLINE,
    val method: PublishMethod = PublishMethod.NONE,
    val localIp: String? = null,
    val networkType: String? = null,
    val iface: String? = null,
    val gateway: String? = null,
    val ipv6: String? = null,
    val sampPort: Int = 7777,
    val publicIp: String? = null,
    val publicPort: Int? = null,
    val nat: NatState = NatState.UNKNOWN,
    val natDetail: String = "Não verificado",
    val directAvailable: Boolean? = null,
    val relay: RelayInfo = RelayInfo(),
    val players: Int? = null,
    val error: String? = null,
    val lastCheckMs: Long = 0L,
    val stage: ConnStage = ConnStage.DISCONNECTED,
    val traversal: String? = null,
    val sampPingMs: Long? = null,
    val infraSource: String? = null,
    val turnStatus: String? = null,
    val provider: String? = null,
    val transport: String? = null,
    val region: String? = null
) {
    fun playerEndpoint(): String? = when (method) {
        PublishMethod.DIRECT -> if (publicIp != null && publicPort != null) "$publicIp:$publicPort" else null
        PublishMethod.PLAYIT -> if (publicIp != null && publicPort != null) "$publicIp:$publicPort" else null
        PublishMethod.RELAY -> relay.endpoint
        PublishMethod.LOCAL -> if (localIp != null) "$localIp:$sampPort" else "127.0.0.1:$sampPort"
        PublishMethod.NONE -> null
    }
}
