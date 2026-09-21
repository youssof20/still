package app.still

import android.app.Application
import app.still.appearance.AppearancePreferencesRepository
import app.still.appearance.FontImporter
import app.still.launcher.InstalledAppCatalog
import app.still.prefs.GesturePreferencesRepository
import app.still.prefs.LauncherPreferencesRepository
import app.still.tasks.StillDatabaseProvider
import app.still.tasks.TaskRepository

class StillApplication : Application() {
    lateinit var appCatalog: InstalledAppCatalog
        private set
    lateinit var gesturePreferences: GesturePreferencesRepository
        private set
    lateinit var launcherPreferences: LauncherPreferencesRepository
        private set
    lateinit var appearancePreferences: AppearancePreferencesRepository
        private set
    lateinit var fontImporter: FontImporter
        private set
    lateinit var taskRepository: TaskRepository
        private set

    override fun onCreate() {
        super.onCreate()
        appCatalog = InstalledAppCatalog(this)
        gesturePreferences = GesturePreferencesRepository(this)
        launcherPreferences = LauncherPreferencesRepository(this)
        appearancePreferences = AppearancePreferencesRepository(this)
        fontImporter = FontImporter(this)
        val db = StillDatabaseProvider.get(this)
        taskRepository = TaskRepository(this, db.taskDao())
    }
}
