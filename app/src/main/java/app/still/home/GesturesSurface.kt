package app.still.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.still.R
import app.still.prefs.GestureAction
import app.still.prefs.GestureDirection
import app.still.prefs.GesturePreferences
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
            .padding(StillSpacing.md),
        verticalArrangement = Arrangement.spacedBy(StillSpacing.md),
    ) {
        Text(
            text = stringResource(R.string.gestures_title),
            style = MaterialTheme.typography.titleLarge,
        )
        GestureDirection.entries.forEach { direction ->
            val title = when (direction) {
                GestureDirection.Up -> stringResource(R.string.gesture_up)
                GestureDirection.Down -> stringResource(R.string.gesture_down)
                GestureDirection.Left -> stringResource(R.string.gesture_left)
                GestureDirection.Right -> stringResource(R.string.gesture_right)
            }
            val action = gestures.action(direction)
            Column(verticalArrangement = Arrangement.spacedBy(StillSpacing.xxs)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = actionLabel(action),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = { onPickAction(direction) }) {
                    Text(stringResource(R.string.gesture_choose_app))
                }
            }
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
            .padding(StillSpacing.md),
        verticalArrangement = Arrangement.spacedBy(StillSpacing.xxs),
    ) {
        PickerRow(stringResource(R.string.gesture_action_none)) { onSelect(GestureAction.None) }
        PickerRow(stringResource(R.string.gesture_action_apps)) { onSelect(GestureAction.Apps) }
        PickerRow(stringResource(R.string.gesture_action_tasks)) { onSelect(GestureAction.Tasks) }
        PickerRow(stringResource(R.string.gesture_action_notifications)) {
            onSelect(GestureAction.Notifications)
        }
        PickerRow(stringResource(R.string.gesture_action_quick_settings)) {
            onSelect(GestureAction.QuickSettings)
        }
        PickerRow(stringResource(R.string.gesture_action_lock)) { onSelect(GestureAction.Lock) }
        PickerRow(stringResource(R.string.gesture_action_camera)) { onSelect(GestureAction.OpenCamera) }
        PickerRow(stringResource(R.string.gesture_action_phone)) { onSelect(GestureAction.OpenPhone) }
        PickerRow(stringResource(R.string.gesture_action_app), onChooseInstalledApp)
    }
}

@Composable
private fun PickerRow(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.fillMaxWidth())
    }
}

fun gestureActionLabel(
    action: GestureAction,
    appLabel: (app.still.launcher.AppTargetId) -> String?,
): String = when (action) {
    GestureAction.None -> "No action"
    GestureAction.Apps -> "App drawer"
    GestureAction.Tasks -> "Tasks"
    GestureAction.Notifications -> "Notification shade"
    GestureAction.QuickSettings -> "Quick settings"
    GestureAction.Lock -> "Lock screen"
    GestureAction.OpenCamera -> "Camera"
    GestureAction.OpenPhone -> "Phone"
    is GestureAction.App -> appLabel(action.id) ?: action.id.packageName
}
