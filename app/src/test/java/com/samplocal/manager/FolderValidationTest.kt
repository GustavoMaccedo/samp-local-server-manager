package com.samplocal.manager

import com.samplocal.manager.core.ServerFolderImporter
import org.junit.Assert.*
import org.junit.Test

class FolderValidationTest {

    private fun snap(
        files: Set<String> = emptySet(),
        dirs: Set<String> = emptySet(),
        name: String = "MeuServidor"
    ) = ServerFolderImporter.TreeSnapshot(name, files, dirs)

    @Test
    fun fullServerIsValid() {
        val v = ServerFolderImporter.validate(
            snap(
                files = setOf(
                    "server.cfg", "samp03svr",
                    "gamemodes/gm.amx", "plugins/sscanf.so",
                    "scriptfiles/a.txt", "filterscripts/f.amx"
                ),
                dirs = setOf("gamemodes", "plugins", "scriptfiles", "filterscripts", "npcmodes", "logs")
            ),
            "hostname TEDTE\nport 7777\n"
        )
        assertTrue(v.valid)
        assertTrue(v.hasServerCfg)
        assertTrue(v.hasSampBinary)
        assertEquals(1, v.gamemodeCount)
        assertEquals(1, v.pluginCount)
        assertEquals("TEDTE", v.hostname)
        assertTrue(v.warnings.isEmpty())
        assertEquals(6, v.presentDirs.size)
    }

    @Test
    fun emptyFolderIsInvalid() {
        val v = ServerFolderImporter.validate(snap(), null)
        assertFalse(v.valid)
        assertTrue(v.warnings.isNotEmpty())
    }

    @Test
    fun cfgOnlyIsWeakButValid() {
        val v = ServerFolderImporter.validate(
            snap(files = setOf("server.cfg")),
            "hostname X\n"
        )
        assertTrue(v.valid)
        assertTrue(v.warnings.any { it.contains("gamemode") })
    }

    @Test
    fun gamemodeOnlyCounts() {
        val v = ServerFolderImporter.validate(
            snap(
                files = setOf("GAMEMODES/GM.AMX"),
                dirs = setOf("gamemodes")
            ),
            null
        )
        assertTrue(v.valid)
        assertEquals(1, v.gamemodeCount)
    }

    @Test
    fun windowsExeRecognizedAsBinary() {
        val v = ServerFolderImporter.validate(
            snap(files = setOf("samp-server.exe", "server.cfg")),
            null
        )
        assertTrue(v.valid)
        assertTrue(v.hasSampBinary)
    }

    @Test
    fun cleanServerId() {
        assertEquals("Meu_Servidor", ServerFolderImporter.cleanServerId("Meu Servidor!"))
        assertEquals("TEDTE", ServerFolderImporter.cleanServerId("TEDTE"))
        assertEquals("Servidor", ServerFolderImporter.cleanServerId("???"))
        assertTrue(ServerFolderImporter.cleanServerId("nome-muito-longo-para-um-servidor-qualquer-xyz").length <= 32)
    }
}
