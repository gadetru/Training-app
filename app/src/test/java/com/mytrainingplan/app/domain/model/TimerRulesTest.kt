package com.mytrainingplan.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Spec 022 §61: fórmula y estados del cronómetro (JVM-testable, sin Android
 * ni Room). `timerElapsedSec(nowMs)` y `timerState`.
 */
class TimerRulesTest {

    @Test
    fun sesion_sin_empezar_estado_sin_empezar_y_elapsed_0() {
        val s = WorkoutSession(id = "s1", routineId = "r", startedAt = 1000L)
        assertEquals(TimerState.SIN_EMPEZAR, s.timerState)
        assertEquals(0, s.timerElapsedSec(5000L))
    }

    @Test
    fun sesion_activa_suma_base_mas_tramo() {
        val s = WorkoutSession(
            id = "s1", routineId = "r", startedAt = 1000L,
            timerBaseSec = 120, runningSince = 4000L
        )
        assertEquals(TimerState.ACTIVO, s.timerState)
        // 120 + (9000 - 4000)/1000 = 125
        assertEquals(125, s.timerElapsedSec(9000L))
    }

    @Test
    fun sesion_en_pausa_congelada_en_base() {
        val s = WorkoutSession(
            id = "s1", routineId = "r", startedAt = 1000L,
            timerBaseSec = 200, runningSince = null
        )
        assertEquals(TimerState.EN_PAUSA, s.timerState)
        assertEquals(200, s.timerElapsedSec(99999L))
    }

    @Test
    fun timer_elapsed_no_es_negativo() {
        val s = WorkoutSession(
            id = "s1", routineId = "r", startedAt = 1000L,
            timerBaseSec = 0, runningSince = 5000L
        )
        // now menor que runningSince -> tramo negativo, coerceAtLeast(0)
        assertEquals(0, s.timerElapsedSec(1000L))
    }
}
