package com.samplocal.manager.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.samplocal.manager.SampApp
import com.samplocal.manager.data.model.ServerStatus
import com.samplocal.manager.net.relay.RelayDirectory
import com.samplocal.manager.net.relay.RelayProtocol
import com.samplocal.manager.net.relay.RelaySessionManager
import com.samplocal.manager.net.relay.UdpBridge
import com.samplocal.manager.net.relay.OwnUdpRelayProvider
import com.samplocal.manager.net.relay.PlayitProvider
import com.samplocal.manager.net.playit.PlayitAgent
import com.samplocal.manager.net.playit.AgentLogState
import com.samplocal.manager.net.playit.HttpConnectProxy
import com.samplocal.manager.net.playit.PlayitApi
import com.samplocal.manager.net.relay.TurnRelayProvider
import com.samplocal.manager.net.relay.ProviderSelector
import com.samplocal.manager.net.relay.ProviderSession
import com.samplocal.manager.net.relay.RelayProvider
import com.samplocal.manager.net.relay.CredentialProvider
import com.samplocal.manager.net.stun.StunClient
import com.samplocal.manager.samp.ConfigManager
import com.samplocal.manager.samp.ServerQueryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramSocket
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ConnStage {
    DISCONNECTED, CHECKING_NETWORK, LOCAL_AVAILABLE, TESTING_DIRECT,
    DIRECT_AVAILABLE, DIRECT_UNAVAILABLE, TRY_NAT_TRAVERSAL,
    TRAVERSAL_OK, TRAVERSAL_FAILED, CONNECTING_RELAY, RELAY_CONNECTED,
    VERIFYING_RELAY, ONLINE, CONNECTIVITY_FAILED,
    PLAYIT_CONNECTING, PLAYIT_TUNNEL_CREATING, PLAYIT_TUNNEL_CONNECTED,
    VERIFYING_UDP, VERIFYING_SAMP
}

private data class RelayHandle(
    val provider: RelayProvider,
    val session: ProviderSession,
    val bridge: UdpBridge?,
    val token: ByteArray,
    val sessionId: ByteArray,
    val startedMs: Long
)

class ConnectivityRepository(
    private val context: Context,
    private val probe: NetworkProbe = NetworkProbe(context),
    private val stun: StunClient = StunClient(),
    private val query: ServerQueryManager = ServerQueryManager()
) {
    private val prefs = context.getSharedPreferences("connectivity", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sampApp get() = context.applicationContext as SampApp
    private val configManager = ConfigManager()
    private val directory = RelayDirectory(context)

    private val _state = MutableStateFlow(loadPersistedMode())
    val state: StateFlow<PublishState> = _state.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private var serverId = "Default"
    private var monitorJob: Job? = null
    private var statsJob: Job? = null
    private var netCallback: ConnectivityManager.NetworkCallback? = null
    private val handles = mutableMapOf<String, RelayHandle>()

    val signaling = SignalingSession(context)

    fun infraSource(): String? = directory.sourceUrl()
    fun setInfraSource(url: String) {
        directory.setSourceUrl(url)
        log("Fonte de infraestrutura atualizada")
    }

    private fun loadPersistedMode(): PublishState {
        val mode = try {
            ConnectivityMode.valueOf(prefs.getString("mode", "LOCAL") ?: "LOCAL")
        } catch (_: Exception) { ConnectivityMode.LOCAL }
        return PublishState(mode = mode, stage = ConnStage.DISCONNECTED)
    }

    fun bind(ids: kotlinx.coroutines.flow.StateFlow<String>) {
        scope.launch {
            ids.collect {
                if (it != serverId) {
                    stopRelayFor(serverId)
                    serverId = it
                }
                refresh(quick = true)
            }
        }
        scope.launch {
            sampApp.serverManager.servers.collect { list ->
                val running = list.firstOrNull { s -> s.id == serverId }?.status ==
                    ServerStatus.RUNNING
                if (!running && _state.value.linkState != LinkState.OFFLINE &&
                    _state.value.method != PublishMethod.NONE
                ) {
                    stopRelayFor(serverId)
                    _state.value = _state.value.copy(
                        linkState = LinkState.OFFLINE,
                        method = PublishMethod.NONE,
                        players = null,
                        stage = ConnStage.DISCONNECTED
                    )
                    log("Servidor parou — publicação pausada")
                }
            }
        }
    }

    fun setMode(mode: ConnectivityMode) {
        prefs.edit().putString("mode", mode.name).apply()
        if (mode != ConnectivityMode.INTERNET) stopRelayFor(serverId)
        _state.value = _state.value.copy(mode = mode, error = null, stage = ConnStage.DISCONNECTED)
        log("Modo: $mode")
        scope.launch { refresh(quick = true) }
    }

    val playit: PlayitProvider = PlayitProvider(context)

    fun clearRelay() {
        stopRelayFor(serverId)
        _state.value = _state.value.copy(relay = RelayInfo())
        log("Relay desconectado")
    }

    private fun stopRelayFor(id: String) {
        handles.remove(id)?.let { h ->
            try { h.session.close() } catch (_: Exception) { }
            h.bridge?.stop()
            log("RELAY_DISCONNECTED sessão de $id encerrada")
        }
        syncPlayitAgent()
    }

    private fun syncPlayitAgent() {        try {
            val inUse = handles.values.any { it.provider.id == "playit" }

            if (!inUse && !playit.isLinked()) playit.agent().stop()
        } catch (_: Exception) { }
    }

    fun unlinkPlayit() {
        try {
            cancelClaim()
            stopRelayFor(serverId)
            playit.clearLink()
            playit.agent().stop()
            try { localProxy.stop() } catch (_: Exception) { }
            _agentProc.value = AgentProc.NOT_STARTED
            _agentDetail.value = ""

            _state.value = _state.value.copy(
                method = PublishMethod.NONE,
                linkState = LinkState.OFFLINE,
                provider = null,
                transport = null,
                region = null,
                publicIp = null,
                publicPort = null,
                directAvailable = null,
                relay = RelayInfo(),
                players = null,
                error = null,
                stage = ConnStage.DISCONNECTED
            )
            log("PLAYIT_DISCONNECTED conta desvinculada, agent parado, estado limpo")
        } catch (_: Exception) { }
    }

    private suspend fun verifyCredential() {
        try {
            val secret = playit.loadSecret()
            if (secret.isNullOrBlank()) {
                log("PLAYIT credencial ausente apos claim")
                return
            }
            val r = playit.playitApi().tunnelsList(secret)
            if (r.isSuccess) {
                val n = r.getOrThrow().size
                log("PLAYIT credencial OK ($n tunnel(s) na conta)")
            } else {
                log("PLAYIT credencial FALHOU: ${r.exceptionOrNull()?.message?.take(140)}")
            }
        } catch (e: Exception) {
            log("PLAYIT credencial FALHOU: ${e.message?.take(140)}")
        }
    }

    private suspend fun startAgentAuthenticated(reason: String): Boolean {
        val secret = playit.loadSecret()
        if (secret.isNullOrBlank()) {
            _agentProc.value = AgentProc.NOT_STARTED
            log("PLAYIT_WAITING_FOR_PROVISION sem secret ($reason)")
            return false
        }
        _agentProc.value = AgentProc.STARTING
        log("PLAYIT_AGENT_STARTING autenticado ($reason)")
        val proxyPort = ensureProxy().takeIf { it > 0 }
        val up = playit.agent().start(secret, proxyPort)
        if (up.isFailure || !playit.agent().isAlive()) {
            _agentProc.value = AgentProc.EXITED
            _agentDetail.value = agentDetailText()
            log("PLAYIT_PROCESS_EXITED ${_agentDetail.value}")
            return false
        }
        _agentProc.value = AgentProc.RUNNING_AUTH
        log("PLAYIT_CONNECTING aguardando control-plane...")

        val end = System.currentTimeMillis() + 30000
        var logState = AgentLogState.UNKNOWN
        var apiOk = false
        while (System.currentTimeMillis() < end) {
            val parsed = PlayitAgent.parseLogTail(playit.agent().readLogTail())
            logState = parsed.state
            if (logState == AgentLogState.CONNECTED) break
            if (logState == AgentLogState.AUTH_ERROR) break
            if (!playit.agent().isAlive()) break
            try {
                val r = playitApiRundata()
                if (r) {
                    apiOk = true
                    break
                }
            } catch (_: Exception) { }
            delay(2000)
        }

        if (!playit.agent().isAlive()) {
            _agentProc.value = AgentProc.EXITED
            _agentDetail.value = agentDetailText()
            log("PLAYIT_PROCESS_EXITED durante conexao ${_agentDetail.value}")
            return false
        }
        if (logState == AgentLogState.CONNECTED || apiOk) {
            log("PLAYIT_CONNECTED + PLAYIT_AGENT_AUTHENTICATED (log=$logState api=$apiOk)")
            agentBackoffMs = 5000L
            return true
        }
        _agentProc.value = AgentProc.AUTH_FAILED
        val tail = lastLogLines(3)
        _agentDetail.value = "log=$logState api=$apiOk $tail".trim().take(220)
        log("PLAYIT_AUTH_FAILED ${_agentDetail.value}")
        return false
    }

    private fun agentDetailText(): String =
        "exit=${playit.agent().lastExitCode} ${playit.agent().lastError ?: ""}".trim()

    private fun lastLogLines(n: Int): String {
        return try {
            playit.agent().readLogTail().lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .takeLast(n)
                .joinToString(" | ")
                .replace(Regex("[0-9a-fA-F]{32,}"), "***")
                .take(160)
        } catch (_: Exception) { "" }
    }

    private suspend fun playitApiRundata(): Boolean {
        return try {
            val secret = playit.loadSecret() ?: return false
            playit.playitApi().agentsRundata(secret).getOrNull()
                ?.optString("status", "") == "success"
        } catch (_: Exception) { false }
    }

    private fun startAgentWatch() {
        if (agentWatchJob?.isActive == true) return
        agentWatchJob = scope.launch {
            while (isActive) {
                delay(15000)
                try {
                    if (!playit.isLinked()) {
                        if (_agentProc.value != AgentProc.NOT_STARTED &&
                            _agentProc.value != AgentProc.RUNNING_UNCLAIMED
                        ) {
                            _agentProc.value = AgentProc.NOT_STARTED
                        }
                        continue
                    }
                    val alive = playit.agent().isAlive()
                    if (alive && _agentProc.value != AgentProc.RUNNING_AUTH) {
                        _agentProc.value = AgentProc.RUNNING_AUTH
                    }
                    if (!alive) {
                        _agentProc.value = AgentProc.EXITED
                        _agentDetail.value =
                            "exit=${playit.agent().lastExitCode} ${playit.agent().lastError ?: ""}".trim()
                        log("PLAYIT_PROCESS_EXITED ${_agentDetail.value} — PLAYIT_RECONNECTING em ${agentBackoffMs / 1000}s")
                        delay(agentBackoffMs)
                        agentBackoffMs = (agentBackoffMs * 2).coerceAtMost(120000L)
                        startAgentAuthenticated("watch")
                    }
                } catch (_: Exception) { }
            }
        }
    }

    fun copyAddress(): String? = _state.value.playerEndpoint()

    data class ClaimUi(val code: String, val url: String, val waiting: Boolean)

    private val _claim = MutableStateFlow<ClaimUi?>(null)
    val claim: StateFlow<ClaimUi?> = _claim.asStateFlow()

    private var claimJob: Job? = null
    private var agentWatchJob: Job? = null
    private var agentBackoffMs: Long = 5000L
    private val localProxy = HttpConnectProxy()
    @Volatile private var proxyPortUsed: Int = -1

    private fun ensureProxy(): Int {
        return try {
            localProxy.start().also { proxyPortUsed = it }
        } catch (e: Exception) {
            log("PLAYIT proxy local falhou: ${e.message}")
            -1
        }
    }

    fun proxyStatus(): String =
        if (proxyPortUsed > 0 && localProxy.isRunning()) "ativo 127.0.0.1:$proxyPortUsed"
        else "parado"

    enum class AgentProc {
        NOT_STARTED, STARTING, RUNNING_UNCLAIMED, RUNNING_AUTH, EXITED, AUTH_FAILED
    }

    private val _agentProc = MutableStateFlow(AgentProc.NOT_STARTED)
    val agentProc: StateFlow<AgentProc> = _agentProc.asStateFlow()
    private val _agentDetail = MutableStateFlow("")
    val agentDetail: StateFlow<String> = _agentDetail.asStateFlow()

    fun startClaim() {
        claimJob?.cancel()
        claimJob = scope.launch {
            try {
                _agentProc.value = AgentProc.STARTING
                log("PLAYIT_AGENT_STARTING (modo provisioning)")
                val proxyPort = ensureProxy().takeIf { it > 0 }
                if (proxyPort != null) log("PLAYIT proxy DNS local 127.0.0.1:$proxyPort")
                val up = playit.agent().startUnclaimed(proxyPort)
                if (up.isFailure || !playit.agent().isAlive()) {
                    _agentProc.value = AgentProc.EXITED
                    _agentDetail.value = playit.agent().lastError ?: "falha ao iniciar"
                    log("PLAYIT_PROCESS_EXITED ${playit.agent().lastError} " +
                        "exit=${playit.agent().lastExitCode}")
                    return@launch
                }
                _agentProc.value = AgentProc.RUNNING_UNCLAIMED
                log("PLAYIT_AGENT_STARTED pid vivo, aguardando claim")
                val code = playit.newClaimCode()
                log("PLAYIT_CLAIM_CREATED $code")
                _claim.value = ClaimUi(code, playit.claimUrl(code), waiting = true)
                log("PLAYIT_CLAIM_URL ${playit.claimUrl(code)}")
                var lastPoll = ""
                val res = playit.pollClaim(code, tries = 90, intervalMs = 2000) { ev ->
                    when (ev) {
                        is PlayitApi.ClaimEvent.Setup ->
                            log("PLAYIT_CLAIM_SETUP registrando codigo")
                        is PlayitApi.ClaimEvent.SetupRetry ->
                            log("PLAYIT_CLAIM_SETUP retry (rede?)")
                        is PlayitApi.ClaimEvent.Poll ->
                            if (ev.status != lastPoll) {
                                lastPoll = ev.status
                                log("PLAYIT_CLAIM_STATUS $lastPoll")
                            }
                        is PlayitApi.ClaimEvent.Approved ->
                            log("PLAYIT_CLAIM_APPROVED trocando por secret")
                        is PlayitApi.ClaimEvent.SecretReceived ->
                            log("PLAYIT_SECRET_RECEIVED salva no Keystore")
                    }
                }
                if (res.isSuccess) {
                    _claim.value = null
                    log("PLAYIT_SECRET_RECEIVED Keystore; reiniciando agent autenticado")
                    startAgentAuthenticated("claim")

                    verifyCredential()
                    refresh(quick = true)
                } else {
                    _claim.value = _claim.value?.copy(waiting = false)
                    log("PLAYIT_ERROR claim: ${res.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                log("PLAYIT_ERROR claim: ${e.message}")
            }
        }
    }

    fun cancelClaim() {
        claimJob?.cancel()
        claimJob = null
        _claim.value = null
        log("PLAYIT claim cancelado pelo usuário")
    }

    fun testConnectivity() {
        scope.launch {
            _state.value = _state.value.copy(linkState = LinkState.CHECKING, error = null)
            log("=== Teste de conectividade ===")
            runFullDiagnosis()
        }
    }

    fun refresh(quick: Boolean = false) {
        scope.launch {
            if (quick) runQuick() else runFullDiagnosis()
        }
    }

    fun testRelay() {
        scope.launch {
            log("RELAY teste manual iniciado")
            val infra = directory.fetch().getOrNull()
            val own = infra?.providers?.firstOrNull { it.type == "OWN" && it.enabled }
            if (own == null) {
                _state.value = _state.value.copy(error = "Infraestrutura indisponível (sem fonte configurada)")
                log("RELAY teste: sem fonte de infraestrutura")
                return@launch
            }
            val sid = RelayProtocol.newSessionId()
            val token = RelayProtocol.newToken()
            val sm = RelaySessionManager(own.host, own.port, sid, token, sampPort())
            val reg = sm.register()
            if (reg.isFailure) {
                log("RELAY teste FALHOU: ${reg.exceptionOrNull()?.message}")
                _messageError("Relay inalcançável: ${reg.exceptionOrNull()?.message}")
                sm.close()
                return@launch
            }
            var ok = 0
            repeat(3) { if (sm.pingOnce()) ok++ }
            log("RELAY teste: registro OK, ping $ok/3")
            _messageError(null)
            _state.value = _state.value.copy(error = null)
            sm.close()
            scope.launch { log("Relay responde ($ok/3 pings). Ative INTERNET para publicar.") }
        }
    }

    fun restartConnection() {
        scope.launch {
            log("Reiniciando conexão...")
            stopRelayFor(serverId)
            _state.value = _state.value.copy(
                linkState = LinkState.RECONNECTING, stage = ConnStage.DISCONNECTED,
                publicIp = null, publicPort = null, method = PublishMethod.NONE
            )
            runFullDiagnosis()
        }
    }

    private fun _messageError(msg: String?) {
        if (msg != null) _state.value = _state.value.copy(error = msg)
    }

    private suspend fun runQuick() {
        val port = sampPort()
        val net = probe.probe()
        _state.value = _state.value.copy(
            localIp = net?.ipv4,
            ipv6 = net?.ipv6,
            iface = net?.iface,
            gateway = net?.gateway,
            networkType = net?.networkType,
            sampPort = port,
            infraSource = infraSource(),
            method = if (_state.value.mode == ConnectivityMode.LOCAL) PublishMethod.LOCAL
            else _state.value.method
        )
    }

    private suspend fun runFullDiagnosis() {
        setStage(ConnStage.CHECKING_NETWORK)
        val port = sampPort()
        if (!NetworkProbe.isValidSampPort(port)) {
            fail("Porta SAMP inválida: $port")
            return
        }
        log("NETWORK_INIT interface ativa?")
        val net = probe.probe()
        if (net == null || net.ipv4 == null) {
            _state.value = _state.value.copy(
                linkState = LinkState.OFFLINE, method = PublishMethod.NONE,
                localIp = null, nat = NatState.UNREACHABLE,
                natDetail = "Sem rede ativa", error = "Sem rede ativa",
                stage = ConnStage.CONNECTIVITY_FAILED
            )
            log("Sem rede ativa")
            return
        }
        log("Interface ${net.iface} (${net.networkType}) IPv4 ${net.ipv4}")
        _state.value = _state.value.copy(
            localIp = net.ipv4, ipv6 = net.ipv6, iface = net.iface,
            gateway = net.gateway, networkType = net.networkType, sampPort = port,
            infraSource = infraSource(), error = null
        )

        when (_state.value.mode) {
            ConnectivityMode.LOCAL -> {
                setStage(ConnStage.LOCAL_AVAILABLE)
                _state.value = _state.value.copy(
                    linkState = LinkState.CONNECTED, method = PublishMethod.LOCAL,
                    natDetail = "Modo local: sem NAT traversal", lastCheckMs = now()
                )
                log("Modo LOCAL ativo em 127.0.0.1:$port")
                updatePlayers()
            }
            ConnectivityMode.LAN -> {
                setStage(ConnStage.LOCAL_AVAILABLE)
                val ok = localBindOk(port)
                val ping = if (ok) query.queryInfo("127.0.0.1", port, 2000).isSuccess else false
                _state.value = _state.value.copy(
                    linkState = if (ok) LinkState.CONNECTED else LinkState.DEGRADED,
                    method = PublishMethod.LOCAL,
                    natDetail = "Rede local: socket ${if (ok) "OK" else "FALHOU"}, query ${if (ping) "OK" else "—"}",
                    lastCheckMs = now()
                )
                log("REDE LOCAL ${net.ipv4}:$port")
                updatePlayers()
            }
            ConnectivityMode.INTERNET -> runInternet(net, port)
        }
    }

    private suspend fun runInternet(net: LocalNetwork, port: Int) {
        log("STUN_STARTED descoberta do reflexivo")
        val localAddr = InetAddress.getByName(net.ipv4!!)
        val sr = stun.discover().getOrNull()

        val sr2 = if (sr != null) {
            try {
                stun.discover(StunClient.ALT_HOST, StunClient.DEFAULT_PORT, 3000).getOrNull()
            } catch (_: Exception) { null }
        } else null
        if (sr == null) {
            _state.value = _state.value.copy(
                linkState = LinkState.DEGRADED, method = PublishMethod.NONE,
                nat = NatState.UNKNOWN, natDetail = "STUN sem resposta",
                error = "STUN indisponível — verifique a internet",
                stage = ConnStage.CONNECTIVITY_FAILED, lastCheckMs = now()
            )
            log("STUN sem resposta (timeout)")
            return
        }
        log("STUN_RESULT ${sr.ip.hostAddress}:${sr.port} RTT ${sr.rttMs}ms")
        val verdict = NatClassifier.classify(localAddr, port, sr.ip, sr.port, hasNetwork = true)
        val mapping = NatClassifier.compareMappings(sr.port, sr2?.port)
        val cgnat = NatClassifier.cgnatSuspect(net.networkType, mapping, verdict.state)
        val traversal = buildString {
            append("mapeamento: ${if (mapping == NatClassifier.Mapping.SYMMETRIC) "simétrico" else if (mapping == NatClassifier.Mapping.PORT_PRESERVED) "porta preservada" else "indeterminado"}")
            if (cgnat) append("; CGNAT provável (móvel + simétrico)")
        }
        log("NAT_DETECTED ${verdict.detail} | $traversal")
        _state.value = _state.value.copy(
            publicIp = sr.ip.hostAddress, publicPort = sr.port,
            nat = verdict.state,
            natDetail = verdict.detail + if (cgnat) " — CGNAT provável" else "",
            traversal = traversal
        )

        setStage(ConnStage.TESTING_DIRECT)
        log("DIRECT_TEST_STARTED bind+query+ping")
        val bindOk = localBindOk(port)
        val serverOk = serverAnswers(port)
        val pingMs = if (serverOk) query.ping("127.0.0.1", port, 2000).getOrNull() else null
        log("DIRECT bind=${if (bindOk) "OK" else "FALHOU"} query=${if (serverOk) "OK" else "FALHOU"} ping=${pingMs?.let { "${it}ms" } ?: "—"}")
        _state.value = _state.value.copy(sampPingMs = pingMs)
        val directProven = verdict.state == NatState.DIRECT && bindOk && serverOk
        if (directProven) {
            setStage(ConnStage.DIRECT_AVAILABLE)
            _state.value = _state.value.copy(
                linkState = LinkState.CONNECTED, method = PublishMethod.DIRECT,
                directAvailable = true, stage = ConnStage.ONLINE, lastCheckMs = now()
            )
            log("DIRECT_TEST_SUCCESS rota direta")
            updatePlayers()
            return
        }
        setStage(ConnStage.DIRECT_UNAVAILABLE)
        _state.value = _state.value.copy(directAvailable = false)
        log("DIRECT_TEST_FAILED sem rota direta comprovada")

        setStage(ConnStage.TRY_NAT_TRAVERSAL)
        if (mapping == NatClassifier.Mapping.PORT_PRESERVED && verdict.state == NatState.NAT) {

            log("NAT_TRAVERSAL porta preservada mas entrada nao comprovavel sem peer")
        } else {
            log("NAT_TRAVERSAL sem par cooperativo (clientes vanilla) — indo ao relay")
        }
        setStage(ConnStage.TRAVERSAL_FAILED)

        setStage(ConnStage.CONNECTING_RELAY)
        connectRelay(port)
    }

    private suspend fun awaitPlayit(port: Int): Boolean {
        setStage(ConnStage.PLAYIT_CONNECTING)
        log("PLAYIT_CONNECTING agent...")
        val health = try {
            playit.healthCheck()
        } catch (e: Exception) {
            com.samplocal.manager.net.relay.ProviderHealth(false, null, e.message ?: "falha")
        }
        if (!health.ok) {
            log("PLAYIT_ERROR ${health.detail}")
            _state.value = _state.value.copy(error = "Playit: ${health.detail}")
            return false
        }
        setStage(ConnStage.PLAYIT_TUNNEL_CREATING)
        log("PLAYIT_TUNNEL_CREATE 127.0.0.1:$port")
        val conn = try {
            playit.connect(com.samplocal.manager.net.relay.ConnectParams(serverId, port))
        } catch (e: Exception) {
            Result.failure<ProviderSession>(e)
        }
        val session = conn.getOrNull()
        if (session == null) {
            val msg = conn.exceptionOrNull()?.message ?: "falha"
            log("PLAYIT_ERROR $msg")
            _state.value = _state.value.copy(error = "Playit: $msg")
            return false
        }
        setStage(ConnStage.PLAYIT_TUNNEL_CONNECTED)
        val ep = session.endpoint
        log("PLAYIT_ENDPOINT ${ep.ip}:${ep.port}")
        _state.value = _state.value.copy(
            publicIp = ep.ip, publicPort = ep.port,
            provider = "playit", transport = "udp",
            region = (session as? PlayitProvider.PlayitSession)?.region ?: "auto"
        )

        setStage(ConnStage.VERIFYING_UDP)
        log("PLAYIT_UDP_TEST tunnel ativo; ping local do samp...")
        val pingMs = query.ping("127.0.0.1", port, 3000).getOrNull()
        setStage(ConnStage.VERIFYING_SAMP)
        val level = playit.validate(
            com.samplocal.manager.net.relay.ConnectParams(serverId, port), session.endpoint
        )
        log("PLAYIT_SAMP_TEST nivel=$level")
        handles[serverId]?.let { old ->
            try { old.session.close() } catch (_: Exception) { }
            old.bridge?.stop()
        }
        handles[serverId] = RelayHandle(playit, session, null, ByteArray(0),
            sessionIdOf(session), now())
        return if (level == PlayitProvider.ValidateLevel.SAMP_REACHABLE) {
            _state.value = _state.value.copy(
                linkState = LinkState.CONNECTED, method = PublishMethod.PLAYIT,
                stage = ConnStage.ONLINE, lastCheckMs = now(), error = null,
                sampPingMs = pingMs
            )
            log("PLAYIT_CONNECTED ${ep.ip}:${ep.port} ONLINE")
            startStatsPump()
            updatePlayers()
            true
        } else {
            _state.value = _state.value.copy(
                linkState = LinkState.DEGRADED, method = PublishMethod.PLAYIT,
                stage = ConnStage.CONNECTIVITY_FAILED,
                error = "Playit: tunnel ativo mas SAMP local não responde (SAMP_UNREACHABLE).",
                sampPingMs = pingMs
            )
            log("PLAYIT SAMP_UNREACHABLE (tunnel ok, samp local mudo)")
            true
        }
    }

    private fun sessionIdOf(session: ProviderSession): ByteArray =
        (session as? PlayitProvider.PlayitSession)?.sid ?: ByteArray(0)

    private suspend fun connectRelay(port: Int) {

        var playitReason: String? = null
        if (playit.isLinked()) {
            if (awaitPlayit(port)) return
            playitReason = _state.value.error
            if (playitReason?.contains("401") == true || playitReason?.contains("rejeitou") == true) {

                log("PLAYIT credencial rejeitada — limpando vínculo automaticamente")
                try {
                    playit.clearLink()
                    playit.agent().stop()
                } catch (_: Exception) { }
                _agentProc.value = AgentProc.NOT_STARTED
                playitReason =
                    "Credencial Playit rejeitada (HTTP 401). Toque CONECTAR PLAYIT para vincular de novo."
            }
            log("Playit indisponível (${playitReason ?: "?"}), tentando relay próprio...")
        } else {
            log("Playit não vinculado — usando relay próprio (se houver)")
        }
        log("RELAY_CONNECTING via fonte de infraestrutura")
        val infra = directory.fetch().getOrNull()
        if (infra == null) {
            return failRelay(
                "Internet indisponível: " + (playitReason?.let { "Playit falhou ($it). " } ?: "") +
                    "sem rota direta e nenhum relay disponível."
            )
        }

        val creds = CredentialProvider(context)
        val providers = infra.providers.filter { it.enabled }.mapNotNull { e ->
            when (e.type) {
                "OWN" -> OwnUdpRelayProvider(scope, context, e)
                "TURN" -> TurnRelayProvider(
                    e, infra.credentials[e.id], CredentialProvider(context)
                )
                else -> null
            }
        }
        if (providers.isEmpty()) {
            return failRelay("Internet indisponível: " + (playitReason?.let { "Playit falhou ($it). " } ?: "") + "nenhum provider no documento.")
        }

        infra.providers.firstOrNull { it.type == "TURN" && it.enabled }?.let { t ->
            val url = infra.credentials[t.id]
            if (!url.isNullOrBlank()) {
                log("TURN complementar: ${t.host}:${t.port}...")
                _state.value = _state.value.copy(turnStatus = "testando...")
            }
        }
        val checked = providers.map { p ->
            log("RELAY_PROVIDER health ${p.id} (${p.kind})...")
            val h = try {
                p.healthCheck()
            } catch (e: Exception) {
                com.samplocal.manager.net.relay.ProviderHealth(false, null, e.message ?: "falha")
            }
            log("RELAY_ALLOCATION_${if (h.ok) "SUCCESS" else "FAILED"} ${p.id}: ${h.detail}")
            ProviderSelector.Candidate(p, h)
        }
        val sel = ProviderSelector.selectForVanilla(checked)
        log("RELAY_PROVIDER_SELECTED ${sel.reason}")
        val provider = sel.provider
        if (provider == null) {
            return failRelay("Internet indisponível: " + (playitReason?.let { "Playit falhou ($it). " } ?: "") + sel.reason + ".")
        }
        log("RELAY_ENDPOINT_RECEIVED via ${provider.id}")
        val conn = try {
            provider.connect(com.samplocal.manager.net.relay.ConnectParams(serverId, port))
        } catch (e: Exception) {
            Result.failure<ProviderSession>(e)
        }
        val session = conn.getOrNull()
        if (session == null) {
            failRelay("Falha ao conectar em ${provider.id}: ${conn.exceptionOrNull()?.message}")

            val next = ProviderSelector.order(checked)
                .firstOrNull { it.provider.id != provider.id && it.provider.supportsVanillaSamp && it.health.ok }
            if (next != null) {
                log("RELAY_RECONNECT tentando ${next.provider.id}...")
                return connectVia(next.provider, port)
            }
            return
        }
        registerHandle(provider, session, port)
    }

    private suspend fun connectVia(provider: RelayProvider, port: Int) {
        val conn = try {
            provider.connect(com.samplocal.manager.net.relay.ConnectParams(serverId, port))
        } catch (e: Exception) {
            Result.failure<ProviderSession>(e)
        }
        val session = conn.getOrNull()
        if (session == null) {
            failRelay("Falha ao conectar em ${provider.id}: ${conn.exceptionOrNull()?.message}")
            return
        }
        registerHandle(provider, session, port)
    }

    private suspend fun registerHandle(provider: RelayProvider, session: ProviderSession, port: Int) {
        handles[serverId]?.let { old ->
            try {
                if (old.session is OwnUdpRelayProvider.OwnSession) old.session.close()
                else old.session.close()
            } catch (_: Exception) { }
            old.bridge?.stop()
        }
        val token = ByteArray(0)
        val sid = RelayProtocol.newSessionId()
        handles[serverId] = RelayHandle(provider, session, bridgeOf(session), token, sid, now())

        setStage(ConnStage.VERIFYING_RELAY)
        var verified = false
        for (i in 0 until 6) {
            kotlinx.coroutines.delay(2500)
            if (session.heartbeat()) {
                verified = true
                break
            }
        }
        val st = session.stats()
        if (!verified) {
            log("RELAY heartbeat sem resposta — degradado")
            _state.value = _state.value.copy(
                linkState = LinkState.DEGRADED, method = PublishMethod.RELAY,
                relay = relayInfo(session, provider, RelayState.FAILED),
                error = "Relay sem resposta ao heartbeat.",
                stage = ConnStage.CONNECTIVITY_FAILED
            )
            return
        }
        _state.value = _state.value.copy(
            linkState = LinkState.RELAYING, method = PublishMethod.RELAY,
            relay = relayInfo(session, provider, RelayState.ACTIVE),
            provider = provider.id, transport = provider.transport, region = provider.region,
            stage = ConnStage.ONLINE, lastCheckMs = now(), error = null
        )
        log("RELAY_CONNECTED ${session.endpoint} — ONLINE via relay")
        log("RELAY_TRAFFIC_TEST heartbeat OK RTT ${st.rttMs}ms")
        startStatsPump()
        updatePlayers()
    }

    private fun bridgeOf(session: ProviderSession): UdpBridge? =
        (session as? OwnUdpRelayProvider.OwnSession)?.bridge

    private fun relayInfo(
        session: ProviderSession, provider: RelayProvider, state: RelayState
    ): RelayInfo {
        val h = handles[serverId]
        val st = session.stats()
        return RelayInfo(
            state = state,
            endpoint = session.endpoint.toString(),
            latencyMs = st.rttMs,
            lossPct = st.lossPct,
            jitterMs = st.jitterMs,
            sessionStartMs = h?.startedMs ?: now(),
            bytesUp = st.bytesUp,
            bytesDown = st.bytesDown,
            pktsUp = st.pktsUp,
            pktsDown = st.pktsDown
        )
    }

    private fun startStatsPump() {
        if (statsJob?.isActive == true) return
        statsJob = scope.launch {
            while (isActive) {
                delay(5000)
                val h = handles[serverId] ?: continue
                if (h.provider.id == "playit") {

                    val alive = try { h.session.heartbeat() } catch (_: Exception) { false }
                    if (!alive) {
                        log("PLAYIT_HEARTBEAT falhou — tentando reconectar")
                        _state.value = _state.value.copy(linkState = LinkState.RECONNECTING)
                        connectRelay(sampPort())
                    }
                    continue
                }
                if (_state.value.method != PublishMethod.RELAY) continue
                val st = try { h.session.stats() } catch (_: Exception) { continue }
                val failed = st.rttMs == null && _state.value.linkState == LinkState.RELAYING &&
                    (System.currentTimeMillis() - h.startedMs) > 60_000
                _state.value = _state.value.copy(
                    relay = _state.value.relay.copy(
                        latencyMs = st.rttMs,
                        lossPct = st.lossPct,
                        jitterMs = st.jitterMs,
                        bytesUp = st.bytesUp,
                        bytesDown = st.bytesDown,
                        pktsUp = st.pktsUp,
                        pktsDown = st.pktsDown,
                        state = if (failed) RelayState.FAILED else RelayState.ACTIVE
                    )
                )
                if (failed) {
                    log("RELAY_DISCONNECTED — tentando reconectar")
                    _state.value = _state.value.copy(linkState = LinkState.RECONNECTING)
                    connectRelay(sampPort())
                }
            }
        }
    }

    private fun failRelay(msg: String) {
        _state.value = _state.value.copy(
            linkState = LinkState.DEGRADED, method = PublishMethod.NONE,
            relay = RelayInfo(state = RelayState.FAILED),
            error = msg, stage = ConnStage.CONNECTIVITY_FAILED, lastCheckMs = now()
        )
        log("RELAY $msg")
    }

    private fun fail(msg: String) {
        _state.value = _state.value.copy(
            linkState = LinkState.OFFLINE, method = PublishMethod.NONE,
            error = msg, stage = ConnStage.CONNECTIVITY_FAILED
        )
        log(msg)
    }

    private fun setStage(s: ConnStage) {
        _state.value = _state.value.copy(stage = s)
    }

    private suspend fun updatePlayers() {
        try {
            val s = sampApp.serverManager.servers.value.firstOrNull { it.id == serverId }
            if (s?.status == ServerStatus.RUNNING) {
                val info = query.queryInfo("127.0.0.1", s.port, 2000).getOrNull()
                _state.value = _state.value.copy(players = info?.players)
            } else {
                _state.value = _state.value.copy(players = null)
            }
        } catch (_: Exception) { }
    }

    private fun localBindOk(port: Int): Boolean {
        return try {
            DatagramSocket(port).use { true }
        } catch (_: Exception) {

            portInUseByUs(port)
        }
    }

    private fun portInUseByUs(port: Int): Boolean {
        return sampApp.serverManager.servers.value
            .firstOrNull { it.id == serverId }?.port == port
    }

    private suspend fun serverAnswers(port: Int): Boolean {
        return try {
            query.queryInfo("127.0.0.1", port, 2000).isSuccess
        } catch (_: Exception) { false }
    }

    private fun sampPort(): Int {
        return try {
            val dir = sampApp.runtimeManager.serverDir(serverId)
            configManager.readConfig(dir).port
        } catch (_: Exception) { 7777 }
    }

    fun startMonitoring() {
        if (monitorJob?.isActive == true) return
        registerNetCallback()
        startAgentWatch()

        scope.launch {
            if (playit.isLinked() && !playit.agent().isAlive()) {
                startAgentAuthenticated("boot")
            } else if (playit.agent().isAlive()) {
                _agentProc.value = AgentProc.RUNNING_AUTH
            }
        }
        monitorJob = scope.launch {
            while (isActive) {
                delay(60_000)
                if (_state.value.mode == ConnectivityMode.INTERNET) {
                    log("Rechecagem periódica")
                    runFullDiagnosis()
                }
            }
        }
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        agentWatchJob?.cancel()
        agentWatchJob = null
        try {
            netCallback?.let {
                context.getSystemService(ConnectivityManager::class.java)
                    ?.unregisterNetworkCallback(it)
            }
        } catch (_: Exception) { }
        netCallback = null
    }

    fun onNetworkLost() {
        scope.launch {
            log("CONNECTIVITY_CHANGED rede perdida — refazendo descoberta")
            _state.value = networkLostTransition(_state.value)
            handles.values.forEach {
                try { it.session.close() } catch (_: Exception) { }
                it.bridge?.stop()
            }
            handles.clear()
            runFullDiagnosis()
        }
    }

    private fun registerNetCallback() {
        try {
            val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
            if (netCallback != null) return
            val req = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            netCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onLost(network: Network) { onNetworkLost() }
                override fun onAvailable(network: Network) {
                    scope.launch {
                        log("CONNECTIVITY_CHANGED rede disponivel — refazendo descoberta")
                        runFullDiagnosis()
                    }
                }
            }
            cm.registerNetworkCallback(req, netCallback!!)
        } catch (_: Exception) { }
    }

    fun close() {
        stopMonitoring()
        statsJob?.cancel()
        handles.values.forEach {
            try { it.session.close() } catch (_: Exception) { }
            it.bridge?.stop()
        }
        handles.clear()
        scope.cancel()
    }

    fun log(msg: String) {
        val ts = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        _logs.value = (_logs.value + "NETWORK $ts $msg").takeLast(200)
    }

    private fun now(): Long = System.currentTimeMillis()

    companion object {

        fun selectMethod(directProven: Boolean, relayActive: Boolean): PublishMethod = when {
            directProven -> PublishMethod.DIRECT
            relayActive -> PublishMethod.RELAY
            else -> PublishMethod.NONE
        }

        fun networkLostTransition(state: PublishState): PublishState = state.copy(
            linkState = LinkState.RECONNECTING,
            publicIp = null,
            publicPort = null,
            directAvailable = null,
            method = PublishMethod.NONE
        )

        fun serverRunningTransition(state: PublishState, running: Boolean): PublishState =
            if (running) state
            else state.copy(linkState = LinkState.OFFLINE, method = PublishMethod.NONE, players = null)
    }
}
