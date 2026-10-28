package uz.sado.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.sado.player.data.*
import uz.sado.player.playback.PlaybackState
import uz.sado.player.playback.PlayerConnection
import uz.sado.player.ui.ActionTarget
import uz.sado.player.ui.AppState
import uz.sado.player.ui.Detail
import uz.sado.player.ui.SmartKind
import uz.sado.player.ui.components.*
import uz.sado.player.ui.theme.Sado

private const val DAY = 24L * 60 * 60 * 1000

/** Aqlli ro'yxatlar: sevimlilar, ko'p tinglangan va h.k. */
fun smartSongs(
    kind: SmartKind,
    songs: List<Song>,
    favorites: Set<Long>,
    counts: Map<Long, Int>,
    lastPlayed: Map<Long, Long>,
): List<Song> {
    val now = System.currentTimeMillis()
    return when (kind) {
        SmartKind.Favorites -> songs.filter { it.id in favorites }.sortedBy { it.title.lowercase() }
        SmartKind.MostPlayed -> songs.filter { (counts[it.id] ?: 0) > 0 }
            .sortedByDescending { counts[it.id] ?: 0 }.take(50)
        SmartKind.Recent -> songs.filter { (lastPlayed[it.id] ?: 0L) > now - 7 * DAY }
            .sortedByDescending { lastPlayed[it.id] ?: 0L }
        SmartKind.NewlyAdded -> {
            val fresh = songs.filter { it.dateAdded * 1000 > now - 30 * DAY }
            (if (fresh.isNotEmpty()) fresh else songs).sortedByDescending { it.dateAdded }.take(100)
        }
    }
}

@Composable
fun DetailScreen(detail: Detail, app: AppState, songs: List<Song>, playback: PlaybackState) {
    val favorites by UserStore.favorites.collectAsStateWithLifecycle()
    val playlists by UserStore.playlists.collectAsStateWithLifecycle()
    val counts by UserStore.playCounts.collectAsStateWithLifecycle()
    val lastPlayed by UserStore.lastPlayed.collectAsStateWithLifecycle()

    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    val title: String
    val subtitle: String
    val list: List<Song>
    var playlistId: Long? = null
    var rowSubtitle: ((Song) -> String)? = null

    when (detail) {
        is Detail.AlbumD -> {
            list = songs.filter { it.albumId == detail.albumId }.sortedBy { it.title.lowercase() }
            title = list.firstOrNull()?.album ?: UNKNOWN_ALBUM
            val artists = list.map { it.artist }.distinct()
            subtitle = "Albom · ${if (artists.size == 1) artists.first() else "Turli ijrochilar"}"
        }
        is Detail.ArtistD -> {
            list = songs.filter { it.artist == detail.name }.sortedWith(compareBy({ it.album.lowercase() }, { it.title.lowercase() }))
            title = detail.name
            subtitle = "Ijrochi · ${list.distinctBy { it.albumId }.size} ta albom"
        }
        is Detail.FolderD -> {
            list = songs.filter { it.folderPath == detail.path }.sortedBy { it.title.lowercase() }
            title = list.firstOrNull()?.folderName ?: detail.path.substringAfterLast('/')
            subtitle = "Papka · ${detail.path.removePrefix("/storage/emulated/0")}"
        }
        is Detail.PlaylistD -> {
            val p = playlists.firstOrNull { it.id == detail.id }
            list = p?.let { Library.songs(it.songIds) } ?: emptyList()
            title = p?.name ?: "Pleylist"
            subtitle = "Pleylist"
            playlistId = p?.id
        }
        is Detail.SmartD -> {
            list = smartSongs(detail.kind, songs, favorites, counts, lastPlayed)
            title = detail.kind.title
            subtitle = "Avtomatik ro'yxat"
            if (detail.kind == SmartKind.MostPlayed) {
                rowSubtitle = { s -> "${s.artist} · ${counts[s.id] ?: 0} marta" }
            }
        }
    }
    val totalMs = remember(list) { list.sumOf { it.durationMs } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = ContentBottomPadding) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Orqaga", onClick = { app.back() })
                Spacer(Modifier.weight(1f))
                if (playlistId != null) {
                    GlassIconButton(Icons.Rounded.Edit, "Nomini o'zgartirish", onClick = { renaming = true })
                    Spacer(Modifier.width(8.dp))
                    GlassIconButton(Icons.Rounded.DeleteOutline, "Pleylistni o'chirish", onClick = { deleting = true })
                }
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (detail is Detail.ArtistD) {
                    Box(
                        Modifier.size(160.dp).clip(CircleShape).background(Sado.colorFor(detail.name.hashCode().toLong())),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(detail.name.firstOrNull()?.uppercase() ?: "?", color = Sado.Bg, fontSize = 64.sp, fontWeight = FontWeight.ExtraBold)
                    }
                } else {
                    Artwork(list.firstOrNull(), Modifier.size(180.dp), cornerRadius = 24.dp, sizePx = 600)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    title, color = Sado.Text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Text(subtitle, color = Sado.Muted, fontSize = 14.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${list.size} ta qo'shiq · ${formatTotal(totalMs)}", color = Sado.Muted, fontSize = 13.sp)
                Spacer(Modifier.height(14.dp))
                if (list.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        AccentButton("Ijro etish", Icons.Rounded.PlayArrow, onClick = { PlayerConnection.play(list, 0) })
                        GlassIconButton(Icons.Rounded.Shuffle, "Aralashtirib ijro", onClick = { PlayerConnection.play(list, shuffle = true) }, size = 44.dp)
                    }
                }
            }
        }
        if (list.isEmpty()) {
            item {
                EmptyState(
                    Icons.Rounded.MusicNote,
                    "Hozircha bo'sh",
                    when (detail) {
                        is Detail.PlaylistD -> "Qo'shiq ustidagi ⋮ → «Pleylistga qo'shish» orqali qo'shiq qo'shing."
                        is Detail.SmartD -> when (detail.kind) {
                            SmartKind.Favorites -> "Yoqqan qo'shiqlarni ♥ bilan belgilang."
                            else -> "Musiqa tinglaganingiz sari bu ro'yxat to'lib boradi."
                        }
                        else -> "Bu yerda qo'shiq yo'q."
                    },
                )
            }
        }
        itemsIndexed(list, key = { i, s -> "${s.id}-$i" }) { i, s ->
            val sub = rowSubtitle
            if (sub != null) {
                SongRow(
                    song = s, isCurrent = s.id == playback.currentId, isPlaying = playback.isPlaying,
                    onClick = { PlayerConnection.play(list, i) },
                    onMore = { app.actionTarget = ActionTarget(s, playlistId) },
                    subtitle = sub(s),
                )
            } else {
                SongRow(
                    song = s, isCurrent = s.id == playback.currentId, isPlaying = playback.isPlaying,
                    onClick = { PlayerConnection.play(list, i) },
                    onMore = { app.actionTarget = ActionTarget(s, playlistId) },
                )
            }
        }
    }

    val pid = playlistId
    if (renaming && pid != null) {
        NameDialog("Nomini o'zgartirish", title, "Saqlash", onConfirm = { UserStore.renamePlaylist(pid, it); renaming = false }, onDismiss = { renaming = false })
    }
    if (deleting && pid != null) {
        ConfirmDialog(
            "Pleylistni o'chirish", "«$title» o'chirilsinmi? Qo'shiqlar telefoningizda qoladi.", "O'chirish",
            onConfirm = { UserStore.deletePlaylist(pid); app.back() },
            onDismiss = { deleting = false },
        )
    }
}
