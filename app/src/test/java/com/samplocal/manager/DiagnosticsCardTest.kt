package com.samplocal.manager

import com.samplocal.manager.ui.components.shortDisplayPath
import org.junit.Assert.*
import org.junit.Test

class DiagnosticsCardTest {

    @Test
    fun shortPathKeepsShort() {
        assertEquals("/a/b", shortDisplayPath("/a/b"))
    }

    @Test
    fun longPathSummarized() {
        val full = "/data/user/0/com.samplocal.manager.debug/files/servers/teste"
        val short = shortDisplayPath(full)
        assertTrue(short.length < full.length)
        assertTrue(short.endsWith("servers/teste"))
        assertTrue(short.startsWith("/data/"))
    }

    @Test
    fun backendPathSummarized() {
        val full = "/data/user/0/com.samplocal.manager.debug/files/runtime/bin/qemu-i386"
        val short = shortDisplayPath(full)
        assertTrue("bin/qemu-i386" in short)
    }
}
