package app.still.prefs

import app.still.launcher.AppTargetId
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GesturePreferencesCodecTest {
    @Test
    fun roundTripNamedActions() {
        listOf(
            GestureAction.None,
            GestureAction.Apps,
            GestureAction.Tasks,
            GestureAction.Notifications,
            GestureAction.QuickSettings,
            GestureAction.Lock,
            GestureAction.OpenCamera,
            GestureAction.OpenPhone,
        ).forEach { action ->
            val encoded = GesturePreferencesRepository.encode(action)
            val decoded = GesturePreferencesRepository.decode(encoded, GestureAction.None)
            assertThat(decoded).isEqualTo(action)
        }
    }

    @Test
    fun roundTripAppAction() {
        val id = AppTargetId("com.android.camera", "com.android.camera.Camera", 0L)
        val action = GestureAction.App(id)
        val encoded = GesturePreferencesRepository.encode(action)
        assertThat(GesturePreferencesRepository.decode(encoded, GestureAction.None)).isEqualTo(action)
    }

    @Test
    fun legacyDisabledBecomesNone() {
        assertThat(GesturePreferencesRepository.decode("disabled", GestureAction.OpenCamera))
            .isEqualTo(GestureAction.None)
    }

    @Test
    fun nullUsesDefault() {
        assertThat(GesturePreferencesRepository.decode(null, GestureAction.Apps))
            .isEqualTo(GestureAction.Apps)
    }
}
