package com.mytrainingplan.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Reglas puras de duración (spec 023, §6): estimación y tiempo real.
 * Sin Android; `runBlocking` no hace falta porque son funciones puras.
 */
class RoutineDurationRulesTest {

    private val rules = RoutineDurationRules

    @Test
    fun estimacion_sin_series_es_nulo() {
        assertNull(rules.estimate(emptyList()))
    }

    @Test
    fun estimacion_una_serie_descanso_90_son_2_min() {
        val sets = listOf(PlannedSet(id = "s1", routineExerciseId = "re", setNumber = 1, restSeconds = 90))
        assertEquals(2, rules.estimate(sets)) // (90 + 30) / 60 = 2
    }

    @Test
    fun estimacion_dos_series_descanso_90_son_4_min() {
        val sets = listOf(
            PlannedSet(id = "s1", routineExerciseId = "re", setNumber = 1, restSeconds = 90),
            PlannedSet(id = "s2", routineExerciseId = "re", setNumber = 2, restSeconds = 90)
        )
        assertEquals(4, rules.estimate(sets)) // (180 + 60) / 60 = 4
    }

    @Test
    fun estimacion_minimo_1_si_hay_series() {
        val sets = listOf(PlannedSet(id = "s1", routineExerciseId = "re", setNumber = 1, restSeconds = 0))
        assertEquals(1, rules.estimate(sets)) // (0 + 30) / 60 = 0 → mínimo 1
    }

    @Test
    fun tiempo_real_minimo_1_sin_haber_corrido() {
        assertEquals(1, rules.fromRealSeconds(0))
    }

    @Test
    fun tiempo_real_menos_de_un_minuto_es_1() {
        assertEquals(1, rules.fromRealSeconds(59))
    }

    @Test
    fun tiempo_real_un_minuto_es_1() {
        assertEquals(1, rules.fromRealSeconds(60))
    }

    @Test
    fun tiempo_real_dos_minutos_son_2() {
        assertEquals(2, rules.fromRealSeconds(120))
    }
}
