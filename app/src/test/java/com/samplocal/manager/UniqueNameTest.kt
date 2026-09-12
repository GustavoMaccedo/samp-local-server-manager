package com.samplocal.manager

import com.samplocal.manager.core.FileManager
import org.junit.Assert.*
import org.junit.Test

class UniqueNameTest {

    @Test
    fun firstUseKeepsName() {
        val dir = createTempDir("uniq")
        try {
            val fm = FileManager()
            assertEquals("mod.amx", fm.resolveUniqueName(dir, "mod.amx").name)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun collisionAppendsCounter() {
        val dir = createTempDir("uniq2")
        try {
            val fm = FileManager()
            java.io.File(dir, "mod.amx").writeText("x")
            assertEquals("mod (2).amx", fm.resolveUniqueName(dir, "mod.amx").name)
            java.io.File(dir, "mod (2).amx").writeText("x")
            assertEquals("mod (3).amx", fm.resolveUniqueName(dir, "mod.amx").name)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun unsafeNamesSanitized() {
        val dir = createTempDir("uniq3")
        try {
            val fm = FileManager()
            val out = fm.resolveUniqueName(dir, "../../evil.sh")
            assertFalse(out.name.contains("/"))
            assertFalse(".." in out.name)

            assertTrue(out.canonicalPath.startsWith(dir.canonicalPath))
        } finally {
            dir.deleteRecursively()
        }
    }
}
