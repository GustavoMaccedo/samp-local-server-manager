package com.samplocal.manager.net

import android.content.Context
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class SignalingSession(context: Context) {

    private val prefs = context.getSharedPreferences("signaling", Context.MODE_PRIVATE)

    val sessionId: String =
        prefs.getString("session_id", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("session_id", it).apply()
        }

    private val secret: ByteArray by lazy {
        val hex = prefs.getString("secret", null)
        if (hex != null) hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        else ByteArray(32).also {
            SecureRandom().nextBytes(it)
            prefs.edit().putString("secret", it.joinToString("") { b -> "%02x".format(b) }).apply()
        }
    }

    fun token(): String = hmac(sessionId.toByteArray())

    fun buildOffer(candidates: List<IceCandidate>, sampPort: Int): String =
        JSONObject()
            .put("v", 1)
            .put("session", sessionId)
            .put("port", sampPort)
            .put("candidates", org.json.JSONArray(candidates.map { it.toJson() }))
            .put("auth", token())
            .toString()

    fun parseAnswer(json: String): Result<List<IceCandidate>> {
        return try {
            val o = JSONObject(json)
            if (o.optInt("v", 0) != 1) throw IllegalArgumentException("versao de oferta invalida")
            if (o.optString("session", "") != sessionId) {
                throw IllegalArgumentException("sessao divergente (resposta de outra publicacao?)")
            }
            val arr = o.optJSONArray("candidates") ?: throw IllegalArgumentException("sem candidatos")
            Result.success((0 until arr.length()).map { IceCandidate.fromJson(arr.getJSONObject(it)) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun hmac(data: ByteArray): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret, "HmacSHA256"))
        return mac.doFinal(data).joinToString("") { "%02x".format(it) }
    }

    companion object {
        fun md5key(user: String, realm: String, pass: String): ByteArray {
            val md = MessageDigest.getInstance("MD5")
            return md.digest("$user:$realm:$pass".toByteArray(Charsets.UTF_8))
        }
    }
}
