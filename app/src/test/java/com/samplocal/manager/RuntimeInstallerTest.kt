package com.samplocal.manager

import com.samplocal.manager.runtime.DirAssetSource
import com.samplocal.manager.runtime.RuntimeInstaller
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class RuntimeInstallerTest {

    private fun sha(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private class FakeEnv(val assetsRoot: File, val target: File, val nativeDir: File)

    private fun fakeEnv(version: String): FakeEnv {
        val assetsRoot = createTempDir("fake-assets")
        fun asset(rel: String, bytes: ByteArray, exec: Boolean = false, source: String = "asset"): String {
            if (source == "asset") {
                File(assetsRoot, rel).apply { parentFile.mkdirs(); writeBytes(bytes) }
            }
            return """{"path":"$rel","sha256":"${sha(bytes)}","size":${bytes.size},"executable":$exec,"source":"$source"}"""
        }
        val nativeDir = createTempDir("fake-native")
        val qemuBytes = "fake-qemu".toByteArray()
        File(nativeDir, "libqemu-i386.so").apply {
            writeBytes(qemuBytes)

            setExecutable(true)
        }
        val manifest = """{"version":"$version","backend":"qemu-i386","backendVersion":"t",
"files":[${asset("rootfs/lib/libc.so.6", "fake-libc".toByteArray())},
${asset("rootfs/lib/libstdc++.so.6", "fake-stdcxx".toByteArray())},
${asset("rootfs/lib/ld-linux.so.2", "fake-ld".toByteArray())},
${asset("libqemu-i386.so", qemuBytes, exec = true, source = "nativelib")}]}"""
        File(assetsRoot, "manifest.json").writeText(manifest)
        return FakeEnv(assetsRoot, createTempDir("rt-target"), nativeDir)
    }

    private fun installer(env: FakeEnv) =
        RuntimeInstaller(env.target, DirAssetSource(env.assetsRoot), env.nativeDir, paceMs = 0)

    private fun cleanup(env: FakeEnv) {
        env.assetsRoot.deleteRecursively(); env.target.deleteRecursively(); env.nativeDir.deleteRecursively()
    }

    @Test
    fun freshInstallExtractsAndMarksVersion() = runTest {
        val env = fakeEnv("9.9.9")
        try {
            val installer = installer(env)
            assertFalse(installer.isInstalled())
            val report = installer.ensureInstalled().getOrThrow()
            assertTrue(report.freshInstall)
            assertEquals(4, report.filesInstalled)
            assertEquals("9.9.9", File(env.target, "version").readText())
            assertEquals("fake-libc", File(env.target, "rootfs/lib/libc.so.6").readText())
            assertTrue(installer.isInstalled())
        } finally {
            cleanup(env)
        }
    }

    @Test
    fun secondRunSkipsExtraction() = runTest {
        val env = fakeEnv("1.2.3")
        try {
            val installer = installer(env)
            installer.ensureInstalled().getOrThrow()
            val second = installer.ensureInstalled().getOrThrow()
            assertFalse(second.freshInstall)
            assertEquals(0, second.filesInstalled)
        } finally {
            cleanup(env)
        }
    }

    @Test
    fun corruptedFileTriggersRepair() = runTest {
        val env = fakeEnv("2.0.0")
        try {
            val installer = installer(env)
            installer.ensureInstalled().getOrThrow()

            File(env.target, "rootfs/lib/libc.so.6").appendBytes("CORROMPIDO".toByteArray())
            val repaired = installer.ensureInstalled().getOrThrow()
            assertTrue(repaired.filesInstalled > 0)
            assertEquals("fake-libc", File(env.target, "rootfs/lib/libc.so.6").readText())
        } finally {
            cleanup(env)
        }
    }

    @Test
    fun versionBumpReinstalls() = runTest {
        val env = fakeEnv("3.0.0")
        try {
            installer(env).ensureInstalled().getOrThrow()
            assertEquals("3.0.0", File(env.target, "version").readText())

            val libc = File(env.assetsRoot, "rootfs/lib/libc.so.6")
            libc.writeBytes("fake-libc-v2".toByteArray())
            val b = libc.readBytes()
            val qemuBytes = File(env.nativeDir, "libqemu-i386.so").readBytes()
            File(env.assetsRoot, "manifest.json").writeText(
                """{"version":"3.1.0","files":[
{"path":"rootfs/lib/libc.so.6","sha256":"${sha(b)}","size":${b.size},"source":"asset"},
{"path":"libqemu-i386.so","sha256":"${sha(qemuBytes)}","size":${qemuBytes.size},"executable":true,"source":"nativelib"}]}"""
            )
            val installer2 = installer(env)
            assertFalse(installer2.isInstalled())
            installer2.ensureInstalled().getOrThrow()
            assertEquals("3.1.0", File(env.target, "version").readText())
            assertEquals("fake-libc-v2", File(env.target, "rootfs/lib/libc.so.6").readText())
        } finally {
            cleanup(env)
        }
    }

    @Test
    fun fullValidateDetectsTamper() = runTest {
        val env = fakeEnv("4.0.0")
        try {
            val installer = installer(env)
            installer.ensureInstalled().getOrThrow()
            assertTrue(installer.fullValidate().isSuccess)

            val f = File(env.target, "rootfs/lib/libc.so.6")
            val b = f.readBytes()
            b[0] = (b[0] + 1).toByte()
            f.writeBytes(b)
            assertTrue(installer.fullValidate().isFailure)
        } finally {
            cleanup(env)
        }
    }

    @Test
    fun missingNativeBinaryFailsWithClearMessage() = runTest {
        val env = fakeEnv("5.0.0")
        try {
            File(env.nativeDir, "libqemu-i386.so").delete()
            val installer = installer(env)
            assertFalse(installer.isInstalled())
            val err = installer.ensureInstalled().exceptionOrNull()?.message.orEmpty()
            assertTrue(err.contains("nativo"))
        } finally {
            cleanup(env)
        }
    }

    @Test
    fun pacedInstallTargetIsTwoMinutesFifty() {
        assertEquals(170_000L, RuntimeInstaller.PACED_INSTALL_MS)
    }

    @Test
    fun installStepKeyMapsRealLabels() {
        assertEquals("verifying", RuntimeInstaller.installStepKey("Verificando dispositivo"))
        assertEquals("x86", RuntimeInstaller.installStepKey("Instalando compatibilidade x86"))
        assertEquals("libs", RuntimeInstaller.installStepKey("Preparando bibliotecas do servidor"))
        assertEquals("validating", RuntimeInstaller.installStepKey("Validando runtime"))
        assertEquals("ready", RuntimeInstaller.installStepKey("Ambiente pronto"))
        assertEquals("verifying", RuntimeInstaller.installStepKey("qualquer outra coisa"))
    }
}
