package com.samplocal.manager

import com.samplocal.manager.runtime.EmulatorManager
import com.samplocal.manager.runtime.RuntimeManifest
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class BundledRuntimeTest {

    @Test
    fun realManifestParsesAndCoversSampDeps() {
        val manifestFile = File("src/main/assets/runtime/manifest.json")
        assertTrue("manifest do payload ausente: ${manifestFile.absolutePath}", manifestFile.isFile)
        val manifest = RuntimeManifest.parse(manifestFile.readText())
        assertEquals("1.0.0", manifest.version)
        assertEquals("qemu-i386", manifest.backend)
        val byPath = manifest.files.associateBy { it.path }

        val native = byPath["libqemu-i386.so"]
        assertNotNull(native)
        assertTrue(native!!.isNativeLib && native.executable)
        val jniFile = File("src/main/jniLibs/arm64-v8a/libqemu-i386.so")
        assertTrue("jniLib ausente", jniFile.isFile)
        assertEquals(native.size, jniFile.length())
        assertTrue(manifest.files.size >= 30)
        for (need in listOf("rootfs/lib/ld-linux.so.2", "rootfs/lib/libc.so.6", "rootfs/lib/libstdc++.so.6")) {
            val e = byPath[need]
            assertNotNull(e)
            assertFalse(e!!.isNativeLib)
        }

        assertTrue(manifest.files.none { it.path.startsWith("/") || ".." in it.path })

        manifest.files.filter { !it.isNativeLib }.forEach { e ->
            val f = File("src/main/assets/runtime", e.path)
            assertTrue("asset ausente: ${e.path}", f.isFile)
            assertEquals("tamanho divergente: ${e.path}", e.size, f.length())
        }

        assertTrue(File("src/main/assets/runtime/bin/qemu-i386").exists().not())
    }

    @Test(expected = IllegalArgumentException::class)
    fun manifestWithoutVersionRejected() {
        RuntimeManifest.parse("""{"backend":"qemu-i386","files":[]}""")
    }

    @Test(expected = IllegalArgumentException::class)
    fun manifestWithTraversalRejected() {
        RuntimeManifest.parse(
            """{"version":"1","files":[{"path":"../evil","sha256":"${"a".repeat(64)}","size":1}]}"""
        )
    }

    @Test
    fun qemuCommandUsesBundledPaths() {
        val cmd = EmulatorManager.buildQemuCommand(
            "/data/user/0/app/files/runtime/bin/qemu-i386",
            "/data/user/0/app/files/runtime/rootfs",
            "/data/user/0/app/files/servers/Default/samp03svr"
        )
        assertEquals(
            listOf(
                "/data/user/0/app/files/runtime/bin/qemu-i386",
                "-L",
                "/data/user/0/app/files/runtime/rootfs",
                "/data/user/0/app/files/servers/Default/samp03svr"
            ),
            cmd
        )
    }

    @Test
    fun pidIsNullWhenNoProcess() {
        val pm = com.samplocal.manager.core.ProcessManager()
        assertFalse(pm.isAlive())
        assertNull(pm.getPid())
    }
}
