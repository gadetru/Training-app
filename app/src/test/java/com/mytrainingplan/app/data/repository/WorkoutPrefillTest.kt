package com.mytrainingplan.app.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Prefill última-vs-plan y cierre sin fantasmas (specs 005/009; spec 011,
 * paso 10): con falsos en memoria (misma firma que los Room).
 * Romper el relleno con la última sesión hace fallar su prueba.
 * Sin `kotlinx-coroutines-test` (no declarado): `runBlocking` de
 * `kotlinx-coroutines-core` (transitivo); los falsos resuelven al instante.
 */
class WorkoutPrefillTest {

    private val exerciseId = "quads/barbell-bench-squat"

    private data class Fixture(
        val routines: FakeRoutineRepository,
        val workouts: FakeWorkoutRepository,
        val routineId: String
    )

    private suspend fun fixture(planReps: Int = 10, planWeight: Double = 60.0): Fixture {
        val exercises = FakeExerciseRepository()
        val routines = FakeRoutineRepository(exercises = exercises)
        val workouts = FakeWorkoutRepository(routines = routines, exercises = exercises)
        val routineId = routines.createRoutine(name = "Torso")
        routines.addExercises(routineId, listOf(exerciseId))
        val detail = routines.observeRoutine(routineId).first()!!
        val item = detail.items.single()
        val set = item.sets.single()
        routines.updateSet(
            routineExerciseId = item.routineExercise.id,
            setId = set.id,
            targetReps = planReps,
            weightKg = planWeight,
            restSeconds = 90,
            loadNote = null,
            rir = null
        )
        routines.saveRoutine(routineId)
        return Fixture(routines, workouts, routineId)
    }

    @Test
    fun sin_historial_prefill_usa_lo_planificado() = runBlocking {
        val f = fixture(planReps = 10, planWeight = 60.0)

        val sessionId = f.workouts.startSession(f.routineId)
        val entry = f.workouts.observeSession(sessionId).first()!!.items
            .single().entries.single()

        assertEquals(10, entry.reps)
        assertEquals(60.0, entry.weightKg, 0.0)
    }

    @Test
    fun con_historial_prefill_usa_ultima_sesion() = runBlocking {
        val f = fixture(planReps = 10, planWeight = 60.0)
        val firstId = f.workouts.startSession(f.routineId)
        val firstEntry = f.workouts.observeSession(firstId).first()!!.items
            .single().entries.single()
        f.workouts.updateEntry(firstId, firstEntry.id, reps = 8, weightKg = 70.0)
        f.workouts.toggleSetDone(firstId, firstEntry.id, done = true)
        f.workouts.finishSession(firstId)

        val secondId = f.workouts.startSession(f.routineId)
        val entry = f.workouts.observeSession(secondId).first()!!.items
            .single().entries.single()

        assertEquals(8, entry.reps)
        assertEquals(70.0, entry.weightKg, 0.0)
    }

    @Test
    fun descartar_no_deja_fantasma_ni_alimenta_prefill() = runBlocking {
        val f = fixture(planReps = 10, planWeight = 60.0)
        val firstId = f.workouts.startSession(f.routineId)
        val firstEntry = f.workouts.observeSession(firstId).first()!!.items
            .single().entries.single()
        f.workouts.updateEntry(firstId, firstEntry.id, reps = 8, weightKg = 70.0)

        f.workouts.discardSession(firstId)

        assertNull(f.workouts.observeSession(firstId).first())
        val secondId = f.workouts.startSession(f.routineId)
        val entry = f.workouts.observeSession(secondId).first()!!.items
            .single().entries.single()
        assertEquals(10, entry.reps)
        assertEquals(60.0, entry.weightKg, 0.0)
    }

    @Test
    fun finish_cierra_la_sesion_activa() = runBlocking {
        val f = fixture()
        val sessionId = f.workouts.startSession(f.routineId)

        f.workouts.finishSession(sessionId)

        assertNull(f.workouts.observeSession(sessionId).first())
    }
}
