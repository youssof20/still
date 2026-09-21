package app.still.appearance

/**
 * Committed appearance settings. Labels are never identity keys for fonts —
 * imported fonts are referenced by stable file id under app-private storage.
 */
data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.Light,
    val colorMode: ColorMode = ColorMode.Neutral,
    val customAccentArgb: Int = DEFAULT_ACCENT,
    val customBackgroundArgb: Int = DEFAULT_BACKGROUND_LIGHT,
    val fontSource: FontSource = FontSource.System,
    val importedFontId: String? = null,
    val homeTextScale: Float = 1.0f,
    val homeTextWeight: TextWeightOption = TextWeightOption.Normal,
    val homeLineSpacing: Float = 1.0f,
    val homeAlignment: HomeAlignment = HomeAlignment.Start,
    val showWallpaperScrim: Boolean = false,
    val scrimStrength: Float = 0.35f,
) {
    companion object {
        const val DEFAULT_ACCENT = 0xFF1B1B1B.toInt()
        const val DEFAULT_BACKGROUND_LIGHT = 0xFFFAFAFA.toInt()
        const val DEFAULT_BACKGROUND_DARK = 0xFF121212.toInt()
        const val DEFAULT_BACKGROUND_BLACK = 0xFF000000.toInt()
        const val MIN_SCALE = 0.85f
        const val MAX_SCALE = 1.6f
    }
}

enum class ThemeMode {
    Light,
    Dark,
    Black,
    System,
}

enum class ColorMode {
    Neutral,
    Dynamic,
    Custom,
}

enum class FontSource {
    System,
    Sans,
    Serif,
    Mono,
    Imported,
}

enum class TextWeightOption {
    Normal,
    Medium,
    Bold,
}

enum class HomeAlignment {
    Start,
    Center,
    End,
}

data class ImportedFontInfo(
    val id: String,
    val displayName: String,
    val fileName: String,
)
