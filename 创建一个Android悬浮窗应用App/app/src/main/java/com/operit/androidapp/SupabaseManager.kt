package com.operit.androidapp

import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class SupabaseManager(private val context: Context) {
    private val client = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())
    private val baseUrl = "https://epwoocskrthfqcvcysww.supabase.co/rest/v1"
    private val apiKey = "sb_publishable_duO2RgPx05O4sINU-kiZtQ_kA5276F2"

    private val monitorRunnable = object : Runnable {
        override fun run() {
            getForegroundApp()?.let { uploadSensorLog(it) }
            handler.postDelayed(this, 10000)
        }
    }

    private val listenRunnable = object : Runnable {
        override fun run() {
            fetchClawdState()
            handler.postDelayed(this, 5000)
        }
    }

    fun start() {
        handler.post(monitorRunnable)
        handler.post(listenRunnable)
    }

    private fun getForegroundApp(): String? {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        return usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000000, time)
            ?.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    private fun uploadSensorLog(packageName: String) {
        val url = "$baseUrl/sensor_logs"
        val json = JSONObject().apply {
            put("sensor_type", "app_usage")
            put("payload", JSONObject().put("foreground_app", packageName))
        }
        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).post(body).addHeader("apikey", apiKey)
            .addHeader("Authorization", "Bearer $apiKey").addHeader("Content-Type", "application/json").build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) { response.close() }
        })
    }

    private fun fetchClawdState() {
        val url = "$baseUrl/clawd_state?select=*&order=updated_at.desc&limit=1"
        val request = Request.Builder().url(url).get().addHeader("apikey", apiKey)
            .addHeader("Authorization", "Bearer $apiKey").build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let {
                    try {
                        val array = JSONArray(it)
                        if (array.length() > 0) Log.d("Supabase", "Command: ${array.getJSONObject(0).optString("speech_bubble", "")}")
                    } catch (e: Exception) {}
                }
                response.close()
            }
        })
    }
}
