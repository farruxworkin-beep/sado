package uz.sado.player.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import uz.sado.player.data.Song
import uz.sado.player.data.UserStore
import uz.sado.player.data.formatDuration
import uz.sado.player.playback.PlaybackState
import uz.sado.player.playback.PlayerConnection
import uz.sado.player.playback.SleepTimer
import uz.sado.player.ui.ActionTarget
import uz.sado.player.ui.AppState
import uz.sado.player.ui.Tab
import uz.sado.player.ui.components.*
import uz.sado.player.ui.glass.GlassSurface
import uz.sado.player.ui.glass.backdropSource
import uz.sado.player.ui.glass.rememberBackdrop
import uz.sado.player.ui.theme.Sado

@Composable
fun PlayerScreen(app: AppState, playback: PlaybackState, song: Song?) {
    val context = LocalContext.current
    val backdrop = rememberBackdrop()
    val favorites by UserStore.favorites.collectAsStateWithLifecycle()
    val timerEnd by SleepTimer.endAt.collectAsStateWithLifecycle()
    val position by rememberPlaybackPosition(playback.isPlaying, playback.currentId)
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    var dragY by remember { mutableFloatStateOf(0f) }

    val duration = playback.durationMs.takeIf { it > 0 } ?: song?.durationMs ?: 0L
    val shownPos = dragFraction?.let { (it * duration).toLong() } ?: position
    val fraction = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val isFav = song != null && song.id in favorites

    val timerText by produceState(initialValue = "Taymer", timerEnd) {
        while (true) {
            val end = timerEnd
            value = if (end == null) "Taymer" else {
                val left = ((end - SystemClock.elapsedRealtime()) / 60_000L) + 1
                "$left daq"
            }
            if (end == null) break
            delay(15_000)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { translationY = dragY.coerceAtLeast(0f) }
            .background(Sado.Bg)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragY > 160.dp.toPx()) app.playerOpen = false
                        dragY = 0f
                    },
                    onDragCancel = { dragY = 0f },
                ) { change, dy ->
                    change.consume()
                    dragY = (dragY + dy).coerceAtLeast(0f)
                }
            }
    ) {
        // ---------- Fon: muqova butun ekran bo'ylab ----------
        Box(Modifier.fillMaxSize().backdropSource(backdrop)) {
            Artwork(song, Modifier.fillMaxSize(), cornerRadius = 0.dp, sizePx = 1080)
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.25f),
                        0.2f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.6f),
                    )
                )
            )
        }

        // ---------- Yuqori panel ----------
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(
                Icons.Rounded.KeyboardArrowDown, "Yopish", onClick = { app.playerOpen = false },
                backdrop = backdrop, size = 52.dp, iconSize = 28.dp,
            )
            Spacer(Modifier.weight(1f))
            GlassSurface(backdrop = backdrop, cornerRadius = 26.dp, refraction = 14.dp, bezel = 14.dp, modifier = Modifier.height(52.dp)) {
                Row(Modifier.fillMaxHeight().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    PillButton(
                        if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        if (isFav) "Sevimlilardan olib tashlash" else "Sevimlilarga qo'shish",
                        tint = if (isFav) Sado.Accent else Sado.Text,
                    ) { song?.let { UserStore.toggleFavorite(it.id) } }
                    PillButton(Icons.AutoMirrored.Rounded.QueueMusic, "Navbat") { app.queueOpen = true }
                    PillButton(Icons.Rounded.IosShare, "Ulashish") { song?.let { shareSong(context, it) } }
                    PillButton(Icons.Rounded.MoreHoriz, "Qo'shimcha amallar") { song?.let { app.actionTarget = ActionTarget(it) } }
                }
            }
        }

        // ---------- Pastki shisha panel ----------
        GlassSurface(
            backdrop = backdrop,
            cornerRadius = 34.dp,
            tint = Color(0x800E0F12),
            refraction = 26.dp,
            bezel = 24.dp,
            blur = 18.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(12.dp)
                .fillMaxWidth(),
        ) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp)) {
                Text(
                    (song?.album ?: "").uppercase(), color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    song?.title ?: playback.title, color = Sado.Text, fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    song?.artist ?: playback.artist, color = Color.White.copy(alpha = 0.75f), fontSize = 15.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(14.dp))
                SeekBar(
                    fraction = fraction,
                    onSeek = { PlayerConnection.seekTo((it * duration).toLong()) },
                    onDragFraction = { dragFraction = it },
                )
                Row(Modifier.fillMaxWidth()) {
                    Text(formatDuration(shownPos), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("−" + formatDuration((duration - shownPos).coerceAtLeast(0)), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RoundIcon(
                        Icons.Rounded.Shuffle, "Aralashtirish",
                        tint = if (playback.shuffle) Sado.Accent else Color.White.copy(alpha = 0.7f),
                    ) { PlayerConnection.toggleShuffle() }
                    RoundIcon(Icons.Rounded.SkipPrevious, "Oldingi qo'shiq", size = 60.dp, iconSize = 36.dp) { PlayerConnection.previous() }
                    GlassSurface(
                        backdrop = null,
                        cornerRadius = 40.dp,
                        tint = Color.White.copy(alpha = 0.16f),
                        rimAlpha = 0.7f,
                        modifier = Modifier.size(80.dp).clip(CircleShape).clickable { PlayerConnection.togglePlay() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            if (playback.isPlaying) "Pauza" else "Ijro etish",
                            tint = Sado.Text, modifier = Modifier.size(40.dp),
                        )
                    }
                    RoundIcon(Icons.Rounded.SkipNext, "Keyingi qo'shiq", size = 60.dp, iconSize = 36.dp) { PlayerConnection.next() }
                    RoundIcon(
                        if (playback.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        when (playback.repeatMode) {
                            Player.REPEAT_MODE_ALL -> "Hammasini takrorlash"
                            Player.REPEAT_MODE_ONE -> "Bittasini takrorlash"
                            else -> "Takrorlash o'chiq"
                        },
                        tint = if (playback.repeatMode != Player.REPEAT_MODE_OFF) Sado.Accent else Color.White.copy(alpha = 0.7f),
                    ) { PlayerConnection.cycleRepeat() }
                }
                Text(
                    modeText(playback), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.12f)))
                Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    UtilButton(Icons.Rounded.Equalizer, "Ekvalayzer", Modifier.weight(1f)) {
                        app.playerOpen = false; app.selectTab(Tab.Sound)
                    }
                    UtilButton(
                        Icons.Rounded.Bedtime, timerText, Modifier.weight(1f),
                        tint = if (timerEnd != null) Sado.Accent else Sado.Text,
                    ) {
                        SleepTimer.cycle()
                        val m = SleepTimer.options[SleepTimer.option.value]
                        context.toast(if (m == 0) "Uyqu taymeri o'chirildi" else "$m daqiqadan keyin to'xtaydi")
                    }
                    UtilButton(Icons.AutoMirrored.Rounded.QueueMusic, "Navbat", Modifier.weight(1f)) { app.queueOpen = true }
                    UtilButton(Icons.Rounded.Share, "Ulashish", Modifier.weight(1f)) { song?.let { shareSong(context, it) } }
                }
            }
        }
    }
}

private fun modeText(p: PlaybackState): String {
    val parts = mutableListOf<String>()
    if (p.shuffle) parts += "Aralash"
    when (p.repeatMode) {
        Player.REPEAT_MODE_ALL -> parts += "Hammasini takrorlash"
        Player.REPEAT_MODE_ONE -> parts += "Bittasini takrorlash"
    }
    return if (parts.isEmpty()) "Ketma-ket ijro" else parts.joinToString(" · ")
}

@Composable
private fun PillButton(icon: ImageVector, desc: String, tint: Color = Sado.Text, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun RoundIcon(
    icon: ImageVector,
    desc: String,
    tint: Color = Sado.Text,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    onClick: () -> Unit,
) {
    Box(Modifier.size(size).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
private fun UtilButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, tint: Color = Sado.Text, onClick: () -> Unit) {
    Column(
        modifier.height(56.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = tint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
