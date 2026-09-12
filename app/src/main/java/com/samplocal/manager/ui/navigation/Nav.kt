package com.samplocal.manager.ui.navigation

sealed class Route(val route: String, val label: String) {
    data object Home : Route("home", "Inicio")
    data object Console : Route("console", "Console")
    data object Files : Route("files", "Arquivos")
    data object Settings : Route("settings", "Ajustes")

    data object Connectivity : Route("connectivity", "Conectividade")

    companion object {
        val tabs = listOf(Home, Console, Files, Settings)
    }
}
