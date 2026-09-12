package com.samplocal.manager.runtime

import com.samplocal.manager.data.model.RuntimeBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class EmulatorCheck(val backend: RuntimeBackend, val available: Boolean, val path: String?)

class EmulatorManager(private val linux: LinuxRuntime) {

    fun check(backend: RuntimeBackend): EmulatorCheck {
        val status = linux.checkRuntime()
        return when (backend) {
            RuntimeBackend.QEMU_I386 -> EmulatorCheck(backend, status.backendAvailable, status.backendPath)
            RuntimeBackend.NATIVE -> EmulatorCheck(backend, true, null)
            else -> EmulatorCheck(backend, false, null)
        }
    }

    fun buildCommand(serverDir: File, backend: RuntimeBackend): List<String> {
        val binary = File(serverDir, "samp03svr").absolutePath
        return when (backend) {
            RuntimeBackend.QEMU_I386 -> buildQemuCommand(
                linux.qemuBinary().absolutePath,
                linux.rootfsDir().absolutePath,
                binary
            )
            else -> listOf(binary)
        }
    }

    fun backendEnv(): Map<String, String> = linux.backendEnv()

    suspend fun verifyRuntime(): Result<String> = withContext(Dispatchers.IO) {
        val st = linux.checkRuntime()
        if (!st.ready) {
            return@withContext Result.failure(
                IllegalStateException("Runtime embutido indisponivel: ${st.details}")
            )
        }
        linux.smokeTestBackend()
    }

    fun runtimeErrorMessage(backend: RuntimeBackend): String = when (backend) {
        RuntimeBackend.QEMU_I386 -> "Falha ao preparar o runtime embutido. Tente reinstalar o aplicativo."
        RuntimeBackend.UNSUPPORTED -> "Arquitetura do servidor incompativel com este dispositivo."
        else -> "Backend nao suportado neste dispositivo."
    }

    companion object {
        fun buildQemuCommand(qemuBin: String, rootfs: String, serverBinary: String): List<String> =
            listOf(qemuBin, "-L", rootfs, serverBinary)
    }
}
