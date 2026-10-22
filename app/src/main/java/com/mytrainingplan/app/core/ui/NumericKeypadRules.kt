package com.mytrainingplan.app.core.ui

object NumericKeypadRules {
    const val TECLA_BORRAR = "⌫"
    const val TECLA_SIGNO = "±"
    const val TECLA_COMA = ","
    const val TECLA_OK = "OK"

    fun applyKey(
        texto: String,
        tecla: String,
        admiteDecimal: Boolean,
        admiteSigno: Boolean
    ): String {
        return when (tecla) {
            TECLA_BORRAR -> borrar(texto, admiteDecimal, admiteSigno)
            TECLA_SIGNO -> if (admiteSigno) ciclarSigno(texto) else texto
            TECLA_COMA -> if (admiteDecimal) NumberInput.sanitizeDecimal(texto + ",") else texto
            TECLA_OK -> texto
            else -> if (tecla.length == 1 && tecla[0].isDigit()) digito(texto, tecla, admiteDecimal) else texto
        }
    }

    private fun digito(texto: String, d: String, admiteDecimal: Boolean): String =
        if (admiteDecimal) NumberInput.sanitizeDecimal(texto + d)
        else NumberInput.sanitizeInt(texto + d)

    private fun borrar(texto: String, admiteDecimal: Boolean, admiteSigno: Boolean): String {
        val sinUltimo = texto.dropLast(1)
        return if (sinUltimo.isEmpty()) ""
        else if (admiteDecimal) NumberInput.sanitizeDecimal(sinUltimo)
        else NumberInput.sanitizeInt(sinUltimo)
    }

    private fun ciclarSigno(texto: String): String = when {
        texto.startsWith("+") -> "-" + texto.substring(1)
        texto.startsWith("-") -> texto.substring(1)
        else -> "+" + texto
    }
}
