package uz.sado.player.data

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val folderPath: String,
    val folderName: String,
    val dateAdded: Long,
    val uri: Uri,
)

data class Album(val id: Long, val title: String, val artist: String, val songs: List<Song>)
data class Artist(val name: String, val songs: List<Song>) {
    val albumCount: Int get() = songs.distinctBy { it.albumId }.size
}
data class Folder(val path: String, val name: String, val songs: List<Song>)

const val UNKNOWN_ARTIST = "Noma'lum ijrochi"
const val UNKNOWN_ALBUM = "Noma'lum albom"

fun albumArtUri(albumId: Long): Uri = Uri.parse("content://media/external/audio/albumart/$albumId")

fun List<Song>.toAlbums(): List<Album> =
    groupBy { it.albumId }
        .map { (id, list) ->
            val sorted = list.sortedBy { it.title.lowercase() }
            val artists = list.map { it.artist }.distinct()
            Album(id, list.first().album, if (artists.size == 1) artists.first() else "Turli ijrochilar", sorted)
        }
        .sortedBy { it.title.lowercase() }

fun List<Song>.toArtists(): List<Artist> =
    groupBy { it.artist }
        .map { (name, list) -> Artist(name, list.sortedWith(compareBy({ it.album.lowercase() }, { it.title.lowercase() }))) }
        .sortedBy { it.name.lowercase() }

fun List<Song>.toFolders(): List<Folder> =
    groupBy { it.folderPath }
        .map { (path, list) -> Folder(path, list.first().folderName, list.sortedBy { it.title.lowercase() }) }
        .sortedBy { it.name.lowercase() }

fun Song.toMediaItem(): MediaItem {
    val extras = Bundle().apply { putLong(EXTRA_ALBUM_ID, albumId) }
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(albumArtUri(albumId))
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setExtras(extras)
                .build()
        )
        .build()
}

const val EXTRA_ALBUM_ID = "sado.albumId"

fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

fun formatTotal(ms: Long): String {
    val minutes = ms / 60_000
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "$h soat $m daq"
        h > 0 -> "$h soat"
        else -> "$m daq"
    }
}
