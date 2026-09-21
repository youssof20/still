package app.still.appearance

/**
 * Named appearance starting points. Presets never lock settings — they only
 * fill a draft the user can still edit and Apply.
 */
object AppearancePresets {
    fun still(): AppearanceSettings = AppearanceSettings(
        themeMode = ThemeMode.Black,
        colorMode = ColorMode.Neutral,
        customAccentArgb = 0xFFE8E8E8.toInt(),
        customBackgroundArgb = 0xFF0A0A0A.toInt(),
        fontSource = FontSource.Sans,
        homeTextScale = 1.0f,
        homeTextWeight = TextWeightOption.Normal,
        homeLineSpacing = 1.05f,
        homeAlignment = HomeAlignment.Start,
    )

    fun oled(): AppearanceSettings = AppearanceSettings(
        themeMode = ThemeMode.Black,
        colorMode = ColorMode.Neutral,
        customAccentArgb = 0xFFFFFFFF.toInt(),
        customBackgroundArgb = 0xFF000000.toInt(),
        fontSource = FontSource.Sans,
        homeTextScale = 1.0f,
        homeTextWeight = TextWeightOption.Medium,
        homeAlignment = HomeAlignment.Start,
    )

    fun paper(): AppearanceSettings = AppearanceSettings(
        themeMode = ThemeMode.Light,
        colorMode = ColorMode.Custom,
        customAccentArgb = 0xFF2C241B.toInt(),
        customBackgroundArgb = 0xFFF4EFE6.toInt(),
        fontSource = FontSource.Serif,
        homeTextScale = 1.05f,
        homeTextWeight = TextWeightOption.Normal,
        homeLineSpacing = 1.15f,
        homeAlignment = HomeAlignment.Start,
    )

    fun terminal(): AppearanceSettings = AppearanceSettings(
        themeMode = ThemeMode.Black,
        colorMode = ColorMode.Custom,
        customAccentArgb = 0xFF33FF66.toInt(),
        customBackgroundArgb = 0xFF000000.toInt(),
        fontSource = FontSource.Mono,
        homeTextScale = 0.95f,
        homeTextWeight = TextWeightOption.Normal,
        homeAlignment = HomeAlignment.Start,
    )

    fun mono(): AppearanceSettings = AppearanceSettings(
        themeMode = ThemeMode.Black,
        colorMode = ColorMode.Custom,
        customAccentArgb = 0xFFFFFFFF.toInt(),
        customBackgroundArgb = 0xFF000000.toInt(),
        fontSource = FontSource.Mono,
        homeTextScale = 1.0f,
        homeTextWeight = TextWeightOption.Bold,
        homeAlignment = HomeAlignment.Start,
    )

    fun retroLcd(): AppearanceSettings = AppearanceSettings(
        themeMode = ThemeMode.Dark,
        colorMode = ColorMode.Custom,
        customAccentArgb = 0xFF9BBF6A.toInt(),
        customBackgroundArgb = 0xFF1A2214.toInt(),
        fontSource = FontSource.Mono,
        homeTextScale = 1.0f,
        homeTextWeight = TextWeightOption.Medium,
        homeAlignment = HomeAlignment.Center,
    )

    fun classicPhone(): AppearanceSettings = AppearanceSettings(
        themeMode = ThemeMode.Black,
        colorMode = ColorMode.Custom,
        customAccentArgb = 0xFFF0F0F0.toInt(),
        customBackgroundArgb = 0xFF000000.toInt(),
        fontSource = FontSource.Sans,
        homeTextScale = 1.1f,
        homeTextWeight = TextWeightOption.Bold,
        homeLineSpacing = 1.2f,
        homeAlignment = HomeAlignment.Center,
    )

    fun material(): AppearanceSettings = AppearanceSettings(
        themeMode = ThemeMode.System,
        colorMode = ColorMode.Dynamic,
        fontSource = FontSource.Sans,
        homeTextScale = 1.0f,
        homeTextWeight = TextWeightOption.Normal,
        homeAlignment = HomeAlignment.Start,
    )

    fun all(): List<Pair<String, AppearanceSettings>> = listOf(
        "Still" to still(),
        "OLED" to oled(),
        "Paper" to paper(),
        "Terminal" to terminal(),
        "Mono" to mono(),
        "Retro LCD" to retroLcd(),
        "Classic Phone" to classicPhone(),
        "Material" to material(),
    )
}
