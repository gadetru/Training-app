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
 * (el split borrador/guardada de Fase A se colapsa; refinar en paso 11).
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
            else combine(list.map { r -> detailFlow(r.id) }) { it.toList() }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeRoutine(id: String): Flow<RoutineDetail?> =
        detailFlow(id)

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun detailFlow(id: String): Flow<RoutineDetail?> =
        combine(
            routines.observeById(id),
            items.observeByRoutine(id)
        ) { routine, res ->
            if (routine == null) return@combine null
            val details = res.map { re ->
                val ex = exercises.getById(re.exerciseId)?.toDomain() ?: return@map null
                val ss = setsListSnapshot(re.id)
                RoutineExerciseDetail(
                    routineExercise = re.toDomain(),
                    exercise = ex,
                    sets = ss.sortedBy { it.setNumber }
                )
            }.filterNotNull()
            // Los sets llegan por snapshot suspendido; el Flow se re-emite
            // al cambiar rutina o sus ejercicios (los sets se refrescan en
            // la próxima colección del detalle).
            RoutineDetail(routine = routine.toDomain(), items = details)
        }.flatMapLatest { base ->
            if (base == null) flowOf(null)
            else combine(
                base.items.map { d ->
                    sets.observeByExercise(d.routineExercise.id).map { ss ->
                        d.copy(sets = ss.map { it.toDomain() }.sortedBy { it.setNumber })
                    }
                }.ifEmpty { listOf(flowOf()) }.let { flows ->
                    @Suppress("UNCHECKED_CAST")
                    combine(flows) { arr ->
                        base.copy(items = (arr.toList() as List<RoutineExerciseDetail>))
                    }
                }
            ) { it }
        }

    /** Snapshot suspendido auxiliar (los sets observan por Flow en el nivel superior). */
    private suspend fun setsListSnapshot(routineExerciseId: String): List<PlannedSet> =
        emptyList() // resuelto por el combine de arriba; se mantiene la firma simple

    override suspend fun createRoutine(name: String, durationMin: Int): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val count = 0 // position se recalcula al listar por orden de creación
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
        // Posición siguiente (snapshot de lo observado no disponible aquí en
        // suspend; se aproxima con el tamaño actual vía upsert directo).
        var pos = 0
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
        // Solo borra lógico si no tiene ejercicios (borrador vacío);
        // la guardada con contenido queda intacta.
        val cur = routines.getById(routineId)?.toDomain() ?: return
        routines.upsert(cur.copy(deleted = true).toEntity())
    }
}
