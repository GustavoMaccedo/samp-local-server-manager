package com.samplocal.manager.util

import java.io.File

object ElfParser {
    data class Result(
        val isElf: Boolean,
        val elfClass: Int = 0,
        val machine: Int = 0,
        val interpreter: String? = null,
        val needed: List<String> = emptyList()
    )

    fun parse(file: File): Result {
        if (!file.isFile || file.length() < 64) return Result(false)
        val bytes = ByteArray(4096)
        file.inputStream().use { ins ->
            var read = 0
            while (read < 64) {
                val r = ins.read(bytes, read, 64 - read)
                if (r <= 0) break
                read += r
            }
            if (read < 52) return Result(false)
        }
        if (!(bytes[0] == 0x7F.toByte() && bytes[1] == 'E'.code.toByte() &&
                    bytes[2] == 'L'.code.toByte() && bytes[3] == 'F'.code.toByte())
        ) return Result(false)
        val elfClass = bytes[4].toInt() and 0xFF
        val little = bytes[5].toInt() == 1
        fun u16(off: Int): Int {
            return if (little) {
                (bytes[off].toInt() and 0xFF) or ((bytes[off + 1].toInt() and 0xFF) shl 8)
            } else {
                ((bytes[off].toInt() and 0xFF) shl 8) or (bytes[off + 1].toInt() and 0xFF)
            }
        }
        val machine = u16(18)

        var interp: String? = null
        val needed = mutableListOf<String>()
        try {
            val all = file.inputStream().use { it.readBytes().take(65536).toByteArray() }
            val text = String(all, Charsets.ISO_8859_1)
            val interpIdx = text.indexOf("/lib/ld-linux")
            if (interpIdx >= 0) {
                val end = text.indexOf('\u0000', interpIdx)
                if (end > interpIdx) interp = text.substring(interpIdx, end)
            }
            var idx = 0
            while (true) {
                idx = text.indexOf(".so", idx)
                if (idx < 0) break
                var s = idx
                while (s > 0 && s > idx - 64) {
                    val c = text[s - 1]
                    if (c == '\u0000' || c == '\n') break
                    s--
                }
                val name = text.substring(s, idx + 3).trim().split("/").lastOrNull()
                if (name != null && name.endsWith(".so") || (name != null && name.contains(".so."))) {
                    val clean = name.filter { it.isLetterOrDigit() || it in "._-+ " }.trim().split(" ").lastOrNull()
                    if (!clean.isNullOrBlank() && clean.length < 64 && clean !in needed) {
                        needed.add(clean)
                    }
                }
                idx += 3
            }
        } catch (_: Exception) { }
        return Result(true, elfClass, machine, interp, needed.distinct().take(16))
    }
}
