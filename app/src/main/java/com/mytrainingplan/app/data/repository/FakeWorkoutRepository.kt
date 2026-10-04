package com.mytrainingplan.app.data.repository

import com.mytrainingplan.app.domain.model.Exercise
import com.mytrainingplan.app.domain.model.PlannedSet
import com.mytrainingplan.app.domain.model.RoutineExercise
import com.mytrainingplan.app.domain.model.SetEntry
import com.mytrainingplan.app.domain.model.WorkoutSession
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Ítem de sesión: ejercicio resuelto + plan + entradas en vivo.
 * Solo compone modelos de `domain/model`; el ViewModel lo mapea a
 * `WorkoutExerciseUi` (añadiendo `expanded`, solo pantalla).
 */
data class WorkoutSessionItem(
    val routineExercise: RoutineExercise,
    val exercise: Exercise,
    val planned: List<PlannedSet> = emptyList(),
    val entries: List<SetEntry> = emptyList()
)

/**
 * Sesión activa con sus ejercicios resueltos.
 * El futuro repositorio real (Room) devolverá esta misma forma.
 */
data class WorkoutSessionDetail(
    val session: WorkoutSession,
    val routineName: String,
    val items: List<WorkoutSessionItem> = emptyList()
)

/**
 * Firma futura del repositorio de sesiones (Fase B: Room).
 * El fake de Fase A implementa esta misma firma.
 */
interface WorkoutRepository {
    /** Crea la sesión con prefill última-vs-plan y devuelve su UUID. */
    suspend fun startSession(routineId: String): String

    /** Sesión activa (null si no existe o ya se cerró). */
    fun observeSession(id: String): Flow<WorkoutSessionDetail?>

    suspend fun toggleSetDone(sessionId: String, entryId: String, done: Boolean)

    suspend fun updateEntry(sessionId: String, entryId: String, reps: Int, weightKg: Double)

    /** Ajusta el descanso de una entrada en pasos de 10 s. */
    suspend fun adjustRest(sessionId: String, entryId: String, deltaSec: Int)

    /** Guarda la sesión (alimenta el prefill futuro) y la cierra. */
    suspend fun finishSession(sessionId: String)

    /** Descarta sin guardar: no deja sesión fantasma. */
    suspend fun discardSession(sessionId: String)

    /** Últimas entradas de ese ejercicio en ese día (para prefill). */
    fun observeLastEntries(routineId: String, exerciseId: String): Flow<List<SetEntry>>
}

/**
 * Repo fake en memoria (spec 005, Fase A).
 * Sin Room, sin Hilt, sin red. Sesiones activas separadas del historial:
 * salir atrás sin finalizar nunca deja sesiones fantasma ni corrompe el
 * prefill (patrón borrador/guardada de `FakeRoutineRepository`).
 *
 * Sin DI en Fase A: los ViewModels comparten [shared] para ver el mismo
 * store en memoria. En Fase B se inyectará el repositorio real con Hilt
 * manteniendo la firma de [WorkoutRepository].
 */
class FakeWorkoutRepository(
    private val routines: RoutineRepository = FakeRoutineRepository.shared,
    private val exercises: ExerciseRepository = FakeExerciseRepository()
) : WorkoutRepository {

    private val sessions = MutableStateFlow<Map<String, WorkoutSessionDetail>>(emptyMap())

    /** Historial de finalizadas por `(routineId, exerciseId)`. */
    private val history = MutableStateFlow<Map<Pair<String, String>, List<SetEntry>>>(emptyMap())

    override fun observeSession(id: String): Flow<WorkoutSessionDetail?> =
        sessions.map { map -> map[id] }

    override fun observeLastEntries(routineId: String, exerciseId: String): Flow<List<SetEntry>> =
        history.map { map -> map[routineId to exerciseId] ?: emptyList() }

    override suspend fun startSession(routineId: String): String {
        val sessionId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val detail = routines.observeRoutine(routineId).firstOrNull()
        if (detail == null) {
            // Id inexistente: estado vacío sin crash (la UI permite volver).
            sessions.update { it + (sessionId to WorkoutSessionDetail(
                session = WorkoutSession(id = sessionId, routineId = routineId, startedAt = now, updatedAt = now),
                routineName = ""
            )) }
            return sessionId
        }
        val items = detail.items.map { item ->
            val last = history.value[routineId to item.exercise.id] ?: emptyList()
            WorkoutSessionItem(
                routineExercise = item.routineExercise,
                exercise = item.exercise,
                planned = item.sets.sortedBy { it.setNumber },
                entries = item.sets.sortedBy { it.setNumber }.map { planned ->
                    // Prefill `docs/MODELO_DE_DATOS.md:86`: última sesión de
                    // ese ejercicio en ese día si existe, si no lo planificado.
                    val prev = last.find { it.plannedSetId == planned.id }
                        ?: last.find { it.setNumber == planned.setNumber }
                    SetEntry(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        exerciseId = item.exercise.id,
                        plannedSetId = planned.id,
                        setNumber = planned.setNumber,
                        reps = prev?.reps ?: planned.targetReps,
                        weightKg = prev?.weightKg ?: planned.weightKg,
                        loadNote = prev?.loadNote ?: planned.loadNote,
                        restSeconds = prev?.restSeconds ?: planned.restSeconds,
                        done = false,
                        updatedAt = now
                    )
                }
            )
        }
        sessions.update { map ->
            map + (sessionId to WorkoutSessionDetail(
                session = WorkoutSession(
                    id = sessionId,
                    routineId = routineId,
                    startedAt = now,
                    updatedAt = now
                ),
                routineName = detail.routine.name,
                items = items
            ))
        }
        return sessionId
    }

    override suspend fun toggleSetDone(sessionId: String, entryId: String, done: Boolean) {
        mutateEntry(sessionId, entryId) { it.copy(done = done, updatedAt = System.currentTimeMillis()) }
    }

    override suspend fun updateEntry(sessionId: String, entryId: String, reps: Int, weightKg: Double) {
        mutateEntry(sessionId, entryId) {
            it.copy(reps = reps.coerceAtLeast(0), weightKg = weightKg, updatedAt = System.currentTimeMillis())
        }
    }

    override suspend fun adjustRest(sessionId: String, entryId: String, deltaSec: Int) {
        mutateEntry(sessionId, entryId) { entry ->
            val base = entry.restSeconds ?: 90
            entry.copy(restSeconds = (base + deltaSec).coerceAtLeast(0), updatedAt = System.currentTimeMillis())
        }
    }

    override suspend fun finishSession(sessionId: String) {
        val detail = sessions.value[sessionId] ?: return
        val now = System.currentTimeMillis()
        // El historial alimenta el prefill de la próxima apertura.
        history.update { map ->
            var next = map
            detail.items.forEach { item ->
                val key = detail.session.routineId to item.exercise.id
                next = next + (key to item.entries.map { it.copy(updatedAt = now) })
            }
            next
        }
        sessions.update { map ->
            val current = map[sessionId] ?: return@update map
            map + (sessionId to current.copy(
                session = current.session.copy(endedAt = now, updatedAt = now)
            ))
        }
        // La finalizada no sigue viva: se retira de activas.
        sessions.update { it - sessionId }
    }

    override suspend fun discardSession(sessionId: String) {
        sessions.update { it - sessionId }
    }

    private inline fun mutateEntry(
        sessionId: String,
        entryId: String,
        transform: (SetEntry) -> SetEntry
    ) {
        sessions.update { map ->
            val current = map[sessionId] ?: return@update map
            val updated = current.copy(
                items = current.items.map { item ->
                    item.copy(
                        entries = item.entries.map { entry ->
                            if (entry.id == entryId) transform(entry) else entry
                        }
                    )
                }
            )
            map + (sessionId to updated)
        }
    }

    companion object {
        /** Store compartido en memoria para Fase A sin DI. */
        val shared: FakeWorkoutRepository by lazy { FakeWorkoutRepository() }
    }
}
