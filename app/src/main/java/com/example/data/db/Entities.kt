package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val filePath: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Long = 0,
    val fileSizeBytes: Long = 0,
    val isMotionEvent: Boolean = false,
    val resolution: String = "720p"
)

@Entity(tableName = "motion_events")
data class MotionEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val snapshotPath: String,
    val recordingPath: String? = null,
    val confidence: Int = 85,
    val zoneName: String = "Full Frame"
)

@Entity(tableName = "paired_cameras")
data class PairedCameraEntity(
    @PrimaryKey val deviceId: String,
    val name: String,
    val ipAddress: String,
    val port: Int = 8080,
    val lastSeen: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val authToken: String = ""
)
