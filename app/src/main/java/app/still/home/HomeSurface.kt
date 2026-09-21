package app.still.home

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.still.R
import app.still.appearance.HomeAlignment
import app.still.launcher.AppTargetId
import app.still.launcher.FavoriteEntry
import app.still.prefs.HomeVerticalPlacement
import app.still.prefs.LauncherPreferences
import app.still.tasks.TaskEntity
import app.still.ui.LocalHomeAlignment
import app.still.ui.SheetActionRow
import app.still.ui.StillSpacing
import app.still.ui.StillType
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private enum class HomeSheetKind { Menu, Favorite, Clock, Date }

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeSurface(
    modifier: Modifier = Modifier,
    favorites: List<FavoriteEntry>,
    launcherPrefs: LauncherPreferences,
    is24Hour: Boolean,
    taskPreview: List<TaskEntity> = emptyList(),
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
    onOpenSettings: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenTasks: () -> Unit,
    onAddTask: () -> Unit,
    onTogglePreviewTaskComplete: (TaskEntity, Boolean) -> Unit,
    onOpenTask: (TaskEntity) -> Unit,
    onAddFavorite: () -> Unit,
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
    var sheet by remember { mutableStateOf<HomeSheetKind?>(null) }
    var favoriteSheetId by remember { mutableStateOf<AppTargetId?>(null) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val view = LocalView.current

    fun buzz() {
        if (!launcherPrefs.hapticFeedback) return
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
            .pointerInput(Unit) {
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
                .padding(horizontal = StillSpacing.homeHorizontal)
                .padding(top = StillSpacing.homeTop, bottom = StillSpacing.s48),
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
                        sheet = HomeSheetKind.Clock
                    },
                    onTapDate = onTapDate,
                    onLongDate = {
                        buzz()
                        sheet = HomeSheetKind.Date
                    },
                )
                Spacer(modifier = Modifier.height(StillSpacing.sectionGap))
            }

            if (favorites.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true)
                        .combinedClickable(
                            onClick = { },
                            onLongClick = {
                                buzz()
                                sheet = HomeSheetKind.Menu
                            },
                        ),
                    horizontalAlignment = homeContentAlignment,
                ) {
                    if (!launcherPrefs.favoritesEmptyHintDismissed) {
                        Text(
                            text = stringResource(R.string.favorites_empty_short),
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = StillType.hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = homeTextAlign,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = stringResource(R.string.favorites_empty_hint),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = StillType.hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = homeTextAlign,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = StillSpacing.s4),
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { },
                            onLongClick = {
                                buzz()
                                sheet = HomeSheetKind.Menu
                            },
                        ),
                    verticalArrangement = Arrangement.Top,
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
                            textAlign = homeTextAlign,
                            onLaunch = { onLaunchFavorite(entry.id) },
                            onLongPress = {
                                buzz()
                                favoriteSheetId = entry.id
                                sheet = HomeSheetKind.Favorite
                            },
                        )
                    }
                }
            }

            if (launcherPrefs.taskPreviewLimit > 0) {
                Spacer(modifier = Modifier.height(StillSpacing.s16))
                TaskPreviewBlock(
                    tasks = taskPreview.take(launcherPrefs.taskPreviewLimit),
                    textAlign = homeTextAlign,
                    onToggleComplete = onTogglePreviewTaskComplete,
                    onOpenTask = onOpenTask,
                    onAddTask = onAddTask,
                    onOpenTasks = onOpenTasks,
                )
            }
        }
    }

    sheet?.let { kind ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                sheet = null
                favoriteSheetId = null
            },
            sheetState = sheetState,
            dragHandle = null,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = StillSpacing.sheetPadding)
                    .padding(top = StillSpacing.s8, bottom = StillSpacing.s32),
            ) {
                when (kind) {
                    HomeSheetKind.Menu -> {
                        SheetActionRow(stringResource(R.string.open_tasks)) {
                            sheet = null
                            onOpenTasks()
                        }
                        SheetActionRow(stringResource(R.string.open_appearance)) {
                            sheet = null
                            onOpenAppearance()
                        }
                        SheetActionRow(stringResource(R.string.open_settings)) {
                            sheet = null
                            onOpenSettings()
                        }
                        SheetActionRow(stringResource(R.string.add_favorite)) {
                            sheet = null
                            onAddFavorite()
                        }
                    }
                    HomeSheetKind.Favorite -> {
                        val entry = favorites.firstOrNull { it.id == favoriteSheetId }
                        if (entry != null) {
                            val idx = favorites.indexOf(entry)
                            Text(
                                entry.displayLabel,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    horizontal = StillSpacing.s8,
                                    vertical = StillSpacing.s8,
                                ),
                            )
                            SheetActionRow(stringResource(R.string.rename_app)) {
                                sheet = null
                                onRenameFavorite(entry.id)
                            }
                            if (idx > 0) {
                                SheetActionRow(stringResource(R.string.move_up)) {
                                    sheet = null
                                    onMoveFavorite(entry.id, true)
                                }
                            }
                            if (idx < favorites.lastIndex) {
                                SheetActionRow(stringResource(R.string.move_down)) {
                                    sheet = null
                                    onMoveFavorite(entry.id, false)
                                }
                            }
                            SheetActionRow(stringResource(R.string.remove_favorite)) {
                                sheet = null
                                onRemoveFavorite(entry.id)
                            }
                            SheetActionRow(stringResource(R.string.hide_from_browsing)) {
                                sheet = null
                                onHideFavorite(entry.id)
                            }
                            SheetActionRow(stringResource(R.string.app_info)) {
                                sheet = null
                                onFavoriteAppInfo(entry.id)
                            }
                            SheetActionRow(stringResource(R.string.uninstall_app)) {
                                sheet = null
                                onUninstallFavorite(entry.id)
                            }
                        }
                    }
                    HomeSheetKind.Clock -> {
                        SheetActionRow(stringResource(R.string.clock_change_app)) {
                            sheet = null
                            onChangeClockApp()
                        }
                        SheetActionRow(stringResource(R.string.clock_disable_tap)) {
                            sheet = null
                            onDisableClockTap()
                        }
                    }
                    HomeSheetKind.Date -> {
                        SheetActionRow(stringResource(R.string.date_change_app)) {
                            sheet = null
                            onChangeDateApp()
                        }
                        SheetActionRow(stringResource(R.string.date_disable_tap)) {
                            sheet = null
                            onDisableDateTap()
                        }
                    }
                }
            }
        }
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
        DateFormat.getTimeInstance(DateFormat.SHORT, locale)
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
                    fontSize = (StillType.clock.value * clockScale).sp,
                    fontWeight = FontWeight.Light,
                    lineHeight = (StillType.clock.value * clockScale * 1.1f).sp,
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
                    fontSize = (StillType.date.value * dateScale).sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = textAlign,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = StillSpacing.s4)
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
    textAlign: TextAlign,
    onLaunch: () -> Unit,
    onLongPress: () -> Unit,
) {
    val label = if (entry.missing) entry.id.packageName else entry.displayLabel
    Text(
        text = label,
        style = MaterialTheme.typography.headlineSmall.copy(
            fontSize = StillType.favorite,
            fontWeight = FontWeight.Normal,
            lineHeight = (StillType.favorite.value * 1.25f).sp,
        ),
        textAlign = textAlign,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onLaunch, onLongClick = onLongPress)
            .padding(vertical = StillSpacing.favoriteRowVertical)
            .semantics { contentDescription = label },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskPreviewBlock(
    tasks: List<TaskEntity>,
    textAlign: TextAlign,
    onToggleComplete: (TaskEntity, Boolean) -> Unit,
    onOpenTask: (TaskEntity) -> Unit,
    onAddTask: () -> Unit,
    onOpenTasks: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpenTasks, onLongClick = onOpenTasks),
        verticalArrangement = Arrangement.spacedBy(StillSpacing.s4),
    ) {
        tasks.forEach { task ->
            Text(
                text = "○  ${task.title}",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = StillType.task),
                textAlign = textAlign,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onToggleComplete(task, true) },
                        onLongClick = { onOpenTask(task) },
                    )
                    .padding(vertical = StillSpacing.s4),
            )
        }
        Text(
            text = stringResource(R.string.add_task_inline),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = StillType.hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = textAlign,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onAddTask, onLongClick = onOpenTasks)
                .padding(vertical = StillSpacing.s4),
        )
    }
}
