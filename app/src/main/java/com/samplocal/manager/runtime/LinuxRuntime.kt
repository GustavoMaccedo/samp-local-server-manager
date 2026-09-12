package com.samplocal.manager.runtime

import android.content.Context
import com.samplocal.manager.data.model.GuestArchitecture
import com.samplocal.manager.data.model.RuntimeBackend
import com.samplocal.manager.data.model.RuntimeStatus
import java.io.File
import java.util.concurrent.TimeUnit

class LinuxRuntime(private val context: Context) {

    fun baseDir(): File = File(context.filesDir, "runtime")
    fun nativeLibDir(): File = File(context.applicationInfo.nativeLibraryDir)

    fun qemuBinary(): File = File(nativeLibDir(), "libqemu-i386.so")

    fun qemuLibDir(): File = File(baseDir(), "lib/qemu")
    fun rootfsDir(): File = File(baseDir(), "rootfs")
    fun serversDir(): File = File(context.filesDir, "servers").apply { mkdirs() }
    fun serverDir(id: String): File = File(serversDir(), id).apply { mkdirs() }
    fun backupsDir(): File = File(context.filesDir, "backups").apply { mkdirs() }

    fun installer(): RuntimeInstaller =
        RuntimeInstaller(baseDir(), AndroidAssetSource(context), nativeLibDir())

    fun backendEnv(): Map<String, String> =
        mapOf("LD_LIBRARY_PATH" to qemuLibDir().absolutePath)

    fun checkRuntime(): RuntimeStatus {
        val host = ArchitectureDetector.detectHostArchitecture()
        val guest = GuestArchitecture.X86_32
        val backend = ArchitectureDetector.detectRequiredRuntime(host, guest)
        val installer = installer()
        val installed = installer.isInstalled()
        val version = installer.installedVersion()
        val qemu = qemuBinary()
        val backendOk = installed && qemu.isFile && qemu.canExecute()
        val ready = backend == RuntimeBackend.QEMU_I386 && backendOk
        val details = buildString {
            append("host=$host guest=$guest backend=$backend ")
            append("runtime=${if (installed) "instalado v$version" else "ausente"} ")
            append("qemu=${if (backendOk) "OK" else "indisponivel"} ")
            append("serversDir=${serversDir().absolutePath}")
        }
        return RuntimeStatus(
            hostArch = host,
            guestArch = guest,
            backend = backend,
            box86Available = false,
            box64Available = false,
            ready = ready,
            details = details,
            backendAvailable = backendOk,
            backendPath = qemu.absolutePath.takeIf { backendOk },
            runtimeInstalled = installed,
            runtimeVersion = version
        )
    }

    fun diagnoseBackend(): String {
        val sb = StringBuilder()
        val qemu = qemuBinary()
        sb.appendLine("qemu path: ${qemu.absolutePath}")
        sb.appendLine("exists: ${qemu.exists()}")
        sb.appendLine("canRead: ${qemu.canRead()}")
        sb.appendLine("canExecute: ${qemu.canExecute()}")
        sb.appendLine("size: ${if (qemu.exists()) qemu.length().toString() else "-"}")
        sb.appendLine("perms: ${posixPerms(qemu)}")
        sb.appendLine("nativeLibDir: ${nativeLibDir().absolutePath}")
        sb.appendLine("elf: ${elfLine(qemu)}")
        val abis = try {
            android.os.Build.SUPPORTED_ABIS?.joinToString(",") ?: "null"
        } catch (_: Exception) { "?" }
        sb.appendLine("device ABIs: $abis")
        sb.appendLine("sdk: ${android.os.Build.VERSION.SDK_INT}")
        val smoke = smokeTestBackend()
        sb.appendLine(
            "exec --version: " + (smoke.getOrNull() ?: "FALHOU: ${smoke.exceptionOrNull()?.message}")
        )
        return sb.toString()
    }

    private fun posixPerms(f: File): String {
        return try {
            java.nio.file.Files.getPosixFilePermissions(f.toPath()).toString()
        } catch (_: Exception) {
            if (!f.exists()) "-" else "r=${f.canRead()} w=${f.canWrite()} x=${f.canExecute()}"
        }
    }

    private fun elfLine(f: File): String {
        if (!f.isFile) return "ausente"
        return try {
            f.inputStream().use { ins ->
                val head = ByteArray(64)
                var n = 0
                while (n < 64) {
                    val r = ins.read(head, n, 64 - n)
                    if (r <= 0) break
                    n += r
                }
                if (n < 20) return "curto"
                val elf = head[0] == 0x7F.toByte() && head[1] == 'E'.code.toByte() &&
                    head[2] == 'L'.code.toByte() && head[3] == 'F'.code.toByte()
                if (!elf) return "nao-ELF"
                val cls = if (head[4].toInt() == 2) "ELF64" else "ELF32"
                val machine = (head[18].toInt() and 0xFF) or ((head[19].toInt() and 0xFF) shl 8)
                val arch = when (machine) {
                    183 -> "AArch64"; 62 -> "x86-64"; 3 -> "i386"; 40 -> "ARM"; else -> "machine=$machine"
                }
                val raw = String(head, Charsets.ISO_8859_1)
                val interp = Regex("/[^\\u0000]{1,64}").findAll(raw)
                    .map { it.value }.firstOrNull { "linker" in it || "ld-linux" in it } ?: "-"
                "$cls $arch interp=$interp"
            }
        } catch (e: Exception) {
            "erro: ${e.message}"
        }
    }
    fun smokeTestBackend(timeoutSec: Long = 15): Result<String> {
        return try {
            val qemu = qemuBinary()
            if (!qemu.isFile || !qemu.canExecute()) {
                return Result.failure(IllegalStateException("Backend QEMU ausente em ${qemu.absolutePath}"))
            }
            val pb = ProcessBuilder(listOf(qemu.absolutePath, "--version"))
                .redirectErrorStream(true)
            pb.environment()["LD_LIBRARY_PATH"] = qemuLibDir().absolutePath
            val p = pb.start()
            val out = p.inputStream.bufferedReader().readText()
            val finished = p.waitFor(timeoutSec, TimeUnit.SECONDS)
            if (!finished) {
                p.destroyForcibly()
                return Result.failure(IllegalStateException("Backend nao respondeu em ${timeoutSec}s"))
            }
            if (p.exitValue() == 0 && out.contains("qemu-i386")) Result.success(out.lineSequence().firstOrNull().orEmpty())
            else Result.failure(IllegalStateException("Backend falhou (exit=${p.exitValue()}): ${out.take(300)}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
