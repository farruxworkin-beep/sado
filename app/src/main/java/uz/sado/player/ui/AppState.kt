package uz.sado.player.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import uz.sado.player.data.Song

enum class Tab(val label: String) {
    Library("Kutubxona"),
    Search("Qidiruv"),
    Playlists("Pleylistlar"),
    Sound("Ovoz"),
}

enum class SmartKind(val title: String) {
    Favorites("Sevimlilar"),
    MostPlayed("Ko'p tinglangan"),
    Recent("Yaqinda tinglangan"),
    NewlyAdded("Yangi qo'shilgan"),
}

sealed interface Detail {
    data class AlbumD(val albumId: Long) : Detail
    data class ArtistD(val name: String) : Detail
    data class FolderD(val path: String) : Detail
    data class PlaylistD(val id: Long) : Detail
    data class SmartD(val kind: SmartKind) : Detail
}

data class ActionTarget(val song: Song, val playlistId: Long? = null)

/** Ilova navigatsiyasi va ochiq oynalar holati. */
@Stable
class AppState {
    var tab by mutableStateOf(Tab.Library)
    val details = mutableStateListOf<Detail>()
    var playerOpen by mutableStateOf(false)
    var queueOpen by mutableStateOf(false)
    var libraryTab by mutableIntStateOf(0)
    var actionTarget by mutableStateOf<ActionTarget?>(null)
    var addToPlaylistSong by mutableStateOf<Song?>(null)

    val canGoBack: Boolean
        get() = queueOpen || playerOpen || details.isNotEmpty() || tab != Tab.Library

    fun selectTab(t: Tab) {
        details.clear()
        tab = t
    }

    fun open(d: Detail) {
        queueOpen = false
        playerOpen = false
        details += d
    }

    fun back() {
        when {
            queueOpen -> queueOpen = false
            playerOpen -> playerOpen = false
            details.isNotEmpty() -> details.removeAt(details.lastIndex)
            tab != Tab.Library -> tab = Tab.Library
        }
    }
}
