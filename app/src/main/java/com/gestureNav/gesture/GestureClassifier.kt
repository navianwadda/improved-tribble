package com.gestureNav.gesture

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.sqrt

object GestureClassifier {

    private const val PINCH_THRESHOLD = 0.09f
    private const val SWIPE_X_THRESHOLD = 0.10f
    private const val SWIPE_Y_THRESHOLD = 0.08f
    private const val SCROLL_Y_THRESHOLD = 0.04f
    private const val FIST_CURL_THRESHOLD = 0.04f

    private var motionStartY: Float? = null
    private var motionStartX: Float? = null
    private var prevWristY: Float? = null
    private var prevWristX: Float? = null

    private var framesSinceGesture = 0
    private var motionFrames = 0
    private const val GESTURE_COOLDOWN_FRAMES = 12
    private const val MOTION_SAMPLE_FRAMES = 4

    fun classify(landmarks: List<NormalizedLandmark>): GestureEvent {
        if (landmarks.size < 21) return GestureEvent.NONE

        val wrist      = landmarks[0]
        val thumbTip   = landmarks[4]
        val indexTip   = landmarks[8]
        val indexPip   = landmarks[6]
        val middleTip  = landmarks[12]
        val middlePip  = landmarks[10]
        val ringTip    = landmarks[16]
        val ringPip    = landmarks[14]
        val pinkyTip   = landmarks[20]
        val pinkyPip   = landmarks[18]

        framesSinceGesture++

        val indexCurled  = indexTip.y()  > indexPip.y()  + FIST_CURL_THRESHOLD
        val middleCurled = middleTip.y() > middlePip.y() + FIST_CURL_THRESHOLD
        val ringCurled   = ringTip.y()   > ringPip.y()   + FIST_CURL_THRESHOLD
        val pinkyCurled  = pinkyTip.y()  > pinkyPip.y()  + FIST_CURL_THRESHOLD

        val isFist = indexCurled && middleCurled && ringCurled && pinkyCurled
        val isOpenPalm = !indexCurled && !middleCurled && !ringCurled && !pinkyCurled

        val onlyIndexUp = !indexCurled && middleCurled && ringCurled && pinkyCurled
        val indexAndMiddleUp = !indexCurled && !middleCurled && ringCurled && pinkyCurled

        if (dist(thumbTip, indexTip) < PINCH_THRESHOLD && framesSinceGesture > GESTURE_COOLDOWN_FRAMES) {
            framesSinceGesture = 0
            clearMotion()
            return GestureEvent.PINCH
        }

        if (isFist && framesSinceGesture > GESTURE_COOLDOWN_FRAMES) {
            framesSinceGesture = 0
            clearMotion()
            return GestureEvent.FIST_CLOSED
        }

        if (onlyIndexUp || indexAndMiddleUp) {
            val currentY = wrist.y()
            val currentX = wrist.x()

            if (motionStartY == null) {
                motionStartY = currentY
                motionStartX = currentX
                motionFrames = 0
            } else {
                motionFrames++
            }

            val startY = motionStartY!!
            val startX = motionStartX!!
            val dy = currentY - startY
            val dx = currentX - startX

            if (motionFrames >= MOTION_SAMPLE_FRAMES && framesSinceGesture > GESTURE_COOLDOWN_FRAMES) {
                DebugLogger.log("finger-scroll dy=$dy dx=$dx (threshold=$SCROLL_Y_THRESHOLD)")
                if (abs(dy) > SCROLL_Y_THRESHOLD) {
                    framesSinceGesture = 0
                    clearMotion()
                    return if (dy < 0) GestureEvent.SCROLL_UP else GestureEvent.SCROLL_DOWN
                }
            }

            prevWristY = currentY
            prevWristX = currentX
            return GestureEvent.NONE
        }

        if (isOpenPalm) {
            val currentY = wrist.y()
            val currentX = wrist.x()

            if (motionStartY == null) {
                motionStartY = currentY
                motionStartX = currentX
                motionFrames = 0
            } else {
                motionFrames++
            }

            val startY = motionStartY!!
            val startX = motionStartX!!
            val dy = currentY - startY
            val dx = currentX - startX

            if (motionFrames >= MOTION_SAMPLE_FRAMES && framesSinceGesture > GESTURE_COOLDOWN_FRAMES) {
                if (abs(dy) > abs(dx)) {
                    if (abs(dy) > SWIPE_Y_THRESHOLD) {
                        framesSinceGesture = 0
                        clearMotion()
                        return if (dy < 0) GestureEvent.SCROLL_UP else GestureEvent.SCROLL_DOWN
                    }
                } else {
                    if (abs(dx) > SWIPE_X_THRESHOLD) {
                        framesSinceGesture = 0
                        clearMotion()
                        return if (dx < 0) GestureEvent.SWIPE_LEFT else GestureEvent.SWIPE_RIGHT
                    }
                }
            }

            prevWristY = currentY
            prevWristX = currentX
            return GestureEvent.OPEN_PALM
        }

        clearMotion()
        prevWristY = wrist.y()
        prevWristX = wrist.x()
        return GestureEvent.NONE
    }

    private fun clearMotion() {
        motionStartY = null
        motionStartX = null
        motionFrames = 0
    }

    private fun dist(a: NormalizedLandmark, b: NormalizedLandmark): Float {
        val dx = a.x() - b.x()
        val dy = a.y() - b.y()
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    fun reset() {
        prevWristY = null
        prevWristX = null
        clearMotion()
        framesSinceGesture = 0
    }
}
