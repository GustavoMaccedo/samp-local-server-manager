package com.samplocal.manager.data.model

data class SampConfig(
    val hostname: String = "SAMP Local Server",
    val gamemode0: String = "grandlarc 1",
    val mapname: String = "San Andreas",
    val maxplayers: Int = 50,
    val port: Int = 7777,
    val language: String = "English",
    val weburl: String = "www.sa-mp.com",
    val rconPassword: String = "changeme",
    val announce: Int = 0,
    val query: Int = 1,
    val password: String = "",
    val extra: Map<String, String> = emptyMap()
) {
    fun toCfgText(): String {
        val sb = StringBuilder()
        sb.appendLine("echo Executing Server Config...")
        sb.appendLine("lanmode 0")
        sb.appendLine("hostname $hostname")
        sb.appendLine("gamemode0 $gamemode0")
        sb.appendLine("mapname $mapname")
        sb.appendLine("maxplayers $maxplayers")
        sb.appendLine("port $port")
        sb.appendLine("language $language")
        sb.appendLine("weburl $weburl")
        sb.appendLine("rcon_password $rconPassword")
        sb.appendLine("announce $announce")
        sb.appendLine("query $query")
        if (password.isNotEmpty()) sb.appendLine("password $password")
        for ((k, v) in extra.toSortedMap()) {
            if (v.isEmpty()) sb.appendLine(k) else sb.appendLine("$k $v")
        }
        return sb.toString()
    }

    companion object {
        private val KNOWN = setOf(
            "echo", "lanmode", "hostname", "gamemode0", "mapname",
            "maxplayers", "port", "language", "weburl",
            "rcon_password", "announce", "query", "password"
        )

        fun parse(text: String): SampConfig {
            var hostname = "SAMP Local Server"
            var gamemode0 = "grandlarc 1"
            var mapname = "San Andreas"
            var maxplayers = 50
            var port = 7777
            var language = "English"
            var weburl = "www.sa-mp.com"
            var rcon = "changeme"
            var announce = 0
            var query = 1
            var password = ""
            val extra = LinkedHashMap<String, String>()
            for (raw in text.lines()) {
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue
                val parts = line.split(Regex("\\s+"), limit = 2)
                val key = parts[0].lowercase()
                val value = if (parts.size > 1) parts[1].trim() else ""
                when (key) {
                    "hostname" -> hostname = value
                    "gamemode0" -> gamemode0 = value
                    "mapname" -> mapname = value
                    "maxplayers" -> maxplayers = value.toIntOrNull() ?: maxplayers
                    "port" -> port = value.toIntOrNull() ?: port
                    "language" -> language = value
                    "weburl" -> weburl = value
                    "rcon_password" -> rcon = value
                    "announce" -> announce = value.toIntOrNull() ?: announce
                    "query" -> query = value.toIntOrNull() ?: query
                    "password" -> password = value
                    "echo", "lanmode" -> Unit
                    else -> extra[parts[0]] = value
                }
            }
            return SampConfig(hostname, gamemode0, mapname, maxplayers, port, language, weburl, rcon, announce, query, password, extra)
        }

        fun validate(cfg: SampConfig): List<String> {
            val errors = mutableListOf<String>()
            if (cfg.hostname.isBlank()) errors.add("hostname vazio")
            if (cfg.gamemode0.isBlank()) errors.add("gamemode0 vazio")
            if (cfg.port !in 1..65535) errors.add("porta fora do intervalo 1-65535")
            if (cfg.maxplayers !in 1..1000) errors.add("maxplayers fora do intervalo 1-1000")
            if (cfg.rconPassword.isBlank() || cfg.rconPassword == "changeme") {
                errors.add("defina um rcon_password seguro")
            }
            return errors
        }
    }
}
