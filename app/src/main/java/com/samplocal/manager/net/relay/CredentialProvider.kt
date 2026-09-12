package com.samplocal.manager.net.relay

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

data class TempCredentials(val username: String, val password: String, val ttlSec: Int)

class CredentialProvider(
    private val context: Context,
    private val store: com.samplocal.manager.util.KeystoreStore = com.samplocal.manager.util.KeystoreStore(context)
) {
    suspend fun fetch(providerId: String, url: String): Result<TempCredentials> =
        withContext(Dispatchers.IO) {
            try {
                cached(providerId)?.let { return@withContext Result.success(it) }
                val conn = URL(url).openConnection() as java.net.HttpURLConnection
                try {
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000
                    conn.setRequestProperty("Accept", "application/json")
                    if (conn.responseCode !in 200..299) {
                        return@withContext Result.failure(
                            IllegalStateException("credenciais HTTP ${conn.responseCode}")
                        )
                    }
                    val body = conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
                    val creds = parse(body)
                    cache(providerId, creds)
                    Result.success(creds)
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun cacheKey(id: String) = "turncreds:$id"

    private fun cached(id: String): TempCredentials? {
        return try {
            val raw = store.load(cacheKey(id)) ?: return null
            val o = JSONObject(String(raw, Charsets.UTF_8))
            if (o.optLong("exp", 0) < System.currentTimeMillis()) {
                store.remove(cacheKey(id))
                return null
            }
            TempCredentials(o.getString("u"), o.getString("p"), o.optInt("ttl", 0))
        } catch (_: Exception) { null }
    }

    private fun cache(id: String, c: TempCredentials) {
        try {

            val exp = System.currentTimeMillis() + (c.ttlSec.coerceAtLeast(120) - 60) * 1000L
            val o = JSONObject().put("u", c.username).put("p", c.password)
                .put("ttl", c.ttlSec).put("exp", exp)
            store.save(cacheKey(id), o.toString().toByteArray(Charsets.UTF_8))
        } catch (_: Exception) { }
    }

    companion object {

        fun parse(body: String): TempCredentials {
            val o = JSONObject(body)

            o.optJSONArray("ice_servers")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val s = arr.optJSONObject(i) ?: continue
                    val urls = s.optString("urls", "")
                    if ("turn:" in urls && "transport=udp" in urls) {
                        val u = s.optString("username", "")
                        val p = s.optString("credential", s.optString("password", ""))
                        if (u.isNotBlank() && p.isNotBlank()) {
                            return TempCredentials(u, p, o.optInt("ttl", 86400))
                        }
                    }
                }
            }
            val u = o.optString("username", "")
            val p = o.optString("password", o.optString("credential", ""))
            require(u.isNotBlank() && p.isNotBlank()) { "credenciais temporarias ausentes" }
            return TempCredentials(u, p, o.optInt("ttl", 3600))
        }
    }
}
