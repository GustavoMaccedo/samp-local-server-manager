package com.samplocal.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.samplocal.manager.ui.navigation.Route
import com.samplocal.manager.ui.screens.AppSettingsScreen
import com.samplocal.manager.ui.screens.ConsoleScreen
import com.samplocal.manager.ui.screens.ConnectivityScreen
import com.samplocal.manager.ui.screens.DashboardScreen
import com.samplocal.manager.ui.screens.FilesScreen
import com.samplocal.manager.ui.screens.SettingsScreen
import com.samplocal.manager.ui.screens.SetupScreen
import com.samplocal.manager.ui.screens.WizardScreen
import com.samplocal.manager.ui.strings.stringsFor
import com.samplocal.manager.ui.theme.SampTheme
import com.samplocal.manager.ui.viewmodel.DashboardViewModel
import com.samplocal.manager.ui.viewmodel.ConnectivityViewModel
import com.samplocal.manager.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()
    private val dash: DashboardViewModel by viewModels()
    private val conn: ConnectivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val dark by vm.appPrefs.darkMode.collectAsState()
            val accent by vm.appPrefs.accentArgb.collectAsState()
            SampTheme(dark = dark, accentArgb = accent) {
                AppRoot(vm, dash, conn)
            }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
private fun AppRoot(vm: MainViewModel, dash: DashboardViewModel, conn: ConnectivityViewModel) {
    val wizardDone by vm.wizardDone.collectAsState()
    val message by vm.message.collectAsState()
    val lang by vm.appPrefs.language.collectAsState()
    val T = remember(lang) { stringsFor(lang) }
    val snackbar = remember { SnackbarHostState() }
    var tab by remember { mutableStateOf(Route.Home.route) }
    var showConn by remember { mutableStateOf(false) }
    var showAppSettings by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); vm.consumeMessage() }
    }

    val setup by vm.setup.collectAsState()
    if (!wizardDone) {

        when (val s = setup) {
            is MainViewModel.SetupState.Idle -> {
                WizardScreen(onStart = { vm.startInstall() }, T = T)
                return
            }
            is MainViewModel.SetupState.Preparing -> {
                SetupScreen(step = s.step, done = s.done, total = s.total, T = T)
                return
            }
            is MainViewModel.SetupState.Failed -> {
                SetupScreen(step = s.step, done = s.done, total = s.total, T = T,
                    error = s.error, onRetry = { vm.retrySetup() })
                return
            }
            MainViewModel.SetupState.Ready ->
                SetupScreen(step = "ready", done = 1, total = 1, T = T)
        }
        return
    }
    when (val s = setup) {
        is MainViewModel.SetupState.Preparing -> {
            SetupScreen(step = s.step, done = s.done, total = s.total, T = T)
            return
        }
        is MainViewModel.SetupState.Failed -> {
            SetupScreen(step = s.step, done = s.done, total = s.total, T = T,
                error = s.error, onRetry = { vm.retrySetup() })
            return
        }
        MainViewModel.SetupState.Ready -> Unit
        MainViewModel.SetupState.Idle -> Unit
    }

    val tabs = listOf(
        Tab(Route.Home.route, T.tabHome, Icons.Filled.Home),
        Tab(Route.Console.route, T.tabConsole, Icons.Filled.Terminal),
        Tab(Route.Files.route, T.tabFiles, Icons.Filled.Folder),
        Tab(Route.Settings.route, T.tabSettings, Icons.Filled.Settings)
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                tabs.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t.route,
                        onClick = { tab = t.route },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) }
                    )
                }
            }
        }
    ) { pad ->
        androidx.compose.foundation.layout.Box(Modifier.padding(pad)) {
            if (showAppSettings) {

                AppSettingsScreen(main = vm, T = T,
                    onBack = { showAppSettings = false })
            } else if (showConn) {
                ConnectivityScreen(main = vm, conn = conn, dash = dash,
                    onBack = { showConn = false }, T = T)
            } else when (tab) {
                Route.Home.route -> DashboardScreen(main = vm, dash = dash,
                    onOpenSettings = { showAppSettings = true },
                    onOpenConnectivity = { showConn = true }, T = T)
                Route.Console.route -> ConsoleScreen(vm, T)
                Route.Files.route -> FilesScreen(vm,
                    onOpenSettings = { tab = Route.Settings.route }, T = T)
                else -> SettingsScreen(vm, T)
            }
        }
    }
}
