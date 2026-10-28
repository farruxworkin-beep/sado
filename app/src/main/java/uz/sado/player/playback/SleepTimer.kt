package uz.sado.player.playback

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Uyqu taymeri: belgilangan vaqtdan keyin ijroni to'xtatadi (ilova yopiq bo'lsa ham). */
object SleepTimer {
    val options = listOf(0, 15, 30, 60, 90)

    private val _endAt = MutableStateFlow<Long?>(null) // SystemClock.elapsedRealtime() bo'yicha
    val endAt: StateFlow<Long?> = _endAt

    private val _option = MutableStateFlow(0)
    val option: StateFlow<Int> = _option

    /** PlaybackService o'rnatadi. */
    var pauseAction: (() -> Unit)? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    fun cycle() {
        val next = (_option.value + 1) % options.size
        start(next)
    }

    fun cancel() = start(0)

    private fun start(optionIndex: Int) {
        job?.cancel()
        _option.value = optionIndex
        val minutes = options[optionIndex]
        if (minutes <= 0) {
            _endAt.value = null
            return
        }
        val ms = minutes * 60_000L
        _endAt.value = SystemClock.elapsedRealtime() + ms
        job = scope.launch {
            delay(ms)
            pauseAction?.invoke()
            _endAt.value = null
            _option.value = 0
        }
    }
}
