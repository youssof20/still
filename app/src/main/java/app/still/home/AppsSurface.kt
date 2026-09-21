package app.still.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import app.still.R
import app.still.launcher.AppSearch
import app.still.launcher.AppShortcutItem
import app.still.launcher.AppTarget
import app.still.launcher.AppTargetId
import app.still.launcher.HideMode
import app.still.ui.StillSpacing

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppsSurface(
    modifier: Modifier = Modifier,
    catalog: List<AppTarget>,
    favoriteIds: Set<AppTargetId>,
    hideModes: Map<AppTargetId, HideMode>,
    searchValue: TextFieldValue,
    focusSearch: Boolean,
    workProfilePaused: Boolean = false,
    hasWorkProfile: Boolean = false,
    shortcutsFor: (AppTargetId) -> List<AppShortcutItem> = { emptyList() },
    onSearchChange: (TextFieldValue) -> Unit,
    onLaunchTarget: (AppTargetId) -> Unit,
    onLaunchShortcut: (AppShortcutItem) -> Unit = {},
    onAddFavorite: (AppTargetId) -> Unit,
    onRemoveFavorite: (AppTargetId) -> Unit,
    onSetAlias: (AppTargetId, String?) -> Unit,
    onSetHideMode: (AppTargetId, HideMode) -> Unit,
    onOpenAppInfo: (AppTargetId) -> Unit,
    onUninstall: (AppTargetId) -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val results = remember(searchValue.text, catalog) {
        AppSearch.filterAndRank(searchValue.text, catalog)
    }
    var actionsTarget by remember { mutableStateOf<AppTarget?>(null) }
    var renameTarget by remember { mutableStateOf<AppTarget?>(null) }

    LaunchedEffect(focusSearch) {
        if (focusSearch) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = StillSpacing.md),
    ) {
        if (workProfilePaused) {
            Text(
                text = stringResource(R.string.work_profile_paused),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = StillSpacing.xs, bottom = StillSpacing.xxs),
            )
        } else if (hasWorkProfile) {
            Text(
                text = stringResource(R.string.work_profile_present),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = StillSpacing.xs, bottom = StillSpacing.xxs),
            )
        }
        OutlinedTextField(
            value = searchValue,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = { Text(stringResource(R.string.search_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { results.firstOrNull()?.let { onLaunchTarget(it.id) } },
            ),
            trailingIcon = {
                if (searchValue.text.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            onSearchChange(TextFieldValue(text = "", selection = TextRange.Zero))
                        },
                    ) {
                        Text(stringResource(R.string.clear_query))
                    }
                }
            },
        )

        if (results.isEmpty() && searchValue.text.isNotBlank()) {
            Text(
                text = stringResource(R.string.empty_results),
                modifier = Modifier.padding(top = StillSpacing.lg),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = StillSpacing.xs),
            ) {
                items(
                    results,
                    key = { it.id.flattenedComponent() + "@" + it.id.userSerialNumber },
                ) { target ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onLaunchTarget(target.id) },
                                onLongClick = { actionsTarget = target },
                            )
                            .padding(vertical = StillSpacing.sm, horizontal = StillSpacing.xs),
                    ) {
                        Text(text = target.displayLabel, style = MaterialTheme.typography.titleMedium)
                        val subtitle = buildString {
                            if (target.alias != null) {
                                append(stringResource(R.string.original_label, target.originalLabel))
                            }
                            target.profileIndicator?.let {
                                if (isNotEmpty()) append(" · ")
                                append(stringResource(R.string.profile_badge, it))
                            }
                        }
                        if (subtitle.isNotEmpty()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    actionsTarget?.let { target ->
        val shortcuts = remember(target.id) { shortcutsFor(target.id) }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { actionsTarget = null },
            sheetState = sheetState,
        ) {
            SheetColumn {
                Text(target.displayLabel, style = MaterialTheme.typography.titleMedium)
                val isFavorite = target.id in favoriteIds
                SheetAction(
                    stringResource(if (isFavorite) R.string.remove_favorite else R.string.add_favorite),
                ) {
                    if (isFavorite) onRemoveFavorite(target.id) else onAddFavorite(target.id)
                    actionsTarget = null
                }
                SheetAction(stringResource(R.string.rename_app)) {
                    renameTarget = target
                    actionsTarget = null
                }
                if (target.alias != null) {
                    SheetAction(stringResource(R.string.reset_name)) {
                        onSetAlias(target.id, null)
                        actionsTarget = null
                    }
                }
                val hideMode = hideModes[target.id] ?: HideMode.None
                if (hideMode != HideMode.FromBrowsing) {
                    SheetAction(stringResource(R.string.hide_from_browsing)) {
                        onSetHideMode(target.id, HideMode.FromBrowsing)
                        actionsTarget = null
                    }
                }
                if (hideMode != HideMode.FromLauncher) {
                    SheetAction(stringResource(R.string.hide_from_launcher)) {
                        onSetHideMode(target.id, HideMode.FromLauncher)
                        actionsTarget = null
                    }
                }
                if (hideMode != HideMode.None) {
                    SheetAction(stringResource(R.string.unhide_app)) {
                        onSetHideMode(target.id, HideMode.None)
                        actionsTarget = null
                    }
                }
                SheetAction(stringResource(R.string.app_info)) {
                    onOpenAppInfo(target.id)
                    actionsTarget = null
                }
                SheetAction(stringResource(R.string.uninstall_app)) {
                    onUninstall(target.id)
                    actionsTarget = null
                }
                if (shortcuts.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.shortcuts_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    shortcuts.forEach { item ->
                        SheetAction(item.label) {
                            onLaunchShortcut(item)
                            actionsTarget = null
                        }
                    }
                }
            }
        }
    }

    renameTarget?.let { target ->
        RenameDialog(
            target = target,
            onDismiss = { renameTarget = null },
            onConfirm = { alias ->
                onSetAlias(target.id, alias)
                renameTarget = null
            },
        )
    }
}

@Composable
private fun RenameDialog(
    target: AppTarget,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
) {
    var draft by remember {
        mutableStateOf(TextFieldValue(target.alias ?: target.originalLabel))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(StillSpacing.xs)) {
                Text(stringResource(R.string.rename_dialog_hint))
                Text(stringResource(R.string.original_label, target.originalLabel))
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(draft.text) }) {
                Text(stringResource(R.string.apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
