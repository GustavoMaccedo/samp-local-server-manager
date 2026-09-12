package com.samplocal.manager

import com.samplocal.manager.core.AppLang
import com.samplocal.manager.ui.strings.Strings
import com.samplocal.manager.ui.strings.stringsFor
import org.junit.Assert.*
import org.junit.Test

class AppStringsTest {

    private fun sample(s: Strings): List<String> = listOf(
        s.settingsTitle, s.secAppearance, s.lang, s.modeDark, s.accent,
        s.autoStart, s.ret7d, s.fontMedium, s.consoleWrap, s.aboutVersion,
        s.creditsRole, s.thanksTitle, s.accentUse, s.tabHome, s.start,
        s.installTitle, s.infoDesc,
        s.dServer, s.dStart, s.dInfo, s.stRunning, s.stOffline,
        s.fImportServer, s.fEmpty, s.fAdd, s.fDeleted,
        s.cClear, s.cSave,
        s.nTitle, s.nTest, s.nConnect, s.nUnlink, s.nClaimTitle,
        s.sTitle, s.sSave, s.gTitle, s.gCopyFull,
        s.mCopying, s.mImportFail, s.mAddrCopied,
        s.mInstallingSvr, s.mSetupFail, s.mViewFailRead,
        s.gInstalled, s.gMissingN, s.nChecking, s.nDegraded, s.nViaRelay
    )

    @Test
    fun allLanguagesHaveNoBlankSamples() {
        for (lang in AppLang.values()) {
            sample(stringsFor(lang)).forEachIndexed { i, v ->
                assertTrue("amostra $i em branco em $lang", v.isNotBlank())
            }
        }
    }

    @Test
    fun spotCheckTranslations() {
        assertEquals("SETTINGS", stringsFor(AppLang.EN).settingsTitle)
        assertEquals("AJUSTES", stringsFor(AppLang.ES).settingsTitle)
        assertEquals("НАСТРОЙКИ", stringsFor(AppLang.RU).settingsTitle)
        assertEquals("START", stringsFor(AppLang.EN).start)
        assertEquals("COMENZAR", stringsFor(AppLang.ES).start)
        assertEquals("НАЧАТЬ", stringsFor(AppLang.RU).start)
        assertEquals("7 дней", stringsFor(AppLang.RU).ret7d)
        assertEquals("INICIAR", stringsFor(AppLang.PT_BR).dStart)
        assertEquals("ЗАПУСК", stringsFor(AppLang.RU).dStart)
        assertEquals("IMPORT SERVER", stringsFor(AppLang.EN).fImportServer)
        assertEquals("TEST CONNECTIVITY", stringsFor(AppLang.EN).nTest)
        assertEquals("ПОДКЛЮЧИТЬ PLAYIT", stringsFor(AppLang.RU).nConnect)
    }
}
