package app.still.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.still.R
import app.still.launcher.AppTarget
import app.still.launcher.AppTargetId
import app.still.launcher.HideMode
import app.still.prefs.GestureActionSlot
import app.still.prefs.GesturePreferences
import app.still.prefs.GestureTargetPreference
import app.still.prefs.LauncherPreferences

@Composable
fun SettingsSurface(
    modifier: Modifier = Modifier,
    defaultHomeHeld: Boolean,
    gestures: GesturePreferences,
    catalog: List<AppTarget>,
    launcherPrefs: LauncherPreferences,
    onRequestDefaultHome: () -> Unit,
    onOpenHomeSettings: () -> Unit,
    onPickSlot: (GestureActionSlot) -> Unit,
    onDisableSlot: (GestureActionSlot) -> Unit,
    onOpenConfigured: (GestureActionSlot) -> Unit,
    onOpenHiddenApps: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenWidgets: () -> Unit = {},
    onSetLayoutLocked: (Boolean) -> Unit,
    onSetShowClock: (Boolean) -> Unit,
    onSetShowDate: (Boolean) -> Unit,
    onSetFocusSearch: (Boolean) -> Unit,
    onSetTaskPreviewLimit: (Int) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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

        OutlinedButton(onClick = onOpenAppearance) {
            Text(stringResource(R.string.open_appearance))
        }

        OutlinedButton(onClick = onOpenWidgets) {
            Text(stringResource(R.string.open_widgets))
        }

        Text(stringResource(R.string.private_space_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.private_space_unsupported),
            style = MaterialTheme.typography.bodySmall,
        )

        Text(stringResource(R.string.hidden_apps), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.hidden_apps_explainer), style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = onOpenHiddenApps) {
            Text(stringResource(R.string.hidden_apps))
        }

        SettingSwitch(
            title = stringResource(R.string.layout_lock),
            summary = stringResource(R.string.layout_lock_summary),
            checked = launcherPrefs.layoutLocked,
            onCheckedChange = onSetLayoutLocked,
        )
        SettingSwitch(
            title = stringResource(R.string.show_clock),
            summary = null,
            checked = launcherPrefs.showClock,
            onCheckedChange = onSetShowClock,
        )
        SettingSwitch(
            title = stringResource(R.string.show_date),
            summary = null,
            checked = launcherPrefs.showDate,
            onCheckedChange = onSetShowDate,
        )
        SettingSwitch(
            title = stringResource(R.string.focus_search),
            summary = null,
            checked = launcherPrefs.focusSearchOnOpenApps,
            onCheckedChange = onSetFocusSearch,
        )

        Text(stringResource(R.string.task_preview), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onSetTaskPreviewLimit(0) }) {
                Text(stringResource(R.string.task_preview_off))
            }
            OutlinedButton(onClick = { onSetTaskPreviewLimit(3) }) {
                Text(stringResource(R.string.task_preview_three))
            }
            OutlinedButton(onClick = { onSetTaskPreviewLimit(5) }) {
                Text(stringResource(R.string.task_preview_five))
            }
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

        Text(stringResource(R.string.gesture_note), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun HiddenAppsSurface(
    modifier: Modifier = Modifier,
    catalog: List<AppTarget>,
    hideModes: Map<AppTargetId, HideMode>,
    onSetHideMode: (AppTargetId, HideMode) -> Unit,
) {
    val hidden = catalog.filter { (hideModes[it.id] ?: HideMode.None) != HideMode.None }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.hidden_apps_explainer), style = MaterialTheme.typography.bodyMedium)
        if (hidden.isEmpty()) {
            Text(stringResource(R.string.hidden_apps_empty))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    hidden,
                    key = { it.id.flattenedComponent() + "@" + it.id.userSerialNumber },
                ) { target ->
                    val mode = hideModes[target.id] ?: HideMode.None
                    Column {
                        Text(target.displayLabel, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = when (mode) {
                                HideMode.FromBrowsing -> stringResource(R.string.hide_mode_browsing)
                                HideMode.FromLauncher -> stringResource(R.string.hide_mode_launcher)
                                HideMode.None -> ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                        TextButton(onClick = { onSetHideMode(target.id, HideMode.None) }) {
                            Text(stringResource(R.string.unhide_app))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PickerSurface(
    modifier: Modifier = Modifier,
    catalog: List<AppTarget>,
    onSelect: (AppTargetId) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        items(
            catalog,
            key = { it.id.flattenedComponent() + "@" + it.id.userSerialNumber },
        ) { target ->
            TextButton(
                onClick = { onSelect(target.id) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(target.displayLabel, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    summary: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            summary?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
