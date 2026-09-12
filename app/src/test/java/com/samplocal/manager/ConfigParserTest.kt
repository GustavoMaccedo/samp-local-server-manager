package com.samplocal.manager

import com.samplocal.manager.data.model.SampConfig
import org.junit.Assert.*
import org.junit.Test

class ConfigParserTest {

    @Test
    fun parseBasicCfg() {
        val text = """
            echo Executing Server Config...
            lanmode 0
            hostname BloodZ - Survival
            gamemode0 meu_gamemode 1
            mapname San Andreas
            maxplayers 50
            port 7777
            language Portuguese
            weburl https://example.com
            rcon_password secreta123
            announce 0
            query 1
        """.trimIndent()
        val cfg = SampConfig.parse(text)
        assertEquals("BloodZ - Survival", cfg.hostname)
        assertEquals("meu_gamemode 1", cfg.gamemode0)
        assertEquals(50, cfg.maxplayers)
        assertEquals(7777, cfg.port)
        assertEquals("secreta123", cfg.rconPassword)
    }

    @Test
    fun roundTripPreservesExtra() {
        val cfg = SampConfig(hostname = "X", extra = mapOf("stream_distance" to "300.0", "sleep" to "1"))
        val text = cfg.toCfgText()
        val back = SampConfig.parse(text)
        assertEquals("300.0", back.extra["stream_distance"])
        assertEquals("X", back.hostname)
    }

    @Test
    fun validateDetectsBadPort() {
        val cfg = SampConfig(port = 99999, rconPassword = "ok123456")
        val errors = SampConfig.validate(cfg)
        assertTrue(errors.any { it.contains("porta") })
    }

    @Test
    fun validateWarnsDefaultRcon() {
        val cfg = SampConfig(rconPassword = "changeme")
        assertTrue(SampConfig.validate(cfg).any { it.contains("rcon") })
    }

    @Test
    fun missingGamemodeDetected() {
        val tmp = createTempDir("samp-test")
        try {
            val cm = com.samplocal.manager.samp.ConfigManager()
            cm.writeConfig(tmp, SampConfig(gamemode0 = "inexistente 1", rconPassword = "x1y2z3"))
            val errors = cm.validateConfig(tmp)
            assertTrue(errors.any { it.contains("Gamemode") })
        } finally {
            tmp.deleteRecursively()
        }
    }
}
