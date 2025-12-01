package com.callmonitor.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {

    @Insert
    suspend fun insert(recording: RecordingEntity): Long

    @Update
    suspend fun update(recording: RecordingEntity)

    @Query("SELECT * FROM recordings ORDER BY timestamp DESC")
    fun getAllRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE isUploaded = 0 ORDER BY timestamp ASC")
    suspend fun getPendingUploads(): List<RecordingEntity>

    @Query("SELECT COUNT(*) FROM recordings WHERE isUploaded = 0")
    fun getPendingUploadCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM recordings")
    fun getTotalRecordingCount(): Flow<Int>

    @Query("UPDATE recordings SET isUploaded = 1 WHERE id = :id")
    suspend fun markAsUploaded(id: Long)

    @Query("UPDATE recordings SET uploadAttempts = uploadAttempts + 1, lastUploadAttempt = :timestamp WHERE id = :id")
    suspend fun incrementUploadAttempt(id: Long, timestamp: Long)

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getRecordingById(id: Long): RecordingEntity?

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteById(id: Long)
}
