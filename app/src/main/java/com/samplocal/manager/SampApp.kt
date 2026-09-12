package com.samplocal.manager

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.samplocal.manager.core.AppPrefs
import com.samplocal.manager.core.ServerManager
import com.samplocal.manager.net.ConnectivityRepository
import com.samplocal.manager.runtime.RuntimeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SampApp : Application() {
    lateinit var serverManager: ServerManager
        private set
    lateinit var runtimeManager: RuntimeManager
        private set
    lateinit var connectivity: ConnectivityRepository
        private set

    val appPrefs: AppPrefs by lazy {
        AppPrefs(getSharedPreferences("app_settings", MODE_PRIVATE))
    }

    private val bgScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var startedActivities = 0
    private val bgHandler = Handler(Looper.getMainLooper())
    private val bgCheck = Runnable {
        if (startedActivities == 0 && !appPrefs.keepBackground.value) {
            bgScope.launch {
                try {
                    serverManager.servers.value
                        .filter { it.status == com.samplocal.manager.data.model.ServerStatus.RUNNING }
                        .forEach { serverManager.stop(it.id) }
                } catch (_: Exception) { }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        runtimeManager = RuntimeManager(this)
        serverManager = ServerManager(this)
        serverManager.refresh()
        connectivity = ConnectivityRepository(this)

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(a: Activity) {
                startedActivities++
                bgHandler.removeCallbacks(bgCheck)
            }
            override fun onActivityStopped(a: Activity) {
                startedActivities = (startedActivities - 1).coerceAtLeast(0)
                bgHandler.removeCallbacks(bgCheck)
                bgHandler.postDelayed(bgCheck, 3000)
            }
            override fun onActivityCreated(a: Activity, b: Bundle?) {}
            override fun onActivityResumed(a: Activity) {}
            override fun onActivityPaused(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
            override fun onActivityDestroyed(a: Activity) {}
        })
    }
}
