package app.still.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import app.still.R
import app.still.launcher.AppSearch
import app.still.launcher.AppShortcutItem
import app.still.launcher.AppTarget
import app.still.launcher.AppTargetId
import app.still.launcher.HideMode
import app.still.ui.SheetActionRow
import app.still.ui.StillSpacing
import app.still.ui.StillType

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
            .padding(horizontal = StillSpacing.homeHorizontal)
            .padding(top = StillSpacing.s16),
    ) {
        if (workProfilePaused) {
            Text(
                text = stringResource(R.string.work_profile_paused),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = StillType.hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = StillSpacing.s8),
            )
        }
        BasicTextField(
            value = searchValue,
            onValueChange = onSearchChange,
            singleLine = true,
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = StillType.drawerApp,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { results.firstOrNull()?.let { onLaunchTarget(it.id) } },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .padding(vertical = StillSpacing.s12),
            decorationBox = { inner ->
                if (searchValue.text.isEmpty()) {
                    Text(
                        text = stringResource(R.string.search_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = StillType.drawerApp),
                    )
                }
                inner()
            },
        )

        if (results.isEmpty() && searchValue.text.isNotBlank()) {
            Text(
                text = stringResource(R.string.empty_results),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = StillSpacing.s24),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(
                    results,
                    key = { it.id.flattenedComponent() + "@" + it.id.userSerialNumber },
                ) { target ->
                    Text(
                        text = target.displayLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = StillType.drawerApp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onLaunchTarget(target.id) },
                                onLongClick = { actionsTarget = target },
                            )
                            .padding(vertical = StillSpacing.s12),
                    )
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
            dragHandle = null,
        ) {
            Column(
                modifier = Modifier
                    .padding(StillSpacing.sheetPadding)
                    .padding(bottom = StillSpacing.s32),
            ) {
                val isFavorite = target.id in favoriteIds
                SheetActionRow(
                    stringResource(if (isFavorite) R.string.remove_favorite else R.string.add_favorite),
                ) {
                    if (isFavorite) onRemoveFavorite(target.id) else onAddFavorite(target.id)
                    actionsTarget = null
                }
                SheetActionRow(stringResource(R.string.rename_app)) {
                    renameTarget = target
                    actionsTarget = null
                }
                if (target.alias != null) {
                    SheetActionRow(stringResource(R.string.reset_name)) {
                        onSetAlias(target.id, null)
                        actionsTarget = null
                    }
                }
                val hideMode = hideModes[target.id] ?: HideMode.None
                if (hideMode == HideMode.None) {
                    SheetActionRow(stringResource(R.string.hide_from_browsing)) {
                        onSetHideMode(target.id, HideMode.FromBrowsing)
                        actionsTarget = null
                    }
                    SheetActionRow(stringResource(R.string.hide_from_launcher)) {
                        onSetHideMode(target.id, HideMode.FromLauncher)
                        actionsTarget = null
                    }
                } else {
                    SheetActionRow(stringResource(R.string.unhide_app)) {
                        onSetHideMode(target.id, HideMode.None)
                        actionsTarget = null
                    }
                }
                SheetActionRow(stringResource(R.string.app_info)) {
                    onOpenAppInfo(target.id)
                    actionsTarget = null
                }
                SheetActionRow(stringResource(R.string.uninstall_app)) {
                    onUninstall(target.id)
                    actionsTarget = null
                }
                shortcuts.take(4).forEach { item ->
                    SheetActionRow(item.label) {
                        onLaunchShortcut(item)
                        actionsTarget = null
                    }
                }
            }
        }
    }

    renameTarget?.let { target ->
        var draft by remember {
            mutableStateOf(TextFieldValue(target.alias ?: target.originalLabel))
        }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(stringResource(R.string.rename_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSetAlias(target.id, draft.text)
                    renameTarget = null
                }) { Text(stringResource(R.string.apply)) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
