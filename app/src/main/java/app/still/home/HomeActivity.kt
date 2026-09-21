package app.still.home

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.format.DateFormat
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.still.R
import app.still.StillApplication
import app.still.appearance.AppearanceSettings
import app.still.appearance.FontSource
import app.still.appearance.ImportedFontInfo
import app.still.appearance.ThemePresetCodec
import app.still.appearance.rememberResolvedAppearance
import app.still.launcher.AppLauncher
import app.still.launcher.AppTargetId
import app.still.launcher.AppVisibility
import app.still.launcher.CatalogOverlay
import app.still.launcher.HideMode
import app.still.launcher.LaunchFailureReason
import app.still.launcher.LaunchResult
import app.still.prefs.GestureActionSlot
import app.still.prefs.GesturePreferences
import app.still.prefs.GestureTargetPreference
import app.still.prefs.LauncherPreferences
import app.still.settings.HomeRoleHelper
import app.still.tasks.AddTaskResult
import app.still.tasks.TaskBackup
import app.still.tasks.TaskEntity
import app.still.ui.StillTheme
import kotlinx.coroutines.launch

enum class StillSurface {
    Home,
    Apps,
    Settings,
    Appearance,
    HiddenApps,
    Picker,
    Tasks,
    AddTask,
    EditTask,
}

class HomeActivity : ComponentActivity() {
    private lateinit var appLauncher: AppLauncher

    private val homeRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        defaultHomeHeld = HomeRoleHelper.isDefaultHome(this)
    }

    private var defaultHomeHeld by mutableStateOf(false)
    private var homeInvocation by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as StillApplication
        appLauncher = AppLauncher(this, app.appCatalog)
        defaultHomeHeld = HomeRoleHelper.isDefaultHome(this)
        lifecycleScope.launch {
            app.taskRepository.purgeExpiredTrash()
        }

        setContent {
            val fontImporter = app.fontImporter
            val committedAppearance by app.appearancePreferences.settings.collectAsStateWithLifecycle(
                initialValue = AppearanceSettings(),
            )
            var appearanceDraft by remember { mutableStateOf(AppearanceSettings()) }
            var importedFontsTick by remember { mutableIntStateOf(0) }
            val importedFonts = remember(importedFontsTick) {
                fontImporter.listImported()
            }
            var surface by rememberSaveable { mutableStateOf(StillSurface.Home) }

            val activeAppearance =
                if (surface == StillSurface.Appearance) appearanceDraft else committedAppearance
            val resolved = rememberResolvedAppearance(activeAppearance, fontImporter)
            val forceSettingsTypography =
                surface == StillSurface.Settings ||
                    surface == StillSurface.Appearance ||
                    surface == StillSurface.HiddenApps

            SideEffect {
                runCatching {
                    if (resolved.showWallpaperScrim) {
                        window.setBackgroundDrawable(ColorDrawable(AndroidColor.TRANSPARENT))
                        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                    }
                }
            }

            StillTheme(
                resolved = resolved,
                forceSettingsTypography = forceSettingsTypography,
            ) {
                val catalogRaw by app.appCatalog.targets.collectAsStateWithLifecycle()
                val gestures by app.gesturePreferences.preferences.collectAsStateWithLifecycle(
                    initialValue = GesturePreferences(),
                )
                val launcherPrefs by app.launcherPreferences.preferences.collectAsStateWithLifecycle(
                    initialValue = LauncherPreferences(),
                )
                val catalog = remember(catalogRaw, launcherPrefs) {
                    CatalogOverlay.apply(catalogRaw, launcherPrefs)
                }
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                val activeTasks by app.taskRepository.active.collectAsStateWithLifecycle(
                    emptyList(),
                )
                val doneTasks by app.taskRepository.done.collectAsStateWithLifecycle(emptyList())
                val trashTasks by app.taskRepository.trash.collectAsStateWithLifecycle(emptyList())
                val taskDraft by app.taskRepository.draft.collectAsStateWithLifecycle("")
                val previewLimit = launcherPrefs.taskPreviewLimit
                val taskPreviewFlow = remember(previewLimit) {
                    app.taskRepository.preview(previewLimit.coerceAtLeast(0))
                }
                val taskPreview by taskPreviewFlow.collectAsStateWithLifecycle(emptyList())

                var editingHome by rememberSaveable { mutableStateOf(false) }
                var searchValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
                    mutableStateOf(TextFieldValue())
                }
                var pickingSlot by rememberSaveable { mutableStateOf<String?>(null) }
                var replaceFavoriteId by rememberSaveable { mutableStateOf<String?>(null) }
                var sessionCompletedIdList by rememberSaveable {
                    mutableStateOf(emptyList<String>())
                }
                val sessionCompletedIds = sessionCompletedIdList.toSet()
                var editingTaskId by rememberSaveable { mutableStateOf<String?>(null) }
                var editInitialTitle by rememberSaveable { mutableStateOf("") }
                var editTitle by rememberSaveable { mutableStateOf("") }
                var addInitialTitle by rememberSaveable { mutableStateOf("") }
                var addTitle by rememberSaveable { mutableStateOf("") }
                var addFromTasks by rememberSaveable { mutableStateOf(false) }
                var editFromTasks by rememberSaveable { mutableStateOf(true) }
                var pendingImportTasks by remember { mutableStateOf<List<TaskEntity>?>(null) }

                val importLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument(),
                ) { uri ->
                    if (uri == null) return@rememberLauncherForActivityResult
                    val raw = runCatching {
                        contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    }.getOrNull()
                    if (raw == null) {
                        scope.launch {
                            snackbarHostState.showSnackbar(getString(R.string.import_failed))
                        }
                        return@rememberLauncherForActivityResult
                    }
                    val parsed = TaskBackup.parseJson(raw)
                    if (parsed.isFailure) {
                        scope.launch {
                            snackbarHostState.showSnackbar(getString(R.string.import_failed))
                        }
                        return@rememberLauncherForActivityResult
                    }
                    pendingImportTasks = parsed.getOrThrow()
                }

                val fontImportLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument(),
                ) { uri ->
                    if (uri == null) return@rememberLauncherForActivityResult
                    val result = fontImporter.importFromUri(uri)
                    if (result.isSuccess) {
                        val info = result.getOrThrow()
                        importedFontsTick += 1
                        appearanceDraft = appearanceDraft.copy(
                            fontSource = FontSource.Imported,
                            importedFontId = info.id,
                        )
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                getString(R.string.appearance_font_imported_ok),
                            )
                        }
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                getString(R.string.appearance_font_import_failed),
                            )
                        }
                    }
                }

                val themePresetImportLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument(),
                ) { uri ->
                    if (uri == null) return@rememberLauncherForActivityResult
                    val raw = runCatching {
                        contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    }.getOrNull()
                    if (raw == null) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                getString(R.string.appearance_preset_failed),
                            )
                        }
                        return@rememberLauncherForActivityResult
                    }
                    val parsed = ThemePresetCodec.parse(raw)
                    if (parsed.isFailure) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                getString(R.string.appearance_preset_failed),
                            )
                        }
                    } else {
                        appearanceDraft = parsed.getOrThrow()
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                getString(R.string.appearance_preset_imported),
                            )
                        }
                    }
                }

                val hasUnsavedAppearanceChanges = appearanceDraft != committedAppearance

                fun showLaunchFailure(reason: LaunchFailureReason) {
                    val message = when (reason) {
                        LaunchFailureReason.Missing -> getString(R.string.launch_failed_missing)
                        LaunchFailureReason.Disabled -> getString(R.string.launch_failed_disabled)
                        LaunchFailureReason.Unavailable -> getString(R.string.launch_failed_unavailable)
                    }
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }

                fun launchTarget(id: AppTargetId) {
                    when (val result = appLauncher.launch(id)) {
                        LaunchResult.Success -> Unit
                        is LaunchResult.Failed -> showLaunchFailure(result.reason)
                    }
                }

                fun launchGesture(slot: GestureActionSlot) {
                    val preference = when (slot) {
                        GestureActionSlot.Camera -> gestures.camera
                        GestureActionSlot.Phone -> gestures.phone
                    }
                    when (preference) {
                        GestureTargetPreference.Disabled -> Unit
                        GestureTargetPreference.Unset -> {
                            pickingSlot = slot.name
                            replaceFavoriteId = null
                            surface = StillSurface.Picker
                        }
                        is GestureTargetPreference.Target -> {
                            val stillPresent = catalog.any { it.id == preference.id }
                            if (!stillPresent) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        getString(
                                            R.string.gesture_target_missing,
                                            slot.name.lowercase(),
                                        ),
                                    )
                                }
                                pickingSlot = slot.name
                                replaceFavoriteId = null
                                surface = StillSurface.Picker
                            } else {
                                launchTarget(preference.id)
                            }
                        }
                    }
                }

                fun clearSessionCompleted() {
                    if (sessionCompletedIdList.isNotEmpty()) {
                        sessionCompletedIdList = emptyList()
                    }
                }

                fun resetToCleanHome() {
                    clearSessionCompleted()
                    surface = StillSurface.Home
                    pickingSlot = null
                    replaceFavoriteId = null
                    editingHome = false
                    editingTaskId = null
                    editInitialTitle = ""
                    editTitle = ""
                    addInitialTitle = ""
                    addTitle = ""
                    addFromTasks = false
                    editFromTasks = true
                    searchValue = TextFieldValue()
                }

                fun navigateBack() {
                    when (surface) {
                        StillSurface.AddTask -> {
                            // Draft is already persisted via setDraft; do not clear on back.
                            surface = if (addFromTasks) StillSurface.Tasks else StillSurface.Home
                            addFromTasks = false
                        }
                        StillSurface.EditTask -> {
                            editingTaskId = null
                            editInitialTitle = ""
                            editTitle = ""
                            surface = if (editFromTasks) StillSurface.Tasks else StillSurface.Home
                            editFromTasks = true
                        }
                        StillSurface.Appearance -> {
                            appearanceDraft = committedAppearance
                            surface = StillSurface.Settings
                        }
                        StillSurface.Tasks -> resetToCleanHome()
                        else -> resetToCleanHome()
                    }
                }

                fun shareText(content: String, mimeType: String, chooserTitle: String) {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = mimeType
                        putExtra(Intent.EXTRA_TEXT, content)
                    }
                    startActivity(Intent.createChooser(send, chooserTitle))
                }

                fun openAddTask(fromTasks: Boolean) {
                    addFromTasks = fromTasks
                    addInitialTitle = taskDraft
                    addTitle = taskDraft
                    surface = StillSurface.AddTask
                }

                fun openEditTask(task: TaskEntity, fromTasks: Boolean) {
                    editingTaskId = task.id
                    editInitialTitle = task.title
                    editTitle = task.title
                    editFromTasks = fromTasks
                    surface = StillSurface.EditTask
                }

                LaunchedEffect(homeInvocation) {
                    resetToCleanHome()
                }

                LaunchedEffect(surface) {
                    if (surface != StillSurface.Tasks &&
                        surface != StillSurface.EditTask &&
                        surface != StillSurface.AddTask
                    ) {
                        clearSessionCompleted()
                    }
                }

                pendingImportTasks?.let { tasks ->
                    AlertDialog(
                        onDismissRequest = { pendingImportTasks = null },
                        title = { Text(stringResource(R.string.import_tasks_json)) },
                        text = {
                            Column {
                                TextButton(onClick = {
                                    val toImport = tasks
                                    pendingImportTasks = null
                                    scope.launch {
                                        app.taskRepository.importReplace(toImport)
                                        snackbarHostState.showSnackbar(getString(R.string.import_ok))
                                    }
                                }) {
                                    Text(stringResource(R.string.import_replace))
                                }
                                TextButton(onClick = {
                                    val toImport = tasks
                                    pendingImportTasks = null
                                    scope.launch {
                                        app.taskRepository.importMerge(toImport)
                                        snackbarHostState.showSnackbar(getString(R.string.import_ok))
                                    }
                                }) {
                                    Text(stringResource(R.string.import_merge))
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { pendingImportTasks = null }) {
                                Text(stringResource(R.string.cancel))
                            }
                        },
                    )
                }

                StillAppScaffold(
                    surface = surface,
                    editingHome = editingHome,
                    defaultHomeHeld = defaultHomeHeld,
                    catalog = catalog,
                    gestures = gestures,
                    launcherPrefs = launcherPrefs,
                    searchValue = searchValue,
                    snackbarHostState = snackbarHostState,
                    pickingSlot = pickingSlot?.let { GestureActionSlot.valueOf(it) },
                    is24Hour = DateFormat.is24HourFormat(this),
                    activeTasks = activeTasks,
                    doneTasks = doneTasks,
                    trashTasks = trashTasks,
                    taskPreview = taskPreview,
                    addInitialTitle = addInitialTitle,
                    editInitialTitle = editInitialTitle,
                    sessionCompletedIds = sessionCompletedIds,
                    onSearchChange = { searchValue = it },
                    onOpenApps = { surface = StillSurface.Apps },
                    onOpenSettings = {
                        editingHome = false
                        surface = StillSurface.Settings
                    },
                    onOpenHiddenApps = { surface = StillSurface.HiddenApps },
                    onOpenAppearance = {
                        appearanceDraft = committedAppearance
                        surface = StillSurface.Appearance
                    },
                    onOpenTasks = { surface = StillSurface.Tasks },
                    onBack = { navigateBack() },
                    onRequestDefaultHome = {
                        homeRoleLauncher.launch(HomeRoleHelper.createHomeRoleRequestIntent(this))
                    },
                    onOpenHomeSettings = {
                        startActivity(HomeRoleHelper.createHomeSettingsIntent())
                    },
                    onLaunchTarget = { launchTarget(it) },
                    onCamera = { launchGesture(GestureActionSlot.Camera) },
                    onPhone = { launchGesture(GestureActionSlot.Phone) },
                    onPickSlot = { slot ->
                        pickingSlot = slot.name
                        replaceFavoriteId = null
                        surface = StillSurface.Picker
                    },
                    onDisableSlot = { slot ->
                        scope.launch {
                            app.gesturePreferences.setTarget(slot, GestureTargetPreference.Disabled)
                        }
                    },
                    onSelectTargetForSlot = { slot, targetId ->
                        scope.launch {
                            app.gesturePreferences.setTarget(
                                slot,
                                GestureTargetPreference.Target(targetId),
                            )
                            pickingSlot = null
                            surface = StillSurface.Settings
                        }
                    },
                    onEnterEditHome = {
                        if (launcherPrefs.layoutLocked) {
                            scope.launch {
                                snackbarHostState.showSnackbar(getString(R.string.layout_locked))
                            }
                        } else {
                            editingHome = true
                        }
                    },
                    onExitEditHome = { editingHome = false },
                    onAddFavorite = { id ->
                        scope.launch { app.launcherPreferences.addFavorite(id) }
                    },
                    onRemoveFavorite = { id ->
                        scope.launch { app.launcherPreferences.removeFavorite(id) }
                    },
                    onMoveFavorite = { id, up ->
                        scope.launch { app.launcherPreferences.moveFavorite(id, towardStart = up) }
                    },
                    onReplaceFavorite = { id ->
                        replaceFavoriteId = AppTargetIdCodecKey.encode(id)
                        pickingSlot = null
                        surface = StillSurface.Picker
                    },
                    onSelectReplacementFavorite = { newId ->
                        val oldKey = replaceFavoriteId
                        scope.launch {
                            if (oldKey != null) {
                                val oldId = AppTargetIdCodecKey.decode(oldKey)
                                if (oldId != null) {
                                    val updated = launcherPrefs.favoriteIds.map {
                                        if (it == oldId) newId else it
                                    }.distinct()
                                    app.launcherPreferences.setFavorites(updated)
                                }
                            } else {
                                app.launcherPreferences.addFavorite(newId)
                            }
                            replaceFavoriteId = null
                            surface = StillSurface.Home
                            editingHome = true
                        }
                    },
                    onSetAlias = { id, alias ->
                        scope.launch { app.launcherPreferences.setAlias(id, alias) }
                    },
                    onSetHideMode = { id, mode ->
                        scope.launch { app.launcherPreferences.setHideMode(id, mode) }
                    },
                    onOpenAppInfo = { id ->
                        when (val result = appLauncher.openAppInfo(id)) {
                            LaunchResult.Success -> Unit
                            is LaunchResult.Failed -> showLaunchFailure(result.reason)
                        }
                    },
                    onSetLayoutLocked = { locked ->
                        scope.launch { app.launcherPreferences.setLayoutLocked(locked) }
                    },
                    onSetShowClock = { show ->
                        scope.launch { app.launcherPreferences.setShowClock(show) }
                    },
                    onSetShowDate = { show ->
                        scope.launch { app.launcherPreferences.setShowDate(show) }
                    },
                    onSetFocusSearch = { focus ->
                        scope.launch { app.launcherPreferences.setFocusSearchOnOpenApps(focus) }
                    },
                    onSetTaskPreviewLimit = { limit ->
                        scope.launch { app.launcherPreferences.setTaskPreviewLimit(limit) }
                    },
                    onAddTaskFromHome = { openAddTask(fromTasks = false) },
                    onAddTaskFromTasks = { openAddTask(fromTasks = true) },
                    onOpenTaskFromHome = { openEditTask(it, fromTasks = false) },
                    onOpenTaskFromTasks = { openEditTask(it, fromTasks = true) },
                    onTogglePreviewTaskComplete = { task, completed ->
                        scope.launch { app.taskRepository.setCompleted(task.id, completed) }
                    },
                    onToggleTaskComplete = { task, completed ->
                        scope.launch {
                            app.taskRepository.setCompleted(task.id, completed)
                            sessionCompletedIdList = if (completed) {
                                sessionCompletedIdList + task.id
                            } else {
                                sessionCompletedIdList - task.id
                            }
                        }
                    },
                    onMoveTask = { id, towardStart ->
                        scope.launch { app.taskRepository.moveActive(id, towardStart) }
                    },
                    onTrashTask = { task ->
                        scope.launch {
                            app.taskRepository.moveToTrash(task.id)
                            sessionCompletedIdList = sessionCompletedIdList - task.id
                        }
                    },
                    onRestoreDone = { task ->
                        scope.launch {
                            app.taskRepository.setCompleted(task.id, false)
                            sessionCompletedIdList = sessionCompletedIdList - task.id
                        }
                    },
                    onRestoreTrash = { task ->
                        scope.launch { app.taskRepository.restoreFromTrash(task.id) }
                    },
                    onDeleteForever = { task ->
                        scope.launch { app.taskRepository.permanentlyDelete(task.id) }
                    },
                    onEmptyTrash = {
                        scope.launch { app.taskRepository.permanentlyDeleteAllTrash() }
                    },
                    onExportJson = {
                        scope.launch {
                            val json = TaskBackup.toJson(app.taskRepository.exportAll())
                            shareText(json, "application/json", getString(R.string.export_tasks_json))
                        }
                    },
                    onExportMarkdown = {
                        scope.launch {
                            val md = TaskBackup.toMarkdown(app.taskRepository.exportAll())
                            shareText(md, "text/markdown", getString(R.string.export_tasks_markdown))
                        }
                    },
                    onImportJson = {
                        importLauncher.launch(arrayOf("application/json", "text/*"))
                    },
                    onDraftChange = { text ->
                        addTitle = text
                        scope.launch { app.taskRepository.setDraft(text) }
                    },
                    onEditTitleChange = { editTitle = it },
                    onSaveAddTask = {
                        scope.launch {
                            when (app.taskRepository.addTask(addTitle)) {
                                is AddTaskResult.Created,
                                AddTaskResult.DuplicateIgnored,
                                -> {
                                    surface = if (addFromTasks) {
                                        StillSurface.Tasks
                                    } else {
                                        StillSurface.Home
                                    }
                                    addFromTasks = false
                                    addInitialTitle = ""
                                    addTitle = ""
                                }
                                AddTaskResult.BlankTitle -> {
                                    snackbarHostState.showSnackbar(
                                        getString(R.string.task_blank_title),
                                    )
                                }
                            }
                        }
                    },
                    onSaveEditTask = {
                        val id = editingTaskId ?: return@StillAppScaffold
                        scope.launch {
                            val ok = app.taskRepository.updateTitle(id, editTitle)
                            if (!ok) {
                                snackbarHostState.showSnackbar(getString(R.string.task_blank_title))
                            } else {
                                editingTaskId = null
                                editInitialTitle = ""
                                editTitle = ""
                                surface = if (editFromTasks) {
                                    StillSurface.Tasks
                                } else {
                                    StillSurface.Home
                                }
                                editFromTasks = true
                            }
                        }
                    },
                    appearanceDraft = appearanceDraft,
                    importedFonts = importedFonts,
                    appearanceContrastWarning = resolved.contrastWarning,
                    appearanceFontLoadFailed = resolved.fontLoadFailed,
                    hasUnsavedAppearanceChanges = hasUnsavedAppearanceChanges,
                    onAppearanceDraftChange = { appearanceDraft = it },
                    onApplyAppearance = {
                        scope.launch {
                            app.appearancePreferences.commit(appearanceDraft)
                            surface = StillSurface.Settings
                        }
                    },
                    onCancelAppearance = {
                        appearanceDraft = committedAppearance
                        surface = StillSurface.Settings
                    },
                    onResetAppearance = {
                        appearanceDraft = AppearanceSettings()
                    },
                    onImportFont = {
                        fontImportLauncher.launch(
                            arrayOf("font/*", "application/octet-stream"),
                        )
                    },
                    onDeleteImportedFont = { id ->
                        fontImporter.delete(id)
                        importedFontsTick += 1
                        if (appearanceDraft.fontSource == FontSource.Imported &&
                            appearanceDraft.importedFontId == id
                        ) {
                            appearanceDraft = appearanceDraft.copy(
                                fontSource = FontSource.System,
                                importedFontId = null,
                            )
                        }
                        if (committedAppearance.fontSource == FontSource.Imported &&
                            committedAppearance.importedFontId == id
                        ) {
                            scope.launch {
                                app.appearancePreferences.commit(
                                    committedAppearance.copy(
                                        fontSource = FontSource.System,
                                        importedFontId = null,
                                    ),
                                )
                            }
                        }
                    },
                    onExportAppearancePreset = {
                        shareText(
                            ThemePresetCodec.toJson(appearanceDraft),
                            "application/json",
                            getString(R.string.appearance_export_preset),
                        )
                    },
                    onImportAppearancePreset = {
                        themePresetImportLauncher.launch(
                            arrayOf("application/json", "text/*"),
                        )
                    },
                    replacingFavorite = replaceFavoriteId != null,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        homeInvocation += 1
    }

    override fun onResume() {
        super.onResume()
        defaultHomeHeld = HomeRoleHelper.isDefaultHome(this)
        val app = application as StillApplication
        app.appCatalog.refresh()
        lifecycleScope.launch {
            app.taskRepository.purgeExpiredTrash()
        }
    }
}

/** Local encode helpers for saveable favorite-replace keys (same codec as prefs). */
private object AppTargetIdCodecKey {
    fun encode(id: AppTargetId): String = app.still.prefs.AppTargetIdCodec.encode(id)
    fun decode(raw: String): AppTargetId? = app.still.prefs.AppTargetIdCodec.decode(raw)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StillAppScaffold(
    surface: StillSurface,
    editingHome: Boolean,
    defaultHomeHeld: Boolean,
    catalog: List<app.still.launcher.AppTarget>,
    gestures: GesturePreferences,
    launcherPrefs: LauncherPreferences,
    searchValue: TextFieldValue,
    snackbarHostState: SnackbarHostState,
    pickingSlot: GestureActionSlot?,
    is24Hour: Boolean,
    activeTasks: List<TaskEntity>,
    doneTasks: List<TaskEntity>,
    trashTasks: List<TaskEntity>,
    taskPreview: List<TaskEntity>,
    addInitialTitle: String,
    editInitialTitle: String,
    sessionCompletedIds: Set<String>,
    onSearchChange: (TextFieldValue) -> Unit,
    onOpenApps: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHiddenApps: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenTasks: () -> Unit,
    onBack: () -> Unit,
    onRequestDefaultHome: () -> Unit,
    onOpenHomeSettings: () -> Unit,
    onLaunchTarget: (AppTargetId) -> Unit,
    onCamera: () -> Unit,
    onPhone: () -> Unit,
    onPickSlot: (GestureActionSlot) -> Unit,
    onDisableSlot: (GestureActionSlot) -> Unit,
    onSelectTargetForSlot: (GestureActionSlot, AppTargetId) -> Unit,
    onEnterEditHome: () -> Unit,
    onExitEditHome: () -> Unit,
    onAddFavorite: (AppTargetId) -> Unit,
    onRemoveFavorite: (AppTargetId) -> Unit,
    onMoveFavorite: (AppTargetId, Boolean) -> Unit,
    onReplaceFavorite: (AppTargetId) -> Unit,
    onSelectReplacementFavorite: (AppTargetId) -> Unit,
    onSetAlias: (AppTargetId, String?) -> Unit,
    onSetHideMode: (AppTargetId, HideMode) -> Unit,
    onOpenAppInfo: (AppTargetId) -> Unit,
    onSetLayoutLocked: (Boolean) -> Unit,
    onSetShowClock: (Boolean) -> Unit,
    onSetShowDate: (Boolean) -> Unit,
    onSetFocusSearch: (Boolean) -> Unit,
    onSetTaskPreviewLimit: (Int) -> Unit,
    onAddTaskFromHome: () -> Unit,
    onAddTaskFromTasks: () -> Unit,
    onOpenTaskFromHome: (TaskEntity) -> Unit,
    onOpenTaskFromTasks: (TaskEntity) -> Unit,
    onTogglePreviewTaskComplete: (TaskEntity, Boolean) -> Unit,
    onToggleTaskComplete: (TaskEntity, Boolean) -> Unit,
    onMoveTask: (String, Boolean) -> Unit,
    onTrashTask: (TaskEntity) -> Unit,
    onRestoreDone: (TaskEntity) -> Unit,
    onRestoreTrash: (TaskEntity) -> Unit,
    onDeleteForever: (TaskEntity) -> Unit,
    onEmptyTrash: () -> Unit,
    onExportJson: () -> Unit,
    onExportMarkdown: () -> Unit,
    onImportJson: () -> Unit,
    onDraftChange: (String) -> Unit,
    onEditTitleChange: (String) -> Unit,
    onSaveAddTask: () -> Unit,
    onSaveEditTask: () -> Unit,
    appearanceDraft: AppearanceSettings,
    importedFonts: List<ImportedFontInfo>,
    appearanceContrastWarning: Boolean,
    appearanceFontLoadFailed: Boolean,
    hasUnsavedAppearanceChanges: Boolean,
    onAppearanceDraftChange: (AppearanceSettings) -> Unit,
    onApplyAppearance: () -> Unit,
    onCancelAppearance: () -> Unit,
    onResetAppearance: () -> Unit,
    onImportFont: () -> Unit,
    onDeleteImportedFont: (String) -> Unit,
    onExportAppearancePreset: () -> Unit,
    onImportAppearancePreset: () -> Unit,
    replacingFavorite: Boolean,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val favorites = remember(launcherPrefs, catalog) {
        CatalogOverlay.resolveFavorites(launcherPrefs, catalog)
    }
    val browseCatalog = remember(catalog, launcherPrefs.hideModes) {
        AppVisibility.filterBrowse(catalog, launcherPrefs.hideModes)
    }
    val searchCatalog = remember(catalog, launcherPrefs.hideModes) {
        AppVisibility.filterSearch(catalog, launcherPrefs.hideModes)
    }

    BackHandler(enabled = surface != StillSurface.Home || editingHome) {
        when {
            editingHome && surface == StillSurface.Home -> onExitEditHome()
            surface == StillSurface.Apps -> {
                keyboard?.hide()
                onBack()
            }
            surface != StillSurface.Home -> onBack()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (surface) {
                            StillSurface.Home -> if (editingHome) {
                                stringResource(R.string.edit_home)
                            } else {
                                stringResource(R.string.home_title)
                            }
                            StillSurface.Apps -> stringResource(R.string.apps_title)
                            StillSurface.Settings -> stringResource(R.string.settings_title)
                            StillSurface.Appearance -> stringResource(R.string.appearance_title)
                            StillSurface.HiddenApps -> stringResource(R.string.hidden_apps)
                            StillSurface.Tasks -> stringResource(R.string.tasks_title)
                            StillSurface.AddTask -> stringResource(R.string.add_task)
                            StillSurface.EditTask -> stringResource(R.string.edit_task)
                            StillSurface.Picker -> when {
                                replacingFavorite -> stringResource(R.string.recover_target)
                                pickingSlot == GestureActionSlot.Camera ->
                                    stringResource(R.string.pick_camera_target)
                                pickingSlot == GestureActionSlot.Phone ->
                                    stringResource(R.string.pick_phone_target)
                                else -> stringResource(R.string.choose_app)
                            }
                        },
                    )
                },
                navigationIcon = {
                    if (surface != StillSurface.Home || editingHome) {
                        TextButton(onClick = {
                            keyboard?.hide()
                            if (editingHome && surface == StillSurface.Home) {
                                onExitEditHome()
                            } else {
                                onBack()
                            }
                        }) {
                            Text(
                                if (editingHome && surface == StillSurface.Home) {
                                    stringResource(R.string.done)
                                } else {
                                    stringResource(R.string.back)
                                },
                            )
                        }
                    }
                },
                actions = {
                    if (surface == StillSurface.Apps) {
                        TextButton(onClick = onOpenSettings) {
                            Text(stringResource(R.string.open_settings))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when (surface) {
            StillSurface.Home -> HomeSurface(
                modifier = contentModifier,
                defaultHomeHeld = defaultHomeHeld,
                favorites = favorites,
                launcherPrefs = launcherPrefs,
                editingHome = editingHome,
                is24Hour = is24Hour,
                taskPreview = taskPreview,
                taskPreviewLimit = launcherPrefs.taskPreviewLimit,
                onOpenApps = onOpenApps,
                onOpenSettings = onOpenSettings,
                onOpenTasks = onOpenTasks,
                onAddTask = onAddTaskFromHome,
                onTogglePreviewTaskComplete = onTogglePreviewTaskComplete,
                onOpenTask = onOpenTaskFromHome,
                onCamera = onCamera,
                onPhone = onPhone,
                onRequestDefaultHome = onRequestDefaultHome,
                onLaunchFavorite = onLaunchTarget,
                onEnterEditHome = onEnterEditHome,
                onExitEditHome = onExitEditHome,
                onRemoveFavorite = onRemoveFavorite,
                onMoveFavorite = onMoveFavorite,
                onReplaceFavorite = onReplaceFavorite,
            )
            StillSurface.Apps -> AppsSurface(
                modifier = contentModifier,
                catalog = searchCatalog,
                favoriteIds = launcherPrefs.favoriteIds.toSet(),
                hideModes = launcherPrefs.hideModes,
                searchValue = searchValue,
                focusSearch = launcherPrefs.focusSearchOnOpenApps,
                onSearchChange = onSearchChange,
                onLaunchTarget = onLaunchTarget,
                onAddFavorite = onAddFavorite,
                onRemoveFavorite = onRemoveFavorite,
                onSetAlias = onSetAlias,
                onSetHideMode = onSetHideMode,
                onOpenAppInfo = onOpenAppInfo,
            )
            StillSurface.Settings -> SettingsSurface(
                modifier = contentModifier,
                defaultHomeHeld = defaultHomeHeld,
                gestures = gestures,
                catalog = catalog,
                launcherPrefs = launcherPrefs,
                onRequestDefaultHome = onRequestDefaultHome,
                onOpenHomeSettings = onOpenHomeSettings,
                onPickSlot = onPickSlot,
                onDisableSlot = onDisableSlot,
                onOpenConfigured = { slot ->
                    when (slot) {
                        GestureActionSlot.Camera -> onCamera()
                        GestureActionSlot.Phone -> onPhone()
                    }
                },
                onOpenHiddenApps = onOpenHiddenApps,
                onOpenAppearance = onOpenAppearance,
                onSetLayoutLocked = onSetLayoutLocked,
                onSetShowClock = onSetShowClock,
                onSetShowDate = onSetShowDate,
                onSetFocusSearch = onSetFocusSearch,
                onSetTaskPreviewLimit = onSetTaskPreviewLimit,
            )
            StillSurface.Appearance -> AppearanceSurface(
                modifier = contentModifier,
                draft = appearanceDraft,
                importedFonts = importedFonts,
                contrastWarning = appearanceContrastWarning,
                fontLoadFailed = appearanceFontLoadFailed,
                hasUnsavedChanges = hasUnsavedAppearanceChanges,
                onDraftChange = onAppearanceDraftChange,
                onApply = onApplyAppearance,
                onCancel = onCancelAppearance,
                onReset = onResetAppearance,
                onImportFont = onImportFont,
                onDeleteImportedFont = onDeleteImportedFont,
                onExportPreset = onExportAppearancePreset,
                onImportPreset = onImportAppearancePreset,
            )
            StillSurface.HiddenApps -> HiddenAppsSurface(
                modifier = contentModifier,
                catalog = catalog,
                hideModes = launcherPrefs.hideModes,
                onSetHideMode = onSetHideMode,
            )
            StillSurface.Picker -> PickerSurface(
                modifier = contentModifier,
                catalog = browseCatalog,
                onSelect = { id ->
                    val slot = pickingSlot
                    if (replacingFavorite) {
                        onSelectReplacementFavorite(id)
                    } else if (slot != null) {
                        onSelectTargetForSlot(slot, id)
                    }
                },
            )
            StillSurface.Tasks -> TasksSurface(
                modifier = contentModifier,
                active = activeTasks,
                done = doneTasks,
                trash = trashTasks,
                sessionCompletedIds = sessionCompletedIds,
                onAddTask = onAddTaskFromTasks,
                onToggleComplete = onToggleTaskComplete,
                onEdit = onOpenTaskFromTasks,
                onMove = onMoveTask,
                onTrash = onTrashTask,
                onRestoreDone = onRestoreDone,
                onRestoreTrash = onRestoreTrash,
                onDeleteForever = onDeleteForever,
                onEmptyTrash = onEmptyTrash,
                onExportJson = onExportJson,
                onExportMarkdown = onExportMarkdown,
                onImportJson = onImportJson,
            )
            StillSurface.AddTask -> AddOrEditTaskSurface(
                modifier = contentModifier,
                initialTitle = addInitialTitle,
                isEdit = false,
                onTitleChange = onDraftChange,
                onSave = onSaveAddTask,
                onCancel = onBack,
            )
            StillSurface.EditTask -> AddOrEditTaskSurface(
                modifier = contentModifier,
                initialTitle = editInitialTitle,
                isEdit = true,
                onTitleChange = onEditTitleChange,
                onSave = onSaveEditTask,
                onCancel = onBack,
            )
        }
    }
}
