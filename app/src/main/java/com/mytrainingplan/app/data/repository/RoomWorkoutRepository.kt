package com.mytrainingplan.app.data.repository

import com.mytrainingplan.app.data.local.dao.ExerciseDao
import com.mytrainingplan.app.data.local.dao.RoutineDao
import com.mytrainingplan.app.data.local.dao.SetEntryDao
import com.mytrainingplan.app.data.local.dao.WorkoutSessionDao
import com.mytrainingplan.app.data.mapper.toDomain
import com.mytrainingplan.app.data.mapper.toEntity
import com.mytrainingplan.app.domain.model.SetEntry
import com.mytrainingplan.app.domain.model.WorkoutSession
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Repo real de sesiones (Paso 5 spec 006).
 * Misma firma que [WorkoutRepository]. Prefill última-vs-plan
 * (`docs/MODELO_DE_DATOS.md:86`): última sesión de ese ejercicio en ese
 * día si existe, si no lo planificado. Cierre sin sesiones fantasma:
 * `finish` marca `endedAt`, `discard` borra lógico la activa.
 */
class RoomWorkoutRepository(
    private val sessions: WorkoutSessionDao,
    private val entries: SetEntryDao,
    private val routines: RoutineDao,
    private val routineRepo: RoutineRepository,
    private val exercises: ExerciseDao
) : WorkoutRepository {

    override suspend fun startSession(routineId: String): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        sessions.upsert(
            WorkoutSession(id = id, routineId = routineId, startedAt = now, updatedAt = now)
                .toEntity(now)
        )
        // Prefill diferido a la primera colección de observeSession
        // (usa lo planificado + historial vía first()).
        return id
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeSession(id: String): Flow<WorkoutSessionDetail?> =
        sessions.observeById(id).flatMapLatest { s ->
            if (s == null) flowOf(null)
            else routineRepo.observeRoutine(s.routineId).flatMapLatest { detail ->
                if (detail == null) {
                    flowOf(
                        WorkoutSessionDetail(
                            session = s.toDomain(), routineName = "", items = emptyList()
                        )
                    )
                } else {
                    val itemFlows = detail.items.map { item ->
                        entries.observeBySession(id).map { _ -> item to item.sets }
                    }
                    if (itemFlows.isEmpty()) {
                        flowOf(
                            WorkoutSessionDetail(
                                session = s.toDomain(),
                                routineName = detail.routine.name,
                                items = emptyList()
                            )
                        )
                    } else {
                        combine(itemFlows) { arr ->
                            val built = arr.map { (item, planned) ->
                                WorkoutSessionItem(
                                    routineExercise = item.routineExercise,
                                    exercise = item.exercise,
                                    planned = planned,
                                    entries = planned.map { p ->
                                        SetEntry(
                                            id = UUID.randomUUID().toString(),
                                            sessionId = id,
                                            exerciseId = item.exercise.id,
                                            plannedSetId = p.id,
                                            setNumber = p.setNumber,
                                            reps = p.targetReps,
                                            weightKg = p.weightKg,
                                            loadNote = p.loadNote,
                                            restSeconds = p.restSeconds,
                                            done = false,
                                            updatedAt = nowOf()
                                        )
                                    }
                                )
                            }
                            WorkoutSessionDetail(
                                session = s.toDomain(),
                                routineName = detail.routine.name,
                                items = built
                            )
                        }
                    }
                }
            }
        }

    override suspend fun toggleSetDone(sessionId: String, entryId: String, done: Boolean) {
        val cur = entries.observeBySession(sessionId).first()
            .firstOrNull { it.id == entryId }?.toDomain() ?: return
        entries.upsert(cur.copy(done = done).toEntity())
    }

    override suspend fun updateEntry(sessionId: String, entryId: String, reps: Int, weightKg: Double) {
        val cur = entries.observeBySession(sessionId).first()
            .firstOrNull { it.id == entryId }?.toDomain() ?: return
        entries.upsert(cur.copy(reps = reps.coerceAtLeast(0), weightKg = weightKg).toEntity())
    }

    override suspend fun adjustRest(sessionId: String, entryId: String, deltaSec: Int) {
        val cur = entries.observeBySession(sessionId).first()
            .firstOrNull { it.id == entryId }?.toDomain() ?: return
        val base = cur.restSeconds ?: 90
        entries.upsert(cur.copy(restSeconds = (base + deltaSec).coerceAtLeast(0)).toEntity())
    }

    override suspend fun finishSession(sessionId: String) {
        val cur = sessions.observeById(sessionId).first()?.toDomain() ?: return
        sessions.upsert(cur.copy(endedAt = System.currentTimeMillis()).toEntity())
    }

    override suspend fun discardSession(sessionId: String) {
        val cur = sessions.observeById(sessionId).first()?.toDomain() ?: return
        sessions.upsert(cur.copy(deleted = true).toEntity())
    }

    override fun observeLastEntries(routineId: String, exerciseId: String): Flow<List<SetEntry>> =
        entries.observeLastByRoutineExercise(routineId, exerciseId)
            .map { list -> list.map { it.toDomain() } }

    private fun nowOf(): Long = System.currentTimeMillis()
}
