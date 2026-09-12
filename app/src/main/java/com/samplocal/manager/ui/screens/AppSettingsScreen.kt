package com.samplocal.manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Minimize
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.WrapText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samplocal.manager.core.AppLang
import com.samplocal.manager.core.AppPrefs
import com.samplocal.manager.core.ConsoleFont
import com.samplocal.manager.ui.components.ColorPickerDialog
import com.samplocal.manager.ui.strings.Strings
import com.samplocal.manager.ui.theme.Accent
import com.samplocal.manager.ui.theme.Bg
import com.samplocal.manager.ui.theme.Red
import com.samplocal.manager.ui.theme.Surface
import com.samplocal.manager.ui.theme.Surface2
import com.samplocal.manager.ui.theme.TextDim
import com.samplocal.manager.ui.theme.TextMain
import com.samplocal.manager.ui.viewmodel.MainViewModel

private fun accentLabel(argb: Int?, def: String): String {
    if (argb == null) return def
    return "#%02X%02X%02X".format((argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF)
}

private fun retentionLabel(days: Int, T: Strings): String = when (days) {
    1 -> T.ret1d
    30 -> T.ret30d
    0 -> T.retNever
    else -> T.ret7d
}

private fun fontLabel(f: ConsoleFont, T: Strings): String = when (f) {
    ConsoleFont.SMALL -> T.fontSmall
    ConsoleFont.MEDIUM -> T.fontMedium
    ConsoleFont.LARGE -> T.fontLarge
}

@Composable
fun AppSettingsScreen(main: MainViewModel, T: Strings, onBack: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val appVersion = remember {
        try {
            val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            pi.versionName ?: "?"
        } catch (_: Exception) { "?" }
    }
    val prefs: AppPrefs = main.appPrefs
    val lang by prefs.language.collectAsState()
    val dark by prefs.darkMode.collectAsState()
    val accent by prefs.accentArgb.collectAsState()
    val autoStart by prefs.autoStart.collectAsState()
    val keepBg by prefs.keepBackground.collectAsState()
    val notif by prefs.notifications.collectAsState()
    val retention by prefs.logRetentionDays.collectAsState()
    val font by prefs.consoleFont.collectAsState()
    val wrap by prefs.consoleWrap.collectAsState()
    val showTime by prefs.consoleTimestamps.collectAsState()

    var langDlg by remember { mutableStateOf(false) }
    var retDlg by remember { mutableStateOf(false) }
    var fontDlg by remember { mutableStateOf(false) }
    var colorDlg by remember { mutableStateOf(false) }
    var creditsDlg by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = T.close, tint = TextMain)
            }
            Column {
                Text(T.settingsTitle, color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text(T.settingsSubtitle, color = TextDim, fontSize = 13.sp)
            }
        }

        SectionCard(icon = Icons.Outlined.Settings, title = T.secAppearance) {
            SettingRow(
                icon = Icons.Outlined.Language, title = T.lang, subtitle = T.langDesc,
                trailing = {
                    ValueChevron(label = langLabel(lang)) { langDlg = true }
                }
            )
            RowDivider()
            SettingRow(
                icon = Icons.Outlined.Palette, title = T.mode, subtitle = T.modeDesc,
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModeChip(selected = dark, label = T.modeDark) { prefs.setDarkMode(true) }
                        ModeChip(selected = !dark, label = T.modeLight) { prefs.setDarkMode(false) }
                    }
                }
            )
            RowDivider()
            SettingRow(
                icon = Icons.Outlined.Brush, title = T.accent, subtitle = T.accentDesc,
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val acc = accent
                        if (acc != null) {
                            Box(
                                Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(acc))
                                    .border(1.dp, TextDim, CircleShape)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        ValueChevron(label = accentLabel(accent, T.accentNameDefault)) { colorDlg = true }
                    }
                }
            )
        }

        SectionCard(icon = Icons.Outlined.Dns, title = T.secServer) {
            SettingRow(
                icon = Icons.Filled.PowerSettingsNew,
                title = T.autoStart, subtitle = T.autoStartDesc,
                trailing = { PrefSwitch(checked = autoStart) { prefs.setAutoStart(it) } }
            )
            RowDivider()
            SettingRow(
                icon = Icons.Outlined.Minimize,
                title = T.keepBg, subtitle = T.keepBgDesc,
                trailing = { PrefSwitch(checked = keepBg) { prefs.setKeepBackground(it) } }
            )
            RowDivider()
            SettingRow(
                icon = Icons.Outlined.Notifications,
                title = T.notif, subtitle = T.notifDesc,
                trailing = { PrefSwitch(checked = notif) { prefs.setNotifications(it) } }
            )
            RowDivider()
            SettingRow(
                icon = Icons.Outlined.DeleteOutline,
                title = T.retention, subtitle = T.retentionDesc,
                trailing = {
                    ValueChevron(label = retentionLabel(retention, T)) { retDlg = true }
                }
            )
        }

        SectionCard(icon = Icons.Outlined.Terminal, title = T.secConsole) {
            SettingRow(
                icon = Icons.Outlined.FormatSize,
                title = T.consoleFont, subtitle = T.consoleFontDesc,
                trailing = {
                    ValueChevron(label = fontLabel(font, T)) { fontDlg = true }
                }
            )
            RowDivider()
            SettingRow(
                icon = Icons.Outlined.WrapText,
                title = T.consoleWrap, subtitle = T.consoleWrapDesc,
                trailing = { PrefSwitch(checked = wrap) { prefs.setConsoleWrap(it) } }
            )
            RowDivider()
            SettingRow(
                icon = Icons.Outlined.Schedule,
                title = T.consoleTime, subtitle = T.consoleTimeDesc,
                trailing = { PrefSwitch(checked = showTime) { prefs.setConsoleTimestamps(it) } }
            )
        }

        SectionCard(icon = Icons.Outlined.Info, title = T.secAbout) {
            SettingRow(
                icon = Icons.Outlined.Info,
                title = T.aboutVersion, subtitle = T.developedBy,
                trailing = {
                    Text("v$appVersion", color = TextDim, fontSize = 13.sp)
                }
            )
            RowDivider()
            SettingRow(
                icon = Icons.Outlined.Group,
                title = T.aboutCredits, subtitle = T.creditsRole,
                trailing = {
                    IconButton(onClick = { creditsDlg = true }) {
                        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = TextDim)
                    }
                }
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = Surface2),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = Red,
                        modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(T.thanksTitle, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(T.thanksDesc, color = TextDim, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        Spacer(Modifier.height(8.dp))
    }

    if (langDlg) {
        OptionDialog(
            title = T.lang,
            options = listOf(
                "Português (BR)" to AppLang.PT_BR,
                "English" to AppLang.EN,
                "Español" to AppLang.ES,
                "Русский" to AppLang.RU
            ),
            selected = lang,
            onSelect = { prefs.setLanguage(it); langDlg = false },
            onDismiss = { langDlg = false }
        )
    }
    if (retDlg) {
        val opts = listOf(
            T.retNever to 0, T.ret1d to 1, T.ret7d to 7, T.ret30d to 30
        )
        OptionDialog(
            title = T.retention,
            options = opts,
            selected = retention,
            onSelect = {
                prefs.setLogRetentionDays(it)
                main.applyLogRetention()
                retDlg = false
            },
            onDismiss = { retDlg = false }
        )
    }
    if (fontDlg) {
        OptionDialog(
            title = T.consoleFont,
            options = listOf(
                T.fontSmall to ConsoleFont.SMALL,
                T.fontMedium to ConsoleFont.MEDIUM,
                T.fontLarge to ConsoleFont.LARGE
            ),
            selected = font,
            onSelect = { prefs.setConsoleFont(it); fontDlg = false },
            onDismiss = { fontDlg = false }
        )
    }
    if (colorDlg) {
        ColorPickerDialog(
            T = T,
            initial = accent?.let { Color(it) },
            onConfirm = { c ->
                prefs.setAccent(c?.toArgb())
                colorDlg = false
            },
            onDismiss = { colorDlg = false }
        )
    }
    if (creditsDlg) {
        AlertDialog(
            onDismissRequest = { creditsDlg = false },
            containerColor = Surface,
            title = { Text(T.aboutCredits, color = TextMain, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(T.creditsRole, color = TextMain, fontSize = 14.sp)
                    Text(T.creditsThanks, color = TextDim, fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { creditsDlg = false }) { Text(T.close) }
            }
        )
    }
}

private fun langLabel(l: AppLang): String = when (l) {
    AppLang.PT_BR -> "Português (BR)"
    AppLang.EN -> "English"
    AppLang.ES -> "Español"
    AppLang.RU -> "Русский"
}

@Composable
private fun SectionCard(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = TextMain, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(title, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp)
            }
            content()
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextDim, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextMain, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextDim, fontSize = 12.sp)
        }
        trailing()
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        color = TextMain.copy(alpha = 0.08f),
        thickness = 1.dp,
        modifier = Modifier.padding(start = 38.dp)
    )
}

@Composable
private fun ValueChevron(label: String, onClick: () -> Unit) {
    Row(
        Modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextDim, fontSize = 13.sp)
        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null,
            tint = TextDim, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ModeChip(selected: Boolean, label: String, onClick: () -> Unit) {
    if (selected) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Surface2, contentColor = TextMain
            ),
            shape = RoundedCornerShape(10.dp)
        ) { Text(label, fontSize = 13.sp) }
    } else {
        OutlinedButton(onClick = onClick, shape = RoundedCornerShape(10.dp)) {
            Text(label, fontSize = 13.sp, color = TextDim)
        }
    }
}

@Composable
private fun PrefSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedTrackColor = Accent,
            checkedThumbColor = Color.White,
            uncheckedTrackColor = Surface2,
            uncheckedThumbColor = TextDim,
            uncheckedBorderColor = TextDim
        )
    )
}

@Composable
private fun <T> OptionDialog(
    title: String,
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        title = { Text(title, color = TextMain, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                options.forEach { (label, value) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelect(value) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = value == selected,
                            onClick = { onSelect(value) },
                            colors = RadioButtonDefaults.colors(selectedColor = Accent)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label, color = TextMain, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}
