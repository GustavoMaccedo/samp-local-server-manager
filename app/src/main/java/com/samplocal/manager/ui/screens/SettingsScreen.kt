package com.samplocal.manager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samplocal.manager.data.model.SampConfig
import com.samplocal.manager.ui.components.DiagnosticsCard
import com.samplocal.manager.ui.theme.Surface
import com.samplocal.manager.ui.theme.TextDim
import com.samplocal.manager.ui.theme.TextMain
import com.samplocal.manager.ui.strings.Strings
import com.samplocal.manager.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(vm: MainViewModel, T: Strings) {
    val selected by vm.selected.collectAsState()
    var cfg by remember(selected) { mutableStateOf(vm.readConfig()) }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(T.sTitle, color = TextMain, fontSize = 20.sp)
        Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(T.sCfgFile.format(selected), color = TextMain, fontSize = 15.sp)
                CfgField("hostname", cfg.hostname) { cfg = cfg.copy(hostname = it) }
                CfgField("gamemode0", cfg.gamemode0) { cfg = cfg.copy(gamemode0 = it) }
                CfgField("mapname", cfg.mapname) { cfg = cfg.copy(mapname = it) }
                CfgField("maxplayers", cfg.maxplayers.toString()) { cfg = cfg.copy(maxplayers = it.toIntOrNull() ?: cfg.maxplayers) }
                CfgField("port", cfg.port.toString()) { cfg = cfg.copy(port = it.toIntOrNull() ?: cfg.port) }
                CfgField("language", cfg.language) { cfg = cfg.copy(language = it) }
                CfgField("weburl", cfg.weburl) { cfg = cfg.copy(weburl = it) }
                CfgField("rcon_password", cfg.rconPassword) { cfg = cfg.copy(rconPassword = it) }
                Button(onClick = { vm.saveConfig(cfg) }, modifier = Modifier.fillMaxWidth()) { Text(T.sSave) }
                val errors = SampConfig.validate(cfg)
                if (errors.isNotEmpty()) Text(errors.joinToString("\n"), color = androidx.compose.ui.graphics.Color(0xFFEF4444), fontSize = 12.sp)
            }
        }
        DiagnosticsCard(vm, T)
        Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(T.sGmInstalled.format(vm.gamemodes().size), color = TextMain, fontSize = 14.sp)
                Text(T.sGmHint, color = TextDim, fontSize = 12.sp)
                vm.gamemodes().forEach { g ->
                    Text("• ${g.name}.amx (${g.sizeBytes / 1024} KB)", color = TextDim, fontSize = 12.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.restart() }) { Text(T.sRestart) }
                }
            }
        }
    }
}

@Composable
private fun CfgField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
