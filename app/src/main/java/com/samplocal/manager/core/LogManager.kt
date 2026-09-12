package com.samplocal.manager.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogLine(val text: String, val tsMs: Long = System.currentTimeMillis())

class LogManager(serverDir: File) {

    private val logsDir = File(serverDir, "logs").apply { mkdirs() }
    private val _entries = MutableStateFlow<List<LogLine>>(emptyList())
    val entries: StateFlow<List<LogLine>> = _entries.asStateFlow()

    @Synchronized
    fun append(line: String, tsMs: Long = System.currentTimeMillis()) {
        val cur = _entries.value
        _entries.value = (cur + LogLine(line, tsMs)).takeLast(2000)
    }

    fun clearLogs() {
        _entries.value = emptyList()
    }

    fun currentText(): String = _entries.value.joinToString("\n") { it.text }

    fun saveLogs(prefix: String = "console"): File {
        val ts = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val f = File(logsDir, "${ts}_$prefix.log")
        f.writeText(currentText())
        return f
    }

    fun pruneOlderThan(days: Int): Int {
        if (days <= 0) return 0
        val cutoff = System.currentTimeMillis() - days * 24L * 3600L * 1000L
        var n = 0
        logsDir.listFiles { f -> f.extension == "log" }?.forEach { f ->
            try {
                if (f.lastModified() < cutoff && f.delete()) n++
            } catch (_: Exception) { }
        }
        return n
    }

    fun readServerLog(serverDir: File, maxLines: Int = 300): List<String> {
        val f = File(serverDir, "server_log.txt")
        if (!f.exists()) return emptyList()
        val all = f.readLines()
        return all.takeLast(maxLines)
    }

    fun listSavedLogs(): List<File> =
        logsDir.listFiles { f -> f.extension == "log" }?.sortedByDescending { it.lastModified() } ?: emptyList()
}
