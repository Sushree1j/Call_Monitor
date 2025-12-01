package com.callmonitor.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.callmonitor.service.RecordingService

class CallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CallReceiver"
        private var lastState = TelephonyManager.CALL_STATE_IDLE
        private var isIncoming = false
        private var savedNumber: String? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_NEW_OUTGOING_CALL -> {
                // Outgoing call
                savedNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
                Log.d(TAG, "Outgoing call to: $savedNumber")
            }
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                    ?: savedNumber

                val state = when (stateStr) {
                    TelephonyManager.EXTRA_STATE_IDLE -> TelephonyManager.CALL_STATE_IDLE
                    TelephonyManager.EXTRA_STATE_OFFHOOK -> TelephonyManager.CALL_STATE_OFFHOOK
                    TelephonyManager.EXTRA_STATE_RINGING -> TelephonyManager.CALL_STATE_RINGING
                    else -> return
                }

                onCallStateChanged(context, state, number)
            }
        }
    }

    private fun onCallStateChanged(context: Context, state: Int, number: String?) {
        if (lastState == state) return

        Log.d(TAG, "Call state changed: $lastState -> $state, number: $number")

        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                // Incoming call ringing
                isIncoming = true
                savedNumber = number
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                // Call answered or outgoing call started
                if (lastState == TelephonyManager.CALL_STATE_RINGING) {
                    // Incoming call answered
                    Log.d(TAG, "Incoming call answered from: $number")
                } else {
                    // Outgoing call started
                    isIncoming = false
                    Log.d(TAG, "Outgoing call started to: $savedNumber")
                }
                // Start recording
                startRecording(context, savedNumber ?: "Unknown", isIncoming)
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                // Call ended
                if (lastState == TelephonyManager.CALL_STATE_OFFHOOK) {
                    // Call was active, now ended
                    Log.d(TAG, "Call ended")
                    stopRecording(context)
                }
                // Reset state
                isIncoming = false
                savedNumber = null
            }
        }

        lastState = state
    }

    private fun startRecording(context: Context, phoneNumber: String, incoming: Boolean) {
        Log.d(TAG, "Starting recording for: $phoneNumber (incoming: $incoming)")
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START_RECORDING
            putExtra(RecordingService.EXTRA_PHONE_NUMBER, phoneNumber)
            putExtra(RecordingService.EXTRA_IS_INCOMING, incoming)
        }
        context.startForegroundService(intent)
    }

    private fun stopRecording(context: Context) {
        Log.d(TAG, "Stopping recording")
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP_RECORDING
        }
        context.startService(intent)
    }
}
