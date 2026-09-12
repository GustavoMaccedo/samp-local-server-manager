package com.samplocal.manager.runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

data class InstallStep(val label: String, val done: Int, val total: Int)

data class InstallReport(
    val version: String,
    val filesInstalled: Int,
    val bytesTotal: Long,
    val freshInstall: Boolean
)

class RuntimeInstaller(
    private val runtimeDir: File,
    private val assets: AssetSource,
    private val nativeLibDir: File,

    private val paceMs: Long = PACED_INSTALL_MS
) {
    private val _progress = MutableStateFlow<InstallStep?>(null)
    val progress: StateFlow<InstallStep?> = _progress.asStateFlow()

    companion object {

        const val PACED_INSTALL_MS = 170_000L

        const val FINAL_BEAT_MS = 2_500L

        fun installStepKey(label: String): String {
            val l = label.lowercase()
            return when {
                "pronto" in l || "finalizando" in l -> "ready"
                "valid" in l -> "validating"
                "bibliotecas do servidor" in l || "preparando bibliotecas" in l -> "libs"
                "x86" in l || "compatibilidade" in l -> "x86"
                else -> "verifying"
            }
        }
    }

    private val versionFile get() = File(runtimeDir, "version")

    fun installedVersion(): String? = try {
        versionFile.takeIf { it.isFile }?.readText()?.trim()?.takeIf { it.isNotEmpty() }
    } catch (_: Exception) { null }

    fun bundledManifest(): RuntimeManifest = RuntimeManifest.load(assets)

    fun nativeBinary(): File =
        File(nativeLibDir, "libqemu-i386.so")

    private fun criticalAssetPaths() = listOf(
        "rootfs/lib/ld-linux.so.2",
        "rootfs/lib/libc.so.6",
        "rootfs/lib/libstdc++.so.6"
    )

    fun isInstalled(): Boolean {
        return try {
            val manifest = bundledManifest()
            if (installedVersion() != manifest.version) return false
            val qemu = nativeBinary()
            if (!qemu.isFile || !qemu.canExecute()) return false
            criticalAssetPaths().all { File(runtimeDir, it).isFile }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun ensureInstalled(): Result<InstallReport> = withContext(Dispatchers.IO) {
        try {
            val manifest = bundledManifest()
            if (isInstalled() && quickValidate(manifest)) {
                _progress.value = null
                return@withContext Result.success(
                    InstallReport(manifest.version, 0, 0, freshInstall = false)
                )
            }
            install(manifest, fresh = installedVersion() == null)
        } catch (e: Exception) {
            _progress.value = null
            Result.failure(e)
        }
    }

    private fun quickValidate(manifest: RuntimeManifest): Boolean {
        return try {
            val qemu = nativeBinary()
            if (!qemu.isFile || !qemu.canExecute()) return false
            manifest.files.filter { !it.isNativeLib }.all { e ->
                val f = File(runtimeDir, e.path)
                f.isFile && (e.size < 0 || f.length() == e.size)
            }
        } catch (_: Exception) { false }
    }

    suspend fun fullValidate(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val manifest = bundledManifest()
            val badAsset = manifest.files.filter { !it.isNativeLib }.firstOrNull { e ->
                val f = File(runtimeDir, e.path)
                !f.isFile || sha256(f) != e.sha256
            }
            if (badAsset != null) {
                return@withContext Result.failure(IllegalStateException("Arquivo corrompido: ${badAsset.path}"))
            }
            val badNative = manifest.files.filter { it.isNativeLib }.firstOrNull { e ->
                File(nativeLibDir, e.path).let { !it.isFile || !it.canExecute() }
            }
            if (badNative != null) {
                return@withContext Result.failure(
                    IllegalStateException("Binario nativo ausente/sem execucao: ${badNative.path}")
                )
            }
            Result.success("Runtime ${manifest.version}: ${manifest.files.size} arquivos integros.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun install(manifest: RuntimeManifest, fresh: Boolean): Result<InstallReport> =
        withContext(Dispatchers.IO) {
            try {
                val native = manifest.files.filter { it.isNativeLib }
                val groups = listOf(
                    "Verificando dispositivo" to native,
                    "Instalando compatibilidade x86" to manifest.files.filter { it.path.startsWith("lib/") },
                    "Preparando bibliotecas do servidor" to manifest.files.filter { it.path.startsWith("rootfs/") }
                )
                val total = manifest.files.size

                val tailUnits = 5
                val validUnits = 3
                val grandTotal = total + tailUnits
                val copyBudget = if (paceMs > 0) paceMs * total / grandTotal else 0L
                val tailBudget = paceMs - copyBudget
                var done = 0
                var bytes = 0L
                runtimeDir.mkdirs()

                val started = System.currentTimeMillis()
                val perFile = copyBudget / total.coerceAtLeast(1)
                for ((label, entries) in groups) {
                    for (e in entries) {
                        if (e.isNativeLib) verifyNative(e) else copyAndVerify(e)
                        done++
                        bytes += e.size.coerceAtLeast(0)
                        if (paceMs > 0) {
                            val wait = (started + perFile * done) - System.currentTimeMillis()
                            if (wait > 0) delay(wait)
                        }
                        _progress.value = InstallStep(label, done, grandTotal)
                    }
                }

                _progress.value = InstallStep("Validando runtime", total + 1, grandTotal)
                if (!quickValidate(manifest)) {
                    throw IllegalStateException("Validacao pos-instalacao falhou")
                }
                for (v in 2..validUnits) {
                    if (paceMs > 0) {
                        val expected = started + copyBudget + tailBudget * v / tailUnits
                        val wait = expected - System.currentTimeMillis()
                        if (wait > 0) delay(wait)
                    }
                    _progress.value = InstallStep("Validando runtime", total + v, grandTotal)
                }
                versionFile.writeText(manifest.version)

                if (paceMs > 0) {
                    val wait = (started + paceMs) - System.currentTimeMillis()
                    if (wait > 0) delay(wait)
                }
                _progress.value = InstallStep("Ambiente pronto", grandTotal, grandTotal)
                if (paceMs > 0) delay(FINAL_BEAT_MS)
                _progress.value = null
                Result.success(InstallReport(manifest.version, total, bytes, freshInstall = fresh))
            } catch (e: Exception) {
                _progress.value = null
                Result.failure(e)
            }
        }

    private fun verifyNative(entry: RuntimeFileEntry) {
        val f = File(nativeLibDir, entry.path)
        if (!f.isFile) {
            throw IllegalStateException(
                "Binario nativo ausente: ${f.absolutePath} (reinstale o APK)"
            )
        }
        if (!f.canExecute()) {
            throw IllegalStateException(
                "Binario nativo sem permissao de execucao: ${f.absolutePath}"
            )
        }
    }

    private fun copyAndVerify(entry: RuntimeFileEntry) {
        val dest = File(runtimeDir, entry.path)
        if (!dest.canonicalPath.startsWith(runtimeDir.canonicalPath + File.separator)) {
            throw SecurityException("path traversal: ${entry.path}")
        }
        dest.parentFile?.mkdirs()
        assets.open(entry.path).use { ins ->
            dest.outputStream().use { out -> ins.copyTo(out, 262144) }
        }
        if (entry.executable) dest.setExecutable(true)
        if (sha256(dest) != entry.sha256) {
            dest.delete()
            throw IllegalStateException("SHA-256 divergente: ${entry.path}")
        }
    }

    private fun sha256(f: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { ins ->
            val buf = ByteArray(262144)
            while (true) {
                val r = ins.read(buf)
                if (r <= 0) break
                md.update(buf, 0, r)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
