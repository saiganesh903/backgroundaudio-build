package com.example.backgroundaudio

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.example.backgroundaudio.integration.ThirdPartyAudioCatalog
import com.google.common.util.concurrent.ListenableFuture

class MainActivity : AppCompatActivity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private lateinit var statusText: TextView
    private lateinit var hintText: TextView
    private lateinit var startButton: Button
    private lateinit var pauseButton: Button
    private lateinit var stopButton: Button

    private val prefs by lazy { getSharedPreferences("backgroundaudio", MODE_PRIVATE) }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = updateUi()
    }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, R.string.notification_denied, Toast.LENGTH_LONG).show()
            }
            // Playback itself does not depend on the notification permission; start either way.
            startPlayback()
        }

    private val pickAudio =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) return@registerForActivityResult
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
            }
            prefs.edit().putString(KEY_URI, uri.toString()).apply()
            val c = controller
            if (c != null && c.mediaItemCount > 0) {
                val wasPlaying = c.playWhenReady
                c.setMediaItem(buildMediaItem())
                c.prepare()
                if (wasPlaying) c.play()
            }
            Toast.makeText(this, R.string.file_selected, Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        hintText = findViewById(R.id.hintText)
        startButton = findViewById(R.id.startButton)
        pauseButton = findViewById(R.id.pauseButton)
        stopButton = findViewById(R.id.stopButton)

        startButton.setOnClickListener { onStartClicked() }
        pauseButton.setOnClickListener { controller?.pause() }
        stopButton.setOnClickListener {
            controller?.sendCustomCommand(SessionCommand(PlaybackService.ACTION_STOP, Bundle.EMPTY), Bundle.EMPTY)
        }
        findViewById<Button>(R.id.chooseButton).setOnClickListener { pickAudio.launch(arrayOf("audio/*")) }
        findViewById<Button>(R.id.infoButton).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.third_party_title)
                .setMessage(ThirdPartyAudioCatalog.describeAll())
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
        updateUi()
    }

    override fun onStart() {
        super.onStart()
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                val c = future.get()
                controller = c
                c.addListener(playerListener)
                updateUi()
            } catch (e: Exception) {
                statusText.text = getString(R.string.status_connect_failed)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onStop() {
        // Releasing the controller does NOT stop playback: the service owns the player.
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
        super.onStop()
    }

    private fun onStartClicked() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        val c = controller
        if (c == null) {
            Toast.makeText(this, R.string.status_connecting, Toast.LENGTH_SHORT).show()
            return
        }
        if (c.mediaItemCount == 0) {
            c.setMediaItem(buildMediaItem())
            c.prepare()
        }
        c.play()
    }

    private fun buildMediaItem(): MediaItem {
        val saved = prefs.getString(KEY_URI, null)
        val uri: Uri
        val title: String
        if (saved != null) {
            uri = saved.toUri()
            title = getString(R.string.track_selected)
        } else {
            uri = Uri.fromFile(ToneGenerator.ensureToneFile(this))
            title = getString(R.string.track_tone)
        }
        return MediaItem.Builder()
            .setMediaId("main")
            .setUri(uri)
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(getString(R.string.app_name))
                    .build()
            )
            .build()
    }

    private fun updateUi() {
        val c = controller
        val green = 0xFF2E7D32.toInt()
        val orange = 0xFFEF6C00.toInt()
        val grey = 0xFF757575.toInt()

        if (c == null) {
            statusText.text = getString(R.string.status_connecting)
            statusText.setTextColor(grey)
            hintText.text = ""
            startButton.isEnabled = false
            pauseButton.isEnabled = false
            stopButton.isEnabled = false
            return
        }

        val active = c.mediaItemCount > 0 && c.playbackState != Player.STATE_IDLE
        when {
            !active -> {
                statusText.text = getString(R.string.status_stopped)
                statusText.setTextColor(grey)
                hintText.text = getString(R.string.hint_stopped)
            }
            c.isPlaying -> {
                statusText.text = getString(R.string.status_playing)
                statusText.setTextColor(green)
                hintText.text = getString(R.string.hint_playing)
            }
            c.playWhenReady && c.playbackState == Player.STATE_BUFFERING -> {
                statusText.text = getString(R.string.status_buffering)
                statusText.setTextColor(orange)
                hintText.text = ""
            }
            else -> {
                statusText.text = getString(R.string.status_paused)
                statusText.setTextColor(orange)
                hintText.text = getString(R.string.hint_paused)
            }
        }
        startButton.isEnabled = !c.isPlaying
        pauseButton.isEnabled = c.isPlaying
        stopButton.isEnabled = active
    }

    companion object {
        private const val KEY_URI = "selected_audio_uri"
    }
}
