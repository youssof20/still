package app.still.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Shared spacing scale — prefer these over one-off dp literals. */
object StillSpacing {
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
    val xxl: Dp = 48.dp

    val homeHorizontal = lg
    val homeTop = md
    val favoriteRowVertical = sm
    val sectionGap = md
    val sheetPadding = md
}
