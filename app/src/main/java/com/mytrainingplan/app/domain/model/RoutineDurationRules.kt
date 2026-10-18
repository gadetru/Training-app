package com.mytrainingplan.app.domain.model

/**
 * Reglas puras de duración de rutina (spec 023).
 * Sin dependencias Android / Room / Retrofit; testeable en JVM.
 *
 * - Modo AUTO: duración estimada = Σ descansos de las series planificadas
 *   + 30 s por serie, redondeo al minuto más cercano, mínimo 1.
 *   Sin series → null (se muestra "—").
 * - Modo tiempo real (al finalizar una sesión AUTO): segundos del cronómetro
 *   → minutos con mínimo 1 si hubo sesión.
 */
object RoutineDurationRules {

    /** Estimación de duración a partir de las series planificadas. */
    fun estimate(sets: List<PlannedSet>): Int? {
        if (sets.isEmpty()) return null
        val totalSec = sets.sumOf { it.restSeconds } + 30 * sets.size
        return (totalSec + 30) / 60
    }

    /** Duración real de una sesión terminada (segundos → minutos, mínimo 1). */
    fun fromRealSeconds(sec: Int): Int {
        return sec.coerceAtLeast(60) / 60
    }
}
