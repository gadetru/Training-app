package com.mytrainingplan.app.feature.routines

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.mytrainingplan.app.core.ui.NumericKeypad
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Smoke del panel numérico propio (spec 026): abrir panel, teclear, borrar
 * y OK. Ejercita el componente compartido usado en editar rutina y en el
 * modal de serie de la sesión.
 */
class NumericKeypadSmokeTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun panel_teclear_borrar_y_ok() {
        var valor = ""
        rule.setContent {
            MaterialTheme {
                NumericKeypad(
                    valor = valor,
                    admiteDecimal = true,
                    onValueChange = { valor = it },
                    onConfirm = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Teclas esperadas presentes.
        rule.onNodeWithTag("keypad7").assertIsDisplayed()
        rule.onNodeWithTag("keypadComa").assertIsDisplayed()
        rule.onNodeWithTag("keypadBorrar").assertIsDisplayed()
        rule.onNodeWithTag("keypadOk").assertIsDisplayed()

        // Teclear 1 y 2 -> 12.
        rule.onNodeWithTag("keypad1").performClick()
        rule.onNodeWithTag("keypad2").performClick()
        assertEquals("12", valor)

        // Borrar elimina el último carácter sin cerrar el panel.
        rule.onNodeWithTag("keypadBorrar").performClick()
        assertEquals("1", valor)

        // OK confirma.
        rule.onNodeWithTag("keypadOk").performClick()
        assertEquals("1", valor)
    }

    @Test
    fun panel_coma_en_kg() {
        var valor = ""
        rule.setContent {
            MaterialTheme {
                NumericKeypad(
                    valor = valor,
                    admiteDecimal = true,
                    onValueChange = { valor = it },
                    onConfirm = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        rule.onNodeWithTag("keypad0").performClick()
        rule.onNodeWithTag("keypadComa").performClick()
        rule.onNodeWithTag("keypad5").performClick()
        assertEquals("0.5", valor)
    }

    @Test
    fun panel_coma_bloqueada_en_reps() {
        var valor = ""
        rule.setContent {
            MaterialTheme {
                NumericKeypad(
                    valor = valor,
                    admiteDecimal = false,
                    onValueChange = { valor = it },
                    onConfirm = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        rule.onNodeWithTag("keypad1").performClick()
        rule.onNodeWithTag("keypadComa").performClick() // no hace nada
        assertEquals("1", valor)
    }
}
