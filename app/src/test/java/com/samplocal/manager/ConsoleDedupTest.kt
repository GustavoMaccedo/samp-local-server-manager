package com.samplocal.manager

import com.samplocal.manager.core.ServerManager
import org.junit.Assert.*
import org.junit.Test

class ConsoleDedupTest {

    @Test
    fun onlyFreshLines() {
        val all = listOf("a", "b", "c")
        val (fresh, next) = ServerManager.newLines(all, 2)
        assertEquals(listOf("c"), fresh)
        assertEquals(3, next)
    }

    @Test
    fun noRepeatOnUnchangedFile() {
        val all = listOf("a", "b")
        val (fresh, _) = ServerManager.newLines(all, 2)
        assertTrue(fresh.isEmpty())
    }

    @Test
    fun recreatedFileResets() {
        val (fresh, next) = ServerManager.newLines(listOf("x"), 10)
        assertEquals(listOf("x"), fresh)
        assertEquals(1, next)
    }
}
