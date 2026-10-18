package com.mytrainingplan.app.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Flujo de duración en el fake de rutinas (spec 023, §6/§7):
 * AUTO guarda la estimación al guardar; MANUAL congela lo escrito.
 * Sin `kotlinx-coroutines-test`: `runBlocking` basta (los falsos resuelven al instante).
 */
class RoutineDurationFlowTest {

    private fun repo() = FakeRoutineRepository(exercises = FakeExerciseRepository())

    @Test
    fun auto_guardar_persiste_estimacion_de_series() = runBlocking {
        val r = repo()
        val id = r.createRoutine(name = "Auto")
        r.addExercises(id, listOf("quads/barbell-bench-squat"))
        val detail = r.observeRoutine(id).first()!!
        val re = detail.items.single().routineExercise.id
        val set = detail.items.single().sets.single().id
        r.updateSet(re, set, 10, 60.0, 90, null, null)
        r.saveRoutine(id)

        val saved = r.observeRoutine(id).first()!!.routine
        assertFalse(saved.durationManual)
        assertEquals(2, saved.durationMin) // (90 + 30) / 60 = 2
    }

    @Test
    fun auto_sin_series_guarda_cero() = runBlocking {
        val r = repo()
        val id = r.createRoutine(name = "Auto sin series")
        r.saveRoutine(id)

        val saved = r.observeRoutine(id).first()!!.routine
        assertFalse(saved.durationManual)
        assertEquals(0, saved.durationMin)
    }

    @Test
    fun manual_guardar_congela_duracion() = runBlocking {
        val r = repo()
        val id = r.createRoutine(name = "Manual", durationMin = 30, durationManual = true)
        r.addExercises(id, listOf("quads/barbell-bench-squat"))
        r.saveRoutine(id)

        val saved = r.observeRoutine(id).first()!!.routine
        assertTrue(saved.durationManual)
        assertEquals(30, saved.durationMin)
    }
}
