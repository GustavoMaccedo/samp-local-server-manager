package com.samplocal.manager.core

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppLang(val tag: String) {
    PT_BR("pt-BR"), EN("en"), RU("ru"), ES("es");
    companion object {
        fun of(tag: String?): AppLang = values().firstOrNull { it.tag == tag } ?: PT_BR
    }
}

enum class ConsoleFont(val sp: Float) {
    SMALL(11f), MEDIUM(13f), LARGE(16f);
    companion object {
        fun of(name: String?): ConsoleFont =
            values().firstOrNull { it.name == name } ?: MEDIUM
    }
}

class AppPrefs(private val prefs: SharedPreferences) {

    private fun boolFlow(key: String, def: Boolean): MutableStateFlow<Boolean> =
        MutableStateFlow(prefs.getBoolean(key, def))

    private fun stringFlow(key: String, def: String): MutableStateFlow<String> =
        MutableStateFlow(prefs.getString(key, def) ?: def)

    private fun intFlow(key: String, def: Int): MutableStateFlow<Int> =
        MutableStateFlow(prefs.getInt(key, def))

    val language = MutableStateFlow(AppLang.of(prefs.getString(Keys.LANG, null)))
    val darkMode = boolFlow(Keys.DARK, true)

    val accentArgb: MutableStateFlow<Int?> = MutableStateFlow(
        if (prefs.contains(Keys.ACCENT)) prefs.getInt(Keys.ACCENT, 0) else null
    )
    val autoStart = boolFlow(Keys.AUTO_START, false)
    val keepBackground = boolFlow(Keys.KEEP_BG, true)
    val notifications = boolFlow(Keys.NOTIF, true)

    val logRetentionDays = intFlow(Keys.RETENTION, 7)
    val consoleFont = MutableStateFlow(ConsoleFont.of(prefs.getString(Keys.CFONT, null)))
    val consoleWrap = boolFlow(Keys.CWRAP, true)
    val consoleTimestamps = boolFlow(Keys.CTIME, false)

    val languageFlow: StateFlow<AppLang> = language.asStateFlow()

    fun setLanguage(v: AppLang) {
        prefs.edit().putString(Keys.LANG, v.tag).apply()
        language.value = v
    }

    fun setDarkMode(v: Boolean) {
        prefs.edit().putBoolean(Keys.DARK, v).apply()
        darkMode.value = v
    }

    fun setAccent(argb: Int?) {
        val e = prefs.edit()
        if (argb == null) e.remove(Keys.ACCENT) else e.putInt(Keys.ACCENT, argb)
        e.apply()
        accentArgb.value = argb
    }

    fun setAutoStart(v: Boolean) {
        prefs.edit().putBoolean(Keys.AUTO_START, v).apply()
        autoStart.value = v
    }

    fun setKeepBackground(v: Boolean) {
        prefs.edit().putBoolean(Keys.KEEP_BG, v).apply()
        keepBackground.value = v
    }

    fun setNotifications(v: Boolean) {
        prefs.edit().putBoolean(Keys.NOTIF, v).apply()
        notifications.value = v
    }

    fun setLogRetentionDays(v: Int) {
        prefs.edit().putInt(Keys.RETENTION, v).apply()
        logRetentionDays.value = v
    }

    fun setConsoleFont(v: ConsoleFont) {
        prefs.edit().putString(Keys.CFONT, v.name).apply()
        consoleFont.value = v
    }

    fun setConsoleWrap(v: Boolean) {
        prefs.edit().putBoolean(Keys.CWRAP, v).apply()
        consoleWrap.value = v
    }

    fun setConsoleTimestamps(v: Boolean) {
        prefs.edit().putBoolean(Keys.CTIME, v).apply()
        consoleTimestamps.value = v
    }

    private object Keys {
        const val LANG = "lang"
        const val DARK = "dark_mode"
        const val ACCENT = "accent_argb"
        const val AUTO_START = "auto_start"
        const val KEEP_BG = "keep_background"
        const val NOTIF = "notifications"
        const val RETENTION = "log_retention_days"
        const val CFONT = "console_font"
        const val CWRAP = "console_wrap"
        const val CTIME = "console_timestamps"
    }
}
