package com.mytrainingplan.app.data.repository

import android.util.Log
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Repo real de sesiones (Paso 5 spec 006; sesión persistida spec 009).
 * Misma firma que [WorkoutRepository]. Prefill última-vs-plan
 * (`docs/MODELO_DE_DATOS.md:86`): última sesión finalizada de esa rutina
 * si existe, si no lo planificado. Las entradas se persisten UNA vez al
 * iniciar (IDs estables); `observeSession` lee DB. Cierre sin sesiones
 * fantasma: `finish` marca `endedAt`, `discard` borra lógico la activa.
 * Diagnóstico interno con `Log` (sin UI de error).
 */
class RoomWorkoutRepository(
    private val sessions: WorkoutSessionDao,
    private val entries: SetEntryDao,
    private val routines: RoutineDao,
    private val routineRepo: RoutineRepository,
    private val exercises: ExerciseDao
) : WorkoutRepository {

    /**
     * Crea la sesión y persiste UNA vez la copia de series con prefill
     * última-vs-plan (spec 009): última sesión finalizada de esa rutina si
     * existe, si no lo planificado. Con rutina inexistente deja la sesión
     * vacía sin crash (la UI permite volver).
     */
    override suspend fun startSession(routineId: String): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        sessions.upsert(
            WorkoutSession(id = id, routineId = routineId, startedAt = now, updatedAt = now)
                .toEntity(now)
        )
        val detail = routineRepo.observeRoutine(routineId).first()
        if (detail == null) {
            Log.w(TAG, "startSession: rutina inexistente routineId=$routineId sessionId=$id")
            return id
        }
        // Última finalizada de esa rutina (las descartadas no cuentan:
        // `observeByRoutine` ya filtra `deleted = 0`).
        val lastId = sessions.observeByRoutine(routineId).first()
            .firstOrNull { it.id != id && it.endedAt != null }?.id
        val lastByExercise: Map<String, List<SetEntry>> = if (lastId == null) {
            emptyMap()
        } else {
            entries.observeBySession(lastId).first()
                .map { it.toDomain() }.groupBy { it.exerciseId }
        }
        val rows = detail.items.flatMap { item ->
            val last = lastByExercise[item.exercise.id] ?: emptyList()
            item.sets.sortedBy { it.setNumber }.map { planned ->
                val prev = last.find { it.plannedSetId == planned.id }
                    ?: last.find { it.setNumber == planned.setNumber }
                SetEntry(
                    id = UUID.randomUUID().toString(),
                    sessionId = id,
                    exerciseId = item.exercise.id,
                    plannedSetId = planned.id,
                    setNumber = planned.setNumber,
                    reps = prev?.reps ?: planned.targetReps,
                    weightKg = prev?.weightKg ?: planned.weightKg,
                    loadNote = prev?.loadNote ?: planned.loadNote,
                    restSeconds = prev?.restSeconds ?: planned.restSeconds,
                    done = false,
                    updatedAt = now
                ).toEntity(now)
            }
        }
        if (rows.isNotEmpty()) entries.upsertAll(rows)
        Log.i(TAG, "startSession: sessionId=$id routineId=$routineId entries=${rows.size}")
        return id
    }

    /**
     * Sesión leída desde DB con IDs estables (spec 009): la copia
     * persistida al iniciar; `toggle/update/adjust` la actualizan.
     * Sesión inexistente o descartada (`deleted = 0` en el DAO) = null.
     */
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
                    entries.observeBySession(id).map { rows ->
                        val byExercise = rows.map { it.toDomain() }.groupBy { it.exerciseId }
                        WorkoutSessionDetail(
                            session = s.toDomain(),
                            routineName = detail.routine.name,
                            items = detail.items.map { item ->
                                WorkoutSessionItem(
                                    routineExercise = item.routineExercise,
                                    exercise = item.exercise,
                                    planned = item.sets.sortedBy { it.setNumber },
                                    entries = (byExercise[item.exercise.id] ?: emptyList())
                                        .sortedBy { it.setNumber }
                                )
                            }
                        )
                    }
                }
            }
        }

    override suspend fun toggleSetDone(sessionId: String, entryId: String, done: Boolean) {
        val cur = entries.observeBySession(sessionId).first()
            .firstOrNull { it.id == entryId }?.toDomain()
        if (cur == null) {
            Log.w(TAG, "toggleSetDone: entrada inexistente sessionId=$sessionId entryId=$entryId")
            return
        }
        entries.upsert(cur.copy(done = done).toEntity())
    }

    override suspend fun updateEntry(sessionId: String, entryId: String, reps: Int, weightKg: Double) {
        val cur = entries.observeBySession(sessionId).first()
            .firstOrNull { it.id == entryId }?.toDomain()
        if (cur == null) {
            Log.w(TAG, "updateEntry: entrada inexistente sessionId=$sessionId entryId=$entryId")
            return
        }
        entries.upsert(cur.copy(reps = reps.coerceAtLeast(0), weightKg = weightKg).toEntity())
    }

    override suspend fun adjustRest(sessionId: String, entryId: String, deltaSec: Int) {
        val cur = entries.observeBySession(sessionId).first()
            .firstOrNull { it.id == entryId }?.toDomain()
        if (cur == null) {
            Log.w(TAG, "adjustRest: entrada inexistente sessionId=$sessionId entryId=$entryId")
            return
        }
        val base = cur.restSeconds ?: 90
        entries.upsert(cur.copy(restSeconds = (base + deltaSec).coerceAtLeast(0)).toEntity())
    }

    override suspend fun finishSession(sessionId: String) {
        val cur = sessions.observeById(sessionId).first()?.toDomain()
        if (cur == null) {
            Log.w(TAG, "finishSession: sesión inexistente sessionId=$sessionId")
            return
        }
        sessions.upsert(cur.copy(endedAt = System.currentTimeMillis()).toEntity())
        Log.i(TAG, "finishSession: sessionId=$sessionId")
    }

    override suspend fun discardSession(sessionId: String) {
        val cur = sessions.observeById(sessionId).first()?.toDomain()
        if (cur == null) {
            Log.w(TAG, "discardSession: sesión inexistente sessionId=$sessionId")
            return
        }
        sessions.upsert(cur.copy(deleted = true).toEntity())
        Log.i(TAG, "discardSession: sessionId=$sessionId")
    }

    override fun observeLastEntries(routineId: String, exerciseId: String): Flow<List<SetEntry>> =
        entries.observeLastByRoutineExercise(routineId, exerciseId)
            .map { list -> list.map { it.toDomain() } }

    companion object {
        /** Tag de diagnóstico interno (spec 009: log sin UI de error). */
        private const val TAG = "WorkoutSession"
    }
}
