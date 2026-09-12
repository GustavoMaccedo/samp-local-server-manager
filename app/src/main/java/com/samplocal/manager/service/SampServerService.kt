package com.samplocal.manager.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.samplocal.manager.MainActivity
import com.samplocal.manager.SampApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SampServerService : Service() {

    companion object {
        const val CHANNEL_ID = "samp_server"
        const val NOTIF_ID = 1001
        const val EXTRA_SERVER_ID = "server_id"
        const val ACTION_START = "com.samplocal.manager.START"
        const val ACTION_STOP = "com.samplocal.manager.STOP"

        fun startIntent(ctx: android.content.Context, serverId: String): Intent {
            return Intent(ctx, SampServerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SERVER_ID, serverId)
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverId: String = "Default"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serverId = intent?.getStringExtra(EXTRA_SERVER_ID) ?: serverId
        when (intent?.action) {
            ACTION_STOP -> {
                scope.launch {
                    (application as SampApp).serverManager.stop(serverId)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIF_ID, buildNotification("Servidor iniciando...", "127.0.0.1"))
                scope.launch {
                    val mgr = (application as SampApp).serverManager
                    mgr.start(serverId)
                    updateNotification()
                }
            }
        }
        return START_STICKY
    }

    private fun serverNotificationsOn(): Boolean = try {
        (application as? SampApp)?.appPrefs?.notifications?.value ?: true
    } catch (_: Exception) { true }

    private fun updateNotification() {
        val mgr = (application as? SampApp)?.serverManager ?: return
        val nm = getSystemService(NotificationManager::class.java)
        if (!serverNotificationsOn()) {

            nm.notify(NOTIF_ID, buildSilentNotification())
            return
        }
        val info = mgr.servers.value.firstOrNull { it.id == serverId }
        val addr = "127.0.0.1:${info?.port ?: 7777}"
        nm.notify(NOTIF_ID, buildNotification(info?.hostname ?: serverId, addr))
    }

    private fun buildNotification(title: String, text: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SAMP Local Server - $title")
            .setContentText("Servidor online em $text")
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    private fun buildSilentNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SAMP Local")
            .setContentText("Servidor em execução")
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "SAMP Server", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
