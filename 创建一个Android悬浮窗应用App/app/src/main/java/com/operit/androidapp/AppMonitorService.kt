package com.operit.androidapp

import android.app.Notification
import com.example.chenxidesktoppet.SupabaseManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AppMonitorService : Service() {
    private lateinit var supabaseManager: SupabaseManager

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification())
        supabaseManager = SupabaseManager(this)
        supabaseManager.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "MonitorChannel",
                "App Monitor Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "MonitorChannel")
            .setContentTitle("辰夕监督中")
            .setContentText("距离国考还有114天，不要摸鱼哦！")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }
}
