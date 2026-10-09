package com.mytrainingplan.app.data.repository

import com.mytrainingplan.app.data.local.dao.ExerciseDao
import com.mytrainingplan.app.data.local.dao.PlannedSetDao
import com.mytrainingplan.app.data.local.dao.RoutineDao
import com.mytrainingplan.app.data.local.dao.RoutineExerciseDao
import com.mytrainingplan.app.data.mapper.toDomain
import com.mytrainingplan.app.data.mapper.toEntity
import com.mytrainingplan.app.domain.model.PlannedSet
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Repo real de rutinas (Paso 5 spec 006).
 * Misma firma que [RoutineRepository]. Persistencia directa en Room:
 * cada mutación hace `@Upsert` al momento con `updatedAt`; `saveRoutine`
 * republica la fecha y `discard` solo borra lógico si quedó vacía
 * (el split borrador/guardada de Fase A se colapsa; spec 008: descarte
 * seguro + `position` real con renumerado sin huecos).
 */
class RoomRoutineRepository(
    private val routines: RoutineDao,
    private val items: RoutineExerciseDao,
    private val sets: PlannedSetDao,
    private val exercises: ExerciseDao
) : RoutineRepository {

    override fun observeRoutines(): Flow<List<com.mytrainingplan.app.domain.model.Routine>> =
        routines.observeAll().map { list -> list.map { it.toDomain() } }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeDetails(): Flow<List<RoutineDetail>> =
        routines.observeAll().flatMapLatest { list ->
            if (list.isEmpty()) flowOf(emptyList())
            else combine(list.map { r -> detailFlow(r.id) }) { it.filterNotNull() }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeRoutine(id: String): Flow<RoutineDetail?> =
        detailFlow(id)

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun detailFlow(id: String): Flow<RoutineDetail?> =
        combine(
            routines.observeById(id),
            items.observeByRoutine(id)
        ) { routine, res -> routine to res }
            .flatMapLatest { (routine, res) ->
                if (routine == null) flowOf(null)
                else if (res.isEmpty()) flowOf(RoutineDetail(routine.toDomain(), emptyList()))
                else combine(
                    res.map { re ->
                        combine(
                            exerciseFlow(re.exerciseId),
                            sets.observeByExercise(re.id)
                        ) { ex, ss -> Triple(re, ex, ss) }
                    }
                ) { arr ->
                    RoutineDetail(
                        routine = routine.toDomain(),
                        items = arr.mapNotNull { (re, ex, ss) ->
                            val e = ex?.toDomain() ?: return@mapNotNull null
                            RoutineExerciseDetail(
                                routineExercise = re.toDomain(),
                                exercise = e,
                                sets = ss.map { it.toDomain() }.sortedBy { it.setNumber }
                            )
                        }
                    )
                }
            }

    /** Ejercicio como Flow (el DAO solo expone `getById` suspendido). */
    private fun exerciseFlow(id: String) = kotlinx.coroutines.flow.flow {
        emit(exercises.getById(id))
    }

    override suspend fun createRoutine(name: String, durationMin: Int): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        // Spec 008: `position` real = nº de rutinas vivas al crear (orden de creación).
        val count = routines.observeAll().first().size
        routines.upsert(
            com.mytrainingplan.app.domain.model.Routine(
                id = id, name = name, durationMin = durationMin,
                position = count, createdAt = now, updatedAt = now
            ).toEntity(now)
        )
        return id
    }

    override suspend fun updateMeta(routineId: String, name: String, durationMin: Int) {
        val cur = routines.getById(routineId)?.toDomain() ?: return
        routines.upsert(cur.copy(name = name, durationMin = durationMin).toEntity())
    }

    override suspend fun addExercises(routineId: String, ids: List<String>) {
        if (ids.isEmpty()) return
        routines.getById(routineId) ?: return
        // Spec 008: la posición del ejercicio continúa tras el máximo existente
        // (antes se reiniciaba a 0 en cada llamada).
        val existing = items.observeByRoutine(routineId).first()
        var pos = (existing.maxOfOrNull { it.position } ?: -1) + 1
        ids.forEach { exId ->
            val ex = exercises.getById(exId) ?: return@forEach
            val reId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            items.upsert(
                com.mytrainingplan.app.domain.model.RoutineExercise(
                    id = reId, routineId = routineId, exerciseId = ex.id,
                    position = pos++, updatedAt = now
                ).toEntity(now)
            )
            sets.upsert(
                PlannedSet(
                    id = UUID.randomUUID().toString(), routineExerciseId = reId,
                    setNumber = 1, targetReps = 0, weightKg = 0.0, restSeconds = 90
                ).toEntity(now)
            )
        }
    }

    override suspend fun updateSet(
        routineExerciseId: String,
        setId: String,
        targetReps: Int,
        weightKg: Double,
        restSeconds: Int,
        loadNote: String?,
        rir: Int?
    ) {
        val target = sets.observeByExercise(routineExerciseId).first()
            .firstOrNull { it.id == setId }?.toDomain() ?: return
        sets.upsert(
            target.copy(
                targetReps = targetReps, weightKg = weightKg,
                restSeconds = restSeconds, loadNote = loadNote, rir = rir
            ).toEntity()
        )
    }

    override suspend fun updateExerciseNote(routineExerciseId: String, note: String) {
        val cur = items.getById(routineExerciseId)?.toDomain() ?: return
        items.upsert(cur.copy(note = note).toEntity())
        // Espejo a todas las series: el prefill de sesión lee `loadNote` por serie.
        sets.observeByExercise(routineExerciseId).first()
            .map { it.toDomain().copy(loadNote = note.ifBlank { null }) }
            .forEach { sets.upsert(it.toEntity()) }
    }

    override suspend fun addSet(routineExerciseId: String) {
        val list = sets.observeByExercise(routineExerciseId).first()
        val first = list.minByOrNull { it.setNumber }?.toDomain()
        val next = (list.maxOfOrNull { it.setNumber } ?: 0) + 1
        val now = System.currentTimeMillis()
        val fresh = first?.copy(id = UUID.randomUUID().toString(), setNumber = next)
            ?: PlannedSet(
                id = UUID.randomUUID().toString(), routineExerciseId = routineExerciseId,
                setNumber = next, targetReps = 0, weightKg = 0.0, restSeconds = 90
            )
        sets.upsert(fresh.toEntity(now))
    }

    override suspend fun deleteSet(routineExerciseId: String, setId: String) {
        val list = sets.observeByExercise(routineExerciseId).first()
        val gone = list.firstOrNull { it.id == setId }?.toDomain() ?: return
        sets.upsert(gone.copy(deleted = true).toEntity())
    }

    override suspend fun deleteExercise(routineExerciseId: String) {
        val cur = items.getById(routineExerciseId)?.toDomain() ?: return
        items.upsert(cur.copy(deleted = true).toEntity())
    }

    override suspend fun saveRoutine(routineId: String) {
        val cur = routines.getById(routineId)?.toDomain() ?: return
        routines.upsert(cur.toEntity())
    }

    override suspend fun discard(routineId: String) {
        // Descarte seguro (spec 008): solo el borrador vacío se borra lógico;
        // la guardada con contenido queda intacta.
        val cur = routines.getById(routineId)?.toDomain() ?: return
        val existing = items.observeByRoutine(routineId).first()
        if (existing.isNotEmpty()) return
        routines.upsert(cur.copy(deleted = true).toEntity())
        renumberRoutines()
    }

    override suspend fun deleteRoutine(routineId: String) {
        // Borrado desde el menú ··· de Home: borrado lógico + renumber sin huecos.
        // Las sesiones históricas quedan intactas (WorkoutSession.routineId es
        // solo referencia, sin FK en cascada).
        val cur = routines.getById(routineId)?.toDomain() ?: return
        routines.upsert(cur.copy(deleted = true).toEntity())
        renumberRoutines()
    }

    /** Recalcula `position` 0..n-1 sin huecos tras un borrado lógico (spec 008). */
    private suspend fun renumberRoutines() {
        val remaining = routines.observeAll().first().sortedBy { it.position }
        remaining.forEachIndexed { index, entity ->
            if (entity.position != index) {
                routines.upsert(
                    entity.copy(position = index, updatedAt = System.currentTimeMillis())
                )
            }
        }
    }
}
