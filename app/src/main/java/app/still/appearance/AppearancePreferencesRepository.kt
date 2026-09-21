package app.still.appearance

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appearanceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "appearance_preferences",
)

class AppearancePreferencesRepository(
    context: Context,
) {
    private val dataStore = context.applicationContext.appearanceDataStore

    val settings: Flow<AppearanceSettings> = dataStore.data.map { prefs ->
        AppearanceSettings(
            themeMode = enumOrDefault(prefs[THEME_MODE], ThemeMode.Light),
            colorMode = enumOrDefault(prefs[COLOR_MODE], ColorMode.Neutral),
            customAccentArgb = prefs[ACCENT] ?: AppearanceSettings.DEFAULT_ACCENT,
            customBackgroundArgb = prefs[BACKGROUND] ?: AppearanceSettings.DEFAULT_BACKGROUND_LIGHT,
            fontSource = enumOrDefault(prefs[FONT_SOURCE], FontSource.System),
            importedFontId = prefs[IMPORTED_FONT_ID],
            homeTextScale = (prefs[TEXT_SCALE] ?: 1.0f).coerceIn(
                AppearanceSettings.MIN_SCALE,
                AppearanceSettings.MAX_SCALE,
            ),
            homeTextWeight = enumOrDefault(prefs[TEXT_WEIGHT], TextWeightOption.Normal),
            homeLineSpacing = (prefs[LINE_SPACING] ?: 1.0f).coerceIn(0.9f, 1.5f),
            homeAlignment = enumOrDefault(prefs[ALIGNMENT], HomeAlignment.Start),
            showWallpaperScrim = prefs[WALLPAPER_SCRIM] ?: false,
            scrimStrength = (prefs[SCRIM_STRENGTH] ?: 0.35f).coerceIn(0f, 0.85f),
        )
    }

    suspend fun commit(settings: AppearanceSettings) {
        dataStore.edit { prefs ->
            prefs[THEME_MODE] = settings.themeMode.name
            prefs[COLOR_MODE] = settings.colorMode.name
            prefs[ACCENT] = settings.customAccentArgb
            prefs[BACKGROUND] = settings.customBackgroundArgb
            prefs[FONT_SOURCE] = settings.fontSource.name
            val imported = settings.importedFontId
            if (imported.isNullOrBlank() || settings.fontSource != FontSource.Imported) {
                prefs.remove(IMPORTED_FONT_ID)
            } else {
                prefs[IMPORTED_FONT_ID] = imported
            }
            prefs[TEXT_SCALE] = settings.homeTextScale.coerceIn(
                AppearanceSettings.MIN_SCALE,
                AppearanceSettings.MAX_SCALE,
            )
            prefs[TEXT_WEIGHT] = settings.homeTextWeight.name
            prefs[LINE_SPACING] = settings.homeLineSpacing.coerceIn(0.9f, 1.5f)
            prefs[ALIGNMENT] = settings.homeAlignment.name
            prefs[WALLPAPER_SCRIM] = settings.showWallpaperScrim
            prefs[SCRIM_STRENGTH] = settings.scrimStrength.coerceIn(0f, 0.85f)
        }
    }

    suspend fun resetToDefaults() {
        commit(AppearanceSettings())
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, default: T): T {
        if (raw.isNullOrBlank()) return default
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
    }

    companion object {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val COLOR_MODE = stringPreferencesKey("color_mode")
        private val ACCENT = intPreferencesKey("custom_accent")
        private val BACKGROUND = intPreferencesKey("custom_background")
        private val FONT_SOURCE = stringPreferencesKey("font_source")
        private val IMPORTED_FONT_ID = stringPreferencesKey("imported_font_id")
        private val TEXT_SCALE = floatPreferencesKey("home_text_scale")
        private val TEXT_WEIGHT = stringPreferencesKey("home_text_weight")
        private val LINE_SPACING = floatPreferencesKey("home_line_spacing")
        private val ALIGNMENT = stringPreferencesKey("home_alignment")
        private val WALLPAPER_SCRIM = booleanPreferencesKey("wallpaper_scrim")
        private val SCRIM_STRENGTH = floatPreferencesKey("scrim_strength")
    }
}
