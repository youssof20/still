package app.still.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Restrained spacing scale. Prefer these over one-off literals. */
object StillSpacing {
    val s4: Dp = 4.dp
    val s8: Dp = 8.dp
    val s12: Dp = 12.dp
    val s16: Dp = 16.dp
    val s24: Dp = 24.dp
    val s32: Dp = 32.dp
    val s40: Dp = 40.dp
    val s48: Dp = 48.dp

    val homeHorizontal = s24
    val homeTop = s48
    val favoriteRowVertical = s8
    val sectionGap = s24
    val sheetPadding = s16
    val settingsHorizontal = s24
}

/** Home typography targets — hierarchy via size/weight/contrast, not containers. */
object StillType {
    val clock: TextUnit = 46.sp
    val date: TextUnit = 17.sp
    val favorite: TextUnit = 24.sp
    val task: TextUnit = 18.sp
    val hint: TextUnit = 15.sp
    val prefTitle: TextUnit = 16.sp
    val prefValue: TextUnit = 15.sp
    val section: TextUnit = 13.sp
    val drawerApp: TextUnit = 22.sp
}
