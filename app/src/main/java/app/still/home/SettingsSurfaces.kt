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
import app.still.R
import app.still.launcher.AppTarget
import app.still.launcher.AppTargetId
import app.still.launcher.HideMode
import app.still.prefs.HomeVerticalPlacement
import app.still.prefs.LauncherPreferences
import app.still.ui.StillSpacing

@Composable
fun SettingsSurface(
    modifier: Modifier = Modifier,
    defaultHomeHeld: Boolean,
    launcherPrefs: LauncherPreferences,
    versionName: String,
    onRequestDefaultHome: () -> Unit,
    onOpenHomeSettings: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenGestures: () -> Unit,
    onOpenHiddenApps: () -> Unit,
    onOpenWidgets: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenOnboarding: () -> Unit,
    onSetLayoutLocked: (Boolean) -> Unit,
    onSetShowClock: (Boolean) -> Unit,
    onSetShowDate: (Boolean) -> Unit,
    onSetClockTapEnabled: (Boolean) -> Unit,
    onSetDateTapEnabled: (Boolean) -> Unit,
    onPickClockApp: () -> Unit,
    onPickDateApp: () -> Unit,
    onClearClockApp: () -> Unit,
    onClearDateApp: () -> Unit,
    onSetFocusSearch: (Boolean) -> Unit,
    onSetTaskPreviewLimit: (Int) -> Unit,
    onSetHaptic: (Boolean) -> Unit,
    onSetVerticalPlacement: (HomeVerticalPlacement) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(StillSpacing.md),
        verticalArrangement = Arrangement.spacedBy(StillSpacing.md),
    ) {
        SectionTitle(stringResource(R.string.settings_section_home))
        Text(
            text = if (defaultHomeHeld) {
                stringResource(R.string.default_home_active)
            } else {
                stringResource(R.string.default_home_inactive)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onRequestDefaultHome) {
            Text(stringResource(R.string.set_as_default_home))
        }
        OutlinedButton(onClick = onOpenHomeSettings) {
            Text(stringResource(R.string.change_default_home))
        }
        SettingSwitch(
            title = stringResource(R.string.show_clock),
            checked = launcherPrefs.showClock,
            onCheckedChange = onSetShowClock,
        )
        SettingSwitch(
            title = stringResource(R.string.clock_tap),
            checked = launcherPrefs.clockTapEnabled,
            onCheckedChange = onSetClockTapEnabled,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(StillSpacing.xs)) {
            OutlinedButton(onClick = onPickClockApp) {
                Text(stringResource(R.string.clock_change_app))
            }
            TextButton(onClick = onClearClockApp) {
                Text(stringResource(R.string.use_system_default))
            }
        }
        SettingSwitch(
            title = stringResource(R.string.show_date),
            checked = launcherPrefs.showDate,
            onCheckedChange = onSetShowDate,
        )
        SettingSwitch(
            title = stringResource(R.string.date_tap),
            checked = launcherPrefs.dateTapEnabled,
            onCheckedChange = onSetDateTapEnabled,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(StillSpacing.xs)) {
            OutlinedButton(onClick = onPickDateApp) {
                Text(stringResource(R.string.date_change_app))
            }
            TextButton(onClick = onClearDateApp) {
                Text(stringResource(R.string.use_system_default))
            }
        }
        SettingSwitch(
            title = stringResource(R.string.layout_lock),
            summary = stringResource(R.string.layout_lock_summary),
            checked = launcherPrefs.layoutLocked,
            onCheckedChange = onSetLayoutLocked,
        )
        Text(stringResource(R.string.home_placement), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(StillSpacing.xs)) {
            OutlinedButton(onClick = { onSetVerticalPlacement(HomeVerticalPlacement.Top) }) {
                Text(stringResource(R.string.placement_top))
            }
            OutlinedButton(onClick = { onSetVerticalPlacement(HomeVerticalPlacement.Center) }) {
                Text(stringResource(R.string.placement_center))
            }
            OutlinedButton(onClick = { onSetVerticalPlacement(HomeVerticalPlacement.Bottom) }) {
                Text(stringResource(R.string.placement_bottom))
            }
        }
        SettingSwitch(
            title = stringResource(R.string.haptic_feedback),
            checked = launcherPrefs.hapticFeedback,
            onCheckedChange = onSetHaptic,
        )

        SectionTitle(stringResource(R.string.settings_section_appearance))
        OutlinedButton(onClick = onOpenAppearance) {
            Text(stringResource(R.string.open_appearance))
        }

        SectionTitle(stringResource(R.string.settings_section_gestures))
        OutlinedButton(onClick = onOpenGestures) {
            Text(stringResource(R.string.gestures_title))
        }

        SectionTitle(stringResource(R.string.settings_section_apps))
        SettingSwitch(
            title = stringResource(R.string.focus_search),
            checked = launcherPrefs.focusSearchOnOpenApps,
            onCheckedChange = onSetFocusSearch,
        )
        Text(stringResource(R.string.hidden_apps_explainer), style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = onOpenHiddenApps) {
            Text(stringResource(R.string.hidden_apps))
        }

        SectionTitle(stringResource(R.string.settings_section_tasks))
        Text(stringResource(R.string.task_preview), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(StillSpacing.xs)) {
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
        OutlinedButton(onClick = onOpenTasks) {
            Text(stringResource(R.string.open_tasks))
        }

        SectionTitle(stringResource(R.string.settings_section_widgets))
        OutlinedButton(onClick = onOpenWidgets) {
            Text(stringResource(R.string.open_widgets))
        }

        SectionTitle(stringResource(R.string.settings_section_privacy))
        Text(stringResource(R.string.privacy_body), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.privacy_degoogle), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.private_space_unsupported), style = MaterialTheme.typography.bodySmall)

        SectionTitle(stringResource(R.string.settings_section_help))
        OutlinedButton(onClick = onOpenOnboarding) {
            Text(stringResource(R.string.quick_guide))
        }

        SectionTitle(stringResource(R.string.settings_section_about))
        Text(stringResource(R.string.about_version, versionName))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = StillSpacing.xs),
    )
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
            .padding(StillSpacing.md),
        verticalArrangement = Arrangement.spacedBy(StillSpacing.sm),
    ) {
        Text(stringResource(R.string.hidden_apps_explainer), style = MaterialTheme.typography.bodyMedium)
        if (hidden.isEmpty()) {
            Text(stringResource(R.string.hidden_apps_empty))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(StillSpacing.xs)) {
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
        contentPadding = PaddingValues(StillSpacing.md),
    ) {
        items(
            catalog,
            key = { it.id.flattenedComponent() + "@" + it.id.userSerialNumber },
        ) { target ->
            TextButton(
                onClick = { onSelect(target.id) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = StillSpacing.sm, horizontal = StillSpacing.xs),
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
    summary: String? = null,
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
