package uz.sado.player.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import uz.sado.player.data.EqSettings
import uz.sado.player.data.UserStore
import kotlin.math.ln

/**
 * Tizim ekvalayzeri, bass kuchaytirgich va 3D (virtualizer).
 * Telefonlarda polosalar soni turlicha (ko'pincha 5 ta), shuning uchun
 * tayyor sozlamalar 10 polosali shablondan telefon polosalariga moslab hisoblanadi.
 */
object AudioFx {

    data class Info(
        val available: Boolean = false,
        val freqsHz: List<Int> = emptyList(),
        val minDb: Float = -15f,
        val maxDb: Float = 15f,
        val bassSupported: Boolean = false,
        val virtualizerSupported: Boolean = false,
    )

    private val _info = MutableStateFlow(Info())
    val info: StateFlow<Info> = _info

    val presetNames = listOf("Oddiy", "Bass", "Pop", "Rok", "Jazz", "Vokal", "Klassik", "Maxsus")
    const val CUSTOM = 7

    private val templateHz = floatArrayOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
    private val templates = arrayOf(
        floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        floatArrayOf(8f, 7f, 5f, 2f, 0f, -1f, 0f, 1f, 2f, 2f),
        floatArrayOf(-1f, 1f, 3f, 4f, 3f, 0f, -1f, -1f, 1f, 2f),
        floatArrayOf(5f, 3f, -1f, -3f, -1f, 2f, 4f, 5f, 5f, 5f),
        floatArrayOf(3f, 2f, 1f, 2f, -1f, -1f, 0f, 1f, 2f, 3f),
        floatArrayOf(-2f, -2f, -1f, 1f, 4f, 5f, 4f, 2f, 0f, -1f),
        floatArrayOf(4f, 3f, 2f, 1f, -1f, -1f, 0f, 2f, 3f, 4f),
        floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
    )

    private var eq: Equalizer? = null
    private var bass: BassBoost? = null
    private var virt: Virtualizer? = null

    fun attach(audioSessionId: Int) {
        release()
        if (audioSessionId <= 0) return
        try {
            val e = Equalizer(0, audioSessionId)
            val range = e.bandLevelRange
            val freqs = (0 until e.numberOfBands).map { e.getCenterFreq(it.toShort()) / 1000 }
            eq = e
            bass = runCatching { BassBoost(0, audioSessionId) }.getOrNull()
            virt = runCatching { Virtualizer(0, audioSessionId) }.getOrNull()
            _info.value = Info(
                available = true,
                freqsHz = freqs,
                minDb = range[0] / 100f,
                maxDb = range[1] / 100f,
                bassSupported = bass?.strengthSupported == true,
                virtualizerSupported = virt?.strengthSupported == true,
            )
            applySettings(UserStore.eq.value)
        } catch (_: Throwable) {
            release()
        }
    }

    fun release() {
        runCatching { eq?.release() }
        runCatching { bass?.release() }
        runCatching { virt?.release() }
        eq = null; bass = null; virt = null
        _info.value = Info()
    }

    /** Joriy sozlama bo'yicha har bir polosa uchun dB qiymatlari. */
    fun levels(s: EqSettings): List<Float> {
        val inf = _info.value
        if (s.preset == CUSTOM && s.custom.size == inf.freqsHz.size) return s.custom
        val p = s.preset.coerceIn(0, templates.lastIndex)
        return inf.freqsHz.map { hz -> interpolate(templates[p], hz.toFloat()).coerceIn(inf.minDb, inf.maxDb) }
    }

    private fun interpolate(values: FloatArray, hz: Float): Float {
        val x = ln(hz.coerceAtLeast(1f))
        val xs = templateHz.map { ln(it) }
        if (x <= xs.first()) return values.first()
        if (x >= xs.last()) return values.last()
        for (i in 0 until xs.size - 1) {
            if (x <= xs[i + 1]) {
                val t = (x - xs[i]) / (xs[i + 1] - xs[i])
                return values[i] + (values[i + 1] - values[i]) * t
            }
        }
        return 0f
    }

    fun applySettings(s: EqSettings) {
        val e = eq ?: return
        try {
            e.setEnabled(s.enabled)
            levels(s).forEachIndexed { i, db ->
                e.setBandLevel(i.toShort(), (db * 100).toInt().toShort())
            }
        } catch (_: Throwable) {
        }
        bass?.let { b ->
            runCatching {
                if (b.strengthSupported) {
                    b.setStrength((s.bass * 1000).toInt().coerceIn(0, 1000).toShort())
                    b.setEnabled(s.enabled && s.bass > 0.01f)
                }
            }
        }
        virt?.let { v ->
            runCatching {
                if (v.strengthSupported) {
                    v.setStrength((s.virtualizer * 1000).toInt().coerceIn(0, 1000).toShort())
                    v.setEnabled(s.enabled && s.virtualizer > 0.01f)
                }
            }
        }
    }

    // ---- UI chaqiradigan amallar ----
    fun setEnabled(on: Boolean) = applySettings(UserStore.updateEq { it.copy(enabled = on) })
    fun setPreset(p: Int) = applySettings(UserStore.updateEq { it.copy(preset = p) })
    fun setBass(v: Float) = applySettings(UserStore.updateEq { it.copy(bass = v) })
    fun setVirtualizer(v: Float) = applySettings(UserStore.updateEq { it.copy(virtualizer = v) })

    fun setBand(index: Int, db: Float) {
        val current = levels(UserStore.eq.value).toMutableList()
        if (index !in current.indices) return
        current[index] = db
        applySettings(UserStore.updateEq { it.copy(preset = CUSTOM, custom = current) })
    }
}
