package com.example.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
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
    private var currentZoomRatio = 1.0f

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

            // Preview use-case
            val preview = Preview.Builder()
                .setTargetResolution(Size(currentQuality.width, currentQuality.height))
                .build()

            previewView?.let {
                preview.setSurfaceProvider(it.surfaceProvider)
            }

            // Image analysis for streaming and motion detection
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

            // Restore torch & zoom states if applicable
            if (currentLensFacing == CameraSelector.LENS_FACING_BACK && isTorchEnabled) {
                cameraControl?.enableTorch(true)
            }
            cameraControl?.setZoomRatio(currentZoomRatio)

            Log.d(tag, "Camera bound successfully: Lens=$currentLensFacing, Quality=${currentQuality.label}")
        } catch (e: Exception) {
            Log.e(tag, "Use case binding failed", e)
        }
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        try {
            // Calculate real FPS
            frameCount++
            val now = System.currentTimeMillis()
            if (now - lastFpsTimestamp >= 1000) {
                currentFps = (frameCount * 1000f) / (now - lastFpsTimestamp)
                frameCount = 0
                lastFpsTimestamp = now
            }

            val bitmap = imageProxy.toBitmap()
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees

            val adjustedBitmap = if (rotationDegrees != 0 || currentLensFacing == CameraSelector.LENS_FACING_FRONT) {
                val matrix = Matrix()
                matrix.postRotate(rotationDegrees.toFloat())
                if (currentLensFacing == CameraSelector.LENS_FACING_FRONT) {
                    matrix.postScale(-1f, 1f) // Mirror front camera preview
                }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }

            // Run offline motion detection
            motionEngine?.processFrame(adjustedBitmap)

            // Compress to JPEG for local streaming
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
        isTorchEnabled = !isTorchEnabled
        cameraControl?.enableTorch(isTorchEnabled)
        return isTorchEnabled
    }

    fun setTorch(enable: Boolean) {
        if (currentLensFacing != CameraSelector.LENS_FACING_BACK) return
        isTorchEnabled = enable
        cameraControl?.enableTorch(isTorchEnabled)
    }

    fun switchCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView?): Boolean {
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
    fun getZoomRatio(): Float = currentZoomRatio

    fun release() {
        try {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
        } catch (e: Exception) {
            Log.e(tag, "Error releasing camera", e)
        }
    }
}
