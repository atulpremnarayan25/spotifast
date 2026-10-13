package rocks.spotifast.media

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import rocks.spotifast.R
import rocks.spotifast.data.models.Track
import rocks.spotifast.native.LibrespotBridge

/**
 * AndroidX Media3 foreground service maintaining active audio playback,
 * system notifications, lockscreen transport controls, and Bluetooth/Auto events.
 */
class SpotifastMediaSessionService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private lateinit var audioFocusManager: AudioFocusManager

    companion object {
        const val CHANNEL_ID = "spotifast_playback_channel"
        private const val NOTIFICATION_ID = 1001

        var currentPlayingTrack: Track? = null
            private set
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        audioFocusManager = AudioFocusManager(
            context = this,
            onPause = {
                player.pause()
                LibrespotBridge.pause()
            },
            onResume = {
                player.play()
                LibrespotBridge.play()
            },
            onSetVolumeMultiplier = { multiplier ->
                player.volume = multiplier
                LibrespotBridge.setVolume((multiplier * 65535).toInt())
            }
        )

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    audioFocusManager.requestAudioFocus()
                    LibrespotBridge.play()
                } else {
                    LibrespotBridge.pause()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    audioFocusManager.abandonAudioFocus()
                }
            }
        })

        mediaSession = MediaSession.Builder(this, player).build()
    }

    fun playTrack(track: Track) {
        currentPlayingTrack = track
        LibrespotBridge.loadTrack(track.uri, track.durationMs)

        val metadata = MediaMetadata.Builder()
            .setTitle(track.name)
            .setArtist(track.artistNames)
            .setAlbumTitle(track.album?.name ?: "")
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId(track.uri)
            .setMediaMetadata(metadata)
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        audioFocusManager.abandonAudioFocus()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        LibrespotBridge.release()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Spotifast Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Spotify audio playback controls"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
