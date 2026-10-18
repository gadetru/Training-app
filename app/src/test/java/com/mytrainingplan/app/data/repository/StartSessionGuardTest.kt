package com.mytrainingplan.app.data.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Spec 022 §61: guardia anti-doble en el fake — `startSession` no crea una
 * segunda sesión si ya hay viva; devuelve la viva.
 */
class StartSessionGuardTest {

    @Test
    fun start_session_no_crea_segunda_si_ya_hay_viva() = runBlocking {
        val exercises = FakeExerciseRepository()
        val routines = FakeRoutineRepository(exercises = exercises)
        val workouts = FakeWorkoutRepository(routines = routines, exercises = exercises)
        val routineId = routines.createRoutine(name = "Torso")

        val id1 = workouts.startSession(routineId)
        val id2 = workouts.startSession(routineId)

        assertEquals(id1, id2)
    }

    @Test
    fun despues_de_finalizar_se_puede_crear_nueva() = runBlocking {
        val exercises = FakeExerciseRepository()
        val routines = FakeRoutineRepository(exercises = exercises)
        val workouts = FakeWorkoutRepository(routines = routines, exercises = exercises)
        val routineId = routines.createRoutine(name = "Torso")

        val id1 = workouts.startSession(routineId)
        workouts.finishSession(id1)
        val id2 = workouts.startSession(routineId)

        assertEquals(false, id1 == id2)
    }
}
