package com.mytrainingplan.app.core.ui

/**
 * Regla única de limpieza de campos numéricos (spec 020): nunca ceros a la
 * izquierda, aplicada mientras se teclea. Pura, sin Compose.
 */
object NumberInput {
    /** Solo dígitos; quita ceros a la izquierda salvo un `0` suelto. `""` se conserva vacío. */
    fun sanitizeInt(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return if (digits.isEmpty()) ""
        else digits.dropWhile { it == '0' }.takeIf { it.isNotEmpty() } ?: "0"
    }

    /**
     * Dígitos + un separador decimal (`,` → `.`). Respeta `0.x`, un punto final al
     * teclear y un punto suelto. Máximo un punto; los siguientes se descartan.
     */
    fun sanitizeDecimal(raw: String): String {
        val only = raw.replace(',', '.').filter { c -> c.isDigit() || c == '.' }
        if (only.isEmpty()) return ""
        val dotIdx = only.indexOf('.')
        val intPart = if (dotIdx < 0) only else only.substring(0, dotIdx)
        var fracPart = if (dotIdx < 0) "" else only.substring(dotIdx + 1).filter { it != '.' }
        val intTrimmed = intPart.dropWhile { it == '0' }
        return buildString {
            val intShow = if (intTrimmed.isNotEmpty()) intTrimmed
            else if (intPart.isNotEmpty()) "0"
            else ""
            append(intShow)
            if (dotIdx >= 0) {
                if (intShow.isEmpty() && fracPart.isNotEmpty()) append("0")
                append(".")
                append(fracPart)
            } else if (intShow.isEmpty()) {
                append("0")
            }
        }
    }
}
