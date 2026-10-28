package uz.sado.player.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.sado.player.data.Song
import uz.sado.player.data.UserStore
import uz.sado.player.playback.PlayerConnection
import uz.sado.player.ui.ActionTarget
import uz.sado.player.ui.AppState
import uz.sado.player.ui.Detail
import uz.sado.player.ui.theme.Sado

fun Context.toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

fun shareSong(context: Context, song: Song) {
    val intent = Intent(Intent.ACTION_SEND)
        .setType("audio/*")
        .putExtra(Intent.EXTRA_STREAM, song.uri)
        .putExtra(Intent.EXTRA_TEXT, "${song.title} — ${song.artist}")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(Intent.createChooser(intent, "Ulashish").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        context.toast("Ulashib bo'lmadi")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongActionsSheet(target: ActionTarget, app: AppState) {
    val context = LocalContext.current
    val favorites by UserStore.favorites.collectAsStateWithLifecycle()
    val song = target.song
    val isFav = song.id in favorites
    val close = { app.actionTarget = null }

    ModalBottomSheet(
        onDismissRequest = close,
        containerColor = Sado.Surface,
        contentColor = Sado.Text,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Sado.Line) },
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(song, Modifier.size(52.dp), cornerRadius = 12.dp, sizePx = 160)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${song.artist} · ${song.album}", color = Sado.Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        HorizontalDivider(color = Sado.Line, modifier = Modifier.padding(vertical = 8.dp))

        ActionItem(Icons.Rounded.SkipNext, "Keyingi bo'lib ijro etish") {
            PlayerConnection.playNext(song); context.toast("Keyingi bo'lib qo'yildi"); close()
        }
        ActionItem(Icons.AutoMirrored.Rounded.QueueMusic, "Navbat oxiriga qo'shish") {
            PlayerConnection.addToQueue(song); context.toast("Navbatga qo'shildi"); close()
        }
        ActionItem(
            if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            if (isFav) "Sevimlilardan olib tashlash" else "Sevimlilarga qo'shish",
        ) { UserStore.toggleFavorite(song.id); close() }
        ActionItem(Icons.AutoMirrored.Rounded.PlaylistAdd, "Pleylistga qo'shish") {
            app.addToPlaylistSong = song; close()
        }
        target.playlistId?.let { pid ->
            ActionItem(Icons.Rounded.RemoveCircleOutline, "Pleylistdan olib tashlash") {
                UserStore.removeFromPlaylist(pid, song.id); close()
            }
        }
        ActionItem(Icons.Rounded.Album, "Albomga o'tish") {
            close(); app.open(Detail.AlbumD(song.albumId))
        }
        ActionItem(Icons.Rounded.Person, "Ijrochiga o'tish") {
            close(); app.open(Detail.ArtistD(song.artist))
        }
        ActionItem(Icons.Rounded.Share, "Ulashish") {
            close(); shareSong(context, song)
        }
        Spacer(Modifier.navigationBarsPadding().height(12.dp))
    }
}

@Composable
private fun ActionItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Sado.Text, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(18.dp))
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun AddToPlaylistDialog(song: Song, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val playlists by UserStore.playlists.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(playlists.isEmpty()) }

    if (creating) {
        NameDialog(
            title = "Yangi pleylist",
            initial = "",
            confirmText = "Yaratish",
            onConfirm = { name ->
                UserStore.createPlaylist(name, song.id)
                context.toast("«$name» pleylistiga qo'shildi")
                onDismiss()
            },
            onDismiss = onDismiss,
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Sado.Surface,
        titleContentColor = Sado.Text,
        textContentColor = Sado.Text,
        title = { Text("Pleylistga qo'shish") },
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { creating = true }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Sado.Accent),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Add, null, tint = Sado.OnAccent) }
                    Spacer(Modifier.width(14.dp))
                    Text("Yangi pleylist", fontWeight = FontWeight.Bold)
                }
                playlists.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                            UserStore.addToPlaylist(p.id, song.id)
                            context.toast("«${p.name}» pleylistiga qo'shildi")
                            onDismiss()
                        }.padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Sado.Surface2),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.AutoMirrored.Rounded.QueueMusic, null, tint = Sado.Muted) }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(p.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${p.songIds.size} ta qo'shiq", color = Sado.Muted, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Bekor qilish", color = Sado.Muted) } },
    )
}

@Composable
fun NameDialog(
    title: String,
    initial: String,
    confirmText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Sado.Surface,
        titleContentColor = Sado.Text,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(60) },
                label = { Text("Nomi") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Sado.Accent,
                    focusedLabelColor = Sado.Accent,
                    cursorColor = Sado.Accent,
                    unfocusedBorderColor = Sado.Line,
                    focusedTextColor = Sado.Text,
                    unfocusedTextColor = Sado.Text,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) {
                Text(confirmText, color = if (name.isNotBlank()) Sado.Accent else Sado.Muted, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Bekor qilish", color = Sado.Muted) } },
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirmText: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Sado.Surface,
        titleContentColor = Sado.Text,
        textContentColor = Sado.Muted,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirmText, color = Sado.Accent, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Bekor qilish", color = Sado.Muted) } },
    )
}
