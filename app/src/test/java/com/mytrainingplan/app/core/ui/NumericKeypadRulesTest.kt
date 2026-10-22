package com.mytrainingplan.app.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Reglas puras del teclado numérico propio (spec 026): dígitos, coma, ±, borrar y OK.
 * Delegación en NumberInput (spec 020); testeable en JVM sin Compose.
 */
class NumericKeypadRulesTest {

    @Test
    fun digitos_entero_soloDigitos() {
        assertEquals("123", NumericKeypadRules.applyKey("12", "3", false, false))
        assertEquals("123", NumericKeypadRules.applyKey("012", "3", false, false))
    }

    @Test
    fun digitos_entero_cerosInicialesSeLimpian() {
        assertEquals("1", NumericKeypadRules.applyKey("0", "1", false, false)) // 0→1
        assertEquals("12", NumericKeypadRules.applyKey("1", "2", false, false)) // 1→12
        assertEquals("2", NumericKeypadRules.applyKey("00", "2", false, false)) // 00→2
    }

    @Test
    fun digitos_decimal_admitePunto() {
        assertEquals("125", NumericKeypadRules.applyKey("12", "5", true, false))
        assertEquals("5", NumericKeypadRules.applyKey("0", "5", true, false)) // 05→5
    }

    @Test
    fun coma_construyeDecimalPasoAPaso() {
        assertEquals("12.", NumericKeypadRules.applyKey("12", NumericKeypadRules.TECLA_COMA, true, false))
        assertEquals("12.5", NumericKeypadRules.applyKey("12,", "5", true, false))
        assertEquals("0.", NumericKeypadRules.applyKey("0", NumericKeypadRules.TECLA_COMA, true, false))
        assertEquals("0.5", NumericKeypadRules.applyKey("0,", "5", true, false))
    }

    @Test
    fun coma_soloSiAdmiteDecimal() {
        assertEquals(".", NumericKeypadRules.applyKey("", ",", true, false))
        assertEquals("12.", NumericKeypadRules.applyKey("12,", ",", true, false)) // segundo punto descartado
        assertEquals("12", NumericKeypadRules.applyKey("12", ",", false, false)) // reps: coma bloqueada
        assertEquals("12", NumericKeypadRules.applyKey("12", ",", false, false))
    }

    @Test
    fun signo_ciclaSinSignoMasMenosSinSigno() {
        assertEquals("+", NumericKeypadRules.applyKey("", NumericKeypadRules.TECLA_SIGNO, false, true))
        assertEquals("-", NumericKeypadRules.applyKey("+", NumericKeypadRules.TECLA_SIGNO, false, true))
        assertEquals("", NumericKeypadRules.applyKey("-", NumericKeypadRules.TECLA_SIGNO, false, true))
    }

    @Test
    fun signo_sobreNumeroConDecimal() {
        assertEquals("+10.5", NumericKeypadRules.applyKey("10.5", NumericKeypadRules.TECLA_SIGNO, true, true))
        assertEquals("-10.5", NumericKeypadRules.applyKey("+10.5", NumericKeypadRules.TECLA_SIGNO, true, true))
        assertEquals("10.5", NumericKeypadRules.applyKey("-10.5", NumericKeypadRules.TECLA_SIGNO, true, true))
    }

    @Test
    fun signo_soloKg_noAplicaEnReps() {
        assertEquals("10", NumericKeypadRules.applyKey("10", NumericKeypadRules.TECLA_SIGNO, false, false))
    }

    @Test
    fun borrar_eliminaUltimoCaracterSinCerrar() {
        assertEquals("12", NumericKeypadRules.applyKey("123", NumericKeypadRules.TECLA_BORRAR, false, false))
        assertEquals("1", NumericKeypadRules.applyKey("12", NumericKeypadRules.TECLA_BORRAR, false, false))
        assertEquals("", NumericKeypadRules.applyKey("1", NumericKeypadRules.TECLA_BORRAR, false, false))
        assertEquals("", NumericKeypadRules.applyKey("", NumericKeypadRules.TECLA_BORRAR, false, false))
    }

    @Test
    fun borrar_decimalRespetaPunto() {
        assertEquals("12.", NumericKeypadRules.applyKey("12.5", NumericKeypadRules.TECLA_BORRAR, true, false))
        assertEquals("0.", NumericKeypadRules.applyKey("0.5", NumericKeypadRules.TECLA_BORRAR, true, false))
    }

    @Test
    fun ok_devuelveTextoIgual() {
        assertEquals("12.5", NumericKeypadRules.applyKey("12.5", NumericKeypadRules.TECLA_OK, true, true))
        assertEquals("", NumericKeypadRules.applyKey("", NumericKeypadRules.TECLA_OK, false, false))
    }

    @Test
    fun teclaDesconocida_noModifica() {
        assertEquals("12", NumericKeypadRules.applyKey("12", "x", false, false))
    }
}
