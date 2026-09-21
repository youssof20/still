package app.still.prefs

import app.still.launcher.AppTargetId
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GesturePreferencesCodecTest {
    @Test
    fun roundTripTarget() {
        val id = AppTargetId("com.android.camera", "com.android.camera.Camera", 0L)
        val encoded = GesturePreferencesRepository.encode(id)
        val decoded = GesturePreferencesRepository.decode(encoded)
        assertThat(decoded).isEqualTo(GestureTargetPreference.Target(id))
    }

    @Test
    fun disabledAndUnsetSentinels() {
        assertThat(GesturePreferencesRepository.decode(null))
            .isEqualTo(GestureTargetPreference.Unset)
        assertThat(GesturePreferencesRepository.decode("disabled"))
            .isEqualTo(GestureTargetPreference.Disabled)
    }

    @Test
    fun corruptPayloadFallsBackToUnset() {
        assertThat(GesturePreferencesRepository.decode("not-a-target"))
            .isEqualTo(GestureTargetPreference.Unset)
    }
}
