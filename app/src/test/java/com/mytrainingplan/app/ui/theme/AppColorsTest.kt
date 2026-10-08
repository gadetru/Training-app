package com.mytrainingplan.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Blindaje de colores (spec 011, paso 7): los hex actuales son la verdad.
 * Cambiar un color a mano hace fallar su prueba.
 */
class AppColorsTest {

    @Test
    fun base_conserva_sus_hex() {
        assertEquals(Color(0xFF111316), AppColors.Bg)
        assertEquals(Color(0xFF1A1C1F), AppColors.Card)
        assertEquals(Color(0xFF1A1C1F), AppColors.SheetBg)
        assertEquals(Color(0xFF1E2023), AppColors.Card2)
        assertEquals(Color(0xFF282A2D), AppColors.CardHigh)
    }

    @Test
    fun acentos_conservan_sus_hex() {
        assertEquals(Color(0xFFFF5E00), AppColors.Orange)
        assertEquals(Color(0xFFCCFF00), AppColors.Volt)
        assertEquals(Color(0xFF00E5FF), AppColors.Cyan)
        assertEquals(Color(0xFF1A0A00), AppColors.OnOrange)
        assertEquals(Color(0xFF1A2A00), AppColors.OnVolt)
        assertEquals(Color(0xFFFF8A80), AppColors.ErrorRed)
    }

    @Test
    fun texto_bordes_y_superficies_conservan_sus_hex() {
        assertEquals(Color(0xFFF5F7FA), AppColors.TextPrimary)
        assertEquals(Color(0xFF8B95A5), AppColors.TextMuted)
        assertEquals(Color(0xFF282E37), AppColors.BorderSubtle)
        assertEquals(Color(0xFF0C0E11), AppColors.InputBg)
        assertEquals(Color(0xE61E2023), AppColors.DockBg)
        assertEquals(Color(0xCC0C0E11), AppColors.BadgeBg)
        assertEquals(Color(0xBF000000), AppColors.Scrim)
    }

    @Test
    fun puerta_appTheme_expone_los_mismos_botes() {
        assertSame(AppColors, AppTheme.colors)
    }
}
