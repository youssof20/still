package app.still.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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

data class LauncherPreferences(
    val favoriteIds: List<AppTargetId> = emptyList(),
    val aliases: Map<AppTargetId, String> = emptyMap(),
    val hideModes: Map<AppTargetId, HideMode> = emptyMap(),
    val layoutLocked: Boolean = false,
    val showClock: Boolean = true,
    val showDate: Boolean = true,
    val focusSearchOnOpenApps: Boolean = true,
    /** Home task preview size. 0 = Off. Default 3. */
    val taskPreviewLimit: Int = 3,
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
            taskPreviewLimit = (prefs[TASK_PREVIEW_KEY] ?: 3).coerceIn(0, 10),
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

    companion object {
        private val FAVORITES_KEY = stringPreferencesKey("favorites")
        private val ALIASES_KEY = stringPreferencesKey("aliases")
        private val HIDES_KEY = stringPreferencesKey("hide_modes")
        private val LAYOUT_LOCKED_KEY = booleanPreferencesKey("layout_locked")
        private val SHOW_CLOCK_KEY = booleanPreferencesKey("show_clock")
        private val SHOW_DATE_KEY = booleanPreferencesKey("show_date")
        private val FOCUS_SEARCH_KEY = booleanPreferencesKey("focus_search_on_apps")
        private val TASK_PREVIEW_KEY = intPreferencesKey("task_preview_limit")

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
