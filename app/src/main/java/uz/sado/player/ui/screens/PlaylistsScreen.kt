package uz.sado.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.sado.player.data.*
import uz.sado.player.playback.PlayerConnection
import uz.sado.player.ui.AppState
import uz.sado.player.ui.Detail
import uz.sado.player.ui.SmartKind
import uz.sado.player.ui.components.*
import uz.sado.player.ui.glass.GlassSurface
import uz.sado.player.ui.theme.Sado

@Composable
fun PlaylistsScreen(app: AppState, songs: List<Song>) {
    val playlists by UserStore.playlists.collectAsStateWithLifecycle()
    val favorites by UserStore.favorites.collectAsStateWithLifecycle()
    val counts by UserStore.playCounts.collectAsStateWithLifecycle()
    val lastPlayed by UserStore.lastPlayed.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }

    val favCount = remember(songs, favorites) { songs.count { it.id in favorites } }
    val newCount = remember(songs) { smartSongs(SmartKind.NewlyAdded, songs, emptySet(), emptyMap(), emptyMap()).size }
    val recentCount = remember(songs, lastPlayed) { smartSongs(SmartKind.Recent, songs, emptySet(), emptyMap(), lastPlayed).size }
    val topCount = remember(songs, counts) { smartSongs(SmartKind.MostPlayed, songs, emptySet(), counts, emptyMap()).size }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = ContentBottomPadding) {
        item {
            ScreenTitle("Pleylistlar") {
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(Sado.Accent).clickable { creating = true },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Add, "Yangi pleylist yaratish", tint = Sado.OnAccent) }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmartTile(
                        "Sevimlilar", "$favCount ta qo'shiq", Icons.Rounded.Favorite, highlighted = true,
                        modifier = Modifier.weight(1f),
                    ) { app.open(Detail.SmartD(SmartKind.Favorites)) }
                    SmartTile(
                        "Ko'p tinglangan", if (topCount > 0) "Top $topCount" else "Hali yo'q", Icons.AutoMirrored.Rounded.TrendingUp,
                        modifier = Modifier.weight(1f),
                    ) { app.open(Detail.SmartD(SmartKind.MostPlayed)) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmartTile(
                        "Yaqinda tinglangan", "So'nggi 7 kun · $recentCount", Icons.Rounded.History,
                        modifier = Modifier.weight(1f),
                    ) { app.open(Detail.SmartD(SmartKind.Recent)) }
                    SmartTile(
                        "Yangi qo'shilgan", "$newCount ta qo'shiq", Icons.Rounded.NewReleases,
                        modifier = Modifier.weight(1f),
                    ) { app.open(Detail.SmartD(SmartKind.NewlyAdded)) }
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Mening pleylistlarim", color = Sado.Text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Text("${playlists.size} ta", color = Sado.Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (playlists.isEmpty()) {
            item {
                EmptyState(
                    Icons.Rounded.LibraryAdd, "Hali pleylist yo'q",
                    "«+» tugmasi bilan yarating yoki qo'shiq ustidagi ⋮ → «Pleylistga qo'shish»ni tanlang.",
                )
            }
        }
        items(playlists, key = { it.id }) { p ->
            val list = remember(p.songIds, songs) { Library.songs(p.songIds) }
            val total = remember(list) { list.sumOf { it.durationMs } }
            ListRow(
                title = p.name,
                subtitle = "${list.size} ta qo'shiq · ${formatTotal(total)}",
                onClick = { app.open(Detail.PlaylistD(p.id)) },
                trailing = {
                    GlassIconButton(
                        Icons.Rounded.PlayArrow, "${p.name} pleylistini ijro etish",
                        onClick = { PlayerConnection.play(list, 0) }, size = 44.dp, iconSize = 20.dp,
                    )
                },
            ) {
                Mosaic(list, Modifier.size(56.dp))
            }
        }
    }

    if (creating) {
        NameDialog(
            "Yangi pleylist", "", "Yaratish",
            onConfirm = { name ->
                val p = UserStore.createPlaylist(name)
                creating = false
                app.open(Detail.PlaylistD(p.id))
            },
            onDismiss = { creating = false },
        )
    }
}

@Composable
private fun SmartTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    val fg = if (highlighted) Sado.OnAccent else Sado.Text
    val sub = if (highlighted) Sado.OnAccent.copy(alpha = 0.75f) else Sado.Muted
    val content: @Composable () -> Unit = {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(24.dp))
            Column {
                Text(title, color = fg, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Text(subtitle, color = sub, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
    if (highlighted) {
        Box(modifier.height(100.dp).clip(RoundedCornerShape(20.dp)).background(Sado.Accent).clickable(onClick = onClick)) { content() }
    } else {
        GlassSurface(
            backdrop = null, cornerRadius = 20.dp,
            modifier = modifier.height(100.dp).clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick),
        ) { content() }
    }
}

/** Pleylistdagi dastlabki 4 ta qo'shiq muqovasidan yig'ilgan mozaika. */
@Composable
fun Mosaic(songs: List<Song>, modifier: Modifier = Modifier) {
    val firsts = songs.distinctBy { it.albumId }.take(4)
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(Sado.Surface2)) {
        when {
            firsts.isEmpty() -> Icon(
                Icons.Rounded.MusicNote, null, tint = Sado.Muted,
                modifier = Modifier.align(Alignment.Center).size(24.dp),
            )
            firsts.size < 4 -> Artwork(firsts.first(), Modifier.fillMaxSize(), cornerRadius = 0.dp, sizePx = 200)
            else -> Column(Modifier.fillMaxSize()) {
                Row(Modifier.weight(1f)) {
                    Artwork(firsts[0], Modifier.weight(1f).fillMaxHeight(), cornerRadius = 0.dp, sizePx = 120)
                    Artwork(firsts[1], Modifier.weight(1f).fillMaxHeight(), cornerRadius = 0.dp, sizePx = 120)
                }
                Row(Modifier.weight(1f)) {
                    Artwork(firsts[2], Modifier.weight(1f).fillMaxHeight(), cornerRadius = 0.dp, sizePx = 120)
                    Artwork(firsts[3], Modifier.weight(1f).fillMaxHeight(), cornerRadius = 0.dp, sizePx = 120)
                }
            }
        }
    }
}

@Suppress("unused")
private val transparent = Color.Transparent
