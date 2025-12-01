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
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.callmonitor.CallMonitorApp
import com.callmonitor.MainActivity
import com.callmonitor.R
import com.callmonitor.upload.UploadWorker
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Background service that runs continuously to:
 * 1. Keep the app alive for call detection
 * 2. Ping the server every 30 seconds
 * 3. Trigger uploads when server is available
 */
class BackgroundService : Service() {

    companion object {
        private const val TAG = "BackgroundService"
        private const val NOTIFICATION_ID = 2001
        private const val SERVER_URL = "http://192.168.0.104:8000"
        private const val PING_INTERVAL_MS = 30_000L // 30 seconds
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pingJob: Job? = null
    private var isServerOnline = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "BackgroundService created")
        startForegroundWithNotification()
        startPingLoop()
        schedulePeriodicUpload()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "BackgroundService started")
        return START_STICKY // Restart if killed
    }

    private fun startForegroundWithNotification() {
        val notification = createNotification("Monitoring for calls...")

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
    }

    private fun createNotification(status: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val serverStatus = if (isServerOnline) "🟢 Server Online" else "🔴 Server Offline"

        return NotificationCompat.Builder(this, CallMonitorApp.CHANNEL_ID_SERVICE)
            .setContentTitle("Call Monitor Active")
            .setContentText("$status | $serverStatus")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun updateNotification(status: String) {
        val notification = createNotification(status)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun startPingLoop() {
        pingJob?.cancel()
        pingJob = serviceScope.launch {
            while (isActive) {
                try {
                    val online = pingServer()
                    if (online != isServerOnline) {
                        isServerOnline = online
                        Log.d(TAG, "Server status changed: ${if (online) "ONLINE" else "OFFLINE"}")
                        
                        if (online) {
                            // Server came online - trigger upload
                            triggerUpload()
                            updateNotification("Syncing recordings...")
                        } else {
                            updateNotification("Waiting for server...")
                        }
                    }
                    
                    if (isServerOnline) {
                        updateNotification("Ready to record")
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Ping error", e)
                    isServerOnline = false
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
                
                responseCode == 200
            } catch (e: Exception) {
                Log.d(TAG, "Server ping failed: ${e.message}")
                false
            }
        }
    }

    private fun triggerUpload() {
        val uploadRequest = OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(this).enqueue(uploadRequest)
        Log.d(TAG, "Upload triggered")
    }

    private fun schedulePeriodicUpload() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Upload every 15 minutes
        val uploadRequest = PeriodicWorkRequestBuilder<UploadWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "periodic_upload",
            ExistingPeriodicWorkPolicy.KEEP,
            uploadRequest
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        pingJob?.cancel()
        serviceScope.cancel()
        Log.d(TAG, "BackgroundService destroyed")
        
        // Restart the service
        val restartIntent = Intent(this, BackgroundService::class.java)
        startService(restartIntent)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Restart when app is swiped away
        val restartIntent = Intent(this, BackgroundService::class.java)
        startService(restartIntent)
    }
}
