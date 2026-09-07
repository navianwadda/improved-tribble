package com.gestureNav.gesture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import androidx.camera.core.ExperimentalGetImage
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

class HandTracker(
    private val context: Context,
    private val onGesture: (GestureEvent) -> Unit,
    private val onLandmarks: (HandLandmarkerResult) -> Unit = {}
) {
    private var handLandmarker: HandLandmarker? = null
    private var lastGestureTime = 0L
    private val gestureCooldownMs = 400L

    fun start() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("hand_landmarker.task")
            .build()

        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setNumHands(1)
            .setMinHandDetectionConfidence(0.6f)
            .setMinHandPresenceConfidence(0.6f)
            .setMinTrackingConfidence(0.6f)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result, _ ->
                onLandmarks(result)
                if (result.landmarks().isNotEmpty()) {
                    val gesture = GestureClassifier.classify(result.landmarks()[0])
                    val now = System.currentTimeMillis()
                    if (gesture != GestureEvent.NONE && now - lastGestureTime > gestureCooldownMs) {
                        lastGestureTime = now
                        onGesture(gesture)
                    }
                }
            }
            .setErrorListener { error ->
                error.printStackTrace()
            }
            .build()

        handLandmarker = HandLandmarker.createFromOptions(context, options)
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    fun processFrame(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val bitmap = imageProxy.toBitmap()
        val matrix = Matrix().apply {
            postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        }
        val flippedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        val mpImage = BitmapImageBuilder(flippedBitmap).build()
        handLandmarker?.detectAsync(mpImage, System.currentTimeMillis())
        imageProxy.close()
    }

    fun stop() {
        handLandmarker?.close()
        handLandmarker = null
        GestureClassifier.reset()
    }
}
