package com.callmonitor.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.callmonitor.CallMonitorApp
import com.callmonitor.MainActivity
import com.callmonitor.R
import com.callmonitor.data.AppDatabase
import com.callmonitor.data.RecordingEntity
import com.callmonitor.encryption.EncryptionManager
import com.callmonitor.recording.AudioRecorder
import com.callmonitor.upload.UploadWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

class RecordingService : Service() {

    companion object {
        private const val TAG = "RecordingService"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START_RECORDING = "com.callmonitor.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.callmonitor.STOP_RECORDING"
        const val EXTRA_PHONE_NUMBER = "phone_number"
        const val EXTRA_IS_INCOMING = "is_incoming"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var audioRecorder: AudioRecorder? = null
    private var currentPhoneNumber: String = "Unknown"
    private var isIncoming: Boolean = false
    private var recordingStartTime: Long = 0
    private var currentRecordingFile: File? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val encryptionManager by lazy { EncryptionManager(this) }
    private val database by lazy { AppDatabase.getInstance(this) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")

        when (intent?.action) {
            ACTION_START_RECORDING -> {
                currentPhoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: "Unknown"
                isIncoming = intent.getBooleanExtra(EXTRA_IS_INCOMING, false)
                startForegroundWithNotification()
                startRecording()
            }
            ACTION_STOP_RECORDING -> {
                stopRecording()
            }
        }

        return START_STICKY
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

        return NotificationCompat.Builder(this, CallMonitorApp.CHANNEL_ID_RECORDING)
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

    private fun startRecording() {
        if (audioRecorder != null) {
            Log.w(TAG, "Recording already in progress")
            return
        }

        // Acquire wake lock to keep CPU running while recording
        acquireWakeLock()

        try {
            val timestamp = System.currentTimeMillis()
            val fileName = "call_${timestamp}_${currentPhoneNumber.replace(Regex("[^0-9+]"), "")}.mp3"
            val outputDir = File(filesDir, "recordings")
            outputDir.mkdirs()
            currentRecordingFile = File(outputDir, fileName)

            audioRecorder = AudioRecorder()
            audioRecorder?.startRecording(currentRecordingFile!!)
            recordingStartTime = timestamp

            Log.d(TAG, "Recording started: ${currentRecordingFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            releaseWakeLock()
            stopSelf()
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "CallMonitor:RecordingWakeLock"
            )
        }
        wakeLock?.acquire(60 * 60 * 1000L) // 1 hour max (for very long calls)
        Log.d(TAG, "Wake lock acquired")
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "Wake lock released")
            }
        }
    }

    private fun stopRecording() {
        Log.d(TAG, "Stopping recording")

        audioRecorder?.let { recorder ->
            try {
                recorder.stopRecording()
                
                currentRecordingFile?.let { file ->
                    if (file.exists() && file.length() > 0) {
                        processRecording(file)
                    } else {
                        Log.w(TAG, "Recording file is empty or doesn't exist")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop recording", e)
            }
        }

        audioRecorder = null
        currentRecordingFile = null
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun processRecording(recordingFile: File) {
        serviceScope.launch {
            try {
                Log.d(TAG, "Processing recording: ${recordingFile.absolutePath}")

                // Encrypt the recording
                val encryptedDir = File(filesDir, "encrypted")
                encryptedDir.mkdirs()
                val encryptedFile = File(encryptedDir, "${recordingFile.nameWithoutExtension}.enc")
                
                encryptionManager.encryptFile(recordingFile, encryptedFile)
                Log.d(TAG, "Recording encrypted: ${encryptedFile.absolutePath}")

                // Delete the original unencrypted file
                recordingFile.delete()

                // Save to database
                val recording = RecordingEntity(
                    fileName = encryptedFile.name,
                    filePath = encryptedFile.absolutePath,
                    phoneNumber = currentPhoneNumber,
                    isIncoming = isIncoming,
                    timestamp = recordingStartTime,
                    duration = System.currentTimeMillis() - recordingStartTime,
                    fileSize = encryptedFile.length(),
                    isUploaded = false
                )
                database.recordingDao().insert(recording)
                Log.d(TAG, "Recording saved to database")

                // Schedule upload
                scheduleUpload()

            } catch (e: Exception) {
                Log.e(TAG, "Failed to process recording", e)
            }
        }
    }

    private fun scheduleUpload() {
        val uploadRequest = OneTimeWorkRequestBuilder<UploadWorker>()
            .build()
        WorkManager.getInstance(this).enqueue(uploadRequest)
        Log.d(TAG, "Upload scheduled")
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRecorder?.stopRecording()
        releaseWakeLock()
        serviceScope.cancel()
        Log.d(TAG, "Service destroyed")
    }
}
