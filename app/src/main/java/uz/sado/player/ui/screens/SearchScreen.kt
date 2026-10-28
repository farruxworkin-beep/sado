package uz.sado.player.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.sado.player.data.*
import uz.sado.player.playback.PlaybackState
import uz.sado.player.playback.PlayerConnection
import uz.sado.player.ui.ActionTarget
import uz.sado.player.ui.AppState
import uz.sado.player.ui.Detail
import uz.sado.player.ui.SmartKind
import uz.sado.player.ui.Tab
import uz.sado.player.ui.components.*
import uz.sado.player.ui.glass.GlassSurface
import uz.sado.player.ui.theme.Sado

/** O'zbekcha apostroflar (ʻ ’ ` ') farqini yo'qotib, kichik harfga o'tkazadi. */
private fun norm(s: String): String =
    s.lowercase().replace('ʻ', '\'').replace('’', '\'').replace('`', '\'').replace('ʼ', '\'').replace('‘', '\'')

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(app: AppState, songs: List<Song>, playback: PlaybackState) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    var query by rememberSaveable { mutableStateOf("") }
    val recents by UserStore.recentSearches.collectAsStateWithLifecycle()
    val lastPlayed by UserStore.lastPlayed.collectAsStateWithLifecycle()

    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let {
            query = it
            UserStore.addRecentSearch(it)
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Qidiruv")
        GlassSurface(
            backdrop = null,
            cornerRadius = 26.dp,
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(52.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(Modifier.fillMaxSize().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Search, null, tint = Sado.Muted)
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Sado.Text, fontSize = 15.sp),
                    cursorBrush = SolidColor(Sado.Accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { UserStore.addRecentSearch(query); focus.clearFocus() }),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) Text("Qo'shiq, ijrochi, albom…", color = Sado.Muted, fontSize = 15.sp)
                            inner()
                        }
                    },
                )
                if (query.isNotEmpty()) {
                    Box(Modifier.size(44.dp).clip(CircleShape).clickable { query = "" }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Close, "Tozalash", tint = Sado.Muted)
                    }
                } else {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).clickable {
                            try {
                                voice.launch(
                                    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "uz-UZ")
                                        .putExtra(RecognizerIntent.EXTRA_PROMPT, "Qo'shiq yoki ijrochi nomini ayting")
                                )
                            } catch (_: ActivityNotFoundException) {
                                context.toast("Telefoningizda ovozli qidiruv yo'q")
                            }
                        },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Mic, "Ovozli qidiruv", tint = Sado.Text) }
                }
            }
        }

        if (query.isBlank()) {
            val recentSongs = remember(songs, lastPlayed) {
                songs.filter { lastPlayed.containsKey(it.id) }.sortedByDescending { lastPlayed[it.id] ?: 0L }.take(12)
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = ContentBottomPadding) {
                if (recents.isNotEmpty()) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 18.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SectionLabel("So'nggi qidiruvlar", Modifier.weight(1f))
                            Text(
                                "Tozalash", color = Sado.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { UserStore.clearRecentSearches() }.padding(8.dp),
                            )
                        }
                    }
                    item {
                        FlowRow(
                            Modifier.padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            recents.forEach { r ->
                                GlassSurface(backdrop = null, cornerRadius = 18.dp, rimAlpha = 0.4f, modifier = Modifier.height(36.dp)) {
                                    Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            r, color = Sado.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.clip(RoundedCornerShape(18.dp)).clickable { query = r }.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                                        )
                                        Box(
                                            Modifier.size(32.dp).clip(CircleShape).clickable { UserStore.removeRecentSearch(r) },
                                            contentAlignment = Alignment.Center,
                                        ) { Icon(Icons.Rounded.Close, "O'chirish", tint = Sado.Muted, modifier = Modifier.size(16.dp)) }
                                    }
                                }
                            }
                        }
                    }
                }
                item { SectionLabel("Bo'limlar bo'yicha", Modifier.padding(start = 20.dp, top = 22.dp, bottom = 10.dp)) }
                item {
                    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CategoryTile("Albomlar", Icons.Rounded.Album, Sado.Accent, Modifier.weight(1f)) { app.libraryTab = 1; app.selectTab(Tab.Library) }
                            CategoryTile("Ijrochilar", Icons.Rounded.Person, Sado.Palette[7], Modifier.weight(1f)) { app.libraryTab = 2; app.selectTab(Tab.Library) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CategoryTile("Papkalar", Icons.Rounded.Folder, Sado.Palette[4], Modifier.weight(1f)) { app.libraryTab = 3; app.selectTab(Tab.Library) }
                            CategoryTile("Sevimlilar", Icons.Rounded.Favorite, Sado.Palette[6], Modifier.weight(1f)) { app.open(Detail.SmartD(SmartKind.Favorites)) }
                        }
                    }
                }
                if (recentSongs.isNotEmpty()) {
                    item { SectionLabel("Yaqinda ijro etilgan", Modifier.padding(start = 20.dp, top = 24.dp, bottom = 10.dp)) }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            itemsIndexed(recentSongs, key = { _, s -> s.id }) { i, s ->
                                Column(Modifier.width(112.dp).clip(RoundedCornerShape(14.dp)).clickable { PlayerConnection.play(recentSongs, i) }) {
                                    Artwork(s, Modifier.size(112.dp), cornerRadius = 14.dp, sizePx = 300)
                                    Spacer(Modifier.height(6.dp))
                                    Text(s.title, color = Sado.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(s.artist, color = Sado.Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val q = norm(query.trim())
            val songHits = remember(songs, q) {
                songs.filter { norm(it.title).contains(q) || norm(it.artist).contains(q) || norm(it.album).contains(q) }.take(100)
            }
            val albumHits = remember(songs, q) { songs.toAlbums().filter { norm(it.title).contains(q) }.take(10) }
            val artistHits = remember(songs, q) { songs.toArtists().filter { norm(it.name).contains(q) }.take(10) }

            LazyColumn(Modifier.fillMaxSize(), contentPadding = ContentBottomPadding) {
                if (songHits.isEmpty() && albumHits.isEmpty() && artistHits.isEmpty()) {
                    item { EmptyState(Icons.Rounded.SearchOff, "Hech narsa topilmadi", "«$query» bo'yicha natija yo'q. Boshqacha yozib ko'ring.") }
                }
                if (artistHits.isNotEmpty()) {
                    item { SectionLabel("Ijrochilar", Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp)) }
                    items(artistHits, key = { "ar-" + it.name }) { a ->
                        ListRow(a.name, "${a.songs.size} ta qo'shiq", onClick = { UserStore.addRecentSearch(query); app.open(Detail.ArtistD(a.name)) }) {
                            Box(Modifier.size(48.dp).clip(CircleShape).background(Sado.colorFor(a.name.hashCode().toLong())), contentAlignment = Alignment.Center) {
                                Text(a.name.firstOrNull()?.uppercase() ?: "?", color = Sado.Bg, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            }
                        }
                    }
                }
                if (albumHits.isNotEmpty()) {
                    item { SectionLabel("Albomlar", Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp)) }
                    items(albumHits, key = { "al-" + it.id }) { a ->
                        ListRow(a.title, "${a.artist} · ${a.songs.size} ta", onClick = { UserStore.addRecentSearch(query); app.open(Detail.AlbumD(a.id)) }) {
                            Artwork(a.songs.first(), Modifier.size(48.dp), cornerRadius = 10.dp, sizePx = 160)
                        }
                    }
                }
                if (songHits.isNotEmpty()) {
                    item { SectionLabel("Qo'shiqlar · ${songHits.size}", Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp)) }
                    itemsIndexed(songHits, key = { _, s -> "s-" + s.id }) { i, s ->
                        SongRow(
                            song = s,
                            isCurrent = s.id == playback.currentId,
                            isPlaying = playback.isPlaying,
                            onClick = { UserStore.addRecentSearch(query); focus.clearFocus(); PlayerConnection.play(songHits, i) },
                            onMore = { app.actionTarget = ActionTarget(s) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTile(label: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    GlassSurface(
        backdrop = null,
        cornerRadius = 20.dp,
        tint = color.copy(alpha = 0.20f),
        modifier = modifier.height(72.dp).clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, color = Sado.Text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
