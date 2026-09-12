package com.samplocal.manager.samp

import com.samplocal.manager.runtime.AssetSource
import com.samplocal.manager.util.ElfParser
import java.io.File
import java.security.MessageDigest

class SampBinaryProvider(
    private val assets: AssetSource,
    private val assetPath: String = ASSET_PATH
) {
    companion object {

        const val ASSET_PATH = "svrmaneger/samp03svr"
    }
    sealed interface EnsureResult {

        data object AlreadyValid : EnsureResult

        data class PresentInvalid(val reason: String) : EnsureResult

        data object Installed : EnsureResult

        data class Failed(val reason: String) : EnsureResult
    }

    data class Expect(val sha256: String, val size: Long)

    fun expected(): Expect {
        val text = try {
            assets.open("$assetPath.sha256").use { it.readBytes().toString(Charsets.UTF_8) }
        } catch (_: Exception) {
            throw IllegalStateException(
                "O executável samp03svr não está disponível no pacote do aplicativo."
            )
        }
        val parts = text.trim().split(Regex("\\s+"))
        require(parts.size >= 2 && parts[0].length == 64) {
            "O executável samp03svr não está disponível no pacote do aplicativo."
        }
        return Expect(parts[0].lowercase(), parts[1].toLong())
    }

    fun target(serverDir: File): File = File(serverDir, "samp03svr")

    fun validate(file: File, expect: Expect = expected()): Result<Unit> {
        if (!file.isFile) return Result.failure(IllegalStateException("samp03svr ausente"))
        if (file.length() <= 0) return Result.failure(IllegalStateException("samp03svr vazio"))
        if (file.length() != expect.size) {
            return Result.failure(
                IllegalStateException("tamanho divergente (esperado ${expect.size}, obtido ${file.length()})")
            )
        }
        if (sha256(file) != expect.sha256) {
            return Result.failure(IllegalStateException("SHA-256 divergente"))
        }
        val elf = ElfParser.parse(file)
        if (!elf.isElf || elf.machine != 3 || elf.elfClass != 1) {
            return Result.failure(IllegalStateException("ELF invalido (esperado i386 32-bit)"))
        }
        return Result.success(Unit)
    }

    fun ensurePresent(serverDir: File): EnsureResult {
        val expect = try {
            expected()
        } catch (e: Exception) {
            return EnsureResult.Failed(e.message ?: "pacote sem samp03svr")
        }
        val dest = target(serverDir)
        if (dest.isFile) {
            val v = validate(dest, expect)
            return if (v.isSuccess) EnsureResult.AlreadyValid
            else EnsureResult.PresentInvalid(v.exceptionOrNull()?.message ?: "invalido")
        }
        return try {
            serverDir.mkdirs()
            assets.open(assetPath).use { ins ->
                dest.outputStream().use { out -> ins.copyTo(out) }
            }
            dest.setExecutable(true)
            val v = validate(dest, expect)
            if (v.isSuccess) EnsureResult.Installed
            else {
                dest.delete()
                EnsureResult.Failed(v.exceptionOrNull()?.message ?: "validacao falhou")
            }
        } catch (e: Exception) {
            try { dest.delete() } catch (_: Exception) { }
            EnsureResult.Failed(e.message ?: "falha de escrita")
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
