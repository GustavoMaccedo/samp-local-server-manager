package com.samplocal.manager

import com.samplocal.manager.core.FileManager
import com.samplocal.manager.util.ZipUtils
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipImportTest {

    private fun makeZip(entries: Map<String, String>): File {
        val zip = File.createTempFile("srv", ".zip")
        ZipOutputStream(zip.outputStream()).use { zos ->
            entries.forEach { (name, content) ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray())
                zos.closeEntry()
            }
        }
        return zip
    }

    @Test
    fun importValidServer() {
        val zip = makeZip(
            mapOf(
                "samp03svr" to "fakebinary",
                "server.cfg" to "hostname X\nport 7777\n",
                "gamemodes/test.amx" to "amx",
                "plugins/sscanf.so" to "so"
            )
        )
        val dest = createTempDir("srv-dest")
        try {
            val fm = FileManager()
            val report = fm.importServerZip(zip, dest, "Default")
            assertTrue(report.foundBinary)
            assertTrue(report.foundConfig)
            assertEquals(1, report.gamemodes)
            assertEquals(1, report.plugins)
            assertTrue(File(dest, "logs").exists())
        } finally {
            zip.delete(); dest.deleteRecursively()
        }
    }

    @Test
    fun importWithoutBinaryWarns() {
        val zip = makeZip(mapOf("server.cfg" to "hostname X"))
        val dest = createTempDir("srv-dest2")
        try {
            val report = FileManager().importServerZip(zip, dest, "S2")
            assertFalse(report.foundBinary)
            assertTrue(report.warnings.any { it.contains("samp03svr") })
        } finally {
            zip.delete(); dest.deleteRecursively()
        }
    }

    @Test(expected = ZipUtils.UnsafeZipException::class)
    fun pathTraversalBlocked() {
        val zip = makeZip(mapOf("../../evil.sh" to "evil", "samp03svr" to "x"))
        val dest = createTempDir("srv-evil")
        try {
            FileManager().importServerZip(zip, dest, "Evil")
        } finally {
            zip.delete(); dest.deleteRecursively()
        }
    }

    @Test
    fun nestedRootFlattened() {
        val zip = makeZip(
            mapOf(
                "MeuServidor/samp03svr" to "bin",
                "MeuServidor/server.cfg" to "hostname Y"
            )
        )
        val dest = createTempDir("srv-nested")
        try {
            val report = FileManager().importServerZip(zip, dest, "N")
            assertTrue(report.foundBinary)
            assertTrue(File(dest, "samp03svr").exists())
        } finally {
            zip.delete(); dest.deleteRecursively()
        }
    }
}
