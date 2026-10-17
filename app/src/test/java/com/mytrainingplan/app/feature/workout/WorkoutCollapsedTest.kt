package com.mytrainingplan.app.feature.workout

import com.mytrainingplan.app.data.repository.FakeExerciseRepository
import com.mytrainingplan.app.data.repository.FakeRoutineRepository
import com.mytrainingplan.app.data.repository.FakeWorkoutRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec 019 paso 3: apertura colapsada, colapso, avance y re-expansión
 * manual. Reglas puras ([WorkoutExpandRules]) con fakes en memoria (misma
 * firma que los Room). Sin `kotlinx-coroutines-test` (no declarado):
 * `runBlocking` basta.
 */
class WorkoutCollapsedTest {

    private val firstId = "quads/barbell-bench-squat"
    private val secondId = "quads/lever-alternate-leg-press"

    private suspend fun twoExerciseItems() = runBlocking {
        val exercises = FakeExerciseRepository()
        val routines = FakeRoutineRepository(exercises = exercises)
        val workouts = FakeWorkoutRepository(routines = routines, exercises = exercises)
        val routineId = routines.createRoutine(name = "Pierna")
        routines.addExercises(routineId, listOf(firstId, secondId))
        routines.saveRoutine(routineId)
        val sessionId = workouts.startSession(routineId)
        workouts.observeSession(sessionId).first()!!.items
    }

    @Test
    fun apertura_todos_colapsados() = runBlocking {
        val items = twoExerciseItems()
        assertEquals(2, items.size)
        items.forEach {
            assertFalse(
                WorkoutExpandRules.defaultExpanded(emptyMap(), it.routineExercise.id)
            )
        }
    }

    @Test
    fun toggle_un_tap_expande_y_otro_colapsa() {
        val id = "re1"
        val expanded = WorkoutExpandRules.toggle(emptyMap(), id)
        assertTrue(WorkoutExpandRules.defaultExpanded(expanded, id))
        val collapsed = WorkoutExpandRules.toggle(expanded, id)
        assertFalse(WorkoutExpandRules.defaultExpanded(collapsed, id))
    }

    @Test
    fun colapso_avanza_al_siguiente_pendiente() = runBlocking {
        val items = twoExerciseItems()
        val finishedEntry = items[0].entries.single()
        val start = mapOf(items[0].routineExercise.id to true)

        val next = WorkoutExpandRules.afterClose(items, start, finishedEntry.id)!!

        assertEquals(false, next[items[0].routineExercise.id])
        assertEquals(true, next[items[1].routineExercise.id])
    }

    @Test
    fun sin_pendientes_solo_colapsa() = runBlocking {
        val items = twoExerciseItems()
        val lastEntry = items[1].entries.single()
        // El primero ya terminado: no es "siguiente pendiente".
        val doneItems = listOf(
            items[0].copy(entries = items[0].entries.map { it.copy(done = true) }),
            items[1]
        )
        val start = mapOf(items[1].routineExercise.id to true)

        val next = WorkoutExpandRules.afterClose(doneItems, start, lastEntry.id)!!

        assertEquals(false, next[items[1].routineExercise.id])
        assertNull(next[items[0].routineExercise.id])
    }

    @Test
    fun con_pendientes_restantes_no_cambia() = runBlocking {
        val items = twoExerciseItems()
        val exercises = FakeExerciseRepository()
        val routines = FakeRoutineRepository(exercises = exercises)
        val workouts = FakeWorkoutRepository(routines = routines, exercises = exercises)
        val routineId = routines.createRoutine(name = "Pierna")
        routines.addExercises(routineId, listOf(firstId))
        routines.saveRoutine(routineId)
        val sessionId = workouts.startSession(routineId)
        workouts.addLiveSet(
            sessionId,
            workouts.observeSession(sessionId).first()!!.items.single().routineExercise.id
        )
        val two = workouts.observeSession(sessionId).first()!!.items
        assertEquals(2, two.single().entries.size)
        val firstEntry = two.single().entries.minByOrNull { it.setNumber }!!

        assertNull(WorkoutExpandRules.afterClose(two, emptyMap(), firstEntry.id))
    }

    @Test
    fun tap_manual_pisa_lo_automatico() = runBlocking {
        val items = twoExerciseItems()
        val finishedEntry = items[0].entries.single()
        val auto = WorkoutExpandRules.afterClose(items, emptyMap(), finishedEntry.id)!!
        assertEquals(false, auto[items[0].routineExercise.id])

        // Un tap re-expande el auto-colapsado sin coste extra.
        val manual = WorkoutExpandRules.toggle(auto, items[0].routineExercise.id)
        assertTrue(WorkoutExpandRules.defaultExpanded(manual, items[0].routineExercise.id))
        // El siguiente sigue expandido: el manual no cierra lo demás.
        assertTrue(WorkoutExpandRules.defaultExpanded(manual, items[1].routineExercise.id))
    }
}
