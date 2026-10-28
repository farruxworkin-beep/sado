package uz.sado.player.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

data class Playlist(val id: Long, val name: String, val songIds: List<Long>)

data class EqSettings(
    val enabled: Boolean = true,
    val preset: Int = 0,
    val custom: List<Float> = emptyList(),
    val bass: Float = 0f,
    val virtualizer: Float = 0f,
)

data class SavedQueueItem(val id: Long, val title: String, val artist: String, val album: String, val albumId: Long)
data class SavedQueue(val items: List<SavedQueueItem>, val index: Int, val positionMs: Long)

/**
 * Foydalanuvchi ma'lumotlari: sevimlilar, pleylistlar, statistika, qidiruvlar,
 * ekvalayzer va oxirgi navbat. Hammasi SharedPreferences da JSON ko'rinishida saqlanadi.
 */
object UserStore {
    private lateinit var prefs: SharedPreferences

    private val _favorites = MutableStateFlow<Set<Long>>(emptySet())
    val favorites: StateFlow<Set<Long>> = _favorites

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists

    private val _playCounts = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val playCounts: StateFlow<Map<Long, Int>> = _playCounts

    private val _lastPlayed = MutableStateFlow<Map<Long, Long>>(emptyMap())
    val lastPlayed: StateFlow<Map<Long, Long>> = _lastPlayed

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches

    private val _eq = MutableStateFlow(EqSettings())
    val eq: StateFlow<EqSettings> = _eq

    private val _sortMode = MutableStateFlow(0)
    val sortMode: StateFlow<Int> = _sortMode

    fun init(context: Context) {
        prefs = context.getSharedPreferences("sado", Context.MODE_PRIVATE)
        _favorites.value = readLongArray("favorites").toSet()
        _playlists.value = readPlaylists()
        _playCounts.value = readMap("playCounts").mapValues { it.value.toInt() }
        _lastPlayed.value = readMap("lastPlayed")
        _recentSearches.value = readStringArray("recentSearches")
        _eq.value = readEq()
        _sortMode.value = prefs.getInt("sortMode", 0)
    }

    // ---------- Sevimlilar ----------
    fun isFavorite(id: Long) = id in _favorites.value

    fun toggleFavorite(id: Long) {
        val next = _favorites.value.toMutableSet().apply { if (!add(id)) remove(id) }
        _favorites.value = next
        prefs.edit().putString("favorites", JSONArray(next.toList()).toString()).apply()
    }

    // ---------- Pleylistlar ----------
    fun createPlaylist(name: String, firstSong: Long? = null): Playlist {
        val p = Playlist(System.currentTimeMillis(), name.trim(), listOfNotNull(firstSong))
        savePlaylists(_playlists.value + p)
        return p
    }

    fun renamePlaylist(id: Long, name: String) =
        savePlaylists(_playlists.value.map { if (it.id == id) it.copy(name = name.trim()) else it })

    fun deletePlaylist(id: Long) = savePlaylists(_playlists.value.filterNot { it.id == id })

    fun addToPlaylist(id: Long, songId: Long) = savePlaylists(_playlists.value.map {
        if (it.id == id && songId !in it.songIds) it.copy(songIds = it.songIds + songId) else it
    })

    fun removeFromPlaylist(id: Long, songId: Long) = savePlaylists(_playlists.value.map {
        if (it.id == id) it.copy(songIds = it.songIds - songId) else it
    })

    private fun savePlaylists(list: List<Playlist>) {
        _playlists.value = list
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(JSONObject().put("id", p.id).put("name", p.name).put("songs", JSONArray(p.songIds)))
        }
        prefs.edit().putString("playlists", arr.toString()).apply()
    }

    private fun readPlaylists(): List<Playlist> = try {
        val arr = JSONArray(prefs.getString("playlists", "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val songs = o.getJSONArray("songs")
            Playlist(o.getLong("id"), o.getString("name"), (0 until songs.length()).map { songs.getLong(it) })
        }
    } catch (_: Exception) {
        emptyList()
    }

    // ---------- Statistika ----------
    fun recordPlay(id: Long) {
        val counts = _playCounts.value.toMutableMap()
        counts[id] = (counts[id] ?: 0) + 1
        _playCounts.value = counts
        val last = _lastPlayed.value.toMutableMap()
        last[id] = System.currentTimeMillis()
        _lastPlayed.value = last
        prefs.edit()
            .putString("playCounts", mapToJson(counts.mapValues { it.value.toLong() }))
            .putString("lastPlayed", mapToJson(last))
            .apply()
    }

    // ---------- Qidiruv tarixi ----------
    fun addRecentSearch(q: String) {
        val t = q.trim()
        if (t.length < 2) return
        val next = (listOf(t) + _recentSearches.value.filterNot { it.equals(t, ignoreCase = true) }).take(10)
        _recentSearches.value = next
        prefs.edit().putString("recentSearches", JSONArray(next).toString()).apply()
    }

    fun removeRecentSearch(q: String) {
        val next = _recentSearches.value - q
        _recentSearches.value = next
        prefs.edit().putString("recentSearches", JSONArray(next).toString()).apply()
    }

    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
        prefs.edit().remove("recentSearches").apply()
    }

    // ---------- Saralash ----------
    fun setSortMode(mode: Int) {
        _sortMode.value = mode
        prefs.edit().putInt("sortMode", mode).apply()
    }

    // ---------- Ekvalayzer ----------
    fun updateEq(transform: (EqSettings) -> EqSettings): EqSettings {
        val next = transform(_eq.value)
        _eq.value = next
        prefs.edit().putString(
            "eq",
            JSONObject()
                .put("enabled", next.enabled)
                .put("preset", next.preset)
                .put("custom", JSONArray(next.custom.map { it.toDouble() }))
                .put("bass", next.bass.toDouble())
                .put("virt", next.virtualizer.toDouble())
                .toString()
        ).apply()
        return next
    }

    private fun readEq(): EqSettings = try {
        val o = JSONObject(prefs.getString("eq", "{}") ?: "{}")
        val c = o.optJSONArray("custom") ?: JSONArray()
        EqSettings(
            enabled = o.optBoolean("enabled", true),
            preset = o.optInt("preset", 0),
            custom = (0 until c.length()).map { c.getDouble(it).toFloat() },
            bass = o.optDouble("bass", 0.0).toFloat(),
            virtualizer = o.optDouble("virt", 0.0).toFloat(),
        )
    } catch (_: Exception) {
        EqSettings()
    }

    // ---------- Oxirgi navbat (ilova qayta ochilganda tiklanadi) ----------
    fun saveQueue(q: SavedQueue) {
        val arr = JSONArray()
        q.items.forEach {
            arr.put(
                JSONObject().put("id", it.id).put("t", it.title).put("a", it.artist)
                    .put("al", it.album).put("alb", it.albumId)
            )
        }
        prefs.edit().putString(
            "queue",
            JSONObject().put("items", arr).put("index", q.index).put("pos", q.positionMs).toString()
        ).apply()
    }

    fun loadQueue(): SavedQueue? = try {
        val raw = prefs.getString("queue", null)
        if (raw == null) null else {
            val o = JSONObject(raw)
            val arr = o.getJSONArray("items")
            SavedQueue(
                items = (0 until arr.length()).map { i ->
                    val it = arr.getJSONObject(i)
                    SavedQueueItem(it.getLong("id"), it.optString("t"), it.optString("a"), it.optString("al"), it.optLong("alb"))
                },
                index = o.optInt("index", 0),
                positionMs = o.optLong("pos", 0),
            )
        }
    } catch (_: Exception) {
        null
    }

    // ---------- yordamchilar ----------
    private fun readLongArray(key: String): List<Long> = try {
        val arr = JSONArray(prefs.getString(key, "[]"))
        (0 until arr.length()).map { arr.getLong(it) }
    } catch (_: Exception) {
        emptyList()
    }

    private fun readStringArray(key: String): List<String> = try {
        val arr = JSONArray(prefs.getString(key, "[]"))
        (0 until arr.length()).map { arr.getString(it) }
    } catch (_: Exception) {
        emptyList()
    }

    private fun readMap(key: String): Map<Long, Long> = try {
        val o = JSONObject(prefs.getString(key, "{}") ?: "{}")
        o.keys().asSequence().associate { it.toLong() to o.getLong(it) }
    } catch (_: Exception) {
        emptyMap()
    }

    private fun mapToJson(map: Map<Long, Long>): String {
        val o = JSONObject()
        map.forEach { (k, v) -> o.put(k.toString(), v) }
        return o.toString()
    }
}
