package com.callmonitor

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class CallMonitorApp : Application() {

    companion object {
        const val CHANNEL_ID_RECORDING = "recording_channel"
        const val CHANNEL_ID_SERVICE = "service_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val notificationManager = getSystemService(NotificationManager::class.java)

                // Recording channel
                val recordingChannel = NotificationChannel(
                    CHANNEL_ID_RECORDING,
                    "Service",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Background service"
                    setShowBadge(false)
                    setSound(null, null)
                }
                notificationManager.createNotificationChannel(recordingChannel)

                // Service channel
                val serviceChannel = NotificationChannel(
                    CHANNEL_ID_SERVICE,
                    "Background",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Background process"
                    setShowBadge(false)
                    setSound(null, null)
                }
                notificationManager.createNotificationChannel(serviceChannel)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
