package com.gestureNav.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Path
import android.os.Build
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.gestureNav.gesture.GestureEvent
import com.gestureNav.gesture.HandTracker
import java.util.concurrent.Executors

class GestureAccessibilityService : AccessibilityService(), LifecycleOwner {

    companion object {
        const val ACTION_TOGGLE = "com.gestureNav.TOGGLE"
        const val PREF_NAME = "gesture_nav_prefs"
        const val PREF_TRACKING = "tracking_enabled"

        var isRunning = false
            private set
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private var handTracker: HandTracker? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private lateinit var prefs: SharedPreferences

    private val screenWidth: Int
        get() {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                wm.currentWindowMetrics.bounds.width()
            } else {
                @Suppress("DEPRECATION")
                val point = android.graphics.Point()
                @Suppress("DEPRECATION")
                wm.defaultDisplay.getSize(point)
                point.x
            }
        }

    private val screenHeight: Int
        get() {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                wm.currentWindowMetrics.bounds.height()
            } else {
                @Suppress("DEPRECATION")
                val point = android.graphics.Point()
                @Suppress("DEPRECATION")
                wm.defaultDisplay.getSize(point)
                point.y
            }
        }

    private val toggleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_TOGGLE) {
                if (isRunning) stopTracking() else startTracking()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val filter = IntentFilter(ACTION_TOGGLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(toggleReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(toggleReceiver, filter)
        }

        if (prefs.getBoolean(PREF_TRACKING, false)) {
            startTracking()
        }
    }

    private fun startTracking() {
        if (isRunning) return
        isRunning = true
        prefs.edit().putBoolean(PREF_TRACKING, true).apply()

        handTracker = HandTracker(
            context = this,
            onGesture = ::handleGesture
        ).also { it.start() }

        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            try {
                cameraProvider = providerFuture.get()
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(cameraExecutor) { frame -> handTracker?.processFrame(frame) } }

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, analysis)
            } catch (e: Exception) {
                e.printStackTrace()
                isRunning = false
                prefs.edit().putBoolean(PREF_TRACKING, false).apply()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopTracking() {
        isRunning = false
        prefs.edit().putBoolean(PREF_TRACKING, false).apply()
        cameraProvider?.unbindAll()
        cameraProvider = null
        handTracker?.stop()
        handTracker = null
    }

    private fun handleGesture(event: GestureEvent) {
        val w = screenWidth
        val h = screenHeight
        val cx = w / 2f
        val cy = h / 2f
        val scrollDist = h * 0.40f

        when (event) {
            GestureEvent.SCROLL_UP -> performSwipe(cx, cy + scrollDist, cx, cy - scrollDist, durationMs = 500)
            GestureEvent.SCROLL_DOWN -> performSwipe(cx, cy - scrollDist, cx, cy + scrollDist, durationMs = 500)
            GestureEvent.SWIPE_RIGHT -> performGlobalAction(GLOBAL_ACTION_BACK)
            GestureEvent.SWIPE_LEFT -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            GestureEvent.FIST_CLOSED -> performGlobalAction(GLOBAL_ACTION_HOME)
            GestureEvent.PINCH -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            else -> {}
        }
    }

    private fun performSwipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 400) {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        stopTracking()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopTracking()
        try {
            unregisterReceiver(toggleReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        cameraExecutor.shutdown()
        return super.onUnbind(intent)
    }
}
