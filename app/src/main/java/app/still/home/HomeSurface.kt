package app.still.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.still.R
import app.still.launcher.AppTargetId
import app.still.launcher.FavoriteEntry
import app.still.prefs.LauncherPreferences
import app.still.tasks.TaskEntity
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeSurface(
    modifier: Modifier = Modifier,
    defaultHomeHeld: Boolean,
    favorites: List<FavoriteEntry>,
    launcherPrefs: LauncherPreferences,
    editingHome: Boolean,
    is24Hour: Boolean,
    taskPreview: List<TaskEntity> = emptyList(),
    taskPreviewLimit: Int = 0,
    onOpenApps: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTasks: () -> Unit = {},
    onAddTask: () -> Unit = {},
    onTogglePreviewTaskComplete: (TaskEntity, Boolean) -> Unit = { _, _ -> },
    onOpenTask: (TaskEntity) -> Unit = {},
    onCamera: () -> Unit,
    onPhone: () -> Unit,
    onRequestDefaultHome: () -> Unit,
    onLaunchFavorite: (AppTargetId) -> Unit,
    onEnterEditHome: () -> Unit,
    onExitEditHome: () -> Unit,
    onRemoveFavorite: (AppTargetId) -> Unit,
    onMoveFavorite: (AppTargetId, Boolean) -> Unit,
    onReplaceFavorite: (AppTargetId) -> Unit,
) {
    var accumulatedHorizontal by remember { mutableStateOf(0f) }
    var showHomeMenu by remember { mutableStateOf(false) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(launcherPrefs.showClock, launcherPrefs.showDate) {
        if (!launcherPrefs.showClock && !launcherPrefs.showDate) return@LaunchedEffect
        while (true) {
            nowMillis = System.currentTimeMillis()
            val delayMs = 60_000L - (System.currentTimeMillis() % 60_000L)
            delay(delayMs.coerceAtLeast(1_000L))
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(editingHome) {
                    if (editingHome) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { accumulatedHorizontal = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            accumulatedHorizontal += dragAmount
                        },
                        onDragEnd = {
                            val threshold = 120f
                            when {
                                accumulatedHorizontal <= -threshold -> onCamera()
                                accumulatedHorizontal >= threshold -> onPhone()
                            }
                            accumulatedHorizontal = 0f
                        },
                        onDragCancel = { accumulatedHorizontal = 0f },
                    )
                }
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { showHomeMenu = false },
                        onLongClick = { showHomeMenu = true },
                    ),
            ) {
                if (launcherPrefs.showClock || launcherPrefs.showDate) {
                    ClockBlock(
                        nowMillis = nowMillis,
                        showClock = launcherPrefs.showClock,
                        showDate = launcherPrefs.showDate,
                        is24Hour = is24Hour,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge,
                    )
                }
            }

            if (!defaultHomeHeld) {
                Text(
                    text = stringResource(R.string.default_home_inactive),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = onRequestDefaultHome) {
                    Text(stringResource(R.string.set_as_default_home))
                }
            }

            if (showHomeMenu) {
                HomeActionsMenu(
                    editingHome = editingHome,
                    layoutLocked = launcherPrefs.layoutLocked,
                    onEditHome = {
                        showHomeMenu = false
                        onEnterEditHome()
                    },
                    onExitEdit = {
                        showHomeMenu = false
                        onExitEditHome()
                    },
                    onSettings = {
                        showHomeMenu = false
                        onOpenSettings()
                    },
                    onDismiss = { showHomeMenu = false },
                )
            }

            if (favorites.isEmpty() && !editingHome) {
                Text(
                    text = stringResource(R.string.favorites_empty),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    itemsIndexed(
                        favorites,
                        key = { _, entry ->
                            entry.id.flattenedComponent() + "@" + entry.id.userSerialNumber
                        },
                    ) { index, entry ->
                        FavoriteRow(
                            entry = entry,
                            editingHome = editingHome,
                            canMoveUp = index > 0,
                            canMoveDown = index < favorites.lastIndex,
                            onLaunch = { onLaunchFavorite(entry.id) },
                            onRemove = { onRemoveFavorite(entry.id) },
                            onMoveUp = { onMoveFavorite(entry.id, true) },
                            onMoveDown = { onMoveFavorite(entry.id, false) },
                            onReplace = { onReplaceFavorite(entry.id) },
                        )
                    }
                }
            }

            if (taskPreviewLimit > 0) {
                TaskPreviewSection(
                    tasks = taskPreview.take(taskPreviewLimit),
                    onOpenTasks = onOpenTasks,
                    onAddTask = onAddTask,
                    onToggleComplete = onTogglePreviewTaskComplete,
                    onOpenTask = onOpenTask,
                )
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
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .pointerInput(Unit) {
                    var accumulated = 0f
                    detectVerticalDragGestures(
                        onDragStart = { accumulated = 0f },
                        onVerticalDrag = { _, dragAmount -> accumulated += dragAmount },
                        onDragEnd = {
                            if (accumulated < -80f) onOpenApps()
                            accumulated = 0f
                        },
                        onDragCancel = { accumulated = 0f },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            TextButton(onClick = onOpenApps) {
                Text(stringResource(R.string.open_apps))
            }
        }
    }
}

@Composable
private fun ClockBlock(
    nowMillis: Long,
    showClock: Boolean,
    showDate: Boolean,
    is24Hour: Boolean,
) {
    val date = Date(nowMillis)
    val timeText = if (showClock) {
        DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(date)
    } else {
        null
    }
    val dateText = if (showDate) {
        DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault()).format(date)
    } else {
        null
    }
    Column {
        timeText?.let {
            Text(text = it, style = MaterialTheme.typography.displaySmall)
        }
        dateText?.let {
            Text(text = it, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun TaskPreviewSection(
    tasks: List<TaskEntity>,
    onOpenTasks: () -> Unit,
    onAddTask: () -> Unit,
    onToggleComplete: (TaskEntity, Boolean) -> Unit,
    onOpenTask: (TaskEntity) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.tasks_title),
            style = MaterialTheme.typography.titleMedium,
        )
        tasks.forEach { task ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Checkbox(
                    checked = false,
                    onCheckedChange = { onToggleComplete(task, true) },
                )
                TextButton(
                    onClick = { onOpenTask(task) },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onAddTask) {
                Text(stringResource(R.string.add_task))
            }
            OutlinedButton(onClick = onOpenTasks) {
                Text(stringResource(R.string.open_tasks))
            }
        }
    }
}

@Composable
private fun HomeActionsMenu(
    editingHome: Boolean,
    layoutLocked: Boolean,
    onEditHome: () -> Unit,
    onExitEdit: () -> Unit,
    onSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.home_actions_title), style = MaterialTheme.typography.titleMedium)
        if (editingHome) {
            TextButton(onClick = onExitEdit) {
                Text(stringResource(R.string.exit_edit_home))
            }
        } else {
            TextButton(onClick = onEditHome) {
                Text(
                    if (layoutLocked) {
                        stringResource(R.string.layout_locked)
                    } else {
                        stringResource(R.string.edit_home)
                    },
                )
            }
        }
        TextButton(onClick = onSettings) {
            Text(stringResource(R.string.open_settings))
        }
        TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.cancel))
        }
    }
}

@Composable
private fun FavoriteRow(
    entry: FavoriteEntry,
    editingHome: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onLaunch: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onReplace: () -> Unit,
) {
    val label = if (entry.missing) entry.id.packageName else entry.displayLabel
    Column(modifier = Modifier.fillMaxWidth()) {
        TextButton(
            onClick = {
                if (entry.missing) onReplace() else onLaunch()
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = label },
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 4.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(text = label, style = MaterialTheme.typography.headlineSmall)
                if (entry.missing) {
                    Text(
                        text = stringResource(R.string.favorite_missing),
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    entry.target?.profileIndicator?.let {
                        Text(
                            text = stringResource(R.string.profile_badge, it),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
        if (editingHome) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Text(stringResource(R.string.move_up))
                }
                TextButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Text(stringResource(R.string.move_down))
                }
                TextButton(onClick = onRemove) {
                    Text(stringResource(R.string.remove_favorite))
                }
                if (entry.missing) {
                    TextButton(onClick = onReplace) {
                        Text(stringResource(R.string.recover_target))
                    }
                }
            }
        }
    }
}
