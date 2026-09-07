package com.gestureNav.gesture

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.sqrt

object GestureClassifier {

    private const val PINCH_THRESHOLD = 0.06f
    private const val SWIPE_X_THRESHOLD = 0.20f
    private const val SWIPE_Y_THRESHOLD = 0.16f

    // Fist: all 4 fingertips must be well below their PIP joints (not just MCP)
    private const val FIST_CURL_THRESHOLD = 0.04f

    private var prevWristY: Float? = null
    private var prevWristX: Float? = null
    private var framesSinceGesture = 0
    private const val GESTURE_COOLDOWN_FRAMES = 15

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

        // Pinch: thumb + index close
        if (dist(thumbTip, indexTip) < PINCH_THRESHOLD && framesSinceGesture > GESTURE_COOLDOWN_FRAMES) {
            framesSinceGesture = 0
            prevWristX = null; prevWristY = null
            return GestureEvent.PINCH
        }

        // Fist: all fingertips below their PIP joints (tighter check than MCP)
        val indexCurled  = indexTip.y()  > indexPip.y()  + FIST_CURL_THRESHOLD
        val middleCurled = middleTip.y() > middlePip.y() + FIST_CURL_THRESHOLD
        val ringCurled   = ringTip.y()   > ringPip.y()   + FIST_CURL_THRESHOLD
        val pinkyCurled  = pinkyTip.y()  > pinkyPip.y()  + FIST_CURL_THRESHOLD

        val isFist = indexCurled && middleCurled && ringCurled && pinkyCurled
        val isOpenPalm = !indexCurled && !middleCurled && !ringCurled && !pinkyCurled

        if (isFist && framesSinceGesture > GESTURE_COOLDOWN_FRAMES) {
            framesSinceGesture = 0
            prevWristX = null; prevWristY = null
            return GestureEvent.FIST_CLOSED
        }

        if (isOpenPalm) {
            val prevY = prevWristY
            val prevX = prevWristX
            prevWristY = wrist.y()
            prevWristX = wrist.x()

            if (prevY != null && prevX != null && framesSinceGesture > GESTURE_COOLDOWN_FRAMES) {
                val dy = wrist.y() - prevY
                val dx = wrist.x() - prevX

                if (abs(dy) > abs(dx)) {
                    if (dy < -SWIPE_Y_THRESHOLD) {
                        framesSinceGesture = 0
                        return GestureEvent.SCROLL_UP
                    } else if (dy > SWIPE_Y_THRESHOLD) {
                        framesSinceGesture = 0
                        return GestureEvent.SCROLL_DOWN
                    }
                } else {
                    if (dx < -SWIPE_X_THRESHOLD) {
                        framesSinceGesture = 0
                        return GestureEvent.SWIPE_LEFT
                    } else if (dx > SWIPE_X_THRESHOLD) {
                        framesSinceGesture = 0
                        return GestureEvent.SWIPE_RIGHT
                    }
                }
            }
            return GestureEvent.OPEN_PALM
        }

        prevWristY = wrist.y()
        prevWristX = wrist.x()
        return GestureEvent.NONE
    }

    private fun dist(a: NormalizedLandmark, b: NormalizedLandmark): Float {
        val dx = a.x() - b.x()
        val dy = a.y() - b.y()
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    fun reset() {
        prevWristY = null
        prevWristX = null
        framesSinceGesture = 0
    }
}
