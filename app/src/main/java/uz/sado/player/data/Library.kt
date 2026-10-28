package uz.sado.player.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Barcha qo'shiqlar ro'yxati. Telefonga yangi fayl tushsa o'zi yangilanadi. */
object Library {
    private lateinit var app: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded

    @Volatile
    private var byId: Map<Long, Song> = emptyMap()
    private var scanJob: Job? = null
    private var observerRegistered = false

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            scanJob?.cancel()
            scanJob = scope.launch {
                delay(1500)
                scanNow()
            }
        }
    }

    fun init(context: Context) {
        app = context.applicationContext
    }

    fun hasPermission(context: Context): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }

    fun refresh() {
        if (!hasPermission(app)) {
            _loaded.value = true
            return
        }
        if (!observerRegistered) {
            app.contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer
            )
            observerRegistered = true
        }
        scanJob?.cancel()
        scanJob = scope.launch { scanNow() }
    }

    private suspend fun scanNow() {
        val list = withContext(Dispatchers.IO) { MediaScanner.scan(app) }
        byId = list.associateBy { it.id }
        _songs.value = list
        _loaded.value = true
    }

    fun song(id: Long): Song? = byId[id]
    fun songs(ids: List<Long>): List<Song> = ids.mapNotNull { byId[it] }
}
