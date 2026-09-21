package app.still

import android.app.Application
import app.still.prefs.GesturePreferencesRepository
import app.still.launcher.InstalledAppCatalog

class StillApplication : Application() {
    lateinit var appCatalog: InstalledAppCatalog
        private set
    lateinit var gesturePreferences: GesturePreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        appCatalog = InstalledAppCatalog(this)
        gesturePreferences = GesturePreferencesRepository(this)
    }
}
