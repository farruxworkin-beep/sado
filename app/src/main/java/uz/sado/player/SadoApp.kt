package uz.sado.player

import android.app.Application
import uz.sado.player.data.Library
import uz.sado.player.data.UserStore

class SadoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        UserStore.init(this)
        Library.init(this)
    }
}
