package com.samplocal.manager.net.playit

import android.content.Context
import android.os.Build
import java.io.File

enum class AgentLogState { CONNECTED, AUTH_ERROR, WAITING, STARTING, UNKNOWN }

open class PlayitAgent(
    private val nativeLibDir: File,
    private val filesDir: File
) {
    constructor(context: Context) : this(
        File(context.applicationInfo.nativeLibraryDir),
        context.filesDir
    )

    interface ProcessStarter {
        fun start(cmd: List<String>, env: Map<String, String>): ManagedProcess
    }

    interface ManagedProcess {
        val alive: Boolean
        fun exitCode(): Int?
        fun stop()
    }

    private class RealProcess(val p: Process) : ManagedProcess {
        override val alive: Boolean get() = try { p.isAlive } catch (_: Exception) { false }
        override fun exitCode(): Int? = try {
            if (p.isAlive) null else p.exitValue()
        } catch (_: Exception) { null }
        override fun stop() {
            try { p.destroy() } catch (_: Exception) { }
        }
    }

    object RealStarter : ProcessStarter {
        override fun start(cmd: List<String>, env: Map<String, String>): ManagedProcess {
            val pb = ProcessBuilder(cmd)
            pb.environment().putAll(env)
            pb.redirectErrorStream(true)
            val proc = pb.start()

            Thread({
                try {
                    proc.inputStream.use { ins ->

                        val buf = ByteArray(8192)
                        while (true) {
                            if (ins.read(buf) <= 0) break
                        }
                    }
                } catch (_: Exception) { }
            }, "playit-drain").apply { isDaemon = true; start() }
            return RealProcess(proc)
        }
    }

    var starter: ProcessStarter = RealStarter
        internal set

    private var process: ManagedProcess? = null
    private var mode: Mode = Mode.STOPPED

    private enum class Mode { STOPPED, UNCLAIMED, AUTHENTICATED }

    @Volatile var lastExitCode: Int? = null
        private set
    @Volatile var lastError: String? = null
        private set

    fun binaryPath(): File = File(nativeLibDir, "libplayit-agent.so")

    fun socketPath(): File = File(filesDir, "playit/playitd.sock")
    fun logPath(): File = File(filesDir, "playit/playitd.log")

    companion object {

        fun checkAbi(abis: List<String>): Result<Unit> {
            val norm = abis.map { it.lowercase() }
            if (norm.none { it == "arm64-v8a" || it.contains("arm64") || it.contains("aarch64") }) {
                return Result.failure(IllegalStateException("Dispositivo incompatível (exige ARM64)"))
            }
            return Result.success(Unit)
        }

        data class LogParse(val state: AgentLogState, val errorLine: String?)

        fun parseLogTail(text: String): LogParse {
            var waiting = false
            var starting = false
            for (raw in text.lines().takeLast(60)) {
                val line = raw.trim()
                if (line.isEmpty()) continue
                if (looksLikeSecret(line)) continue
                val low = line.lowercase()
                if ("playit connected" in low) return LogParse(AgentLogState.CONNECTED, null)
                if ("invalidagentkey" in low || "nolongervalid" in low ||
                    ("apierror(auth" in low.replace(" ", ""))
                ) {
                    return LogParse(AgentLogState.AUTH_ERROR, line.take(220))
                }
                if ("failed to start playit agent" in low || "setup error" in low) {
                    return LogParse(AgentLogState.AUTH_ERROR, line.take(220))
                }
                if ("waiting for frontend secret provisioning" in low) waiting = true
                if ("starting playitd" in low) starting = true
            }
            return LogParse(
                when {
                    waiting -> AgentLogState.WAITING
                    starting -> AgentLogState.STARTING
                    else -> AgentLogState.UNKNOWN
                },
                null
            )
        }

        private fun looksLikeSecret(line: String): Boolean {
            if (!line.contains("secret", ignoreCase = true)) return false
            if ("waiting for frontend secret provisioning" in line.lowercase()) return false
            if ("secret provisioning is unavailable" in line.lowercase()) return false
            return true
        }
    }

    open fun checkBinary(): Result<Unit> {
        val abis = (Build.SUPPORTED_ABIS?.toList() ?: emptyList())
        val abi = checkAbi(abis)
        if (abi.isFailure) return abi
        val bin = binaryPath()
        if (!bin.isFile) return Result.failure(IllegalStateException("Binário playit ausente no APK"))
        if (!bin.canExecute()) return Result.failure(
            IllegalStateException("Binário playit sem permissão de execução")
        )
        return Result.success(Unit)
    }

    fun isAlive(): Boolean = process?.alive == true

    fun isAuthenticated(): Boolean = isAlive() && mode == Mode.AUTHENTICATED

    fun startUnclaimed(proxyPort: Int? = null): Result<Unit> {
        if (isAlive()) return Result.success(Unit)
        val chk = checkBinary()
        if (chk.isFailure) return chk
        return try {
            prepareDirs()
            process = starter.start(
                listOf(
                    binaryPath().absolutePath,
                    "--socket-path", socketPath().absolutePath,
                    "--log-path", logPath().absolutePath
                ),
                proxyEnv(proxyPort)
            )
            mode = Mode.UNCLAIMED
            lastExitCode = null
            lastError = null
            Thread.sleep(800)
            if (isAlive()) Result.success(Unit)
            else failStart("encerrou ao iniciar sem secret")
        } catch (e: Exception) {
            failStart(e.message ?: "falha ao iniciar")
        }
    }

    fun start(secret: String, proxyPort: Int? = null): Result<Unit> {
        if (secret.isBlank()) return Result.failure(IllegalStateException("Sem credencial vinculada"))
        if (isAlive() && mode == Mode.AUTHENTICATED) return Result.success(Unit)
        stop()
        val chk = checkBinary()
        if (chk.isFailure) return chk
        return try {
            prepareDirs()

            process = starter.start(
                listOf(
                    binaryPath().absolutePath,
                    "--secret", secret,
                    "--socket-path", socketPath().absolutePath,
                    "--log-path", logPath().absolutePath
                ),
                proxyEnv(proxyPort)
            )
            mode = Mode.AUTHENTICATED
            lastExitCode = null
            lastError = null
            Thread.sleep(800)
            if (isAlive()) Result.success(Unit)
            else failStart("encerrou ao iniciar (ver playitd.log)")
        } catch (e: Exception) {
            failStart(e.message ?: "falha ao iniciar")
        }
    }

    fun stop() {
        try {
            lastExitCode = process?.exitCode()
            process?.stop()
        } catch (_: Exception) { }
        process = null
        mode = Mode.STOPPED
    }

    fun readLogTail(): String {
        return try {
            val f = logPath()
            if (!f.isFile) ""
            else f.readLines().takeLast(60).joinToString("\n")
        } catch (_: Exception) { "" }
    }

    private fun prepareDirs() {
        socketPath().parentFile?.mkdirs()
        logPath().parentFile?.mkdirs()
    }

    private fun proxyEnv(proxyPort: Int?): Map<String, String> {
        if (proxyPort == null || proxyPort <= 0) return emptyMap()
        val v = "http://127.0.0.1:$proxyPort"
        return mapOf(
            "HTTPS_PROXY" to v,
            "HTTP_PROXY" to v,
            "https_proxy" to v,
            "http_proxy" to v
        )
    }

    private fun failStart(reason: String): Result<Unit> {
        lastError = reason
        try { lastExitCode = process?.exitCode() } catch (_: Exception) { }
        try { process?.stop() } catch (_: Exception) { }
        process = null
        mode = Mode.STOPPED
        return Result.failure(IllegalStateException("Agent encerrou: $reason"))
    }
}
