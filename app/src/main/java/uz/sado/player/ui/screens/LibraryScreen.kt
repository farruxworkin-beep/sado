package uz.sado.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import uz.sado.player.ui.Tab
import uz.sado.player.ui.components.*
import uz.sado.player.ui.glass.GlassSurface
import uz.sado.player.ui.theme.Sado

private val libraryTabs = listOf("Qo'shiqlar", "Albomlar", "Ijrochilar", "Papkalar")
val sortLabels = listOf("Sana bo'yicha", "Nomi bo'yicha", "Ijrochi bo'yicha", "Davomiylik")

fun sortSongs(songs: List<Song>, mode: Int): List<Song> = when (mode) {
    1 -> songs.sortedBy { it.title.lowercase() }
    2 -> songs.sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))
    3 -> songs.sortedByDescending { it.durationMs }
    else -> songs.sortedByDescending { it.dateAdded }
}

@Composable
fun LibraryScreen(
    app: AppState,
    songs: List<Song>,
    loaded: Boolean,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    playback: PlaybackState,
) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Kutubxona") {
            GlassIconButton(Icons.Rounded.Search, "Qidirish", onClick = { app.selectTab(Tab.Search) })
            GlassIconButton(Icons.Rounded.Refresh, "Qayta skanerlash", onClick = {
                Library.refresh(); context.toast("Musiqalar qayta skanerlanmoqda")
            })
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            libraryTabs.forEachIndexed { i, label ->
                GlassChip(label, selected = app.libraryTab == i, onClick = { app.libraryTab = i })
            }
        }

        when {
            !hasPermission -> EmptyState(
                Icons.Rounded.MusicNote,
                "Musiqaga ruxsat kerak",
                "Telefoningizdagi qo'shiqlarni ko'rsatish uchun musiqa fayllariga ruxsat bering.",
            ) { AccentButton("Ruxsat berish", null, onRequestPermission) }

            !loaded -> Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Sado.Accent)
            }

            songs.isEmpty() -> EmptyState(
                Icons.Rounded.MusicOff,
                "Qo'shiq topilmadi",
                "Telefonga musiqa fayllarini (MP3, FLAC, M4A, OGG…) yuklang — ular shu yerda avtomatik paydo bo'ladi.",
            )

            else -> when (app.libraryTab) {
                0 -> SongsTab(app, songs, playback)
                1 -> AlbumsTab(app, songs)
                2 -> ArtistsTab(app, songs)
                else -> FoldersTab(app, songs)
            }
        }
    }
}

@Composable
private fun SongsTab(app: AppState, songs: List<Song>, playback: PlaybackState) {
    val sortMode by UserStore.sortMode.collectAsStateWithLifecycle()
    val sorted = remember(songs, sortMode) { sortSongs(songs, sortMode) }
    val total = remember(songs) { songs.sumOf { it.durationMs } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = ContentBottomPadding) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AccentButton("Aralashtirib ijro", Icons.Rounded.Shuffle, onClick = { PlayerConnection.play(sorted, shuffle = true) })
                Row(
                    Modifier.height(44.dp).clip(RoundedCornerShape(22.dp))
                        .clickable { UserStore.setSortMode((sortMode + 1) % sortLabels.size) }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(sortLabels[sortMode.coerceIn(0, sortLabels.lastIndex)], color = Sado.Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Rounded.Sort, "Saralash", tint = Sado.Muted, modifier = Modifier.size(18.dp))
                }
            }
        }
        item {
            SectionLabel(
                "${songs.size} ta qo'shiq · ${formatTotal(total)}",
                Modifier.padding(start = 20.dp, top = 8.dp, bottom = 6.dp),
            )
        }
        itemsIndexed(sorted, key = { _, s -> s.id }) { i, s ->
            SongRow(
                song = s,
                isCurrent = s.id == playback.currentId,
                isPlaying = playback.isPlaying,
                onClick = { PlayerConnection.play(sorted, i) },
                onMore = { app.actionTarget = ActionTarget(s) },
            )
        }
    }
}

@Composable
private fun AlbumsTab(app: AppState, songs: List<Song>) {
    val albums = remember(songs) { songs.toAlbums() }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 176.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(albums, key = { it.id }) { a ->
            Column(
                Modifier.clip(RoundedCornerShape(16.dp)).clickable { app.open(Detail.AlbumD(a.id)) }
            ) {
                Artwork(a.songs.first(), Modifier.fillMaxWidth().aspectRatio(1f), cornerRadius = 16.dp, sizePx = 400)
                Spacer(Modifier.height(8.dp))
                Text(a.title, color = Sado.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${a.artist} · ${a.songs.size} ta", color = Sado.Muted, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ArtistsTab(app: AppState, songs: List<Song>) {
    val artists = remember(songs) { songs.toArtists() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = ContentBottomPadding) {
        items(artists, key = { it.name }) { a ->
            ListRow(
                title = a.name,
                subtitle = "${a.albumCount} ta albom · ${a.songs.size} ta qo'shiq",
                onClick = { app.open(Detail.ArtistD(a.name)) },
            ) {
                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(Sado.colorFor(a.name.hashCode().toLong())),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        a.name.firstOrNull()?.uppercase() ?: "?",
                        color = Sado.Bg, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun FoldersTab(app: AppState, songs: List<Song>) {
    val folders = remember(songs) { songs.toFolders() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = ContentBottomPadding) {
        items(folders, key = { it.path }) { f ->
            ListRow(
                title = f.name,
                subtitle = "${f.songs.size} ta qo'shiq · ${f.path.removePrefix("/storage/emulated/0")}",
                onClick = { app.open(Detail.FolderD(f.path)) },
            ) {
                GlassSurface(backdrop = null, cornerRadius = 14.dp, modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Folder, null, tint = Sado.Accent)
                }
            }
        }
    }
}

@Composable
fun ListRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    leading: @Composable () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Sado.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Sado.Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) trailing() else Icon(Icons.Rounded.ChevronRight, null, tint = Sado.Muted)
    }
}
