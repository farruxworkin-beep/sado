package uz.sado.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import uz.sado.player.data.Library
import uz.sado.player.data.formatDuration
import uz.sado.player.playback.PlaybackState
import uz.sado.player.playback.PlayerConnection
import uz.sado.player.playback.QueueEntry
import uz.sado.player.ui.AppState
import uz.sado.player.ui.components.*
import uz.sado.player.ui.glass.GlassSurface
import uz.sado.player.ui.theme.Sado

@Composable
fun QueueScreen(app: AppState, playback: PlaybackState) {
    val current = playback.currentId?.let { Library.song(it) }
    // Bir qo'shiq navbatda ikki marta bo'lishi mumkin — kalitlarni noyob qilamiz
    val keyed = remember(playback.upcoming) {
        val seen = HashMap<Long, Int>()
        playback.upcoming.map { e ->
            val n = (seen[e.songId] ?: 0) + 1
            seen[e.songId] = n
            "${e.songId}#$n" to e
        }
    }

    Box(Modifier.fillMaxSize().background(Sado.Bg)) {
        AmbientBackground(listOf(Sado.Palette[0], Sado.Palette[2], Sado.Palette[3]))
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Orqaga", onClick = { app.queueOpen = false })
                Spacer(Modifier.width(8.dp))
                Text("Navbat", color = Sado.Text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                if (keyed.isNotEmpty()) {
                    Text(
                        "Tozalash", color = Sado.Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { PlayerConnection.clearUpcoming() }.padding(12.dp),
                    )
                }
            }

            SectionLabel("Hozir ijro etilmoqda", Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp))
            GlassSurface(backdrop = null, cornerRadius = 22.dp, modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth()) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Artwork(current, Modifier.size(52.dp), cornerRadius = 12.dp, sizePx = 160)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(current?.title ?: playback.title, color = Sado.Accent, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(current?.artist ?: playback.artist, color = Sado.Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).clickable { PlayerConnection.togglePlay() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Sado.Text)
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel("Keyingi · ${keyed.size} ta", Modifier.weight(1f))
                Text(
                    if (playback.shuffle) "Aralash rejim" else "≡ ni surib tartiblang",
                    color = Sado.Muted, fontSize = 12.sp,
                )
            }

            if (keyed.isEmpty()) {
                EmptyState(
                    Icons.Rounded.MusicNote, "Navbat bo'sh",
                    "Qo'shiq ustidagi ⋮ → «Navbat oxiriga qo'shish»ni tanlang.",
                )
            }

            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
                items(keyed, key = { it.first }) { (_, entry) ->
                    QueueRow(entry, playback)
                }
            }
        }
    }
}

@Composable
private fun QueueRow(entry: QueueEntry, playback: PlaybackState) {
    val song = Library.song(entry.songId)
    val currentEntry by rememberUpdatedState(entry)
    val currentPlayback by rememberUpdatedState(playback)
    var drag by remember { mutableFloatStateOf(0f) }
    val canReorder = !playback.shuffle

    Row(
        Modifier
            .fillMaxWidth()
            .zIndex(if (drag != 0f) 1f else 0f)
            .graphicsLayer { translationY = drag }
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (drag != 0f) Color.White.copy(alpha = 0.10f) else Color.Transparent)
            .clickable { PlayerConnection.playAt(entry.index) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 40.dp, height = 48.dp)
                .then(
                    if (canReorder) Modifier.pointerInput(Unit) {
                        val step = 60.dp.toPx()
                        detectVerticalDragGestures(
                            onDragEnd = { drag = 0f },
                            onDragCancel = { drag = 0f },
                        ) { change, dy ->
                            change.consume()
                            drag += dy
                            val e = currentEntry
                            val p = currentPlayback
                            if (drag > step && e.index + 1 < p.itemCount) {
                                PlayerConnection.move(e.index, e.index + 1)
                                drag -= step
                            } else if (drag < -step && e.index - 1 > p.currentIndex) {
                                PlayerConnection.move(e.index, e.index - 1)
                                drag += step
                            }
                        }
                    } else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.DragHandle, "Tartiblash",
                tint = if (canReorder) Sado.Muted else Sado.Muted.copy(alpha = 0.3f),
            )
        }
        Artwork(song, Modifier.size(44.dp), cornerRadius = 10.dp, sizePx = 160)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song?.title ?: "—", color = Sado.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(song?.artist, song?.durationMs?.let { formatDuration(it) }).joinToString(" · "),
                color = Sado.Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            Modifier.size(44.dp).clip(CircleShape).clickable { PlayerConnection.removeAt(entry.index) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Close, "Navbatdan olib tashlash", tint = Sado.Muted, modifier = Modifier.size(20.dp))
        }
    }
}
