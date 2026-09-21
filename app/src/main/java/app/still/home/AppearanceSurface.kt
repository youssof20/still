package app.still.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import app.still.R
import app.still.appearance.AppearancePresets
import app.still.appearance.AppearanceSettings
import app.still.appearance.ColorMode
import app.still.appearance.FontSource
import app.still.appearance.HomeAlignment
import app.still.appearance.ImportedFontInfo
import app.still.appearance.TextWeightOption
import app.still.appearance.ThemeMode
import app.still.ui.PrefRow
import app.still.ui.PrefSection
import app.still.ui.SheetActionRow
import app.still.ui.StillSpacing
import app.still.ui.StillType

private enum class AppearancePane {
    Root,
    Presets,
    Theme,
    Typeface,
    Weight,
    Alignment,
}

@Composable
fun AppearanceSurface(
    modifier: Modifier = Modifier,
    draft: AppearanceSettings,
    importedFonts: List<ImportedFontInfo>,
    contrastWarning: Boolean,
    fontLoadFailed: Boolean,
    hasUnsavedChanges: Boolean,
    onDraftChange: (AppearanceSettings) -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit,
    onImportFont: () -> Unit,
    onDeleteImportedFont: (String) -> Unit,
    onExportPreset: () -> Unit,
    onImportPreset: () -> Unit,
) {
    var pane by remember { mutableStateOf(AppearancePane.Root) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = StillSpacing.settingsHorizontal)
            .padding(bottom = StillSpacing.s48),
    ) {
        when (pane) {
            AppearancePane.Root -> {
                Text(
                    text = stringResource(R.string.appearance_preview_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = StillSpacing.s16, bottom = StillSpacing.s8),
                )
                AppearancePreviewQuiet(draft)

                if (contrastWarning) {
                    Text(
                        stringResource(R.string.appearance_contrast_warning),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = StillSpacing.s8),
                    )
                }
                if (fontLoadFailed) {
                    Text(
                        stringResource(R.string.appearance_font_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                PrefSection(stringResource(R.string.appearance_title))
                PrefRow(
                    title = stringResource(R.string.appearance_presets),
                    value = presetNameFor(draft),
                    onClick = { pane = AppearancePane.Presets },
                )
                PrefRow(
                    title = stringResource(R.string.appearance_theme_mode),
                    value = modeLabel(draft.themeMode),
                    onClick = { pane = AppearancePane.Theme },
                )
                PrefRow(
                    title = stringResource(R.string.appearance_typeface),
                    value = fontSourceLabel(draft.fontSource),
                    onClick = { pane = AppearancePane.Typeface },
                )
                PrefRow(
                    title = stringResource(R.string.appearance_text_scale),
                    value = String.format("%.0f%%", draft.homeTextScale * 100),
                    onClick = {
                        val next = when {
                            draft.homeTextScale < 0.95f -> 1.0f
                            draft.homeTextScale < 1.1f -> 1.15f
                            draft.homeTextScale < 1.3f -> 1.35f
                            else -> 0.9f
                        }
                        onDraftChange(draft.copy(homeTextScale = next))
                    },
                )
                PrefRow(
                    title = stringResource(R.string.appearance_text_weight),
                    value = draft.homeTextWeight.name,
                    onClick = { pane = AppearancePane.Weight },
                )
                PrefRow(
                    title = stringResource(R.string.appearance_alignment),
                    value = draft.homeAlignment.name,
                    onClick = { pane = AppearancePane.Alignment },
                )

                PrefSection(stringResource(R.string.apply))
                PrefRow(
                    title = stringResource(R.string.appearance_apply),
                    value = if (hasUnsavedChanges) "•" else null,
                    onClick = if (hasUnsavedChanges && !contrastWarning) onApply else null,
                )
                PrefRow(title = stringResource(R.string.cancel), onClick = onCancel)
                PrefRow(title = stringResource(R.string.appearance_reset), onClick = onReset)
                PrefRow(title = stringResource(R.string.appearance_export_preset), onClick = onExportPreset)
                PrefRow(title = stringResource(R.string.appearance_import_preset), onClick = onImportPreset)
            }
            AppearancePane.Presets -> {
                PrefSection(stringResource(R.string.appearance_presets))
                AppearancePresets.all().forEach { (name, preset) ->
                    PrefRow(
                        title = name,
                        value = if (presetNameFor(draft) == name) "✓" else null,
                        onClick = {
                            onDraftChange(preset)
                            pane = AppearancePane.Root
                        },
                    )
                }
                PrefRow(title = stringResource(R.string.back), onClick = { pane = AppearancePane.Root })
            }
            AppearancePane.Theme -> {
                PrefSection(stringResource(R.string.appearance_theme_mode))
                ThemeMode.entries.forEach { mode ->
                    PrefRow(
                        title = modeLabel(mode),
                        value = if (draft.themeMode == mode) "✓" else null,
                        onClick = {
                            onDraftChange(draft.copy(themeMode = mode))
                            pane = AppearancePane.Root
                        },
                    )
                }
                PrefRow(title = stringResource(R.string.back), onClick = { pane = AppearancePane.Root })
            }
            AppearancePane.Typeface -> {
                PrefSection(stringResource(R.string.appearance_typeface))
                listOf(FontSource.System, FontSource.Sans, FontSource.Serif, FontSource.Mono).forEach { source ->
                    PrefRow(
                        title = fontSourceLabel(source),
                        value = if (draft.fontSource == source) "✓" else null,
                        onClick = {
                            onDraftChange(draft.copy(fontSource = source, importedFontId = null))
                            pane = AppearancePane.Root
                        },
                    )
                }
                PrefRow(title = stringResource(R.string.appearance_import_font), onClick = onImportFont)
                importedFonts.forEach { font ->
                    PrefRow(
                        title = font.displayName,
                        value = if (draft.importedFontId == font.id) "✓" else "×",
                        onClick = {
                            onDraftChange(
                                draft.copy(fontSource = FontSource.Imported, importedFontId = font.id),
                            )
                            pane = AppearancePane.Root
                        },
                    )
                }
                PrefRow(title = stringResource(R.string.back), onClick = { pane = AppearancePane.Root })
            }
            AppearancePane.Weight -> {
                PrefSection(stringResource(R.string.appearance_text_weight))
                TextWeightOption.entries.forEach { weight ->
                    PrefRow(
                        title = weight.name,
                        value = if (draft.homeTextWeight == weight) "✓" else null,
                        onClick = {
                            onDraftChange(draft.copy(homeTextWeight = weight))
                            pane = AppearancePane.Root
                        },
                    )
                }
                PrefRow(title = stringResource(R.string.back), onClick = { pane = AppearancePane.Root })
            }
            AppearancePane.Alignment -> {
                PrefSection(stringResource(R.string.appearance_alignment))
                HomeAlignment.entries.forEach { align ->
                    PrefRow(
                        title = align.name,
                        value = if (draft.homeAlignment == align) "✓" else null,
                        onClick = {
                            onDraftChange(draft.copy(homeAlignment = align))
                            pane = AppearancePane.Root
                        },
                    )
                }
                PrefRow(title = stringResource(R.string.back), onClick = { pane = AppearancePane.Root })
            }
        }
    }
}

@Composable
private fun AppearancePreviewQuiet(draft: AppearanceSettings) {
    val align = when (draft.homeAlignment) {
        HomeAlignment.Start -> TextAlign.Start
        HomeAlignment.Center -> TextAlign.Center
        HomeAlignment.End -> TextAlign.End
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = StillSpacing.s8),
    ) {
        Text(
            "18:51",
            style = MaterialTheme.typography.displayMedium.copy(fontSize = StillType.clock),
            textAlign = align,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Monday, 21 September",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = StillType.date),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = align,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Camera",
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = StillType.favorite),
            textAlign = align,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = StillSpacing.s16),
        )
        Text(
            "Messages",
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = StillType.favorite),
            textAlign = align,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "○ Buy groceries",
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = StillType.task),
            textAlign = align,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = StillSpacing.s12),
        )
    }
}

private fun presetNameFor(draft: AppearanceSettings): String {
    return AppearancePresets.all().firstOrNull { (_, preset) ->
        preset.themeMode == draft.themeMode &&
            preset.fontSource == draft.fontSource &&
            preset.homeAlignment == draft.homeAlignment &&
            preset.colorMode == draft.colorMode
    }?.first ?: "Custom"
}

@Composable
private fun modeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.Light -> stringResource(R.string.appearance_mode_light)
    ThemeMode.Dark -> stringResource(R.string.appearance_mode_dark)
    ThemeMode.Black -> stringResource(R.string.appearance_mode_black)
    ThemeMode.System -> stringResource(R.string.appearance_mode_system)
}

@Composable
private fun fontSourceLabel(source: FontSource): String = when (source) {
    FontSource.System -> stringResource(R.string.appearance_font_system)
    FontSource.Sans -> stringResource(R.string.appearance_font_sans)
    FontSource.Serif -> stringResource(R.string.appearance_font_serif)
    FontSource.Mono -> stringResource(R.string.appearance_font_mono)
    FontSource.Imported -> stringResource(R.string.appearance_font_imported)
}
