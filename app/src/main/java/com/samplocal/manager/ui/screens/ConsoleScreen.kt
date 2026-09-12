package com.samplocal.manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samplocal.manager.ui.theme.Bg
import com.samplocal.manager.ui.theme.TextDim
import com.samplocal.manager.ui.strings.Strings
import com.samplocal.manager.ui.theme.TextMain
import com.samplocal.manager.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConsoleScreen(vm: MainViewModel, T: Strings) {
    val selected by vm.selected.collectAsState()
    val consoleMap by vm.consoleMap.collectAsState()
    val entries = consoleMap[selected] ?: emptyList()
    val fontSp by vm.consoleFontSp.collectAsState()
    val wrap by vm.consoleWrap.collectAsState()
    val showTime by vm.consoleTimestamps.collectAsState()
    val state = rememberLazyListState()
    val timeFmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) state.animateScrollToItem(entries.size - 1)
    }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.clearConsole() }) { Text(T.cClear) }
            OutlinedButton(onClick = { vm.saveLog() }) { Text(T.cSave) }
        }
        Text(T.cTitle.format(selected, entries.size), color = TextDim, fontSize = 12.sp)
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize().background(Bg).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (entries.isEmpty()) {
                item { Text(T.cEmpty, color = TextDim, fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
            } else {
                items(entries.size) { i ->
                    val e = entries[i]
                    val text = if (showTime) "[${timeFmt.format(Date(e.tsMs))}] ${e.text}" else e.text
                    Text(text, color = TextMain, fontFamily = FontFamily.Monospace,
                        fontSize = fontSp.sp, softWrap = wrap)
                }
            }
        }
    }
}
