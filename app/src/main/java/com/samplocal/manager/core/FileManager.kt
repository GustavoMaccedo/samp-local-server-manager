package com.samplocal.manager.core

import com.samplocal.manager.util.ZipUtils
import java.io.File

data class ImportReport(
    val serverId: String,
    val foundBinary: Boolean,
    val foundConfig: Boolean,
    val gamemodes: Int,
    val plugins: Int,
    val warnings: List<String>
)

class FileManager {

    fun ensureServerStructure(serverDir: File) {
        listOf(
            "gamemodes", "filterscripts", "scriptfiles",
            "plugins", "npcmodes", "logs"
        ).forEach { File(serverDir, it).mkdirs() }
    }

    fun listFiles(dir: File): List<File> =
        dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()

    fun delete(file: File, confirmName: String? = null): Boolean {
        if (confirmName != null && file.name != confirmName) return false
        return if (file.isDirectory) file.deleteRecursively() else file.delete()
    }

    fun rename(file: File, newName: String): Boolean {
        val clean = newName.trim().take(64)
        if (clean.isEmpty() || clean.contains("/") || clean.contains("..")) return false
        return file.renameTo(File(file.parentFile, clean))
    }

    fun resolveUniqueName(dir: File, rawName: String): File {
        var base = rawName.substringAfterLast("/").substringAfterLast("\\").trim()
        if (base.isEmpty() || base == "." || base == "..") base = "arquivo"
        base = base.take(128)
        val dot = base.lastIndexOf('.')
        val stem = if (dot > 0) base.substring(0, dot) else base
        val ext = if (dot > 0) base.substring(dot) else ""
        var candidate = File(dir, base)
        var n = 1
        while (candidate.exists()) {
            n++
            candidate = File(dir, "$stem ($n)$ext")
        }
        return candidate
    }

    fun importServerZip(zipFile: File, destDir: File, serverId: String): ImportReport {
        require(zipFile.exists()) { "ZIP nao encontrado" }
        destDir.mkdirs()
        val warnings = mutableListOf<String>()
        ZipUtils.safeUnzip(zipFile, destDir)

        val children = destDir.listFiles()?.toList() ?: emptyList()
        val singleDir = if (children.size == 1 && children[0].isDirectory) children[0] else null
        if (singleDir != null && !File(destDir, "samp03svr").exists() && File(singleDir, "samp03svr").exists()) {
            singleDir.listFiles()?.forEach { it.renameTo(File(destDir, it.name)) }
            singleDir.deleteRecursively()
        }

        ensureServerStructure(destDir)
        val binary = File(destDir, "samp03svr")
        val cfg = File(destDir, "server.cfg")
        val gmCount = File(destDir, "gamemodes").listFiles { f -> f.extension.lowercase() == "amx" }?.size ?: 0
        val pluginCount = File(destDir, "plugins").listFiles { f -> f.extension.lowercase() == "so" }?.size ?: 0
        if (!binary.exists()) warnings.add("Nao foi encontrado um samp03svr Linux valido.")
        else {
            binary.setExecutable(true)
        }
        if (!cfg.exists()) warnings.add("server.cfg nao encontrado, sera criado um padrao.")
        if (gmCount == 0) warnings.add("Nenhum gamemode .amx encontrado.")
        return ImportReport(serverId, binary.exists(), cfg.exists(), gmCount, pluginCount, warnings)
    }
}
