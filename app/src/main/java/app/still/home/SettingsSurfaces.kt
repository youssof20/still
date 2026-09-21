package app.still.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.still.R
import app.still.launcher.AppTarget
import app.still.launcher.AppTargetId
import app.still.launcher.HideMode
import app.still.prefs.HomeVerticalPlacement
import app.still.prefs.LauncherPreferences
import app.still.ui.PrefRow
import app.still.ui.PrefSection
import app.still.ui.PrefSwitch
import app.still.ui.StillSpacing

@Composable
fun SettingsSurface(
    modifier: Modifier = Modifier,
    defaultHomeHeld: Boolean,
    launcherPrefs: LauncherPreferences,
    versionName: String,
    onRequestDefaultHome: () -> Unit,
    onOpenHomeSettings: () -> Unit,
    onOpenHomeDetail: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenGestures: () -> Unit,
    onOpenHiddenApps: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenOnboarding: () -> Unit,
    onSetHaptic: (Boolean) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = StillSpacing.settingsHorizontal)
            .padding(bottom = StillSpacing.s48),
    ) {
        PrefSection(stringResource(R.string.settings_title))
        PrefRow(
            title = stringResource(R.string.settings_section_home),
            value = "›",
            onClick = onOpenHomeDetail,
        )
        PrefRow(
            title = stringResource(R.string.open_appearance),
            value = "›",
            onClick = onOpenAppearance,
        )
        PrefRow(
            title = stringResource(R.string.gestures_title),
            value = "›",
            onClick = onOpenGestures,
        )
        PrefRow(
            title = stringResource(R.string.hidden_apps),
            value = "›",
            onClick = onOpenHiddenApps,
        )
        PrefRow(
            title = stringResource(R.string.open_tasks),
            value = when (launcherPrefs.taskPreviewLimit) {
                0 -> stringResource(R.string.pref_off)
                else -> launcherPrefs.taskPreviewLimit.toString()
            },
            onClick = onOpenTasks,
        )

        PrefSection(stringResource(R.string.settings_section_privacy))
        Text(
            text = stringResource(R.string.privacy_body),
            modifier = Modifier.padding(vertical = StillSpacing.s8),
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        )

        PrefSection(stringResource(R.string.settings_section_help))
        PrefRow(title = stringResource(R.string.quick_guide), value = "›", onClick = onOpenOnboarding)
        PrefSwitch(
            title = stringResource(R.string.haptic_feedback),
            checked = launcherPrefs.hapticFeedback,
            onCheckedChange = onSetHaptic,
        )

        PrefSection(stringResource(R.string.settings_section_about))
        PrefRow(title = stringResource(R.string.about_version, versionName))

        if (!defaultHomeHeld) {
            PrefSection(stringResource(R.string.set_as_default_home))
            PrefRow(
                title = stringResource(R.string.set_as_default_home),
                onClick = onRequestDefaultHome,
            )
            PrefRow(
                title = stringResource(R.string.change_default_home),
                onClick = onOpenHomeSettings,
            )
        }
    }
}

@Composable
fun HomeSettingsSurface(
    modifier: Modifier = Modifier,
    launcherPrefs: LauncherPreferences,
    clockActionLabel: String,
    dateActionLabel: String,
    onSetShowClock: (Boolean) -> Unit,
    onSetShowDate: (Boolean) -> Unit,
    onSetClockTapEnabled: (Boolean) -> Unit,
    onSetDateTapEnabled: (Boolean) -> Unit,
    onPickClockApp: () -> Unit,
    onPickDateApp: () -> Unit,
    onClearClockApp: () -> Unit,
    onClearDateApp: () -> Unit,
    onSetLayoutLocked: (Boolean) -> Unit,
    onSetFocusSearch: (Boolean) -> Unit,
    onSetTaskPreviewLimit: (Int) -> Unit,
    onSetVerticalPlacement: (HomeVerticalPlacement) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = StillSpacing.settingsHorizontal)
            .padding(bottom = StillSpacing.s48),
    ) {
        PrefSwitch(
            title = stringResource(R.string.show_clock),
            checked = launcherPrefs.showClock,
            onCheckedChange = onSetShowClock,
        )
        PrefRow(
            title = stringResource(R.string.clock_tap),
            value = if (launcherPrefs.clockTapEnabled) clockActionLabel else stringResource(R.string.pref_off),
            onClick = {
                if (launcherPrefs.clockTapEnabled) onPickClockApp()
                else onSetClockTapEnabled(true)
            },
        )
        if (launcherPrefs.clockTapEnabled) {
            PrefRow(
                title = stringResource(R.string.clock_disable_tap),
                onClick = { onSetClockTapEnabled(false) },
            )
            PrefRow(
                title = stringResource(R.string.use_system_default),
                onClick = onClearClockApp,
            )
        }

        PrefSwitch(
            title = stringResource(R.string.show_date),
            checked = launcherPrefs.showDate,
            onCheckedChange = onSetShowDate,
        )
        PrefRow(
            title = stringResource(R.string.date_tap),
            value = if (launcherPrefs.dateTapEnabled) dateActionLabel else stringResource(R.string.pref_off),
            onClick = {
                if (launcherPrefs.dateTapEnabled) onPickDateApp()
                else onSetDateTapEnabled(true)
            },
        )
        if (launcherPrefs.dateTapEnabled) {
            PrefRow(
                title = stringResource(R.string.date_disable_tap),
                onClick = { onSetDateTapEnabled(false) },
            )
            PrefRow(
                title = stringResource(R.string.use_system_default),
                onClick = onClearDateApp,
            )
        }

        PrefRow(
            title = stringResource(R.string.home_placement),
            value = when (launcherPrefs.homeVerticalPlacement) {
                HomeVerticalPlacement.Top -> stringResource(R.string.placement_top)
                HomeVerticalPlacement.Center -> stringResource(R.string.placement_center)
                HomeVerticalPlacement.Bottom -> stringResource(R.string.placement_bottom)
            },
            onClick = {
                val next = when (launcherPrefs.homeVerticalPlacement) {
                    HomeVerticalPlacement.Top -> HomeVerticalPlacement.Center
                    HomeVerticalPlacement.Center -> HomeVerticalPlacement.Bottom
                    HomeVerticalPlacement.Bottom -> HomeVerticalPlacement.Top
                }
                onSetVerticalPlacement(next)
            },
        )

        PrefRow(
            title = stringResource(R.string.task_preview),
            value = when (launcherPrefs.taskPreviewLimit) {
                0 -> stringResource(R.string.pref_off)
                1 -> "1"
                3 -> "3"
                5 -> "5"
                else -> launcherPrefs.taskPreviewLimit.toString()
            },
            onClick = {
                val next = when (launcherPrefs.taskPreviewLimit) {
                    0 -> 1
                    1 -> 3
                    3 -> 5
                    else -> 0
                }
                onSetTaskPreviewLimit(next)
            },
        )

        PrefSwitch(
            title = stringResource(R.string.layout_lock),
            checked = launcherPrefs.layoutLocked,
            onCheckedChange = onSetLayoutLocked,
            summary = stringResource(R.string.layout_lock_summary),
        )
        PrefSwitch(
            title = stringResource(R.string.focus_search),
            checked = launcherPrefs.focusSearchOnOpenApps,
            onCheckedChange = onSetFocusSearch,
        )
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
            .padding(horizontal = StillSpacing.settingsHorizontal),
    ) {
        Text(
            text = stringResource(R.string.hidden_apps_explainer),
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = StillSpacing.s12),
        )
        if (hidden.isEmpty()) {
            Text(
                stringResource(R.string.hidden_apps_empty),
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn {
                items(
                    hidden,
                    key = { it.id.flattenedComponent() + "@" + it.id.userSerialNumber },
                ) { target ->
                    PrefRow(
                        title = target.displayLabel,
                        value = stringResource(R.string.unhide_app),
                        onClick = { onSetHideMode(target.id, HideMode.None) },
                    )
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
        contentPadding = PaddingValues(horizontal = StillSpacing.settingsHorizontal),
    ) {
        items(
            catalog,
            key = { it.id.flattenedComponent() + "@" + it.id.userSerialNumber },
        ) { target ->
            PrefRow(
                title = target.displayLabel,
                onClick = { onSelect(target.id) },
            )
        }
    }
}
