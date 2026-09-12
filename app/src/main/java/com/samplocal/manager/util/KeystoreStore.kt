package com.samplocal.manager.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeystoreStore(context: Context) : SecretStore {

    private val prefs = context.getSharedPreferences("secrets", Context.MODE_PRIVATE)
    private val alias = "samp-relay-v1"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return gen.generateKey()
    }

    override fun save(name: String, raw: ByteArray) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = cipher.iv
        val enc = cipher.doFinal(raw)
        val hex = (iv + enc).joinToString("") { "%02x".format(it) }
        prefs.edit().putString("k:$name", hex).apply()
    }

    override fun load(name: String): ByteArray? {
        return try {
            val hex = prefs.getString("k:$name", null) ?: return null
            val all = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            val iv = all.copyOfRange(0, 12)
            val enc = all.copyOfRange(12, all.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            cipher.doFinal(enc)
        } catch (_: Exception) { null }
    }

    override fun remove(name: String) {
        prefs.edit().remove("k:$name").apply()
    }
}
