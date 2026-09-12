package com.samplocal.manager.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samplocal.manager.net.ConnectivityRepository
import com.samplocal.manager.net.ConnStage
import com.samplocal.manager.net.ConnectivityMode
import com.samplocal.manager.net.LinkState
import com.samplocal.manager.net.NatState
import com.samplocal.manager.net.PublishMethod
import com.samplocal.manager.net.PublishState
import com.samplocal.manager.net.RelayState
import com.samplocal.manager.ui.theme.Green
import com.samplocal.manager.ui.theme.Red
import com.samplocal.manager.ui.theme.Surface
import com.samplocal.manager.ui.theme.TextDim
import com.samplocal.manager.ui.theme.TextMain
import com.samplocal.manager.ui.theme.Yellow
import com.samplocal.manager.ui.viewmodel.ConnectivityViewModel
import com.samplocal.manager.ui.viewmodel.DashboardViewModel
import com.samplocal.manager.ui.strings.Strings
import com.samplocal.manager.ui.strings.statusText
import com.samplocal.manager.ui.viewmodel.MainViewModel

@Composable
fun ConnectivityScreen(
    main: MainViewModel,
    conn: ConnectivityViewModel,
    dash: DashboardViewModel,
    onBack: () -> Unit,
    T: Strings
) {
    val ctx = LocalContext.current
    DisposableEffect(Unit) {
        conn.start(main)
        dash.start()
        onDispose {
            conn.stop()
            dash.stop()
        }
    }
    val st by conn.state.collectAsStateWithLifecycle()
    val logs by conn.logs.collectAsStateWithLifecycle()
    val testing by conn.testing.collectAsStateWithLifecycle()
    val msg by conn.message.collectAsState()
    val server by main.servers.collectAsState()
    val current = server.firstOrNull { it.id == main.selected.collectAsState().value }
    val uptime by dash.uptimeSec.collectAsStateWithLifecycle()
    val cpu by dash.cpuPct.collectAsStateWithLifecycle()
    val memMb by dash.memMb.collectAsStateWithLifecycle()

    var showLogs by remember { mutableStateOf(false) }
    var showTech by remember { mutableStateOf(false) }
    var answerText by remember { mutableStateOf("") }
    var infraUrl by remember { mutableStateOf(conn.infraSource() ?: "") }

    LaunchedEffect(msg) {
        if (msg != null) {
            kotlinx.coroutines.delay(4000)
            conn.consumeMessage()
        }
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = T.close, tint = TextDim)
            }
            Column(Modifier.weight(1f)) {
                Text(T.nTitle, color = TextMain, fontSize = 18.sp,
                    fontWeight = FontWeight.Bold)
                Text(current?.name ?: "", color = TextDim, fontSize = 12.sp)
            }
            Icon(Icons.Filled.Lan, contentDescription = null, tint = Green,
                modifier = Modifier.size(26.dp))
        }

        Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeChip(T.nLocal, st.mode == ConnectivityMode.LOCAL) {
                        conn.setMode(ConnectivityMode.LOCAL)
                    }
                    ModeChip(T.nLan, st.mode == ConnectivityMode.LAN) {
                        conn.setMode(ConnectivityMode.LAN)
                    }
                    ModeChip(T.nInternet, st.mode == ConnectivityMode.INTERNET) {
                        conn.setMode(ConnectivityMode.INTERNET)
                    }
                }
                StatusLine(st, T)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ConnHeader(icon = Icons.Filled.Dns, title = T.nConn)
                ConnRow(T.nMethod, methodText(st, T))
                ConnRow(T.nAddress, st.playerEndpoint() ?: T.nUnavailable)
                ConnRow(T.nNat, natText(st, T))
                ConnRow(T.nConn, linkText(st, T))
                if (st.method == PublishMethod.RELAY) {
                    ConnRow(T.nLatency, st.relay.latencyMs?.let { "$it ms" } ?: T.nUnavailable)
                }
                st.error?.let {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Error, contentDescription = null, tint = Red,
                            modifier = Modifier.size(16.dp))
                        Text(it, color = Red, fontSize = 12.sp)
                    }
                }
                if (testing || st.linkState == LinkState.CHECKING ||
                    st.linkState == LinkState.RECONNECTING
                ) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ConnHeader(icon = Icons.Filled.Lan, title = T.nPath)
                PathStep(T.nDevice, detail = st.localIp ?: "—", state = StepState.OK)
                PathStep(T.nNat, detail = natText(st, T), state = natStep(st))
                PathStep(T.nRelay, detail = relayStepDetail(st, T), state = relayStep(st))
                PathStep(T.nInternet, detail = st.publicIp ?: "—", state = internetStep(st))
                PathStep(T.nPlayers,
                    detail = st.players?.let { T.nPlayersOnline.format(it) } ?: "—",
                    state = if (st.players != null) StepState.OK else StepState.IDLE, last = true)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ConnHeader(icon = Icons.Filled.Public, title = T.nAddrTitle)
                val ep = st.playerEndpoint()
                Text(
                    ep ?: T.nAddrNone,
                    color = if (ep != null) TextMain else TextDim,
                    fontSize = 17.sp, fontWeight = FontWeight.Bold
                )
                if (st.method == PublishMethod.RELAY) {
                    Text(T.nRelayDest,
                        color = TextDim, fontSize = 12.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { conn.test() }, enabled = !testing,
                        modifier = Modifier.weight(1f)) { Text(T.nTest) }
                    OutlinedButton(onClick = { conn.copyAddress(ctx) },
                        modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(T.nCopy)
                    }
                }
                OutlinedButton(onClick = { conn.refresh() }, enabled = !testing,
                    modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Refresh, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(T.nRefresh)
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ConnHeader(icon = Icons.Filled.Storage, title = T.nSamp)
                ConnRow(T.dStatus, current?.status?.let { statusText(it, T) } ?: "—")
                ConnRow(T.nPort, "${st.sampPort}")
                ConnRow(T.nTransport, "UDP")
                ConnRow(T.dPlayers.lowercase(), st.players?.let { "$it" } ?: "—")
                ConnRow(T.nGamemode, current?.gamemode?.ifBlank { null } ?: "—")
                ConnRow(T.nHostname, current?.hostname ?: "—")
                ConnRow(T.nUptime, DashboardViewModel.formatUptime(uptime))
                ConnRow(T.nCpu, "${cpu.toInt()}%")
                ConnRow(T.nRam, "${memMb.toInt()} MB")
                st.sampPingMs?.let { ConnRow(T.nPingSamp, "${it} ms") }
                st.sampPingMs?.let { ConnRow(T.nPingLocal, "${it} ms") }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ConnHeader(icon = Icons.Filled.Public, title = "PLAYIT")
                val linked = remember(msg, st.linkState) { conn.playitLinked() }
                if (!linked) {
                    Text(T.nNoCfg, color = TextDim, fontSize = 13.sp)
                    Text(T.nPlayitDesc,
                        color = TextDim, fontSize = 12.sp)
                    Button(onClick = { conn.startClaim() },
                        modifier = Modifier.fillMaxWidth()) {
                        Text(T.nConnect)
                    }
                } else {
                    val agentOk = remember(msg, st.linkState, testing) { conn.playitAgentAlive() }
                    val proc by conn.agentProc.collectAsStateWithLifecycle()
                    val detail by conn.agentDetail.collectAsStateWithLifecycle()
                    ConnRow(T.nState, when {
                        st.provider == "playit" && st.linkState == LinkState.CONNECTED -> T.nConnected
                        else -> if (agentOk) T.nLinkedActive else T.nLinked
                    })
                    ConnRow(T.nAgent, agentProcText(proc, detail, T))
                    if (st.provider == "playit") {
                        ConnRow(T.nEndpoint, st.playerEndpoint() ?: "—")
                        ConnRow(T.nTransport, "UDP")
                        st.sampPingMs?.let { ConnRow(T.nPingSamp, "$it ms") }
                    }
                    TextButton(onClick = { conn.unlinkPlayit() }) { Text(T.nUnlink) }
                    TextButton(onClick = { conn.relinkPlayit() }) { Text(T.nRelink) }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ConnHeader(icon = Icons.Filled.Link, title = T.nRelay)
                when (st.relay.state) {
                    RelayState.NOT_CONFIGURED -> {
                        Text(T.nRelayNoreq, color = TextDim, fontSize = 13.sp)
                        Text(T.nRelayDirect,
                            color = TextDim, fontSize = 12.sp)
                    }
                    else -> {
                        ConnRow(T.nState, relayStateText(st, T))
                        ConnRow(T.nEndpoint, st.relay.endpoint ?: "—")
                        ConnRow(T.nLatency, st.relay.latencyMs?.let { "$it ms" } ?: T.nUnavailable)
                        ConnRow(T.nLoss, st.relay.lossPct?.let { "${it.toInt()}%" } ?: T.nUnavailable)
                        ConnRow(T.nJitter, st.relay.jitterMs?.let { "${it.toInt()} ms" } ?: T.nUnavailable)
                        ConnRow(T.nSession, sessionDuration(st))
                        ConnRow(T.nTraffic, "${formatBytes(st.relay.bytesDown)} ↓ / ${formatBytes(st.relay.bytesUp)} ↑")
                        ConnRow(T.nPackets, "${st.relay.pktsDown} ↓ / ${st.relay.pktsUp} ↑")
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { conn.testRelay() }, modifier = Modifier.weight(1f)) {
                        Text(T.nTestRelay)
                    }
                    OutlinedButton(onClick = { conn.restartConnection() },
                        modifier = Modifier.weight(1f)) {
                        Text(T.nRestartConn)
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ConnHeader(icon = Icons.Filled.Storage, title = T.nDiag)
                ConnRow(T.nIface, "${st.iface ?: "—"} (${st.networkType ?: "—"})")
                ConnRow(T.nIpv4Local, st.localIp ?: T.nUnavailable)
                ConnRow(T.nIpv4Pub, st.publicIp ?: T.nUnavailable)
                ConnRow(T.nIpv6, st.ipv6 ?: T.nUnavailLow)
                ConnRow(T.nUdp, if (st.publicIp != null || st.localIp != null) T.nAvailable else T.nUnavailLow)
                ConnRow(T.nNat, natText(st, T))
                ConnRow(T.nTraversal, st.traversal ?: "—")
                ConnRow(T.nDirectConn, directText(st, T))
                ConnRow(T.nRelay, relayText(st, T))
                ConnRow(T.nPa, playitAgentText(conn, T))
                ConnRow(T.nPt, playitTunnelText(st, T))
                ConnRow(T.nProxy, conn.proxyStatus())
                ConnRow(T.nUdp, udpText(st, T))
                ConnRow(T.nSampRow, sampText(st, T))
                ConnRow(T.nEndpoint, st.playerEndpoint() ?: "—")
                ConnRow(T.nLatency, latencyText(st))
                st.turnStatus?.let { ConnRow(T.nTurn, it) }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
            var showTech by remember { mutableStateOf(false) }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { showTech = !showTech }) {
                    Icon(Icons.Filled.Terminal, contentDescription = null, tint = TextDim,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (showTech) T.nTechHide else T.nTechShow)
                }
                if (showTech) {
                    TechRow(T.nTechHost, "ARM64")
                    TechRow(T.nTechGuest, "X86_32")
                    TechRow(T.nTechBackend, "QEMU i386")
                    TechRow(T.nIface, st.iface ?: "—")
                    TechRow(T.nIpv4Local, st.localIp ?: "—")
                    TechRow(T.nIpv4Pub, st.publicIp ?: "—")
                    TechRow(T.nIpv6, st.ipv6 ?: "—")
                    TechRow(T.nNat, "${st.nat}: ${st.natDetail}")
                    TechRow(T.nTechStun, st.publicIp?.let { T.gOk } ?: "—")
                    TechRow(T.nUdp, "bind+query")
                    TechRow(T.nTechDirect, directText(st, T))
                    TechRow(T.nRelay, relayText(st, T))
                    TechRow(T.nTechProvider, st.provider ?: "—")
                    TechRow(T.nTechTransport, st.transport ?: "—")
                    TechRow(T.nTechRegion, st.region ?: "—")
                    TechRow(T.nTechRelayLat, st.relay.latencyMs?.let { "$it ms" } ?: "—")
                    TechRow(T.nTechSession, conn.sessionIdShort())
                    TechRow(T.nTechServer, current?.id ?: "—")
                    TechRow(T.nTechPackets, "${st.relay.pktsUp + st.relay.pktsDown}")
                    TechRow(T.nTechBytes, "${st.relay.bytesUp + st.relay.bytesDown}")
                    TechRow(T.nTechHeartbeat, st.relay.sessionStartMs.let {
                        if (it == 0L) "—" else java.text.SimpleDateFormat("HH:mm:ss",
                            java.util.Locale.US).format(java.util.Date(it))
                    })
                    androidx.compose.material3.OutlinedTextField(
                        value = infraUrl, onValueChange = { infraUrl = it },
                        label = { Text(T.nInfraLabel) },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = { conn.setInfraSource(infraUrl) },
                        modifier = Modifier.fillMaxWidth()) { Text(T.nInfraSave) }
                    OutlinedButton(onClick = { conn.exportOffer(ctx, st.sampPort) },
                        modifier = Modifier.fillMaxWidth()) {
                        Text(T.nOffer)
                    }
                    var answerText by remember { mutableStateOf("") }
                    androidx.compose.material3.OutlinedTextField(
                        value = answerText, onValueChange = { answerText = it },
                        label = { Text(T.nAnswerLabel) },
                        modifier = Modifier.fillMaxWidth(), minLines = 2
                    )
                    Button(onClick = { conn.importAnswer(answerText) },
                        modifier = Modifier.fillMaxWidth()) {
                        Text(T.nAnswerImport)
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
            var showLogs by remember { mutableStateOf(false) }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Terminal, contentDescription = null, tint = TextDim,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(T.nLogTitle, color = TextMain, fontSize = 13.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { showLogs = !showLogs }) {
                        Text(if (showLogs) T.nLogHide else T.nLogShow)
                    }
                }
                val shown = if (showLogs) logs else logs.takeLast(6)
                if (shown.isEmpty()) Text(T.nLogEmpty, color = TextDim, fontSize = 12.sp)
                shown.forEach {
                    Text(it, color = TextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
        msg?.let { Text(it, color = Yellow, fontSize = 12.sp) }
        Spacer(Modifier.height(4.dp))
    }

    val claim by conn.claim.collectAsStateWithLifecycle()
    claim?.let { c ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { conn.cancelClaim() },
            title = { Text(T.nClaimTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(T.nClaimDesc,
                        color = TextDim, fontSize = 13.sp)
                    Text(T.nClaimStep1, color = TextMain, fontSize = 13.sp)
                    Text(c.url, color = TextMain, fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace)
                    Text(T.nClaimStep2, color = TextMain, fontSize = 13.sp)
                    Text(T.nClaimKeep,
                        color = TextDim, fontSize = 12.sp)
                    if (c.waiting) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp)
                            Text(T.nClaimWait, color = TextDim, fontSize = 12.sp)
                        }
                    }
                    OutlinedButton(onClick = { conn.copyClaimUrl(ctx, c.url) },
                        modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(T.nClaimCopy)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { conn.cancelClaim() }) { Text(T.cancel) } }
        )
    }
}

private enum class StepState { OK, BUSY, FAIL, IDLE }

@Composable
private fun PathStep(title: String, detail: String, state: StepState, last: Boolean = false) {
    val color = when (state) {
        StepState.OK -> Green
        StepState.BUSY -> Yellow
        StepState.FAIL -> Red
        StepState.IDLE -> TextDim
    }
    val mark = when (state) {
        StepState.OK -> "✓"
        StepState.BUSY -> "●"
        StepState.FAIL -> "×"
        StepState.IDLE -> "○"
    }
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(mark, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = TextDim, fontSize = 12.sp, maxLines = 1)
        }
        if (!last) {
            Text("↓", color = TextDim, fontSize = 12.sp)
        }
    }
}

private fun natStep(st: PublishState): StepState = when (st.nat) {
    com.samplocal.manager.net.NatState.DIRECT -> StepState.OK
    com.samplocal.manager.net.NatState.NAT -> StepState.BUSY
    else -> StepState.IDLE
}

private fun relayStep(st: PublishState): StepState = when (st.relay.state) {
    com.samplocal.manager.net.RelayState.ACTIVE -> StepState.OK
    com.samplocal.manager.net.RelayState.STANDBY -> StepState.BUSY
    com.samplocal.manager.net.RelayState.FAILED -> StepState.FAIL
    com.samplocal.manager.net.RelayState.NOT_CONFIGURED ->
        if (st.method == PublishMethod.DIRECT) StepState.OK else StepState.IDLE
}

private fun relayStepDetail(st: PublishState, T: Strings): String = when (st.relay.state) {
    com.samplocal.manager.net.RelayState.ACTIVE -> st.relay.endpoint ?: T.nRsActive
    com.samplocal.manager.net.RelayState.NOT_CONFIGURED -> T.nRelayNoreq
    else -> st.relay.state.name
}

private fun internetStep(st: PublishState): StepState =
    if (st.publicIp != null) StepState.OK else StepState.IDLE

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label, fontSize = 12.sp) })
}

@Composable
private fun StatusLine(st: PublishState, T: Strings) {
    val (text, color) = when (st.mode) {
        ConnectivityMode.LOCAL -> T.nLocal to Green
        ConnectivityMode.LAN -> T.nLanLong to Green
        ConnectivityMode.INTERNET -> when (st.method) {
            PublishMethod.DIRECT -> T.nDirect to Green
            PublishMethod.RELAY -> T.nRelay to Yellow
            else -> T.nChecking to Yellow
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.size(10.dp)) { drawCircle(color) }
        Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun agentProcText(proc: ConnectivityRepository.AgentProc, detail: String, T: Strings): String =
    when (proc) {
        ConnectivityRepository.AgentProc.NOT_STARTED -> T.nApStopped
        ConnectivityRepository.AgentProc.STARTING -> T.nApStarting
        ConnectivityRepository.AgentProc.RUNNING_UNCLAIMED -> T.nApUnclaimed
        ConnectivityRepository.AgentProc.RUNNING_AUTH -> T.nApRunning
        ConnectivityRepository.AgentProc.EXITED -> "${T.nApExited}${if (detail.isNotBlank()) " ($detail)" else ""}"
        ConnectivityRepository.AgentProc.AUTH_FAILED -> T.nApAuthFail
    }

private fun relayStateText(st: PublishState, T: Strings): String = when (st.relay.state) {
    com.samplocal.manager.net.RelayState.ACTIVE -> T.nRsActive
    com.samplocal.manager.net.RelayState.STANDBY -> T.nRsStandby
    com.samplocal.manager.net.RelayState.FAILED -> T.nRsFailed
    com.samplocal.manager.net.RelayState.NOT_CONFIGURED -> T.nRsNone
}

private fun sessionDuration(st: PublishState): String {
    val start = st.relay.sessionStartMs
    if (start == 0L) return "—"
    val s = ((System.currentTimeMillis() - start) / 1000).coerceAtLeast(0)
    return String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
}

@Composable
private fun ConnHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = TextMain, modifier = Modifier.size(20.dp))
        Text(title, color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ConnRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextDim, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value.take(42), color = TextMain, fontSize = 13.sp,
            fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun TechRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = TextDim, fontSize = 12.sp)
        Text(value, color = TextMain, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}

private fun methodText(st: PublishState, T: Strings): String = when (st.method) {
    PublishMethod.LOCAL -> if (st.mode == ConnectivityMode.LAN) T.nLanLong else T.nLocal
    PublishMethod.DIRECT -> T.nDirect
    PublishMethod.PLAYIT -> T.nPlayit
    PublishMethod.RELAY -> T.nRelay
    PublishMethod.NONE -> "—"
}

private fun natText(st: PublishState, T: Strings): String = when (st.nat) {
    NatState.DIRECT -> T.nNatNone
    NatState.NAT -> if ("CGNAT" in st.natDetail) T.nNatCgnat else T.nNatDetected
    NatState.UNREACHABLE -> T.nNatNoNet
    NatState.UNKNOWN -> T.nNatUnknown
}

private fun linkText(st: PublishState, T: Strings): String = when (st.linkState) {
    LinkState.CONNECTED -> T.nAvailable
    LinkState.CHECKING -> T.nChecking
    LinkState.DEGRADED -> T.nDegraded
    LinkState.RECONNECTING -> T.nReconnecting
    LinkState.RELAYING -> T.nViaRelay
    LinkState.OFFLINE -> "Offline"
}

private fun directText(st: PublishState, T: Strings): String = when (st.directAvailable) {
    true -> T.nAvailable
    false -> T.nUnavailLow
    null -> "—"
}

private fun relayText(st: PublishState, T: Strings): String = when (st.relay.state) {
    RelayState.ACTIVE -> T.nActiveLow
    RelayState.STANDBY -> T.nRsStandby
    RelayState.FAILED -> T.nRsFailed
    RelayState.NOT_CONFIGURED -> T.nRsNone
}

private fun playitAgentText(conn: ConnectivityViewModel, T: Strings): String {
    return try {
        if (!conn.playitLinked()) T.nNotLinked
        else if (conn.playitAgentAlive()) T.nActiveLow else T.nApStopped
    } catch (_: Exception) { "—" }
}

private fun playitTunnelText(st: PublishState, T: Strings): String =
    if (st.provider == "playit" && st.playerEndpoint() != null) T.nActiveLow else "—"

private fun udpText(st: PublishState, T: Strings): String =
    if (st.playerEndpoint() != null) T.nReachOk else "—"

private fun sampText(st: PublishState, T: Strings): String =
    st.sampPingMs?.let { T.nResponds.format(it) } ?: "—"

private fun latencyText(st: PublishState): String =
    st.relay.latencyMs?.let { "$it ms" }
        ?: st.sampPingMs?.let { "$it ms" } ?: "—"

private fun formatBytes(b: Long): String {
    if (b < 1024) return "$b B"
    val kb = b / 1024.0
    return if (kb < 1024) String.format("%.1f KB", kb) else String.format("%.2f MB", kb / 1024)
}
