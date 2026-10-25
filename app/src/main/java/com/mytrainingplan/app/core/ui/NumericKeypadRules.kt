package com.mytrainingplan.app.core.ui

object NumericKeypadRules {
    const val TECLA_BORRAR = "⌫"
    const val TECLA_COMA = ","
    const val TECLA_OK = "OK"

    fun applyKey(
        texto: String,
        tecla: String,
        admiteDecimal: Boolean
    ): String {
        return when (tecla) {
            TECLA_BORRAR -> borrar(texto, admiteDecimal)
            TECLA_COMA -> if (admiteDecimal) NumberInput.sanitizeDecimal(texto + ",") else texto
            TECLA_OK -> texto
            else -> if (tecla.length == 1 && tecla[0].isDigit()) digito(texto, tecla, admiteDecimal) else texto
        }
    }

    private fun digito(texto: String, d: String, admiteDecimal: Boolean): String =
        if (admiteDecimal) NumberInput.sanitizeDecimal(texto + d)
        else NumberInput.sanitizeInt(texto + d)

    private fun borrar(texto: String, admiteDecimal: Boolean): String {
        val sinUltimo = texto.dropLast(1)
        return if (sinUltimo.isEmpty()) ""
        else if (admiteDecimal) NumberInput.sanitizeDecimal(sinUltimo)
        else NumberInput.sanitizeInt(sinUltimo)
    }
}
