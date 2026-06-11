package com.initcn.powertools.core.theme

import androidx.compose.ui.unit.dp

/**
 * Centralized dimension system for PowerTools.
 * * Strictly use these predefined values instead of hardcoding raw .dp
 * values inside Composables to ensure perfect visual consistency.
 */
object Dimens {

    // --- Spacing ---
    val XXS = 2.dp
    val XS = 4.dp
    val SM = 8.dp
    val MD = 16.dp
    val LG = 24.dp
    val XL = 32.dp
    val XXL = 48.dp
    val XXXL = 64.dp

    // --- Corner Radius ---
    val RadiusSM = 8.dp
    val RadiusMD = 12.dp
    val RadiusLG = 16.dp
    val RadiusXL = 24.dp

    // --- Elevation ---
    val ElevationNone = 0.dp
    val ElevationSM = 1.dp
    val ElevationMD = 3.dp
    val ElevationLG = 6.dp

    // --- Icon Sizes ---
    val IconSM = 18.dp
    val IconMD = 24.dp
    val IconLG = 32.dp
    val IconXL = 48.dp

    // --- Component Sizing ---
    val ButtonHeight = 52.dp
    val CardMinHeight = 88.dp
    val TopBarHeight = 64.dp

    // --- Layout Specific ---
    val ScreenPadding = MD

    /** * Applies safe bottom padding to LazyColumns so the
     * Floating Action Button (FAB) doesn't overlap the last item.
     */
    val ListBottomFabClearance = 80.dp
}