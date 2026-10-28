package uz.sado.player.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore

/** Telefondagi musiqa fayllarini MediaStore orqali o'qiydi. */
object MediaScanner {

    @Suppress("DEPRECATION")
    fun scan(context: Context): List<Song> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val args = arrayOf("15000") // 15 soniyadan qisqa ovozlar (qo'ng'iroq, bildirishnoma) tashlab ketiladi

        val result = ArrayList<Song>()
        try {
            context.contentResolver.query(collection, projection, selection, args, null)?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val path = c.getString(dataCol) ?: ""
                    val folderPath = path.substringBeforeLast('/', "")
                    val folderName = folderPath.substringAfterLast('/').ifBlank { "Ichki xotira" }
                    val rawArtist = c.getString(artistCol)
                    val rawAlbum = c.getString(albumCol)
                    result += Song(
                        id = id,
                        title = c.getString(titleCol)?.takeIf { it.isNotBlank() }
                            ?: path.substringAfterLast('/').substringBeforeLast('.'),
                        artist = if (rawArtist.isNullOrBlank() || rawArtist == "<unknown>") UNKNOWN_ARTIST else rawArtist,
                        album = if (rawAlbum.isNullOrBlank() || rawAlbum == "<unknown>") UNKNOWN_ALBUM else rawAlbum,
                        albumId = c.getLong(albumIdCol),
                        durationMs = c.getLong(durCol),
                        folderPath = folderPath,
                        folderName = folderName,
                        dateAdded = c.getLong(dateCol),
                        uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                    )
                }
            }
        } catch (_: SecurityException) {
            // Ruxsat berilmagan — bo'sh ro'yxat qaytadi
        }
        return result
    }
}
