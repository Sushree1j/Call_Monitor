package com.callmonitor.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.callmonitor.CallMonitorApp
import com.callmonitor.MainActivity
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

class BackgroundService : Service() {

    companion object {
        private const val TAG = "BackgroundService"
        private const val NOTIFICATION_ID = 2001
        private const val SERVER_URL = "http://192.168.0.104:8000"
        private const val PING_INTERVAL_MS = 30_000L
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pingJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "BackgroundService created")
        try {
            startForegroundWithNotification()
            startPingLoop()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "BackgroundService started")
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = createNotification()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground", e)
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CallMonitorApp.CHANNEL_ID_SERVICE)
            .setContentTitle("")
            .setContentText("")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()
    }

    private fun startPingLoop() {
        pingJob?.cancel()
        pingJob = serviceScope.launch {
            while (isActive) {
                try {
                    pingServer()
                } catch (e: Exception) {
                    Log.e(TAG, "Ping error", e)
                }
                delay(PING_INTERVAL_MS)
            }
        }
    }

    private suspend fun pingServer(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$SERVER_URL/health")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.requestMethod = "GET"
                val responseCode = connection.responseCode
                connection.disconnect()
                Log.d(TAG, "Server ping: $responseCode")
                responseCode == 200
            } catch (e: Exception) {
                Log.d(TAG, "Server offline")
                false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pingJob?.cancel()
        serviceScope.cancel()
        Log.d(TAG, "BackgroundService destroyed")
    }
}
