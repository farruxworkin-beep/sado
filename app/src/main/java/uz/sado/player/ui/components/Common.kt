package uz.sado.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import uz.sado.player.data.Song
import uz.sado.player.data.formatDuration
import uz.sado.player.playback.PlayerConnection
import uz.sado.player.ui.glass.Backdrop
import uz.sado.player.ui.glass.GlassSurface
import uz.sado.player.ui.theme.Sado

/** Orqa fondagi yumshoq rangli dog'lar — shisha ular orqali "jonlanadi". */
@Composable
fun AmbientBackground(colors: List<Color>, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize().background(Sado.Bg)) {
        val spots = listOf(
            Offset(size.width * 0.05f, size.height * 0.05f) to 0.75f,
            Offset(size.width * 1.0f, size.height * 0.45f) to 0.7f,
            Offset(size.width * 0.2f, size.height * 0.95f) to 0.8f,
        )
        spots.forEachIndexed { i, (center, k) ->
            val color = colors[i % colors.size]
            val r = size.width * k
            drawCircle(
                brush = Brush.radialGradient(
                    0f to color.copy(alpha = 0.42f),
                    1f to Color.Transparent,
                    center = center,
                    radius = r,
                ),
                radius = r,
                center = center,
            )
        }
    }
}

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier, actions: @Composable () -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text, color = Sado.Text, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { actions() }
    }
}

/** Yumaloq shisha tugma. backdrop = null bo'lsa — matlangan shisha. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    size: Dp = 48.dp,
    iconSize: Dp = 22.dp,
    tint: Color = Sado.Text,
) {
    GlassSurface(
        backdrop = backdrop,
        cornerRadius = size / 2,
        refraction = 10.dp,
        bezel = 12.dp,
        modifier = modifier.size(size).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun AccentButton(text: String, icon: ImageVector?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Sado.Accent)
            .clickable(onClick = onClick)
            .padding(start = if (icon != null) 14.dp else 18.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = Sado.OnAccent, modifier = Modifier.size(18.dp))
        Text(text, color = Sado.OnAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun GlassChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (selected) {
        Box(
            modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(Sado.Accent)
                .clickable(onClick = onClick).padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) { Text(text, color = Sado.OnAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
    } else {
        GlassSurface(
            backdrop = null,
            cornerRadius = 18.dp,
            rimAlpha = 0.4f,
            modifier = modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, color = Sado.Text, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(), color = Sado.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp, modifier = modifier,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String = "${song.artist} · ${formatDuration(song.durationMs)}",
) {
    val rowMod = if (isCurrent) {
        Modifier.background(Color.White.copy(alpha = 0.07f))
    } else Modifier
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(rowMod)
            .combinedClickable(onClick = onClick, onLongClick = onMore)
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp)) {
            Artwork(song, Modifier.fillMaxSize(), cornerRadius = 10.dp, sizePx = 160)
            if (isCurrent) {
                Box(
                    Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.GraphicEq, if (isPlaying) "Ijro etilmoqda" else "Pauzada",
                        tint = Sado.Accent, modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title, color = if (isCurrent) Sado.Accent else Sado.Text,
                fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = Sado.Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onMore),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.MoreVert, "Qo'shimcha amallar", tint = Sado.Muted)
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GlassSurface(backdrop = null, cornerRadius = 36.dp, modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Sado.Accent, modifier = Modifier.size(32.dp))
        }
        Text(title, color = Sado.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(text, color = Sado.Muted, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(4.dp))
            action()
        }
    }
}

/** Ijro joyini har 250 ms da yangilab turadi. */
@Composable
fun rememberPlaybackPosition(isPlaying: Boolean, key: Any?): State<Long> {
    val pos = remember { mutableLongStateOf(PlayerConnection.positionMs) }
    LaunchedEffect(isPlaying, key) {
        pos.longValue = PlayerConnection.positionMs
        while (isPlaying) {
            delay(250)
            pos.longValue = PlayerConnection.positionMs
        }
    }
    return pos
}

/** Ingichka vaqt chizig'i: bosib yoki surib o'tkazish mumkin. */
@Composable
fun SeekBar(
    fraction: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Sado.Text,
    trackColor: Color = Color.White.copy(alpha = 0.18f),
    onDragFraction: (Float?) -> Unit = {},
) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val seek by rememberUpdatedState(onSeek)
    val dragCb by rememberUpdatedState(onDragFraction)
    val shown = (dragFraction ?: fraction).coerceIn(0f, 1f)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectTapGestures { seek((it.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragFraction = (it.x / size.width).coerceIn(0f, 1f); dragCb(dragFraction)
                    },
                    onDragEnd = {
                        dragFraction?.let(seek); dragFraction = null; dragCb(null)
                    },
                    onDragCancel = { dragFraction = null; dragCb(null) },
                ) { change, _ ->
                    dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    dragCb(dragFraction)
                }
            }
    ) {
        val h = if (dragFraction != null) 10.dp.toPx() else 6.dp.toPx()
        val top = (size.height - h) / 2
        drawRoundRect(trackColor, Offset(0f, top), Size(size.width, h), CornerRadius(h / 2))
        drawRoundRect(activeColor, Offset(0f, top), Size(size.width * shown, h), CornerRadius(h / 2))
    }
}

val ContentBottomPadding = PaddingValues(bottom = 176.dp)

@Suppress("unused")
val UnspecifiedText: TextUnit = TextUnit.Unspecified
