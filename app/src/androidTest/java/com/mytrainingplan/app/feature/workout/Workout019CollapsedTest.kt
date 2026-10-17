package com.mytrainingplan.app.feature.workout

import com.mytrainingplan.app.data.repository.FakeExerciseRepository
import com.mytrainingplan.app.data.repository.FakeRoutineRepository
import com.mytrainingplan.app.data.repository.FakeWorkoutRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Smoke del auto-colapsado (spec 019, paso 4): apertura colapsada, colapso
 * al cerrar el descanso, avance al siguiente, sin-descanso al marcar y
 * re-expansión con tap. ViewModel real + fakes en memoria (patrón del test
 * del cronómetro del 013); el tick a 0 se cubre con pausa de 1 s.
 */
class Workout019CollapsedTest {

    private val firstId = "quads/barbell-bench-squat"
    private val secondId = "quads/lever-alternate-leg-press"

    private data class Fixture(
        val vm: WorkoutViewModel,
        val scope: CoroutineScope,
        val routineId: String,
        val firstEntryId: String,
        val secondEntryId: String,
        val firstRoutineExerciseId: String,
        val secondRoutineExerciseId: String
    )

    private fun fixture(firstRest: Int = 90, secondRest: Int = 90): Fixture {
        val exercises = FakeExerciseRepository()
        val routines = FakeRoutineRepository(exercises = exercises)
        val workouts = FakeWorkoutRepository(routines = routines, exercises = exercises)
        val routineId: String
        val firstEntryId: String
        val secondEntryId: String
        val firstRe: String
        val secondRe: String
        runBlocking {
            routineId = routines.createRoutine(name = "Pierna")
            routines.addExercises(routineId, listOf(firstId, secondId))
            val detail = routines.observeRoutine(routineId).first()!!
            detail.items.forEach { item ->
                val rest = if (item.exercise.id == firstId) firstRest else secondRest
                item.sets.forEach { set ->
                    routines.updateSet(
                        routineExerciseId = item.routineExercise.id,
                        setId = set.id,
                        targetReps = set.targetReps,
                        weightKg = set.weightKg,
                        restSeconds = rest,
                        loadNote = set.loadNote,
                        rir = set.rir
                    )
                }
            }
            routines.saveRoutine(routineId)
        }
        val vm = WorkoutViewModel(workouts)
        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        scope.launch { vm.uiState.collect {} }
        vm.openSession(routineId)
        runBlocking {
            val detail2 = workouts.observeSession(
                poll(10_000) { vm.uiState.value.sessionId.ifEmpty { null } }!!
            ).first()!!
            val first = detail2.items[0]
            val second = detail2.items[1]
            firstEntryId = first.entries.single().id
            secondEntryId = second.entries.single().id
            firstRe = first.routineExercise.id
            secondRe = second.routineExercise.id
        }
        // El cronómetro debe estar activo: marcar exige no-pausa.
        vm.onPauseToggle()
        return Fixture(vm, scope, routineId, firstEntryId, secondEntryId, firstRe, secondRe)
    }

    private fun poll(timeoutMs: Long, block: () -> String?): String? {
        val deadline = System.currentTimeMillis() + timeoutMs
        var v = block()
        while (v == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(100)
            v = block()
        }
        return v
    }

    private fun await(timeoutMs: Long, cond: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!cond() && System.currentTimeMillis() < deadline) {
            Thread.sleep(100)
        }
        assertTrue("condición no alcanzada a tiempo", cond())
    }

    @Test
    fun apertura_todos_colapsados() {
        val f = fixture()
        try {
            await(10_000) { f.vm.uiState.value.exercises.size == 2 }
            assertTrue(f.vm.uiState.value.exercises.none { it.expanded })
        } finally {
            f.scope.cancel()
        }
    }

    @Test
    fun terminar_descanso_colapsa_y_avanza() {
        val f = fixture()
        try {
            await(10_000) { f.vm.uiState.value.exercises.size == 2 }
            f.vm.onToggleExpanded(f.firstRoutineExerciseId)
            await(5_000) {
                f.vm.uiState.value.exercises.firstOrNull { it.routineExercise.id == f.firstRoutineExerciseId }?.expanded == true
            }
            f.vm.onToggleDone(f.firstEntryId)
            await(10_000) { f.vm.uiState.value.restRemainingSec != null }
            f.vm.onRestFinish()
            await(10_000) {
                val list = f.vm.uiState.value.exercises
                list.first { it.routineExercise.id == f.firstRoutineExerciseId }.expanded == false &&
                    list.first { it.routineExercise.id == f.secondRoutineExerciseId }.expanded == true
            }
        } finally {
            f.scope.cancel()
        }
    }

    @Test
    fun tick_a_cero_colapsa_y_avanza() {
        val f = fixture(firstRest = 1)
        try {
            await(10_000) { f.vm.uiState.value.exercises.size == 2 }
            f.vm.onToggleDone(f.firstEntryId)
            await(10_000) { f.vm.uiState.value.restRemainingSec != null }
            // El tick cierra solo a 0 (~1 s) y dispara el mismo avance.
            await(10_000) {
                val list = f.vm.uiState.value.exercises
                f.vm.uiState.value.restRemainingSec == null &&
                    list.first { it.routineExercise.id == f.firstRoutineExerciseId }.expanded == false &&
                    list.first { it.routineExercise.id == f.secondRoutineExerciseId }.expanded == true
            }
        } finally {
            f.scope.cancel()
        }
    }

    @Test
    fun sin_descanso_colapsa_al_marcar() {
        val f = fixture(firstRest = 0)
        try {
            await(10_000) { f.vm.uiState.value.exercises.size == 2 }
            f.vm.onToggleDone(f.firstEntryId)
            await(10_000) {
                val s = f.vm.uiState.value
                s.restRemainingSec == null &&
                    s.exercises.first { it.routineExercise.id == f.firstRoutineExerciseId }.expanded == false &&
                    s.exercises.first { it.routineExercise.id == f.secondRoutineExerciseId }.expanded == true
            }
        } finally {
            f.scope.cancel()
        }
    }

    @Test
    fun ultimo_sin_pendientes_solo_colapsa_y_tap_reexpande() {
        val f = fixture()
        try {
            await(10_000) { f.vm.uiState.value.exercises.size == 2 }
            // Completa el primero y avanza al segundo.
            f.vm.onToggleDone(f.firstEntryId)
            await(10_000) { f.vm.uiState.value.restRemainingSec != null }
            f.vm.onRestFinish()
            await(10_000) {
                f.vm.uiState.value.exercises.first { it.routineExercise.id == f.secondRoutineExerciseId }.expanded
            }
            // Completa el último: solo colapsa, nada se expande.
            f.vm.onToggleDone(f.secondEntryId)
            await(10_000) { f.vm.uiState.value.restRemainingSec != null }
            f.vm.onRestFinish()
            await(10_000) {
                f.vm.uiState.value.exercises.none { it.expanded }
            }
            assertNull(f.vm.uiState.value.restRemainingSec)
            // Un tap re-expande el auto-colapsado.
            f.vm.onToggleExpanded(f.secondRoutineExerciseId)
            await(5_000) {
                f.vm.uiState.value.exercises.first { it.routineExercise.id == f.secondRoutineExerciseId }.expanded
            }
            assertEquals(2, f.vm.uiState.value.totalCount)
            assertEquals(2, f.vm.uiState.value.doneCount)
        } finally {
            f.scope.cancel()
        }
    }
}
