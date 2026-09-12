package com.samplocal.manager.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.io.File

data class ProcessResult(val pid: Long, val exitCode: Int? = null)

class ProcessManager {

    private var process: Process? = null
    private var stdoutJob: Job? = null
    private var stderrJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _stdout = MutableSharedFlow<String>(extraBufferCapacity = 1024)
    val stdout: SharedFlow<String> = _stdout
    private val _stderr = MutableSharedFlow<String>(extraBufferCapacity = 1024)
    val stderr: SharedFlow<String> = _stderr

    @Synchronized
    fun isAlive(): Boolean = try {
        process?.isAlive == true
    } catch (_: Exception) { false }

    @Synchronized
    fun getPid(): Long? {
        return try {
            val p = process ?: return null
            if (!p.isAlive) return null
            pidOf(p)
        } catch (_: Exception) { null }
    }

    suspend fun startProcess(
        command: List<String>,
        workDir: File,
        env: Map<String, String> = emptyMap(),
        onLine: (String) -> Unit = {}
    ): Result<Long?> {
        stopProcess(force = true)
        return try {
            workDir.mkdirs()
            val pb = ProcessBuilder(command)
                .directory(workDir)
                .redirectErrorStream(false)
            pb.environment().putAll(env)

            val p = pb.start()
            synchronized(this) { process = p }
            stdoutJob = scope.launch {
                try {
                    p.inputStream.bufferedReader().forEachLine { line ->
                        _stdout.tryEmit(line)
                        onLine(line)
                    }
                } catch (_: Exception) { }
            }
            stderrJob = scope.launch {
                try {
                    p.errorStream.bufferedReader().forEachLine { line ->
                        _stderr.tryEmit(line)
                        onLine(line)
                    }
                } catch (_: Exception) { }
            }
            Result.success(pidOf(p))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun pidOf(p: Process): Long? {
        return try {
            p.javaClass.getMethod("pid").invoke(p) as Long
        } catch (_: Exception) {
            null
        }
    }

    fun stopProcess(force: Boolean = false) {
        try { stdoutJob?.cancel() } catch (_: Exception) { }
        try { stderrJob?.cancel() } catch (_: Exception) { }
        val p = synchronized(this) { process }
        try {
            if (p?.isAlive == true) {
                if (force) p.destroyForcibly() else p.destroy()
            }
        } catch (_: Exception) { }
    }

    fun killProcess() = stopProcess(force = true)

    fun captureStdout(): SharedFlow<String> = stdout
    fun captureStderr(): SharedFlow<String> = stderr
}
