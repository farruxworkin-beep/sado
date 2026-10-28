package uz.sado.player.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.sado.player.data.Song
import uz.sado.player.data.albumArtUri
import uz.sado.player.ui.theme.Sado

/** Muqova rasmlarini xotirada saqlab turuvchi kesh. */
object ArtworkCache {
    private val cache = object : LruCache<String, ImageBitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }
    private val misses = HashSet<String>()

    fun key(song: Song, size: Int) = "${song.albumId}:${song.id.takeIf { song.albumId <= 0 } ?: 0}:$size"

    fun peek(key: String): ImageBitmap? = cache.get(key)

    suspend fun load(context: Context, song: Song, size: Int): ImageBitmap? {
        val k = key(song, size)
        cache.get(k)?.let { return it }
        synchronized(misses) { if (k in misses) return null }
        val bmp = withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(song.uri, Size(size, size), null)
                } else {
                    context.contentResolver.openInputStream(albumArtUri(song.albumId))?.use { input ->
                        val bytes = input.readBytes()
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                        var sample = 1
                        while (bounds.outWidth / (sample * 2) >= size && bounds.outHeight / (sample * 2) >= size) sample *= 2
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                    }
                }
            } catch (_: Throwable) {
                null
            }
        }
        return if (bmp != null) {
            bmp.asImageBitmap().also { cache.put(k, it) }
        } else {
            synchronized(misses) { misses += k }
            null
        }
    }
}

/** Qo'shiq muqovasi; rasmi bo'lmasa rangli plastinka chiziladi. */
@Composable
fun Artwork(
    song: Song?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
    sizePx: Int = 256,
) {
    val context = LocalContext.current
    val key = song?.let { ArtworkCache.key(it, sizePx) }
    val bitmap by produceState(initialValue = key?.let { ArtworkCache.peek(it) }, key) {
        if (song != null && value == null) value = ArtworkCache.load(context, song, sizePx)
    }
    val shape = RoundedCornerShape(cornerRadius)
    val color = song?.let { Sado.colorFor(it.albumId) } ?: Sado.Surface2

    Box(modifier.clip(shape).background(color)) {
        val bmp = bitmap
        if (bmp != null) {
            Image(bmp, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            VinylPlaceholder(Modifier.fillMaxSize())
        }
    }
}

@Composable
fun VinylPlaceholder(modifier: Modifier = Modifier, ringColor: Color = Color(0xFF0E0F12)) {
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2)
        val maxR = size.minDimension * 0.38f
        val w = (size.minDimension * 0.035f).coerceAtLeast(1f)
        listOf(1f, 0.72f, 0.44f).forEach { k ->
            drawCircle(ringColor.copy(alpha = 0.32f), radius = maxR * k, center = c, style = Stroke(w))
        }
        drawCircle(ringColor.copy(alpha = 0.4f), radius = maxR * 0.12f, center = c)
    }
}
