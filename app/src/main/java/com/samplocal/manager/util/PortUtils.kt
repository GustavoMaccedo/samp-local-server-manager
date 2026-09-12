package com.samplocal.manager.util

import java.net.ServerSocket

object PortUtils {
    fun isPortAvailable(port: Int): Boolean {
        return try {
            ServerSocket(port).use { true }
        } catch (_: Exception) {
            false
        }
    }
}
