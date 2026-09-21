package app.still.appearance

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Typeface as ComposeTypeface
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

data class ResolvedAppearance(
    val colorScheme: ColorScheme,
    val homeTypography: Typography,
    val settingsTypography: Typography,
    val contrastWarning: Boolean,
    val fontLoadFailed: Boolean,
    val homeAlignment: HomeAlignment,
    val showWallpaperScrim: Boolean,
    val scrimStrength: Float,
)

object AppearanceResolver {
    fun resolve(
        context: Context,
        settings: AppearanceSettings,
        systemDark: Boolean,
        fontImporter: FontImporter,
    ): ResolvedAppearance {
        val dark = when (settings.themeMode) {
            ThemeMode.Light -> false
            ThemeMode.Dark, ThemeMode.Black -> true
            ThemeMode.System -> systemDark
        }
        val black = settings.themeMode == ThemeMode.Black

        var fontFailed = false
        val homeFamily = resolveFontFamily(settings, fontImporter) {
            fontFailed = true
        }
        val settingsTypography = Typography()
        val homeTypography = scaleTypography(
            baseFamily = homeFamily,
            scale = settings.homeTextScale,
            weight = settings.homeTextWeight,
            lineSpacing = settings.homeLineSpacing,
        )

        val scheme = when (settings.colorMode) {
            ColorMode.Dynamic -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                } else {
                    neutralScheme(dark, black)
                }
            }
            ColorMode.Neutral -> neutralScheme(dark, black)
            ColorMode.Custom -> customScheme(
                dark = dark,
                black = black,
                accent = Color(settings.customAccentArgb),
                background = Color(settings.customBackgroundArgb),
            )
        }

        val warning = settings.colorMode == ColorMode.Custom &&
            !hasReadableContrast(
                Color(settings.customAccentArgb),
                Color(settings.customBackgroundArgb),
            )

        return ResolvedAppearance(
            colorScheme = scheme,
            homeTypography = homeTypography,
            settingsTypography = settingsTypography,
            contrastWarning = warning,
            fontLoadFailed = fontFailed,
            homeAlignment = settings.homeAlignment,
            showWallpaperScrim = settings.showWallpaperScrim,
            scrimStrength = settings.scrimStrength,
        )
    }

    private fun resolveFontFamily(
        settings: AppearanceSettings,
        fontImporter: FontImporter,
        onFail: () -> Unit,
    ): FontFamily {
        return when (settings.fontSource) {
            FontSource.System -> FontFamily.Default
            FontSource.Sans -> FontFamily.SansSerif
            FontSource.Serif -> FontFamily.Serif
            FontSource.Mono -> FontFamily.Monospace
            FontSource.Imported -> {
                val id = settings.importedFontId
                val typeface = id?.let { fontImporter.loadTypeface(it) }
                if (typeface == null) {
                    onFail()
                    FontFamily.Default
                } else {
                    FontFamily(ComposeTypeface(typeface))
                }
            }
        }
    }

    private fun scaleTypography(
        baseFamily: FontFamily,
        scale: Float,
        weight: TextWeightOption,
        lineSpacing: Float,
    ): Typography {
        val base = Typography()
        val w = when (weight) {
            TextWeightOption.Normal -> FontWeight.Normal
            TextWeightOption.Medium -> FontWeight.Medium
            TextWeightOption.Bold -> FontWeight.Bold
        }
        fun TextStyle.scaled(): TextStyle = copy(
            fontFamily = baseFamily,
            fontWeight = w,
            fontSize = (fontSize.value * scale).sp,
            lineHeight = (lineHeight.value * scale * lineSpacing).sp,
        )
        return Typography(
            displayLarge = base.displayLarge.scaled(),
            displayMedium = base.displayMedium.scaled(),
            displaySmall = base.displaySmall.scaled(),
            headlineLarge = base.headlineLarge.scaled(),
            headlineMedium = base.headlineMedium.scaled(),
            headlineSmall = base.headlineSmall.scaled(),
            titleLarge = base.titleLarge.scaled(),
            titleMedium = base.titleMedium.scaled(),
            titleSmall = base.titleSmall.scaled(),
            bodyLarge = base.bodyLarge.scaled(),
            bodyMedium = base.bodyMedium.scaled(),
            bodySmall = base.bodySmall.scaled(),
            labelLarge = base.labelLarge.scaled(),
            labelMedium = base.labelMedium.scaled(),
            labelSmall = base.labelSmall.scaled(),
        )
    }

    private fun neutralScheme(dark: Boolean, black: Boolean): ColorScheme {
        return if (!dark) {
            lightColorScheme(
                primary = Color(0xFF1B1B1B),
                onPrimary = Color.White,
                secondary = Color(0xFF4A4A4A),
                background = Color(0xFFFAFAFA),
                onBackground = Color(0xFF1B1B1B),
                surface = Color(0xFFFAFAFA),
                onSurface = Color(0xFF1B1B1B),
            )
        } else if (black) {
            darkColorScheme(
                primary = Color(0xFFE8E8E8),
                onPrimary = Color.Black,
                secondary = Color(0xFFB0B0B0),
                background = Color.Black,
                onBackground = Color(0xFFE8E8E8),
                surface = Color.Black,
                onSurface = Color(0xFFE8E8E8),
            )
        } else {
            darkColorScheme(
                primary = Color(0xFFE0E0E0),
                onPrimary = Color(0xFF121212),
                secondary = Color(0xFFB0B0B0),
                background = Color(0xFF121212),
                onBackground = Color(0xFFE0E0E0),
                surface = Color(0xFF121212),
                onSurface = Color(0xFFE0E0E0),
            )
        }
    }

    private fun customScheme(
        dark: Boolean,
        black: Boolean,
        accent: Color,
        background: Color,
    ): ColorScheme {
        val bg = if (black) Color.Black else background
        val onBg = if (bg.luminance() > 0.5f) Color(0xFF1B1B1B) else Color(0xFFF5F5F5)
        val onAccent = if (accent.luminance() > 0.5f) Color.Black else Color.White
        return if (!dark && !black) {
            lightColorScheme(
                primary = accent,
                onPrimary = onAccent,
                secondary = accent.copy(alpha = 0.8f),
                background = bg,
                onBackground = onBg,
                surface = bg,
                onSurface = onBg,
            )
        } else {
            darkColorScheme(
                primary = accent,
                onPrimary = onAccent,
                secondary = accent.copy(alpha = 0.8f),
                background = bg,
                onBackground = onBg,
                surface = bg,
                onSurface = onBg,
            )
        }
    }

    fun hasReadableContrast(foreground: Color, background: Color): Boolean {
        val l1 = foreground.luminance() + 0.05f
        val l2 = background.luminance() + 0.05f
        val ratio = max(l1, l2) / min(l1, l2)
        return ratio >= 3.0f
    }
}

@Composable
fun rememberResolvedAppearance(
    settings: AppearanceSettings,
    fontImporter: FontImporter,
): ResolvedAppearance {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    return remember(settings, systemDark) {
        AppearanceResolver.resolve(context, settings, systemDark, fontImporter)
    }
}
