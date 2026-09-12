package com.samplocal.manager.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samplocal.manager.SampApp
import com.samplocal.manager.core.ServerFolderImporter
import com.samplocal.manager.core.ConsoleFont
import com.samplocal.manager.runtime.RootAssetSource
import com.samplocal.manager.data.model.SampConfig
import com.samplocal.manager.data.model.SampServerInfo
import com.samplocal.manager.data.model.ServerStatus
import com.samplocal.manager.runtime.RuntimeInstaller
import com.samplocal.manager.runtime.RuntimeManager
import com.samplocal.manager.samp.ConfigManager
import com.samplocal.manager.samp.GamemodeManager
import com.samplocal.manager.samp.PluginManager
import com.samplocal.manager.samp.SampBinaryProvider
import com.samplocal.manager.ui.strings.Strings
import com.samplocal.manager.ui.strings.stringsFor
import com.samplocal.manager.util.WizardFlag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val sampApp get() = getApplication<SampApp>()
    val servers get() = sampApp.serverManager.servers
    val consoleMap get() = sampApp.serverManager.console

    val appPrefs get() = sampApp.appPrefs
    private val _consoleFontSp = MutableStateFlow(ConsoleFont.MEDIUM.sp)
    val consoleFontSp: StateFlow<Float> = _consoleFontSp.asStateFlow()
    val consoleWrap: StateFlow<Boolean> get() = sampApp.appPrefs.consoleWrap
    val consoleTimestamps: StateFlow<Boolean> get() = sampApp.appPrefs.consoleTimestamps

    private val _selected = MutableStateFlow("Default")
    val selected: StateFlow<String> = _selected.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _wizardDone = MutableStateFlow(false)
    val wizardDone: StateFlow<Boolean> = _wizardDone.asStateFlow()

    sealed interface FolderImport {
        data object Idle : FolderImport
        data object Validating : FolderImport
        data class NeedName(val uri: Uri, val info: ServerFolderImporter.FolderValidation) : FolderImport
        data class Copying(val done: Int, val step: String = "") : FolderImport
        data class Invalid(val reason: String, val details: String) : FolderImport
        data class Done(val serverId: String) : FolderImport
    }

    private val _folderImport = MutableStateFlow<FolderImport>(FolderImport.Idle)
    val folderImport: StateFlow<FolderImport> = _folderImport.asStateFlow()

    private val _fileOps = MutableStateFlow(0)
    val fileOps: StateFlow<Int> = _fileOps.asStateFlow()

    private fun folderImporter() = ServerFolderImporter(getApplication())

    sealed interface SetupState {

        data object Idle : SetupState
        data class Preparing(val step: String, val done: Int, val total: Int) : SetupState
        data object Ready : SetupState
        data class Failed(val step: String, val done: Int, val total: Int, val error: String) : SetupState
    }

    private val _setup = MutableStateFlow<SetupState>(SetupState.Idle)
    val setup: StateFlow<SetupState> = _setup.asStateFlow()

    private val configManager = ConfigManager()
    private val gamemodeManager = GamemodeManager()
    private val pluginManager = PluginManager()

    init {

        _wizardDone.value = prefsDone()
        viewModelScope.launch {

            launch {
                sampApp.appPrefs.consoleFont.collect { _consoleFontSp.value = it.sp }
            }

            launch {
                sampApp.runtimeManager.installerProgress.collect { step ->
                    val cur = _setup.value
                    if (cur is SetupState.Preparing && step != null) {
                        _setup.value = SetupState.Preparing(RuntimeInstaller.installStepKey(step.label), step.done, step.total)
                    }
                }
            }

            if (_wizardDone.value) {
                runSilentCheck()
            } else {
                _setup.value = SetupState.Idle
            }
        }
    }

    private suspend fun runSilentCheck() = coroutineScope {
        val progressJob = launch {
            sampApp.runtimeManager.installerProgress.collect { step ->
                if (step != null) {
                    _setup.value = SetupState.Preparing(RuntimeInstaller.installStepKey(step.label), step.done, step.total)
                }
            }
        }
        try {
            val ensured = sampApp.runtimeManager.ensureInstalled()
            if (ensured.isFailure) {
                val last = lastProgress()
                _setup.value = SetupState.Failed(
                    last.step, last.done, last.total,
                    ensured.exceptionOrNull()?.message ?: uiStrings().mSetupFail
                )
            } else {
                try {
                    sampApp.serverManager.refresh()
                    if (sampApp.serverManager.servers.value.isEmpty()) {
                        sampApp.serverManager.install("Default")
                    }
                } catch (_: Exception) { }
                _setup.value = SetupState.Ready
                applyLogRetention()
                maybeAutoStart()
            }
        } finally {
            progressJob.cancel()
        }
    }

    private fun lastProgress(): SetupState.Preparing =
        (_setup.value as? SetupState.Preparing) ?: SetupState.Preparing("verifying", 0, 0)

    private suspend fun runInstallFlow() {
        _setup.value = SetupState.Preparing("verifying", 0, 0)
        val ensured = sampApp.runtimeManager.ensureInstalled()
        if (ensured.isFailure) {
            val last = lastProgress()
            _setup.value = SetupState.Failed(
                last.step, last.done, last.total,
                ensured.exceptionOrNull()?.message ?: uiStrings().mSetupFail
            )
            return
        }
        if (!_wizardDone.value) completeWizard()
        try {
            sampApp.serverManager.refresh()
            if (sampApp.serverManager.servers.value.isEmpty()) {

                sampApp.serverManager.install("Default")
            }
        } catch (_: Exception) { }
        _setup.value = SetupState.Ready
        applyLogRetention()
        maybeAutoStart()
    }

    private fun maybeAutoStart() {
        if (!sampApp.appPrefs.autoStart.value) return
        val cur = currentServer() ?: return
        if (cur.status.isActive) return
        viewModelScope.launch {
            val r = sampApp.serverManager.start(cur.id)
            _message.value = r.getOrNull() ?: r.exceptionOrNull()?.message
        }
    }

    fun applyLogRetention(): Int {
        val days = sampApp.appPrefs.logRetentionDays.value
        if (days <= 0) return 0
        var n = 0
        try {
            sampApp.serverManager.servers.value.forEach { s ->
                try { n += sampApp.serverManager.logManager(s.id).pruneOlderThan(days) } catch (_: Exception) { }
            }
        } catch (_: Exception) { }
        return n
    }

    fun startInstall() {
        if (_setup.value is SetupState.Preparing) return
        viewModelScope.launch { runInstallFlow() }
    }

    fun retrySetup() {
        if (_setup.value is SetupState.Preparing) return
        viewModelScope.launch { runInstallFlow() }
    }

    fun select(id: String) { _selected.value = id }

    fun currentServer(): SampServerInfo? =
        servers.value.firstOrNull { it.id == _selected.value } ?: servers.value.firstOrNull()

    fun consumeMessage() { _message.value = null }

    fun uiStrings(): Strings = stringsFor(sampApp.appPrefs.language.value)
    fun say(msg: String) { _message.value = msg }

    fun start() {
        val id = _selected.value
        viewModelScope.launch {
            val r = sampApp.serverManager.start(id)
            _message.value = r.getOrNull() ?: r.exceptionOrNull()?.message
        }
    }

    fun stop() {
        val id = _selected.value
        viewModelScope.launch {
            val r = sampApp.serverManager.stop(id)
            _message.value = r.getOrNull() ?: r.exceptionOrNull()?.message
        }
    }

    fun restart() {
        val id = _selected.value
        viewModelScope.launch {
            val r = sampApp.serverManager.restart(id)
            _message.value = r.getOrNull() ?: r.exceptionOrNull()?.message
        }
    }

    fun clearConsole() = sampApp.serverManager.clearConsole(_selected.value)

    fun saveLog(): String {
        return try {
            val f = sampApp.serverManager.logManager(_selected.value).saveLogs()
            _message.value = uiStrings().mLogSaved.format(f.name)
            f.absolutePath
        } catch (e: Exception) {
            _message.value = e.message
            ""
        }
    }

    fun readConfig(): SampConfig {
        val dir = sampApp.runtimeManager.serverDir(_selected.value)
        return configManager.readConfig(dir)
    }

    fun saveConfig(cfg: SampConfig) {
        viewModelScope.launch {
            val dir = sampApp.runtimeManager.serverDir(_selected.value)
            configManager.writeConfig(dir, cfg)
            sampApp.serverManager.refresh()
            _message.value = uiStrings().mCfgSaved
        }
    }

    fun serverDir(): File = sampApp.runtimeManager.serverDir(_selected.value)
    fun runtimeManager(): RuntimeManager = sampApp.runtimeManager
    fun gamemodes() = gamemodeManager.listGamemodes(serverDir())
    fun plugins(cfgText: String) = pluginManager.detectPlugins(serverDir(), cfgText)

    fun serverNeedsReauth(id: String): Boolean =
        try { folderImporter().needsReauth(id) } catch (_: Exception) { false }

    fun serverSourceLabel(id: String): String? =
        try { folderImporter().sourceUri(id)?.toString() } catch (_: Exception) { null }

    fun onFolderPicked(uri: Uri) {
        _folderImport.value = FolderImport.Validating
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val importer = folderImporter()
                importer.takePersistable(uri)
                val snapshot = importer.snapshotTree(uri)
                val cfgText = importer.readServerCfg(uri)
                val info = ServerFolderImporter.validate(snapshot, cfgText)
                if (!info.valid) {
                    val details = buildString {
                        appendLine("Pasta: ${snapshot.displayName}")
                        appendLine("${info.dirCount} pastas, ${info.fileCount} arquivos")
                        info.warnings.forEach { appendLine("• $it") }
                    }
                    _folderImport.value = FolderImport.Invalid(
                        "A pasta selecionada não parece conter um servidor SAMP válido.",
                        details
                    )
                } else {
                    _folderImport.value = FolderImport.NeedName(uri, info)
                }
            } catch (e: Exception) {
                _folderImport.value = FolderImport.Invalid(
                    "Não foi possível ler a pasta.",
                    e.message ?: "erro desconhecido"
                )
            }
        }
    }

    fun suggestServerName(info: ServerFolderImporter.FolderValidation): String {
        val base = info.hostname?.takeIf { it.isNotBlank() } ?: info.displayName
        return ServerFolderImporter.cleanServerId(base)
    }

    fun confirmFolderImport(name: String) {
        val cur = _folderImport.value as? FolderImport.NeedName ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var id = ServerFolderImporter.cleanServerId(name)
                val existing = sampApp.serverManager.servers.value.map { it.id }.toSet()
                var n = 2
                while (id in existing) id = "${ServerFolderImporter.cleanServerId(name)}_$n".take(32).also { n++ }
                _folderImport.value = FolderImport.Copying(0)
                val importer = folderImporter()
                val dest = sampApp.runtimeManager.serverDir(id)
                importer.importTree(cur.uri, dest) { done, _ ->
                    _folderImport.value = FolderImport.Copying(done, uiStrings().mCopying)
                }

                sampApp.serverManager.install(id)

                _folderImport.value = FolderImport.Copying(0, uiStrings().mInstallingSvr)
                val binaryMsg = when (
                    val br = SampBinaryProvider(RootAssetSource(getApplication())).ensurePresent(dest)
                ) {
                    is SampBinaryProvider.EnsureResult.AlreadyValid ->
                        uiStrings().mSvrKept
                    is SampBinaryProvider.EnsureResult.Installed ->
                        uiStrings().mSvrInstalled
                    is SampBinaryProvider.EnsureResult.PresentInvalid ->
                        uiStrings().mSvrInvalid.format(br.reason)
                    is SampBinaryProvider.EnsureResult.Failed ->
                        throw IllegalStateException(uiStrings().mSvrInstallFail.format(br.reason))
                }
                _folderImport.value = FolderImport.Copying(0, uiStrings().mValidatingExe)
                importer.setSourceUri(id, cur.uri)
                _selected.value = id
                _folderImport.value = FolderImport.Done(id)
                _message.value = uiStrings().mSrvAdded.format(id, binaryMsg)
            } catch (e: Exception) {
                _folderImport.value = FolderImport.Invalid(
                    uiStrings().mImportFolderFail, e.message ?: uiStrings().mUnknownErr
                )
            }
        }
    }

    fun cancelFolderImport() {
        _folderImport.value = FolderImport.Idle
    }

    fun consumeFolderDone() {
        if (_folderImport.value is FolderImport.Done) _folderImport.value = FolderImport.Idle
    }

    fun reauthorizeServer(id: String, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val importer = folderImporter()
                importer.takePersistable(uri)
                importer.setSourceUri(id, uri)
                _message.value = uiStrings().mReauthOk.format(id)
            } catch (e: Exception) {
                _message.value = uiStrings().mReauthFail.format(e.message)
            }
        }
    }

    fun createServer(id: String) {
        viewModelScope.launch {
            val clean = id.trim().replace(Regex("[^A-Za-z0-9_\\-]"), "_").take(32)
            if (clean.isEmpty()) { _message.value = uiStrings().mBadName; return@launch }
            sampApp.serverManager.install(clean)
            _selected.value = clean
            _message.value = uiStrings().mSrvCreated.format(clean)
        }
    }

    fun importServerZip(uri: Uri) {
        val id = _selected.value
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                val tmp = File(app.cacheDir, "import_server.zip")
                app.contentResolver.openInputStream(uri)?.use { ins ->
                    tmp.outputStream().use { ins.copyTo(it) }
                } ?: throw IllegalStateException(uiStrings().mViewFailRead)
                val r = sampApp.serverManager.install(id, tmp)
                _message.value = r.getOrNull()
                    ?: uiStrings().mImportFail.format(r.exceptionOrNull()?.message)
            } catch (e: Exception) {
                _message.value = uiStrings().mImportFail.format(e.message)
            }
        }
    }

    fun importGamemode(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                val name = displayName(app, uri) ?: uri.lastPathSegment ?: "gamemode.amx"
                if (!name.lowercase().endsWith(".amx")) {
                    _message.value = uiStrings().mPickAmx
                    return@launch
                }
                val tmp = File(app.cacheDir, "import_gm.amx")
                app.contentResolver.openInputStream(uri)?.use { ins ->
                    tmp.outputStream().use { ins.copyTo(it) }
                } ?: throw IllegalStateException(uiStrings().mViewFailRead)
                val dir = sampApp.runtimeManager.serverDir(_selected.value)
                val gm = gamemodeManager.installGamemode(dir, tmp, null)
                configManager.updateConfig(dir) { it.copy(gamemode0 = "${gm.name} 1") }
                sampApp.serverManager.refresh()
                _message.value = uiStrings().mGmImported.format(gm.name)
            } catch (e: Exception) {
                _message.value = uiStrings().mImportFail.format(e.message)
            }
        }
    }

    fun importPlugin(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                val name = displayName(app, uri) ?: uri.lastPathSegment ?: "plugin.so"
                if (!name.lowercase().endsWith(".so")) {
                    _message.value = uiStrings().mPickSo
                    return@launch
                }
                val tmp = File(app.cacheDir, "import_plugin.so")

                val named = File(app.cacheDir, File(name).name)
                app.contentResolver.openInputStream(uri)?.use { ins ->
                    tmp.outputStream().use { ins.copyTo(it) }
                } ?: throw IllegalStateException(uiStrings().mViewFailRead)
                tmp.copyTo(named, overwrite = true)
                val dir = sampApp.runtimeManager.serverDir(_selected.value)
                val info = pluginManager.installPlugin(dir, named)
                sampApp.serverManager.refresh()
                _message.value = uiStrings().mPluginImported.format(info.file)
            } catch (e: Exception) {
                _message.value = uiStrings().mImportFail.format(e.message)
            }
        }
    }

    fun importFiles(uris: List<Uri>, dest: File) {
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                val fm = com.samplocal.manager.core.FileManager()
                dest.mkdirs()
                var ok = 0
                for (uri in uris) {
                    try {
                        val name = displayName(app, uri) ?: "arquivo"
                        val out = fm.resolveUniqueName(dest, name)
                        app.contentResolver.openInputStream(uri)?.use { ins ->
                            out.outputStream().use { ins.copyTo(it) }
                        } ?: continue
                        ok++
                    } catch (_: Exception) { }
                }
                _message.value = if (ok == uris.size) "$ok arquivo(s) importado(s)."
                else "$ok de ${uris.size} arquivo(s) importado(s)."
                _fileOps.value = _fileOps.value + 1
            } catch (e: Exception) {
                _message.value = uiStrings().mImportFail.format(e.message)
            }
        }
    }
    private fun displayName(app: Application, uri: Uri): String? {
        return try {
            app.contentResolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) c.getString(idx) else null
                } else null
            } ?: uri.lastPathSegment?.substringAfterLast('/')
        } catch (_: Exception) {
            uri.lastPathSegment?.substringAfterLast('/')
        }
    }

    fun openServerFolder(ctx: Context) {
        val dir = serverDir()
        try {
            val cm = ctx.getSystemService(ClipboardManager::class.java)
            cm?.setPrimaryClip(ClipData.newPlainText("pasta do servidor", dir.absolutePath))
        } catch (_: Exception) { }
        try {
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", dir)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ctx.startActivity(Intent.createChooser(intent, "Abrir pasta do servidor"))
        } catch (_: Exception) {
            _message.value = uiStrings().mFolderCopied.format(dir.absolutePath)
        }
    }

    fun completeWizard() {
        _wizardDone.value = true

        WizardFlag.markOnboarded(getApplication())
    }

    private fun prefsDone(): Boolean =
        !WizardFlag.shouldShowWizard(getApplication())

    fun isRunning(): Boolean {
        val s = currentServer()?.status
        return s == ServerStatus.RUNNING || s == ServerStatus.STARTING
    }

    fun testBackend() {
        viewModelScope.launch(Dispatchers.IO) {
            _diagBusy.value = true
            try {
                val r = sampApp.runtimeManager.smokeTest()
                _message.value = r.getOrNull()?.let { "Backend OK: $it" }
                    ?: "Backend FALHOU: ${r.exceptionOrNull()?.message}"
            } finally {
                _diagBusy.value = false
            }
        }
    }

    fun validateRuntimeFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            _diagBusy.value = true
            try {
                val r = sampApp.runtimeManager.fullValidate()
                _message.value = r.getOrNull() ?: "Integridade FALHOU: ${r.exceptionOrNull()?.message}"
            } finally {
                _diagBusy.value = false
            }
        }
    }

    private val _diagBusy = MutableStateFlow(false)
    val diagBusy: StateFlow<Boolean> = _diagBusy.asStateFlow()

    fun pluginCount(): Int {
        return try {
            File(serverDir(), "plugins").listFiles { f -> f.extension.lowercase() == "so" }?.size ?: 0
        } catch (_: Exception) { 0 }
    }

    fun gamemodeCount(): Int {
        return try { gamemodes().size } catch (_: Exception) { 0 }
    }

    suspend fun buildFullReport(): String = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val rt = sampApp.runtimeManager.getRuntimeStatus()
        val current = currentServer()
        val dir = serverDir()
        val bin = File(dir, "samp03svr")
        val libs = sampApp.runtimeManager.librariesStatus()
        buildString {
            appendLine("SAMP LOCAL DIAGNOSTIC")
            appendLine("Device architecture: ${rt.hostArch}")
            appendLine("Runtime: ${if (rt.runtimeInstalled) "Installed (v${rt.runtimeVersion})" else "Missing"}")
            appendLine("Guest architecture: ${rt.guestArch}")
            appendLine("Backend: ${rt.backend}")
            appendLine("Backend available: ${if (rt.backendAvailable) "YES" else "NO"}")
            appendLine("Backend path: ${rt.backendPath ?: "-"}")
            appendLine("samp03svr: ${if (bin.isFile) "OK (${bin.length() / 1024} KB)" else "Missing"}")
            appendLine("ELF: ${com.samplocal.manager.util.ElfParser.parse(bin).let {
                if (!it.isElf) "Invalid" else "Valid (${if (it.elfClass == 1) "ELF32" else "ELF64"})"
            }}")
            appendLine("Required libraries: ${if (libs.missing.isEmpty()) "OK (${libs.checked})" else "${libs.missing.size} missing: ${libs.missing.joinToString(", ")}"}")
            appendLine("Plugins: ${pluginCount()}")
            appendLine("Gamemodes: ${gamemodeCount()}")
            appendLine("Server directory: ${dir.absolutePath}")
            appendLine("Process: ${current?.status ?: "STOPPED"}")
            appendLine("PID: ${current?.pid?.toString() ?: "unavailable"}")
            appendLine("backend")
            appendLine(sampApp.runtimeManager.diagnose().trim())
        }
    }

    fun copyDiagnostics(ctx: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val report = buildFullReport()
                val cm = ctx.getSystemService(ClipboardManager::class.java)
                cm?.setPrimaryClip(ClipData.newPlainText("diagnostico", report))
                _message.value = uiStrings().mDiagCopied
            } catch (e: Exception) {
                _message.value = uiStrings().mDiagFail.format(e.message)
            }
        }
    }
}
