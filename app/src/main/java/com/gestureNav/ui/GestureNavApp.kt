package com.gestureNav.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.gestureNav.service.GestureAccessibilityService

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun GestureNavApp() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    var accessibilityEnabled by remember { mutableStateOf(isAccessibilityEnabled(context)) }
    var serviceRunning by remember { mutableStateOf(GestureAccessibilityService.isRunning) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityEnabled = isAccessibilityEnabled(context)
                serviceRunning = GestureAccessibilityService.isRunning
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val allReady = cameraPermission.status.isGranted && accessibilityEnabled

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            Text(
                text = "GestureNav",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Control your phone with your hands",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 40.dp)
            )

            SetupStep(
                number = 1,
                title = "Camera permission",
                subtitle = if (cameraPermission.status.isGranted) "Granted" else "Required for hand tracking",
                done = cameraPermission.status.isGranted,
                actionLabel = if (!cameraPermission.status.isGranted) "Grant" else null,
                onAction = { cameraPermission.launchPermissionRequest() }
            )

            Spacer(Modifier.height(12.dp))

            SetupStep(
                number = 2,
                title = "Accessibility service",
                subtitle = if (accessibilityEnabled) "Enabled" else "Required to control other apps",
                done = accessibilityEnabled,
                actionLabel = if (!accessibilityEnabled) "Enable" else null,
                onAction = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            )

            Spacer(Modifier.height(40.dp))

            if (allReady) {
                AnimatedContent(targetState = serviceRunning, label = "toggle") { running ->
                    Button(
                        onClick = {
                            val action = if (running) GestureAccessibilityService.ACTION_STOP
                                       else GestureAccessibilityService.ACTION_START
                            context.startService(
                                Intent(context, GestureAccessibilityService::class.java).apply { this.action = action }
                            )
                            serviceRunning = !running
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (running) MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (running) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (running) "Stop GestureNav" else "Start GestureNav",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))
                GestureGuide()
            }
        }
    }
}

@Composable
private fun SetupStep(
    number: Int,
    title: String,
    subtitle: String,
    done: Boolean,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (done) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (done) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (done) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                } else {
                    Text("$number", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (actionLabel != null) {
                TextButton(onClick = onAction) {
                    Text(actionLabel, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GestureGuide() {
    Text(
        "Gesture Reference",
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    )

    val gestures = listOf(
        Triple(Icons.Default.KeyboardArrowUp, "Open palm — move up", "Scroll up"),
        Triple(Icons.Default.KeyboardArrowDown, "Open palm — move down", "Scroll down"),
        Triple(Icons.Default.ArrowBack, "Open palm — swipe right", "Go back"),
        Triple(Icons.Default.Apps, "Open palm — swipe left", "Recents"),
        Triple(Icons.Default.Home, "Close fist", "Home"),
        Triple(Icons.Default.Notifications, "Pinch fingers", "Notifications")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        gestures.forEach { (icon, gesture, action) ->
            GestureRow(icon, gesture, action)
        }
    }
}

@Composable
private fun GestureRow(icon: ImageVector, gesture: String, action: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(action, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(gesture, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun isAccessibilityEnabled(context: Context): Boolean {
    val service = "${context.packageName}/${GestureAccessibilityService::class.java.canonicalName}"
    return try {
        val enabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED
        )
        if (enabled != 1) return false
        val services = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        TextUtils.SimpleStringSplitter(':').apply { setString(services) }.any {
            it.equals(service, ignoreCase = true)
        }
    } catch (e: Exception) {
        false
    }
}
