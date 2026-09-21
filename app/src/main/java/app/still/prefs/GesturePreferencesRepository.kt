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

enum class GestureDirection {
    Up,
    Down,
    Left,
    Right,
}

/**
 * What a home swipe does. App targets use [App]; camera/phone defaults resolve at run time
 * without forcing those apps onto the home chrome.
 */
sealed class GestureAction {
    data object None : GestureAction()
    data object Apps : GestureAction()
    data object Tasks : GestureAction()
    data object Notifications : GestureAction()
    data object QuickSettings : GestureAction()
    data object Lock : GestureAction()
    data object OpenCamera : GestureAction()
    data object OpenPhone : GestureAction()
    data class App(val id: AppTargetId) : GestureAction()
}

data class GesturePreferences(
    val up: GestureAction = GestureAction.Apps,
    val down: GestureAction = GestureAction.Notifications,
    val left: GestureAction = GestureAction.OpenCamera,
    val right: GestureAction = GestureAction.OpenPhone,
) {
    fun action(direction: GestureDirection): GestureAction = when (direction) {
        GestureDirection.Up -> up
        GestureDirection.Down -> down
        GestureDirection.Left -> left
        GestureDirection.Right -> right
    }
}

class GesturePreferencesRepository(
    context: Context,
) {
    private val dataStore = context.applicationContext.gestureDataStore

    val preferences: Flow<GesturePreferences> = dataStore.data.map { prefs ->
        GesturePreferences(
            up = decode(prefs[UP_KEY], default = GestureAction.Apps),
            down = decode(prefs[DOWN_KEY], default = GestureAction.Notifications),
            left = decodeLegacyOrNew(prefs[LEFT_KEY], prefs[LEGACY_CAMERA_KEY], GestureAction.OpenCamera),
            right = decodeLegacyOrNew(prefs[RIGHT_KEY], prefs[LEGACY_PHONE_KEY], GestureAction.OpenPhone),
        )
    }

    suspend fun setAction(direction: GestureDirection, action: GestureAction) {
        dataStore.edit { prefs ->
            prefs[keyFor(direction)] = encode(action)
            // Clear legacy keys once rewritten so migration does not stick.
            when (direction) {
                GestureDirection.Left -> prefs.remove(LEGACY_CAMERA_KEY)
                GestureDirection.Right -> prefs.remove(LEGACY_PHONE_KEY)
                else -> Unit
            }
        }
    }

    private fun keyFor(direction: GestureDirection) = when (direction) {
        GestureDirection.Up -> UP_KEY
        GestureDirection.Down -> DOWN_KEY
        GestureDirection.Left -> LEFT_KEY
        GestureDirection.Right -> RIGHT_KEY
    }

    companion object {
        private val UP_KEY = stringPreferencesKey("gesture_up")
        private val DOWN_KEY = stringPreferencesKey("gesture_down")
        private val LEFT_KEY = stringPreferencesKey("gesture_left")
        private val RIGHT_KEY = stringPreferencesKey("gesture_right")
        private val LEGACY_CAMERA_KEY = stringPreferencesKey("camera_target")
        private val LEGACY_PHONE_KEY = stringPreferencesKey("phone_target")
        private const val DISABLED_SENTINEL = "disabled"

        fun encode(action: GestureAction): String = when (action) {
            GestureAction.None -> "none"
            GestureAction.Apps -> "apps"
            GestureAction.Tasks -> "tasks"
            GestureAction.Notifications -> "notifications"
            GestureAction.QuickSettings -> "quick_settings"
            GestureAction.Lock -> "lock"
            GestureAction.OpenCamera -> "open_camera"
            GestureAction.OpenPhone -> "open_phone"
            is GestureAction.App -> "app:" + AppTargetIdCodec.encode(action.id)
        }

        fun decode(raw: String?, default: GestureAction): GestureAction {
            if (raw == null) return default
            return when (raw) {
                "none", DISABLED_SENTINEL -> GestureAction.None
                "apps" -> GestureAction.Apps
                "tasks" -> GestureAction.Tasks
                "notifications" -> GestureAction.Notifications
                "quick_settings" -> GestureAction.QuickSettings
                "lock" -> GestureAction.Lock
                "open_camera" -> GestureAction.OpenCamera
                "open_phone" -> GestureAction.OpenPhone
                else -> {
                    if (raw.startsWith("app:")) {
                        val id = AppTargetIdCodec.decode(raw.removePrefix("app:"))
                        if (id != null) GestureAction.App(id) else default
                    } else {
                        // Legacy absolute AppTargetId codec string
                        val id = AppTargetIdCodec.decode(raw)
                        if (id != null) GestureAction.App(id) else default
                    }
                }
            }
        }

        private fun decodeLegacyOrNew(
            primary: String?,
            legacy: String?,
            default: GestureAction,
        ): GestureAction {
            if (primary != null) return decode(primary, default)
            if (legacy == null) return default
            if (legacy == DISABLED_SENTINEL) return GestureAction.None
            val id = AppTargetIdCodec.decode(legacy)
            return if (id != null) GestureAction.App(id) else default
        }
    }
}
