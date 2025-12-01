package com.callmonitor.upload

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.callmonitor.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

class UploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "UploadWorker"
        private const val DEFAULT_SERVER_URL = "http://192.168.0.104:8000"
        private const val PREF_SERVER_URL = "server_url"
    }

    private val database = AppDatabase.getInstance(applicationContext)
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "UploadWorker started")

        val serverUrl = prefs.getString(PREF_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        
        val apiService = createApiService(serverUrl)
        if (apiService == null) {
            Log.e(TAG, "Failed to create API service")
            return@withContext Result.retry()
        }

        val pendingUploads = database.recordingDao().getPendingUploads()
        Log.d(TAG, "Pending uploads: ${pendingUploads.size}")

        if (pendingUploads.isEmpty()) {
            return@withContext Result.success()
        }

        var allSuccessful = true

        for (recording in pendingUploads) {
            try {
                val file = File(recording.filePath)
                if (!file.exists()) {
                    Log.w(TAG, "File not found: ${recording.filePath}")
                    database.recordingDao().deleteById(recording.id)
                    continue
                }

                Log.d(TAG, "Uploading: ${recording.fileName}")

                val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", recording.fileName, requestFile)

                val phoneNumberBody = recording.phoneNumber.toRequestBody("text/plain".toMediaTypeOrNull())
                val isIncomingBody = recording.isIncoming.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val timestampBody = recording.timestamp.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val durationBody = recording.duration.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                val response = apiService.uploadRecording(
                    file = filePart,
                    phoneNumber = phoneNumberBody,
                    isIncoming = isIncomingBody,
                    timestamp = timestampBody,
                    duration = durationBody
                )

                if (response.isSuccessful) {
                    Log.d(TAG, "Upload successful: ${recording.fileName}")
                    database.recordingDao().markAsUploaded(recording.id)
                    
                    // Optionally delete local file after successful upload
                    // file.delete()
                } else {
                    Log.e(TAG, "Upload failed: ${response.code()} - ${response.message()}")
                    database.recordingDao().incrementUploadAttempt(recording.id, System.currentTimeMillis())
                    allSuccessful = false
                }

            } catch (e: Exception) {
                Log.e(TAG, "Upload error for ${recording.fileName}", e)
                database.recordingDao().incrementUploadAttempt(recording.id, System.currentTimeMillis())
                allSuccessful = false
            }
        }

        if (allSuccessful) {
            Log.d(TAG, "All uploads completed successfully")
            Result.success()
        } else {
            Log.d(TAG, "Some uploads failed, will retry")
            Result.retry()
        }
    }

    private fun createApiService(baseUrl: String): ApiService? {
        return try {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            retrofit.create(ApiService::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create API service", e)
            null
        }
    }
}
