package app.still.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import app.still.appearance.HomeAlignment
import app.still.appearance.ResolvedAppearance

val LocalHomeAlignment = staticCompositionLocalOf { HomeAlignment.Start }
val LocalUseSettingsTypography = staticCompositionLocalOf { false }

@Composable
fun StillTheme(
    resolved: ResolvedAppearance,
    forceSettingsTypography: Boolean = false,
    content: @Composable () -> Unit,
) {
    val typography = if (forceSettingsTypography) {
        resolved.settingsTypography
    } else {
        resolved.homeTypography
    }
    CompositionLocalProvider(
        LocalHomeAlignment provides resolved.homeAlignment,
        LocalUseSettingsTypography provides forceSettingsTypography,
    ) {
        MaterialTheme(
            colorScheme = resolved.colorScheme,
            typography = typography,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (resolved.showWallpaperScrim) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color.Black.copy(alpha = resolved.scrimStrength.coerceIn(0f, 0.85f)),
                            ),
                    )
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (resolved.showWallpaperScrim) {
                        Color.Transparent
                    } else {
                        MaterialTheme.colorScheme.background
                    },
                    content = content,
                )
            }
        }
    }
}
