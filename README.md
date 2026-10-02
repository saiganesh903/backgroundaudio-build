# BackgroundAudio

Non-root Android app (Kotlin, AndroidX Media3) whose audio keeps playing with the screen off.
minSdk 26 (Android 8.0), compile/target SDK 35. No root, Magisk, ADB or system changes needed.

## How it works
- `PlaybackService` (a Media3 `MediaSessionService`, foreground service type `mediaPlayback`) owns the ExoPlayer and MediaSession.
- Audio focus + `USAGE_MEDIA` attributes, pause on headphone unplug, media notification with Play/Pause + Stop, lock-screen / Bluetooth / headset controls via the MediaSession.
- `MainActivity` is only a remote control (MediaController). Closing it does not stop audio.
- Default audio is a generated looping tone (no asset files). "Choose my own audio file" plays any local audio file.
- One wake lock: ExoPlayer's `WAKE_MODE_LOCAL` (needs `WAKE_LOCK`), held only while playing local audio. Remove `.setWakeMode(...)` and the permission if you want none.

## Build the APK (pick one)
A) Android Studio: File > Open this folder, let Gradle sync (it downloads Gradle 8.9 + SDK 35 if prompted),
   then Build > Build APK(s). Output: `app/build/outputs/apk/debug/app-debug.apk`.
B) Command line (JDK 17 + Android SDK installed): `gradle wrapper --gradle-version 8.9` once, then `./gradlew assembleDebug`.
C) No local setup: push this folder to a GitHub repo; the included workflow `.github/workflows/build-apk.yml`
   builds it (Actions tab > Build debug APK > download artifact `BackgroundAudio-debug-apk`).

## Install on your phone
1. Copy `app-debug.apk` to the phone (USB, cloud drive, email).
2. Open it in a file manager; when asked, allow "Install unknown apps" for that file manager/browser.
3. Install, open BackgroundAudio. (ADB is optional: `adb install app-debug.apk`; not needed to run the app.)

## Test
1. Open BackgroundAudio, tap START AUDIO, allow notifications (Android 13+).
2. Status shows "● PLAYING"; you hear a soft pulsing chord. Media volume must be up.
3. Press the power button: screen off, audio continues.
4. Wake the phone: lock screen shows the media controls; unlock: still playing.
5. Pause/play from the notification, then STOP (app or notification): audio ends, notification disappears.
Also try: swipe app out of Recents while playing (keeps playing), unplug headphones (pauses).

If it stops after a minute or two on Xiaomi/Huawei/Oppo/Vivo/Samsung etc., the manufacturer's battery manager is
killing it: set the app's battery usage to "Unrestricted" / allow auto-start. That is a user setting, not something an app can force.

See INTEGRATION.md for the third-party app analysis.
