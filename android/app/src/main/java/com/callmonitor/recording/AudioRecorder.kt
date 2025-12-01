package com.callmonitor.recording

import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class AudioRecorder {

    companion object {
        private const val TAG = "AudioRecorder"
    }

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false

    fun startRecording(outputFile: File) {
        if (isRecording) {
            Log.w(TAG, "Already recording")
            return
        }

        try {
            mediaRecorder = createMediaRecorder().apply {
                // Try VOICE_CALL first (works on OnePlus and many OEM devices)
                // Falls back to MIC if VOICE_CALL fails
                val audioSource = getAudioSource()
                setAudioSource(audioSource)
                Log.d(TAG, "Using audio source: $audioSource")

                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(outputFile.absolutePath)

                prepare()
                start()
            }

            isRecording = true
            Log.d(TAG, "Recording started: ${outputFile.absolutePath}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording with primary source, trying fallback", e)
            tryFallbackRecording(outputFile)
        }
    }

    private fun getAudioSource(): Int {
        // Try VOICE_CALL first - works on OnePlus, Xiaomi, Samsung, and many others
        return try {
            // Test if VOICE_CALL is available
            MediaRecorder.AudioSource.VOICE_CALL
        } catch (e: Exception) {
            Log.w(TAG, "VOICE_CALL not available, using MIC")
            MediaRecorder.AudioSource.MIC
        }
    }

    private fun tryFallbackRecording(outputFile: File) {
        try {
            mediaRecorder?.release()
            mediaRecorder = createMediaRecorder().apply {
                // Fallback to VOICE_RECOGNITION (often less restricted than VOICE_CALL)
                setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(outputFile.absolutePath)

                prepare()
                start()
            }

            isRecording = true
            Log.d(TAG, "Recording started with VOICE_RECOGNITION fallback")

        } catch (e: Exception) {
            Log.e(TAG, "Fallback recording also failed, trying MIC", e)
            tryMicRecording(outputFile)
        }
    }

    private fun tryMicRecording(outputFile: File) {
        try {
            mediaRecorder?.release()
            mediaRecorder = createMediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(outputFile.absolutePath)

                prepare()
                start()
            }

            isRecording = true
            Log.d(TAG, "Recording started with MIC source")

        } catch (e: Exception) {
            Log.e(TAG, "All recording methods failed", e)
            throw e
        }
    }

    @Suppress("DEPRECATION")
    private fun createMediaRecorder(): MediaRecorder {
        return MediaRecorder()
    }

    fun stopRecording() {
        if (!isRecording) {
            Log.w(TAG, "Not recording")
            return
        }

        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
            Log.d(TAG, "Recording stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        } finally {
            mediaRecorder = null
            isRecording = false
        }
    }

    fun isRecording(): Boolean = isRecording
}
