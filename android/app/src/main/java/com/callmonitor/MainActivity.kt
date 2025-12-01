package com.callmonitor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.callmonitor.data.AppDatabase
import com.callmonitor.databinding.ActivityMainBinding
import com.callmonitor.service.BackgroundService
import com.callmonitor.upload.UploadWorker
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val database by lazy { AppDatabase.getInstance(this) }
    private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    companion object {
        private const val PREF_SERVER_URL = "server_url"
        private const val DEFAULT_SERVER_URL = "http://192.168.0.104:8000"
        private const val UPLOAD_WORK_NAME = "periodic_upload"
    }

    private val requiredPermissions = mutableListOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.PROCESS_OUTGOING_CALLS
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            updateUI()
            startBackgroundService()
            Toast.makeText(this, "All permissions granted. Call recording is active!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Some permissions were denied. App may not work properly.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        checkPermissions()
        observeRecordings()
        startBackgroundService()
    }

    private fun startBackgroundService() {
        val serviceIntent = Intent(this, BackgroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun setupUI() {
        // Load saved server URL
        val savedUrl = prefs.getString(PREF_SERVER_URL, DEFAULT_SERVER_URL)
        binding.editServerUrl.setText(savedUrl)

        // Save server URL button
        binding.btnSaveServer.setOnClickListener {
            val url = binding.editServerUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                prefs.edit().putString(PREF_SERVER_URL, url).apply()
                Toast.makeText(this, "Server URL saved", Toast.LENGTH_SHORT).show()
            }
        }

        // Manual sync button
        binding.btnSyncNow.setOnClickListener {
            triggerManualSync()
        }

        // Request permissions button
        binding.btnRequestPermissions.setOnClickListener {
            requestPermissions()
        }
    }

    private fun checkPermissions() {
        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            binding.txtPermissionStatus.text = "✅ All permissions granted"
            binding.txtPermissionStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            binding.txtPermissionStatus.text = "⚠️ Permissions needed"
            binding.txtPermissionStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
            requestPermissions()
        }
    }

    private fun requestPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            // Show explanation dialog
            AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage(
                    "This app needs the following permissions to record calls:\n\n" +
                    "• Microphone - To record audio\n" +
                    "• Phone State - To detect calls\n" +
                    "• Call Log - To get caller information\n" +
                    "• Notifications - To show recording status"
                )
                .setPositiveButton("Grant") { _, _ ->
                    permissionLauncher.launch(permissionsToRequest)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun observeRecordings() {
        lifecycleScope.launch {
            database.recordingDao().getTotalRecordingCount().collectLatest { total ->
                binding.txtTotalRecordings.text = "Total Recordings: $total"
            }
        }

        lifecycleScope.launch {
            database.recordingDao().getPendingUploadCount().collectLatest { pending ->
                binding.txtPendingUploads.text = "Pending Uploads: $pending"
            }
        }
    }

    private fun updateUI() {
        checkPermissions()
    }

    private fun schedulePeriodicUpload() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadRequest = PeriodicWorkRequestBuilder<UploadWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            UPLOAD_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            uploadRequest
        )
    }

    private fun triggerManualSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadRequest = androidx.work.OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueue(uploadRequest)
        Toast.makeText(this, "Sync triggered", Toast.LENGTH_SHORT).show()
    }
}
