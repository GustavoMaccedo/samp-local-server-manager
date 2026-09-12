package com.samplocal.manager.samp

import com.samplocal.manager.data.model.SampConfig
import java.io.File

class ConfigManager {

    fun configFile(serverDir: File): File = File(serverDir, "server.cfg")

    fun readConfig(serverDir: File): SampConfig {
        val f = configFile(serverDir)
        if (!f.exists()) return SampConfig()
        return SampConfig.parse(f.readText())
    }

    fun writeConfig(serverDir: File, cfg: SampConfig) {
        serverDir.mkdirs()
        configFile(serverDir).writeText(cfg.toCfgText())
    }

    fun updateConfig(serverDir: File, mutate: (SampConfig) -> SampConfig): SampConfig {
        val updated = mutate(readConfig(serverDir))
        writeConfig(serverDir, updated)
        return updated
    }

    fun validateConfig(serverDir: File): List<String> {
        val cfg = readConfig(serverDir)
        val errors = SampConfig.validate(cfg).toMutableList()
        if (!configFile(serverDir).exists()) errors.add("server.cfg nao encontrado")
        val gmName = cfg.gamemode0.split(" ").firstOrNull()?.trim().orEmpty()
        if (gmName.isNotEmpty()) {
            val amx = File(serverDir, "gamemodes/$gmName.amx")
            val pwn = File(serverDir, "gamemodes/$gmName.pwn")
            if (!amx.exists() && !pwn.exists()) errors.add("Gamemode nao encontrado: gamemodes/$gmName.amx")
        }
        return errors
    }
}
