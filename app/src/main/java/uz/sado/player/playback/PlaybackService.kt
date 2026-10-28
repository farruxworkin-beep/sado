package uz.sado.player.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import uz.sado.player.MainActivity
import uz.sado.player.data.EXTRA_ALBUM_ID
import uz.sado.player.data.SavedQueue
import uz.sado.player.data.SavedQueueItem
import uz.sado.player.data.UserStore
import uz.sado.player.data.albumArtUri
import android.content.ContentUris
import android.os.Bundle
import android.provider.MediaStore

/**
 * Fon rejimida ijro etuvchi xizmat. Bildirishnoma, qulf ekrani va
 * quloqchin tugmalari Media3 MediaSession orqali avtomatik ishlaydi.
 */
class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null
    private val handler = Handler(Looper.getMainLooper())
    private val saveRunnable = Runnable { saveQueue() }

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true) // quloqchin uzilsa pauza
            .build()

        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        session = MediaSession.Builder(this, player)
            .setSessionActivity(openApp)
            .build()

        AudioFx.attach(player.audioSessionId)
        SleepTimer.pauseAction = { player.pause() }

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.mediaId?.toLongOrNull()?.let { UserStore.recordPlay(it) }
                scheduleSave()
            }

            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) = scheduleSave()

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) scheduleSave()
            }
        })

        restoreQueue(player)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        saveQueue()
        handler.removeCallbacksAndMessages(null)
        SleepTimer.pauseAction = null
        AudioFx.release()
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    private fun scheduleSave() {
        handler.removeCallbacks(saveRunnable)
        handler.postDelayed(saveRunnable, 800)
    }

    private fun saveQueue() {
        val p = session?.player ?: return
        val items = (0 until p.mediaItemCount).mapNotNull { i ->
            val item = p.getMediaItemAt(i)
            val id = item.mediaId.toLongOrNull() ?: return@mapNotNull null
            val md = item.mediaMetadata
            SavedQueueItem(
                id = id,
                title = md.title?.toString() ?: "",
                artist = md.artist?.toString() ?: "",
                album = md.albumTitle?.toString() ?: "",
                albumId = md.extras?.getLong(EXTRA_ALBUM_ID) ?: 0L,
            )
        }
        UserStore.saveQueue(SavedQueue(items, p.currentMediaItemIndex.coerceAtLeast(0), p.currentPosition.coerceAtLeast(0L)))
    }

    private fun restoreQueue(player: ExoPlayer) {
        val saved = UserStore.loadQueue() ?: return
        if (saved.items.isEmpty()) return
        val items = saved.items.map { s ->
            MediaItem.Builder()
                .setMediaId(s.id.toString())
                .setUri(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, s.id))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(s.title)
                        .setArtist(s.artist)
                        .setAlbumTitle(s.album)
                        .setArtworkUri(albumArtUri(s.albumId))
                        .setExtras(Bundle().apply { putLong(EXTRA_ALBUM_ID, s.albumId) })
                        .build()
                )
                .build()
        }
        val index = saved.index.coerceIn(0, items.lastIndex)
        player.setMediaItems(items, index, saved.positionMs)
        player.prepare()
    }
}
