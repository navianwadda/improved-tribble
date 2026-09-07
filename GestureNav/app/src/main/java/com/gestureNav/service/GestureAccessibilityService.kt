package com.gestureNav.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.view.Display
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
import com.gestureNav.ui.MainActivity
import java.util.concurrent.Executors

class GestureAccessibilityService : AccessibilityService(), LifecycleOwner {

    companion object {
        const val ACTION_START = "com.gestureNav.START"
        const val ACTION_STOP = "com.gestureNav.STOP"
        const val CHANNEL_ID = "gesture_nav_channel"
        const val NOTIF_ID = 1

        var isRunning = false
            private set
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private var handTracker: HandTracker? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private val screenWidth: Int get() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            bounds.width()
        } else {
            @Suppress("DEPRECATION")
            val display = wm.defaultDisplay
            val point = android.graphics.Point()
            display.getSize(point)
            point.x
        }
    }

    private val screenHeight: Int get() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            bounds.height()
        } else {
            @Suppress("DEPRECATION")
            val display = wm.defaultDisplay
            val point = android.graphics.Point()
            display.getSize(point)
            point.y
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        if (isRunning) return
        isRunning = true
        startForeground(NOTIF_ID, buildNotification())

        handTracker = HandTracker(
            context = this,
            onGesture = ::handleGesture
        ).also { it.start() }

        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            cameraProvider = providerFuture.get()
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(cameraExecutor) { frame -> handTracker?.processFrame(frame) } }

            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, analysis)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopTracking() {
        isRunning = false
        cameraProvider?.unbindAll()
        handTracker?.stop()
        handTracker = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun handleGesture(event: GestureEvent) {
        val w = screenWidth
        val h = screenHeight
        val cx = w / 2f
        val cy = h / 2f

        when (event) {
            GestureEvent.SCROLL_UP -> performSwipe(cx, cy + 400f, cx, cy - 400f)
            GestureEvent.SCROLL_DOWN -> performSwipe(cx, cy - 400f, cx, cy + 400f)
            GestureEvent.SWIPE_RIGHT -> performGlobalAction(GLOBAL_ACTION_BACK)
            GestureEvent.SWIPE_LEFT -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            GestureEvent.FIST_CLOSED -> performGlobalAction(GLOBAL_ACTION_HOME)
            GestureEvent.PINCH -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            else -> {}
        }
    }

    private fun performSwipe(startX: Float, startY: Float, endX: Float, endY: Float) {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 300)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() { stopTracking() }

    override fun onUnbind(intent: Intent?): Boolean {
        stopTracking()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        return super.onUnbind(intent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "GestureNav Active",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Shown while gesture control is active" }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, GestureAccessibilityService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("GestureNav is active")
            .setContentText("Hand gestures controlling your phone")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_delete, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }
}
