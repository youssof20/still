package app.still.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.still.R
import app.still.StillApplication
import app.still.launcher.AppLauncher
import app.still.launcher.AppSearch
import app.still.launcher.AppTarget
import app.still.launcher.AppTargetId
import app.still.launcher.LaunchFailureReason
import app.still.launcher.LaunchResult
import app.still.prefs.GestureActionSlot
import app.still.prefs.GesturePreferences
import app.still.prefs.GestureTargetPreference
import app.still.settings.HomeRoleHelper
import app.still.ui.StillTheme
import kotlinx.coroutines.launch

class HomeActivity : ComponentActivity() {
    private lateinit var appLauncher: AppLauncher

    private val homeRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        // Cancellation is fine: remain on Home with no onboarding loop.
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
                val catalog by app.appCatalog.targets.collectAsStateWithLifecycle()
                val gestures by app.gesturePreferences.preferences.collectAsStateWithLifecycle(
                    initialValue = GesturePreferences(),
                )
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                var surface by rememberSaveable { mutableStateOf(Surface.Home) }
                var searchValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
                    mutableStateOf(TextFieldValue())
                }
                var pickingSlot by rememberSaveable { mutableStateOf<String?>(null) }

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
                            surface = Surface.Picker
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
                                surface = Surface.Picker
                            } else {
                                launchTarget(preference.id)
                            }
                        }
                    }
                }

                fun resetToCleanHome() {
                    surface = Surface.Home
                    pickingSlot = null
                    searchValue = TextFieldValue()
                }

                LaunchedEffect(homeInvocation) {
                    resetToCleanHome()
                }

                StillAppScaffold(
                    surface = surface,
                    defaultHomeHeld = defaultHomeHeld,
                    catalog = catalog,
                    gestures = gestures,
                    searchValue = searchValue,
                    snackbarHostState = snackbarHostState,
                    pickingSlot = pickingSlot?.let { GestureActionSlot.valueOf(it) },
                    onSearchChange = { searchValue = it },
                    onOpenApps = { surface = Surface.Apps },
                    onOpenSettings = { surface = Surface.Settings },
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
                        surface = Surface.Picker
                    },
                    onDisableSlot = { slot ->
                        scope.launch {
                            app.gesturePreferences.setTarget(
                                slot,
                                GestureTargetPreference.Disabled,
                            )
                        }
                    },
                    onSelectTargetForSlot = { slot, targetId ->
                        scope.launch {
                            app.gesturePreferences.setTarget(
                                slot,
                                GestureTargetPreference.Target(targetId),
                            )
                            pickingSlot = null
                            surface = Surface.Settings
                        }
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // singleTask Home invocations land here instead of creating duplicate activities.
        homeInvocation += 1
    }

    override fun onResume() {
        super.onResume()
        defaultHomeHeld = HomeRoleHelper.isDefaultHome(this)
        (application as StillApplication).appCatalog.refresh()
    }
}

private enum class Surface {
    Home,
    Apps,
    Settings,
    Picker,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StillAppScaffold(
    surface: Surface,
    defaultHomeHeld: Boolean,
    catalog: List<AppTarget>,
    gestures: GesturePreferences,
    searchValue: TextFieldValue,
    snackbarHostState: SnackbarHostState,
    pickingSlot: GestureActionSlot?,
    onSearchChange: (TextFieldValue) -> Unit,
    onOpenApps: () -> Unit,
    onOpenSettings: () -> Unit,
    onBackToHome: () -> Unit,
    onRequestDefaultHome: () -> Unit,
    onOpenHomeSettings: () -> Unit,
    onLaunchTarget: (AppTargetId) -> Unit,
    onCamera: () -> Unit,
    onPhone: () -> Unit,
    onPickSlot: (GestureActionSlot) -> Unit,
    onDisableSlot: (GestureActionSlot) -> Unit,
    onSelectTargetForSlot: (GestureActionSlot, AppTargetId) -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current

    BackHandler(enabled = surface != Surface.Home) {
        when (surface) {
            Surface.Apps -> {
                keyboard?.hide()
                onBackToHome()
            }
            Surface.Settings, Surface.Picker -> onBackToHome()
            Surface.Home -> Unit
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (surface) {
                            Surface.Home -> stringResource(R.string.home_title)
                            Surface.Apps -> stringResource(R.string.apps_title)
                            Surface.Settings -> stringResource(R.string.settings_title)
                            Surface.Picker -> when (pickingSlot) {
                                GestureActionSlot.Camera -> stringResource(R.string.pick_camera_target)
                                GestureActionSlot.Phone -> stringResource(R.string.pick_phone_target)
                                null -> stringResource(R.string.choose_app)
                            }
                        },
                    )
                },
                navigationIcon = {
                    if (surface != Surface.Home) {
                        TextButton(onClick = {
                            keyboard?.hide()
                            onBackToHome()
                        }) {
                            Text(stringResource(R.string.back))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (surface) {
            Surface.Home -> HomeSurface(
                modifier = Modifier.padding(padding),
                defaultHomeHeld = defaultHomeHeld,
                onOpenApps = onOpenApps,
                onOpenSettings = onOpenSettings,
                onCamera = onCamera,
                onPhone = onPhone,
                onRequestDefaultHome = onRequestDefaultHome,
            )
            Surface.Apps -> AppsSurface(
                modifier = Modifier.padding(padding),
                catalog = catalog,
                searchValue = searchValue,
                onSearchChange = onSearchChange,
                onLaunchTarget = onLaunchTarget,
            )
            Surface.Settings -> SettingsSurface(
                modifier = Modifier.padding(padding),
                defaultHomeHeld = defaultHomeHeld,
                gestures = gestures,
                catalog = catalog,
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
            )
            Surface.Picker -> PickerSurface(
                modifier = Modifier.padding(padding),
                catalog = catalog,
                slot = pickingSlot,
                onSelect = onSelectTargetForSlot,
            )
        }
    }
}

@Composable
private fun HomeSurface(
    modifier: Modifier = Modifier,
    defaultHomeHeld: Boolean,
    onOpenApps: () -> Unit,
    onOpenSettings: () -> Unit,
    onCamera: () -> Unit,
    onPhone: () -> Unit,
    onRequestDefaultHome: () -> Unit,
) {
    var accumulatedDrag by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { accumulatedDrag = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        accumulatedDrag += dragAmount
                    },
                    onDragEnd = {
                        val threshold = 120f
                        when {
                            accumulatedDrag <= -threshold -> onCamera()
                            accumulatedDrag >= threshold -> onPhone()
                        }
                        accumulatedDrag = 0f
                    },
                    onDragCancel = { accumulatedDrag = 0f },
                )
            }
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = if (defaultHomeHeld) {
                    stringResource(R.string.default_home_active)
                } else {
                    stringResource(R.string.default_home_inactive)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!defaultHomeHeld) {
                Button(onClick = onRequestDefaultHome) {
                    Text(stringResource(R.string.set_as_default_home))
                }
            }
            Button(onClick = onOpenApps, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.open_apps))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCamera) {
                    Text(stringResource(R.string.open_camera))
                }
                OutlinedButton(onClick = onPhone) {
                    Text(stringResource(R.string.open_phone))
                }
            }
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.open_settings))
            }
            Text(
                text = stringResource(R.string.gesture_note),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun AppsSurface(
    modifier: Modifier = Modifier,
    catalog: List<AppTarget>,
    searchValue: TextFieldValue,
    onSearchChange: (TextFieldValue) -> Unit,
    onLaunchTarget: (AppTargetId) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val results = remember(searchValue.text, catalog) {
        AppSearch.filterAndRank(searchValue.text, catalog)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        OutlinedTextField(
            value = searchValue,
            onValueChange = { onSearchChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = { Text(stringResource(R.string.search_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    // Explicit IME action only. Typing never auto-launches,
                    // including when exactly one result remains.
                    results.firstOrNull()?.let { onLaunchTarget(it.id) }
                },
            ),
            trailingIcon = {
                if (searchValue.text.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            onSearchChange(TextFieldValue(text = "", selection = TextRange.Zero))
                        },
                    ) {
                        Text(stringResource(R.string.clear_query))
                    }
                }
            },
        )

        if (results.isEmpty() && searchValue.text.isNotBlank()) {
            Text(
                text = stringResource(R.string.empty_results),
                modifier = Modifier.padding(top = 24.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            // Horizontal Camera/Phone gestures are not attached here so list scroll wins.
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(
                    results,
                    key = { it.id.flattenedComponent() + "@" + it.id.userSerialNumber },
                ) { target ->
                    AppRow(target = target, onClick = { onLaunchTarget(target.id) })
                }
            }
        }
    }
}

@Composable
private fun SettingsSurface(
    modifier: Modifier = Modifier,
    defaultHomeHeld: Boolean,
    gestures: GesturePreferences,
    catalog: List<AppTarget>,
    onRequestDefaultHome: () -> Unit,
    onOpenHomeSettings: () -> Unit,
    onPickSlot: (GestureActionSlot) -> Unit,
    onDisableSlot: (GestureActionSlot) -> Unit,
    onOpenConfigured: (GestureActionSlot) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = if (defaultHomeHeld) {
                stringResource(R.string.default_home_active)
            } else {
                stringResource(R.string.default_home_inactive)
            },
        )
        Button(onClick = onRequestDefaultHome) {
            Text(stringResource(R.string.set_as_default_home))
        }
        OutlinedButton(onClick = onOpenHomeSettings) {
            Text(stringResource(R.string.change_default_home))
        }

        GestureSlotRow(
            title = stringResource(R.string.camera_action),
            preference = gestures.camera,
            catalog = catalog,
            onChoose = { onPickSlot(GestureActionSlot.Camera) },
            onDisable = { onDisableSlot(GestureActionSlot.Camera) },
            onOpen = { onOpenConfigured(GestureActionSlot.Camera) },
        )
        GestureSlotRow(
            title = stringResource(R.string.phone_action),
            preference = gestures.phone,
            catalog = catalog,
            onChoose = { onPickSlot(GestureActionSlot.Phone) },
            onDisable = { onDisableSlot(GestureActionSlot.Phone) },
            onOpen = { onOpenConfigured(GestureActionSlot.Phone) },
        )
    }
}

@Composable
private fun GestureSlotRow(
    title: String,
    preference: GestureTargetPreference,
    catalog: List<AppTarget>,
    onChoose: () -> Unit,
    onDisable: () -> Unit,
    onOpen: () -> Unit,
) {
    val summary = when (preference) {
        GestureTargetPreference.Disabled -> stringResource(R.string.action_disabled)
        GestureTargetPreference.Unset -> stringResource(R.string.choose_app)
        is GestureTargetPreference.Target -> {
            catalog.firstOrNull { it.id == preference.id }?.displayLabel
                ?: stringResource(R.string.launch_failed_missing)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(text = summary, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onChoose) {
                Text(stringResource(R.string.choose_app))
            }
            OutlinedButton(onClick = onDisable) {
                Text(stringResource(R.string.disable_action))
            }
            if (preference is GestureTargetPreference.Target) {
                TextButton(onClick = onOpen) {
                    Text(title)
                }
            }
        }
    }
}

@Composable
private fun PickerSurface(
    modifier: Modifier = Modifier,
    catalog: List<AppTarget>,
    slot: GestureActionSlot?,
    onSelect: (GestureActionSlot, AppTargetId) -> Unit,
) {
    if (slot == null) {
        Text(modifier = modifier.padding(16.dp), text = stringResource(R.string.choose_app))
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        items(
            catalog,
            key = { it.id.flattenedComponent() + "@" + it.id.userSerialNumber },
        ) { target ->
            AppRow(target = target, onClick = { onSelect(slot, target.id) })
        }
    }
}

@Composable
private fun AppRow(
    target: AppTarget,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(text = target.displayLabel, style = MaterialTheme.typography.titleMedium)
            val subtitle = buildString {
                if (target.alias != null) {
                    append(target.originalLabel)
                }
                target.profileIndicator?.let {
                    if (isNotEmpty()) append(" · ")
                    append(stringResource(R.string.profile_badge, it))
                }
            }
            if (subtitle.isNotEmpty()) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
