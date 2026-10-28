package uz.sado.player.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.sado.player.data.UserStore
import uz.sado.player.playback.AudioFx
import uz.sado.player.ui.components.EmptyState
import uz.sado.player.ui.components.GlassChip
import uz.sado.player.ui.components.SectionLabel
import uz.sado.player.ui.glass.GlassSurface
import uz.sado.player.ui.theme.Sado
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EqualizerScreen() {
    val info by AudioFx.info.collectAsStateWithLifecycle()
    val eq by UserStore.eq.collectAsStateWithLifecycle()
    val levels = remember(eq, info) { AudioFx.levels(eq) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 176.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Ekvalayzer", color = Sado.Text, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Switch(
                checked = eq.enabled,
                onCheckedChange = { AudioFx.setEnabled(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Sado.Bg,
                    checkedTrackColor = Sado.Accent,
                    uncheckedThumbColor = Sado.Muted,
                    uncheckedTrackColor = Sado.Surface2,
                    uncheckedBorderColor = Sado.Line,
                ),
            )
        }

        if (!info.available) {
            EmptyState(
                Icons.Rounded.GraphicEq,
                "Ekvalayzer hali tayyor emas",
                "Biror qo'shiqni ijro eting — ekvalayzer shundan keyin faollashadi. Ba'zi telefonlar tizim ekvalayzerini qo'llamaydi.",
            )
            return@Column
        }

        SectionLabel("Tayyor sozlamalar", Modifier.padding(start = 20.dp, top = 8.dp, bottom = 10.dp))
        FlowRow(
            Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AudioFx.presetNames.forEachIndexed { i, name ->
                GlassChip(name, selected = eq.preset == i, onClick = { AudioFx.setPreset(i) })
            }
        }

        GlassSurface(
            backdrop = null,
            cornerRadius = 28.dp,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 22.dp).fillMaxWidth()
                .alpha(if (eq.enabled) 1f else 0.45f),
        ) {
            Column(Modifier.padding(vertical = 16.dp, horizontal = 8.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("+${info.maxDb.roundToInt()} dB", color = Sado.Muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text(AudioFx.presetNames[eq.preset.coerceIn(0, AudioFx.presetNames.lastIndex)], color = Sado.Muted, fontSize = 11.sp)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    info.freqsHz.forEachIndexed { i, hz ->
                        val v = levels.getOrElse(i) { 0f }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                (if (v > 0.05f) "+" else "") + "%.0f".format(v),
                                color = if (kotlin.math.abs(v) < 0.5f) Sado.Muted else Sado.Text,
                                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(8.dp))
                            BandSlider(
                                value = v, min = info.minDb, max = info.maxDb, enabled = eq.enabled,
                                onChange = { AudioFx.setBand(i, it) },
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(formatHz(hz), color = Sado.Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("${info.minDb.roundToInt()} dB", color = Sado.Muted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp))
            }
        }

        if (info.bassSupported) {
            EffectCard("Bass kuchaytirish", eq.bass, eq.enabled) { AudioFx.setBass(it) }
        }
        if (info.virtualizerSupported) {
            EffectCard("Fazoviy ovoz (3D)", eq.virtualizer, eq.enabled) { AudioFx.setVirtualizer(it) }
        }
    }
}

private fun formatHz(hz: Int): String = if (hz >= 1000) {
    val k = hz / 1000f
    if (k >= 10f || k % 1f == 0f) "${k.roundToInt()}k" else "%.1fk".format(k)
} else "$hz"

@Composable
private fun EffectCard(title: String, value: Float, enabled: Boolean, onChange: (Float) -> Unit) {
    GlassSurface(
        backdrop = null,
        cornerRadius = 22.dp,
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp).fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Sado.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${(value * 100).roundToInt()}%", color = Sado.Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = value,
                onValueChange = onChange,
                enabled = enabled,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = Sado.Text,
                    activeTrackColor = Sado.Accent,
                    inactiveTrackColor = Sado.Line,
                ),
            )
        }
    }
}

@Composable
private fun BandSlider(value: Float, min: Float, max: Float, enabled: Boolean, onChange: (Float) -> Unit) {
    val cb by rememberUpdatedState(onChange)
    val isEnabled by rememberUpdatedState(enabled)
    Canvas(
        Modifier
            .width(34.dp)
            .height(200.dp)
            .pointerInput(min, max) {
                fun valueAt(y: Float) = (max - (y / size.height) * (max - min)).coerceIn(min, max)
                detectVerticalDragGestures(
                    onDragStart = { if (isEnabled) cb(valueAt(it.y)) },
                ) { change, _ ->
                    change.consume()
                    if (isEnabled) cb(valueAt(change.position.y))
                }
            }
            .pointerInput(min, max) {
                fun valueAt(y: Float) = (max - (y / size.height) * (max - min)).coerceIn(min, max)
                detectTapGestures { if (isEnabled) cb(valueAt(it.y)) }
            }
    ) {
        val cx = size.width / 2
        val trackW = 4.dp.toPx()
        val range = (max - min).takeIf { it > 0f } ?: 1f
        fun yOf(v: Float) = size.height * (max - v) / range
        val zeroY = yOf(0f.coerceIn(min, max))
        val y = yOf(value)
        drawRoundRect(Color.White.copy(alpha = 0.14f), Offset(cx - trackW / 2, 0f), Size(trackW, size.height), CornerRadius(trackW / 2))
        drawLine(Color.White.copy(alpha = 0.25f), Offset(cx - 10.dp.toPx(), zeroY), Offset(cx + 10.dp.toPx(), zeroY), strokeWidth = 2.dp.toPx())
        val top = minOf(y, zeroY)
        drawRoundRect(Sado.Accent, Offset(cx - trackW / 2, top), Size(trackW, kotlin.math.abs(y - zeroY)), CornerRadius(trackW / 2))
        val knobR = 9.dp.toPx()
        drawCircle(Color.Black.copy(alpha = 0.35f), knobR + 2.dp.toPx(), Offset(cx, y.coerceIn(knobR, size.height - knobR) + 1.dp.toPx()))
        drawCircle(Sado.Text, knobR, Offset(cx, y.coerceIn(knobR, size.height - knobR)))
    }
}
