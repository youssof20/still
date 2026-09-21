package app.still.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StillLightColors = lightColorScheme(
    primary = Color(0xFF1B1B1B),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF4A4A4A),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1B1B1B),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF1B1B1B),
)

@Composable
fun StillTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StillLightColors,
        content = content,
    )
}
