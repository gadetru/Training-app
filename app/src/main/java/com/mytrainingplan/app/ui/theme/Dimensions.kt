package com.mytrainingplan.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Botes de medidas con nombre (spec 011, paso 1): los dp/sp actuales son
 * la verdad. Las pantallas `home, routines, workout, exercises` migran a
 * estos botes (pasos 3-6); nada usa `N.dp` / `N.sp` suelto en esas features.
 */
object AppDimens {
    // Márgenes de pantalla.
    val ScreenHorizontal: Dp = 20.dp
    val ContentTop: Dp = 4.dp
    val HomeBottom: Dp = 96.dp
    val DetailBottom: Dp = 110.dp
    val EditBottom: Dp = 150.dp
    val WorkoutBottom: Dp = 170.dp
    val DockBottom: Dp = 16.dp
    val RestHorizontal: Dp = 32.dp

    // Espaciados.
    val SpaceXxs: Dp = 2.dp
    val SpaceXs: Dp = 4.dp
    val SpaceSm: Dp = 6.dp
    val SpaceMd: Dp = 8.dp
    val SpaceLg: Dp = 10.dp
    val SpaceXl: Dp = 12.dp
    val SpaceXxl: Dp = 14.dp
    val SpaceHuge: Dp = 16.dp
    val DialogPadding: Dp = 24.dp

    // Radios.
    val RadiusXs: Dp = 4.dp
    val RadiusSm: Dp = 6.dp
    val RadiusMd: Dp = 8.dp
    val RadiusLg: Dp = 10.dp
    val RadiusXl: Dp = 12.dp
    val RadiusCard: Dp = 16.dp
    val RadiusDock: Dp = 24.dp
    val RadiusPill: Dp = 999.dp

    // Táctiles y piezas.
    val TouchMin: Dp = 48.dp
    val Avatar: Dp = 44.dp
    val Thumb: Dp = 56.dp
    val AccentBarWidth: Dp = 4.dp
    val AccentBarHeight: Dp = 88.dp
    val DetailHeroHeight: Dp = 240.dp
    val ConfirmHeight: Dp = 52.dp
    val ProgressBarThin: Dp = 6.dp
    val ProgressBarThick: Dp = 8.dp
    val Divider: Dp = 1.dp
    val BorderThin: Dp = 1.dp
    val BorderThick: Dp = 2.dp
    val DurationWidth: Dp = 64.dp
    val SetBadge: Dp = 28.dp
    val SetNumber: Dp = 24.dp
    val SetCell: Dp = 36.dp
    val RirWidth: Dp = 54.dp
    val HeaderMetaWidth: Dp = 44.dp
    val PauseWidth: Dp = 44.dp
    val LabelDot: Dp = 6.dp
    val StatusDot: Dp = 8.dp
    val PresenceDot: Dp = 12.dp
    val DayDot: Dp = 20.dp
    val IconSm: Dp = 14.dp
    val IconMd: Dp = 16.dp
    val IconLg: Dp = 22.dp
    val TabHeight: Dp = 56.dp
    val PlaceholderIcon: Dp = 40.dp
}

/**
 * Tamaños de texto con nombre (spec 011, paso 1): los sp actuales son
 * la verdad. Misma regla que [AppDimens]: nada usa `N.sp` suelto en las
 * cuatro features.
 */
object AppTextSizes {
    val Badge: TextUnit = 8.sp
    val Tiny: TextUnit = 9.sp
    val Caption: TextUnit = 10.sp
    val Small: TextUnit = 11.sp
    val Body: TextUnit = 12.sp
    val BodyLg: TextUnit = 13.sp
    val TitleSm: TextUnit = 14.sp
    val Title: TextUnit = 15.sp
    val TitleLg: TextUnit = 16.sp
    val Headline: TextUnit = 18.sp
    val DisplaySm: TextUnit = 20.sp
    val Display: TextUnit = 22.sp
    val DisplayLg: TextUnit = 24.sp
    val RestClock: TextUnit = 56.sp
}
