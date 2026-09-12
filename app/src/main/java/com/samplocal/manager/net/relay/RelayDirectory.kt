package com.samplocal.manager.net.relay

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ProviderEntry(
    val id: String,
    val type: String,
    val host: String,
    val port: Int,
    val transport: String = "udp",
    val region: String = "—",
    val priority: Int = 100,
    val enabled: Boolean = true
)

data class InfraConfig(
    val relayHost: String,
    val relayPort: Int,
    val version: Int = 1,
    val turn: TurnRef? = null,
    val providers: List<ProviderEntry> = emptyList(),
    val credentials: Map<String, String> = emptyMap()
)

data class TurnRef(val host: String, val port: Int, val user: String, val pass: String)

class RelayDirectory(private val context: Context) {

    private val prefs = context.getSharedPreferences("infra", Context.MODE_PRIVATE)

    fun sourceUrl(): String? =
        prefs.getString("infra_url", null)?.takeIf { it.isNotBlank() }

    fun setSourceUrl(url: String) {
        prefs.edit().putString("infra_url", url.trim()).apply()
    }

    suspend fun fetch(): Result<InfraConfig> = withContext(Dispatchers.IO) {
        val url = sourceUrl()
            ?: return@withContext Result.failure(IllegalStateException("Fonte de infraestrutura não configurada"))
        try {
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            try {
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("Accept", "application/json")
                if (conn.responseCode !in 200..299) {
                    return@withContext Result.failure(
                        IllegalStateException("infra HTTP ${conn.responseCode}")
                    )
                }
                val body = conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
                Result.success(parse(body))
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private val FORBIDDEN = setOf("user", "pass", "password", "credential", "token", "secret")

        fun parse(body: String): InfraConfig {
            val o = JSONObject(body)
            if (o.optInt("version", 0) !in 1..2) throw IllegalArgumentException("versao de infra invalida")

            val legacyRelay = o.optJSONObject("relay")?.let {
                val host = it.optString("host", "")
                val port = it.optInt("port", 0)
                if (host.isBlank() || port !in 1..65535) null
                else InfraConfig(host, port, 1)
            }
            val turn = o.optJSONObject("turn")?.let { t ->
                val th = t.optString("host", "")
                val tu = t.optString("user", "")

                if (th.isBlank()) null
                else TurnRef(th, t.optInt("port", 3478), tu, t.optString("pass", ""))
            }
            val providers = mutableListOf<ProviderEntry>()
            o.optJSONArray("providers")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val p = arr.optJSONObject(i) ?: continue
                    for (k in FORBIDDEN) {
                        if (p.has(k)) throw IllegalArgumentException(
                            "segredo '$k' em providers[], use credentials{}"
                        )
                    }
                    val id = p.optString("id", "")
                    val host = p.optString("host", "")
                    val port = p.optInt("port", 0)
                    if (id.isBlank() || host.isBlank() || port !in 1..65535) continue
                    providers.add(
                        ProviderEntry(
                            id = id,
                            type = p.optString("type", "OWN"),
                            host = host,
                            port = port,
                            transport = p.optString("transport", "udp"),
                            region = p.optString("region", "—"),
                            priority = p.optInt("priority", 100),
                            enabled = p.optBoolean("enabled", true)
                        )
                    )
                }
            }
            val creds = mutableMapOf<String, String>()
            o.optJSONObject("credentials")?.let { c ->
                for (k in c.keys()) creds[k] = c.optString(k, "")
            }

            if (legacyRelay != null && providers.none { it.type == "OWN" }) {
                providers.add(
                    0, ProviderEntry("own", "OWN", legacyRelay.relayHost, legacyRelay.relayPort,
                        "udp", "local", 10, true)
                )
            }
            return InfraConfig(
                relayHost = providers.firstOrNull { it.type == "OWN" }?.host ?: legacyRelay?.relayHost ?: "",
                relayPort = providers.firstOrNull { it.type == "OWN" }?.port ?: legacyRelay?.relayPort ?: 0,
                version = o.optInt("version", 1),
                turn = turn,
                providers = providers,
                credentials = creds
            )
        }
    }
}
