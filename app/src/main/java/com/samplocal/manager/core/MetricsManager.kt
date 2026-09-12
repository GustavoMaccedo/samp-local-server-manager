package com.samplocal.manager.core

import java.io.File

class MetricsManager(
    private val reader: ProcReader = RealProcReader(),
    private val clockMs: () -> Long = { android.os.SystemClock.elapsedRealtime() }
) {
    companion object {

        const val CLK_TCK = 100L
        const val HISTORY_MAX = 60
    }

    data class CpuState(val totalTicks: Long, val atMs: Long)
    data class MetricPoint(val cpuPct: Float, val rssMb: Float)

    private var lastCpu: CpuState? = null
    private val _history = ArrayDeque<MetricPoint>()
    val history: List<MetricPoint> get() = _history.toList()

    fun reset() {
        lastCpu = null
        _history.clear()
    }

    fun findPid(marker: String): Int? {
        return try {
            reader.listPids().firstOrNull { pid ->
                reader.cmdline(pid)?.contains(marker) == true
            }
        } catch (_: Exception) { null }
    }

    fun sample(pid: Int): MetricPoint? {
        return try {
            val stat = reader.procStat(pid) ?: return null
            val now = clockMs()
            val rssMb = (reader.rssKb(pid) ?: 0L) / 1024f
            val prev = lastCpu
            lastCpu = CpuState(stat.totalTicks, now)
            val cpu = if (prev == null) 0f else {
                val dtTicks = (stat.totalTicks - prev.totalTicks).coerceAtLeast(0)
                val dtSec = (now - prev.atMs) / 1000.0
                if (dtSec <= 0) 0f else (100.0 * dtTicks / CLK_TCK / dtSec).toFloat()
            }
            val point = MetricPoint(cpuPct = cpu.coerceAtLeast(0f), rssMb = rssMb)
            _history.addLast(point)
            while (_history.size > HISTORY_MAX) _history.removeFirst()
            point
        } catch (_: Exception) { null }
    }
}

data class ProcStat(val utime: Long, val stime: Long) {
    val totalTicks: Long get() = utime + stime
}

interface ProcReader {
    fun listPids(): List<Int>
    fun cmdline(pid: Int): String?
    fun procStat(pid: Int): ProcStat?
    fun rssKb(pid: Int): Long?
}

class RealProcReader : ProcReader {
    override fun listPids(): List<Int> {
        return File("/proc").listFiles()
            ?.mapNotNull { it.name.toIntOrNull() }
            ?: emptyList()
    }

    override fun cmdline(pid: Int): String? {
        return try {
            File("/proc/$pid/cmdline").readBytes()
                .toString(Charsets.ISO_8859_1)
                .replace('\u0000', ' ').trim().ifEmpty { null }
        } catch (_: Exception) { null }
    }

    override fun procStat(pid: Int): ProcStat? {
        return try {

            val line = File("/proc/$pid/stat").readText()
            val after = line.substringAfterLast(')').trim().split(Regex("\\s+"))
            if (after.size < 13) return null
            ProcStat(
                utime = after[11].toLong(),
                stime = after[12].toLong()
            )
        } catch (_: Exception) { null }
    }

    override fun rssKb(pid: Int): Long? {
        return try {
            File("/proc/$pid/status").bufferedReader().useLines { lines ->
                lines.firstOrNull { it.startsWith("VmRSS:") }
                    ?.split(Regex("\\s+"))?.getOrNull(1)?.toLong()
            }
        } catch (_: Exception) { null }
    }
}
