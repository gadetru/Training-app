package com.mytrainingplan.app.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regla única de limpieza numérica (spec 020): sin ceros a la izquierda,
 * aplicada mientras se teclea.
 */
class NumberInputTest {

    @Test
    fun sanitizeInt_quitaCerosIniciales() {
        assertEquals("12", NumberInput.sanitizeInt("012"))
        assertEquals("12", NumberInput.sanitizeInt("00012"))
    }

    @Test
    fun sanitizeInt_ceroSueltoSeMantiene() {
        assertEquals("0", NumberInput.sanitizeInt("0"))
        assertEquals("0", NumberInput.sanitizeInt("00"))
        assertEquals("0", NumberInput.sanitizeInt("000"))
    }

    @Test
    fun sanitizeInt_vacioSeConservaVacio() {
        assertEquals("", NumberInput.sanitizeInt(""))
        assertEquals("", NumberInput.sanitizeInt("   "))
    }

    @Test
    fun sanitizeInt_soloDigitos() {
        assertEquals("123", NumberInput.sanitizeInt("1a2b3c"))
    }

    @Test
    fun sanitizeDecimal_cerosIniciales() {
        assertEquals("12", NumberInput.sanitizeDecimal("012"))
        assertEquals("0", NumberInput.sanitizeDecimal("00"))
    }

    @Test
    fun sanitizeDecimal_comaSeNormalizaApunto() {
        assertEquals("0.5", NumberInput.sanitizeDecimal("0,5"))
    }

    @Test
    fun sanitizeDecimal_puntoFinalMientrasSeTeclea() {
        assertEquals("0.", NumberInput.sanitizeDecimal("0."))
        assertEquals("12.", NumberInput.sanitizeDecimal("12."))
    }

    @Test
    fun sanitizeDecimal_puntoSuelto() {
        assertEquals(".", NumberInput.sanitizeDecimal("."))
    }

    @Test
    fun sanitizeDecimal_signoNegativoSePreserva() {
        assertEquals("-10", NumberInput.sanitizeDecimal("-10"))
        assertEquals("-0.5", NumberInput.sanitizeDecimal("-0,5"))
    }

    @Test
    fun sanitizeDecimal_signoPositivoSePreserva() {
        assertEquals("+10", NumberInput.sanitizeDecimal("+10"))
    }

    @Test
    fun sanitizeDecimal_signoSoloSePermite() {
        assertEquals("-", NumberInput.sanitizeDecimal("-"))
    }

    @Test
    fun sanitizeDecimal_unSoloPunto() {
        assertEquals("1.25", NumberInput.sanitizeDecimal("1.2.5"))
    }

    @Test
    fun sanitizeDecimal_vacioSeConservaVacio() {
        assertEquals("", NumberInput.sanitizeDecimal(""))
    }
}
