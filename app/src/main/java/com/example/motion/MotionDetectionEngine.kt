package com.example.motion

import android.graphics.Bitmap
import android.graphics.Rect
import com.example.data.model.MotionZoneConfig
import java.io.ByteArrayOutputStream

class MotionDetectionEngine(
    private var config: MotionZoneConfig = MotionZoneConfig(),
    private val onMotionTriggered: (motionScore: Int, zone: String, snapshotBytes: ByteArray) -> Unit
) {
    private var lastFrameTime = 0L
    private var lastTriggerTime = 0L
    private var previousGrid: IntArray? = null
    private val gridCols = 8
    private val gridRows = 8

    fun updateConfig(newConfig: MotionZoneConfig) {
        config = newConfig
    }

    fun processFrame(bitmap: Bitmap) {
        val now = System.currentTimeMillis()
        // Process max 5 frames/sec to keep CPU usage minimal (< 5%)
        if (now - lastFrameTime < 200) return
        lastFrameTime = now

        try {
            // Downscale to 64x64 for lightning-fast algorithmic differential analysis
            val small = Bitmap.createScaledBitmap(bitmap, 64, 64, false)
            val currentGrid = IntArray(gridCols * gridRows)
            val blockW = 64 / gridCols
            val blockH = 64 / gridRows

            // Calculate average luminance per grid block
            for (gy in 0 until gridRows) {
                for (gx in 0 until gridCols) {
                    var sumLum = 0
                    var count = 0
                    for (by in 0 until blockH) {
                        for (bx in 0 until blockW) {
                            val px = small.getPixel(gx * blockW + bx, gy * blockH + by)
                            // Luminance formula
                            val r = (px shr 16) and 0xFF
                            val g = (px shr 8) and 0xFF
                            val b = px and 0xFF
                            sumLum += (r * 299 + g * 587 + b * 114) / 1000
                            count++
                        }
                    }
                    currentGrid[gy * gridCols + gx] = sumLum / count
                }
            }

            val prev = previousGrid
            previousGrid = currentGrid
            if (prev == null) return

            // Calculate differential motion score per block
            var motionBlocks = 0
            var activeZoneBlocks = 0
            val threshold = (100 - config.sensitivity).coerceIn(10, 80) // Higher sensitivity = lower threshold

            for (gy in 0 until gridRows) {
                for (gx in 0 until gridCols) {
                    if (!isBlockInActiveZone(gx, gy, config.activeZone)) continue
                    activeZoneBlocks++

                    val idx = gy * gridCols + gx
                    val diff = Math.abs(currentGrid[idx] - prev[idx])
                    if (diff > threshold) {
                        motionBlocks++
                    }
                }
            }

            if (activeZoneBlocks == 0) activeZoneBlocks = 1
            val motionPercent = (motionBlocks * 100) / activeZoneBlocks

            // Trigger criteria: at least 3 blocks changed or motionPercent > 12%
            if (motionBlocks >= 3 && motionPercent >= 12) {
                val cooldownMs = config.cooldownSeconds * 1000L
                if (now - lastTriggerTime > cooldownMs) {
                    lastTriggerTime = now
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                    val snapshotBytes = stream.toByteArray()
                    onMotionTriggered(motionPercent, config.activeZone, snapshotBytes)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isBlockInActiveZone(gx: Int, gy: Int, zone: String): Boolean {
        return when (zone) {
            "CENTER" -> gx in 2..5 && gy in 2..5
            "ENTRYWAY" -> gx in 2..5 && gy in 4..7
            "LEFT_SECTOR" -> gx in 0..3
            "RIGHT_SECTOR" -> gx in 4..7
            else -> true // FULL_FRAME
        }
    }
}
