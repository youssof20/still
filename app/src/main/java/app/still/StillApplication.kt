package app.still

import android.app.Application
import app.still.launcher.InstalledAppCatalog
import app.still.prefs.GesturePreferencesRepository
import app.still.prefs.LauncherPreferencesRepository

class StillApplication : Application() {
    lateinit var appCatalog: InstalledAppCatalog
        private set
    lateinit var gesturePreferences: GesturePreferencesRepository
        private set
    lateinit var launcherPreferences: LauncherPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        appCatalog = InstalledAppCatalog(this)
        gesturePreferences = GesturePreferencesRepository(this)
        launcherPreferences = LauncherPreferencesRepository(this)
    }
}
