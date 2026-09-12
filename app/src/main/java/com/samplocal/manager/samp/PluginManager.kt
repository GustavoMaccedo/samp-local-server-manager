package com.samplocal.manager.samp

import java.io.File

data class PluginInfo(val name: String, val file: String, val sizeBytes: Long, val enabled: Boolean)

class PluginManager {

    fun pluginsDir(serverDir: File): File = File(serverDir, "plugins").apply { mkdirs() }

    fun listPlugins(serverDir: File): List<PluginInfo> {
        val dir = pluginsDir(serverDir)
        val files = dir.listFiles { f -> f.extension.lowercase() == "so" } ?: emptyArray()
        return files.map { PluginInfo(it.nameWithoutExtension, it.name, it.length(), true) }
            .sortedBy { it.name.lowercase() }
    }

    fun detectPlugins(serverDir: File, cfgText: String): List<PluginInfo> {
        val line = cfgText.lines().firstOrNull { it.trim().lowercase().startsWith("plugins") }
        val declared = line?.split(Regex("\\s+"))?.drop(1)?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        val existing = listPlugins(serverDir).associateBy { it.file }
        return declared.map { name ->
            val file = if (name.lowercase().endsWith(".so")) name else "$name.so"
            val found = existing[file]
            if (found != null) found
            else PluginInfo(name.removeSuffix(".so"), file, 0L, false)
        }
    }

    fun installPlugin(serverDir: File, source: File): PluginInfo {
        require(source.exists()) { "Plugin nao encontrado" }
        require(source.extension.lowercase() == "so") { "Plugin precisa ser .so Linux" }
        val dest = File(pluginsDir(serverDir), source.name)
        source.copyTo(dest, overwrite = true)

        dest.setReadable(true)
        return PluginInfo(dest.nameWithoutExtension, dest.name, dest.length(), true)
    }

    fun removePlugin(serverDir: File, fileName: String): Boolean {
        val f = File(pluginsDir(serverDir), fileName)
        if (!f.exists() || f.extension.lowercase() != "so") return false
        return f.delete()
    }
}
