package app.still.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.still.launcher.AppTargetId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.gestureDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "gesture_preferences",
)

enum class GestureActionSlot {
    Camera,
    Phone,
}

sealed class GestureTargetPreference {
    data object Disabled : GestureTargetPreference()
    data object Unset : GestureTargetPreference()
    data class Target(val id: AppTargetId) : GestureTargetPreference()
}

data class GesturePreferences(
    val camera: GestureTargetPreference = GestureTargetPreference.Unset,
    val phone: GestureTargetPreference = GestureTargetPreference.Unset,
)

class GesturePreferencesRepository(
    context: Context,
) {
    private val dataStore = context.applicationContext.gestureDataStore

    val preferences: Flow<GesturePreferences> = dataStore.data.map { prefs ->
        GesturePreferences(
            camera = decode(prefs[CAMERA_KEY]),
            phone = decode(prefs[PHONE_KEY]),
        )
    }

    suspend fun setTarget(slot: GestureActionSlot, preference: GestureTargetPreference) {
        dataStore.edit { prefs ->
            val key = keyFor(slot)
            when (preference) {
                GestureTargetPreference.Unset -> prefs.remove(key)
                GestureTargetPreference.Disabled -> prefs[key] = DISABLED_SENTINEL
                is GestureTargetPreference.Target -> prefs[key] = encode(preference.id)
            }
        }
    }

    private fun keyFor(slot: GestureActionSlot) = when (slot) {
        GestureActionSlot.Camera -> CAMERA_KEY
        GestureActionSlot.Phone -> PHONE_KEY
    }

    companion object {
        private val CAMERA_KEY = stringPreferencesKey("camera_target")
        private val PHONE_KEY = stringPreferencesKey("phone_target")
        private const val DISABLED_SENTINEL = "disabled"
        private const val FIELD_SEP = "\u0001"

        fun encode(id: AppTargetId): String =
            listOf(id.packageName, id.activityClassName, id.userSerialNumber.toString())
                .joinToString(FIELD_SEP)

        fun decode(raw: String?): GestureTargetPreference {
            if (raw == null) return GestureTargetPreference.Unset
            if (raw == DISABLED_SENTINEL) return GestureTargetPreference.Disabled
            val parts = raw.split(FIELD_SEP)
            if (parts.size != 3) return GestureTargetPreference.Unset
            val serial = parts[2].toLongOrNull() ?: return GestureTargetPreference.Unset
            return runCatching {
                GestureTargetPreference.Target(
                    AppTargetId.parse(parts[0], parts[1], serial),
                )
            }.getOrDefault(GestureTargetPreference.Unset)
        }
    }
}
