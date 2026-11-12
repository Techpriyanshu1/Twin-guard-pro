package com.example.recording

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.example.data.db.MotionEventEntity
import com.example.data.db.RecordingEntity
import com.example.data.db.TwinGuardDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocalRecorder(private val context: Context) {

    private val db = TwinGuardDatabase.getDatabase(context)

    val recordingsDir: File
        get() = File(context.getExternalFilesDir(null), "TwinGuard/Camera_Recordings").apply { mkdirs() }

    val motionEventsDir: File
        get() = File(context.getExternalFilesDir(null), "TwinGuard/Motion_Events").apply { mkdirs() }

    val screenshotsDir: File
        get() = File(context.getExternalFilesDir(null), "TwinGuard/Screenshots").apply { mkdirs() }

    suspend fun saveScreenshot(jpegBytes: ByteArray, isMotionEvent: Boolean = false): String = withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val dir = if (isMotionEvent) motionEventsDir else screenshotsDir
        val prefix = if (isMotionEvent) "MOTION_SNAP_" else "SNAP_"
        val file = File(dir, "${prefix}${timeStamp}.jpg")

        FileOutputStream(file).use { out ->
            out.write(jpegBytes)
            out.flush()
        }

        file.absolutePath
    }

    suspend fun recordMotionEvent(
        snapshotBytes: ByteArray,
        confidence: Int,
        zoneName: String,
        videoPath: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val snapshotPath = saveScreenshot(snapshotBytes, isMotionEvent = true)
        val event = MotionEventEntity(
            snapshotPath = snapshotPath,
            recordingPath = videoPath,
            confidence = confidence,
            zoneName = zoneName
        )
        db.motionEventDao().insert(event)
    }

    suspend fun registerRecording(
        file: File,
        title: String,
        durationSeconds: Long,
        isMotionEvent: Boolean = false,
        resolution: String = "720p"
    ): Long = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) return@withContext -1L
        val entity = RecordingEntity(
            title = title,
            filePath = file.absolutePath,
            durationSeconds = durationSeconds,
            fileSizeBytes = file.length(),
            isMotionEvent = isMotionEvent,
            resolution = resolution
        )
        db.recordingDao().insert(entity)
    }

    suspend fun deleteRecording(recording: RecordingEntity) = withContext(Dispatchers.IO) {
        val file = File(recording.filePath)
        if (file.exists()) {
            file.delete()
        }
        db.recordingDao().delete(recording)
    }

    suspend fun deleteMotionEvent(event: MotionEventEntity) = withContext(Dispatchers.IO) {
        val snap = File(event.snapshotPath)
        if (snap.exists()) snap.delete()
        event.recordingPath?.let {
            val vid = File(it)
            if (vid.exists()) vid.delete()
        }
        db.motionEventDao().delete(event)
    }

    fun getStorageStats(): Pair<Long, Long> {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalMb = (totalBlocks * blockSize) / (1024 * 1024)
            val freeMb = (availableBlocks * blockSize) / (1024 * 1024)
            Pair(freeMb, totalMb)
        } catch (e: Exception) {
            Pair(1024L, 64000L)
        }
    }
}
