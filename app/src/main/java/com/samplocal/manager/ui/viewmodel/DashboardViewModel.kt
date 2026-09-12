package com.samplocal.manager.ui.viewmodel

import android.app.ActivityManager
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samplocal.manager.SampApp
import com.samplocal.manager.core.MetricsManager
import com.samplocal.manager.data.model.SampServerInfo
import com.samplocal.manager.data.model.ServerStatus
import com.samplocal.manager.samp.ConfigManager
import com.samplocal.manager.samp.ServerQueryManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val sampApp get() = getApplication<SampApp>()
    private val serversFlow get() = sampApp.serverManager.servers
    private val configManager = ConfigManager()
    private val metrics = MetricsManager()
    private val query = ServerQueryManager()

    val servers: StateFlow<List<SampServerInfo>> get() = serversFlow

    private val _serverId = MutableStateFlow("Default")
    val serverId: StateFlow<String> = _serverId.asStateFlow()

    private val _server = MutableStateFlow<SampServerInfo?>(null)
    val server: StateFlow<SampServerInfo?> = _server.asStateFlow()

    private val _uptimeSec = MutableStateFlow(0L)
    val uptimeSec: StateFlow<Long> = _uptimeSec.asStateFlow()

    private val _cpuPct = MutableStateFlow(0f)
    val cpuPct: StateFlow<Float> = _cpuPct.asStateFlow()

    private val _memMb = MutableStateFlow(0f)
    val memMb: StateFlow<Float> = _memMb.asStateFlow()

    private val _memPct = MutableStateFlow(0f)
    val memPct: StateFlow<Float> = _memPct.asStateFlow()

    private val _cpuHistory = MutableStateFlow<List<Float>>(emptyList())
    val cpuHistory: StateFlow<List<Float>> = _cpuHistory.asStateFlow()

    private val _memHistory = MutableStateFlow<List<Float>>(emptyList())
    val memHistory: StateFlow<List<Float>> = _memHistory.asStateFlow()

    private val _live = MutableStateFlow<ServerQueryManager.QueryInfo?>(null)
    val live: StateFlow<ServerQueryManager.QueryInfo?> = _live.asStateFlow()

    private val _version = MutableStateFlow<String?>(null)
    val version: StateFlow<String?> = _version.asStateFlow()

    private val _measuring = MutableStateFlow(false)
    val measuring: StateFlow<Boolean> = _measuring.asStateFlow()

    private var jobs = listOf<Job>()
    private var bound: Job? = null

    fun bind(ids: StateFlow<String>) {
        bound?.cancel()
        bound = viewModelScope.launch {
            ids.collect { id ->
                _serverId.value = id
                metrics.reset()
                _cpuHistory.value = emptyList()
                _memHistory.value = emptyList()
                _live.value = null
                _cpuPct.value = 0f
                _memMb.value = 0f
                _memPct.value = 0f
                _uptimeSec.value = 0L
                refreshStatic(id)
            }
        }
        viewModelScope.launch {
            serversFlow.collect { list ->
                _server.value = list.firstOrNull { it.id == _serverId.value }
                    ?: list.firstOrNull()
            }
        }
    }

    private fun refreshStatic(id: String) {
        try {
            val dir = sampApp.runtimeManager.serverDir(id)
            _version.value = parseServerVersion(dir)
            _server.value = serversFlow.value.firstOrNull { it.id == id }
        } catch (_: Exception) { }
    }

    fun start() {
        if (jobs.any { it.isActive }) return
        _measuring.value = true
        val uptime = viewModelScope.launch {
            while (isActive) {
                _uptimeSec.value = currentUptime()
                delay(1000)
            }
        }
        val sampler = viewModelScope.launch {
            while (isActive) {
                sampleOnce()
                delay(2000)
            }
        }
        val querier = viewModelScope.launch {
            while (isActive) {
                queryOnce()
                delay(5000)
            }
        }
        jobs = listOf(uptime, sampler, querier)
    }

    fun stop() {
        jobs.forEach { it.cancel() }
        jobs = emptyList()
        _measuring.value = false
    }

    private fun currentUptime(): Long {
        val s = _server.value ?: return 0L
        if (s.status != ServerStatus.RUNNING && s.status != ServerStatus.STARTING) return 0L
        val started = sampApp.serverManager.startedAt(s.id) ?: return 0L
        return ((System.currentTimeMillis() - started) / 1000).coerceAtLeast(0L)
    }

    private fun sampleOnce() {
        try {
            val s = _server.value
            if (s == null || (s.status != ServerStatus.RUNNING && s.status != ServerStatus.STARTING)) {
                _cpuPct.value = 0f
                _memMb.value = 0f
                _memPct.value = 0f
                return
            }
            val marker = File(sampApp.runtimeManager.serverDir(s.id), "samp03svr").absolutePath
            val pid = metrics.findPid(marker) ?: return
            val point = metrics.sample(pid) ?: return
            _cpuPct.value = point.cpuPct
            _memMb.value = point.rssMb
            _memPct.value = (point.rssMb / deviceTotalMb() * 100f).coerceIn(0f, 100f)
            _cpuHistory.value = metrics.history.map { it.cpuPct }
            _memHistory.value = metrics.history.map { it.rssMb }
        } catch (_: Exception) { }
    }

    private fun queryOnce() {
        try {
            val s = _server.value ?: return
            if (s.status != ServerStatus.RUNNING) {
                if (_live.value != null) _live.value = null
                return
            }
            viewModelScope.launch {
                val r = query.queryInfo("127.0.0.1", s.port, 2000)
                if (r.isSuccess) _live.value = r.getOrNull()
            }
        } catch (_: Exception) { }
    }

    private fun deviceTotalMb(): Float {
        return try {
            val am = getApplication<Application>().getSystemService(ActivityManager::class.java)
            val mi = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)
            (mi.totalMem / (1024f * 1024f)).coerceAtLeast(1f)
        } catch (_: Exception) { 4096f }
    }

    companion object {
        fun formatUptime(totalSec: Long): String {
            val h = totalSec / 3600
            val m = (totalSec % 3600) / 60
            val s = totalSec % 60
            return String.format("%02d:%02d:%02d", h, m, s)
        }

        private val VERSION_RE = Regex("""v(\d+\.\d+\.\d+(?:-R\d+)?)""")

        fun parseServerVersion(serverDir: File): String? {
            return try {
                val log = File(serverDir, "server_log.txt")
                if (!log.isFile) return null
                VERSION_RE.find(log.readText().takeLast(20000))?.groupValues?.get(1)
            } catch (_: Exception) { null }
        }
    }
}
