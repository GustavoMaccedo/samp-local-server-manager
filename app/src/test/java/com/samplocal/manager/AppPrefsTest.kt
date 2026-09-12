package com.samplocal.manager

import android.content.SharedPreferences
import com.samplocal.manager.core.AppLang
import com.samplocal.manager.core.AppPrefs
import com.samplocal.manager.core.ConsoleFont
import org.junit.Assert.*
import org.junit.Test

private class FakePrefs : SharedPreferences {
    val map = mutableMapOf<String, Any?>()
    inner class Ed : SharedPreferences.Editor {
        private val put = mutableMapOf<String, Any?>()
        private val del = mutableSetOf<String>()
        override fun putBoolean(k: String, v: Boolean) = apply { put[k] = v }
        override fun putInt(k: String, v: Int) = apply { put[k] = v }
        override fun putString(k: String, v: String?) = apply { put[k] = v }
        override fun putFloat(k: String, v: Float) = apply { put[k] = v }
        override fun putLong(k: String, v: Long) = apply { put[k] = v }
        override fun putStringSet(k: String, v: MutableSet<String>?) = apply { put[k] = v }
        override fun remove(k: String) = apply { del += k }
        override fun clear() = apply { map.clear(); put.clear() }
        override fun commit(): Boolean {
            map.keys.removeAll(del); map.putAll(put); return true
        }
        override fun apply() {
            commit()
        }
    }
    override fun getBoolean(k: String, d: Boolean) = map[k] as? Boolean ?: d
    override fun getInt(k: String, d: Int) = (map[k] as? Int) ?: d
    override fun getString(k: String, d: String?) = map[k] as? String ?: d
    override fun getFloat(k: String, d: Float) = (map[k] as? Float) ?: d
    override fun getLong(k: String, d: Long) = (map[k] as? Long) ?: d
    override fun getStringSet(k: String, d: MutableSet<String>?) =
        @Suppress("UNCHECKED_CAST") (map[k] as? MutableSet<String>) ?: d
    override fun contains(k: String) = map.containsKey(k)
    override fun edit() = Ed()
    override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun getAll() = map.toMap()
}

class AppPrefsTest {

    @Test
    fun defaultsMatchCurrentBehavior() {
        val p = AppPrefs(FakePrefs())
        assertEquals(AppLang.PT_BR, p.language.value)
        assertTrue(p.darkMode.value)
        assertNull(p.accentArgb.value)
        assertFalse(p.autoStart.value)
        assertTrue(p.keepBackground.value)
        assertTrue(p.notifications.value)
        assertEquals(7, p.logRetentionDays.value)
        assertEquals(ConsoleFont.MEDIUM, p.consoleFont.value)
        assertTrue(p.consoleWrap.value)
        assertFalse(p.consoleTimestamps.value)
    }

    @Test
    fun settersPersistAndUpdateFlows() {
        val backing = FakePrefs()
        val p = AppPrefs(backing)
        p.setLanguage(AppLang.RU)
        p.setDarkMode(false)
        p.setAccent(0xFFFF0000.toInt())
        p.setAutoStart(true)
        p.setKeepBackground(false)
        p.setNotifications(false)
        p.setLogRetentionDays(30)
        p.setConsoleFont(ConsoleFont.LARGE)
        p.setConsoleWrap(false)
        p.setConsoleTimestamps(true)
        assertEquals(AppLang.RU, p.language.value)
        assertFalse(p.darkMode.value)
        assertEquals(0xFFFF0000.toInt(), p.accentArgb.value)
        assertTrue(p.autoStart.value)
        assertFalse(p.keepBackground.value)
        assertFalse(p.notifications.value)
        assertEquals(30, p.logRetentionDays.value)
        assertEquals(ConsoleFont.LARGE, p.consoleFont.value)
        assertFalse(p.consoleWrap.value)
        assertTrue(p.consoleTimestamps.value)

        val p2 = AppPrefs(backing)
        assertEquals(AppLang.RU, p2.language.value)
        assertEquals(0xFFFF0000.toInt(), p2.accentArgb.value)
        assertEquals(ConsoleFont.LARGE, p2.consoleFont.value)
    }

    @Test
    fun accentNullClearsToDefault() {
        val backing = FakePrefs()
        val p = AppPrefs(backing)
        p.setAccent(0xFF00FF00.toInt())
        assertNotNull(p.accentArgb.value)
        p.setAccent(null)
        assertNull(AppPrefs(backing).accentArgb.value)
    }

    @Test
    fun invalidStoredValuesFallBack() {
        val backing = FakePrefs()
        backing.map["lang"] = "xx"
        backing.map["console_font"] = "HUGE"
        val p = AppPrefs(backing)
        assertEquals(AppLang.PT_BR, p.language.value)
        assertEquals(ConsoleFont.MEDIUM, p.consoleFont.value)
    }
}
