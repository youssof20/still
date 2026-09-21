package app.still.home

import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import app.still.R
import app.still.StillApplication
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
import app.still.ui.StillTheme
import kotlinx.coroutines.launch

enum class StillSurface {
    Home,
    Apps,
    Settings,
    HiddenApps,
    Picker,
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

        setContent {
            StillTheme {
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

                var surface by rememberSaveable { mutableStateOf(StillSurface.Home) }
                var editingHome by rememberSaveable { mutableStateOf(false) }
                var searchValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
                    mutableStateOf(TextFieldValue())
                }
                var pickingSlot by rememberSaveable { mutableStateOf<String?>(null) }
                var replaceFavoriteId by rememberSaveable { mutableStateOf<String?>(null) }

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

                fun resetToCleanHome() {
                    surface = StillSurface.Home
                    pickingSlot = null
                    replaceFavoriteId = null
                    editingHome = false
                    searchValue = TextFieldValue()
                }

                LaunchedEffect(homeInvocation) {
                    resetToCleanHome()
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
                    onSearchChange = { searchValue = it },
                    onOpenApps = { surface = StillSurface.Apps },
                    onOpenSettings = {
                        editingHome = false
                        surface = StillSurface.Settings
                    },
                    onOpenHiddenApps = { surface = StillSurface.HiddenApps },
                    onBackToHome = { resetToCleanHome() },
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
        (application as StillApplication).appCatalog.refresh()
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
    onSearchChange: (TextFieldValue) -> Unit,
    onOpenApps: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHiddenApps: () -> Unit,
    onBackToHome: () -> Unit,
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
                onBackToHome()
            }
            surface != StillSurface.Home -> onBackToHome()
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
                            StillSurface.HiddenApps -> stringResource(R.string.hidden_apps)
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
                                onBackToHome()
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
                onOpenApps = onOpenApps,
                onOpenSettings = onOpenSettings,
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
                onSetLayoutLocked = onSetLayoutLocked,
                onSetShowClock = onSetShowClock,
                onSetShowDate = onSetShowDate,
                onSetFocusSearch = onSetFocusSearch,
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
        }
    }
}
