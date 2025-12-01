package com.callmonitor

import android.app.Application
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

            // Recording channel
            val recordingChannel = NotificationChannel(
                CHANNEL_ID_RECORDING,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(recordingChannel)

            // Service channel
            val serviceChannel = NotificationChannel(
                CHANNEL_ID_SERVICE,
                "Call Monitor Service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Background service for call monitoring"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
