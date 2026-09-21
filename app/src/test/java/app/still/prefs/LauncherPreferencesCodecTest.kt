package app.still.prefs

import app.still.launcher.AppTargetId
import app.still.launcher.HideMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LauncherPreferencesCodecTest {
    private val camera = AppTargetId("com.camera", "com.camera.Main", 0L)
    private val phone = AppTargetId("com.phone", "com.phone.Main", 10L)

    @Test
    fun favoritesRoundTripPreservesOrderWithoutCap() {
        val ids = (0 until 12).map { index ->
            AppTargetId("com.app$index", "com.app$index.Main", index.toLong())
        }
        val encoded = AppTargetIdCodec.encodeList(ids)
        assertThat(AppTargetIdCodec.decodeList(encoded)).isEqualTo(ids)
    }

    @Test
    fun aliasesPreserveOriginalKeyAndSkipCorruptRows() {
        val encoded = LauncherPreferencesRepository.encodeAliases(
            mapOf(camera to "Snap", phone to "Call"),
        )
        val decoded = LauncherPreferencesRepository.decodeAliases(encoded)
        assertThat(decoded[camera]).isEqualTo("Snap")
        assertThat(decoded[phone]).isEqualTo("Call")
        assertThat(LauncherPreferencesRepository.decodeAliases("not-valid")).isEmpty()
    }

    @Test
    fun hideModesRoundTripAndIgnoreNone() {
        val encoded = LauncherPreferencesRepository.encodeHides(
            mapOf(camera to HideMode.FromBrowsing, phone to HideMode.FromLauncher),
        )
        val decoded = LauncherPreferencesRepository.decodeHides(encoded)
        assertThat(decoded[camera]).isEqualTo(HideMode.FromBrowsing)
        assertThat(decoded[phone]).isEqualTo(HideMode.FromLauncher)
    }

    @Test
    fun corruptFavoriteRowsAreDroppedWithoutFailingWholeList() {
        val good = AppTargetIdCodec.encode(camera)
        val decoded = AppTargetIdCodec.decodeList("$good\nnot-a-target\n")
        assertThat(decoded).containsExactly(camera)
    }
}
