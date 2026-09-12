package com.samplocal.manager.runtime

import android.content.Context
import com.samplocal.manager.data.model.RuntimeStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

class RuntimeManager(private val context: Context) {
    private val linux = LinuxRuntime(context)
    private val emu = EmulatorManager(linux)
    private val deps = DependencyManager(context)
    private val installer = linux.installer()

    val installerProgress: StateFlow<InstallStep?> get() = installer.progress
    val dependencyProgress get() = deps.progress

    suspend fun checkRuntime(): RuntimeStatus = withContext(Dispatchers.IO) {
        linux.checkRuntime()
    }

    fun getRuntimeStatus(): RuntimeStatus = linux.checkRuntime()

    suspend fun ensureInstalled(): Result<RuntimeStatus> = withContext(Dispatchers.IO) {
        linux.serversDir()
        linux.backupsDir()
        val installed = installer.ensureInstalled()
        if (installed.isFailure) {
            return@withContext Result.failure(
                installed.exceptionOrNull() ?: IllegalStateException("Falha ao instalar runtime")
            )
        }
        val smoke = linux.smokeTestBackend()
        if (smoke.isFailure) {
            return@withContext Result.failure(
                IllegalStateException("Backend instalado mas nao executa: ${smoke.exceptionOrNull()?.message}")
            )
        }
        val status = linux.checkRuntime()
        if (!status.ready) Result.failure(IllegalStateException("Runtime invalido apos instalacao: ${status.details}"))
        else Result.success(status)
    }

    suspend fun installRuntime(): List<String> {
        ensureInstalled().getOrThrow()
        return listOf("Diretorios", "Runtime embutido", "Compatibilidade x86", "Validacao")
    }

    suspend fun verifyRuntime(): Result<String> = emu.verifyRuntime()

    suspend fun fullValidate(): Result<String> = withContext(Dispatchers.IO) {
        installer.fullValidate()
    }

    suspend fun smokeTest(): Result<String> = withContext(Dispatchers.IO) {
        linux.smokeTestBackend()
    }

    suspend fun diagnose(): String = withContext(Dispatchers.IO) {
        linux.diagnoseBackend()
    }

    data class LibsStatus(val checked: Int, val missing: List<String>, val all: List<String> = emptyList())

    suspend fun librariesStatus(): LibsStatus = withContext(Dispatchers.IO) {
        try {
            val manifest = installer.bundledManifest()
            val libs = manifest.files.filter {
                !it.isNativeLib && (it.path.startsWith("lib/") || it.path.startsWith("rootfs/lib"))
            }
            val base = linux.baseDir()
            val missing = libs.filterNot { File(base, it.path).let { f -> f.isFile && f.length() > 0 } }
                .map { it.path.substringAfterLast("/") }
            LibsStatus(libs.size, missing, libs.map { it.path.substringAfterLast("/") })
        } catch (_: Exception) {
            LibsStatus(0, listOf("manifest"))
        }
    }

    fun sampBinary(serverId: String): File =
        File(linux.serverDir(serverId), "samp03svr")

    fun serverDir(serverId: String): File = linux.serverDir(serverId)
    fun serversDir(): File = linux.serversDir()

    fun buildStartCommand(serverId: String): List<String> {
        val status = linux.checkRuntime()
        return emu.buildCommand(linux.serverDir(serverId), status.backend)
    }

    fun backendEnv(): Map<String, String> = emu.backendEnv()
}
