package com.samplocal.manager

import com.samplocal.manager.samp.GamemodeManager
import com.samplocal.manager.samp.PluginManager
import com.samplocal.manager.util.PortUtils
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.net.ServerSocket

class GamemodePluginTest {

    @Test
    fun installAndListGamemode() {
        val serverDir = createTempDir("gm-test")
        try {
            val src = File.createTempFile("mode", ".amx")
            src.writeBytes(ByteArray(100) { 1 })
            val gm = GamemodeManager().installGamemode(serverDir, src, "meu_gamemode")
            assertEquals("meu_gamemode", gm.name)
            assertEquals(1, GamemodeManager().listGamemodes(serverDir).size)
            src.delete()
        } finally {
            serverDir.deleteRecursively()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectNonAmx() {
        val serverDir = createTempDir("gm-bad")
        try {
            val src = File.createTempFile("mode", ".txt")
            src.writeText("x")
            GamemodeManager().installGamemode(serverDir, src, null)
        } finally {
            serverDir.deleteRecursively()
        }
    }

    @Test
    fun pluginDetectCrossCheck() {
        val serverDir = createTempDir("plug-test")
        try {
            File(serverDir, "plugins").mkdirs()
            File(serverDir, "plugins/sscanf.so").writeBytes(ByteArray(10))
            val pm = PluginManager()
            val detected = pm.detectPlugins(serverDir, "plugins sscanf.so missing.so")
            assertEquals(2, detected.size)
            assertTrue(detected.first { it.file == "sscanf.so" }.enabled)
            assertFalse(detected.first { it.file == "missing.so" }.enabled)
        } finally {
            serverDir.deleteRecursively()
        }
    }

    @Test
    fun portCheckWorks() {

        val s = ServerSocket(0)
        val free = s.localPort
        s.close()
        Thread.sleep(100)
        assertTrue(PortUtils.isPortAvailable(free))
    }

    @Test
    fun occupiedPortDetected() {
        val s = ServerSocket(0)
        try {
            assertFalse(PortUtils.isPortAvailable(s.localPort))
        } finally {
            s.close()
        }
    }
}
