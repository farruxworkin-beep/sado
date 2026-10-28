package uz.sado.player.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.sado.player.data.Library
import uz.sado.player.data.Song
import uz.sado.player.playback.PlaybackState
import uz.sado.player.playback.PlayerConnection
import uz.sado.player.ui.components.AddToPlaylistDialog
import uz.sado.player.ui.components.AmbientBackground
import uz.sado.player.ui.components.Artwork
import uz.sado.player.ui.components.SongActionsSheet
import uz.sado.player.ui.components.rememberPlaybackPosition
import uz.sado.player.ui.glass.Backdrop
import uz.sado.player.ui.glass.GlassSurface
import uz.sado.player.ui.glass.backdropSource
import uz.sado.player.ui.glass.rememberBackdrop
import uz.sado.player.ui.screens.DetailScreen
import uz.sado.player.ui.screens.EqualizerScreen
import uz.sado.player.ui.screens.LibraryScreen
import uz.sado.player.ui.screens.PlayerScreen
import uz.sado.player.ui.screens.PlaylistsScreen
import uz.sado.player.ui.screens.QueueScreen
import uz.sado.player.ui.screens.SearchScreen
import uz.sado.player.ui.theme.Sado

@Composable
fun SadoRoot(hasPermission: Boolean, onRequestPermission: () -> Unit) {
    val app = remember { AppState() }
    val backdrop = rememberBackdrop()
    val songs by Library.songs.collectAsStateWithLifecycle()
    val loaded by Library.loaded.collectAsStateWithLifecycle()
    val playback by PlayerConnection.state.collectAsStateWithLifecycle()
    val current: Song? = remember(playback.currentId, songs) { playback.currentId?.let { Library.song(it) } }

    BackHandler(enabled = app.canGoBack) { app.back() }

    val ambient = when (app.tab) {
        Tab.Library -> listOf(Sado.Palette[0], Sado.Palette[3], Sado.Palette[1])
        Tab.Search -> listOf(Sado.Palette[7], Sado.Palette[4], Sado.Palette[3])
        Tab.Playlists -> listOf(Sado.Accent, Sado.Palette[5], Sado.Palette[6])
        Tab.Sound -> listOf(Sado.Accent, Sado.Palette[0], Sado.Palette[1])
    }

    Box(Modifier.fillMaxSize().background(Sado.Bg)) {
        // ---------- Asosiy kontent (shisha ortidagi fon) ----------
        Box(Modifier.fillMaxSize().backdropSource(backdrop)) {
            AmbientBackground(ambient)
            Box(Modifier.fillMaxSize().statusBarsPadding()) {
                val detail = app.details.lastOrNull()
                if (detail != null) {
                    DetailScreen(detail, app, songs, playback)
                } else when (app.tab) {
                    Tab.Library -> LibraryScreen(app, songs, loaded, hasPermission, onRequestPermission, playback)
                    Tab.Search -> SearchScreen(app, songs, playback)
                    Tab.Playlists -> PlaylistsScreen(app, songs)
                    Tab.Sound -> EqualizerScreen()
                }
            }
        }

        // ---------- Suzib turuvchi shisha panellar ----------
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
        ) {
            AnimatedVisibility(visible = playback.currentId != null) {
                Column {
                    MiniPlayer(backdrop, current, playback, onOpen = { app.playerOpen = true })
                    Spacer(Modifier.height(8.dp))
                }
            }
            BottomBar(backdrop, app.tab) { app.selectTab(it) }
        }

        // ---------- To'liq ekranli pleyer va navbat ----------
        AnimatedVisibility(
            visible = app.playerOpen,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            PlayerScreen(app, playback, current)
        }
        AnimatedVisibility(
            visible = app.queueOpen,
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { it },
        ) {
            QueueScreen(app, playback)
        }

        app.actionTarget?.let { SongActionsSheet(it, app) }
        app.addToPlaylistSong?.let { song -> AddToPlaylistDialog(song) { app.addToPlaylistSong = null } }
    }
}

@Composable
private fun MiniPlayer(backdrop: Backdrop, song: Song?, playback: PlaybackState, onOpen: () -> Unit) {
    val position by rememberPlaybackPosition(playback.isPlaying, playback.currentId)
    val duration = playback.durationMs.takeIf { it > 0 } ?: song?.durationMs ?: 0L
    val fraction = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f

    GlassSurface(
        backdrop = backdrop,
        cornerRadius = 24.dp,
        modifier = Modifier.fillMaxWidth().height(64.dp),
    ) {
        Row(
            Modifier.fillMaxSize().padding(start = 8.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(18.dp)).clickable(onClick = onOpen),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Artwork(song, Modifier.size(46.dp), cornerRadius = 10.dp, sizePx = 160)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        song?.title ?: playback.title, color = Sado.Text, fontSize = 14.sp,
                        fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        song?.artist ?: playback.artist, color = Sado.Muted, fontSize = 12.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Sado.Text)
                    .clickable { PlayerConnection.togglePlay() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    if (playback.isPlaying) "Pauza" else "Ijro etish",
                    tint = Sado.Bg,
                )
            }
            Box(
                Modifier.size(44.dp).clip(CircleShape).clickable { PlayerConnection.next() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.SkipNext, "Keyingi qo'shiq", tint = Sado.Text)
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 18.dp)
                .fillMaxWidth()
                .height(2.dp)
        ) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).background(Sado.Accent))
        }
    }
}

@Composable
private fun BottomBar(backdrop: Backdrop, selected: Tab, onSelect: (Tab) -> Unit) {
    GlassSurface(
        backdrop = backdrop,
        cornerRadius = 33.dp,
        modifier = Modifier.fillMaxWidth().height(66.dp),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tab.entries.forEach { t ->
                val isSel = t == selected
                val color = if (isSel) Sado.Accent else Sado.Muted
                Column(
                    Modifier
                        .height(54.dp)
                        .widthIn(min = 72.dp)
                        .clip(RoundedCornerShape(27.dp))
                        .background(if (isSel) Sado.Accent.copy(alpha = 0.14f) else Color.Transparent)
                        .clickable { onSelect(t) }
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(tabIcon(t), null, tint = color, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(3.dp))
                    Text(t.label, color = color, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun tabIcon(t: Tab): ImageVector = when (t) {
    Tab.Library -> Icons.Rounded.LibraryMusic
    Tab.Search -> Icons.Rounded.Search
    Tab.Playlists -> Icons.AutoMirrored.Rounded.QueueMusic
    Tab.Sound -> Icons.Rounded.Equalizer
}
