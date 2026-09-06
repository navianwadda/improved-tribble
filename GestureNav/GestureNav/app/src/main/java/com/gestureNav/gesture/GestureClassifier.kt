package com.gestureNav.gesture

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

object GestureClassifier {

    private const val FIST_CURL_THRESHOLD = 0.06f
    private const val PINCH_THRESHOLD = 0.06f
    private const val SWIPE_X_THRESHOLD = 0.18f
    private const val SWIPE_Y_THRESHOLD = 0.14f

    private var prevWristY: Float? = null
    private var prevWristX: Float? = null
    private var framesSinceGesture = 0
    private const val GESTURE_COOLDOWN_FRAMES = 12

    fun classify(landmarks: List<NormalizedLandmark>): GestureEvent {
        if (landmarks.size < 21) return GestureEvent.NONE

        val wrist = landmarks[0]
        val thumbTip = landmarks[4]
        val indexTip = landmarks[8]
        val middleTip = landmarks[12]
        val ringTip = landmarks[16]
        val pinkyTip = landmarks[20]
        val indexMcp = landmarks[5]
        val middleMcp = landmarks[9]
        val ringMcp = landmarks[13]
        val pinkyMcp = landmarks[17]

        framesSinceGesture++

        val pinchDist = dist(thumbTip, indexTip)
        if (pinchDist < PINCH_THRESHOLD && framesSinceGesture > GESTURE_COOLDOWN_FRAMES) {
            framesSinceGesture = 0
            prevWristX = null
            prevWristY = null
            return GestureEvent.PINCH
        }

        val indexCurled = indexTip.y() > indexMcp.y() + FIST_CURL_THRESHOLD
        val middleCurled = middleTip.y() > middleMcp.y() + FIST_CURL_THRESHOLD
        val ringCurled = ringTip.y() > ringMcp.y() + FIST_CURL_THRESHOLD
        val pinkyCurled = pinkyTip.y() > pinkyMcp.y() + FIST_CURL_THRESHOLD

        val isFist = indexCurled && middleCurled && ringCurled && pinkyCurled
        val isOpenPalm = !indexCurled && !middleCurled && !ringCurled && !pinkyCurled

        if (isFist && framesSinceGesture > GESTURE_COOLDOWN_FRAMES) {
            framesSinceGesture = 0
            prevWristX = null
            prevWristY = null
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

                if (Math.abs(dy) > Math.abs(dx)) {
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
        return Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    fun reset() {
        prevWristY = null
        prevWristX = null
        framesSinceGesture = 0
    }
}
