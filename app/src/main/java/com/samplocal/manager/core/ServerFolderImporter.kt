package com.samplocal.manager.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

class ServerFolderImporter(private val context: Context) {

    data class TreeSnapshot(
        val displayName: String,
        val files: Set<String>,
        val dirs: Set<String>
    )

    data class FolderValidation(
        val displayName: String,
        val hasServerCfg: Boolean,
        val hasSampBinary: Boolean,
        val gamemodeCount: Int,
        val pluginCount: Int,
        val dirCount: Int,
        val fileCount: Int,
        val presentDirs: List<String>,
        val hostname: String?,
        val warnings: List<String>,
        val valid: Boolean
    )

    data class ImportStats(val filesCopied: Int, val bytesCopied: Long)

    companion object {
        const val MAX_SCAN_FILES = 5000
        const val MAX_COPY_FILES = 20000
        private val KNOWN_DIRS = listOf(
            "gamemodes", "filterscripts", "plugins", "scriptfiles", "npcmodes", "logs"
        )

        fun cleanServerId(raw: String): String {
            val clean = raw.trim().replace(Regex("[^A-Za-z0-9_\\-]"), "_")
                .trim('_').take(32)
            return clean.ifEmpty { "Servidor" }
        }

        fun validate(snapshot: TreeSnapshot, cfgText: String?): FolderValidation {
            val lowerFiles = snapshot.files.map { it.lowercase() }.toSet()
            val lowerDirs = snapshot.dirs.map { it.lowercase() }.toSet()
            val hasCfg = "server.cfg" in lowerFiles
            val hasBinary = lowerFiles.any {
                it == "samp03svr" || it.endsWith("/samp03svr") ||
                    it == "samp-server.exe" || it.endsWith("/samp-server.exe")
            }
            val gmCount = lowerFiles.count { it.endsWith(".amx") && "gamemodes/" in it }
            val pluginCount = lowerFiles.count { it.endsWith(".so") && "plugins/" in it }
            val present = KNOWN_DIRS.filter { it in lowerDirs }
            val hostname = cfgText?.lines()
                ?.firstOrNull { it.trim().lowercase().startsWith("hostname") }
                ?.split(Regex("\\s+"), limit = 2)?.getOrNull(1)?.trim()
                ?.takeIf { it.isNotEmpty() }
            val warnings = mutableListOf<String>()
            if (!hasCfg) warnings.add("server.cfg nao encontrado")
            if (gmCount == 0) warnings.add("nenhum gamemode .amx em gamemodes/")
            if (pluginCount == 0) warnings.add("nenhum plugin .so em plugins/")
            if (!hasBinary) warnings.add("executavel do servidor nao encontrado")
            val valid = hasCfg || hasBinary || gmCount > 0
            return FolderValidation(
                displayName = snapshot.displayName,
                hasServerCfg = hasCfg,
                hasSampBinary = hasBinary,
                gamemodeCount = gmCount,
                pluginCount = pluginCount,
                dirCount = snapshot.dirs.size,
                fileCount = snapshot.files.size,
                presentDirs = present,
                hostname = hostname,
                warnings = warnings,
                valid = valid
            )
        }
    }

    private val prefs get() =
        context.getSharedPreferences("server_folders", Context.MODE_PRIVATE)

    fun setSourceUri(serverId: String, uri: Uri) {
        prefs.edit().putString("src:$serverId", uri.toString()).apply()
    }

    fun sourceUri(serverId: String): Uri? =
        prefs.getString("src:$serverId", null)?.let { Uri.parse(it) }

    fun hasPersistedPermission(uri: Uri): Boolean {
        return try {
            context.contentResolver.persistedUriPermissions.any {
                it.uri == uri && it.isReadPermission
            }
        } catch (_: Exception) { false }
    }

    fun needsReauth(serverId: String): Boolean {
        val uri = sourceUri(serverId) ?: return false
        return !hasPersistedPermission(uri)
    }

    fun takePersistable(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: SecurityException) {

        }
    }

    fun snapshotTree(treeUri: Uri): TreeSnapshot {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IllegalArgumentException("Pasta inacessivel")
        val name = root.name ?: "Servidor"
        val files = mutableSetOf<String>()
        val dirs = mutableSetOf<String>()
        val queue = ArrayDeque<Pair<DocumentFile, String>>()
        queue.add(root to "")
        var count = 0
        while (queue.isNotEmpty() && count < MAX_SCAN_FILES) {
            val (dir, rel) = queue.removeFirst()
            val children = try {
                dir.listFiles()
            } catch (_: Exception) { emptyArray() }
            for (child in children) {
                val childName = child.name ?: continue
                if (childName in setOf(".", "..")) continue
                val childRel = if (rel.isEmpty()) childName else "$rel/$childName"
                if (child.isDirectory) {
                    dirs.add(childRel)
                    if (childRel.count { it == '/' } < 6) queue.add(child to childRel)
                } else {
                    files.add(childRel)
                    count++
                }
            }
        }
        return TreeSnapshot(name, files, dirs)
    }

    fun readServerCfg(treeUri: Uri): String? {
        return try {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return null
            val cfg = root.listFiles().firstOrNull {
                it.isFile && it.name?.lowercase() == "server.cfg"
            } ?: return null
            context.contentResolver.openInputStream(cfg.uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            }
        } catch (_: Exception) { null }
    }

    fun importTree(treeUri: Uri, destDir: File, onProgress: (Int, Int) -> Unit = { _, _ -> }): ImportStats {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IllegalArgumentException("Pasta inacessivel")
        destDir.mkdirs()
        val base = destDir.canonicalPath
        var files = 0
        var bytes = 0L
        var scanned = 0
        val queue = ArrayDeque<Pair<DocumentFile, File>>()
        queue.add(root to destDir)
        while (queue.isNotEmpty()) {
            val (src, dst) = queue.removeFirst()
            val children = try {
                src.listFiles()
            } catch (e: Exception) {
                throw IllegalStateException("Falha ao ler a pasta: ${e.message}")
            }
            for (child in children) {
                val name = child.name ?: continue
                if (name in setOf(".", "..") || "/" in name || "\\" in name) continue
                val out = File(dst, name)
                if (!out.canonicalPath.startsWith(base + File.separator)) continue
                if (child.isDirectory) {
                    out.mkdirs()
                    queue.add(child to out)
                } else {
                    if (++scanned > MAX_COPY_FILES) {
                        throw IllegalStateException("Pasta grande demais (limite $MAX_COPY_FILES arquivos)")
                    }
                    context.contentResolver.openInputStream(child.uri)?.use { ins ->
                        out.outputStream().use { fos -> bytes += ins.copyTo(fos) }
                    } ?: throw IllegalStateException("Nao foi possivel ler $name")
                    files++
                    if (files % 10 == 0) onProgress(files, -1)
                }
            }
        }
        onProgress(files, files)
        File(destDir, "samp03svr").takeIf { it.isFile }?.setExecutable(true)
        return ImportStats(files, bytes)
    }
}
