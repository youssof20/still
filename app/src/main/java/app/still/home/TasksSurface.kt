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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.still.R
import app.still.tasks.TaskEntity
import app.still.tasks.TaskRepository

@Composable
fun TasksSurface(
    modifier: Modifier = Modifier,
    active: List<TaskEntity>,
    done: List<TaskEntity>,
    trash: List<TaskEntity>,
    sessionCompletedIds: Set<String>,
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
    var confirmEmptyTrash by remember { mutableStateOf(false) }

    // Keep completed-this-session rows in the active list (struck through) until leaving surface.
    val visibleActive = remember(active, sessionCompletedIds, done) {
        val completedHere = done.filter { it.id in sessionCompletedIds }
        active + completedHere
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onAddTask) {
                Text(stringResource(R.string.add_task))
            }
            OutlinedButton(onClick = onExportJson) {
                Text(stringResource(R.string.export_tasks_json))
            }
            OutlinedButton(onClick = onExportMarkdown) {
                Text(stringResource(R.string.export_tasks_markdown))
            }
            OutlinedButton(onClick = onImportJson) {
                Text(stringResource(R.string.import_tasks_json))
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(visibleActive, key = { _, task -> task.id }) { index, task ->
                val completed = task.completedAt != null
                TaskRow(
                    task = task,
                    completed = completed,
                    editingControls = !completed,
                    canMoveUp = !completed && index > 0,
                    canMoveDown = !completed && index < active.lastIndex,
                    onToggle = { onToggleComplete(task, !completed) },
                    onEdit = { onEdit(task) },
                    onMoveUp = { onMove(task.id, true) },
                    onMoveDown = { onMove(task.id, false) },
                    onTrash = { onTrash(task) },
                )
            }

            item {
                TextButton(onClick = { doneExpanded = !doneExpanded }) {
                    Text(
                        stringResource(
                            if (doneExpanded) R.string.hide_done else R.string.show_done,
                            done.size,
                        ),
                    )
                }
            }
            if (doneExpanded) {
                items(done.filter { it.id !in sessionCompletedIds }, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        completed = true,
                        editingControls = false,
                        canMoveUp = false,
                        canMoveDown = false,
                        onToggle = { onRestoreDone(task) },
                        onEdit = { onEdit(task) },
                        onMoveUp = {},
                        onMoveDown = {},
                        onTrash = { onTrash(task) },
                    )
                }
            }

            item {
                TextButton(onClick = { trashExpanded = !trashExpanded }) {
                    Text(
                        stringResource(
                            if (trashExpanded) R.string.hide_trash else R.string.show_trash,
                            trash.size,
                        ),
                    )
                }
            }
            if (trashExpanded) {
                item {
                    Text(
                        stringResource(R.string.trash_retention, TaskRepository.TRASH_RETENTION_DAYS),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (trash.isNotEmpty()) {
                    item {
                        TextButton(onClick = { confirmEmptyTrash = true }) {
                            Text(stringResource(R.string.empty_trash))
                        }
                    }
                }
                items(trash, key = { "trash-${it.id}" }) { task ->
                    Column {
                        Text(task.title, style = MaterialTheme.typography.titleMedium)
                        Row {
                            TextButton(onClick = { onRestoreTrash(task) }) {
                                Text(stringResource(R.string.restore))
                            }
                            TextButton(onClick = { onDeleteForever(task) }) {
                                Text(stringResource(R.string.delete_forever))
                            }
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
                }) {
                    Text(stringResource(R.string.delete_forever))
                }
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
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var value by remember(initialTitle) { mutableStateOf(TextFieldValue(initialTitle)) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(if (isEdit) R.string.edit_task else R.string.add_task),
            style = MaterialTheme.typography.titleLarge,
        )
        OutlinedTextField(
            value = value,
            onValueChange = {
                value = it
                onTitleChange(it.text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = { Text(stringResource(R.string.task_title_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSave() }),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onSave) {
                Text(stringResource(R.string.save))
            }
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: TaskEntity,
    completed: Boolean,
    editingControls: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onTrash: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Checkbox(checked = completed, onCheckedChange = { onToggle() })
            TextButton(
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
            ) {
                Text(
                    text = task.title,
                    style = TextStyle(
                        textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (editingControls) {
            Row {
                TextButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Text(stringResource(R.string.move_up))
                }
                TextButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Text(stringResource(R.string.move_down))
                }
                TextButton(onClick = onTrash) {
                    Text(stringResource(R.string.move_to_trash))
                }
            }
        } else if (completed) {
            TextButton(onClick = onTrash) {
                Text(stringResource(R.string.move_to_trash))
            }
        }
    }
}
