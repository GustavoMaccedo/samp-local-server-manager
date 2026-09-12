package com.samplocal.manager.samp

import java.io.File

data class GamemodeInfo(val name: String, val hasAmx: Boolean, val hasPwn: Boolean, val sizeBytes: Long)

class GamemodeManager {

    fun gamemodesDir(serverDir: File): File = File(serverDir, "gamemodes").apply { mkdirs() }

    fun listGamemodes(serverDir: File): List<GamemodeInfo> {
        val dir = gamemodesDir(serverDir)
        val amxFiles = dir.listFiles { f -> f.extension.lowercase() == "amx" } ?: emptyArray()
        return amxFiles.map { f ->
            val base = f.nameWithoutExtension
            GamemodeInfo(
                name = base,
                hasAmx = true,
                hasPwn = File(dir, "$base.pwn").exists(),
                sizeBytes = f.length()
            )
        }.sortedBy { it.name.lowercase() }
    }

    fun detectGamemode(serverDir: File, cfgGamemode0: String): GamemodeInfo? {
        val base = cfgGamemode0.split(" ").firstOrNull()?.trim().orEmpty()
        if (base.isEmpty()) return null
        val dir = gamemodesDir(serverDir)
        val amx = File(dir, "$base.amx")
        if (!amx.exists()) return null
        return GamemodeInfo(base, true, File(dir, "$base.pwn").exists(), amx.length())
    }

    fun installGamemode(serverDir: File, sourceAmx: File, targetName: String? = null): GamemodeInfo {
        require(sourceAmx.exists()) { "Arquivo .amx nao encontrado" }
        require(sourceAmx.extension.lowercase() == "amx") { "Arquivo precisa ser .amx" }
        val clean = (targetName ?: sourceAmx.nameWithoutExtension)
            .replace(Regex("[^A-Za-z0-9_\\-]"), "_").take(48)
        require(clean.isNotBlank()) { "Nome de gamemode invalido" }
        val dir = gamemodesDir(serverDir)
        val dest = File(dir, "$clean.amx")
        sourceAmx.copyTo(dest, overwrite = true)
        return GamemodeInfo(clean, true, false, dest.length())
    }

    fun importGamemode(serverDir: File, fileName: String, bytes: ByteArray): GamemodeInfo {
        require(fileName.lowercase().endsWith(".amx")) { "Selecione um arquivo .amx" }
        val base = fileName.substringAfterLast("/").substringAfterLast("\\")
            .removeSuffix(".amx").removeSuffix(".AMX")
            .replace(Regex("[^A-Za-z0-9_\\-]"), "_").take(48)
        val dest = File(gamemodesDir(serverDir), "$base.amx")
        dest.writeBytes(bytes)
        return GamemodeInfo(base, true, false, dest.length())
    }
}
