package com.example.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Log
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.data.model.CameraQuality
import com.example.motion.MotionDetectionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(
    private val context: Context,
    private val onFrameEncoded: (ByteArray) -> Unit,
    private val motionEngine: MotionDetectionEngine? = null
) {
    private val tag = "CameraManager"
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null

    private var currentLensFacing = CameraSelector.LENS_FACING_BACK
    private var currentQuality = CameraQuality.BALANCED
    private var isTorchEnabled = false
    private var isStrobeEnabled = false
    private var isPrivacyMode = false
    private var isNightVision = false
    private var currentZoomRatio = 1.0f

    private var strobeJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var frameCount = 0
    private var lastFpsTimestamp = System.currentTimeMillis()
    var currentFps = 30f
        private set

    fun initialize(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView?,
        onReady: () -> Unit = {}
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCamera(lifecycleOwner, previewView)
                onReady()
            } catch (e: Exception) {
                Log.e(tag, "Camera initialization failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun bindCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView?) {
        val provider = cameraProvider ?: return
        try {
            provider.unbindAll()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(currentLensFacing)
                .build()

            val preview = Preview.Builder()
                .setTargetResolution(Size(currentQuality.width, currentQuality.height))
                .build()

            previewView?.let {
                preview.setSurfaceProvider(it.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(currentQuality.width, currentQuality.height))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                processImageProxy(imageProxy)
            }

            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )

            cameraControl = camera?.cameraControl
            cameraInfo = camera?.cameraInfo

            if (currentLensFacing == CameraSelector.LENS_FACING_BACK && isTorchEnabled) {
                cameraControl?.enableTorch(true)
            }
            cameraControl?.setZoomRatio(currentZoomRatio)

            Log.d(tag, "Camera bound: Lens=$currentLensFacing, Quality=${currentQuality.label}")
        } catch (e: Exception) {
            Log.e(tag, "Use case binding failed", e)
        }
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        try {
            frameCount++
            val now = System.currentTimeMillis()
            if (now - lastFpsTimestamp >= 1000) {
                currentFps = (frameCount * 1000f) / (now - lastFpsTimestamp)
                frameCount = 0
                lastFpsTimestamp = now
            }

            if (isPrivacyMode) {
                // Send privacy placeholder frame
                val privacyBitmap = Bitmap.createBitmap(currentQuality.width, currentQuality.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(privacyBitmap)
                canvas.drawColor(Color.parseColor("#090D16"))
                val paint = Paint().apply {
                    color = Color.parseColor("#38BDF8")
                    textSize = 42f
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("🔒 PRIVACY SHIELD ACTIVE", (currentQuality.width / 2).toFloat(), (currentQuality.height / 2).toFloat(), paint)

                val stream = ByteArrayOutputStream()
                privacyBitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream)
                onFrameEncoded(stream.toByteArray())
                return
            }

            val bitmap = imageProxy.toBitmap()
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees

            val adjustedBitmap = if (rotationDegrees != 0 || currentLensFacing == CameraSelector.LENS_FACING_FRONT) {
                val matrix = Matrix()
                matrix.postRotate(rotationDegrees.toFloat())
                if (currentLensFacing == CameraSelector.LENS_FACING_FRONT) {
                    matrix.postScale(-1f, 1f)
                }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }

            // Run offline motion and human silhouette detection
            motionEngine?.processFrame(adjustedBitmap)

            val qualityCompress = when (currentQuality) {
                CameraQuality.LOW -> 60
                CameraQuality.BALANCED -> 75
                CameraQuality.HIGH -> 85
            }
            val stream = ByteArrayOutputStream()
            adjustedBitmap.compress(Bitmap.CompressFormat.JPEG, qualityCompress, stream)
            val jpegBytes = stream.toByteArray()

            onFrameEncoded(jpegBytes)
        } catch (e: Exception) {
            Log.e(tag, "Error processing frame", e)
        } finally {
            imageProxy.close()
        }
    }

    fun toggleTorch(): Boolean {
        if (currentLensFacing != CameraSelector.LENS_FACING_BACK) return false
        stopStrobe()
        isTorchEnabled = !isTorchEnabled
        cameraControl?.enableTorch(isTorchEnabled)
        return isTorchEnabled
    }

    fun toggleStrobe(): Boolean {
        if (currentLensFacing != CameraSelector.LENS_FACING_BACK) return false
        if (isStrobeEnabled) {
            stopStrobe()
            return false
        }
        isStrobeEnabled = true
        isTorchEnabled = false
        strobeJob = coroutineScope.launch {
            var state = false
            while (isActive && isStrobeEnabled) {
                state = !state
                cameraControl?.enableTorch(state)
                delay(120) // Flash at ~8Hz
            }
            cameraControl?.enableTorch(false)
        }
        return true
    }

    fun stopStrobe() {
        isStrobeEnabled = false
        strobeJob?.cancel()
        strobeJob = null
        if (!isTorchEnabled) {
            cameraControl?.enableTorch(false)
        }
    }

    fun togglePrivacyMode(): Boolean {
        isPrivacyMode = !isPrivacyMode
        return isPrivacyMode
    }

    fun toggleNightVision(): Boolean {
        isNightVision = !isNightVision
        return isNightVision
    }

    fun switchCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView?): Boolean {
        stopStrobe()
        currentLensFacing = if (currentLensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        isTorchEnabled = false
        bindCamera(lifecycleOwner, previewView)
        return currentLensFacing == CameraSelector.LENS_FACING_FRONT
    }

    fun setZoom(zoom: Float) {
        currentZoomRatio = zoom.coerceIn(1.0f, 5.0f)
        cameraControl?.setZoomRatio(currentZoomRatio)
    }

    fun zoomIn() {
        setZoom(currentZoomRatio + 0.5f)
    }

    fun zoomOut() {
        setZoom(currentZoomRatio - 0.5f)
    }

    fun setQuality(quality: CameraQuality, lifecycleOwner: LifecycleOwner, previewView: PreviewView?) {
        if (currentQuality == quality) return
        currentQuality = quality
        bindCamera(lifecycleOwner, previewView)
    }

    fun isFrontCamera(): Boolean = currentLensFacing == CameraSelector.LENS_FACING_FRONT
    fun isTorchOn(): Boolean = isTorchEnabled
    fun isStrobeOn(): Boolean = isStrobeEnabled
    fun isPrivacyOn(): Boolean = isPrivacyMode
    fun isNightVisionOn(): Boolean = isNightVision
    fun getZoomRatio(): Float = currentZoomRatio

    fun release() {
        stopStrobe()
        try {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
        } catch (e: Exception) {
            Log.e(tag, "Error releasing camera", e)
        }
    }
}
