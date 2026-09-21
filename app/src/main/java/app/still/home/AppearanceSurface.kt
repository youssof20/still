package app.still.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.still.R
import app.still.appearance.AppearanceSettings
import app.still.appearance.ColorMode
import app.still.appearance.FontSource
import app.still.appearance.HomeAlignment
import app.still.appearance.ImportedFontInfo
import app.still.appearance.TextWeightOption
import app.still.appearance.ThemeMode

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
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.appearance_preview_label), style = MaterialTheme.typography.titleMedium)
        AppearancePreviewSample(draft = draft)

        if (contrastWarning) {
            Text(
                stringResource(R.string.appearance_contrast_warning),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (fontLoadFailed) {
            Text(
                stringResource(R.string.appearance_font_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Text(stringResource(R.string.appearance_theme_mode), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = draft.themeMode == mode,
                    onClick = { onDraftChange(draft.copy(themeMode = mode)) },
                    label = { Text(modeLabel(mode)) },
                )
            }
        }

        Text(stringResource(R.string.appearance_color_mode), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ColorMode.entries.forEach { mode ->
                FilterChip(
                    selected = draft.colorMode == mode,
                    onClick = { onDraftChange(draft.copy(colorMode = mode)) },
                    label = { Text(colorModeLabel(mode)) },
                )
            }
        }

        if (draft.colorMode == ColorMode.Custom) {
            AccentPresets(
                selectedAccent = draft.customAccentArgb,
                selectedBackground = draft.customBackgroundArgb,
                onAccent = { onDraftChange(draft.copy(customAccentArgb = it)) },
                onBackground = { onDraftChange(draft.copy(customBackgroundArgb = it)) },
            )
        }

        Text(stringResource(R.string.appearance_font), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.appearance_font_settings_note), style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                FontSource.System,
                FontSource.Sans,
                FontSource.Serif,
                FontSource.Mono,
            ).forEach { source ->
                FilterChip(
                    selected = draft.fontSource == source,
                    onClick = {
                        onDraftChange(draft.copy(fontSource = source, importedFontId = null))
                    },
                    label = { Text(fontSourceLabel(source)) },
                )
            }
        }
        OutlinedButton(onClick = onImportFont) {
            Text(stringResource(R.string.appearance_import_font))
        }
        importedFonts.forEach { font ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = draft.fontSource == FontSource.Imported && draft.importedFontId == font.id,
                    onClick = {
                        onDraftChange(
                            draft.copy(fontSource = FontSource.Imported, importedFontId = font.id),
                        )
                    },
                    label = { Text(font.displayName) },
                )
                TextButton(onClick = { onDeleteImportedFont(font.id) }) {
                    Text(stringResource(R.string.appearance_delete_font))
                }
            }
        }

        Text(stringResource(R.string.appearance_text_scale), style = MaterialTheme.typography.titleMedium)
        Slider(
            value = draft.homeTextScale,
            onValueChange = { onDraftChange(draft.copy(homeTextScale = it)) },
            valueRange = AppearanceSettings.MIN_SCALE..AppearanceSettings.MAX_SCALE,
        )

        Text(stringResource(R.string.appearance_text_weight), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextWeightOption.entries.forEach { weight ->
                FilterChip(
                    selected = draft.homeTextWeight == weight,
                    onClick = { onDraftChange(draft.copy(homeTextWeight = weight)) },
                    label = { Text(weight.name) },
                )
            }
        }

        Text(stringResource(R.string.appearance_line_spacing), style = MaterialTheme.typography.titleMedium)
        Slider(
            value = draft.homeLineSpacing,
            onValueChange = { onDraftChange(draft.copy(homeLineSpacing = it)) },
            valueRange = 0.9f..1.5f,
        )

        Text(stringResource(R.string.appearance_alignment), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeAlignment.entries.forEach { align ->
                FilterChip(
                    selected = draft.homeAlignment == align,
                    onClick = { onDraftChange(draft.copy(homeAlignment = align)) },
                    label = { Text(align.name) },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.appearance_wallpaper_scrim))
                Text(stringResource(R.string.appearance_wallpaper_scrim_note), style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = draft.showWallpaperScrim,
                onCheckedChange = { onDraftChange(draft.copy(showWallpaperScrim = it)) },
            )
        }
        if (draft.showWallpaperScrim) {
            Text(stringResource(R.string.appearance_scrim_strength))
            Slider(
                value = draft.scrimStrength,
                onValueChange = { onDraftChange(draft.copy(scrimStrength = it)) },
                valueRange = 0f..0.85f,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onApply, enabled = hasUnsavedChanges && !contrastWarning) {
                Text(stringResource(R.string.appearance_apply))
            }
            TextButton(onClick = onCancel, enabled = hasUnsavedChanges) {
                Text(stringResource(R.string.cancel))
            }
            TextButton(onClick = onReset) {
                Text(stringResource(R.string.appearance_reset))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onExportPreset) {
                Text(stringResource(R.string.appearance_export_preset))
            }
            OutlinedButton(onClick = onImportPreset) {
                Text(stringResource(R.string.appearance_import_preset))
            }
        }
    }
}

@Composable
private fun AppearancePreviewSample(draft: AppearanceSettings) {
    val align = when (draft.homeAlignment) {
        HomeAlignment.Start -> TextAlign.Start
        HomeAlignment.Center -> TextAlign.Center
        HomeAlignment.End -> TextAlign.End
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("12:34", style = MaterialTheme.typography.displaySmall, textAlign = align, modifier = Modifier.fillMaxWidth())
        Text("Messages — long label sample", style = MaterialTheme.typography.headlineSmall, textAlign = align, modifier = Modifier.fillMaxWidth())
        Text("Buy milk · メモ · غداً", style = MaterialTheme.typography.titleMedium, textAlign = align, modifier = Modifier.fillMaxWidth())
        Text("Punctuation: A, B; C — “quote” (ok)?", style = MaterialTheme.typography.bodyMedium, textAlign = align, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun AccentPresets(
    selectedAccent: Int,
    selectedBackground: Int,
    onAccent: (Int) -> Unit,
    onBackground: (Int) -> Unit,
) {
    Text(stringResource(R.string.appearance_accent), style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            0xFF1B1B1B.toInt() to "Ink",
            0xFF0B57D0.toInt() to "Blue",
            0xFF1B7F4E.toInt() to "Green",
            0xFF8B1E3F.toInt() to "Wine",
        ).forEach { (color, label) ->
            FilterChip(
                selected = selectedAccent == color,
                onClick = { onAccent(color) },
                label = { Text(label) },
            )
        }
    }
    Text(stringResource(R.string.appearance_background), style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            0xFFFAFAFA.toInt() to "Light",
            0xFF121212.toInt() to "Dark",
            0xFF000000.toInt() to "Black",
            0xFFF5F0E8.toInt() to "Paper",
        ).forEach { (color, label) ->
            FilterChip(
                selected = selectedBackground == color,
                onClick = { onBackground(color) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun modeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.Light -> stringResource(R.string.appearance_mode_light)
    ThemeMode.Dark -> stringResource(R.string.appearance_mode_dark)
    ThemeMode.Black -> stringResource(R.string.appearance_mode_black)
    ThemeMode.System -> stringResource(R.string.appearance_mode_system)
}

@Composable
private fun colorModeLabel(mode: ColorMode): String = when (mode) {
    ColorMode.Neutral -> stringResource(R.string.appearance_color_neutral)
    ColorMode.Dynamic -> stringResource(R.string.appearance_color_dynamic)
    ColorMode.Custom -> stringResource(R.string.appearance_color_custom)
}

@Composable
private fun fontSourceLabel(source: FontSource): String = when (source) {
    FontSource.System -> stringResource(R.string.appearance_font_system)
    FontSource.Sans -> stringResource(R.string.appearance_font_sans)
    FontSource.Serif -> stringResource(R.string.appearance_font_serif)
    FontSource.Mono -> stringResource(R.string.appearance_font_mono)
    FontSource.Imported -> stringResource(R.string.appearance_font_imported)
}
