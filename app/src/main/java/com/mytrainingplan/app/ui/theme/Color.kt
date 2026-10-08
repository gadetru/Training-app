package com.mytrainingplan.app.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

/**
 * Botes con nombre (spec 011, paso 1): los hex actuales son la verdad.
 * Las pantallas `home, routines, workout, exercises` migran a estos botes
 * (pasos 3-6); nada crea `Color(0xFF...)` suelto en esas features.
 */
object AppColors {
    val Bg = Color(0xFF111316)
    val Card = Color(0xFF1A1C1F)
    val SheetBg = Color(0xFF1A1C1F)
    val Card2 = Color(0xFF1E2023)
    val CardHigh = Color(0xFF282A2D)
    val Orange = Color(0xFFFF5E00)
    val Volt = Color(0xFFCCFF00)
    val Cyan = Color(0xFF00E5FF)
    val TextPrimary = Color(0xFFF5F7FA)
    val TextMuted = Color(0xFF8B95A5)
    val BorderSubtle = Color(0xFF282E37)
    val InputBg = Color(0xFF0C0E11)
    val OnOrange = Color(0xFF1A0A00)
    val OnVolt = Color(0xFF1A2A00)
    val ErrorRed = Color(0xFFFF8A80)
    val DockBg = Color(0xE61E2023)
    val BadgeBg = Color(0xCC0C0E11)
    val Scrim = Color(0xBF000000)
}