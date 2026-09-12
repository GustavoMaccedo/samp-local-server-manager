package com.samplocal.manager

import com.samplocal.manager.runtime.DirAssetSource
import com.samplocal.manager.samp.SampBinaryProvider
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class SampBinaryTest {

    private val realAssets = File("src/main/assets")
    private val sha = "0cdfa430392986b3a60f0b3bf6a641175679c3285fb452e1bdcf63321cfd9873"

    @Test
    fun officialAssetMatchesKnownHash() {

        val f = File(realAssets, SampBinaryProvider.ASSET_PATH)
        assertTrue("asset oficial ausente em ${f.path}", f.isFile)
        val p = SampBinaryProvider(DirAssetSource(realAssets))
        val expect = p.expected()
        assertEquals(sha, expect.sha256)
        assertEquals(1416856L, expect.size)
        assertTrue(p.validate(f).isSuccess)
    }

    @Test
    fun existingBinaryIsNeverReplaced() {
        val dir = createTempDir("srv-with-bin")
        try {
            val dest = File(dir, "samp03svr")
            File(realAssets, "svrmaneger/samp03svr").copyTo(dest)
            val before = dest.lastModified()
            Thread.sleep(1100)
            val r = SampBinaryProvider(DirAssetSource(realAssets)).ensurePresent(dir)
            assertTrue(r is SampBinaryProvider.EnsureResult.AlreadyValid)
            assertEquals(before, dest.lastModified())
            assertEquals(sha, shaOf(dest))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun missingBinaryInstalledAndValidated() {
        val dir = createTempDir("srv-no-bin")
        try {
            File(dir, "server.cfg").writeText("hostname X\n")
            val r = SampBinaryProvider(DirAssetSource(realAssets)).ensurePresent(dir)
            assertTrue(r is SampBinaryProvider.EnsureResult.Installed)
            val dest = File(dir, "samp03svr")
            assertTrue(dest.isFile)
            assertEquals(sha, shaOf(dest))

            assertEquals("hostname X\n", File(dir, "server.cfg").readText())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun missingOfficialFailsClearly() {
        val emptyAssets = createTempDir("empty-assets")
        val dir = createTempDir("srv-no-official")
        try {
            val r = SampBinaryProvider(DirAssetSource(emptyAssets)).ensurePresent(dir)
            assertTrue(r is SampBinaryProvider.EnsureResult.Failed)
            val reason = (r as SampBinaryProvider.EnsureResult.Failed).reason
            assertTrue(reason.contains("pacote do aplicativo"))
            assertFalse(File(dir, "samp03svr").exists())
        } finally {
            emptyAssets.deleteRecursively(); dir.deleteRecursively()
        }
    }

    @Test
    fun writeFailureNotMarkedInstalled() {
        val dir = createTempDir("srv-write-fail")
        try {

            val blocker = File.createTempFile("blocker", null)
            try {
                val r = SampBinaryProvider(DirAssetSource(realAssets)).ensurePresent(blocker)
                assertTrue(r is SampBinaryProvider.EnsureResult.Failed)
            } finally {
                blocker.delete()
            }
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun corruptedCopyFailsValidation() {
        val dir = createTempDir("srv-corrupt")
        try {
            val p = SampBinaryProvider(DirAssetSource(realAssets))
            assertTrue(p.ensurePresent(dir) is SampBinaryProvider.EnsureResult.Installed)
            val dest = File(dir, "samp03svr")

            val b = dest.readBytes()
            b[100] = (b[100] + 1).toByte()
            dest.writeBytes(b)
            assertTrue(p.validate(dest).isFailure)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun restartStillDetectsInstalled() {
        val dir = createTempDir("srv-restart")
        try {
            SampBinaryProvider(DirAssetSource(realAssets)).ensurePresent(dir)

            val r2 = SampBinaryProvider(DirAssetSource(realAssets)).ensurePresent(dir)
            assertTrue(r2 is SampBinaryProvider.EnsureResult.AlreadyValid)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun presentInvalidIsPreservedAndReported() {
        val dir = createTempDir("srv-invalid")
        try {
            File(dir, "samp03svr").writeText("nao-e-um-binario")
            val r = SampBinaryProvider(DirAssetSource(realAssets)).ensurePresent(dir)
            assertTrue(r is SampBinaryProvider.EnsureResult.PresentInvalid)

            assertEquals("nao-e-um-binario", File(dir, "samp03svr").readText())
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun shaOf(f: File): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        return md.digest(f.readBytes()).joinToString("") { "%02x".format(it) }
    }
}
