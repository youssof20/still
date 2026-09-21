package app.still.appearance

import org.json.JSONObject

/**
 * Appearance-only theme preset. Does not include tasks, favorites, aliases,
 * imported font binary files, or private-profile data.
 */
object ThemePresetCodec {
    const val FORMAT = "still.theme"
    const val VERSION = 1

    fun toJson(settings: AppearanceSettings): String {
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("version", VERSION)
        root.put("themeMode", settings.themeMode.name)
        root.put("colorMode", settings.colorMode.name)
        root.put("customAccentArgb", settings.customAccentArgb)
        root.put("customBackgroundArgb", settings.customBackgroundArgb)
        // Imported fonts are device-local files and are not redistributed in presets.
        root.put(
            "fontSource",
            if (settings.fontSource == FontSource.Imported) FontSource.System.name else settings.fontSource.name,
        )
        root.put("homeTextScale", settings.homeTextScale.toDouble())
        root.put("homeTextWeight", settings.homeTextWeight.name)
        root.put("homeLineSpacing", settings.homeLineSpacing.toDouble())
        root.put("homeAlignment", settings.homeAlignment.name)
        root.put("showWallpaperScrim", settings.showWallpaperScrim)
        root.put("scrimStrength", settings.scrimStrength.toDouble())
        return root.toString()
    }

    fun parse(raw: String): Result<AppearanceSettings> = runCatching {
        val root = JSONObject(raw)
        require(root.optString("format") == FORMAT) { "Unsupported theme format" }
        require(root.getInt("version") == VERSION) { "Unsupported theme version" }
        val scale = root.optDouble("homeTextScale", 1.0).toFloat()
            .coerceIn(AppearanceSettings.MIN_SCALE, AppearanceSettings.MAX_SCALE)
        val spacing = root.optDouble("homeLineSpacing", 1.0).toFloat().coerceIn(0.9f, 1.5f)
        val scrim = root.optDouble("scrimStrength", 0.35).toFloat().coerceIn(0f, 0.85f)
        AppearanceSettings(
            themeMode = enumValueOf(root.getString("themeMode")),
            colorMode = enumValueOf(root.getString("colorMode")),
            customAccentArgb = root.getInt("customAccentArgb"),
            customBackgroundArgb = root.getInt("customBackgroundArgb"),
            fontSource = enumValueOf(root.optString("fontSource", FontSource.System.name)),
            importedFontId = null,
            homeTextScale = scale,
            homeTextWeight = enumValueOf(root.optString("homeTextWeight", TextWeightOption.Normal.name)),
            homeLineSpacing = spacing,
            homeAlignment = enumValueOf(root.optString("homeAlignment", HomeAlignment.Start.name)),
            showWallpaperScrim = root.optBoolean("showWallpaperScrim", false),
            scrimStrength = scrim,
        )
    }
}
