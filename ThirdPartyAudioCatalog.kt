package com.example.backgroundaudio.integration

/**
 * Integration layer (design stage): documents, per third-party app, what a normal non-rooted
 * Android app can and cannot do. Nothing here bypasses anything; there is deliberately no
 * capture/keep-alive code yet. See INTEGRATION.md.
 *
 * Four separate problems:
 *  1. Keep OUR app's audio alive in background  -> solved (PlaybackService).
 *  2. Keep ANOTHER app's process alive           -> not possible; only the user (battery settings)
 *                                                   or that app's own foreground service can do it.
 *  3. Capture ANOTHER app's audio                -> AudioPlaybackCapture (API 29+, MediaProjection
 *                                                   user consent, per-app opt-out). Not implemented yet.
 *  4. Make another app keep PLAYING after screen-off -> only that app decides; we cannot force it.
 */
data class ThirdPartyApp(
    val name: String,
    val packageName: String,
    val verdict: String
)

object ThirdPartyAudioCatalog {

    val apps = listOf(
        ThirdPartyApp(
            "YouTube", "com.google.android.youtube",
            "Background play is controlled by YouTube itself (a Premium feature, with the user's " +
                "settings). We cannot force it. Capturing its audio is technically possible only if " +
                "YouTube allows capture, and it would not keep the video app playing after screen-off."
        ),
        ThirdPartyApp(
            "Chrome", "com.android.chrome",
            "Chrome decides per site whether media continues in background; some sites keep playing " +
                "with a media notification, some (e.g. YouTube without Premium) pause. We cannot override it."
        ),
        ThirdPartyApp(
            "Instagram", "com.instagram.android",
            "No known official background-audio feature. Video generally pauses when the app leaves the " +
                "foreground, so there is nothing to continue or capture."
        ),
        ThirdPartyApp(
            "X", "com.twitter.android",
            "Video generally pauses in background; the app's own audio features (e.g. Spaces) are the " +
                "app's choice. We cannot force playback."
        )
    )

    fun describeAll(): String = buildString {
        appendLine("What a normal (non-root) app can and cannot do:")
        appendLine()
        appendLine("• Our own audio in background/screen-off: YES (this app).")
        appendLine("• Keep another app's process alive: NO.")
        appendLine("• Capture another app's audio: only via the official AudioPlaybackCapture API " +
            "with your explicit consent, and only if that app allows it. Not implemented yet.")
        appendLine("• Force another app to keep playing: NO.")
        appendLine()
        apps.forEach { appendLine("${it.name} (${it.packageName}):\n${it.verdict}\n") }
        append("Verify on your device; apps change behavior between versions.")
    }
}
