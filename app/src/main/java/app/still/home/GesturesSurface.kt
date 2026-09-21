package app.still.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.still.R
import app.still.prefs.GestureAction
import app.still.prefs.GestureDirection
import app.still.prefs.GesturePreferences
import app.still.ui.PrefRow
import app.still.ui.PrefSection
import app.still.ui.SheetActionRow
import app.still.ui.StillSpacing

@Composable
fun GesturesSurface(
    modifier: Modifier = Modifier,
    gestures: GesturePreferences,
    actionLabel: (GestureAction) -> String,
    onPickAction: (GestureDirection) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = StillSpacing.settingsHorizontal)
            .padding(bottom = StillSpacing.s48),
    ) {
        PrefSection(stringResource(R.string.gestures_title))
        GestureDirection.entries.forEach { direction ->
            val title = when (direction) {
                GestureDirection.Up -> stringResource(R.string.gesture_up)
                GestureDirection.Down -> stringResource(R.string.gesture_down)
                GestureDirection.Left -> stringResource(R.string.gesture_left)
                GestureDirection.Right -> stringResource(R.string.gesture_right)
            }
            PrefRow(
                title = title,
                value = actionLabel(gestures.action(direction)),
                onClick = { onPickAction(direction) },
            )
        }
    }
}

@Composable
fun GestureActionPickerSurface(
    modifier: Modifier = Modifier,
    onSelect: (GestureAction) -> Unit,
    onChooseInstalledApp: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = StillSpacing.settingsHorizontal)
            .padding(bottom = StillSpacing.s48),
    ) {
        PrefSection(stringResource(R.string.choose_action))
        SheetActionRow(stringResource(R.string.gesture_action_apps)) {
            onSelect(GestureAction.Apps)
        }
        SheetActionRow(stringResource(R.string.gesture_action_notifications)) {
            onSelect(GestureAction.Notifications)
        }
        SheetActionRow(stringResource(R.string.gesture_action_quick_settings)) {
            onSelect(GestureAction.QuickSettings)
        }
        SheetActionRow(stringResource(R.string.gesture_action_tasks)) {
            onSelect(GestureAction.Tasks)
        }
        SheetActionRow(stringResource(R.string.gesture_action_camera)) {
            onSelect(GestureAction.OpenCamera)
        }
        SheetActionRow(stringResource(R.string.gesture_action_phone)) {
            onSelect(GestureAction.OpenPhone)
        }
        SheetActionRow(stringResource(R.string.gesture_action_app), onChooseInstalledApp)
        SheetActionRow(stringResource(R.string.gesture_action_none)) {
            onSelect(GestureAction.None)
        }
    }
}

fun gestureActionLabel(
    action: GestureAction,
    appLabel: (app.still.launcher.AppTargetId) -> String?,
): String = when (action) {
    GestureAction.None -> "None"
    GestureAction.Apps -> "Apps"
    GestureAction.Tasks -> "Tasks"
    GestureAction.Notifications -> "Notifications"
    GestureAction.QuickSettings -> "Quick settings"
    GestureAction.Lock -> "Lock"
    GestureAction.OpenCamera -> "Camera"
    GestureAction.OpenPhone -> "Phone"
    is GestureAction.App -> appLabel(action.id) ?: action.id.packageName
}
