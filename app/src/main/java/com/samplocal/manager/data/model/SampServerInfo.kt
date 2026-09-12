package com.samplocal.manager.data.model

data class SampServerInfo(
    val id: String,
    val name: String,
    val port: Int = 7777,
    val maxPlayers: Int = 50,
    val hostname: String = "SAMP Local Server",
    val gamemode: String = "",
    val status: ServerStatus = ServerStatus.STOPPED,
    val pid: Int? = null,
    val uptimeSec: Long = 0L,
    val playerCount: Int = 0,
    val lastError: String? = null
)
