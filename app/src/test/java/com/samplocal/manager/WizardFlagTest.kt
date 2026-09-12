package com.samplocal.manager

import com.samplocal.manager.util.WizardFlag
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class WizardFlagTest {

    private fun record(dir: File) = File(dir, "onboarded_update_time")

    @Test
    fun freshInstallNeedsWizard() {
        val dir = createTempDir("wizard-flag")
        try {
            assertTrue(WizardFlag.needsWizard(record(dir), 12345L))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun onboardedSameInstallSkipsWizard() {
        val dir = createTempDir("wizard-flag")
        try {
            val f = record(dir)
            WizardFlag.markOnboarded(f, 12345L)
            assertFalse(WizardFlag.needsWizard(f, 12345L))

            assertFalse(WizardFlag.needsWizard(File(dir, "onboarded_update_time"), 12345L))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun reinstallNeedsWizardAgain() {
        val dir = createTempDir("wizard-flag")
        try {
            val f = record(dir)
            WizardFlag.markOnboarded(f, 111L)
            assertFalse(WizardFlag.needsWizard(f, 111L))

            assertTrue(WizardFlag.needsWizard(f, 222L))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun corruptedRecordNeedsWizard() {
        val dir = createTempDir("wizard-flag")
        try {
            val f = record(dir)
            f.parentFile?.mkdirs()
            f.writeText("lixo")
            assertTrue(WizardFlag.needsWizard(f, 999L))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun unknownUpdateTimeFallsBackToRecord() {
        val dir = createTempDir("wizard-flag")
        try {
            val f = record(dir)
            assertTrue(WizardFlag.needsWizard(f, null))
            WizardFlag.markOnboarded(f, null)
            assertFalse(WizardFlag.needsWizard(f, null))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun missingParentIsCreated() {
        val dir = createTempDir("wizard-flag")
        try {
            val f = File(dir, "sub/dir/onboarded_update_time")
            WizardFlag.markOnboarded(f, 7L)
            assertFalse(WizardFlag.needsWizard(f, 7L))
        } finally {
            dir.deleteRecursively()
        }
    }
}
