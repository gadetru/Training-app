package com.mytrainingplan.app.data.repository

import com.mytrainingplan.app.domain.model.HistoryCalcs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Edición en vivo (spec 016, pasos 1-2 + 6-7): clonado de la última serie,
 * simetría del borrado (viva + planificada), prefill al día siguiente con
 * las añadidas, descarte que conserva la edición y auto-reemplazo del
 * apunte al finalizar. Con falsos en memoria (misma firma que los Room).
 * Sin `kotlinx-coroutines-test` (no declarado): `runBlocking` basta.
 */
class WorkoutLiveEditTest {

    private val exerciseId = "quads/barbell-bench-squat"

    private data class Fixture(
        val routines: FakeRoutineRepository,
        val schedules: FakeScheduleRepository,
        val workouts: FakeWorkoutRepository,
        val routineId: String,
        val routineExerciseId: String
    )

    private suspend fun fixture(planReps: Int = 10, planWeight: Double = 60.0): Fixture {
        val exercises = FakeExerciseRepository()
        val routines = FakeRoutineRepository(exercises = exercises)
        val schedules = FakeScheduleRepository(routines = routines)
        val workouts = FakeWorkoutRepository(routines = routines, exercises = exercises, schedule = schedules)
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
        return Fixture(routines, schedules, workouts, routineId, item.routineExercise.id)
    }

    @Test
    fun add_clona_ultima_con_numero_siguiente() = runBlocking {
        val f = fixture()
        val sessionId = f.workouts.startSession(f.routineId)
        val first = f.workouts.observeSession(sessionId).first()!!.items
            .single().entries.single()
        f.workouts.updateEntry(sessionId, first.id, reps = 8, weightKg = 70.0)

        f.workouts.addLiveSet(sessionId, f.routineExerciseId)

        val entries = f.workouts.observeSession(sessionId).first()!!.items
            .single().entries.sortedBy { it.setNumber }
        assertEquals(2, entries.size)
        val added = entries.last()
        assertEquals(2, added.setNumber)
        assertEquals(8, added.reps)
        assertEquals(70.0, added.weightKg, 0.0)
        assertEquals(90, added.restSeconds)
        assertFalse(added.done)
        assertNotNull(added.plannedSetId)
    }

    @Test
    fun add_tambien_crea_planificada_en_rutina() = runBlocking {
        val f = fixture()
        val sessionId = f.workouts.startSession(f.routineId)
        val first = f.workouts.observeSession(sessionId).first()!!.items
            .single().entries.single()
        f.workouts.updateEntry(sessionId, first.id, reps = 8, weightKg = 70.0)

        f.workouts.addLiveSet(sessionId, f.routineExerciseId)

        val sets = f.routines.observeRoutine(f.routineId).first()!!.items
            .single().sets.sortedBy { it.setNumber }
        assertEquals(2, sets.size)
        assertEquals(8, sets.last().targetReps)
        assertEquals(70.0, sets.last().weightKg, 0.0)
    }

    @Test
    fun prefill_otro_dia_trae_aniadidas() = runBlocking {
        val f = fixture()
        val firstId = f.workouts.startSession(f.routineId)
        f.workouts.addLiveSet(firstId, f.routineExerciseId)
        f.workouts.addLiveSet(firstId, f.routineExerciseId)
        f.workouts.finishSession(firstId)

        val secondId = f.workouts.startSession(f.routineId)
        val entries = f.workouts.observeSession(secondId).first()!!.items
            .single().entries

        assertEquals(3, entries.size)
    }

    @Test
    fun borrar_pendiente_es_simetrico() = runBlocking {
        val f = fixture()
        val sessionId = f.workouts.startSession(f.routineId)
        f.workouts.addLiveSet(sessionId, f.routineExerciseId)
        val added = f.workouts.observeSession(sessionId).first()!!.items
            .single().entries.maxByOrNull { it.setNumber }!!

        f.workouts.deleteLiveSet(sessionId, added.id)

        val entries = f.workouts.observeSession(sessionId).first()!!.items
            .single().entries
        assertEquals(1, entries.size)
        val sets = f.routines.observeRoutine(f.routineId).first()!!.items
            .single().sets
        assertEquals(1, sets.size)
    }

    @Test
    fun borrar_hecha_no_hace_nada() = runBlocking {
        val f = fixture()
        val sessionId = f.workouts.startSession(f.routineId)
        val first = f.workouts.observeSession(sessionId).first()!!.items
            .single().entries.single()
        f.workouts.toggleSetDone(sessionId, first.id, done = true)

        f.workouts.deleteLiveSet(sessionId, first.id)

        val entries = f.workouts.observeSession(sessionId).first()!!.items
            .single().entries
        assertEquals(1, entries.size)
        val sets = f.routines.observeRoutine(f.routineId).first()!!.items
            .single().sets
        assertEquals(1, sets.size)
    }

    @Test
    fun descartar_conserva_edicion_de_rutina() = runBlocking {
        val f = fixture()
        val sessionId = f.workouts.startSession(f.routineId)
        f.workouts.addLiveSet(sessionId, f.routineExerciseId)

        f.workouts.discardSession(sessionId)

        assertNull(f.workouts.observeSession(sessionId).first())
        val sets = f.routines.observeRoutine(f.routineId).first()!!.items
            .single().sets
        assertEquals(2, sets.size)
    }

    @Test
    fun finalizar_otra_rutina_sustituye_apunte_sin_duplicar() = runBlocking {
        val f = fixture()
        val piernaId = f.routines.createRoutine(name = "Pierna")
        f.routines.saveRoutine(piernaId)
        val today = HistoryCalcs.todayKey()
        f.schedules.setScheduled(today, piernaId)

        val sessionId = f.workouts.startSession(f.routineId)
        f.workouts.finishSession(sessionId)

        assertEquals(
            f.routineId,
            f.schedules.observeScheduledByDate(today).first()!!.routineId
        )
        assertEquals(
            1,
            f.schedules.observeScheduledBetween(today, "9999-12-31").first().size
        )
    }

    // ---------- Spec 021: replaceExercise ----------

    private val newExerciseId = "pectorals/barbell-bench-press"

    @Test
    fun replace_reescribe_entradas_hechas_incluidas() = runBlocking {
        val f = fixture()
        val sessionId = f.workouts.startSession(f.routineId)
        val item = f.workouts.observeSession(sessionId).first()!!.items.single()
        val entry = item.entries.single()
        f.workouts.toggleSetDone(sessionId, entry.id, done = true)

        f.workouts.replaceExercise(sessionId, item.routineExercise.id, newExerciseId)

        val after = f.workouts.observeSession(sessionId).first()!!.items.single()
        assertEquals(newExerciseId, after.exercise.id)
        assertEquals(newExerciseId, after.routineExercise.exerciseId)
        assertEquals(newExerciseId, after.entries.single().exerciseId)
        assertTrue(after.entries.single().done) // hecha sigue hecha
    }

    @Test
    fun replace_planificados_intactos() = runBlocking {
        val f = fixture()
        val sessionId = f.workouts.startSession(f.routineId)
        val item = f.workouts.observeSession(sessionId).first()!!.items.single()
        val plannedBefore = item.planned.single()

        f.workouts.replaceExercise(sessionId, item.routineExercise.id, newExerciseId)

        val after = f.workouts.observeSession(sessionId).first()!!.items.single()
        // El slot de rutina cambió de ejercicio pero su posición/id se conserva;
        // las planificadas siguen vinculadas al mismo routineExerciseId.
        assertEquals(item.routineExercise.id, after.routineExercise.id)
        assertEquals(item.routineExercise.position, after.routineExercise.position)
        assertEquals(newExerciseId, after.routineExercise.exerciseId)
        assertEquals(plannedBefore.id, after.planned.single().id)
        assertEquals(plannedBefore.targetReps, after.planned.single().targetReps)
    }

    @Test
    fun replace_duplicado_permitido() = runBlocking {
        val f = fixture()
        val routineId = f.routineId
        // Añade un segundo ejercicio a la rutina para forzar duplicado.
        f.routines.addExercises(routineId, listOf(newExerciseId))
        f.routines.saveRoutine(routineId)
        val sessionId = f.workouts.startSession(routineId)
        val squatItem = f.workouts.observeSession(sessionId).first()!!.items
            .single { it.routineExercise.exerciseId == "quads/barbell-bench-squat" }

        f.workouts.replaceExercise(sessionId, squatItem.routineExercise.id, newExerciseId)

        val after = f.workouts.observeSession(sessionId).first()!!.items
        val replaced = after.single { it.routineExercise.id == squatItem.routineExercise.id }
        assertEquals(newExerciseId, replaced.exercise.id)
        // Queda un segundo slot con el mismo ejercicio (duplicado permitido).
        assertEquals(2, after.count { it.exercise.id == newExerciseId })
    }

    @Test
    fun replace_rutina_persiste() = runBlocking {
        val f = fixture()
        val sessionId = f.workouts.startSession(f.routineId)
        val item = f.workouts.observeSession(sessionId).first()!!.items.single()

        f.workouts.replaceExercise(sessionId, item.routineExercise.id, newExerciseId)

        // Reabrir la rutina: el slot conserva el nuevo ejercicio.
        val detail = f.routines.observeRoutine(f.routineId).first()!!
        val slot = detail.items.single { it.routineExercise.id == item.routineExercise.id }
        assertEquals(newExerciseId, slot.routineExercise.exerciseId)
    }

    @Test
    fun replace_id_igual_no_hace_nada() = runBlocking {
        val f = fixture()
        val sessionId = f.workouts.startSession(f.routineId)
        val item = f.workouts.observeSession(sessionId).first()!!.items.single()

        f.workouts.replaceExercise(sessionId, item.routineExercise.id, item.exercise.id)

        val after = f.workouts.observeSession(sessionId).first()!!.items.single()
        assertEquals(item.exercise.id, after.exercise.id)
    }

    @Test
    fun replace_sesion_inexistente_no_cambia() = runBlocking {
        val f = fixture()
        f.workouts.replaceExercise("sesion-inesistente", "re-inesistente", newExerciseId)
        // No debe lanzar.
    }
}