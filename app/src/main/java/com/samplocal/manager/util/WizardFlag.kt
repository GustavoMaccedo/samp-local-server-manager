package com.samplocal.manager.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File

object WizardFlag {
    private const val FILE_NAME = "onboarded_update_time"

    fun recordFile(context: Context): File =
        File(context.noBackupFilesDir, FILE_NAME)

    fun currentUpdateTime(context: Context): Long? = try {
        val pm = context.packageManager
        val pi = if (Build.VERSION.SDK_INT >= 33) {
            pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, 0)
        }
        pi.lastUpdateTime
    } catch (_: Exception) { null }

    fun needsWizard(record: File, updateTime: Long?): Boolean {
        if (updateTime == null) return !safeIsFile(record)
        return try {
            record.readText().trim().toLong() != updateTime
        } catch (_: Exception) { true }
    }

    fun markOnboarded(record: File, updateTime: Long?) {
        record.parentFile?.mkdirs()
        record.writeText(updateTime?.toString() ?: "1")
    }

    fun shouldShowWizard(context: Context): Boolean {
        cleanupLegacy(context)
        return try {
            needsWizard(recordFile(context), currentUpdateTime(context))
        } catch (_: Exception) { true }
    }

    fun markOnboarded(context: Context) {
        cleanupLegacy(context)
        try { markOnboarded(recordFile(context), currentUpdateTime(context)) } catch (_: Exception) { }
    }

    private fun safeIsFile(f: File): Boolean = try { f.isFile } catch (_: Exception) { false }

    private fun cleanupLegacy(context: Context) {
        try {
            val legacy = context.getSharedPreferences("samp", Context.MODE_PRIVATE)
            if (legacy.contains("wizard")) legacy.edit().remove("wizard").apply()
        } catch (_: Exception) { }
    }
}
