package com.samplocal.manager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samplocal.manager.data.model.ServerStatus
import com.samplocal.manager.runtime.ArchitectureDetector
import com.samplocal.manager.ui.components.LineChart
import com.samplocal.manager.ui.components.StatusBadge
import com.samplocal.manager.ui.theme.Divider
import com.samplocal.manager.ui.theme.Green
import com.samplocal.manager.ui.theme.Red
import com.samplocal.manager.ui.theme.Surface
import com.samplocal.manager.ui.theme.TextDim
import com.samplocal.manager.ui.theme.TextMain
import com.samplocal.manager.ui.viewmodel.DashboardViewModel
import com.samplocal.manager.ui.strings.Strings
import com.samplocal.manager.ui.strings.statusText
import com.samplocal.manager.ui.viewmodel.MainViewModel

@Composable
fun DashboardScreen(
    main: MainViewModel,
    dash: DashboardViewModel,
    onOpenSettings: () -> Unit,
    onOpenConnectivity: () -> Unit,
    T: Strings
) {
    val servers by main.servers.collectAsState()
    val selected by main.selected.collectAsState()

    DisposableEffect(Unit) {
        dash.bind(main.selected)
        dash.start()
        onDispose { dash.stop() }
    }

    val server by dash.server.collectAsStateWithLifecycle()
    val uptime by dash.uptimeSec.collectAsStateWithLifecycle()
    val cpu by dash.cpuPct.collectAsStateWithLifecycle()
    val memMb by dash.memMb.collectAsStateWithLifecycle()
    val memPct by dash.memPct.collectAsStateWithLifecycle()
    val cpuHist by dash.cpuHistory.collectAsStateWithLifecycle()
    val memHist by dash.memHistory.collectAsStateWithLifecycle()
    val live by dash.live.collectAsStateWithLifecycle()
    val version by dash.version.collectAsStateWithLifecycle()

    val status = server?.status ?: ServerStatus.STOPPED
    val running = status == ServerStatus.RUNNING
    val busy = status == ServerStatus.STARTING || status == ServerStatus.STOPPING ||
        status == ServerStatus.INSTALLING
    val port = server?.port ?: 7777
    val cfg = remember(selected, status) { main.readConfig() }
    val hostArch = remember { ArchitectureDetector.detectHostArchitecture().toString() }

    val playersNow = live?.players
    val playersText = when {
        playersNow != null -> "$playersNow / ${live?.maxPlayers ?: server?.maxPlayers ?: 50}"
        running -> "… / ${server?.maxPlayers ?: 50}"
        else -> "0 / ${server?.maxPlayers ?: 50}"
    }
    val passwordOn = live?.passworded ?: cfg.password.isNotEmpty()
    val language = live?.language?.ifBlank { null } ?: cfg.language.ifBlank { null } ?: "N/A"
    val map = cfg.mapname.ifBlank { null } ?: "N/A"

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("SAMP LOCAL", color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f))
            IconButton(onClick = onOpenConnectivity) {
                Icon(Icons.Filled.Lan, contentDescription = T.dConnDesc,
                    tint = TextDim)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = T.dOpenSettings, tint = TextDim)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
            var expanded by remember { mutableStateOf(false) }
            var showCreate by remember { mutableStateOf(false) }
            var newName by remember { mutableStateOf("") }
            Column(Modifier.padding(12.dp)) {
                Text(T.dServer, color = TextDim, fontSize = 12.sp)
                TextButton(onClick = { expanded = true }) {
                    Icon(Icons.Filled.Dns, contentDescription = null, tint = TextDim,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(server?.name ?: selected, color = TextMain, fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = T.dSwitch,
                        tint = TextDim)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    servers.forEach { s ->
                        DropdownMenuItem(
                            text = { Text("${s.name} :${s.port}") },
                            onClick = { main.select(s.id); expanded = false }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(T.dNewServer) },
                        onClick = { expanded = false; showCreate = true }
                    )
                }
                if (showCreate) {
                    OutlinedTextField(value = newName, onValueChange = { newName = it },
                        label = { Text(T.dServerName) }, singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            main.createServer(newName.ifBlank { "Server${servers.size + 1}" })
                            showCreate = false; newName = ""
                        }) { Text(T.dCreate) }
                        TextButton(onClick = { showCreate = false }) { Text(T.cancel) }
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(icon = Icons.Filled.Dns, title = (server?.name ?: selected).uppercase())
                StatusBadge(status, T)
                InfoLine(icon = Icons.Filled.Public, label = T.dAddress,
                    value = "127.0.0.1:$port")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(icon = Icons.Filled.Group, label = T.dPlayers, value = playersText,
                        modifier = Modifier.weight(1f))
                    StatTile(icon = Icons.Filled.Schedule, label = T.dUptime,
                        value = DashboardViewModel.formatUptime(uptime),
                        modifier = Modifier.weight(1f))
                }
                server?.lastError?.let {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Error, contentDescription = null, tint = Red,
                            modifier = Modifier.size(16.dp))
                        Text(it, color = Red, fontSize = 12.sp)
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { main.start() },
                enabled = !running && !busy,
                colors = ButtonDefaults.buttonColors(containerColor = Green),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(T.dStart)
            }
            OutlinedButton(
                onClick = { main.restart() },
                enabled = running,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.RestartAlt, contentDescription = null,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(T.dRestart)
            }
            Button(
                onClick = { main.stop() },
                enabled = running || busy,
                colors = ButtonDefaults.buttonColors(containerColor = Red),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Stop, contentDescription = null,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(T.dStop)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionHeader(icon = Icons.Filled.Storage, title = T.dInfo)
                InfoRow(T.dHostname, server?.hostname ?: "N/A")
                InfoRow(T.dAddress, "127.0.0.1:$port")
                InfoRow(T.dGamemode, live?.gamemode?.ifBlank { null }
                    ?: server?.gamemode?.ifBlank { null } ?: "N/A")
                InfoRow(T.dPlayers.lowercase(), playersText)
                InfoRow(T.dVersion, version?.let { "SA-MP $it" } ?: "N/A")
                InfoRow(T.dPassword, if (passwordOn) "ON" else "OFF")
                InfoRow(T.dLanguage, language)
                InfoRow(T.dMap, map)
                InfoRow(T.dRuntime, "QEMU i386")
                InfoRow(T.dHost, hostArch)
                InfoRow(T.dStatus, statusText(status, T))
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(icon = Icons.Filled.Memory, title = T.dMemory)
                Text(
                    if (running) T.dMemRunning.format(memMb.toInt(), memPct.toInt())
                    else T.dMemStopped,
                    color = TextDim, fontSize = 12.sp
                )
                val memMax = memNiceMax((memHist.maxOrNull() ?: 0f))
                LineChart(values = memHist, maxValue = memMax,
                    formatY = { "${it.toInt()} ${T.dMb}" }, lineColor = Green)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(icon = Icons.Filled.Speed, title = T.dCpu)
                Text(
                    if (running) T.dCpuRunning.format(cpu.toInt())
                    else T.dCpuStopped,
                    color = TextDim, fontSize = 12.sp
                )
                val cpuMax = cpuNiceMax(cpuHist.maxOrNull() ?: 0f)
                LineChart(values = cpuHist, maxValue = cpuMax,
                    formatY = { "${it.toInt()}%" }, lineColor = Green)
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = TextMain, modifier = Modifier.size(20.dp))
        Text(title, color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoLine(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = label, tint = TextDim, modifier = Modifier.size(16.dp))
        Text(value, color = TextDim, fontSize = 13.sp)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextDim, fontSize = 13.sp)
        Text(value.take(40), color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatTile(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = Divider), modifier = modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, contentDescription = label, tint = TextDim,
                    modifier = Modifier.size(16.dp))
                Text(label, color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text(value, color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1)
        }
    }
}

private fun memNiceMax(peak: Float): Float {
    if (peak <= 0f) return 500f
    return ((peak / 500f).toInt() + 1) * 500f
}

private fun cpuNiceMax(peak: Float): Float {
    if (peak <= 100f) return 100f
    return ((peak / 25f).toInt() + 1) * 25f
}
