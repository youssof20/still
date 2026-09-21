package app.still.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import app.still.R
import app.still.tasks.TaskEntity
import app.still.ui.PrefRow
import app.still.ui.SheetActionRow
import app.still.ui.StillSpacing
import app.still.ui.StillType

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TasksSurface(
    modifier: Modifier = Modifier,
    active: List<TaskEntity>,
    done: List<TaskEntity>,
    trash: List<TaskEntity>,
    sessionCompletedIds: Set<String>,
    draftText: String = "",
    onDraftChange: (String) -> Unit = {},
    onSubmitDraft: () -> Unit = {},
    onAddTask: () -> Unit,
    onToggleComplete: (TaskEntity, Boolean) -> Unit,
    onEdit: (TaskEntity) -> Unit,
    onMove: (String, Boolean) -> Unit,
    onTrash: (TaskEntity) -> Unit,
    onRestoreDone: (TaskEntity) -> Unit,
    onRestoreTrash: (TaskEntity) -> Unit,
    onDeleteForever: (TaskEntity) -> Unit,
    onEmptyTrash: () -> Unit,
    onExportJson: () -> Unit,
    onExportMarkdown: () -> Unit,
    onImportJson: () -> Unit,
) {
    var doneExpanded by remember { mutableStateOf(false) }
    var trashExpanded by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var confirmEmptyTrash by remember { mutableStateOf(false) }
    var longPressed by remember { mutableStateOf<TaskEntity?>(null) }

    val visibleActive = remember(active, sessionCompletedIds, done) {
        val completedHere = done.filter { it.id in sessionCompletedIds }
        active + completedHere
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = StillSpacing.settingsHorizontal)
            .padding(top = StillSpacing.s16, bottom = StillSpacing.s48),
    ) {
        BasicTextField(
            value = draftText,
            onValueChange = onDraftChange,
            singleLine = true,
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = StillType.task,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmitDraft() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = StillSpacing.s12),
            decorationBox = { inner ->
                if (draftText.isEmpty()) {
                    Text(
                        text = stringResource(R.string.task_title_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = StillType.task),
                    )
                }
                inner()
            },
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(visibleActive, key = { _, task -> task.id }) { index, task ->
                val completed = task.completedAt != null
                Text(
                    text = if (completed) "✓  ${task.title}" else "○  ${task.title}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = StillType.task,
                        textDecoration = if (completed) TextDecoration.LineThrough else null,
                    ),
                    color = if (completed) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onToggleComplete(task, !completed) },
                            onLongClick = {
                                if (!completed) longPressed = task
                                else onEdit(task)
                            },
                        )
                        .padding(vertical = StillSpacing.s12),
                )
            }

            if (done.isNotEmpty()) {
                item {
                    PrefRow(
                        title = stringResource(R.string.tasks_done_count, done.size),
                        onClick = { doneExpanded = !doneExpanded },
                    )
                }
                if (doneExpanded) {
                    itemsIndexed(done.filter { it.id !in sessionCompletedIds }, key = { _, t -> "d-${t.id}" }) { _, task ->
                        Text(
                            text = "✓  ${task.title}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = StillType.hint),
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onRestoreDone(task) },
                                    onLongClick = { onTrash(task) },
                                )
                                .padding(vertical = StillSpacing.s8),
                        )
                    }
                }
            }

            item {
                PrefRow(
                    title = stringResource(R.string.tasks_overflow),
                    onClick = { showMore = true },
                )
            }
        }
    }

    longPressed?.let { task ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { longPressed = null },
            sheetState = sheetState,
            dragHandle = null,
        ) {
            Column(modifier = Modifier.padding(StillSpacing.sheetPadding).padding(bottom = StillSpacing.s32)) {
                SheetActionRow(stringResource(R.string.edit_task)) {
                    longPressed = null
                    onEdit(task)
                }
                SheetActionRow(stringResource(R.string.move_up)) {
                    longPressed = null
                    onMove(task.id, true)
                }
                SheetActionRow(stringResource(R.string.move_down)) {
                    longPressed = null
                    onMove(task.id, false)
                }
                SheetActionRow(stringResource(R.string.move_to_trash)) {
                    longPressed = null
                    onTrash(task)
                }
            }
        }
    }

    if (showMore) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showMore = false },
            sheetState = sheetState,
            dragHandle = null,
        ) {
            Column(modifier = Modifier.padding(StillSpacing.sheetPadding).padding(bottom = StillSpacing.s32)) {
                SheetActionRow(stringResource(R.string.export_tasks_json)) {
                    showMore = false
                    onExportJson()
                }
                SheetActionRow(stringResource(R.string.export_tasks_markdown)) {
                    showMore = false
                    onExportMarkdown()
                }
                SheetActionRow(stringResource(R.string.import_tasks_json)) {
                    showMore = false
                    onImportJson()
                }
                PrefRow(
                    title = stringResource(R.string.show_trash, trash.size),
                    onClick = {
                        trashExpanded = !trashExpanded
                    },
                )
                if (trashExpanded) {
                    trash.forEach { task ->
                        SheetActionRow(task.title) {
                            showMore = false
                            onRestoreTrash(task)
                        }
                    }
                    if (trash.isNotEmpty()) {
                        SheetActionRow(stringResource(R.string.empty_trash)) {
                            showMore = false
                            confirmEmptyTrash = true
                        }
                    }
                }
            }
        }
    }

    if (confirmEmptyTrash) {
        AlertDialog(
            onDismissRequest = { confirmEmptyTrash = false },
            title = { Text(stringResource(R.string.empty_trash)) },
            text = { Text(stringResource(R.string.empty_trash_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmEmptyTrash = false
                    onEmptyTrash()
                }) { Text(stringResource(R.string.empty_trash)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmEmptyTrash = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
fun AddOrEditTaskSurface(
    modifier: Modifier = Modifier,
    initialTitle: String,
    isEdit: Boolean,
    onTitleChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    var value by remember { mutableStateOf(initialTitle) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(StillSpacing.settingsHorizontal)
            .padding(top = StillSpacing.s24),
    ) {
        BasicTextField(
            value = value,
            onValueChange = {
                value = it
                onTitleChange(it)
            },
            singleLine = true,
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = StillType.favorite,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSave() }),
            modifier = Modifier.fillMaxWidth(),
        )
        PrefRow(title = stringResource(R.string.save), onClick = onSave)
        PrefRow(title = stringResource(R.string.cancel), onClick = onCancel)
    }
}
