package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY timestamp DESC")
    fun getAllRecordings(): Flow<List<RecordingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recording: RecordingEntity): Long

    @Delete
    suspend fun delete(recording: RecordingEntity)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM recordings")
    suspend fun clearAll()
}

@Dao
interface MotionEventDao {
    @Query("SELECT * FROM motion_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<MotionEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: MotionEventEntity): Long

    @Delete
    suspend fun delete(event: MotionEventEntity)

    @Query("DELETE FROM motion_events WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM motion_events")
    suspend fun clearAll()
}

@Dao
interface PairedCameraDao {
    @Query("SELECT * FROM paired_cameras ORDER BY lastSeen DESC")
    fun getAllPairedCameras(): Flow<List<PairedCameraEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(camera: PairedCameraEntity)

    @Delete
    suspend fun delete(camera: PairedCameraEntity)

    @Query("SELECT * FROM paired_cameras WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getCameraById(deviceId: String): PairedCameraEntity?
}
