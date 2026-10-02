package com.example.backgroundaudio

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Foreground media-playback service. It owns the ExoPlayer and the MediaSession, so audio keeps
 * playing when the activity is gone and when the screen is off. Media3 automatically:
 *  - promotes this service to a foreground service (type mediaPlayback) while playing,
 *  - posts the media notification (also used for lock-screen / quick-settings controls),
 *  - routes headset / Bluetooth media buttons to the player.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true) // pause when headphones are unplugged
            .setWakeMode(C.WAKE_MODE_LOCAL)    // CPU wake lock only while playing local audio
            .build()
            .apply { repeatMode = Player.REPEAT_MODE_ALL }

        val stopButton = CommandButton.Builder()
            .setDisplayName(getString(R.string.action_stop))
            .setIconResId(R.drawable.ic_stop)
            .setSessionCommand(SessionCommand(ACTION_STOP, Bundle.EMPTY))
            .build()

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(SessionCallback())
            .setCustomLayout(ImmutableList.of(stopButton))
            .setSessionActivity(openAppIntent)
            .build()

        val provider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelName(R.string.notification_channel_name)
            .build()
        provider.setSmallIcon(R.drawable.ic_notification)
        setMediaNotificationProvider(provider)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    /** User swiped the app away from Recents: keep playing if playing, otherwise shut down. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private inner class SessionCallback : MediaSession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(ACTION_STOP, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands)
                .build()
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            // Controllers send items with the URI in requestMetadata; restore it for the player.
            val resolved = mediaItems.map { item ->
                item.buildUpon().setUri(item.requestMetadata.mediaUri ?: item.localConfiguration?.uri).build()
            }.toMutableList()
            return Futures.immediateFuture(resolved)
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == ACTION_STOP) {
                session.player.stop()
                session.player.clearMediaItems()
                stopSelf()
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    companion object {
        const val ACTION_STOP = "com.example.backgroundaudio.STOP"
    }
}
