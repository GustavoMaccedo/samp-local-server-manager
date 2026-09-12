package com.samplocal.manager.runtime

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

data class InstallProgress(val step: String, val done: Int, val total: Int)

class DependencyManager(private val context: Context) {
    private val _progress = MutableStateFlow<InstallProgress?>(null)
    val progress: StateFlow<InstallProgress?> = _progress

    suspend fun ensureBaseDirs(): List<String> = withContext(Dispatchers.IO) {
        val rt = LinuxRuntime(context)
        val steps = listOf("Diretorios", "Runtime", "Dependencias", "Verificacao de arquitetura")
        val done = mutableListOf<String>()
        steps.forEachIndexed { i, s ->
            _progress.value = InstallProgress(s, i, steps.size)
            when (s) {
                "Diretorios" -> {
                    rt.serversDir(); rt.baseDir(); rt.backupsDir()
                    File(rt.serversDir(), "Default").mkdirs()
                }
                "Runtime" -> {   }
                "Dependencias" -> {   }
                "Verificacao de arquitetura" -> { rt.checkRuntime() }
            }
            done.add(s)
            _progress.value = InstallProgress(s, i + 1, steps.size)
        }
        _progress.value = null
        done
    }
}
