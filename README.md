# GestureNav

Control scroll, back, home, and recents on any Android app using hand gestures — no touching required.

## How it works

- **MediaPipe** detects hand landmarks via the front camera in real-time
- **AccessibilityService** injects swipe gestures and global actions system-wide
- Runs as a foreground service with a persistent notification

## Gestures

| Gesture | Action |
|---|---|
| Open palm → move up | Scroll up |
| Open palm → move down | Scroll down |
| Open palm → swipe right | Go back |
| Open palm → swipe left | Recent apps |
| Close fist | Home |
| Pinch | Notification shade |

## Setup

### 1. Download the MediaPipe model

Download `hand_landmarker.task` from MediaPipe:
```
https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task
```
Place it in:
```
app/src/main/assets/hand_landmarker.task
```

### 2. Build & install

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 3. First launch

1. Tap **Grant** → allow camera permission
2. Tap **Enable** → find "GestureNav" in Accessibility settings and turn it on
3. Return to the app → tap **Start GestureNav**

The service now runs in the background. Use gestures in any app.

## Architecture

```
GestureNavApp (Compose UI)
    └── GestureAccessibilityService (foreground service + LifecycleOwner)
            ├── ProcessCameraProvider (CameraX, front camera)
            ├── HandTracker (MediaPipe LIVE_STREAM mode)
            │       └── GestureClassifier (landmark → GestureEvent)
            └── dispatchGesture() / performGlobalAction()
```

## Tuning sensitivity

In `GestureClassifier.kt`:
- `SWIPE_Y_THRESHOLD` / `SWIPE_X_THRESHOLD` — how far the wrist must move to trigger scroll/swipe
- `FIST_CURL_THRESHOLD` — how curled fingers must be to count as a fist

In `HandTracker.kt`:
- `gestureCooldownMs` — minimum time between consecutive gesture triggers (default 400ms)

## Requirements

- Android 8.0+ (API 26)
- Front-facing camera
- Accessibility service permission
- Camera permission
