# Third-party apps: what Android permits

Four different problems:

1. **Keep OUR app's audio playing in background / screen off.** Supported and implemented (foreground media service + MediaSession).
2. **Keep ANOTHER app's process alive.** Not possible for a normal app. Android's process manager, Doze and App Standby decide;
   only that app (via its own foreground service) or the user (battery settings) can change it. Killing/keeping other apps needs system privileges.
3. **Capture ANOTHER app's audio.** Official route: `AudioPlaybackCapture` (API 29+). Requires `RECORD_AUDIO`, a `MediaProjection`
   consent dialog every session, and a foreground service of type `mediaProjection`. Only audio with USAGE_MEDIA/GAME/UNKNOWN is capturable,
   and each app/stream can opt out (manifest `allowAudioPlaybackCapture`, `setAllowedCapturePolicy`; DRM content is excluded). The source keeps playing
   to the speaker too (capture does not mute it), so re-playing it would double the audio.
4. **Make ANOTHER app keep playing after screen-off.** Not possible. Whether YouTube/Chrome/Instagram/X pauses on screen-off is that app's decision.
   Capturing (3) cannot help if the app has already paused.

Per app (behavior can change between versions, verify on your phone):
- YouTube: background play is the app's own policy (Premium feature). No legitimate bypass. Its terms also forbid circumventing this.
- Chrome: depends on the website; many sites keep playing with a media notification, some pause. Not controllable by us.
- Instagram: no known official background audio; video pauses when not foreground.
- X: video pauses in background; Spaces-style audio is the app's own feature.

Possible next step (phase 2, only if you want it): a prototype that requests MediaProjection consent and tests whether each of these apps
allows capture at all, with clear in-app messages when it does not. I have not implemented any bypass and will not.
