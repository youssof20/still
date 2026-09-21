package app.still.widgets

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.widgetDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "widget_placements",
)

class WidgetPlacementRepository(
    context: Context,
) {
    private val dataStore = context.applicationContext.widgetDataStore

    val placements: Flow<List<WidgetPlacement>> = dataStore.data.map { prefs ->
        decodeList(prefs[PLACEMENTS_KEY])
    }

    suspend fun setPlacements(list: List<WidgetPlacement>) {
        dataStore.edit { it[PLACEMENTS_KEY] = encodeList(list) }
    }

    suspend fun add(placement: WidgetPlacement) {
        dataStore.edit { prefs ->
            val current = decodeList(prefs[PLACEMENTS_KEY]).toMutableList()
            current.removeAll { it.appWidgetId == placement.appWidgetId }
            current += placement
            prefs[PLACEMENTS_KEY] = encodeList(current)
        }
    }

    suspend fun remove(appWidgetId: Int) {
        dataStore.edit { prefs ->
            val current = decodeList(prefs[PLACEMENTS_KEY]).filterNot { it.appWidgetId == appWidgetId }
            prefs[PLACEMENTS_KEY] = encodeList(current)
        }
    }

    suspend fun update(placement: WidgetPlacement) {
        dataStore.edit { prefs ->
            val current = decodeList(prefs[PLACEMENTS_KEY]).map {
                if (it.appWidgetId == placement.appWidgetId) placement else it
            }
            prefs[PLACEMENTS_KEY] = encodeList(current)
        }
    }

    companion object {
        private val PLACEMENTS_KEY = stringPreferencesKey("placements")
        private const val RECORD = "\n"
        private const val FIELD = "\u0001"

        fun encodeList(list: List<WidgetPlacement>): String =
            list.joinToString(RECORD) { p ->
                listOf(
                    p.appWidgetId.toString(),
                    p.providerFlattened,
                    p.span.name,
                    p.alignment.name,
                ).joinToString(FIELD)
            }

        fun decodeList(raw: String?): List<WidgetPlacement> {
            if (raw.isNullOrBlank()) return emptyList()
            return raw.split(RECORD).mapNotNull { line ->
                val parts = line.split(FIELD)
                if (parts.size < 2) return@mapNotNull null
                val id = parts[0].toIntOrNull() ?: return@mapNotNull null
                val provider = parts[1].ifBlank { return@mapNotNull null }
                val span = parts.getOrNull(2)?.let {
                    runCatching { WidgetSpan.valueOf(it) }.getOrDefault(WidgetSpan.Full)
                } ?: WidgetSpan.Full
                val alignment = parts.getOrNull(3)?.let {
                    runCatching { WidgetAlignment.valueOf(it) }.getOrDefault(WidgetAlignment.Stretch)
                } ?: WidgetAlignment.Stretch
                runCatching {
                    WidgetPlacement(id, provider, span, alignment)
                }.getOrNull()
            }
        }
    }
}
