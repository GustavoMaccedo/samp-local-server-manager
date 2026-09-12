package com.samplocal.manager.ui.screens

import android.app.Activity
import android.content.Intent
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samplocal.manager.core.FileManager
import com.samplocal.manager.core.ServerFolderImporter
import com.samplocal.manager.data.model.ServerStatus
import com.samplocal.manager.ui.theme.Divider
import com.samplocal.manager.ui.theme.Green
import com.samplocal.manager.ui.theme.Orange
import com.samplocal.manager.ui.theme.Red
import com.samplocal.manager.ui.theme.Surface
import com.samplocal.manager.ui.theme.Surface2
import com.samplocal.manager.ui.theme.TextDim
import com.samplocal.manager.ui.theme.TextMain
import com.samplocal.manager.ui.theme.Yellow
import com.samplocal.manager.ui.strings.Strings
import com.samplocal.manager.ui.viewmodel.MainViewModel
import java.io.File

@Composable
fun FilesScreen(vm: MainViewModel, onOpenSettings: () -> Unit, T: Strings) {
    val servers by vm.servers.collectAsState()
    val selected by vm.selected.collectAsState()
    val folderState by vm.folderImport.collectAsState()
    var pendingReauth by remember { mutableStateOf<String?>(null) }

    val treePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            res.data?.data?.let { uri ->
                val reauth = pendingReauth
                pendingReauth = null
                if (reauth != null) vm.reauthorizeServer(reauth, uri)
                else vm.onFolderPicked(uri)
            }
        } else {
            pendingReauth = null
        }
    }
    val soPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.importPlugin(uri)
    }
    fun launchTree() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
            putExtra("android.provider.extra.INITIAL_URI", MediaStore.Downloads.EXTERNAL_CONTENT_URI)
        }
        treePicker.launch(intent)
    }

    var segments by remember(selected) { mutableStateOf(listOf<String>()) }
    var nonce by remember { mutableStateOf(0) }
    val fileOps by vm.fileOps.collectAsState()
    val serverRoot = remember(selected, nonce) { vm.serverDir() }
    val currentDir = remember(selected, segments, nonce, fileOps) {
        segments.fold(serverRoot) { acc, s -> File(acc, s) }
    }
    fun refresh() { nonce++ }
    val currentDirRef = rememberUpdatedState(currentDir)
    val filesPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) vm.importFiles(uris, currentDirRef.value)
    }
    val children = remember(selected, segments, nonce, fileOps) {
        currentDir.listFiles()
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?: emptyList()
    }
    val dirCount = children.count { it.isDirectory }
    val fileCount = children.count { it.isFile }
    val needsReauth = remember(selected, nonce) { vm.serverNeedsReauth(selected) }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Dns, contentDescription = "SAMP Local",
                tint = Green,
                modifier = Modifier.size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("SAMP LOCAL", color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(T.fSubtitle, color = TextDim, fontSize = 12.sp)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = T.dOpenSettings, tint = TextDim)
            }
        }

        var menuOpen by remember { mutableStateOf(false) }
        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            modifier = Modifier.fillMaxWidth().clickable { menuOpen = true }
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Dns, contentDescription = null, tint = TextDim,
                    modifier = Modifier.size(30.dp)
                        .clip(RoundedCornerShape(10.dp)))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    val current = servers.firstOrNull { it.id == selected }
                    Text(current?.name ?: selected, color = TextMain, fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold)
                    StatusDotRow(current?.status ?: ServerStatus.STOPPED)
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = T.dSwitch,
                    tint = TextDim)
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    servers.forEach { s ->
                        DropdownMenuItem(
                            text = { Text("${s.name} :${s.port}") },
                            onClick = {
                                vm.select(s.id)
                                segments = emptyList()
                                menuOpen = false
                            }
                        )
                    }
                }
            }
        }

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val narrow = maxWidth < 360.dp
            if (narrow) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ImportServerButton(Modifier.fillMaxWidth(), T) { launchTree() }
                    PluginButton(Modifier.fillMaxWidth(), T) { soPicker.launch("*/*") }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ImportServerButton(Modifier.weight(1.2f), T) { launchTree() }
                    PluginButton(Modifier.weight(1f), T) { soPicker.launch("*/*") }
                }
            }
        }

        when (val st = folderState) {
            is MainViewModel.FolderImport.Validating ->
                StateCard(T.fChecking) { CircularProgressIndicator() }
            is MainViewModel.FolderImport.Copying ->
                StateCard("${st.step}… ${if (st.done > 0) T.fFilesUnit.format(st.done) else ""}".trim()) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            is MainViewModel.FolderImport.Invalid ->
                Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Error, contentDescription = null, tint = Red,
                                modifier = Modifier.size(20.dp))
                            Text(T.fInvalidDir, color = TextMain, fontSize = 15.sp,
                                fontWeight = FontWeight.Bold)
                        }
                        Text(st.reason, color = TextDim, fontSize = 13.sp)
                        Text(st.details, color = TextDim, fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace)
                        OutlinedButton(onClick = {
                            vm.cancelFolderImport()
                            launchTree()
                        }) { Text(T.fChooseOther) }
                    }
                }
            is MainViewModel.FolderImport.NeedName ->
                NameDialog(
                    initial = vm.suggestServerName(st.info),
                    info = st.info, T = T,
                    onConfirm = { vm.confirmFolderImport(it) },
                    onDismiss = { vm.cancelFolderImport() }
                )
            else -> Unit
        }
        if (folderState is MainViewModel.FolderImport.Done) {
            vm.consumeFolderDone()
            segments = emptyList()
            refresh()
        }

        Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Folder, contentDescription = null, tint = TextDim,
                        modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(T.fFilesTitle, color = TextMain, fontSize = 16.sp,
                            fontWeight = FontWeight.Bold)
                        Text(shortPath(currentDir.absolutePath), color = TextDim, fontSize = 11.sp,
                            maxLines = 1)
                    }
                    Text(T.fFolders.format(dirCount), color = TextDim, fontSize = 12.sp)
                    Text("  |  ", color = Divider, fontSize = 12.sp)
                    Text(T.fFiles.format(fileCount), color = TextDim, fontSize = 12.sp)
                }
                TextButton(onClick = { filesPicker.launch("*/*") }) {
                    Icon(Icons.Filled.UploadFile, contentDescription = null,
                        tint = TextDim, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(T.fImportFile, color = TextDim, fontSize = 12.sp)
                }
            }
        }

        if (needsReauth) {
            Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(T.fReauthTitle, color = Yellow, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold)
                    Text(T.fReauthDesc,
                        color = TextDim, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            pendingReauth = selected
                            launchTree()
                        }) { Text(T.fReauth) }
                        TextButton(onClick = { refresh() }) { Text(T.retry) }
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (segments.isNotEmpty()) segments = segments.dropLast(1) }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = T.fBackDesc, tint = TextDim)
            }
            Text(
                (listOf(T.fBreadcrumb) + segments).joinToString(" > "),
                color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { refresh() }) {
                Icon(Icons.Filled.Refresh, contentDescription = T.fRefreshDesc, tint = TextDim)
            }
        }

        if (children.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = TextDim,
                        modifier = Modifier.size(44.dp))
                    Text(T.fEmpty, color = TextMain, fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold)
                    Text(T.fEmptyDesc,
                        color = TextDim, fontSize = 12.sp)
                    Button(onClick = { launchTree() }) { Text(T.fImportServer) }
                }
            }
        } else {
            children.forEach { f ->
                FileRow(
                    file = f,
                    onOpenDir = { segments = segments + f.name },
                    onChanged = { refresh() },
                    vm = vm, T = T
                )
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun ImportServerButton(modifier: Modifier, T: Strings, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Green),
        modifier = modifier
    ) {
        Icon(Icons.Filled.CreateNewFolder, contentDescription = null,
            modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(8.dp))
        Text(T.fImportServer, fontSize = 13.sp)
    }
}

@Composable
private fun PluginButton(modifier: Modifier, T: Strings, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Icon(Icons.Filled.Power, contentDescription = null,
            modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(8.dp))
        Text(T.fPlugin, fontSize = 13.sp)
    }
}

@Composable
private fun StateCard(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
        Column(Modifier.padding(14.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun StatusDotRow(status: ServerStatus) {
    val color = when (status) {
        ServerStatus.RUNNING -> Green
        ServerStatus.STARTING, ServerStatus.INSTALLING -> Yellow
        ServerStatus.STOPPING -> Orange
        else -> Red
    }
    val label = when (status) {
        ServerStatus.RUNNING -> "ONLINE"
        ServerStatus.STARTING -> "STARTING"
        ServerStatus.STOPPING -> "STOPPING"
        ServerStatus.INSTALLING -> "INSTALLING"
        ServerStatus.CRASHED, ServerStatus.ERROR -> "ERROR"
        ServerStatus.STOPPED -> "OFFLINE"
    }
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.size(9.dp)) { drawCircle(color) }
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NameDialog(
    initial: String,
    info: ServerFolderImporter.FolderValidation,
    T: Strings,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(T.fAddServer) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(T.fVerifying, color = TextMain, fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold)
                CheckLine(T.fDirFound, true)
                CheckLine(
                    if (info.hasServerCfg) "server.cfg — " + T.fCfgFound else "server.cfg — " + T.fCfgMissing,
                    info.hasServerCfg
                )
                CheckLine(
                    if (info.gamemodeCount > 0) T.fGmCount.format(info.gamemodeCount) else T.fGmNone,
                    info.gamemodeCount > 0
                )
                CheckLine(
                    if (info.pluginCount > 0) T.fPlCount.format(info.pluginCount) else T.fPlNone,
                    info.pluginCount > 0
                )
                Text(T.fReady, color = TextDim, fontSize = 12.sp)
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text(T.dServerName) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(name) }) { Text(T.fAdd) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(T.cancel) } }
    )
}

@Composable
private fun CheckLine(text: String, ok: Boolean) {
    val dot = if (ok) Green else Yellow
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.size(8.dp)) { drawCircle(dot) }
        Text(text, color = TextDim, fontSize = 13.sp)
    }
}

@Composable
private fun FileRow(
    file: File,
    onOpenDir: () -> Unit,
    onChanged: () -> Unit,
    vm: MainViewModel,
    T: Strings
) {
    var menu by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<String?>(null) }
    var renaming by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(file.name) }
    var confirmDelete by remember { mutableStateOf(false) }
    val fm = remember { FileManager() }
    val isDir = file.isDirectory
    val protected = !isDir && file.name == "samp03svr"

    Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp)
                .clickable(enabled = isDir) { if (isDir) onOpenDir() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when {
                    isDir -> Icons.Filled.Folder
                    file.name == "samp03svr" -> Icons.Filled.Terminal
                    file.extension.lowercase() == "so" -> Icons.Filled.Power
                    else -> Icons.Filled.Description
                },
                contentDescription = if (isDir) T.fFolderDesc else T.fFileDesc,
                tint = if (isDir) Green else TextDim,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(file.name, color = TextMain, fontSize = 14.sp, maxLines = 1)
                if (!isDir) Text(formatSize(file.length()), color = TextDim, fontSize = 11.sp)
            }
            if (isDir) {
                Icon(Icons.Filled.ChevronRight, contentDescription = T.fOpenFolder, tint = TextDim)
            } else {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = T.fFileActions,
                        tint = TextDim)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (isPreviewable(file)) {
                        DropdownMenuItem(
                            text = { Text(T.fView) },
                            onClick = {
                                menu = false
                                preview = readPreview(file)
                                    ?: T.fViewFail
                            }
                        )
                    }
                    if (!protected) {
                        DropdownMenuItem(
                            text = { Text(T.fRename) },
                            onClick = { menu = false; newName = file.name; renaming = true }
                        )
                        DropdownMenuItem(
                            text = { Text(T.fDelete) },
                            onClick = { menu = false; confirmDelete = true }
                        )
                    }
                }
            }
        }
    }

    preview?.let { text ->
        AlertDialog(
            onDismissRequest = { preview = null },
            title = { Text(file.name) },
            text = {
                Text(text, fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                    modifier = Modifier.verticalScroll(rememberScrollState()))
            },
            confirmButton = { TextButton(onClick = { preview = null }) { Text(T.close) } }
        )
    }
    if (renaming) {
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text(T.fRename) },
            text = {
                OutlinedTextField(value = newName, onValueChange = { newName = it },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    renaming = false
                    if (fm.rename(file, newName)) {
                        vm.say(T.fRenamed.format(newName))
                        onChanged()
                    } else vm.say(T.fBadName)
                }) { Text(T.fOk) }
            },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text(T.cancel) } }
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(T.fDeleteTitle.format(file.name)) },
            text = { Text(T.fDeleteDesc) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    if (fm.delete(file)) {
                        vm.say(T.fDeleted.format(file.name))
                        onChanged()
                    } else vm.say(T.fDeleteFail)
                }) { Text(T.fDelete) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(T.cancel) } }
        )
    }
}

private val PREVIEW_EXT = setOf("cfg", "txt", "log", "json", "ini", "dat", "pwn", "md")

private fun isPreviewable(f: File): Boolean =
    f.isFile && f.extension.lowercase() in PREVIEW_EXT && f.length() <= 200 * 1024

private fun readPreview(f: File): String? {
    return try {
        f.readText().take(20000)
    } catch (_: Exception) { null }
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    return String.format("%.1f MB", kb / 1024.0)
}

private fun shortPath(abs: String): String =
    if (abs.length <= 64) abs else "…${abs.takeLast(63)}"
