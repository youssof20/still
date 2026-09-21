package app.still.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.HapticFeedbackConstants
import app.still.R
import app.still.appearance.HomeAlignment
import app.still.launcher.AppTargetId
import app.still.launcher.FavoriteEntry
import app.still.prefs.HomeVerticalPlacement
import app.still.prefs.LauncherPreferences
import app.still.tasks.TaskEntity
import app.still.ui.LocalHomeAlignment
import app.still.ui.StillSpacing
import app.still.widgets.StillWidgetHostController
import app.still.widgets.WidgetPlacement
import app.still.widgets.WidgetShelfSection
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class HomeSheetKind {
    EditHome,
    Favorite,
    Clock,
    Date,
    TaskArea,
}

data class HomeSheetState(
    val kind: HomeSheetKind,
    val favoriteId: AppTargetId? = null,
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeSurface(
    modifier: Modifier = Modifier,
    favorites: List<FavoriteEntry>,
    launcherPrefs: LauncherPreferences,
    editingHome: Boolean,
    is24Hour: Boolean,
    taskPreview: List<TaskEntity> = emptyList(),
    widgetPlacements: List<WidgetPlacement> = emptyList(),
    widgetHost: StillWidgetHostController? = null,
    onGestureUp: () -> Unit,
    onGestureDown: () -> Unit,
    onGestureLeft: () -> Unit,
    onGestureRight: () -> Unit,
    onTapClock: () -> Unit,
    onTapDate: () -> Unit,
    onChangeClockApp: () -> Unit,
    onDisableClockTap: () -> Unit,
    onChangeDateApp: () -> Unit,
    onDisableDateTap: () -> Unit,
    onLaunchFavorite: (AppTargetId) -> Unit,
    onRemoveFavorite: (AppTargetId) -> Unit,
    onRenameFavorite: (AppTargetId) -> Unit,
    onFavoriteAppInfo: (AppTargetId) -> Unit,
    onHideFavorite: (AppTargetId) -> Unit,
    onUninstallFavorite: (AppTargetId) -> Unit,
    onMoveFavorite: (AppTargetId, Boolean) -> Unit,
    onOpenApps: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenGestures: () -> Unit,
    onOpenTasks: () -> Unit,
    onAddTask: () -> Unit,
    onTogglePreviewTaskComplete: (TaskEntity, Boolean) -> Unit,
    onOpenTask: (TaskEntity) -> Unit,
    onAddFavorite: () -> Unit,
    onAddWidget: () -> Unit,
    onOpenWidgets: () -> Unit,
    onToggleLayoutLock: () -> Unit,
    onDismissEmptyHint: () -> Unit,
    onRemoveWidget: (Int) -> Unit = {},
    onUpdateWidget: (WidgetPlacement) -> Unit = {},
    onEnterEditHome: () -> Unit,
    onExitEditHome: () -> Unit,
) {
    val homeAlignment = LocalHomeAlignment.current
    val homeTextAlign = when (homeAlignment) {
        HomeAlignment.Start -> TextAlign.Start
        HomeAlignment.Center -> TextAlign.Center
        HomeAlignment.End -> TextAlign.End
    }
    val homeContentAlignment = when (homeAlignment) {
        HomeAlignment.Start -> Alignment.Start
        HomeAlignment.Center -> Alignment.CenterHorizontally
        HomeAlignment.End -> Alignment.End
    }
    var sheet by remember { mutableStateOf<HomeSheetState?>(null) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val view = LocalView.current
    val haptic = launcherPrefs.hapticFeedback

    fun buzz() {
        if (!haptic) return
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    LaunchedEffect(launcherPrefs.showClock, launcherPrefs.showDate) {
        if (!launcherPrefs.showClock && !launcherPrefs.showDate) return@LaunchedEffect
        while (true) {
            nowMillis = System.currentTimeMillis()
            val delayMs = 60_000L - (System.currentTimeMillis() % 60_000L)
            delay(delayMs.coerceAtLeast(1_000L))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(editingHome) {
                if (editingHome) return@pointerInput
                var totalX = 0f
                var totalY = 0f
                detectDragGestures(
                    onDragStart = {
                        totalX = 0f
                        totalY = 0f
                    },
                    onDragEnd = {
                        val threshold = 100f
                        if (abs(totalX) < threshold && abs(totalY) < threshold) return@detectDragGestures
                        if (abs(totalX) > abs(totalY)) {
                            if (totalX <= -threshold) onGestureLeft()
                            else if (totalX >= threshold) onGestureRight()
                        } else {
                            if (totalY <= -threshold) onGestureUp()
                            else if (totalY >= threshold) onGestureDown()
                        }
                    },
                    onDragCancel = {
                        totalX = 0f
                        totalY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalX += dragAmount.x
                        totalY += dragAmount.y
                    },
                )
            },
    ) {
        val verticalArrangement = when (launcherPrefs.homeVerticalPlacement) {
            HomeVerticalPlacement.Top -> Arrangement.Top
            HomeVerticalPlacement.Center -> Arrangement.Center
            HomeVerticalPlacement.Bottom -> Arrangement.Bottom
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = StillSpacing.homeHorizontal, vertical = StillSpacing.homeTop),
            verticalArrangement = verticalArrangement,
            horizontalAlignment = homeContentAlignment,
        ) {
            if (launcherPrefs.showClock || launcherPrefs.showDate) {
                ClockDateBlock(
                    nowMillis = nowMillis,
                    showClock = launcherPrefs.showClock,
                    showDate = launcherPrefs.showDate,
                    is24Hour = is24Hour,
                    textAlign = homeTextAlign,
                    clockScale = launcherPrefs.clockScale,
                    dateScale = launcherPrefs.dateScale,
                    onTapClock = onTapClock,
                    onLongClock = {
                        buzz()
                        sheet = HomeSheetState(HomeSheetKind.Clock)
                    },
                    onTapDate = onTapDate,
                    onLongDate = {
                        buzz()
                        sheet = HomeSheetState(HomeSheetKind.Date)
                    },
                )
                Spacer(modifier = Modifier.height(StillSpacing.sectionGap))
            }

            if (favorites.isEmpty() && !editingHome) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true)
                        .combinedClickable(
                            onClick = { },
                            onLongClick = {
                                buzz()
                                sheet = HomeSheetState(HomeSheetKind.EditHome)
                            },
                        ),
                    horizontalAlignment = homeContentAlignment,
                ) {
                    if (!launcherPrefs.favoritesEmptyHintDismissed) {
                        Text(
                            text = stringResource(R.string.favorites_empty_short),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = homeTextAlign,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = stringResource(R.string.favorites_empty_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = homeTextAlign,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        TextButton(onClick = onDismissEmptyHint) {
                            Text(stringResource(R.string.done))
                        }
                    }
                }
            } else {
                val rowSpacing = (4f * launcherPrefs.favoriteSpacingScale).dp
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { },
                            onLongClick = {
                                buzz()
                                sheet = HomeSheetState(HomeSheetKind.EditHome)
                            },
                        ),
                    contentPadding = PaddingValues(vertical = StillSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(rowSpacing),
                    horizontalAlignment = homeContentAlignment,
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
                            textAlign = homeTextAlign,
                            contentAlignment = homeContentAlignment,
                            onLaunch = { onLaunchFavorite(entry.id) },
                            onLongPress = {
                                buzz()
                                sheet = HomeSheetState(HomeSheetKind.Favorite, entry.id)
                            },
                            onMoveUp = { onMoveFavorite(entry.id, true) },
                            onMoveDown = { onMoveFavorite(entry.id, false) },
                            onRemove = { onRemoveFavorite(entry.id) },
                        )
                    }
                }
            }

            if (widgetHost != null) {
                WidgetShelfSection(
                    placements = widgetPlacements,
                    host = widgetHost,
                    editing = editingHome,
                    onRemove = onRemoveWidget,
                    onUpdate = onUpdateWidget,
                    onAddWidget = onAddWidget,
                )
            }

            if (launcherPrefs.taskPreviewLimit > 0) {
                Spacer(modifier = Modifier.height(StillSpacing.sm))
                TaskPreviewSection(
                    tasks = taskPreview.take(launcherPrefs.taskPreviewLimit),
                    textAlign = homeTextAlign,
                    onOpenTasks = onOpenTasks,
                    onAddTask = onAddTask,
                    onToggleComplete = onTogglePreviewTaskComplete,
                    onOpenTask = onOpenTask,
                    onLongPressArea = {
                        buzz()
                        sheet = HomeSheetState(HomeSheetKind.TaskArea)
                    },
                )
            }

            if (editingHome) {
                Spacer(modifier = Modifier.height(StillSpacing.sm))
                TextButton(onClick = onExitEditHome) {
                    Text(stringResource(R.string.exit_edit_home))
                }
            }
        }
    }

    sheet?.let { state ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { sheet = null },
            sheetState = sheetState,
        ) {
            when (state.kind) {
                HomeSheetKind.EditHome -> EditHomeSheet(
                    layoutLocked = launcherPrefs.layoutLocked,
                    onAddFavorite = {
                        sheet = null
                        onAddFavorite()
                    },
                    onAddWidget = {
                        sheet = null
                        onAddWidget()
                    },
                    onTasks = {
                        sheet = null
                        onOpenTasks()
                    },
                    onAppearance = {
                        sheet = null
                        onOpenAppearance()
                    },
                    onGestures = {
                        sheet = null
                        onOpenGestures()
                    },
                    onSettings = {
                        sheet = null
                        onOpenSettings()
                    },
                    onToggleLock = {
                        onToggleLayoutLock()
                        sheet = null
                    },
                    onEditLayout = {
                        sheet = null
                        onEnterEditHome()
                    },
                    onWidgets = {
                        sheet = null
                        onOpenWidgets()
                    },
                )
                HomeSheetKind.Favorite -> {
                    val entry = favorites.firstOrNull { it.id == state.favoriteId }
                    if (entry != null) {
                        FavoriteSheet(
                            entry = entry,
                            onRename = {
                                sheet = null
                                onRenameFavorite(entry.id)
                            },
                            onRemove = {
                                sheet = null
                                onRemoveFavorite(entry.id)
                            },
                            onAppInfo = {
                                sheet = null
                                onFavoriteAppInfo(entry.id)
                            },
                            onHide = {
                                sheet = null
                                onHideFavorite(entry.id)
                            },
                            onUninstall = {
                                sheet = null
                                onUninstallFavorite(entry.id)
                            },
                        )
                    }
                }
                HomeSheetKind.Clock -> ClockSheet(
                    onChangeApp = {
                        sheet = null
                        onChangeClockApp()
                    },
                    onDisableTap = {
                        sheet = null
                        onDisableClockTap()
                    },
                    onAppearance = {
                        sheet = null
                        onOpenAppearance()
                    },
                )
                HomeSheetKind.Date -> DateSheet(
                    onChangeApp = {
                        sheet = null
                        onChangeDateApp()
                    },
                    onDisableTap = {
                        sheet = null
                        onDisableDateTap()
                    },
                    onAppearance = {
                        sheet = null
                        onOpenAppearance()
                    },
                )
                HomeSheetKind.TaskArea -> TaskAreaSheet(
                    onOpenTasks = {
                        sheet = null
                        onOpenTasks()
                    },
                    onAddTask = {
                        sheet = null
                        onAddTask()
                    },
                )
            }
        }
    }
}

@Composable
private fun EditHomeSheet(
    layoutLocked: Boolean,
    onAddFavorite: () -> Unit,
    onAddWidget: () -> Unit,
    onTasks: () -> Unit,
    onAppearance: () -> Unit,
    onGestures: () -> Unit,
    onSettings: () -> Unit,
    onToggleLock: () -> Unit,
    onEditLayout: () -> Unit,
    onWidgets: () -> Unit,
) {
    SheetColumn {
        SheetAction(stringResource(R.string.add_favorite), onAddFavorite)
        SheetAction(stringResource(R.string.widget_add), onAddWidget)
        SheetAction(stringResource(R.string.open_widgets), onWidgets)
        SheetAction(stringResource(R.string.open_tasks), onTasks)
        SheetAction(stringResource(R.string.open_appearance), onAppearance)
        SheetAction(stringResource(R.string.gestures_title), onGestures)
        SheetAction(stringResource(R.string.open_settings), onSettings)
        SheetAction(
            if (layoutLocked) stringResource(R.string.unlock_layout) else stringResource(R.string.layout_lock),
            onToggleLock,
        )
        if (!layoutLocked) {
            SheetAction(stringResource(R.string.edit_home), onEditLayout)
        }
    }
}

@Composable
private fun FavoriteSheet(
    entry: FavoriteEntry,
    onRename: () -> Unit,
    onRemove: () -> Unit,
    onAppInfo: () -> Unit,
    onHide: () -> Unit,
    onUninstall: () -> Unit,
) {
    SheetColumn {
        Text(entry.displayLabel, style = MaterialTheme.typography.titleMedium)
        SheetAction(stringResource(R.string.rename_app), onRename)
        SheetAction(stringResource(R.string.remove_favorite), onRemove)
        SheetAction(stringResource(R.string.app_info), onAppInfo)
        SheetAction(stringResource(R.string.hide_from_browsing), onHide)
        SheetAction(stringResource(R.string.uninstall_app), onUninstall)
    }
}

@Composable
private fun ClockSheet(
    onChangeApp: () -> Unit,
    onDisableTap: () -> Unit,
    onAppearance: () -> Unit,
) {
    SheetColumn {
        SheetAction(stringResource(R.string.clock_change_app), onChangeApp)
        SheetAction(stringResource(R.string.clock_disable_tap), onDisableTap)
        SheetAction(stringResource(R.string.clock_appearance), onAppearance)
    }
}

@Composable
private fun DateSheet(
    onChangeApp: () -> Unit,
    onDisableTap: () -> Unit,
    onAppearance: () -> Unit,
) {
    SheetColumn {
        SheetAction(stringResource(R.string.date_change_app), onChangeApp)
        SheetAction(stringResource(R.string.date_disable_tap), onDisableTap)
        SheetAction(stringResource(R.string.date_appearance), onAppearance)
    }
}

@Composable
private fun TaskAreaSheet(
    onOpenTasks: () -> Unit,
    onAddTask: () -> Unit,
) {
    SheetColumn {
        SheetAction(stringResource(R.string.open_tasks), onOpenTasks)
        SheetAction(stringResource(R.string.add_task), onAddTask)
    }
}

@Composable
fun SheetColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(StillSpacing.sheetPadding)
            .padding(bottom = StillSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(StillSpacing.xxs),
        content = { content() },
    )
}

@Composable
fun SheetAction(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = StillSpacing.sm, horizontal = StillSpacing.xs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClockDateBlock(
    nowMillis: Long,
    showClock: Boolean,
    showDate: Boolean,
    is24Hour: Boolean,
    textAlign: TextAlign,
    clockScale: Float,
    dateScale: Float,
    onTapClock: () -> Unit,
    onLongClock: () -> Unit,
    onTapDate: () -> Unit,
    onLongDate: () -> Unit,
) {
    val locale = Locale.getDefault()
    val timeFormat = remember(is24Hour, locale) {
        if (is24Hour) {
            DateFormat.getTimeInstance(DateFormat.SHORT, locale)
        } else {
            DateFormat.getTimeInstance(DateFormat.SHORT, locale)
        }
    }
    val dateFormat = remember(locale) {
        DateFormat.getDateInstance(DateFormat.FULL, locale)
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        if (showClock) {
            val time = timeFormat.format(Date(nowMillis))
            Text(
                text = time,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = (MaterialTheme.typography.displayMedium.fontSize.value * clockScale).sp,
                ),
                textAlign = textAlign,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = onTapClock, onLongClick = onLongClock)
                    .semantics { contentDescription = time },
            )
        }
        if (showDate) {
            val date = dateFormat.format(Date(nowMillis))
            Text(
                text = date,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = (MaterialTheme.typography.titleMedium.fontSize.value * dateScale).sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = textAlign,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = onTapDate, onLongClick = onLongDate)
                    .semantics { contentDescription = date },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteRow(
    entry: FavoriteEntry,
    editingHome: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    textAlign: TextAlign,
    contentAlignment: Alignment.Horizontal,
    onLaunch: () -> Unit,
    onLongPress: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val label = if (entry.missing) entry.id.packageName else entry.displayLabel
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = textAlign,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onLaunch, onLongClick = onLongPress)
                .padding(vertical = StillSpacing.favoriteRowVertical)
                .semantics { contentDescription = label },
        )
        if (entry.missing) {
            Text(
                text = stringResource(R.string.favorite_missing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            entry.target?.profileIndicator?.let {
                Text(
                    text = stringResource(R.string.profile_badge, it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (editingHome) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(StillSpacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Text(stringResource(R.string.move_up))
                }
                TextButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Text(stringResource(R.string.move_down))
                }
                TextButton(onClick = onRemove) {
                    Text(stringResource(R.string.remove_favorite))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskPreviewSection(
    tasks: List<TaskEntity>,
    textAlign: TextAlign,
    onOpenTasks: () -> Unit,
    onAddTask: () -> Unit,
    onToggleComplete: (TaskEntity, Boolean) -> Unit,
    onOpenTask: (TaskEntity) -> Unit,
    onLongPressArea: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpenTasks, onLongClick = onLongPressArea),
        verticalArrangement = Arrangement.spacedBy(StillSpacing.xxs),
    ) {
        Text(
            text = stringResource(R.string.tasks_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth(),
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
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f)
                        .combinedClickable(
                            onClick = { onOpenTask(task) },
                            onLongClick = onLongPressArea,
                        ),
                )
            }
        }
        TextButton(onClick = onAddTask) {
            Text(stringResource(R.string.add_task))
        }
    }
}
