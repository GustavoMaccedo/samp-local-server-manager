package com.samplocal.manager.util

interface SecretStore {
    fun save(name: String, raw: ByteArray)
    fun load(name: String): ByteArray?
    fun remove(name: String)
}
