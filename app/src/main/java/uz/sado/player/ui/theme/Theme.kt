package uz.sado.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Sado {
    val Bg = Color(0xFF0E0F12)
    val Surface = Color(0xFF17191E)
    val Surface2 = Color(0xFF20232A)
    val Line = Color(0xFF2A2E36)
    val Text = Color(0xFFF2F3F5)
    val Muted = Color(0xFF9BA1AD)
    val Accent = Color(0xFFC8F560)
    val OnAccent = Color(0xFF0E0F12)

    /** Muqovasi yo'q qo'shiqlar va fon dog'lari uchun ranglar. */
    val Palette = listOf(
        Color(0xFF4C6FFF), Color(0xFFE8734A), Color(0xFF2BB673), Color(0xFFB05CE6),
        Color(0xFFF2C14E), Color(0xFF38BDF8), Color(0xFFF472B6), Color(0xFF7AA2FF),
    )

    fun colorFor(key: Long): Color = Palette[((key % Palette.size) + Palette.size).toInt() % Palette.size]
}

@Composable
fun SadoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Sado.Accent,
            onPrimary = Sado.OnAccent,
            secondary = Sado.Accent,
            background = Sado.Bg,
            onBackground = Sado.Text,
            surface = Sado.Surface,
            onSurface = Sado.Text,
            surfaceVariant = Sado.Surface2,
            onSurfaceVariant = Sado.Muted,
            surfaceContainerHigh = Sado.Surface2,
            outline = Sado.Line,
        ),
        content = content,
    )
}
