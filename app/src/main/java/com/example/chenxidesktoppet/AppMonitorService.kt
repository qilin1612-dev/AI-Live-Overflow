package com.example.chenxidesktoppet

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class AppMonitorService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var webView: WebView
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = true

    private val supabaseUrl = "https://epwoocskrthfqcvcysww.supabase.co"
    private val supabaseKey = "sb_publishable_duO2RgPx05O4sINU-kiZtQ_kA5276F2"

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        setupOverlay()
        startLoops()
    }

    private fun startForegroundService() {
        val channelId = "chenxi_pet_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "辰夕陪伴服务", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId).setContentTitle("辰夕正在后台看着你").setSmallIcon(android.R.drawable.ic_dialog_info).build()
        } else {
            Notification.Builder(this).setContentTitle("辰夕正在后台看着你").setSmallIcon(android.R.drawable.ic_dialog_info).build()
        }
        startForeground(1, notification)
    }

    private fun setupOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.END
        params.y = 200

        webView = WebView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled = true
            webViewClient = WebViewClient()
            loadUrl("file:///android_asset/pet.html")
        }

        var initialY = 0
        var initialTouchY = 0f
        webView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialY = params.y
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(webView, params)
                    true
                }
                else -> false
            }
        }
        windowManager.addView(webView, params)
    }

    private fun startLoops() {
        // 读取 AI 指令
        thread {
            while (isRunning) {
                try {
                    val url = URL("$supabaseUrl/rest/v1/clawd_state?select=*&order=id.desc&limit=1")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("apikey", supabaseKey)
                    conn.setRequestProperty("Authorization", "Bearer $supabaseKey")
                    if (conn.responseCode == 200) {
                        val reader = BufferedReader(InputStreamReader(conn.inputStream))
                        val response = reader.readText()
                        reader.close()
                        val jsonArray = JSONArray(response)
                        if (jsonArray.length() > 0) {
                            val obj = jsonArray.getJSONObject(0)
                            val msg = obj.optString("message", "我在后台陪着你")
                            val mood = obj.optString("mood", "normal")
                            val color = if (mood == "warning" || mood == "angry") "#e06c75" else "#98c379"
                            handler.post {
                                webView.evaluateJavascript("updateState('$msg', '$color')", null)
                            }
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
                Thread.sleep(3000)
            }
        }

        // 上报应用监控
        thread {
            var lastPkg = ""
            while (isRunning) {
                try {
                    val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                    val time = System.currentTimeMillis()
                    val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 10000, time)
                    var currentPkg = ""
                    var maxTime = 0L
                    for (stat in stats) {
                        if (stat.lastTimeUsed > maxTime) {
                            maxTime = stat.lastTimeUsed
                            currentPkg = stat.packageName
                        }
                    }
                    if (currentPkg.isNotEmpty() && currentPkg != lastPkg) {
                        lastPkg = currentPkg
                        val postUrl = URL("$supabaseUrl/rest/v1/sensor_logs")
                        val conn = postUrl.openConnection() as HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("apikey", supabaseKey)
                        conn.setRequestProperty("Authorization", "Bearer $supabaseKey")
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.doOutput = true
                        val json = JSONObject()
                        json.put("event_type", "app_changed")
                        json.put("package_name", currentPkg)
                        val writer = OutputStreamWriter(conn.outputStream)
                        writer.write(json.toString())
                        writer.flush()
                        writer.close()
                        conn.responseCode
                    }
                } catch (e: Exception) { e.printStackTrace() }
                Thread.sleep(3000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        if (::windowManager.isInitialized && ::webView.isInitialized) {
            windowManager.removeView(webView)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
