package com.samplocal.manager.util

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

object ZipUtils {
    class UnsafeZipException(message: String) : Exception(message)

    fun safeUnzip(zipFile: File, destDir: File, onEntry: ((String) -> Unit)? = null) {
        destDir.mkdirs()
        val canonicalDest = destDir.canonicalPath
        ZipFile(zipFile).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val e = entries.nextElement()
                var name = e.name.replace('\\', '/')

                if (name.contains("..") || name.startsWith("/") || name.contains(":")) {

                    val test = File(destDir, name).canonicalPath
                    if (!test.startsWith(canonicalDest)) {
                        throw UnsafeZipException("Entrada insegura bloqueada: ${e.name}")
                    }
                    if (name.contains("..")) throw UnsafeZipException("Entrada insegura bloqueada: ${e.name}")
                }
                val out = File(destDir, name)
                if (!out.canonicalPath.startsWith(canonicalDest)) {
                    throw UnsafeZipException("Path traversal bloqueado: ${e.name}")
                }
                if (e.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    zip.getInputStream(e).use { ins ->
                        FileOutputStream(out).use { fos -> ins.copyTo(fos) }
                    }
                    onEntry?.invoke(name)
                }
            }
        }
    }

    fun listTopLevel(zipFile: File): List<String> {
        ZipFile(zipFile).use { zip ->
            return zip.entries().asSequence().map { it.name }.take(200).toList()
        }
    }
}
