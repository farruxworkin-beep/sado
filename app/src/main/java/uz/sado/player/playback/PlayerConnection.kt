package uz.sado.player.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import uz.sado.player.data.Song
import uz.sado.player.data.toMediaItem

data class QueueEntry(val index: Int, val songId: Long)

data class PlaybackState(
    val connected: Boolean = false,
    val currentId: Long? = null,
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val durationMs: Long = 0,
    val itemCount: Int = 0,
    val upcoming: List<QueueEntry> = emptyList(),
    val title: String = "",
    val artist: String = "",
)

/** UI va ijro xizmati o'rtasidagi ko'prik (MediaController). */
object PlayerConnection {
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state

    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = refresh()
    }

    fun connect(context: Context) {
        if (future != null) return
        val app = context.applicationContext
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val f = MediaController.Builder(app, token).buildAsync()
        future = f
        f.addListener({
            try {
                val c = f.get()
                controller = c
                c.addListener(listener)
                refresh()
            } catch (_: Exception) {
                future = null
            }
        }, ContextCompat.getMainExecutor(app))
    }

    fun disconnect() {
        controller?.removeListener(listener)
        future?.let { MediaController.releaseFuture(it) }
        future = null
        controller = null
        _state.value = _state.value.copy(connected = false)
    }

    val positionMs: Long get() = controller?.currentPosition ?: 0L

    private fun refresh() {
        val c = controller ?: return
        val tl = c.currentTimeline
        val upcoming = ArrayList<QueueEntry>()
        if (!tl.isEmpty && c.currentMediaItemIndex in 0 until tl.windowCount) {
            var i = tl.getNextWindowIndex(c.currentMediaItemIndex, Player.REPEAT_MODE_OFF, c.shuffleModeEnabled)
            while (i != C.INDEX_UNSET && upcoming.size < 1000) {
                c.getMediaItemAt(i).mediaId.toLongOrNull()?.let { upcoming += QueueEntry(i, it) }
                i = tl.getNextWindowIndex(i, Player.REPEAT_MODE_OFF, c.shuffleModeEnabled)
            }
        }
        val item = c.currentMediaItem
        _state.value = PlaybackState(
            connected = true,
            currentId = item?.mediaId?.toLongOrNull(),
            currentIndex = c.currentMediaItemIndex,
            isPlaying = c.isPlaying,
            shuffle = c.shuffleModeEnabled,
            repeatMode = c.repeatMode,
            durationMs = c.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L,
            itemCount = c.mediaItemCount,
            upcoming = upcoming,
            title = item?.mediaMetadata?.title?.toString() ?: "",
            artist = item?.mediaMetadata?.artist?.toString() ?: "",
        )
    }

    // ---------------- Amallar ----------------

    fun play(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = false) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        val start = if (shuffle) songs.indices.random() else startIndex.coerceIn(0, songs.lastIndex)
        c.setMediaItems(songs.map { it.toMediaItem() }, start, 0L)
        c.shuffleModeEnabled = shuffle
        c.prepare()
        c.play()
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            if (c.playbackState == Player.STATE_ENDED) c.seekToDefaultPosition()
            c.play()
        }
    }

    fun next() { controller?.seekToNext() }
    fun previous() { controller?.seekToPrevious() }
    fun seekTo(ms: Long) { controller?.seekTo(ms.coerceAtLeast(0L)) }

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun playNext(song: Song) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) return play(listOf(song))
        c.addMediaItem((c.currentMediaItemIndex + 1).coerceAtMost(c.mediaItemCount), song.toMediaItem())
    }

    fun addToQueue(song: Song) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) return play(listOf(song))
        c.addMediaItem(song.toMediaItem())
    }

    fun playAt(index: Int) {
        val c = controller ?: return
        if (index !in 0 until c.mediaItemCount) return
        c.seekTo(index, 0L)
        c.play()
    }

    fun removeAt(index: Int) {
        val c = controller ?: return
        if (index in 0 until c.mediaItemCount) c.removeMediaItem(index)
    }

    fun move(from: Int, to: Int) {
        val c = controller ?: return
        if (from in 0 until c.mediaItemCount && to in 0 until c.mediaItemCount) c.moveMediaItem(from, to)
    }

    /** Hozirgi qo'shiqdan boshqa hammasini navbatdan olib tashlaydi. */
    fun clearUpcoming() {
        val c = controller ?: return
        val cur = c.currentMediaItemIndex
        if (cur < 0) return
        if (cur + 1 < c.mediaItemCount) c.removeMediaItems(cur + 1, c.mediaItemCount)
        if (cur > 0) c.removeMediaItems(0, cur)
    }
}
