package app.still

import android.app.Application
import app.still.appearance.AppearancePreferencesRepository
import app.still.appearance.FontImporter
import app.still.launcher.AppShortcutHelper
import app.still.launcher.InstalledAppCatalog
import app.still.prefs.GesturePreferencesRepository
import app.still.prefs.LauncherPreferencesRepository
import app.still.tasks.StillDatabaseProvider
import app.still.tasks.TaskRepository
import app.still.widgets.StillWidgetHostController
import app.still.widgets.WidgetPlacementRepository

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
    lateinit var widgetPlacements: WidgetPlacementRepository
        private set
    lateinit var widgetHost: StillWidgetHostController
        private set
    lateinit var appShortcuts: AppShortcutHelper
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
        widgetPlacements = WidgetPlacementRepository(this)
        widgetHost = StillWidgetHostController(this)
        appShortcuts = AppShortcutHelper(this, appCatalog)
    }
}
