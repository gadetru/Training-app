package com.mytrainingplan.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mytrainingplan.app.ui.theme.AppColors
import kotlinx.coroutines.delay

/**
 * Caret parpadeante (spec 026): indicador visual de foco en la celda que se está
 * editando. Se dibuja tras el texto (el llamador lo coloca) y solo se monta cuando
 * la celda está enfocada, sin abrir el IME del sistema.
 */
@Composable
fun BlinkingCaret(
    modifier: Modifier = Modifier,
    widthDp: Dp = 2.dp,
    heightDp: Dp = 18.dp,
    color: Color = AppColors.Orange,
) {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            visible = !visible
            delay(500)
        }
    }
    if (visible) {
        Box(
            modifier = modifier
                .width(widthDp)
                .height(heightDp)
                .background(color)
        )
    }
}
