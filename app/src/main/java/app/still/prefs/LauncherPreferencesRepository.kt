package app.still.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.still.launcher.AppTargetId
import app.still.launcher.HideMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "launcher_preferences",
)

enum class HomeVerticalPlacement {
    Top,
    Center,
    Bottom,
}

data class LauncherPreferences(
    val favoriteIds: List<AppTargetId> = emptyList(),
    val aliases: Map<AppTargetId, String> = emptyMap(),
    val hideModes: Map<AppTargetId, HideMode> = emptyMap(),
    val layoutLocked: Boolean = false,
    val showClock: Boolean = true,
    val showDate: Boolean = true,
    val focusSearchOnOpenApps: Boolean = true,
    /** Home task preview size. 0 = Off. Default off for a quieter home. */
    val taskPreviewLimit: Int = 0,
    val clockTapEnabled: Boolean = true,
    val dateTapEnabled: Boolean = true,
    /** null = system default clock/calendar resolution */
    val clockAppId: AppTargetId? = null,
    val dateAppId: AppTargetId? = null,
    val favoritesEmptyHintDismissed: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val hapticFeedback: Boolean = false,
    val homeVerticalPlacement: HomeVerticalPlacement = HomeVerticalPlacement.Top,
    val favoriteSpacingScale: Float = 1.0f,
    val clockScale: Float = 1.0f,
    val dateScale: Float = 1.0f,
)

class LauncherPreferencesRepository(
    context: Context,
) {
    private val dataStore = context.applicationContext.launcherDataStore

    val preferences: Flow<LauncherPreferences> = dataStore.data.map { prefs ->
        LauncherPreferences(
            favoriteIds = AppTargetIdCodec.decodeList(prefs[FAVORITES_KEY]),
            aliases = decodeAliases(prefs[ALIASES_KEY]),
            hideModes = decodeHides(prefs[HIDES_KEY]),
            layoutLocked = prefs[LAYOUT_LOCKED_KEY] ?: false,
            showClock = prefs[SHOW_CLOCK_KEY] ?: true,
            showDate = prefs[SHOW_DATE_KEY] ?: true,
            focusSearchOnOpenApps = prefs[FOCUS_SEARCH_KEY] ?: true,
            taskPreviewLimit = (prefs[TASK_PREVIEW_KEY] ?: 0).coerceIn(0, 10),
            clockTapEnabled = prefs[CLOCK_TAP_KEY] ?: true,
            dateTapEnabled = prefs[DATE_TAP_KEY] ?: true,
            clockAppId = prefs[CLOCK_APP_KEY]?.let { AppTargetIdCodec.decode(it) },
            dateAppId = prefs[DATE_APP_KEY]?.let { AppTargetIdCodec.decode(it) },
            favoritesEmptyHintDismissed = prefs[EMPTY_HINT_KEY] ?: false,
            onboardingCompleted = prefs[ONBOARDING_KEY] ?: false,
            hapticFeedback = prefs[HAPTIC_KEY] ?: false,
            homeVerticalPlacement = prefs[VERTICAL_KEY]?.let {
                runCatching { HomeVerticalPlacement.valueOf(it) }.getOrNull()
            } ?: HomeVerticalPlacement.Top,
            favoriteSpacingScale = (prefs[FAV_SPACING_KEY] ?: 1.0f).coerceIn(0.75f, 1.5f),
            clockScale = (prefs[CLOCK_SCALE_KEY] ?: 1.0f).coerceIn(0.8f, 1.6f),
            dateScale = (prefs[DATE_SCALE_KEY] ?: 1.0f).coerceIn(0.8f, 1.6f),
        )
    }

    suspend fun setFavorites(ids: List<AppTargetId>) {
        dataStore.edit { it[FAVORITES_KEY] = AppTargetIdCodec.encodeList(ids) }
    }

    suspend fun addFavorite(id: AppTargetId) {
        dataStore.edit { prefs ->
            val current = AppTargetIdCodec.decodeList(prefs[FAVORITES_KEY]).toMutableList()
            if (id !in current) {
                current += id
                prefs[FAVORITES_KEY] = AppTargetIdCodec.encodeList(current)
            }
        }
    }

    suspend fun removeFavorite(id: AppTargetId) {
        dataStore.edit { prefs ->
            val current = AppTargetIdCodec.decodeList(prefs[FAVORITES_KEY]).filterNot { it == id }
            prefs[FAVORITES_KEY] = AppTargetIdCodec.encodeList(current)
        }
    }

    suspend fun moveFavorite(id: AppTargetId, towardStart: Boolean) {
        dataStore.edit { prefs ->
            val current = AppTargetIdCodec.decodeList(prefs[FAVORITES_KEY]).toMutableList()
            val index = current.indexOf(id)
            if (index < 0) return@edit
            val swapWith = if (towardStart) index - 1 else index + 1
            if (swapWith !in current.indices) return@edit
            val tmp = current[index]
            current[index] = current[swapWith]
            current[swapWith] = tmp
            prefs[FAVORITES_KEY] = AppTargetIdCodec.encodeList(current)
        }
    }

    suspend fun setAlias(id: AppTargetId, alias: String?) {
        dataStore.edit { prefs ->
            val map = decodeAliases(prefs[ALIASES_KEY]).toMutableMap()
            val trimmed = alias?.trim().orEmpty()
            if (trimmed.isEmpty()) {
                map.remove(id)
            } else {
                map[id] = trimmed
            }
            prefs[ALIASES_KEY] = encodeAliases(map)
        }
    }

    suspend fun setHideMode(id: AppTargetId, mode: HideMode) {
        dataStore.edit { prefs ->
            val map = decodeHides(prefs[HIDES_KEY]).toMutableMap()
            if (mode == HideMode.None) {
                map.remove(id)
            } else {
                map[id] = mode
            }
            prefs[HIDES_KEY] = encodeHides(map)
        }
    }

    suspend fun setLayoutLocked(locked: Boolean) {
        dataStore.edit { it[LAYOUT_LOCKED_KEY] = locked }
    }

    suspend fun setShowClock(show: Boolean) {
        dataStore.edit { it[SHOW_CLOCK_KEY] = show }
    }

    suspend fun setShowDate(show: Boolean) {
        dataStore.edit { it[SHOW_DATE_KEY] = show }
    }

    suspend fun setFocusSearchOnOpenApps(focus: Boolean) {
        dataStore.edit { it[FOCUS_SEARCH_KEY] = focus }
    }

    suspend fun setTaskPreviewLimit(limit: Int) {
        dataStore.edit { it[TASK_PREVIEW_KEY] = limit.coerceIn(0, 10) }
    }

    suspend fun setClockTapEnabled(enabled: Boolean) {
        dataStore.edit { it[CLOCK_TAP_KEY] = enabled }
    }

    suspend fun setDateTapEnabled(enabled: Boolean) {
        dataStore.edit { it[DATE_TAP_KEY] = enabled }
    }

    suspend fun setClockApp(id: AppTargetId?) {
        dataStore.edit {
            if (id == null) it.remove(CLOCK_APP_KEY)
            else it[CLOCK_APP_KEY] = AppTargetIdCodec.encode(id)
        }
    }

    suspend fun setDateApp(id: AppTargetId?) {
        dataStore.edit {
            if (id == null) it.remove(DATE_APP_KEY)
            else it[DATE_APP_KEY] = AppTargetIdCodec.encode(id)
        }
    }

    suspend fun setFavoritesEmptyHintDismissed(dismissed: Boolean) {
        dataStore.edit { it[EMPTY_HINT_KEY] = dismissed }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[ONBOARDING_KEY] = completed }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        dataStore.edit { it[HAPTIC_KEY] = enabled }
    }

    suspend fun setHomeVerticalPlacement(placement: HomeVerticalPlacement) {
        dataStore.edit { it[VERTICAL_KEY] = placement.name }
    }

    suspend fun setFavoriteSpacingScale(scale: Float) {
        dataStore.edit { it[FAV_SPACING_KEY] = scale.coerceIn(0.75f, 1.5f) }
    }

    suspend fun setClockScale(scale: Float) {
        dataStore.edit { it[CLOCK_SCALE_KEY] = scale.coerceIn(0.8f, 1.6f) }
    }

    suspend fun setDateScale(scale: Float) {
        dataStore.edit { it[DATE_SCALE_KEY] = scale.coerceIn(0.8f, 1.6f) }
    }

    companion object {
        private val FAVORITES_KEY = stringPreferencesKey("favorites")
        private val ALIASES_KEY = stringPreferencesKey("aliases")
        private val HIDES_KEY = stringPreferencesKey("hide_modes")
        private val LAYOUT_LOCKED_KEY = booleanPreferencesKey("layout_locked")
        private val SHOW_CLOCK_KEY = booleanPreferencesKey("show_clock")
        private val SHOW_DATE_KEY = booleanPreferencesKey("show_date")
        private val FOCUS_SEARCH_KEY = booleanPreferencesKey("focus_search_on_apps")
        private val TASK_PREVIEW_KEY = intPreferencesKey("task_preview_limit")
        private val CLOCK_TAP_KEY = booleanPreferencesKey("clock_tap_enabled")
        private val DATE_TAP_KEY = booleanPreferencesKey("date_tap_enabled")
        private val CLOCK_APP_KEY = stringPreferencesKey("clock_app")
        private val DATE_APP_KEY = stringPreferencesKey("date_app")
        private val EMPTY_HINT_KEY = booleanPreferencesKey("favorites_empty_hint_dismissed")
        private val ONBOARDING_KEY = booleanPreferencesKey("onboarding_completed")
        private val HAPTIC_KEY = booleanPreferencesKey("haptic_feedback")
        private val VERTICAL_KEY = stringPreferencesKey("home_vertical_placement")
        private val FAV_SPACING_KEY = floatPreferencesKey("favorite_spacing_scale")
        private val CLOCK_SCALE_KEY = floatPreferencesKey("clock_scale")
        private val DATE_SCALE_KEY = floatPreferencesKey("date_scale")

        fun encodeAliases(map: Map<AppTargetId, String>): String =
            map.entries.joinToString(AppTargetIdCodec.RECORD_SEP) { (id, alias) ->
                AppTargetIdCodec.encode(id) + AppTargetIdCodec.VALUE_SEP + alias.replace("\n", " ")
            }

        fun decodeAliases(raw: String?): Map<AppTargetId, String> {
            if (raw.isNullOrBlank()) return emptyMap()
            return raw.split(AppTargetIdCodec.RECORD_SEP).mapNotNull { line ->
                val sep = line.indexOf(AppTargetIdCodec.VALUE_SEP)
                if (sep <= 0) return@mapNotNull null
                val id = AppTargetIdCodec.decode(line.substring(0, sep)) ?: return@mapNotNull null
                val alias = line.substring(sep + 1).trim()
                if (alias.isEmpty()) null else id to alias
            }.toMap()
        }

        fun encodeHides(map: Map<AppTargetId, HideMode>): String =
            map.entries.joinToString(AppTargetIdCodec.RECORD_SEP) { (id, mode) ->
                AppTargetIdCodec.encode(id) + AppTargetIdCodec.VALUE_SEP + mode.name
            }

        fun decodeHides(raw: String?): Map<AppTargetId, HideMode> {
            if (raw.isNullOrBlank()) return emptyMap()
            return raw.split(AppTargetIdCodec.RECORD_SEP).mapNotNull { line ->
                val sep = line.indexOf(AppTargetIdCodec.VALUE_SEP)
                if (sep <= 0) return@mapNotNull null
                val id = AppTargetIdCodec.decode(line.substring(0, sep)) ?: return@mapNotNull null
                val mode = runCatching {
                    HideMode.valueOf(line.substring(sep + 1))
                }.getOrNull() ?: return@mapNotNull null
                if (mode == HideMode.None) null else id to mode
            }.toMap()
        }
    }
}
