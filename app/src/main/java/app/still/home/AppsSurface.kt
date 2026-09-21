package app.still.home

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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import app.still.R
import app.still.launcher.AppSearch
import app.still.launcher.AppTarget
import app.still.launcher.AppTargetId
import app.still.launcher.HideMode

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
    shortcutsFor: (AppTargetId) -> List<app.still.launcher.AppShortcutItem> = { emptyList() },
    onSearchChange: (TextFieldValue) -> Unit,
    onLaunchTarget: (AppTargetId) -> Unit,
    onLaunchShortcut: (app.still.launcher.AppShortcutItem) -> Unit = {},
    onAddFavorite: (AppTargetId) -> Unit,
    onRemoveFavorite: (AppTargetId) -> Unit,
    onSetAlias: (AppTargetId, String?) -> Unit,
    onSetHideMode: (AppTargetId, HideMode) -> Unit,
    onOpenAppInfo: (AppTargetId) -> Unit,
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
            .padding(horizontal = 16.dp),
    ) {
        if (workProfilePaused) {
            Text(
                text = stringResource(R.string.work_profile_paused),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
        } else if (hasWorkProfile) {
            Text(
                text = stringResource(R.string.work_profile_present),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
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
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(
                    results,
                    key = { it.id.flattenedComponent() + "@" + it.id.userSerialNumber },
                ) { target ->
                    AppTargetRow(
                        target = target,
                        onClick = { onLaunchTarget(target.id) },
                        onOpenMenu = { actionsTarget = target },
                    )
                }
            }
        }
    }

    actionsTarget?.let { target ->
        val shortcuts = remember(target.id) { shortcutsFor(target.id) }
        AppActionsDialog(
            target = target,
            isFavorite = target.id in favoriteIds,
            hideMode = hideModes[target.id] ?: HideMode.None,
            shortcuts = shortcuts,
            onDismiss = { actionsTarget = null },
            onAddFavorite = {
                onAddFavorite(target.id)
                actionsTarget = null
            },
            onRemoveFavorite = {
                onRemoveFavorite(target.id)
                actionsTarget = null
            },
            onRename = {
                renameTarget = target
                actionsTarget = null
            },
            onResetName = {
                onSetAlias(target.id, null)
                actionsTarget = null
            },
            onHideBrowsing = {
                onSetHideMode(target.id, HideMode.FromBrowsing)
                actionsTarget = null
            },
            onHideLauncher = {
                onSetHideMode(target.id, HideMode.FromLauncher)
                actionsTarget = null
            },
            onUnhide = {
                onSetHideMode(target.id, HideMode.None)
                actionsTarget = null
            },
            onAppInfo = {
                onOpenAppInfo(target.id)
                actionsTarget = null
            },
            onShortcut = { item ->
                onLaunchShortcut(item)
                actionsTarget = null
            },
        )
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
private fun AppTargetRow(
    target: AppTarget,
    onClick: () -> Unit,
    onOpenMenu: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TextButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
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
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        TextButton(onClick = onOpenMenu) {
            Text(stringResource(R.string.apps_menu))
        }
    }
}

@Composable
private fun AppActionsDialog(
    target: AppTarget,
    isFavorite: Boolean,
    hideMode: HideMode,
    shortcuts: List<app.still.launcher.AppShortcutItem>,
    onDismiss: () -> Unit,
    onAddFavorite: () -> Unit,
    onRemoveFavorite: () -> Unit,
    onRename: () -> Unit,
    onResetName: () -> Unit,
    onHideBrowsing: () -> Unit,
    onHideLauncher: () -> Unit,
    onUnhide: () -> Unit,
    onAppInfo: () -> Unit,
    onShortcut: (app.still.launcher.AppShortcutItem) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(target.displayLabel) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = if (isFavorite) onRemoveFavorite else onAddFavorite) {
                    Text(
                        stringResource(
                            if (isFavorite) R.string.remove_favorite else R.string.add_favorite,
                        ),
                    )
                }
                TextButton(onClick = onRename) {
                    Text(stringResource(R.string.rename_app))
                }
                if (target.alias != null) {
                    TextButton(onClick = onResetName) {
                        Text(stringResource(R.string.reset_name))
                    }
                }
                if (hideMode != HideMode.FromBrowsing) {
                    TextButton(onClick = onHideBrowsing) {
                        Text(stringResource(R.string.hide_from_browsing))
                    }
                }
                if (hideMode != HideMode.FromLauncher) {
                    TextButton(onClick = onHideLauncher) {
                        Text(stringResource(R.string.hide_from_launcher))
                    }
                }
                if (hideMode != HideMode.None) {
                    TextButton(onClick = onUnhide) {
                        Text(stringResource(R.string.unhide_app))
                    }
                }
                TextButton(onClick = onAppInfo) {
                    Text(stringResource(R.string.app_info))
                }
                if (shortcuts.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.shortcuts_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    shortcuts.forEach { item ->
                        TextButton(onClick = { onShortcut(item) }) {
                            Text(item.label)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
