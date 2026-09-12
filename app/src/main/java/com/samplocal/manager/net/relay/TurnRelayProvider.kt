package com.samplocal.manager.net.relay

import com.samplocal.manager.net.turn.TurnClient
import com.samplocal.manager.net.turn.TurnConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TurnRelayProvider(
    private val entry: ProviderEntry,
    private val credentialsUrl: String?,
    private val credentialProvider: CredentialProvider,
    private val turn: TurnClient = TurnClient()
) : RelayProvider {
    override val id = entry.id
    override val kind = ProviderKind.TURN
    override val region = entry.region
    override val priority = entry.priority
    override val transport = entry.transport

    override val supportsVanillaSamp = false
    val vanillaReason =
        "TURN exige permissão por IP de jogador (RFC 8656); clientes vanilla não sinalizam"

    private var allocation: com.samplocal.manager.net.turn.TurnAllocation? = null
    private var lastLatency: Long? = null

    private suspend fun creds(): Result<TempCredentials> {
        if (credentialsUrl.isNullOrBlank()) {
            return Result.failure(IllegalStateException("sem URL de credenciais temporárias"))
        }
        return credentialProvider.fetch(entry.id, credentialsUrl)
    }

    override suspend fun healthCheck(): ProviderHealth = withContext(Dispatchers.IO) {
        val c = creds().getOrNull()
            ?: return@withContext ProviderHealth(false, null, "sem credenciais temporárias")
        try {
            val t0 = System.currentTimeMillis()
            val alloc = turn.allocate(TurnConfig(entry.host, entry.port, c.username, c.password))
                .getOrNull()
            if (alloc == null) {
                ProviderHealth(false, null, "allocate falhou")
            } else {
                lastLatency = System.currentTimeMillis() - t0

                ProviderHealth(true, lastLatency, "allocate OK ${alloc.relayedIp.hostAddress}:${alloc.relayedPort}")
            }
        } catch (e: Exception) {
            ProviderHealth(false, null, e.message ?: "falha")
        }
    }

    suspend fun trafficSelfTest(peerIp: String, peerPort: Int): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val c = creds().getOrNull()
                    ?: return@withContext Result.failure(IllegalStateException("sem credenciais"))
                val alloc = turn.allocate(TurnConfig(entry.host, entry.port, c.username, c.password))
                    .getOrThrow()
                Result.success(
                    "encanamento OK via ${alloc.relayedIp.hostAddress}:${alloc.relayedPort} " +
                        "(peer autorizado $peerIp:$peerPort; vanilla segue incompatível)"
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun connect(params: ConnectParams): Result<ProviderSession> =
        withContext(Dispatchers.IO) {

            Result.failure(IllegalStateException("$vanillaReason (provider ${entry.id})"))
        }

    override fun close() {
        allocation = null
    }
}
