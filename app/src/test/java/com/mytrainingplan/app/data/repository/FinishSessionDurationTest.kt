package com.mytrainingplan.app.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Duración al finalizar sesión (spec 023, §7):
 * AUTO adopta el tiempo real medido (mín. 1); MANUAL queda intacta.
 * Usa falsos en memoria (misma firma que Room).
 */
class FinishSessionDurationTest {

    private val exerciseId = "quads/barbell-bench-squat"

    private data class Fixture(
        val routines: FakeRoutineRepository,
        val workouts: FakeWorkoutRepository,
        val routineId: String
    )

    private suspend fun fixture(durationMin: Int, durationManual: Boolean): Fixture {
        val exercises = FakeExerciseRepository()
        val routines = FakeRoutineRepository(exercises = exercises)
        val workouts = FakeWorkoutRepository(routines = routines, exercises = exercises)
        val routineId = routines.createRoutine(name = "R", durationMin = durationMin, durationManual = durationManual)
        routines.addExercises(routineId, listOf(exerciseId))
        val detail = routines.observeRoutine(routineId).first()!!
        val item = detail.items.single()
        val set = item.sets.single()
        routines.updateSet(
            routineExerciseId = item.routineExercise.id,
            setId = set.id,
            targetReps = 10,
            weightKg = 60.0,
            restSeconds = 90,
            loadNote = null,
            rir = null
        )
        routines.saveRoutine(routineId)
        return Fixture(routines, workouts, routineId)
    }

    @Test
    fun finalizar_auto_adopta_tiempo_real_medido() = runBlocking {
        val f = fixture(durationMin = 0, durationManual = false)
        val sessionId = f.workouts.startSession(f.routineId)
        val now = System.currentTimeMillis()
        // Acumula 120 s en el cronómetro.
        f.workouts.timerStart(sessionId, now)
        f.workouts.timerPause(sessionId, now + 120_000)
        f.workouts.finishSession(sessionId)

        val saved = f.routines.observeRoutine(f.routineId).first()!!.routine
        assertFalse(saved.durationManual)
        assertEquals(2, saved.durationMin) // 120 s → 2 min
    }

    @Test
    fun finalizar_sin_cronometro_auto_minimo_1() = runBlocking {
        val f = fixture(durationMin = 0, durationManual = false)
        val sessionId = f.workouts.startSession(f.routineId)
        f.workouts.finishSession(sessionId)

        val saved = f.routines.observeRoutine(f.routineId).first()!!.routine
        assertFalse(saved.durationManual)
        assertEquals(1, saved.durationMin) // 0 s → mínimo 1
    }

    @Test
    fun finalizar_manual_no_cambia_duracion() = runBlocking {
        val f = fixture(durationMin = 30, durationManual = true)
        val sessionId = f.workouts.startSession(f.routineId)
        val now = System.currentTimeMillis()
        f.workouts.timerStart(sessionId, now)
        f.workouts.timerPause(sessionId, now + 120_000)
        f.workouts.finishSession(sessionId)

        val saved = f.routines.observeRoutine(f.routineId).first()!!.routine
        assertTrue(saved.durationManual)
        assertEquals(30, saved.durationMin)
    }
}
