package com.callmonitor.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val phoneNumber: String,
    val isIncoming: Boolean,
    val timestamp: Long,
    val duration: Long,
    val fileSize: Long,
    val isUploaded: Boolean = false,
    val uploadAttempts: Int = 0,
    val lastUploadAttempt: Long? = null
)
