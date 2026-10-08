package com.mytrainingplan.app.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Blindaje de medidas (spec 011, paso 7): los dp/sp actuales son la verdad.
 * Cambiar un tamaño a mano hace fallar su prueba.
 */
class AppDimensTest {

    @Test
    fun margenes_de_pantalla_conservan_sus_dp() {
        assertEquals(20.dp, AppDimens.ScreenHorizontal)
        assertEquals(4.dp, AppDimens.ContentTop)
        assertEquals(96.dp, AppDimens.HomeBottom)
        assertEquals(110.dp, AppDimens.DetailBottom)
        assertEquals(150.dp, AppDimens.EditBottom)
        assertEquals(170.dp, AppDimens.WorkoutBottom)
        assertEquals(16.dp, AppDimens.DockBottom)
        assertEquals(32.dp, AppDimens.RestHorizontal)
        assertEquals(24.dp, AppDimens.DialogPadding)
    }

    @Test
    fun espaciados_conservan_sus_dp() {
        assertEquals(2.dp, AppDimens.SpaceXxs)
        assertEquals(3.dp, AppDimens.SpaceTiny)
        assertEquals(4.dp, AppDimens.SpaceXs)
        assertEquals(6.dp, AppDimens.SpaceSm)
        assertEquals(8.dp, AppDimens.SpaceMd)
        assertEquals(10.dp, AppDimens.SpaceLg)
        assertEquals(12.dp, AppDimens.SpaceXl)
        assertEquals(14.dp, AppDimens.SpaceXxl)
        assertEquals(16.dp, AppDimens.SpaceHuge)
    }

    @Test
    fun radios_conservan_sus_dp() {
        assertEquals(4.dp, AppDimens.RadiusXs)
        assertEquals(6.dp, AppDimens.RadiusSm)
        assertEquals(8.dp, AppDimens.RadiusMd)
        assertEquals(10.dp, AppDimens.RadiusLg)
        assertEquals(12.dp, AppDimens.RadiusXl)
        assertEquals(16.dp, AppDimens.RadiusCard)
        assertEquals(24.dp, AppDimens.RadiusDock)
        assertEquals(999.dp, AppDimens.RadiusPill)
    }

    @Test
    fun tactiles_y_piezas_conservan_sus_dp() {
        assertEquals(48.dp, AppDimens.TouchMin)
        assertEquals(44.dp, AppDimens.Avatar)
        assertEquals(56.dp, AppDimens.Thumb)
        assertEquals(4.dp, AppDimens.AccentBarWidth)
        assertEquals(88.dp, AppDimens.AccentBarHeight)
        assertEquals(240.dp, AppDimens.DetailHeroHeight)
        assertEquals(52.dp, AppDimens.ConfirmHeight)
        assertEquals(28.dp, AppDimens.ChipMinHeight)
        assertEquals(10.dp, AppDimens.TitleDot)
        assertEquals(6.dp, AppDimens.ProgressBarThin)
        assertEquals(8.dp, AppDimens.ProgressBarThick)
        assertEquals(1.dp, AppDimens.Divider)
        assertEquals(1.dp, AppDimens.BorderThin)
        assertEquals(2.dp, AppDimens.BorderThick)
        assertEquals(64.dp, AppDimens.DurationWidth)
        assertEquals(28.dp, AppDimens.SetBadge)
        assertEquals(24.dp, AppDimens.SetNumber)
        assertEquals(36.dp, AppDimens.SetCell)
        assertEquals(54.dp, AppDimens.RirWidth)
        assertEquals(44.dp, AppDimens.HeaderMetaWidth)
        assertEquals(44.dp, AppDimens.PauseWidth)
        assertEquals(6.dp, AppDimens.LabelDot)
        assertEquals(8.dp, AppDimens.StatusDot)
        assertEquals(12.dp, AppDimens.PresenceDot)
        assertEquals(20.dp, AppDimens.DayDot)
        assertEquals(14.dp, AppDimens.IconSm)
        assertEquals(16.dp, AppDimens.IconMd)
        assertEquals(22.dp, AppDimens.IconLg)
        assertEquals(20.dp, AppDimens.IconXl)
        assertEquals(18.dp, AppDimens.IconInline)
        assertEquals(32.dp, AppDimens.SetNumWidth)
        assertEquals(40.dp, AppDimens.PlaceholderIcon)
    }

    @Test
    fun textos_conservan_sus_sp() {
        assertEquals(8.sp, AppTextSizes.Badge)
        assertEquals(9.sp, AppTextSizes.Tiny)
        assertEquals(10.sp, AppTextSizes.Caption)
        assertEquals(11.sp, AppTextSizes.Small)
        assertEquals(12.sp, AppTextSizes.Body)
        assertEquals(13.sp, AppTextSizes.BodyLg)
        assertEquals(14.sp, AppTextSizes.TitleSm)
        assertEquals(15.sp, AppTextSizes.Title)
        assertEquals(16.sp, AppTextSizes.TitleLg)
        assertEquals(18.sp, AppTextSizes.Headline)
        assertEquals(20.sp, AppTextSizes.DisplaySm)
        assertEquals(22.sp, AppTextSizes.Display)
        assertEquals(24.sp, AppTextSizes.DisplayLg)
        assertEquals(56.sp, AppTextSizes.RestClock)
    }

    @Test
    fun puerta_appTheme_expone_las_mismas_medidas() {
        assertSame(AppDimens, AppTheme.dimens)
        assertSame(AppTextSizes, AppTheme.textSizes)
    }
}
