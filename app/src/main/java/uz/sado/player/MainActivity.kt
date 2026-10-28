package uz.sado.player

import android.Manifest
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import uz.sado.player.data.Library
import uz.sado.player.playback.PlayerConnection
import uz.sado.player.ui.SadoRoot
import uz.sado.player.ui.theme.SadoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        val permissions = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_AUDIO)
                add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }.toTypedArray()

        setContent {
            SadoTheme {
                val context = LocalContext.current
                var granted by remember { mutableStateOf(Library.hasPermission(context)) }
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    granted = Library.hasPermission(context)
                    Library.refresh()
                }
                LaunchedEffect(Unit) {
                    if (granted) Library.refresh() else launcher.launch(permissions)
                }
                SadoRoot(
                    hasPermission = granted,
                    onRequestPermission = { launcher.launch(permissions) },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        PlayerConnection.connect(this)
    }

    override fun onStop() {
        PlayerConnection.disconnect()
        super.onStop()
    }
}
