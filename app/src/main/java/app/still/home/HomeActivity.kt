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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import app.still.BuildConfig
import app.still.R
import app.still.StillApplication
import app.still.appearance.AppearanceSettings
import app.still.appearance.FontSource
import app.still.appearance.ImportedFontInfo
import app.still.appearance.ThemePresetCodec
import app.still.appearance.rememberResolvedAppearance
import app.still.launcher.AppLauncher
import app.still.launcher.AppShortcutItem
import app.still.launcher.AppTarget
import app.still.launcher.AppTargetId
import app.still.launcher.AppVisibility
import app.still.launcher.CatalogOverlay
import app.still.launcher.HideMode
import app.still.launcher.LaunchFailureReason
import app.still.launcher.LaunchResult
import app.still.launcher.SystemAppIntents
import app.still.prefs.GestureAction
import app.still.prefs.GestureDirection
import app.still.prefs.GesturePreferences
import app.still.prefs.HomeVerticalPlacement
import app.still.prefs.LauncherPreferences
import app.still.settings.HomeRoleHelper
import app.still.tasks.AddTaskResult
import app.still.tasks.TaskBackup
import app.still.tasks.TaskEntity
import app.still.ui.StillSpacing
import app.still.ui.StillTheme
import app.still.widgets.RememberWidgetHostListening
import app.still.widgets.StillWidgetHostController
import app.still.widgets.WidgetBindFlow
import app.still.widgets.WidgetPlacement
import app.still.widgets.WidgetProviderOption
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class StillSurface {
    Home,
    Apps,
    Settings,
    Appearance,
    HiddenApps,
    Widgets,
    Gestures,
    GestureActionPicker,
    Onboarding,
    Picker,
    Tasks,
    AddTask,
    EditTask,
}

private const val PICKER_GESTURE_APP = "gesture_app"
private const val PICKER_CLOCK_APP = "clock_app"
private const val PICKER_DATE_APP = "date_app"
private const val PICKER_FAVORITE_REPLACE = "favorite_replace"
private const val PICKER_ADD_FAVORITE = "add_favorite"

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
                    surface == StillSurface.HiddenApps ||
                    surface == StillSurface.Widgets ||
                    surface == StillSurface.Gestures ||
                    surface == StillSurface.GestureActionPicker ||
                    surface == StillSurface.Onboarding

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
                RememberWidgetHostListening(app.widgetHost)

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
                val widgetPlacements by app.widgetPlacements.placements.collectAsStateWithLifecycle(
                    emptyList(),
                )

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
                var pickingGestureDirection by rememberSaveable { mutableStateOf<String?>(null) }
                var pickerPurpose by rememberSaveable { mutableStateOf<String?>(null) }
                var replaceFavoriteId by rememberSaveable { mutableStateOf<String?>(null) }
                var renameFavoriteId by rememberSaveable { mutableStateOf<String?>(null) }
                var renameDraft by rememberSaveable(stateSaver = TextFieldValue.Saver) {
                    mutableStateOf(TextFieldValue())
                }
                var onboardingFromSettings by rememberSaveable { mutableStateOf(false) }
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
                var pickingWidgetProvider by rememberSaveable { mutableStateOf(false) }
                var pendingWidgetId by rememberSaveable { mutableIntStateOf(0) }
                var pendingWidgetProvider by rememberSaveable { mutableStateOf<String?>(null) }
                val widgetProviders = remember(surface) {
                    if (surface == StillSurface.Widgets) {
                        app.widgetHost.listProviders()
                    } else {
                        emptyList()
                    }
                }

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

                fun cancelPendingWidget(messageRes: Int) {
                    if (pendingWidgetId > 0) {
                        app.widgetHost.deleteId(pendingWidgetId)
                    }
                    pendingWidgetId = 0
                    pendingWidgetProvider = null
                    scope.launch {
                        snackbarHostState.showSnackbar(getString(messageRes))
                    }
                }

                val widgetStepHandler = remember {
                    object {
                        var handle: (WidgetBindFlow.NextStep) -> Unit = {}
                    }
                }

                val bindLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult(),
                ) { result ->
                    val provider = pendingWidgetProvider
                    if (provider == null || pendingWidgetId <= 0) {
                        cancelPendingWidget(R.string.widget_bind_failed)
                        return@rememberLauncherForActivityResult
                    }
                    val step = WidgetBindFlow.afterBindResult(
                        app.widgetHost,
                        WidgetBindFlow.Pending(pendingWidgetId, provider),
                        result.resultCode,
                    )
                    when (step) {
                        is WidgetBindFlow.NextStep.Failed ->
                            cancelPendingWidget(R.string.widget_bind_cancelled)
                        else -> widgetStepHandler.handle(step)
                    }
                }

                val configureLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult(),
                ) { result ->
                    val provider = pendingWidgetProvider
                    if (provider == null || pendingWidgetId <= 0) {
                        cancelPendingWidget(R.string.widget_configure_cancelled)
                        return@rememberLauncherForActivityResult
                    }
                    val step = WidgetBindFlow.afterConfigureResult(
                        WidgetBindFlow.Pending(pendingWidgetId, provider),
                        result.resultCode,
                    )
                    when (step) {
                        is WidgetBindFlow.NextStep.Failed ->
                            cancelPendingWidget(R.string.widget_configure_cancelled)
                        else -> widgetStepHandler.handle(step)
                    }
                }

                fun handleWidgetNextStep(step: WidgetBindFlow.NextStep) {
                    when (step) {
                        is WidgetBindFlow.NextStep.LaunchBind -> {
                            bindLauncher.launch(step.intent)
                        }
                        is WidgetBindFlow.NextStep.LaunchConfigure -> {
                            configureLauncher.launch(step.intent)
                        }
                        is WidgetBindFlow.NextStep.Complete -> {
                            scope.launch {
                                app.widgetPlacements.add(step.placement)
                                pendingWidgetId = 0
                                pendingWidgetProvider = null
                                pickingWidgetProvider = false
                                surface = StillSurface.Widgets
                            }
                        }
                        is WidgetBindFlow.NextStep.Failed -> {
                            if (step.releaseId > 0 && step.releaseId != pendingWidgetId) {
                                app.widgetHost.deleteId(step.releaseId)
                            }
                            cancelPendingWidget(R.string.widget_bind_failed)
                        }
                    }
                }
                widgetStepHandler.handle = ::handleWidgetNextStep

                fun startAddWidget(option: WidgetProviderOption) {
                    val (pending, step) = WidgetBindFlow.begin(
                        app.widgetHost,
                        option.providerFlattened,
                    )
                    pendingWidgetId = pending.appWidgetId
                    pendingWidgetProvider = pending.providerFlattened
                    handleWidgetNextStep(step)
                }

                fun removeWidget(id: Int) {
                    app.widgetHost.deleteId(id)
                    scope.launch { app.widgetPlacements.remove(id) }
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

                fun runGesture(direction: GestureDirection) {
                    when (val action = gestures.action(direction)) {
                        GestureAction.None -> Unit
                        GestureAction.Apps -> surface = StillSurface.Apps
                        GestureAction.Tasks -> surface = StillSurface.Tasks
                        GestureAction.Notifications -> {
                            if (!SystemAppIntents.expandNotifications(this@HomeActivity)) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        getString(R.string.shade_unavailable),
                                    )
                                }
                            }
                        }
                        GestureAction.QuickSettings -> {
                            if (!SystemAppIntents.expandQuickSettings(this@HomeActivity)) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        getString(R.string.shade_unavailable),
                                    )
                                }
                            }
                        }
                        GestureAction.Lock -> {
                            if (!SystemAppIntents.lockScreen(this@HomeActivity)) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        getString(R.string.lock_unavailable),
                                    )
                                }
                            }
                        }
                        GestureAction.OpenCamera -> {
                            if (!SystemAppIntents.openDefaultCamera(this@HomeActivity)) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        getString(R.string.app_unavailable),
                                    )
                                }
                            }
                        }
                        GestureAction.OpenPhone -> {
                            if (!SystemAppIntents.openDefaultPhone(this@HomeActivity)) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        getString(R.string.app_unavailable),
                                    )
                                }
                            }
                        }
                        is GestureAction.App -> launchTarget(action.id)
                    }
                }

                fun finishOnboarding() {
                    scope.launch {
                        app.launcherPreferences.setOnboardingCompleted(true)
                    }
                    onboardingFromSettings = false
                    surface = StillSurface.Home
                }

                fun clearSessionCompleted() {
                    if (sessionCompletedIdList.isNotEmpty()) {
                        sessionCompletedIdList = emptyList()
                    }
                }

                fun resetToCleanHome() {
                    clearSessionCompleted()
                    surface = StillSurface.Home
                    pickingGestureDirection = null
                    pickerPurpose = null
                    replaceFavoriteId = null
                    renameFavoriteId = null
                    editingHome = false
                    editingTaskId = null
                    editInitialTitle = ""
                    editTitle = ""
                    addInitialTitle = ""
                    addTitle = ""
                    addFromTasks = false
                    editFromTasks = true
                    pickingWidgetProvider = false
                    onboardingFromSettings = false
                    searchValue = TextFieldValue()
                }

                fun navigateBack() {
                    when (surface) {
                        StillSurface.AddTask -> {
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
                        StillSurface.Widgets -> {
                            if (pickingWidgetProvider) {
                                pickingWidgetProvider = false
                            } else {
                                surface = StillSurface.Settings
                            }
                        }
                        StillSurface.Gestures -> surface = StillSurface.Settings
                        StillSurface.GestureActionPicker -> {
                            pickingGestureDirection = null
                            surface = StillSurface.Gestures
                        }
                        StillSurface.Onboarding -> {
                            if (onboardingFromSettings) {
                                onboardingFromSettings = false
                                surface = StillSurface.Settings
                            } else {
                                finishOnboarding()
                            }
                        }
                        StillSurface.Picker -> {
                            when (pickerPurpose) {
                                PICKER_GESTURE_APP -> {
                                    pickerPurpose = null
                                    surface = StillSurface.GestureActionPicker
                                }
                                PICKER_CLOCK_APP, PICKER_DATE_APP -> {
                                    pickerPurpose = null
                                    surface = StillSurface.Settings
                                }
                                PICKER_FAVORITE_REPLACE, PICKER_ADD_FAVORITE -> {
                                    pickerPurpose = null
                                    replaceFavoriteId = null
                                    surface = StillSurface.Home
                                    editingHome = true
                                }
                                else -> resetToCleanHome()
                            }
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
                    if (homeInvocation > 0) {
                        resetToCleanHome()
                    }
                }

                LaunchedEffect(Unit) {
                    val prefs = app.launcherPreferences.preferences.first()
                    if (!prefs.onboardingCompleted) {
                        surface = StillSurface.Onboarding
                    }
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

                renameFavoriteId?.let { encodedId ->
                    val id = AppTargetIdCodecKey.decode(encodedId)
                    val target = id?.let { targetId -> catalog.find { it.id == targetId } }
                    if (id == null) {
                        renameFavoriteId = null
                    } else {
                        AlertDialog(
                            onDismissRequest = { renameFavoriteId = null },
                            title = { Text(stringResource(R.string.rename_dialog_title)) },
                            text = {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(StillSpacing.xs),
                                ) {
                                    Text(stringResource(R.string.rename_dialog_hint))
                                    if (target != null) {
                                        Text(
                                            stringResource(
                                                R.string.original_label,
                                                target.originalLabel,
                                            ),
                                        )
                                    }
                                    OutlinedTextField(
                                        value = renameDraft,
                                        onValueChange = { renameDraft = it },
                                        singleLine = true,
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    val alias = renameDraft.text.trim().ifBlank { null }
                                    scope.launch {
                                        app.launcherPreferences.setAlias(id, alias)
                                    }
                                    renameFavoriteId = null
                                }) {
                                    Text(stringResource(R.string.apply))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { renameFavoriteId = null }) {
                                    Text(stringResource(R.string.cancel))
                                }
                            },
                        )
                    }
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
                    pickerPurpose = pickerPurpose,
                    is24Hour = DateFormat.is24HourFormat(this),
                    versionName = BuildConfig.VERSION_NAME,
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
                    onOpenWidgets = {
                        pickingWidgetProvider = false
                        surface = StillSurface.Widgets
                    },
                    onOpenGestures = { surface = StillSurface.Gestures },
                    onOpenOnboarding = {
                        onboardingFromSettings = true
                        surface = StillSurface.Onboarding
                    },
                    onOpenTasks = { surface = StillSurface.Tasks },
                    onBack = { navigateBack() },
                    onFinishOnboarding = { finishOnboarding() },
                    onRequestDefaultHome = {
                        homeRoleLauncher.launch(HomeRoleHelper.createHomeRoleRequestIntent(this))
                    },
                    onOpenHomeSettings = {
                        startActivity(HomeRoleHelper.createHomeSettingsIntent())
                    },
                    onLaunchTarget = { launchTarget(it) },
                    onGesture = { direction -> runGesture(direction) },
                    onTapClock = {
                        if (!launcherPrefs.clockTapEnabled) return@StillAppScaffold
                        val clockId = launcherPrefs.clockAppId
                        if (clockId != null) {
                            launchTarget(clockId)
                        } else if (!SystemAppIntents.openDefaultClock(this)) {
                            scope.launch {
                                snackbarHostState.showSnackbar(getString(R.string.app_unavailable))
                            }
                        }
                    },
                    onTapDate = {
                        if (!launcherPrefs.dateTapEnabled) return@StillAppScaffold
                        val dateId = launcherPrefs.dateAppId
                        if (dateId != null) {
                            launchTarget(dateId)
                        } else if (!SystemAppIntents.openDefaultCalendar(this)) {
                            scope.launch {
                                snackbarHostState.showSnackbar(getString(R.string.app_unavailable))
                            }
                        }
                    },
                    onPickGestureAction = { direction ->
                        pickingGestureDirection = direction.name
                        surface = StillSurface.GestureActionPicker
                    },
                    onSelectGestureAction = { action ->
                        val directionName = pickingGestureDirection
                        if (directionName != null) {
                            val direction = runCatching {
                                GestureDirection.valueOf(directionName)
                            }.getOrNull()
                            if (direction != null) {
                                scope.launch {
                                    app.gesturePreferences.setAction(direction, action)
                                }
                            }
                        }
                        pickingGestureDirection = null
                        surface = StillSurface.Gestures
                    },
                    onChooseGestureInstalledApp = {
                        pickerPurpose = PICKER_GESTURE_APP
                        surface = StillSurface.Picker
                    },
                    onPickClockApp = {
                        pickerPurpose = PICKER_CLOCK_APP
                        surface = StillSurface.Picker
                    },
                    onPickDateApp = {
                        pickerPurpose = PICKER_DATE_APP
                        surface = StillSurface.Picker
                    },
                    onClearClockApp = {
                        scope.launch { app.launcherPreferences.setClockApp(null) }
                    },
                    onClearDateApp = {
                        scope.launch { app.launcherPreferences.setDateApp(null) }
                    },
                    workProfilePaused = app.appShortcuts.workProfilesPaused(),
                    hasWorkProfile = app.appShortcuts.hasWorkProfile(),
                    shortcutsFor = { app.appShortcuts.listShortcuts(it) },
                    onLaunchShortcut = { item ->
                        when (app.appShortcuts.startShortcut(item)) {
                            LaunchResult.Success -> Unit
                            is LaunchResult.Failed -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        getString(R.string.shortcut_failed),
                                    )
                                }
                            }
                        }
                    },
                    widgetPlacements = widgetPlacements,
                    widgetHost = app.widgetHost,
                    widgetProviders = widgetProviders,
                    pickingWidgetProvider = pickingWidgetProvider,
                    onRemoveWidget = { removeWidget(it) },
                    onUpdateWidget = { placement ->
                        scope.launch { app.widgetPlacements.update(placement) }
                    },
                    onAddWidget = {
                        pickingWidgetProvider = true
                        surface = StillSurface.Widgets
                    },
                    onStartPickWidget = { pickingWidgetProvider = true },
                    onCancelPickWidget = { pickingWidgetProvider = false },
                    onSelectWidgetProvider = { startAddWidget(it) },
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
                    onRenameFavorite = { id ->
                        val target = catalog.find { it.id == id }
                        renameDraft = TextFieldValue(target?.alias ?: target?.originalLabel ?: "")
                        renameFavoriteId = AppTargetIdCodecKey.encode(id)
                    },
                    onHideFavorite = { id ->
                        scope.launch {
                            app.launcherPreferences.setHideMode(id, HideMode.FromBrowsing)
                        }
                    },
                    onUninstallFavorite = { id ->
                        SystemAppIntents.requestUninstall(this, id.packageName)
                    },
                    onUninstall = { id ->
                        SystemAppIntents.requestUninstall(this, id.packageName)
                    },
                    onReplaceFavorite = { id ->
                        replaceFavoriteId = AppTargetIdCodecKey.encode(id)
                        pickerPurpose = PICKER_FAVORITE_REPLACE
                        surface = StillSurface.Picker
                    },
                    onAddFavoritePick = {
                        replaceFavoriteId = null
                        pickerPurpose = PICKER_ADD_FAVORITE
                        surface = StillSurface.Picker
                    },
                    onSelectPickerTarget = { newId ->
                        when (pickerPurpose) {
                            PICKER_GESTURE_APP -> {
                                val directionName = pickingGestureDirection
                                val direction = directionName?.let {
                                    runCatching { GestureDirection.valueOf(it) }.getOrNull()
                                }
                                if (direction != null) {
                                    scope.launch {
                                        app.gesturePreferences.setAction(
                                            direction,
                                            GestureAction.App(newId),
                                        )
                                    }
                                }
                                pickerPurpose = null
                                pickingGestureDirection = null
                                surface = StillSurface.Gestures
                            }
                            PICKER_CLOCK_APP -> {
                                scope.launch { app.launcherPreferences.setClockApp(newId) }
                                pickerPurpose = null
                                surface = StillSurface.Settings
                            }
                            PICKER_DATE_APP -> {
                                scope.launch { app.launcherPreferences.setDateApp(newId) }
                                pickerPurpose = null
                                surface = StillSurface.Settings
                            }
                            PICKER_FAVORITE_REPLACE -> {
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
                                    }
                                    replaceFavoriteId = null
                                    pickerPurpose = null
                                    surface = StillSurface.Home
                                    editingHome = true
                                }
                            }
                            PICKER_ADD_FAVORITE -> {
                                scope.launch {
                                    app.launcherPreferences.addFavorite(newId)
                                    replaceFavoriteId = null
                                    pickerPurpose = null
                                    surface = StillSurface.Home
                                    editingHome = true
                                }
                            }
                            else -> Unit
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
                    onToggleLayoutLock = {
                        scope.launch {
                            app.launcherPreferences.setLayoutLocked(!launcherPrefs.layoutLocked)
                        }
                    },
                    onDismissEmptyHint = {
                        scope.launch {
                            app.launcherPreferences.setFavoritesEmptyHintDismissed(true)
                        }
                    },
                    onSetShowClock = { show ->
                        scope.launch { app.launcherPreferences.setShowClock(show) }
                    },
                    onSetShowDate = { show ->
                        scope.launch { app.launcherPreferences.setShowDate(show) }
                    },
                    onSetClockTapEnabled = { enabled ->
                        scope.launch { app.launcherPreferences.setClockTapEnabled(enabled) }
                    },
                    onSetDateTapEnabled = { enabled ->
                        scope.launch { app.launcherPreferences.setDateTapEnabled(enabled) }
                    },
                    onSetFocusSearch = { focus ->
                        scope.launch { app.launcherPreferences.setFocusSearchOnOpenApps(focus) }
                    },
                    onSetTaskPreviewLimit = { limit ->
                        scope.launch { app.launcherPreferences.setTaskPreviewLimit(limit) }
                    },
                    onSetHaptic = { enabled ->
                        scope.launch { app.launcherPreferences.setHapticFeedback(enabled) }
                    },
                    onSetVerticalPlacement = { placement ->
                        scope.launch {
                            app.launcherPreferences.setHomeVerticalPlacement(placement)
                        }
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
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        homeInvocation += 1
    }

    override fun onStart() {
        super.onStart()
        val app = application as StillApplication
        app.widgetHost.startListening()
    }

    override fun onStop() {
        val app = application as StillApplication
        app.widgetHost.stopListening()
        super.onStop()
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
    catalog: List<AppTarget>,
    gestures: GesturePreferences,
    launcherPrefs: LauncherPreferences,
    searchValue: TextFieldValue,
    snackbarHostState: SnackbarHostState,
    pickerPurpose: String?,
    is24Hour: Boolean,
    versionName: String,
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
    onOpenWidgets: () -> Unit,
    onOpenGestures: () -> Unit,
    onOpenOnboarding: () -> Unit,
    onOpenTasks: () -> Unit,
    onBack: () -> Unit,
    onFinishOnboarding: () -> Unit,
    onRequestDefaultHome: () -> Unit,
    onOpenHomeSettings: () -> Unit,
    onLaunchTarget: (AppTargetId) -> Unit,
    onGesture: (GestureDirection) -> Unit,
    onTapClock: () -> Unit,
    onTapDate: () -> Unit,
    onPickGestureAction: (GestureDirection) -> Unit,
    onSelectGestureAction: (GestureAction) -> Unit,
    onChooseGestureInstalledApp: () -> Unit,
    onPickClockApp: () -> Unit,
    onPickDateApp: () -> Unit,
    onClearClockApp: () -> Unit,
    onClearDateApp: () -> Unit,
    workProfilePaused: Boolean,
    hasWorkProfile: Boolean,
    shortcutsFor: (AppTargetId) -> List<AppShortcutItem>,
    onLaunchShortcut: (AppShortcutItem) -> Unit,
    widgetPlacements: List<WidgetPlacement>,
    widgetHost: StillWidgetHostController,
    widgetProviders: List<WidgetProviderOption>,
    pickingWidgetProvider: Boolean,
    onRemoveWidget: (Int) -> Unit,
    onUpdateWidget: (WidgetPlacement) -> Unit,
    onAddWidget: () -> Unit,
    onStartPickWidget: () -> Unit,
    onCancelPickWidget: () -> Unit,
    onSelectWidgetProvider: (WidgetProviderOption) -> Unit,
    onEnterEditHome: () -> Unit,
    onExitEditHome: () -> Unit,
    onAddFavorite: (AppTargetId) -> Unit,
    onRemoveFavorite: (AppTargetId) -> Unit,
    onMoveFavorite: (AppTargetId, Boolean) -> Unit,
    onRenameFavorite: (AppTargetId) -> Unit,
    onHideFavorite: (AppTargetId) -> Unit,
    onUninstallFavorite: (AppTargetId) -> Unit,
    onUninstall: (AppTargetId) -> Unit,
    onReplaceFavorite: (AppTargetId) -> Unit,
    onAddFavoritePick: () -> Unit,
    onSelectPickerTarget: (AppTargetId) -> Unit,
    onSetAlias: (AppTargetId, String?) -> Unit,
    onSetHideMode: (AppTargetId, HideMode) -> Unit,
    onOpenAppInfo: (AppTargetId) -> Unit,
    onSetLayoutLocked: (Boolean) -> Unit,
    onToggleLayoutLock: () -> Unit,
    onDismissEmptyHint: () -> Unit,
    onSetShowClock: (Boolean) -> Unit,
    onSetShowDate: (Boolean) -> Unit,
    onSetClockTapEnabled: (Boolean) -> Unit,
    onSetDateTapEnabled: (Boolean) -> Unit,
    onSetFocusSearch: (Boolean) -> Unit,
    onSetTaskPreviewLimit: (Int) -> Unit,
    onSetHaptic: (Boolean) -> Unit,
    onSetVerticalPlacement: (HomeVerticalPlacement) -> Unit,
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
    val hideTopBar =
        (surface == StillSurface.Home && !editingHome) || surface == StillSurface.Onboarding

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
            if (hideTopBar) {
                // Home (non-edit) and Onboarding intentionally have no top bar.
            } else {
                TopAppBar(
                    title = {
                        Text(
                            when (surface) {
                                StillSurface.Home -> stringResource(R.string.edit_home)
                                StillSurface.Apps -> stringResource(R.string.apps_title)
                                StillSurface.Settings -> stringResource(R.string.settings_title)
                                StillSurface.Appearance -> stringResource(R.string.appearance_title)
                                StillSurface.HiddenApps -> stringResource(R.string.hidden_apps)
                                StillSurface.Widgets -> if (pickingWidgetProvider) {
                                    stringResource(R.string.widget_add)
                                } else {
                                    stringResource(R.string.widgets_title)
                                }
                                StillSurface.Gestures -> stringResource(R.string.gestures_title)
                                StillSurface.GestureActionPicker ->
                                    stringResource(R.string.gesture_choose_app)
                                StillSurface.Onboarding -> ""
                                StillSurface.Tasks -> stringResource(R.string.tasks_title)
                                StillSurface.AddTask -> stringResource(R.string.add_task)
                                StillSurface.EditTask -> stringResource(R.string.edit_task)
                                StillSurface.Picker -> when (pickerPurpose) {
                                    PICKER_FAVORITE_REPLACE ->
                                        stringResource(R.string.recover_target)
                                    PICKER_CLOCK_APP -> stringResource(R.string.clock_change_app)
                                    PICKER_DATE_APP -> stringResource(R.string.date_change_app)
                                    PICKER_GESTURE_APP -> stringResource(R.string.gesture_action_app)
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
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when (surface) {
            StillSurface.Home -> HomeSurface(
                modifier = contentModifier,
                favorites = favorites,
                launcherPrefs = launcherPrefs,
                editingHome = editingHome,
                is24Hour = is24Hour,
                taskPreview = taskPreview,
                widgetPlacements = widgetPlacements,
                widgetHost = widgetHost,
                onGestureUp = { onGesture(GestureDirection.Up) },
                onGestureDown = { onGesture(GestureDirection.Down) },
                onGestureLeft = { onGesture(GestureDirection.Left) },
                onGestureRight = { onGesture(GestureDirection.Right) },
                onTapClock = onTapClock,
                onTapDate = onTapDate,
                onChangeClockApp = onPickClockApp,
                onDisableClockTap = { onSetClockTapEnabled(false) },
                onChangeDateApp = onPickDateApp,
                onDisableDateTap = { onSetDateTapEnabled(false) },
                onLaunchFavorite = onLaunchTarget,
                onRemoveFavorite = onRemoveFavorite,
                onRenameFavorite = onRenameFavorite,
                onFavoriteAppInfo = onOpenAppInfo,
                onHideFavorite = onHideFavorite,
                onUninstallFavorite = onUninstallFavorite,
                onMoveFavorite = onMoveFavorite,
                onOpenApps = onOpenApps,
                onOpenSettings = onOpenSettings,
                onOpenAppearance = onOpenAppearance,
                onOpenGestures = onOpenGestures,
                onOpenTasks = onOpenTasks,
                onAddTask = onAddTaskFromHome,
                onTogglePreviewTaskComplete = onTogglePreviewTaskComplete,
                onOpenTask = onOpenTaskFromHome,
                onAddFavorite = onAddFavoritePick,
                onAddWidget = onAddWidget,
                onOpenWidgets = onOpenWidgets,
                onToggleLayoutLock = onToggleLayoutLock,
                onDismissEmptyHint = onDismissEmptyHint,
                onRemoveWidget = onRemoveWidget,
                onUpdateWidget = onUpdateWidget,
                onEnterEditHome = onEnterEditHome,
                onExitEditHome = onExitEditHome,
            )
            StillSurface.Apps -> AppsSurface(
                modifier = contentModifier,
                catalog = searchCatalog,
                favoriteIds = launcherPrefs.favoriteIds.toSet(),
                hideModes = launcherPrefs.hideModes,
                searchValue = searchValue,
                focusSearch = launcherPrefs.focusSearchOnOpenApps,
                workProfilePaused = workProfilePaused,
                hasWorkProfile = hasWorkProfile,
                shortcutsFor = shortcutsFor,
                onSearchChange = onSearchChange,
                onLaunchTarget = onLaunchTarget,
                onLaunchShortcut = onLaunchShortcut,
                onAddFavorite = onAddFavorite,
                onRemoveFavorite = onRemoveFavorite,
                onSetAlias = onSetAlias,
                onSetHideMode = onSetHideMode,
                onOpenAppInfo = onOpenAppInfo,
                onUninstall = onUninstall,
            )
            StillSurface.Settings -> SettingsSurface(
                modifier = contentModifier,
                defaultHomeHeld = defaultHomeHeld,
                launcherPrefs = launcherPrefs,
                versionName = versionName,
                onRequestDefaultHome = onRequestDefaultHome,
                onOpenHomeSettings = onOpenHomeSettings,
                onOpenAppearance = onOpenAppearance,
                onOpenGestures = onOpenGestures,
                onOpenHiddenApps = onOpenHiddenApps,
                onOpenWidgets = onOpenWidgets,
                onOpenTasks = onOpenTasks,
                onOpenOnboarding = onOpenOnboarding,
                onSetLayoutLocked = onSetLayoutLocked,
                onSetShowClock = onSetShowClock,
                onSetShowDate = onSetShowDate,
                onSetClockTapEnabled = onSetClockTapEnabled,
                onSetDateTapEnabled = onSetDateTapEnabled,
                onPickClockApp = onPickClockApp,
                onPickDateApp = onPickDateApp,
                onClearClockApp = onClearClockApp,
                onClearDateApp = onClearDateApp,
                onSetFocusSearch = onSetFocusSearch,
                onSetTaskPreviewLimit = onSetTaskPreviewLimit,
                onSetHaptic = onSetHaptic,
                onSetVerticalPlacement = onSetVerticalPlacement,
            )
            StillSurface.Gestures -> GesturesSurface(
                modifier = contentModifier,
                gestures = gestures,
                actionLabel = { action ->
                    gestureActionLabel(action) { id ->
                        catalog.find { it.id == id }?.displayLabel
                    }
                },
                onPickAction = onPickGestureAction,
            )
            StillSurface.GestureActionPicker -> GestureActionPickerSurface(
                modifier = contentModifier,
                onSelect = onSelectGestureAction,
                onChooseInstalledApp = onChooseGestureInstalledApp,
            )
            StillSurface.Onboarding -> OnboardingSurface(
                modifier = contentModifier,
                onFinished = onFinishOnboarding,
            )
            StillSurface.Widgets -> WidgetsSurface(
                modifier = contentModifier,
                placements = widgetPlacements,
                providers = widgetProviders,
                host = widgetHost,
                pickingProvider = pickingWidgetProvider,
                onStartAdd = onStartPickWidget,
                onCancelPick = onCancelPickWidget,
                onSelectProvider = onSelectWidgetProvider,
                onRemove = onRemoveWidget,
                onUpdate = onUpdateWidget,
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
                onSelect = onSelectPickerTarget,
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
