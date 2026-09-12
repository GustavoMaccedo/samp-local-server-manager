package com.samplocal.manager.net.relay

import android.content.Context
import com.samplocal.manager.util.KeystoreStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramSocket

class OwnUdpRelayProvider(
    private val appScope: CoroutineScope,
    context: Context,
    private val entry: ProviderEntry
) : RelayProvider {
    override val id = entry.id
    override val kind = ProviderKind.OWN_UDP
    override val region = entry.region
    override val priority = entry.priority
    override val transport = entry.transport
    override val supportsVanillaSamp = true

    private val keystore by lazy { KeystoreStore(context) }
    private var active: OwnSession? = null

    private fun tokenFor(serverId: String): ByteArray {
        return try {
            keystore.load("relay:$serverId") ?: RelayProtocol.newToken().also {
                keystore.save("relay:$serverId", it)
            }
        } catch (_: Exception) {
            RelayProtocol.newToken()
        }
    }

    override suspend fun healthCheck(): ProviderHealth = withContext(Dispatchers.IO) {

        val sid = RelayProtocol.newSessionId()
        val token = RelayProtocol.newToken()
        val sm = RelaySessionManager(entry.host, entry.port, sid, token, 7777)
        try {
            val reg = sm.register(6000)
            if (reg.isFailure) {
                return@withContext ProviderHealth(false, null, "registro: ${reg.exceptionOrNull()?.message}")
            }
            var ok = 0
            var rtt: Long? = null
            val t0 = System.currentTimeMillis()
            repeat(2) {
                val a = System.currentTimeMillis()
                if (sm.pingOnce(4000)) {
                    ok++
                    if (rtt == null) rtt = System.currentTimeMillis() - a
                }
            }
            sm.close()
            if (ok == 0) ProviderHealth(false, null, "sem resposta ao ping")
            else ProviderHealth(true, rtt ?: (System.currentTimeMillis() - t0), "registro+ping OK")
        } catch (e: Exception) {
            try { sm.close() } catch (_: Exception) { }
            ProviderHealth(false, null, e.message ?: "falha")
        }
    }

    override suspend fun connect(params: ConnectParams): Result<ProviderSession> =
        withContext(Dispatchers.IO) {
            try {
                close()
                val token = tokenFor(params.serverId)
                val sid = RelayProtocol.newSessionId()
                val sm = RelaySessionManager(entry.host, entry.port, sid, token, params.sampPort)
                val reg = sm.register()
                if (reg.isFailure) {
                    sm.close()
                    return@withContext Result.failure(
                        reg.exceptionOrNull() ?: IllegalStateException("registro falhou")
                    )
                }
                val bridge = UdpBridge(entry.host, entry.port, token, sid, params.sampPort)
                sm.socket()?.let { bridge.start(appScope, it) }
                sm.startHeartbeat(appScope)
                val session = OwnSession(id, entry, sm, bridge, token, sid)
                active = session
                Result.success(session)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override fun close() {
        active?.close()
        active = null
    }

    class OwnSession(
        override val providerId: String,
        entry: ProviderEntry,
        val manager: RelaySessionManager,
        val bridge: UdpBridge,
        val token: ByteArray,
        val sessionId: ByteArray
    ) : ProviderSession {
        override val endpoint: ProviderEndpoint =
            ProviderEndpoint(entry.host, manager.sessionPort)
        override suspend fun heartbeat(): Boolean = withContext(Dispatchers.IO) {
            manager.pingOnce()
        }
        override fun stats(): SessionStats {
            val hb = manager.stats
            return SessionStats(
                rttMs = hb.lastRttMs, lossPct = hb.lossPct, jitterMs = hb.jitterMs,
                bytesUp = bridge.bytesUp.get(), bytesDown = bridge.bytesDown.get(),
                pktsUp = bridge.pktsUp.get(), pktsDown = bridge.pktsDown.get(),
                startedMs = hb.lastHeartbeatMs
            )
        }
        override fun close() {
            try { manager.close() } catch (_: Exception) { }
            bridge.stop()
        }
    }
}
