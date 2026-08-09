package com.example.chenxidesktoppet

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }
        val btnOverlay = Button(this).apply {
            text = "1. 开启悬浮窗权限"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                } else {
                    Toast.makeText(this@MainActivity, "悬浮窗已授权", Toast.LENGTH_SHORT).show()
                }
            }
        }
        val btnUsage = Button(this).apply {
            text = "2. 开启使用情况访问权限"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        }
        val btnStart = Button(this).apply {
            text = "3. 启动辰夕桌宠"
            setOnClickListener {
                if (Settings.canDrawOverlays(this@MainActivity)) {
                    startForegroundService(Intent(this@MainActivity, AppMonitorService::class.java))
                    Toast.makeText(this@MainActivity, "辰夕已启动，请回到桌面", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "请先开启悬浮窗权限哦", Toast.LENGTH_SHORT).show()
                }
            }
        }
        layout.addView(btnOverlay)
        layout.addView(btnUsage)
        layout.addView(btnStart)
        setContentView(layout)
    }
}
