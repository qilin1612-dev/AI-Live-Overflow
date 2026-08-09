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
import android.view.View
import android.view.WindowManager
import android.widget.TextView
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
    private lateinit var floatingView: TextView
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
            val channel = NotificationChannel(channelId, "辰夕监督桌宠", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId).setContentTitle("桌宠后台正在运行！").setContentText("距离国考还有114天，不要摸鱼哦！").setSmallIcon(android.R.drawable.ic_dialog_info).build()
        } else {
            Notification.Builder(this).setContentTitle("桌宠后台正在运行！").setContentText("距离国考还有114天，不要摸鱼哦！").setSmallIcon(android.R.drawable.ic_dialog_info).build()
        }
        startForeground(1, notification)
    }

    private fun setupOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 200

        floatingView = TextView(this).apply {
            text = "辰夕: 正在连接世界..."
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#88000000"))
            setPadding(20, 20, 20, 20)
            textSize = 16f
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        floatingView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingView, params)
                    true
                }
                else -> false
            }
        }
        windowManager.addView(floatingView, params)
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
                            val msg = obj.optString("message", "辰夕: 我在后台陪着你")
                            handler.post {
                                floatingView.text = msg
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
        if (::windowManager.isInitialized && ::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
