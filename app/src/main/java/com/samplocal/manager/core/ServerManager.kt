package com.samplocal.manager.core

import android.content.Context
import com.samplocal.manager.data.model.SampServerInfo
import com.samplocal.manager.data.model.ServerStatus
import com.samplocal.manager.runtime.ArchitectureDetector
import com.samplocal.manager.runtime.RuntimeManager
import com.samplocal.manager.samp.ConfigManager
import com.samplocal.manager.util.PortUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ServerManager(private val context: Context) {

    private val runtime = RuntimeManager(context)
    private val configManager = ConfigManager()
    private val fileManager = FileManager()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val processes = mutableMapOf<String, ProcessManager>()
    private val logManagers = mutableMapOf<String, LogManager>()
    private val monitorJobs = mutableMapOf<String, Job>()
    private val startTime = mutableMapOf<String, Long>()

    private val logOffsets = mutableMapOf<String, Int>()

    private val _servers = MutableStateFlow<List<SampServerInfo>>(emptyList())
    val servers: StateFlow<List<SampServerInfo>> = _servers.asStateFlow()

    private val _console = MutableStateFlow<Map<String, List<LogLine>>>(emptyMap())
    val console: StateFlow<Map<String, List<LogLine>>> = _console.asStateFlow()

    fun logManager(serverId: String): LogManager =
        logManagers.getOrPut(serverId) { LogManager(runtime.serverDir(serverId)) }

    fun processManager(serverId: String): ProcessManager =
        processes.getOrPut(serverId) { ProcessManager() }

    fun refresh() {
        val dir = runtime.serversDir()
        val ids = dir.listFiles { f -> f.isDirectory }?.map { it.name } ?: emptyList()
        val current = _servers.value.associateBy { it.id }
        _servers.value = ids.sorted().map { id ->
            val serverDir = File(dir, id)
            val cfg = try { configManager.readConfig(serverDir) } catch (_: Exception) { null }
            val prev = current[id]
            val pm = processes[id]
            val alive = pm?.isAlive() == true
            val status = when {
                alive && (prev?.status == ServerStatus.STARTING || prev?.status == ServerStatus.RUNNING) -> {

                    if (prev.status == ServerStatus.STARTING && hasStarted(serverDir)) ServerStatus.RUNNING
                    else prev.status
                }
                alive -> ServerStatus.RUNNING
                prev?.status == ServerStatus.STARTING ||
                    prev?.status == ServerStatus.STOPPING -> prev.status
                prev?.status == ServerStatus.CRASHED ||
                    prev?.status == ServerStatus.ERROR -> prev.status
                else -> ServerStatus.STOPPED
            }
            SampServerInfo(
                id = id,
                name = id,
                port = cfg?.port ?: prev?.port ?: 7777,
                maxPlayers = cfg?.maxplayers ?: 50,
                hostname = cfg?.hostname ?: id,
                gamemode = cfg?.gamemode0 ?: "",
                status = status,
                pid = pm?.getPid()?.toInt(),
                uptimeSec = startTime[id]?.let { (System.currentTimeMillis() - it) / 1000 } ?: 0L,
                lastError = prev?.lastError
            )
        }
    }

    private fun hasStarted(serverDir: File): Boolean {
        val log = File(serverDir, "server_log.txt")
        if (!log.exists()) return consoleHasMarker()
        return try {
            val text = log.readText().takeLast(20000)
            text.contains("Server Plugins") || text.contains("Gamemode") ||
                text.contains("Incoming") || text.contains("Loaded") || consoleHasMarker()
        } catch (_: Exception) { consoleHasMarker() }
    }

    private fun consoleHasMarker(): Boolean {
        val all = _console.value.values.flatten().takeLast(50).joinToString("\n") { it.text }
        return all.contains("SA-MP") || all.contains("Loaded") || all.contains("server.cfg")
    }

    fun isInstalled(serverId: String): Boolean =
        File(runtime.serverDir(serverId), "samp03svr").exists()

    fun isRunning(serverId: String): Boolean =
        processes[serverId]?.isAlive() == true

    fun startedAt(serverId: String): Long? = startTime[serverId]

    fun status(serverId: String): ServerStatus =
        _servers.value.firstOrNull { it.id == serverId }?.status ?: ServerStatus.STOPPED

    suspend fun install(serverId: String, zipFile: File? = null): Result<String> {
        setStatus(serverId, ServerStatus.INSTALLING)
        return try {
            val dir = runtime.serverDir(serverId)
            fileManager.ensureServerStructure(dir)
            if (zipFile != null) {
                val report = fileManager.importServerZip(zipFile, dir, serverId)
                if (!report.foundBinary) {
                    setStatus(serverId, ServerStatus.ERROR, "Servidor nao encontrado.")
                    return Result.failure(IllegalStateException("Nao foi encontrado um samp03svr Linux valido."))
                }
            }

            if (!File(dir, "server.cfg").exists()) {
                configManager.writeConfig(dir, com.samplocal.manager.data.model.SampConfig(port = 7777))
            }

            runtime.installRuntime()
            setStatus(serverId, ServerStatus.STOPPED)
            refresh()
            Result.success("Servidor importado com sucesso.")
        } catch (e: Exception) {
            setStatus(serverId, ServerStatus.ERROR, e.message)
            Result.failure(e)
        }
    }

    suspend fun start(serverId: String): Result<String> {
        val dir = runtime.serverDir(serverId)

        if (!isInstalled(serverId)) {
            setStatus(serverId, ServerStatus.ERROR, "Servidor nao encontrado.")
            return Result.failure(IllegalStateException("Servidor nao encontrado."))
        }

        appendConsole(serverId, "Preparando runtime embutido...")
        val ensured = runtime.ensureInstalled()
        if (ensured.isFailure) {
            val msg = "Falha ao preparar runtime: ${ensured.exceptionOrNull()?.message}"
            setStatus(serverId, ServerStatus.ERROR, msg)
            appendConsole(serverId, msg)
            try {
                appendConsole(serverId, "diagnostico")
                runtime.diagnose().lines().forEach { appendConsole(serverId, it) }
            } catch (_: Exception) { }
            return Result.failure(IllegalStateException(msg))
        }
        val rtStatus = ensured.getOrThrow()

        val binInfo = ArchitectureDetector.detectBinaryArchitecture(File(dir, "samp03svr"))
        if (!binInfo.isElf) {
            setStatus(serverId, ServerStatus.ERROR, "Arquitetura do servidor incompativel.")
            return Result.failure(IllegalStateException("Binario samp03svr invalido (nao-ELF)."))
        }

        if (!File(dir, "server.cfg").exists()) {
            setStatus(serverId, ServerStatus.ERROR, "server.cfg nao encontrado.")
            return Result.failure(IllegalStateException("server.cfg nao encontrado."))
        }
        val cfg = configManager.readConfig(dir)

        if (!PortUtils.isPortAvailable(cfg.port) && !isPortOwnedByUs(serverId, cfg.port)) {
            setStatus(serverId, ServerStatus.ERROR, "Porta ja esta em uso.")
            return Result.failure(IllegalStateException("Porta ${cfg.port} ja esta em uso por outro servidor."))
        }

        setStatus(serverId, ServerStatus.STARTING)
        appendConsole(serverId, "Validando servidor... OK")
        appendConsole(serverId, "Runtime: ${rtStatus.backend} host=${rtStatus.hostArch} guest=${rtStatus.guestArch}")
        appendConsole(serverId, "server.cfg: ${cfg.hostname} :${cfg.port} (${cfg.gamemode0})")
        appendConsole(serverId, "Starting samp03svr...")

        val pm = processManager(serverId)
        val lm = logManager(serverId)
        lm.clearLogs()
        val cmd = runtime.buildStartCommand(serverId)
        val env = runtime.backendEnv()

        try { File(dir, "server_log.txt").delete() } catch (_: Exception) { }
        logOffsets[serverId] = 0
        val res = pm.startProcess(cmd, dir, env) { line ->
            appendConsole(serverId, line)
        }
        if (res.isFailure) {
            setStatus(serverId, ServerStatus.ERROR, res.exceptionOrNull()?.message)
            return Result.failure(res.exceptionOrNull() ?: IllegalStateException("Falha ao iniciar"))
        }
        startTime[serverId] = System.currentTimeMillis()
        startMonitor(serverId)

        scope.launch {
            repeat(40) {
                delay(500)
                pumpServerLog(serverId, dir)
                if (hasStarted(dir)) {
                    setStatus(serverId, ServerStatus.RUNNING)
                    appendConsole(serverId, "Servidor ONLINE em 127.0.0.1:${cfg.port}")
                    return@launch
                }
                if (pm.isAlive().not()) {
                    setStatus(serverId, ServerStatus.CRASHED, "Servidor encerrou inesperadamente.")
                    appendConsole(serverId, "Servidor encerrou inesperadamente. Veja server_log.txt.")
                    return@launch
                }
            }
            if (status(serverId) == ServerStatus.STARTING) {

                if (pm.isAlive()) setStatus(serverId, ServerStatus.RUNNING)
            }
        }
        refresh()
        return Result.success("Iniciando servidor...")
    }

    private fun isPortOwnedByUs(serverId: String, port: Int): Boolean {

        return isRunning(serverId) && (_servers.value.firstOrNull { it.id == serverId }?.port == port)
    }

    private fun startMonitor(serverId: String) {
        monitorJobs[serverId]?.cancel()
        monitorJobs[serverId] = scope.launch {
            val dir = runtime.serverDir(serverId)
            while (true) {
                delay(2000)
                val pm = processes[serverId] ?: break
                if (status(serverId) == ServerStatus.RUNNING || status(serverId) == ServerStatus.STARTING) {
                    if (!pm.isAlive()) {

                        if (status(serverId) != ServerStatus.STOPPING) {
                            setStatus(serverId, ServerStatus.CRASHED, "Servidor encerrou inesperadamente.")
                        }
                        break
                    }

                    refresh()
                    pumpServerLog(serverId, dir)
                } else break
            }
        }
    }

    suspend fun stop(serverId: String, force: Boolean = false): Result<String> {
        setStatus(serverId, ServerStatus.STOPPING)
        appendConsole(serverId, "Parando servidor...")
        return try {
            val pm = processes[serverId]
            if (pm == null || !pm.isAlive()) {
                setStatus(serverId, ServerStatus.STOPPED)
                refresh()
                return Result.success("Servidor ja estava parado.")
            }
            if (!force) {
                pm.stopProcess(force = false)

                repeat(10) {
                    delay(500)
                    if (!pm.isAlive()) return@repeat
                }
            }
            if (pm.isAlive()) pm.killProcess()
            delay(500)

            try { logManager(serverId).saveLogs() } catch (_: Exception) { }
            setStatus(serverId, ServerStatus.STOPPED)
            startTime.remove(serverId)
            monitorJobs[serverId]?.cancel()
            refresh()
            Result.success("Servidor parado.")
        } catch (e: Exception) {
            setStatus(serverId, ServerStatus.ERROR, e.message)
            Result.failure(e)
        }
    }

    suspend fun restart(serverId: String): Result<String> {
        stop(serverId)
        delay(1000)
        return start(serverId)
    }

    fun appendConsole(serverId: String, line: String, tsMs: Long = System.currentTimeMillis()) {
        val cur = _console.value[serverId] ?: emptyList()
        if (cur.lastOrNull()?.text == line) return
        _console.value = _console.value + (serverId to (cur + LogLine(line, tsMs)).takeLast(1000))
        try { logManager(serverId).append(line, tsMs) } catch (_: Exception) { }
    }

    private fun pumpServerLog(serverId: String, dir: File) {
        try {
            val f = File(dir, "server_log.txt")
            if (!f.isFile) {
                logOffsets[serverId] = 0
                return
            }
            val (fresh, next) = newLines(f.readLines(), logOffsets[serverId] ?: 0)
            if (fresh.isNotEmpty()) {
                logOffsets[serverId] = next
                fresh.forEach { appendConsole(serverId, it) }
            }
        } catch (_: Exception) { }
    }

    companion object {

        internal fun newLines(all: List<String>, offset: Int): Pair<List<String>, Int> {
            val start = if (all.size < offset) 0 else offset
            return all.drop(start).takeLast(500) to all.size
        }
    }

    fun clearConsole(serverId: String) {
        _console.value = _console.value + (serverId to emptyList())
        try { logManager(serverId).clearLogs() } catch (_: Exception) { }
    }

    private fun setStatus(serverId: String, status: ServerStatus, error: String? = null) {
        val cur = _servers.value.toMutableList()
        val idx = cur.indexOfFirst { it.id == serverId }
        if (idx >= 0) {
            cur[idx] = cur[idx].copy(status = status, lastError = error ?: cur[idx].lastError)
        } else {
            cur.add(SampServerInfo(id = serverId, name = serverId, status = status, lastError = error))
        }
        _servers.value = cur
    }
}
