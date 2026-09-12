package com.samplocal.manager.ui.strings

import com.samplocal.manager.core.AppLang

interface Strings {
    val settingsTitle: String
    val settingsSubtitle: String
    val secAppearance: String
    val secServer: String
    val secConsole: String
    val secAbout: String
    val lang: String
    val langDesc: String
    val mode: String
    val modeDesc: String
    val modeDark: String
    val modeLight: String
    val accent: String
    val accentDesc: String
    val accentNameDefault: String
    val autoStart: String
    val autoStartDesc: String
    val keepBg: String
    val keepBgDesc: String
    val notif: String
    val notifDesc: String
    val retention: String
    val retentionDesc: String
    val retNever: String
    val ret1d: String
    val ret7d: String
    val ret30d: String
    val consoleFont: String
    val consoleFontDesc: String
    val fontSmall: String
    val fontMedium: String
    val fontLarge: String
    val consoleWrap: String
    val consoleWrapDesc: String
    val consoleTime: String
    val consoleTimeDesc: String
    val aboutVersion: String
    val aboutCredits: String
    val creditsRole: String
    val creditsThanks: String
    val thanksTitle: String
    val thanksDesc: String
    val developedBy: String
    val accentTitle: String
    val accentHex: String
    val accentUse: String
    val accentReset: String
    val cancel: String
    val confirm: String
    val close: String
    val retry: String
    val tabHome: String
    val tabConsole: String
    val tabFiles: String
    val tabSettings: String
    val wizardTagline: String
    val featDirs: String
    val featRuntime: String
    val featDeps: String
    val featArch: String
    val start: String
    val installTitle: String
    val installDesc: String
    val infoTitle: String
    val infoDesc: String
    val dServer: String
    val dSwitch: String
    val dNewServer: String
    val dServerName: String
    val dCreate: String
    val dAddress: String
    val dPlayers: String
    val dUptime: String
    val dStart: String
    val dRestart: String
    val dStop: String
    val dInfo: String
    val dHostname: String
    val dGamemode: String
    val dVersion: String
    val dPassword: String
    val dLanguage: String
    val dMap: String
    val dRuntime: String
    val dHost: String
    val dStatus: String
    val dMemory: String
    val dCpu: String
    val dMb: String
    val dMemStopped: String
    val dMemRunning: String
    val dCpuStopped: String
    val dCpuRunning: String
    val dConnDesc: String
    val dOpenSettings: String
    val stOnline: String
    val stOffline: String
    val stStopped: String
    val stStarting: String
    val stRunning: String
    val stStopping: String
    val stInstalling: String
    val stCrashed: String
    val stError: String
    val fSubtitle: String
    val fImportServer: String
    val fPlugin: String
    val fChecking: String
    val fFilesUnit: String
    val fInvalidDir: String
    val fChooseOther: String
    val fFilesTitle: String
    val fFolders: String
    val fFiles: String
    val fImportFile: String
    val fReauthTitle: String
    val fReauthDesc: String
    val fReauth: String
    val fBreadcrumb: String
    val fBackDesc: String
    val fRefreshDesc: String
    val fEmpty: String
    val fEmptyDesc: String
    val fAddServer: String
    val fVerifying: String
    val fDirFound: String
    val fCfgFound: String
    val fCfgMissing: String
    val fGmNone: String
    val fGmCount: String
    val fPlNone: String
    val fPlCount: String
    val fReady: String
    val fAdd: String
    val fFolderDesc: String
    val fFileDesc: String
    val fOpenFolder: String
    val fFileActions: String
    val fView: String
    val fViewFail: String
    val fRename: String
    val fDelete: String
    val fDeleteTitle: String
    val fDeleteDesc: String
    val fOk: String
    val fRenamed: String
    val fBadName: String
    val fDeleted: String
    val fDeleteFail: String
    val cClear: String
    val cSave: String
    val cTitle: String
    val cEmpty: String
    val nTitle: String
    val nLan: String
    val nInternet: String
    val nConn: String
    val nMethod: String
    val nLocal: String
    val nLanLong: String
    val nDirect: String
    val nPlayit: String
    val nRelay: String
    val nAddress: String
    val nUnavailable: String
    val nNat: String
    val nNatNone: String
    val nNatCgnat: String
    val nNatDetected: String
    val nNatNoNet: String
    val nNatUnknown: String
    val nLatency: String
    val nPath: String
    val nDevice: String
    val nPlayers: String
    val nPlayersOnline: String
    val nAddrTitle: String
    val nAddrNone: String
    val nRelayDest: String
    val nTest: String
    val nCopy: String
    val nRefresh: String
    val nSamp: String
    val nPort: String
    val nTransport: String
    val nGamemode: String
    val nHostname: String
    val nUptime: String
    val nCpu: String
    val nRam: String
    val nPingSamp: String
    val nPingLocal: String
    val nNoCfg: String
    val nPlayitDesc: String
    val nConnect: String
    val nState: String
    val nConnected: String
    val nLinkedActive: String
    val nLinked: String
    val nAgent: String
    val nApStopped: String
    val nApStarting: String
    val nApUnclaimed: String
    val nApRunning: String
    val nApExited: String
    val nApAuthFail: String
    val nUnlink: String
    val nRelink: String
    val nRelayNoreq: String
    val nRelayDirect: String
    val nRsActive: String
    val nRsStandby: String
    val nRsFailed: String
    val nRsNone: String
    val nEndpoint: String
    val nLoss: String
    val nJitter: String
    val nSession: String
    val nTraffic: String
    val nPackets: String
    val nTestRelay: String
    val nRestartConn: String
    val nDiag: String
    val nIface: String
    val nIpv4Local: String
    val nIpv4Pub: String
    val nIpv6: String
    val nUdp: String
    val nTraversal: String
    val nDirectConn: String
    val nPa: String
    val nPt: String
    val nProxy: String
    val nSampRow: String
    val nTurn: String
    val nAvailable: String
    val nUnavailLow: String
    val nNotLinked: String
    val nActiveLow: String
    val nReachOk: String
    val nResponds: String
    val nTechShow: String
    val nTechHide: String
    val nTechHost: String
    val nTechGuest: String
    val nTechBackend: String
    val nTechStun: String
    val nTechDirect: String
    val nTechProvider: String
    val nTechTransport: String
    val nTechRegion: String
    val nTechRelayLat: String
    val nTechSession: String
    val nTechServer: String
    val nTechPackets: String
    val nTechBytes: String
    val nTechHeartbeat: String
    val nInfraLabel: String
    val nInfraSave: String
    val nOffer: String
    val nAnswerLabel: String
    val nAnswerImport: String
    val nLogTitle: String
    val nLogShow: String
    val nLogHide: String
    val nLogEmpty: String
    val nClaimTitle: String
    val nClaimDesc: String
    val nClaimStep1: String
    val nClaimStep2: String
    val nClaimKeep: String
    val nClaimWait: String
    val nClaimCopy: String
    val nChecking: String
    val nDegraded: String
    val nReconnecting: String
    val nViaRelay: String
    val sTitle: String
    val sCfgFile: String
    val sSave: String
    val sGmInstalled: String
    val sGmHint: String
    val sRestart: String
    val gTitle: String
    val gLibs: String
    val gTestBackend: String
    val gCheckFiles: String
    val gCopyFull: String
    val gCopyBtn: String
    val gLibsTitle: String
    val gLibsFail: String
    val mCopying: String
    val mValidatingExe: String
    val mLogSaved: String
    val mCfgSaved: String
    val mSrvAdded: String
    val mReauthOk: String
    val mReauthFail: String
    val mBadName: String
    val mSrvCreated: String
    val mImportFail: String
    val mPickAmx: String
    val mGmImported: String
    val mPickSo: String
    val mPluginImported: String
    val mFolderCopied: String
    val mDiagCopied: String
    val mDiagFail: String
    val mSrcUpdated: String
    val mLinkCopied: String
    val mUnlinked: String
    val mRelinked: String
    val mFail: String
    val mNoExt: String
    val mAddrCopied: String
    val mOfferCopied: String
    val mExportFail: String
    val mCandidates: String
    val mBadAnswer: String
    val gInstalled: String
    val gMissing: String
    val gYes: String
    val gNo: String
    val gInvalid: String
    val gOk: String
    val gMissingN: String
    val mInstallingSvr: String
    val mSvrKept: String
    val mSvrInstalled: String
    val mSvrInvalid: String
    val mSvrInstallFail: String
    val mImportFolderFail: String
    val mUnknownErr: String
    val mSetupFail: String
    val mViewFailRead: String
    val gDevArch: String
    val gRuntime: String
    val gGuestArch: String
    val gBackend: String
    val gBackendAvail: String
    val gBackendPath: String
    val gPlugins: String
    val gGamemodes: String
    val gServerDir: String
    val gProcess: String
    val gPid: String
    val mPathCopied: String
    val instVerifying: String
    val instX86: String
    val instLibs: String
    val instValidating: String
    val instReady: String
}

fun statusText(s: com.samplocal.manager.data.model.ServerStatus, T: Strings): String = when (s) {
    com.samplocal.manager.data.model.ServerStatus.STOPPED -> T.stStopped
    com.samplocal.manager.data.model.ServerStatus.STARTING -> T.stStarting
    com.samplocal.manager.data.model.ServerStatus.RUNNING -> T.stRunning
    com.samplocal.manager.data.model.ServerStatus.STOPPING -> T.stStopping
    com.samplocal.manager.data.model.ServerStatus.INSTALLING -> T.stInstalling
    com.samplocal.manager.data.model.ServerStatus.CRASHED -> T.stCrashed
    com.samplocal.manager.data.model.ServerStatus.ERROR -> T.stError
}

fun badgeText(s: com.samplocal.manager.data.model.ServerStatus, T: Strings): String = when (s) {
    com.samplocal.manager.data.model.ServerStatus.RUNNING -> T.stOnline
    com.samplocal.manager.data.model.ServerStatus.STOPPED -> T.stOffline
    else -> statusText(s, T)
}

private class PtStrings : Strings {
    override val settingsTitle = "AJUSTES"
    override val settingsSubtitle = "Personalize sua experiência"
    override val secAppearance = "APARÊNCIA"
    override val secServer = "SERVIDOR"
    override val secConsole = "CONSOLE"
    override val secAbout = "SOBRE"
    override val lang = "Linguagem"
    override val langDesc = "Escolha o idioma do aplicativo"
    override val mode = "Modo"
    override val modeDesc = "Defina o tema do aplicativo"
    override val modeDark = "Escuro"
    override val modeLight = "Claro"
    override val accent = "Cor de destaque"
    override val accentDesc = "Escolha a cor principal do app"
    override val accentNameDefault = "Verde"
    override val autoStart = "Inicialização automática"
    override val autoStartDesc = "Inicia o servidor ao abrir o app"
    override val keepBg = "Minimizar para bandeja"
    override val keepBgDesc = "Mantém o servidor em segundo plano"
    override val notif = "Notificações"
    override val notifDesc = "Receba avisos sobre o servidor"
    override val retention = "Limpar logs automaticamente"
    override val retentionDesc = "Remove logs antigos automaticamente"
    override val retNever = "Nunca"
    override val ret1d = "1 dia"
    override val ret7d = "7 dias"
    override val ret30d = "30 dias"
    override val consoleFont = "Tamanho da fonte"
    override val consoleFontDesc = "Define o tamanho da fonte no console"
    override val fontSmall = "Pequeno"
    override val fontMedium = "Médio"
    override val fontLarge = "Grande"
    override val consoleWrap = "Quebra de linha"
    override val consoleWrapDesc = "Ativa a quebra de linha automática"
    override val consoleTime = "Exibir timestamps"
    override val consoleTimeDesc = "Mostra o horário nas mensagens"
    override val aboutVersion = "Versão do aplicativo"
    override val aboutCredits = "Créditos"
    override val creditsRole = "Desenvolvido por Gustavo Maccedo (IkeSamp)"
    override val creditsThanks = "Agradecimento especial ao grupo NexusDev"
    override val thanksTitle = "Obrigado por usar o SAMP Local!"
    override val thanksDesc = "Feito com dedicação para a comunidade."
    override val developedBy = "SAMP Local"
    override val accentTitle = "Cor de destaque"
    override val accentHex = "Hexadecimal"
    override val accentUse = "USAR COR"
    override val accentReset = "Padrão"
    override val cancel = "Cancelar"
    override val confirm = "Confirmar"
    override val close = "Fechar"
    override val retry = "TENTAR NOVAMENTE"
    override val tabHome = "Início"
    override val tabConsole = "Console"
    override val tabFiles = "Arquivos"
    override val tabSettings = "Ajustes"
    override val wizardTagline = "Transforme seu Android em um ambiente local\npara servidores SA-MP."
    override val featDirs = "Diretórios"
    override val featRuntime = "Runtime"
    override val featDeps = "Dependências"
    override val featArch = "Verificação de arquitetura"
    override val start = "COMEÇAR"
    override val installTitle = "Instalação do ambiente"
    override val installDesc = "Estamos preparando tudo para você rodar seu servidor SA-MP localmente. Isso pode levar alguns minutos."
    override val infoTitle = "Nenhuma instalação manual necessária:"
    override val infoDesc = "o runtime sai do próprio APK."
    override val dServer = "Servidor"
    override val dSwitch = "Trocar servidor"
    override val dNewServer = "Novo servidor"
    override val dServerName = "Nome do servidor"
    override val dCreate = "Criar"
    override val dAddress = "Endereço"
    override val dPlayers = "PLAYERS"
    override val dUptime = "UPTIME"
    override val dStart = "INICIAR"
    override val dRestart = "REINICIAR"
    override val dStop = "PARAR"
    override val dInfo = "INFORMAÇÕES DO SERVIDOR"
    override val dHostname = "Hostname"
    override val dGamemode = "Gamemode"
    override val dVersion = "Versão"
    override val dPassword = "Senha"
    override val dLanguage = "Idioma"
    override val dMap = "Mapa"
    override val dRuntime = "Runtime"
    override val dHost = "Host"
    override val dStatus = "Status"
    override val dMemory = "MEMORY USAGE"
    override val dCpu = "CPU USAGE"
    override val dMb = "MB"
    override val dMemStopped = "0 MB · parado"
    override val dMemRunning = "%1\$d MB (%2\$d%%) · processo QEMU (RSS)"
    override val dCpuStopped = "0% · parado"
    override val dCpuRunning = "%d%% · processo QEMU (host)"
    override val dConnDesc = "Conectividade do servidor"
    override val dOpenSettings = "Abrir ajustes"
    override val stOnline = "ONLINE"
    override val stOffline = "OFFLINE"
    override val stStopped = "PARADO"
    override val stStarting = "INICIANDO"
    override val stRunning = "RODANDO"
    override val stStopping = "PARANDO"
    override val stInstalling = "INSTALANDO"
    override val stCrashed = "FALHOU"
    override val stError = "ERRO"
    override val fSubtitle = "Gerenciamento de servidores SAMP"
    override val fImportServer = "IMPORTAR SERVIDOR"
    override val fPlugin = "PLUGIN (.SO)"
    override val fChecking = "Verificando servidor…"
    override val fFilesUnit = "%d arquivos"
    override val fInvalidDir = "Diretório inválido"
    override val fChooseOther = "ESCOLHER OUTRA PASTA"
    override val fFilesTitle = "Arquivos do Servidor"
    override val fFolders = "%d pastas"
    override val fFiles = "%d arquivos"
    override val fImportFile = "Importar arquivo"
    override val fReauthTitle = "Acesso à pasta de origem expirado"
    override val fReauthDesc = "O servidor continua salvo. Autorize de novo para reimportar."
    override val fReauth = "REAUTORIZAR PASTA"
    override val fBreadcrumb = "Servidor"
    override val fBackDesc = "Voltar pasta"
    override val fRefreshDesc = "Atualizar lista"
    override val fEmpty = "Pasta vazia"
    override val fEmptyDesc = "Adicione arquivos ao diretório do servidor."
    override val fAddServer = "Adicionar servidor"
    override val fVerifying = "Verificando servidor"
    override val fDirFound = "Diretório encontrado"
    override val fCfgFound = "server.cfg encontrado"
    override val fCfgMissing = "server.cfg ausente"
    override val fGmNone = "Gamemodes ausentes"
    override val fGmCount = "%d gamemode(s)"
    override val fPlNone = "Plugins ausentes"
    override val fPlCount = "%d plugin(s)"
    override val fReady = "Servidor pronto para adicionar"
    override val fAdd = "ADICIONAR"
    override val fFolderDesc = "Pasta"
    override val fFileDesc = "Arquivo"
    override val fOpenFolder = "Abrir pasta"
    override val fFileActions = "Ações do arquivo"
    override val fView = "Visualizar"
    override val fViewFail = "Não foi possível visualizar."
    override val fRename = "Renomear"
    override val fDelete = "Excluir"
    override val fDeleteTitle = "Excluir %s?"
    override val fDeleteDesc = "Esta ação não pode ser desfeita."
    override val fOk = "OK"
    override val fRenamed = "Renomeado para %s"
    override val fBadName = "Nome inválido."
    override val fDeleted = "Excluído: %s"
    override val fDeleteFail = "Falha ao excluir."
    override val cClear = "LIMPAR"
    override val cSave = "SALVAR LOG"
    override val cTitle = "Console: %1\$s (%2\$d linhas)"
    override val cEmpty = "SA-MP Dedicated Server\nAguardando inicializacao...\nInicie o servidor no painel Início."
    override val nTitle = "Conectividade do servidor"
    override val nLan = "REDE LOCAL"
    override val nInternet = "INTERNET"
    override val nConn = "CONECTIVIDADE"
    override val nMethod = "Método"
    override val nLocal = "Local"
    override val nLanLong = "Rede local"
    override val nDirect = "Direct UDP"
    override val nPlayit = "PLAYIT"
    override val nRelay = "UDP RELAY"
    override val nAddress = "Endereço"
    override val nUnavailable = "Indisponível"
    override val nNat = "NAT"
    override val nNatNone = "Sem NAT"
    override val nNatCgnat = "CGNAT provável"
    override val nNatDetected = "Detectado"
    override val nNatNoNet = "Sem rede"
    override val nNatUnknown = "Indeterminado"
    override val nLatency = "Latência"
    override val nPath = "CAMINHO DA CONEXÃO"
    override val nDevice = "DISPOSITIVO"
    override val nPlayers = "JOGADORES"
    override val nPlayersOnline = "%d online"
    override val nAddrTitle = "ENDEREÇO DO SERVIDOR"
    override val nAddrNone = "ENDEREÇO INDISPONÍVEL"
    override val nRelayDest = "Endereço do relay, destino: servidor local"
    override val nTest = "TESTAR CONECTIVIDADE"
    override val nCopy = "COPIAR ENDEREÇO"
    override val nRefresh = "Atualizar"
    override val nSamp = "SERVIDOR SAMP"
    override val nPort = "Porta"
    override val nTransport = "Transporte"
    override val nGamemode = "Gamemode"
    override val nHostname = "Hostname"
    override val nUptime = "Uptime"
    override val nCpu = "CPU"
    override val nRam = "RAM"
    override val nPingSamp = "Ping SAMP"
    override val nPingLocal = "Ping local"
    override val nNoCfg = "Não configurado"
    override val nPlayitDesc = "Conecte SUA conta Playit para disponibilizar o servidor na Internet. Cada pessoa usa a própria conta."
    override val nConnect = "CONECTAR PLAYIT"
    override val nState = "Estado"
    override val nConnected = "Conectado"
    override val nLinkedActive = "Vinculado (agent ativo)"
    override val nLinked = "Vinculado"
    override val nAgent = "Agent"
    override val nApStopped = "parado"
    override val nApStarting = "iniciando…"
    override val nApUnclaimed = "aguardando vínculo"
    override val nApRunning = "em execução"
    override val nApExited = "encerrou"
    override val nApAuthFail = "falha de autenticação"
    override val nUnlink = "Desvincular conta"
    override val nRelink = "Usar outra conta"
    override val nRelayNoreq = "Relay: Não necessário"
    override val nRelayDirect = "Conexão direta disponível ou modo local."
    override val nRsActive = "ATIVO"
    override val nRsStandby = "STANDBY"
    override val nRsFailed = "FALHOU"
    override val nRsNone = "NÃO CONFIGURADO"
    override val nEndpoint = "Endpoint"
    override val nLoss = "Perda"
    override val nJitter = "Jitter"
    override val nSession = "Sessão"
    override val nTraffic = "Tráfego"
    override val nPackets = "Pacotes"
    override val nTestRelay = "TESTAR RELAY"
    override val nRestartConn = "REINICIAR CONEXÃO"
    override val nDiag = "DIAGNÓSTICO DE REDE"
    override val nIface = "Interface"
    override val nIpv4Local = "IPv4 local"
    override val nIpv4Pub = "IPv4 público"
    override val nIpv6 = "IPv6"
    override val nUdp = "UDP"
    override val nTraversal = "Travessia"
    override val nDirectConn = "Conexão direta"
    override val nPa = "Playit agent"
    override val nPt = "Playit tunnel"
    override val nProxy = "Proxy DNS"
    override val nSampRow = "SAMP"
    override val nTurn = "TURN"
    override val nAvailable = "disponível"
    override val nUnavailLow = "indisponível"
    override val nNotLinked = "não vinculado"
    override val nActiveLow = "ativo"
    override val nReachOk = "alcance verificado"
    override val nResponds = "responde (%d ms)"
    override val nTechShow = "DETALHES TÉCNICOS"
    override val nTechHide = "Ocultar detalhes técnicos"
    override val nTechHost = "Arquitetura host"
    override val nTechGuest = "Guest"
    override val nTechBackend = "Backend"
    override val nTechStun = "STUN"
    override val nTechDirect = "Direct path"
    override val nTechProvider = "Provider"
    override val nTechTransport = "Transport"
    override val nTechRegion = "Region"
    override val nTechRelayLat = "Relay latency"
    override val nTechSession = "Session ID"
    override val nTechServer = "Server ID"
    override val nTechPackets = "Packets"
    override val nTechBytes = "Bytes"
    override val nTechHeartbeat = "Last heartbeat"
    override val nInfraLabel = "Fonte de infraestrutura (https)"
    override val nInfraSave = "SALVAR FONTE"
    override val nOffer = "COPIAR OFERTA (SINALIZAÇÃO)"
    override val nAnswerLabel = "Resposta de sinalização (JSON)"
    override val nAnswerImport = "IMPORTAR RESPOSTA"
    override val nLogTitle = "LOG TÉCNICO"
    override val nLogShow = "Ver logs"
    override val nLogHide = "Ocultar"
    override val nLogEmpty = "Nenhum evento ainda."
    override val nClaimTitle = "Conectar Playit"
    override val nClaimDesc = "A primeira vinculação exige autorização da sua conta Playit (uma única vez)."
    override val nClaimStep1 = "1. Abra o link abaixo no navegador"
    override val nClaimStep2 = "2. Aprove o agent e aguarde aqui"
    override val nClaimKeep = "Mantenha o aplicativo aberto: o agent precisa estar executando."
    override val nClaimWait = "Aguardando aprovação…"
    override val nClaimCopy = "COPIAR LINK"
    override val nChecking = "verificando…"
    override val nDegraded = "Limitada"
    override val nReconnecting = "Reconectando…"
    override val nViaRelay = "Via relay"
    override val sTitle = "Configurações"
    override val sCfgFile = "server.cfg: %s"
    override val sSave = "Salvar server.cfg"
    override val sGmInstalled = "Gamemodes instalados: %d"
    override val sGmHint = "Para importar .amx ou .so, use a aba Arquivos."
    override val sRestart = "Reiniciar"
    override val gTitle = "Diagnóstico"
    override val gLibs = "Bibliotecas necessárias"
    override val gTestBackend = "Testar backend"
    override val gCheckFiles = "Verificar arquivos"
    override val gCopyFull = "COPIAR DIAGNÓSTICO COMPLETO"
    override val gCopyBtn = "COPIAR"
    override val gLibsTitle = "Bibliotecas necessárias (%s)"
    override val gLibsFail = "Não foi possível carregar a lista."
    override val mCopying = "Copiando arquivos"
    override val mValidatingExe = "Validando executável"
    override val mLogSaved = "Log salvo: %s"
    override val mCfgSaved = "server.cfg salvo."
    override val mSrvAdded = "Servidor %1\$s adicionado. %2\$s"
    override val mReauthOk = "Pasta de %s autorizada novamente."
    override val mReauthFail = "Falha ao autorizar: %s"
    override val mBadName = "Nome inválido."
    override val mSrvCreated = "Servidor %s criado."
    override val mImportFail = "Importação falhou: %s"
    override val mPickAmx = "Selecione um arquivo .amx"
    override val mGmImported = "Gamemode %s.amx importado e ativado."
    override val mPickSo = "Selecione um plugin .so Linux"
    override val mPluginImported = "Plugin %s importado. Adicione em server.cfg se necessário."
    override val mFolderCopied = "Pasta: %s (caminho copiado)"
    override val mDiagCopied = "Diagnóstico copiado."
    override val mDiagFail = "Falha no diagnóstico: %s"
    override val mSrcUpdated = "Fonte atualizada. Rode Testar conexão."
    override val mLinkCopied = "Link copiado."
    override val mUnlinked = "Conta Playit desvinculada."
    override val mRelinked = "Conta anterior removida. Vincule a nova conta."
    override val mFail = "Falha: %s"
    override val mNoExt = "Conexão externa indisponível."
    override val mAddrCopied = "Endereço copiado: %s"
    override val mOfferCopied = "Oferta copiada (sessão %s…)"
    override val mExportFail = "Falha ao exportar: %s"
    override val mCandidates = "%d candidato(s) remoto(s) recebido(s)."
    override val mBadAnswer = "Resposta inválida: %s"
    override val gInstalled = "Instalado"
    override val gMissing = "Ausente"
    override val gYes = "SIM"
    override val gNo = "NÃO"
    override val gInvalid = "Inválido"
    override val gOk = "OK"
    override val gMissingN = "%d ausentes"
    override val mInstallingSvr = "Instalando samp03svr"
    override val mSvrKept = "samp03svr existente preservado."
    override val mSvrInstalled = "samp03svr instalado e validado."
    override val mSvrInvalid = "Atenção: samp03svr existente inválido (%s), preservado."
    override val mSvrInstallFail = "Não foi possível instalar o samp03svr: %s"
    override val mImportFolderFail = "Falha ao importar a pasta."
    override val mUnknownErr = "erro desconhecido"
    override val mSetupFail = "Falha ao preparar ambiente."
    override val mViewFailRead = "Não foi possível ler o arquivo"
    override val gDevArch = "Arquitetura do dispositivo"
    override val gRuntime = "Runtime"
    override val gGuestArch = "Arquitetura guest"
    override val gBackend = "Backend"
    override val gBackendAvail = "Backend disponível"
    override val gBackendPath = "Caminho do backend"
    override val gPlugins = "Plugins"
    override val gGamemodes = "Gamemodes"
    override val gServerDir = "Diretório do servidor"
    override val gProcess = "Processo"
    override val gPid = "PID"
    override val mPathCopied = "Caminho copiado."
    override val instVerifying = "Verificando dispositivo"
    override val instX86 = "Instalando compatibilidade x86"
    override val instLibs = "Preparando bibliotecas do servidor"
    override val instValidating = "Validando runtime"
    override val instReady = "Ambiente pronto"
}
    private class EnStrings : Strings {
    override val settingsTitle = "SETTINGS"
    override val settingsSubtitle = "Personalize your experience"
    override val secAppearance = "APPEARANCE"
    override val secServer = "SERVER"
    override val secConsole = "CONSOLE"
    override val secAbout = "ABOUT"
    override val lang = "Language"
    override val langDesc = "Choose the app language"
    override val mode = "Mode"
    override val modeDesc = "Set the app theme"
    override val modeDark = "Dark"
    override val modeLight = "Light"
    override val accent = "Accent color"
    override val accentDesc = "Choose the app main color"
    override val accentNameDefault = "Green"
    override val autoStart = "Auto start"
    override val autoStartDesc = "Starts the server when opening the app"
    override val keepBg = "Minimize to tray"
    override val keepBgDesc = "Keeps the server running in background"
    override val notif = "Notifications"
    override val notifDesc = "Receive server alerts"
    override val retention = "Auto-clear logs"
    override val retentionDesc = "Removes old logs automatically"
    override val retNever = "Never"
    override val ret1d = "1 day"
    override val ret7d = "7 days"
    override val ret30d = "30 days"
    override val consoleFont = "Font size"
    override val consoleFontDesc = "Sets the console font size"
    override val fontSmall = "Small"
    override val fontMedium = "Medium"
    override val fontLarge = "Large"
    override val consoleWrap = "Line wrap"
    override val consoleWrapDesc = "Enables automatic line wrapping"
    override val consoleTime = "Show timestamps"
    override val consoleTimeDesc = "Shows the time on messages"
    override val aboutVersion = "App version"
    override val aboutCredits = "Credits"
    override val creditsRole = "Developed by Gustavo Maccedo (IkeSamp)"
    override val creditsThanks = "Special thanks to the NexusDev group"
    override val thanksTitle = "Thanks for using SAMP Local!"
    override val thanksDesc = "Made with dedication for the community."
    override val developedBy = "SAMP Local"
    override val accentTitle = "Accent color"
    override val accentHex = "Hexadecimal"
    override val accentUse = "USE COLOR"
    override val accentReset = "Default"
    override val cancel = "Cancel"
    override val confirm = "Confirm"
    override val close = "Close"
    override val retry = "RETRY"
    override val tabHome = "Home"
    override val tabConsole = "Console"
    override val tabFiles = "Files"
    override val tabSettings = "Settings"
    override val wizardTagline = "Turn your Android into a local environment\nfor SA-MP servers."
    override val featDirs = "Directories"
    override val featRuntime = "Runtime"
    override val featDeps = "Dependencies"
    override val featArch = "Architecture check"
    override val start = "START"
    override val installTitle = "Environment setup"
    override val installDesc = "We are preparing everything so you can run your SA-MP server locally. This may take a few minutes."
    override val infoTitle = "No manual installation needed:"
    override val infoDesc = "the runtime ships inside the APK."
    override val dServer = "Server"
    override val dSwitch = "Switch server"
    override val dNewServer = "New server"
    override val dServerName = "Server name"
    override val dCreate = "Create"
    override val dAddress = "Address"
    override val dPlayers = "PLAYERS"
    override val dUptime = "UPTIME"
    override val dStart = "START"
    override val dRestart = "RESTART"
    override val dStop = "STOP"
    override val dInfo = "SERVER INFORMATION"
    override val dHostname = "Hostname"
    override val dGamemode = "Gamemode"
    override val dVersion = "Version"
    override val dPassword = "Password"
    override val dLanguage = "Language"
    override val dMap = "Map"
    override val dRuntime = "Runtime"
    override val dHost = "Host"
    override val dStatus = "Status"
    override val dMemory = "MEMORY USAGE"
    override val dCpu = "CPU USAGE"
    override val dMb = "MB"
    override val dMemStopped = "0 MB · stopped"
    override val dMemRunning = "%1\$d MB (%2\$d%%) · QEMU process (RSS)"
    override val dCpuStopped = "0% · stopped"
    override val dCpuRunning = "%d%% · QEMU process (host)"
    override val dConnDesc = "Server connectivity"
    override val dOpenSettings = "Open settings"
    override val stOnline = "ONLINE"
    override val stOffline = "OFFLINE"
    override val stStopped = "STOPPED"
    override val stStarting = "STARTING"
    override val stRunning = "RUNNING"
    override val stStopping = "STOPPING"
    override val stInstalling = "INSTALLING"
    override val stCrashed = "CRASHED"
    override val stError = "ERROR"
    override val fSubtitle = "SAMP server management"
    override val fImportServer = "IMPORT SERVER"
    override val fPlugin = "PLUGIN (.SO)"
    override val fChecking = "Checking server…"
    override val fFilesUnit = "%d files"
    override val fInvalidDir = "Invalid directory"
    override val fChooseOther = "CHOOSE ANOTHER FOLDER"
    override val fFilesTitle = "Server Files"
    override val fFolders = "%d folders"
    override val fFiles = "%d files"
    override val fImportFile = "Import file"
    override val fReauthTitle = "Source folder access expired"
    override val fReauthDesc = "The server is still saved. Authorize again to reimport."
    override val fReauth = "RE-AUTHORIZE FOLDER"
    override val fBreadcrumb = "Server"
    override val fBackDesc = "Back folder"
    override val fRefreshDesc = "Refresh list"
    override val fEmpty = "Empty folder"
    override val fEmptyDesc = "Add files to the server directory."
    override val fAddServer = "Add server"
    override val fVerifying = "Checking server"
    override val fDirFound = "Directory found"
    override val fCfgFound = "server.cfg found"
    override val fCfgMissing = "server.cfg missing"
    override val fGmNone = "No gamemodes"
    override val fGmCount = "%d gamemode(s)"
    override val fPlNone = "No plugins"
    override val fPlCount = "%d plugin(s)"
    override val fReady = "Server ready to add"
    override val fAdd = "ADD"
    override val fFolderDesc = "Folder"
    override val fFileDesc = "File"
    override val fOpenFolder = "Open folder"
    override val fFileActions = "File actions"
    override val fView = "View"
    override val fViewFail = "Could not preview."
    override val fRename = "Rename"
    override val fDelete = "Delete"
    override val fDeleteTitle = "Delete %s?"
    override val fDeleteDesc = "This cannot be undone."
    override val fOk = "OK"
    override val fRenamed = "Renamed to %s"
    override val fBadName = "Invalid name."
    override val fDeleted = "Deleted: %s"
    override val fDeleteFail = "Failed to delete."
    override val cClear = "CLEAR"
    override val cSave = "SAVE LOG"
    override val cTitle = "Console: %1\$s (%2\$d lines)"
    override val cEmpty = "SA-MP Dedicated Server\nWaiting for startup...\nStart the server on the Home tab."
    override val nTitle = "Server connectivity"
    override val nLan = "LOCAL NETWORK"
    override val nInternet = "INTERNET"
    override val nConn = "CONNECTIVITY"
    override val nMethod = "Method"
    override val nLocal = "Local"
    override val nLanLong = "Local network"
    override val nDirect = "Direct UDP"
    override val nPlayit = "PLAYIT"
    override val nRelay = "UDP RELAY"
    override val nAddress = "Address"
    override val nUnavailable = "Unavailable"
    override val nNat = "NAT"
    override val nNatNone = "No NAT"
    override val nNatCgnat = "Likely CGNAT"
    override val nNatDetected = "Detected"
    override val nNatNoNet = "No network"
    override val nNatUnknown = "Unknown"
    override val nLatency = "Latency"
    override val nPath = "CONNECTION PATH"
    override val nDevice = "DEVICE"
    override val nPlayers = "PLAYERS"
    override val nPlayersOnline = "%d online"
    override val nAddrTitle = "SERVER ADDRESS"
    override val nAddrNone = "ADDRESS UNAVAILABLE"
    override val nRelayDest = "Relay address, target: local server"
    override val nTest = "TEST CONNECTIVITY"
    override val nCopy = "COPY ADDRESS"
    override val nRefresh = "Refresh"
    override val nSamp = "SAMP SERVER"
    override val nPort = "Port"
    override val nTransport = "Transport"
    override val nGamemode = "Gamemode"
    override val nHostname = "Hostname"
    override val nUptime = "Uptime"
    override val nCpu = "CPU"
    override val nRam = "RAM"
    override val nPingSamp = "SAMP ping"
    override val nPingLocal = "Local ping"
    override val nNoCfg = "Not configured"
    override val nPlayitDesc = "Connect YOUR Playit account to expose the server on the Internet. Each person uses their own account."
    override val nConnect = "CONNECT PLAYIT"
    override val nState = "State"
    override val nConnected = "Connected"
    override val nLinkedActive = "Linked (agent active)"
    override val nLinked = "Linked"
    override val nAgent = "Agent"
    override val nApStopped = "stopped"
    override val nApStarting = "starting…"
    override val nApUnclaimed = "awaiting link"
    override val nApRunning = "running"
    override val nApExited = "exited"
    override val nApAuthFail = "authentication failed"
    override val nUnlink = "Unlink account"
    override val nRelink = "Use another account"
    override val nRelayNoreq = "Relay: Not needed"
    override val nRelayDirect = "Direct connection available or local mode."
    override val nRsActive = "ACTIVE"
    override val nRsStandby = "STANDBY"
    override val nRsFailed = "FAILED"
    override val nRsNone = "NOT CONFIGURED"
    override val nEndpoint = "Endpoint"
    override val nLoss = "Loss"
    override val nJitter = "Jitter"
    override val nSession = "Session"
    override val nTraffic = "Traffic"
    override val nPackets = "Packets"
    override val nTestRelay = "TEST RELAY"
    override val nRestartConn = "RESTART CONNECTION"
    override val nDiag = "NETWORK DIAGNOSTICS"
    override val nIface = "Interface"
    override val nIpv4Local = "Local IPv4"
    override val nIpv4Pub = "Public IPv4"
    override val nIpv6 = "IPv6"
    override val nUdp = "UDP"
    override val nTraversal = "Traversal"
    override val nDirectConn = "Direct connection"
    override val nPa = "Playit agent"
    override val nPt = "Playit tunnel"
    override val nProxy = "DNS proxy"
    override val nSampRow = "SAMP"
    override val nTurn = "TURN"
    override val nAvailable = "available"
    override val nUnavailLow = "unavailable"
    override val nNotLinked = "not linked"
    override val nActiveLow = "active"
    override val nReachOk = "reachability verified"
    override val nResponds = "responds (%d ms)"
    override val nTechShow = "TECHNICAL DETAILS"
    override val nTechHide = "Hide technical details"
    override val nTechHost = "Host architecture"
    override val nTechGuest = "Guest"
    override val nTechBackend = "Backend"
    override val nTechStun = "STUN"
    override val nTechDirect = "Direct path"
    override val nTechProvider = "Provider"
    override val nTechTransport = "Transport"
    override val nTechRegion = "Region"
    override val nTechRelayLat = "Relay latency"
    override val nTechSession = "Session ID"
    override val nTechServer = "Server ID"
    override val nTechPackets = "Packets"
    override val nTechBytes = "Bytes"
    override val nTechHeartbeat = "Last heartbeat"
    override val nInfraLabel = "Infrastructure source (https)"
    override val nInfraSave = "SAVE SOURCE"
    override val nOffer = "COPY OFFER (SIGNALING)"
    override val nAnswerLabel = "Signaling answer (JSON)"
    override val nAnswerImport = "IMPORT ANSWER"
    override val nLogTitle = "TECHNICAL LOG"
    override val nLogShow = "View logs"
    override val nLogHide = "Hide"
    override val nLogEmpty = "No events yet."
    override val nClaimTitle = "Connect Playit"
    override val nClaimDesc = "First-time linking requires authorizing your Playit account (once)."
    override val nClaimStep1 = "1. Open the link below in your browser"
    override val nClaimStep2 = "2. Approve the agent and wait here"
    override val nClaimKeep = "Keep the app open: the agent must be running."
    override val nClaimWait = "Waiting for approval…"
    override val nClaimCopy = "COPY LINK"
    override val nChecking = "checking…"
    override val nDegraded = "Limited"
    override val nReconnecting = "Reconnecting…"
    override val nViaRelay = "Via relay"
    override val sTitle = "Settings"
    override val sCfgFile = "server.cfg: %s"
    override val sSave = "Save server.cfg"
    override val sGmInstalled = "Installed gamemodes: %d"
    override val sGmHint = "To import .amx or .so, use the Files tab."
    override val sRestart = "Restart"
    override val gTitle = "Diagnostics"
    override val gLibs = "Required libraries"
    override val gTestBackend = "Test backend"
    override val gCheckFiles = "Check files"
    override val gCopyFull = "COPY FULL DIAGNOSTICS"
    override val gCopyBtn = "COPY"
    override val gLibsTitle = "Required libraries (%s)"
    override val gLibsFail = "Could not load the list."
    override val mCopying = "Copying files"
    override val mValidatingExe = "Validating executable"
    override val mLogSaved = "Log saved: %s"
    override val mCfgSaved = "server.cfg saved."
    override val mSrvAdded = "Server %1\$s added. %2\$s"
    override val mReauthOk = "%s folder authorized again."
    override val mReauthFail = "Authorization failed: %s"
    override val mBadName = "Invalid name."
    override val mSrvCreated = "Server %s created."
    override val mImportFail = "Import failed: %s"
    override val mPickAmx = "Select an .amx file"
    override val mGmImported = "Gamemode %s.amx imported and enabled."
    override val mPickSo = "Select a Linux .so plugin"
    override val mPluginImported = "Plugin %s imported. Add it to server.cfg if needed."
    override val mFolderCopied = "Folder: %s (path copied)"
    override val mDiagCopied = "Diagnostics copied."
    override val mDiagFail = "Diagnostics failed: %s"
    override val mSrcUpdated = "Source updated. Run Test connection."
    override val mLinkCopied = "Link copied."
    override val mUnlinked = "Playit account unlinked."
    override val mRelinked = "Previous account removed. Link the new account."
    override val mFail = "Failed: %s"
    override val mNoExt = "External connection unavailable."
    override val mAddrCopied = "Address copied: %s"
    override val mOfferCopied = "Offer copied (session %s…)"
    override val mExportFail = "Failed to export: %s"
    override val mCandidates = "%d remote candidate(s) received."
    override val mBadAnswer = "Invalid answer: %s"
    override val gInstalled = "Installed"
    override val gMissing = "Missing"
    override val gYes = "YES"
    override val gNo = "NO"
    override val gInvalid = "Invalid"
    override val gOk = "OK"
    override val gMissingN = "%d missing"
    override val mInstallingSvr = "Installing samp03svr"
    override val mSvrKept = "existing samp03svr preserved."
    override val mSvrInstalled = "samp03svr installed and validated."
    override val mSvrInvalid = "Warning: existing samp03svr invalid (%s), preserved."
    override val mSvrInstallFail = "Could not install samp03svr: %s"
    override val mImportFolderFail = "Failed to import folder."
    override val mUnknownErr = "unknown error"
    override val mSetupFail = "Failed to prepare environment."
    override val mViewFailRead = "Could not read the file"
    override val gDevArch = "Device architecture"
    override val gRuntime = "Runtime"
    override val gGuestArch = "Guest architecture"
    override val gBackend = "Backend"
    override val gBackendAvail = "Backend available"
    override val gBackendPath = "Backend path"
    override val gPlugins = "Plugins"
    override val gGamemodes = "Gamemodes"
    override val gServerDir = "Server directory"
    override val gProcess = "Process"
    override val gPid = "PID"
    override val mPathCopied = "Path copied."
    override val instVerifying = "Checking device"
    override val instX86 = "Installing x86 compatibility"
    override val instLibs = "Preparing server libraries"
    override val instValidating = "Validating runtime"
    override val instReady = "Environment ready"
}
    private class EsStrings : Strings {
    override val settingsTitle = "AJUSTES"
    override val settingsSubtitle = "Personaliza tu experiencia"
    override val secAppearance = "APARIENCIA"
    override val secServer = "SERVIDOR"
    override val secConsole = "CONSOLA"
    override val secAbout = "ACERCA DE"
    override val lang = "Idioma"
    override val langDesc = "Elige el idioma de la aplicación"
    override val mode = "Modo"
    override val modeDesc = "Define el tema de la aplicación"
    override val modeDark = "Oscuro"
    override val modeLight = "Claro"
    override val accent = "Color de destaque"
    override val accentDesc = "Elige el color principal de la app"
    override val accentNameDefault = "Verde"
    override val autoStart = "Inicio automático"
    override val autoStartDesc = "Inicia el servidor al abrir la app"
    override val keepBg = "Minimizar a la bandeja"
    override val keepBgDesc = "Mantiene el servidor en segundo plano"
    override val notif = "Notificaciones"
    override val notifDesc = "Recibe avisos sobre el servidor"
    override val retention = "Borrar registros automáticamente"
    override val retentionDesc = "Elimina registros antiguos automáticamente"
    override val retNever = "Nunca"
    override val ret1d = "1 día"
    override val ret7d = "7 días"
    override val ret30d = "30 días"
    override val consoleFont = "Tamaño de fuente"
    override val consoleFontDesc = "Define el tamaño de fuente en la consola"
    override val fontSmall = "Pequeño"
    override val fontMedium = "Mediano"
    override val fontLarge = "Grande"
    override val consoleWrap = "Ajuste de línea"
    override val consoleWrapDesc = "Activa el ajuste de línea automático"
    override val consoleTime = "Mostrar marcas de tiempo"
    override val consoleTimeDesc = "Muestra la hora en los mensajes"
    override val aboutVersion = "Versión de la aplicación"
    override val aboutCredits = "Créditos"
    override val creditsRole = "Desarrollado por Gustavo Maccedo (IkeSamp)"
    override val creditsThanks = "Agradecimiento especial al grupo NexusDev"
    override val thanksTitle = "¡Gracias por usar SAMP Local!"
    override val thanksDesc = "Hecho con dedicación para la comunidad."
    override val developedBy = "SAMP Local"
    override val accentTitle = "Color de destaque"
    override val accentHex = "Hexadecimal"
    override val accentUse = "USAR COLOR"
    override val accentReset = "Predeterminado"
    override val cancel = "Cancelar"
    override val confirm = "Confirmar"
    override val close = "Cerrar"
    override val retry = "REINTENTAR"
    override val tabHome = "Inicio"
    override val tabConsole = "Consola"
    override val tabFiles = "Archivos"
    override val tabSettings = "Ajustes"
    override val wizardTagline = "Convierte tu Android en un entorno local\npara servidores SA-MP."
    override val featDirs = "Directorios"
    override val featRuntime = "Runtime"
    override val featDeps = "Dependencias"
    override val featArch = "Verificación de arquitectura"
    override val start = "COMENZAR"
    override val installTitle = "Instalación del entorno"
    override val installDesc = "Estamos preparando todo para que ejecutes tu servidor SA-MP localmente. Esto puede tardar unos minutos."
    override val infoTitle = "No se necesita instalación manual:"
    override val infoDesc = "el runtime viene dentro del APK."
    override val dServer = "Servidor"
    override val dSwitch = "Cambiar servidor"
    override val dNewServer = "Nuevo servidor"
    override val dServerName = "Nombre del servidor"
    override val dCreate = "Crear"
    override val dAddress = "Dirección"
    override val dPlayers = "PLAYERS"
    override val dUptime = "UPTIME"
    override val dStart = "INICIAR"
    override val dRestart = "REINICIAR"
    override val dStop = "DETENER"
    override val dInfo = "INFORMACIÓN DEL SERVIDOR"
    override val dHostname = "Hostname"
    override val dGamemode = "Gamemode"
    override val dVersion = "Versión"
    override val dPassword = "Contraseña"
    override val dLanguage = "Idioma"
    override val dMap = "Mapa"
    override val dRuntime = "Runtime"
    override val dHost = "Host"
    override val dStatus = "Estado"
    override val dMemory = "USO DE MEMORIA"
    override val dCpu = "USO DE CPU"
    override val dMb = "MB"
    override val dMemStopped = "0 MB · detenido"
    override val dMemRunning = "%1\$d MB (%2\$d%%) · proceso QEMU (RSS)"
    override val dCpuStopped = "0% · detenido"
    override val dCpuRunning = "%d%% · proceso QEMU (host)"
    override val dConnDesc = "Conectividad del servidor"
    override val dOpenSettings = "Abrir ajustes"
    override val stOnline = "EN LÍNEA"
    override val stOffline = "DESCONECTADO"
    override val stStopped = "DETENIDO"
    override val stStarting = "INICIANDO"
    override val stRunning = "EN EJECUCIÓN"
    override val stStopping = "DETENIENDO"
    override val stInstalling = "INSTALANDO"
    override val stCrashed = "FALLÓ"
    override val stError = "ERROR"
    override val fSubtitle = "Gestión de servidores SAMP"
    override val fImportServer = "IMPORTAR SERVIDOR"
    override val fPlugin = "PLUGIN (.SO)"
    override val fChecking = "Verificando servidor…"
    override val fFilesUnit = "%d archivos"
    override val fInvalidDir = "Directorio inválido"
    override val fChooseOther = "ELEGIR OTRA CARPETA"
    override val fFilesTitle = "Archivos del servidor"
    override val fFolders = "%d carpetas"
    override val fFiles = "%d archivos"
    override val fImportFile = "Importar archivo"
    override val fReauthTitle = "Acceso a la carpeta expirado"
    override val fReauthDesc = "El servidor sigue guardado. Autorice de nuevo para reimportar."
    override val fReauth = "REAUTORIZAR CARPETA"
    override val fBreadcrumb = "Servidor"
    override val fBackDesc = "Carpeta anterior"
    override val fRefreshDesc = "Actualizar lista"
    override val fEmpty = "Carpeta vacía"
    override val fEmptyDesc = "Agregue archivos al directorio del servidor."
    override val fAddServer = "Agregar servidor"
    override val fVerifying = "Verificando servidor"
    override val fDirFound = "Directorio encontrado"
    override val fCfgFound = "server.cfg encontrado"
    override val fCfgMissing = "server.cfg ausente"
    override val fGmNone = "Sin gamemodes"
    override val fGmCount = "%d gamemode(s)"
    override val fPlNone = "Sin plugins"
    override val fPlCount = "%d plugin(s)"
    override val fReady = "Servidor listo para agregar"
    override val fAdd = "AGREGAR"
    override val fFolderDesc = "Carpeta"
    override val fFileDesc = "Archivo"
    override val fOpenFolder = "Abrir carpeta"
    override val fFileActions = "Acciones del archivo"
    override val fView = "Ver"
    override val fViewFail = "No se pudo previsualizar."
    override val fRename = "Renombrar"
    override val fDelete = "Eliminar"
    override val fDeleteTitle = "¿Eliminar %s?"
    override val fDeleteDesc = "Esta acción no se puede deshacer."
    override val fOk = "OK"
    override val fRenamed = "Renombrado a %s"
    override val fBadName = "Nombre inválido."
    override val fDeleted = "Eliminado: %s"
    override val fDeleteFail = "No se pudo eliminar."
    override val cClear = "LIMPIAR"
    override val cSave = "GUARDAR LOG"
    override val cTitle = "Consola: %1\$s (%2\$d líneas)"
    override val cEmpty = "SA-MP Dedicated Server\nEsperando inicio...\nInicie el servidor en la pestaña Inicio."
    override val nTitle = "Conectividad del servidor"
    override val nLan = "RED LOCAL"
    override val nInternet = "INTERNET"
    override val nConn = "CONECTIVIDAD"
    override val nMethod = "Método"
    override val nLocal = "Local"
    override val nLanLong = "Red local"
    override val nDirect = "Direct UDP"
    override val nPlayit = "PLAYIT"
    override val nRelay = "UDP RELAY"
    override val nAddress = "Dirección"
    override val nUnavailable = "No disponible"
    override val nNat = "NAT"
    override val nNatNone = "Sin NAT"
    override val nNatCgnat = "Probable CGNAT"
    override val nNatDetected = "Detectado"
    override val nNatNoNet = "Sin red"
    override val nNatUnknown = "Indeterminado"
    override val nLatency = "Latencia"
    override val nPath = "RUTA DE CONEXIÓN"
    override val nDevice = "DISPOSITIVO"
    override val nPlayers = "JUGADORES"
    override val nPlayersOnline = "%d online"
    override val nAddrTitle = "DIRECCIÓN DEL SERVIDOR"
    override val nAddrNone = "DIRECCIÓN NO DISPONIBLE"
    override val nRelayDest = "Dirección del relay, destino: servidor local"
    override val nTest = "PROBAR CONECTIVIDAD"
    override val nCopy = "COPIAR DIRECCIÓN"
    override val nRefresh = "Actualizar"
    override val nSamp = "SERVIDOR SAMP"
    override val nPort = "Puerto"
    override val nTransport = "Transporte"
    override val nGamemode = "Gamemode"
    override val nHostname = "Hostname"
    override val nUptime = "Tiempo activo"
    override val nCpu = "CPU"
    override val nRam = "RAM"
    override val nPingSamp = "Ping SAMP"
    override val nPingLocal = "Ping local"
    override val nNoCfg = "No configurado"
    override val nPlayitDesc = "Conecte SU cuenta Playit para exponer el servidor en Internet. Cada persona usa su propia cuenta."
    override val nConnect = "CONECTAR PLAYIT"
    override val nState = "Estado"
    override val nConnected = "Conectado"
    override val nLinkedActive = "Vinculado (agente activo)"
    override val nLinked = "Vinculado"
    override val nAgent = "Agent"
    override val nApStopped = "detenido"
    override val nApStarting = "iniciando…"
    override val nApUnclaimed = "esperando vínculo"
    override val nApRunning = "en ejecución"
    override val nApExited = "terminó"
    override val nApAuthFail = "fallo de autenticación"
    override val nUnlink = "Desvincular cuenta"
    override val nRelink = "Usar otra cuenta"
    override val nRelayNoreq = "Relay: No necesario"
    override val nRelayDirect = "Conexión directa disponible o modo local."
    override val nRsActive = "ACTIVO"
    override val nRsStandby = "STANDBY"
    override val nRsFailed = "FALLÓ"
    override val nRsNone = "NO CONFIGURADO"
    override val nEndpoint = "Endpoint"
    override val nLoss = "Pérdida"
    override val nJitter = "Jitter"
    override val nSession = "Sesión"
    override val nTraffic = "Tráfico"
    override val nPackets = "Paquetes"
    override val nTestRelay = "PROBAR RELAY"
    override val nRestartConn = "REINICIAR CONEXIÓN"
    override val nDiag = "DIAGNÓSTICO DE RED"
    override val nIface = "Interfaz"
    override val nIpv4Local = "IPv4 local"
    override val nIpv4Pub = "IPv4 pública"
    override val nIpv6 = "IPv6"
    override val nUdp = "UDP"
    override val nTraversal = "Atravesamiento"
    override val nDirectConn = "Conexión directa"
    override val nPa = "Playit agent"
    override val nPt = "Playit tunnel"
    override val nProxy = "Proxy DNS"
    override val nSampRow = "SAMP"
    override val nTurn = "TURN"
    override val nAvailable = "disponible"
    override val nUnavailLow = "no disponible"
    override val nNotLinked = "no vinculado"
    override val nActiveLow = "activo"
    override val nReachOk = "alcance verificado"
    override val nResponds = "responde (%d ms)"
    override val nTechShow = "DETALLES TÉCNICOS"
    override val nTechHide = "Ocultar detalles técnicos"
    override val nTechHost = "Arquitectura host"
    override val nTechGuest = "Guest"
    override val nTechBackend = "Backend"
    override val nTechStun = "STUN"
    override val nTechDirect = "Direct path"
    override val nTechProvider = "Provider"
    override val nTechTransport = "Transport"
    override val nTechRegion = "Region"
    override val nTechRelayLat = "Relay latency"
    override val nTechSession = "Session ID"
    override val nTechServer = "Server ID"
    override val nTechPackets = "Packets"
    override val nTechBytes = "Bytes"
    override val nTechHeartbeat = "Last heartbeat"
    override val nInfraLabel = "Fuente de infraestructura (https)"
    override val nInfraSave = "GUARDAR FUENTE"
    override val nOffer = "COPIAR OFERTA (SEÑALIZACIÓN)"
    override val nAnswerLabel = "Respuesta de señalización (JSON)"
    override val nAnswerImport = "IMPORTAR RESPUESTA"
    override val nLogTitle = "REGISTRO TÉCNICO"
    override val nLogShow = "Ver registros"
    override val nLogHide = "Ocultar"
    override val nLogEmpty = "Sin eventos aún."
    override val nClaimTitle = "Conectar Playit"
    override val nClaimDesc = "La primera vinculación exige autorizar su cuenta Playit (una sola vez)."
    override val nClaimStep1 = "1. Abra el enlace en el navegador"
    override val nClaimStep2 = "2. Apruebe el agente y espere aquí"
    override val nClaimKeep = "Mantenga la app abierta: el agente debe estar ejecutándose."
    override val nClaimWait = "Esperando aprobación…"
    override val nClaimCopy = "COPIAR ENLACE"
    override val nChecking = "verificando…"
    override val nDegraded = "Limitada"
    override val nReconnecting = "Reconectando…"
    override val nViaRelay = "Vía relay"
    override val sTitle = "Ajustes"
    override val sCfgFile = "server.cfg: %s"
    override val sSave = "Guardar server.cfg"
    override val sGmInstalled = "Gamemodes instalados: %d"
    override val sGmHint = "Para importar .amx o .so, use la pestaña Archivos."
    override val sRestart = "Reiniciar"
    override val gTitle = "Diagnóstico"
    override val gLibs = "Bibliotecas necesarias"
    override val gTestBackend = "Probar backend"
    override val gCheckFiles = "Verificar archivos"
    override val gCopyFull = "COPIAR DIAGNÓSTICO COMPLETO"
    override val gCopyBtn = "COPIAR"
    override val gLibsTitle = "Bibliotecas necesarias (%s)"
    override val gLibsFail = "No se pudo cargar la lista."
    override val mCopying = "Copiando archivos"
    override val mValidatingExe = "Validando ejecutable"
    override val mLogSaved = "Registro guardado: %s"
    override val mCfgSaved = "server.cfg guardado."
    override val mSrvAdded = "Servidor %1\$s agregado. %2\$s"
    override val mReauthOk = "Carpeta de %s autorizada de nuevo."
    override val mReauthFail = "Fallo al autorizar: %s"
    override val mBadName = "Nombre inválido."
    override val mSrvCreated = "Servidor %s creado."
    override val mImportFail = "Falló la importación: %s"
    override val mPickAmx = "Seleccione un archivo .amx"
    override val mGmImported = "Gamemode %s.amx importado y activado."
    override val mPickSo = "Seleccione un plugin .so Linux"
    override val mPluginImported = "Plugin %s importado. Agréguelo a server.cfg si es necesario."
    override val mFolderCopied = "Carpeta: %s (ruta copiada)"
    override val mDiagCopied = "Diagnóstico copiado."
    override val mDiagFail = "Fallo en el diagnóstico: %s"
    override val mSrcUpdated = "Fuente actualizada. Ejecute Probar conexión."
    override val mLinkCopied = "Enlace copiado."
    override val mUnlinked = "Cuenta Playit desvinculada."
    override val mRelinked = "Cuenta anterior eliminada. Vincule la nueva cuenta."
    override val mFail = "Fallo: %s"
    override val mNoExt = "Conexión externa no disponible."
    override val mAddrCopied = "Dirección copiada: %s"
    override val mOfferCopied = "Oferta copiada (sesión %s…)"
    override val mExportFail = "Fallo al exportar: %s"
    override val mCandidates = "%d candidato(s) remoto(s) recibido(s)."
    override val mBadAnswer = "Respuesta inválida: %s"
    override val gInstalled = "Instalado"
    override val gMissing = "Ausente"
    override val gYes = "SÍ"
    override val gNo = "NO"
    override val gInvalid = "Inválido"
    override val gOk = "OK"
    override val gMissingN = "%d ausentes"
    override val mInstallingSvr = "Instalando samp03svr"
    override val mSvrKept = "samp03svr existente preservado."
    override val mSvrInstalled = "samp03svr instalado y validado."
    override val mSvrInvalid = "Atención: samp03svr existente inválido (%s), preservado."
    override val mSvrInstallFail = "No se pudo instalar samp03svr: %s"
    override val mImportFolderFail = "Fallo al importar la carpeta."
    override val mUnknownErr = "error desconocido"
    override val mSetupFail = "Fallo al preparar el entorno."
    override val mViewFailRead = "No se pudo leer el archivo"
    override val gDevArch = "Arquitectura del dispositivo"
    override val gRuntime = "Runtime"
    override val gGuestArch = "Arquitectura guest"
    override val gBackend = "Backend"
    override val gBackendAvail = "Backend disponible"
    override val gBackendPath = "Ruta del backend"
    override val gPlugins = "Plugins"
    override val gGamemodes = "Gamemodes"
    override val gServerDir = "Directorio del servidor"
    override val gProcess = "Proceso"
    override val gPid = "PID"
    override val mPathCopied = "Ruta copiada."
    override val instVerifying = "Verificando dispositivo"
    override val instX86 = "Instalando compatibilidad x86"
    override val instLibs = "Preparando bibliotecas del servidor"
    override val instValidating = "Validando runtime"
    override val instReady = "Ambiente listo"
}
    private class RuStrings : Strings {
    override val settingsTitle = "НАСТРОЙКИ"
    override val settingsSubtitle = "Настройте приложение под себя"
    override val secAppearance = "ВНЕШНИЙ ВИД"
    override val secServer = "СЕРВЕР"
    override val secConsole = "КОНСОЛЬ"
    override val secAbout = "О ПРИЛОЖЕНИИ"
    override val lang = "Язык"
    override val langDesc = "Выберите язык приложения"
    override val mode = "Режим"
    override val modeDesc = "Выберите тему приложения"
    override val modeDark = "Тёмная"
    override val modeLight = "Светлая"
    override val accent = "Акцентный цвет"
    override val accentDesc = "Выберите основной цвет приложения"
    override val accentNameDefault = "Зелёный"
    override val autoStart = "Автозапуск"
    override val autoStartDesc = "Запускать сервер при открытии приложения"
    override val keepBg = "Свернуть в трей"
    override val keepBgDesc = "Оставлять сервер работать в фоне"
    override val notif = "Уведомления"
    override val notifDesc = "Получать оповещения о сервере"
    override val retention = "Автоочистка логов"
    override val retentionDesc = "Автоматически удалять старые логи"
    override val retNever = "Никогда"
    override val ret1d = "1 день"
    override val ret7d = "7 дней"
    override val ret30d = "30 дней"
    override val consoleFont = "Размер шрифта"
    override val consoleFontDesc = "Размер шрифта в консоли"
    override val fontSmall = "Мелкий"
    override val fontMedium = "Средний"
    override val fontLarge = "Крупный"
    override val consoleWrap = "Перенос строк"
    override val consoleWrapDesc = "Автоматический перенос строк"
    override val consoleTime = "Показывать время"
    override val consoleTimeDesc = "Показывать время в сообщениях"
    override val aboutVersion = "Версия приложения"
    override val aboutCredits = "Авторы"
    override val creditsRole = "Разработчик: Gustavo Maccedo (IkeSamp)"
    override val creditsThanks = "Особая благодарность группе NexusDev"
    override val thanksTitle = "Спасибо, что используете SAMP Local!"
    override val thanksDesc = "Сделано с любовью для сообщества."
    override val developedBy = "SAMP Local"
    override val accentTitle = "Акцентный цвет"
    override val accentHex = "Шестнадцатеричный код"
    override val accentUse = "ПРИМЕНИТЬ"
    override val accentReset = "По умолчанию"
    override val cancel = "Отмена"
    override val confirm = "Подтвердить"
    override val close = "Закрыть"
    override val retry = "ПОВТОРИТЬ"
    override val tabHome = "Главная"
    override val tabConsole = "Консоль"
    override val tabFiles = "Файлы"
    override val tabSettings = "Настройки"
    override val wizardTagline = "Превратите Android в локальную среду\nдля SA-MP серверов."
    override val featDirs = "Каталоги"
    override val featRuntime = "Runtime"
    override val featDeps = "Зависимости"
    override val featArch = "Проверка архитектуры"
    override val start = "НАЧАТЬ"
    override val installTitle = "Установка окружения"
    override val installDesc = "Готовим всё для запуска вашего SA-MP сервера локально. Это может занять несколько минут."
    override val infoTitle = "Ручная установка не требуется:"
    override val infoDesc = "runtime уже внутри APK."
    override val dServer = "Сервер"
    override val dSwitch = "Сменить сервер"
    override val dNewServer = "Новый сервер"
    override val dServerName = "Имя сервера"
    override val dCreate = "Создать"
    override val dAddress = "Адрес"
    override val dPlayers = "ИГРОКИ"
    override val dUptime = "АПТАЙМ"
    override val dStart = "ЗАПУСК"
    override val dRestart = "ПЕРЕЗАПУСК"
    override val dStop = "СТОП"
    override val dInfo = "ИНФОРМАЦИЯ О СЕРВЕРЕ"
    override val dHostname = "Hostname"
    override val dGamemode = "Gamemode"
    override val dVersion = "Версия"
    override val dPassword = "Пароль"
    override val dLanguage = "Язык"
    override val dMap = "Карта"
    override val dRuntime = "Runtime"
    override val dHost = "Host"
    override val dStatus = "Состояние"
    override val dMemory = "ИСПОЛЬЗОВАНИЕ ПАМЯТИ"
    override val dCpu = "ИСПОЛЬЗОВАНИЕ CPU"
    override val dMb = "МБ"
    override val dMemStopped = "0 МБ · остановлен"
    override val dMemRunning = "%1\$d МБ (%2\$d%%) · процесс QEMU (RSS)"
    override val dCpuStopped = "0% · остановлен"
    override val dCpuRunning = "%d%% · процесс QEMU (хост)"
    override val dConnDesc = "Подключение сервера"
    override val dOpenSettings = "Открыть настройки"
    override val stOnline = "ОНЛАЙН"
    override val stOffline = "ОФЛАЙН"
    override val stStopped = "ОСТАНОВЛЕН"
    override val stStarting = "ЗАПУСК"
    override val stRunning = "РАБОТАЕТ"
    override val stStopping = "ОСТАНОВКА"
    override val stInstalling = "УСТАНОВКА"
    override val stCrashed = "СБОЙ"
    override val stError = "ОШИБКА"
    override val fSubtitle = "Управление SAMP серверами"
    override val fImportServer = "ИМПОРТ СЕРВЕРА"
    override val fPlugin = "ПЛАГИН (.SO)"
    override val fChecking = "Проверка сервера…"
    override val fFilesUnit = "файлов: %d"
    override val fInvalidDir = "Недопустимый каталог"
    override val fChooseOther = "ВЫБРАТЬ ДРУГУЮ ПАПКУ"
    override val fFilesTitle = "Файлы сервера"
    override val fFolders = "Папок: %d"
    override val fFiles = "Файлов: %d"
    override val fImportFile = "Импорт файла"
    override val fReauthTitle = "Доступ к папке истёк"
    override val fReauthDesc = "Сервер сохранён. Разрешите доступ заново для повторного импорта."
    override val fReauth = "РАЗРЕШИТЬ ЗАНОВО"
    override val fBreadcrumb = "Сервер"
    override val fBackDesc = "Папка назад"
    override val fRefreshDesc = "Обновить список"
    override val fEmpty = "Папка пуста"
    override val fEmptyDesc = "Добавьте файлы в каталог сервера."
    override val fAddServer = "Добавить сервер"
    override val fVerifying = "Проверка сервера"
    override val fDirFound = "Каталог найден"
    override val fCfgFound = "server.cfg найден"
    override val fCfgMissing = "server.cfg отсутствует"
    override val fGmNone = "Нет гейммодов"
    override val fGmCount = "Гейммодов: %d"
    override val fPlNone = "Нет плагинов"
    override val fPlCount = "Плагинов: %d"
    override val fReady = "Сервер готов к добавлению"
    override val fAdd = "ДОБАВИТЬ"
    override val fFolderDesc = "Папка"
    override val fFileDesc = "Файл"
    override val fOpenFolder = "Открыть папку"
    override val fFileActions = "Действия с файлом"
    override val fView = "Просмотр"
    override val fViewFail = "Не удалось просмотреть."
    override val fRename = "Переименовать"
    override val fDelete = "Удалить"
    override val fDeleteTitle = "Удалить %s?"
    override val fDeleteDesc = "Это действие нельзя отменить."
    override val fOk = "OK"
    override val fRenamed = "Переименовано в %s"
    override val fBadName = "Недопустимое имя."
    override val fDeleted = "Удалено: %s"
    override val fDeleteFail = "Не удалось удалить."
    override val cClear = "ОЧИСТИТЬ"
    override val cSave = "СОХРАНИТЬ ЛОГ"
    override val cTitle = "Консоль: %1\$s (строк: %2\$d)"
    override val cEmpty = "SA-MP Dedicated Server\nОжидание запуска...\nЗапустите сервер на вкладке Главная."
    override val nTitle = "Подключение сервера"
    override val nLan = "ЛОКАЛЬНАЯ СЕТЬ"
    override val nInternet = "ИНТЕРНЕТ"
    override val nConn = "ПОДКЛЮЧЕНИЕ"
    override val nMethod = "Метод"
    override val nLocal = "Локально"
    override val nLanLong = "Локальная сеть"
    override val nDirect = "Direct UDP"
    override val nPlayit = "PLAYIT"
    override val nRelay = "UDP RELAY"
    override val nAddress = "Адрес"
    override val nUnavailable = "Недоступно"
    override val nNat = "NAT"
    override val nNatNone = "Без NAT"
    override val nNatCgnat = "Вероятно CGNAT"
    override val nNatDetected = "Обнаружен"
    override val nNatNoNet = "Нет сети"
    override val nNatUnknown = "Не определён"
    override val nLatency = "Задержка"
    override val nPath = "МАРШРУТ ПОДКЛЮЧЕНИЯ"
    override val nDevice = "УСТРОЙСТВО"
    override val nPlayers = "ИГРОКИ"
    override val nPlayersOnline = "%d online"
    override val nAddrTitle = "АДРЕС СЕРВЕРА"
    override val nAddrNone = "АДРЕС НЕДОСТУПЕН"
    override val nRelayDest = "Адрес relay, цель: локальный сервер"
    override val nTest = "ПРОВЕРИТЬ ПОДКЛЮЧЕНИЕ"
    override val nCopy = "КОПИРОВАТЬ АДРЕС"
    override val nRefresh = "Обновить"
    override val nSamp = "SAMP СЕРВЕР"
    override val nPort = "Порт"
    override val nTransport = "Транспорт"
    override val nGamemode = "Gamemode"
    override val nHostname = "Hostname"
    override val nUptime = "Аптайм"
    override val nCpu = "CPU"
    override val nRam = "RAM"
    override val nPingSamp = "Пинг SAMP"
    override val nPingLocal = "Локальный пинг"
    override val nNoCfg = "Не настроено"
    override val nPlayitDesc = "Подключите СВОЙ аккаунт Playit, чтобы открыть сервер в Интернет. У каждого свой аккаунт."
    override val nConnect = "ПОДКЛЮЧИТЬ PLAYIT"
    override val nState = "Состояние"
    override val nConnected = "Подключено"
    override val nLinkedActive = "Привязано (агент активен)"
    override val nLinked = "Привязано"
    override val nAgent = "Agent"
    override val nApStopped = "остановлен"
    override val nApStarting = "запуск…"
    override val nApUnclaimed = "ожидание привязки"
    override val nApRunning = "работает"
    override val nApExited = "завершён"
    override val nApAuthFail = "ошибка аутентификации"
    override val nUnlink = "Отвязать аккаунт"
    override val nRelink = "Другой аккаунт"
    override val nRelayNoreq = "Relay: не требуется"
    override val nRelayDirect = "Доступно прямое подключение или локальный режим."
    override val nRsActive = "АКТИВЕН"
    override val nRsStandby = "STANDBY"
    override val nRsFailed = "ОШИБКА"
    override val nRsNone = "НЕ НАСТРОЕН"
    override val nEndpoint = "Endpoint"
    override val nLoss = "Потери"
    override val nJitter = "Jitter"
    override val nSession = "Сессия"
    override val nTraffic = "Трафик"
    override val nPackets = "Пакеты"
    override val nTestRelay = "ПРОВЕРИТЬ RELAY"
    override val nRestartConn = "ПЕРЕЗАПУСТИТЬ"
    override val nDiag = "ДИАГНОСТИКА СЕТИ"
    override val nIface = "Интерфейс"
    override val nIpv4Local = "Локальный IPv4"
    override val nIpv4Pub = "Публичный IPv4"
    override val nIpv6 = "IPv6"
    override val nUdp = "UDP"
    override val nTraversal = "Прохождение"
    override val nDirectConn = "Прямое соединение"
    override val nPa = "Playit agent"
    override val nPt = "Playit tunnel"
    override val nProxy = "DNS прокси"
    override val nSampRow = "SAMP"
    override val nTurn = "TURN"
    override val nAvailable = "доступно"
    override val nUnavailLow = "недоступно"
    override val nNotLinked = "не привязано"
    override val nActiveLow = "активен"
    override val nReachOk = "доступ проверен"
    override val nResponds = "отвечает (%d мс)"
    override val nTechShow = "ТЕХНИЧЕСКИЕ ДЕТАЛИ"
    override val nTechHide = "Скрыть технические детали"
    override val nTechHost = "Архитектура хоста"
    override val nTechGuest = "Guest"
    override val nTechBackend = "Backend"
    override val nTechStun = "STUN"
    override val nTechDirect = "Direct path"
    override val nTechProvider = "Provider"
    override val nTechTransport = "Transport"
    override val nTechRegion = "Region"
    override val nTechRelayLat = "Relay latency"
    override val nTechSession = "Session ID"
    override val nTechServer = "Server ID"
    override val nTechPackets = "Packets"
    override val nTechBytes = "Bytes"
    override val nTechHeartbeat = "Last heartbeat"
    override val nInfraLabel = "Источник инфраструктуры (https)"
    override val nInfraSave = "СОХРАНИТЬ"
    override val nOffer = "КОПИРОВАТЬ ОФФЕР"
    override val nAnswerLabel = "Ответ сигналинга (JSON)"
    override val nAnswerImport = "ИМПОРТ ОТВЕТА"
    override val nLogTitle = "ТЕХНИЧЕСКИЙ ЛОГ"
    override val nLogShow = "Показать логи"
    override val nLogHide = "Скрыть"
    override val nLogEmpty = "Пока нет событий."
    override val nClaimTitle = "Подключить Playit"
    override val nClaimDesc = "Первая привязка требует подтверждения вашего аккаунта Playit (один раз)."
    override val nClaimStep1 = "1. Откройте ссылку в браузере"
    override val nClaimStep2 = "2. Подтвердите агент и ждите здесь"
    override val nClaimKeep = "Не закрывайте приложение: агент должен работать."
    override val nClaimWait = "Ожидание подтверждения…"
    override val nClaimCopy = "КОПИРОВАТЬ ССЫЛКУ"
    override val nChecking = "проверка…"
    override val nDegraded = "Ограничено"
    override val nReconnecting = "Переподключение…"
    override val nViaRelay = "Через relay"
    override val sTitle = "Настройки"
    override val sCfgFile = "server.cfg: %s"
    override val sSave = "Сохранить server.cfg"
    override val sGmInstalled = "Установлено гейммодов: %d"
    override val sGmHint = "Для импорта .amx или .so используйте вкладку Файлы."
    override val sRestart = "Перезапустить"
    override val gTitle = "Диагностика"
    override val gLibs = "Нужные библиотеки"
    override val gTestBackend = "Проверить backend"
    override val gCheckFiles = "Проверить файлы"
    override val gCopyFull = "КОПИРОВАТЬ ДИАГНОСТИКУ"
    override val gCopyBtn = "КОПИРОВАТЬ"
    override val gLibsTitle = "Нужные библиотеки (%s)"
    override val gLibsFail = "Не удалось загрузить список."
    override val mCopying = "Копирование файлов"
    override val mValidatingExe = "Проверка исполняемого"
    override val mLogSaved = "Лог сохранён: %s"
    override val mCfgSaved = "server.cfg сохранён."
    override val mSrvAdded = "Сервер %1\$s добавлен. %2\$s"
    override val mReauthOk = "Папка %s разрешена заново."
    override val mReauthFail = "Ошибка доступа: %s"
    override val mBadName = "Недопустимое имя."
    override val mSrvCreated = "Сервер %s создан."
    override val mImportFail = "Ошибка импорта: %s"
    override val mPickAmx = "Выберите .amx файл"
    override val mGmImported = "Гейммод %s.amx импортирован и включён."
    override val mPickSo = "Выберите Linux .so плагин"
    override val mPluginImported = "Плагин %s импортирован. Добавьте в server.cfg при необходимости."
    override val mFolderCopied = "Папка: %s (путь скопирован)"
    override val mDiagCopied = "Диагностика скопирована."
    override val mDiagFail = "Ошибка диагностики: %s"
    override val mSrcUpdated = "Источник обновлён. Запустите проверку."
    override val mLinkCopied = "Ссылка скопирована."
    override val mUnlinked = "Аккаунт Playit отвязан."
    override val mRelinked = "Старый аккаунт удалён. Привяжите новый."
    override val mFail = "Ошибка: %s"
    override val mNoExt = "Внешнее подключение недоступно."
    override val mAddrCopied = "Адрес скопирован: %s"
    override val mOfferCopied = "Оффер скопирован (сессия %s…)"
    override val mExportFail = "Ошибка экспорта: %s"
    override val mCandidates = "Получено кандидатов: %d"
    override val mBadAnswer = "Недопустимый ответ: %s"
    override val gInstalled = "Установлен"
    override val gMissing = "Отсутствует"
    override val gYes = "ДА"
    override val gNo = "НЕТ"
    override val gInvalid = "Недопустим"
    override val gOk = "OK"
    override val gMissingN = "Нет: %d"
    override val mInstallingSvr = "Установка samp03svr"
    override val mSvrKept = "существующий samp03svr сохранён."
    override val mSvrInstalled = "samp03svr установлен и проверен."
    override val mSvrInvalid = "Внимание: существующий samp03svr недопустим (%s), сохранён."
    override val mSvrInstallFail = "Не удалось установить samp03svr: %s"
    override val mImportFolderFail = "Ошибка импорта папки."
    override val mUnknownErr = "неизвестная ошибка"
    override val mSetupFail = "Ошибка подготовки окружения."
    override val mViewFailRead = "Не удалось прочитать файл"
    override val gDevArch = "Архитектура устройства"
    override val gRuntime = "Runtime"
    override val gGuestArch = "Архитектура guest"
    override val gBackend = "Backend"
    override val gBackendAvail = "Backend доступен"
    override val gBackendPath = "Путь backend"
    override val gPlugins = "Плагины"
    override val gGamemodes = "Гейммоды"
    override val gServerDir = "Каталог сервера"
    override val gProcess = "Процесс"
    override val gPid = "PID"
    override val mPathCopied = "Путь скопирован."
    override val instVerifying = "Проверка устройства"
    override val instX86 = "Установка x86-совместимости"
    override val instLibs = "Подготовка библиотек сервера"
    override val instValidating = "Проверка runtime"
    override val instReady = "Окружение готово"
}

fun stringsFor(lang: AppLang): Strings = when (lang) {
    AppLang.PT_BR -> PtStrings()
    AppLang.EN -> EnStrings()
    AppLang.ES -> EsStrings()
    AppLang.RU -> RuStrings()
}
