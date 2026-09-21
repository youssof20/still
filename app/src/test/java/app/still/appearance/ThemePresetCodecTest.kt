package app.still.appearance

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ThemePresetCodecTest {
    @Test
    fun roundTripDropsImportedFontBinaryReference() {
        val settings = AppearanceSettings(
            themeMode = ThemeMode.Dark,
            colorMode = ColorMode.Custom,
            customAccentArgb = 0xFF0B57D0.toInt(),
            customBackgroundArgb = 0xFF121212.toInt(),
            fontSource = FontSource.Imported,
            importedFontId = "abc123",
            homeTextScale = 1.2f,
            homeTextWeight = TextWeightOption.Medium,
            homeAlignment = HomeAlignment.Center,
        )
        val parsed = ThemePresetCodec.parse(ThemePresetCodec.toJson(settings)).getOrThrow()
        assertThat(parsed.themeMode).isEqualTo(ThemeMode.Dark)
        assertThat(parsed.colorMode).isEqualTo(ColorMode.Custom)
        assertThat(parsed.fontSource).isEqualTo(FontSource.System)
        assertThat(parsed.importedFontId).isNull()
        assertThat(parsed.homeTextScale).isWithin(0.01f).of(1.2f)
        assertThat(parsed.homeAlignment).isEqualTo(HomeAlignment.Center)
    }

    @Test
    fun outOfRangeScaleIsClamped() {
        val json = """
            {"format":"still.theme","version":1,"themeMode":"Light","colorMode":"Neutral",
            "customAccentArgb":1,"customBackgroundArgb":2,"fontSource":"Sans",
            "homeTextScale":9.0,"homeTextWeight":"Normal","homeLineSpacing":3.0,
            "homeAlignment":"Start","showWallpaperScrim":false,"scrimStrength":2.0}
        """.trimIndent()
        val parsed = ThemePresetCodec.parse(json).getOrThrow()
        assertThat(parsed.homeTextScale).isEqualTo(AppearanceSettings.MAX_SCALE)
        assertThat(parsed.homeLineSpacing).isEqualTo(1.5f)
        assertThat(parsed.scrimStrength).isEqualTo(0.85f)
    }

    @Test
    fun invalidPresetFailsWithoutThrowingFromResult() {
        assertThat(ThemePresetCodec.parse("not-json").isFailure).isTrue()
        assertThat(ThemePresetCodec.parse("""{"format":"other","version":1}""").isFailure).isTrue()
    }
}

class AppearanceContrastTest {
    @Test
    fun blackOnWhitePasses() {
        assertThat(
            AppearanceResolver.hasReadableContrast(Color.Black, Color.White),
        ).isTrue()
    }

    @Test
    fun nearIdenticalFails() {
        assertThat(
            AppearanceResolver.hasReadableContrast(Color(0xFF808080), Color(0xFF858585)),
        ).isFalse()
    }
}
