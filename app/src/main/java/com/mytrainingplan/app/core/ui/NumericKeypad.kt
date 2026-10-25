package com.mytrainingplan.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import com.mytrainingplan.app.ui.theme.AppColors
import com.mytrainingplan.app.ui.theme.AppDimens
import com.mytrainingplan.app.ui.theme.AppTextSizes

/**
 * Panel numérico propio tipo calculadora (spec 026): teclas 0-9, coma, borrar y OK.
 * Sin letras del IME del sistema. Puro Compose, sin Compose en las reglas (esas viven en
 * [NumericKeypadRules]). Se ancla abajo con [navigationBarsPadding] y aplica [imePadding]
 * por si quedara IME residual. Testeable vía [testTag] en cada tecla.
 *
 * El panel se muestra dentro de una tarjeta (Caja) como la vista del ejercicio; no hay
 * visor propio del valor (ese vive en la celda que se está editando).
 *
 * @param valor texto actual del borrador.
 * @param admiteDecimal si se permite separador decimal (solo kg).
 * @param onValueChange se notifica tras cada tecla (dígito, coma, borrar).
 * @param onConfirm se notifica al pulsar OK (el llamador cierra el panel).
 * @param aplicaNavigationBars true fuera de diálogo (pantalla); false dentro del AlertDialog
 *                      que ya gestiona insets propios.
 * @param modifier Modificador base del panel (fillMaxWidth, etc.).
 */
@Composable
fun NumericKeypad(
    valor: String,
    admiteDecimal: Boolean = false,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    aplicaNavigationBars: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val navegacion = if (aplicaNavigationBars) Modifier.navigationBarsPadding() else Modifier
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(navegacion)
            .imePadding()
            .padding(horizontal = AppDimens.ScreenHorizontal, vertical = AppDimens.SpaceMd)
            .clip(RoundedCornerShape(AppDimens.RadiusCard))
            .background(AppColors.Card)
            .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusCard))
            .padding(AppDimens.SpaceXl),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMd),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val filas = listOf(
            listOf("7", "8", "9"),
            listOf("4", "5", "6"),
            listOf("1", "2", "3"),
            listOf("0", NumericKeypadRules.TECLA_COMA, NumericKeypadRules.TECLA_BORRAR)
        )

        filas.forEach { fila ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                fila.forEach { tecla ->
                    KeypadKey(
                        tecla = tecla,
                        admiteDecimal = admiteDecimal,
                        onPress = { teclaPulsada ->
                            val nuevo = NumericKeypadRules.applyKey(
                                texto = valor,
                                tecla = teclaPulsada,
                                admiteDecimal = admiteDecimal
                            )
                            onValueChange(nuevo)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(AppDimens.SpaceSm))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.ConfirmHeight)
                .clip(RoundedCornerShape(AppDimens.RadiusMd))
                .background(AppColors.Orange)
                .clickable(onClick = onConfirm)
                .testTag("keypadOk"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "OK",
                color = AppColors.OnOrange,
                fontSize = AppTextSizes.Headline,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
    }
}

@Composable
private fun KeypadKey(
    tecla: String,
    admiteDecimal: Boolean,
    onPress: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val habilitada = tecla != NumericKeypadRules.TECLA_COMA || admiteDecimal

    val tag = when (tecla) {
        NumericKeypadRules.TECLA_COMA -> "keypadComa"
        NumericKeypadRules.TECLA_BORRAR -> "keypadBorrar"
        NumericKeypadRules.TECLA_OK -> "keypadOk"
        else -> "keypad$tecla"
    }

    Box(
        modifier = modifier
            .height(AppDimens.TouchMin)
            .clip(RoundedCornerShape(AppDimens.RadiusMd))
            .background(
                color = if (habilitada) AppColors.CardHigh else AppColors.InputBg,
                shape = RoundedCornerShape(AppDimens.RadiusMd)
            )
            .clickable(enabled = habilitada, onClick = { onPress(tecla) })
            .padding(AppDimens.SpaceXs)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = tecla,
            style = TextStyle(
                fontSize = AppTextSizes.Headline,
                color = if (habilitada) AppColors.TextPrimary else AppColors.TextMuted
            )
        )
    }
}
