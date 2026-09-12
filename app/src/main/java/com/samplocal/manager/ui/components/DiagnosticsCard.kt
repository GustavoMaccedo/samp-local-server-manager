package com.samplocal.manager.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Troubleshoot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samplocal.manager.runtime.RuntimeManager
import com.samplocal.manager.ui.theme.Green
import com.samplocal.manager.ui.theme.Red
import com.samplocal.manager.ui.theme.Surface
import com.samplocal.manager.ui.theme.TextDim
import com.samplocal.manager.ui.theme.TextMain
import com.samplocal.manager.ui.theme.Yellow
import com.samplocal.manager.ui.strings.Strings
import com.samplocal.manager.ui.strings.statusText
import com.samplocal.manager.ui.viewmodel.MainViewModel
import com.samplocal.manager.util.ElfParser
import java.io.File

private enum class Tone { NEUTRAL, OK, BAD, WARN }

@Composable
fun DiagnosticsCard(vm: MainViewModel, T: Strings) {
    val ctx = LocalContext.current
    val selected by vm.selected.collectAsState()
    val servers by vm.servers.collectAsState()
    val busy by vm.diagBusy.collectAsState()

    val rt = remember(selected, busy) { vm.runtimeManager().getRuntimeStatus() }
    val current = servers.firstOrNull { it.id == selected }
    val dir = remember(selected) { vm.serverDir() }
    val bin = remember(selected) { File(dir, "samp03svr") }
    val elf = remember(selected, busy) { ElfParser.parse(bin) }
    var libs by remember(selected) { mutableStateOf(RuntimeManager.LibsStatus(0, emptyList())) }
    LaunchedEffect(selected) {
        libs = vm.runtimeManager().librariesStatus()
    }
    var pathDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var libsDialog by remember { mutableStateOf(false) }

    val runtimeValue: Pair<String, Tone> = when {
        rt.runtimeInstalled -> "${T.gInstalled} (v${rt.runtimeVersion})" to Tone.OK
        else -> T.gMissing to Tone.BAD
    }
    val backendValue: Pair<String, Tone> =
        if (rt.backendAvailable) T.gYes to Tone.OK else T.gNo to Tone.BAD
    val sampValue: Pair<String, Tone> =
        if (bin.isFile) T.gOk to Tone.OK else T.gMissing to Tone.BAD
    val elfValue: Pair<String, Tone> = when {
        !bin.isFile -> T.gMissing to Tone.BAD
        !elf.isElf -> T.gInvalid to Tone.BAD
        else -> "${T.gOk} (${if (elf.elfClass == 1) "ELF32" else "ELF64"})" to Tone.OK
    }
    val libsValue: Pair<String, Tone> = when {
        libs.checked == 0 && libs.missing.isNotEmpty() -> T.stError to Tone.BAD
        libs.missing.isEmpty() -> T.gOk to Tone.OK
        else -> T.gMissingN.format(libs.missing.size) to Tone.BAD
    }

    Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Troubleshoot, contentDescription = null,
                    tint = TextMain, modifier = Modifier.size(20.dp))
                Text(T.gTitle, color = TextMain, fontSize = 17.sp,
                    fontWeight = FontWeight.Bold)
            }
            DiagRow(T.gDevArch, rt.hostArch.toString(), Tone.NEUTRAL)
            DiagRow(T.gRuntime, runtimeValue.first, runtimeValue.second)
            DiagRow(T.gGuestArch, rt.guestArch.toString(), Tone.NEUTRAL)
            DiagRow(T.gBackend, rt.backend.toString(), Tone.NEUTRAL)
            DiagRow(T.gBackendAvail, backendValue.first, backendValue.second)
            PathRow(T.gBackendPath, rt.backendPath ?: "—") { pathDialog = T.gBackendPath to it }
            DiagRow("samp03svr", sampValue.first, sampValue.second)
            DiagRow("ELF", elfValue.first, elfValue.second)
            Row(
                Modifier.fillMaxWidth().clickable { libsDialog = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(T.gLibs, color = TextDim, fontSize = 13.sp)
                Text(libsValue.first, color = toneColor(libsValue.second), fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold)
            }
            DiagRow(T.gPlugins, vm.pluginCount().toString(), Tone.NEUTRAL)
            DiagRow(T.gGamemodes, vm.gamemodeCount().toString(), Tone.NEUTRAL)
            PathRow(T.gServerDir, dir.absolutePath) { pathDialog = T.gServerDir to it }
            DiagRow(T.gProcess, current?.status?.let { statusText(it, T) } ?: T.stStopped, Tone.NEUTRAL)
            DiagRow(T.gPid, current?.pid?.toString() ?: T.nUnavailLow, Tone.NEUTRAL)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { vm.testBackend() },
                    enabled = !busy,
                    modifier = Modifier.weight(1f)
                ) {
                    if (busy) SmallSpin() else Text(T.gTestBackend, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = { vm.validateRuntimeFiles() },
                    enabled = !busy,
                    modifier = Modifier.weight(1f)
                ) {
                    if (busy) SmallSpin() else Text(T.gCheckFiles, fontSize = 13.sp)
                }
            }
            OutlinedButton(
                onClick = { vm.copyDiagnostics(ctx) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(T.gCopyFull, fontSize = 13.sp)
            }
        }
    }

    pathDialog?.let { (label, full) ->
        AlertDialog(
            onDismissRequest = { pathDialog = null },
            title = { Text(label) },
            text = {
                Text(full, fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                    modifier = Modifier.verticalScroll(rememberScrollState()))
            },
            confirmButton = {
                TextButton(onClick = {
                    copyText(ctx, full)
                    vm.say(T.mPathCopied)
                    pathDialog = null
                }) { Text(T.gCopyBtn) }
            },
            dismissButton = { TextButton(onClick = { pathDialog = null }) { Text(T.close) } }
        )
    }
    if (libsDialog) {
        AlertDialog(
            onDismissRequest = { libsDialog = false },
            title = { Text(T.gLibsTitle.format(libs.checked)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (libs.all.isEmpty()) {
                        Text(T.gLibsFail, color = TextDim,
                            fontSize = 13.sp)
                    }
                    libs.all.forEach { name ->
                        val miss = name in libs.missing
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(name, color = TextDim, fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace)
                            Text(if (miss) "FALTA" else "OK",
                                color = if (miss) Red else Green, fontSize = 12.sp,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { libsDialog = false }) { Text(T.close) } }
        )
    }
}

@Composable
private fun toneColor(t: Tone) = when (t) {
    Tone.OK -> Green
    Tone.BAD -> Red
    Tone.WARN -> Yellow
    Tone.NEUTRAL -> TextMain
}

@Composable
private fun DiagRow(label: String, value: String, tone: Tone) {
    val tc = toneColor(tone)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextDim, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (tone == Tone.OK || tone == Tone.BAD) {
                Canvas(Modifier.size(8.dp)) { drawCircle(tc) }
            }
            Text(value, color = tc, fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun PathRow(label: String, fullPath: String, onTap: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().clickable { onTap(fullPath) }) {
        Text(label, color = TextDim, fontSize = 13.sp)
        Text(shortDisplayPath(fullPath), color = TextMain, fontSize = 12.sp,
            fontFamily = FontFamily.Monospace, maxLines = 1)
    }
}

internal fun shortDisplayPath(full: String): String {
    if (full.length <= 44) return full
    val parts = full.split("/").filter { it.isNotEmpty() }
    if (parts.size <= 3) return "…${full.takeLast(43)}"
    return "/${parts.first()}/…/${parts.takeLast(2).joinToString("/")}"
}

private fun copyText(ctx: Context, text: String) {
    try {
        val cm = ctx.getSystemService(ClipboardManager::class.java)
        cm?.setPrimaryClip(ClipData.newPlainText("path", text))
    } catch (_: Exception) { }
}

@Composable
private fun SmallSpin() {
    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
}
