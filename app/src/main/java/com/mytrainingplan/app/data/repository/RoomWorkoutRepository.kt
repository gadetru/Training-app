package com.mytrainingplan.app.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.mytrainingplan.app.data.local.AppDatabase
import com.mytrainingplan.app.data.local.dao.ExerciseDao
import com.mytrainingplan.app.data.local.dao.PlannedSetDao
import com.mytrainingplan.app.data.local.dao.RoutineDao
import com.mytrainingplan.app.data.local.dao.RoutineExerciseDao
import com.mytrainingplan.app.data.local.dao.SetEntryDao
import com.mytrainingplan.app.data.local.dao.WorkoutSessionDao
import com.mytrainingplan.app.data.mapper.toDomain
import com.mytrainingplan.app.data.mapper.toEntity
import com.mytrainingplan.app.domain.model.HistoryCalcs
import com.mytrainingplan.app.domain.model.PlannedSet
import com.mytrainingplan.app.domain.model.RoutineDurationRules
import com.mytrainingplan.app.domain.model.SetEntry
import com.mytrainingplan.app.domain.model.WorkoutSession
import com.mytrainingplan.app.domain.model.timerElapsedSec
import java.time.ZoneId
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
    private val exercises: ExerciseDao,
    // Spec 016 paso 2: la doble escritura (viva + planificada) necesita la
    // DB para la transacción y los DAOs de ambas tablas.
    private val db: AppDatabase,
    private val routineExercises: RoutineExerciseDao,
    private val plannedSets: PlannedSetDao,
    // Spec 016 paso 6: el auto-reemplazo del apunte al finalizar necesita el
    // repo de apuntes (sin ciclo: ese repo solo depende de DAOs). La zona es
    // la del sistema por defecto (día local `yyyy-MM-dd`, como el spec 015).
    private val schedule: ScheduleRepository,
    private val zone: ZoneId = ZoneId.systemDefault()
) : WorkoutRepository {

    /**
     * Crea la sesión y persiste UNA vez la copia de series con prefill
     * última-vs-plan (spec 009): última sesión finalizada de esa rutina si
     * existe, si no lo planificado. Con rutina inexistente deja la sesión
     * vacía sin crash (la UI permite volver).
     */
    override suspend fun startSession(routineId: String): String {
        // Guardia anti-doble (spec 022 §45): si ya hay una sesión viva,
        // no crear fantasma; devolver la viva + Log (comportamiento de retomar).
        val viva = sessions.observeLive().first().firstOrNull()
        if (viva != null) {
            Log.w(TAG, "startSession: ya hay sesión viva sessionId=${viva.id} routineId=${viva.routineId}; se retorna la viva")
            return viva.id
        }
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

    /**
     * Doble escritura atómica (spec 016, opción B): serie viva (nº siguiente,
     * sin marcar, calcada de la última: reps, kg, pausa, nota) + serie
     * planificada en la rutina con los mismos valores. `withTransaction`
     * (Context7 `/websites/developer_android_training_data-storage_room`):
     * jamás viva sin planificada ni al revés. `@Upsert`, borrado lógico,
     * sin cambio de esquema (DB v3).
     */
    override suspend fun addLiveSet(sessionId: String, routineExerciseId: String) {
        if (sessions.observeById(sessionId).first() == null) {
            Log.w(TAG, "addLiveSet: sesión inexistente sessionId=$sessionId")
            return
        }
        val re = routineExercises.getById(routineExerciseId)?.toDomain()
        if (re == null) {
            Log.w(TAG, "addLiveSet: ejercicio inexistente routineExerciseId=$routineExerciseId")
            return
        }
        val now = System.currentTimeMillis()
        val live = entries.observeBySession(sessionId).first()
            .map { it.toDomain() }.filter { it.exerciseId == re.exerciseId }
        val planned = plannedSets.observeByExercise(routineExerciseId).first()
            .map { it.toDomain() }
        val lastLive = live.maxByOrNull { it.setNumber }
        val lastPlanned = planned.maxByOrNull { it.setNumber }
        val nextNumber = maxOf(
            live.maxOfOrNull { it.setNumber } ?: 0,
            planned.maxOfOrNull { it.setNumber } ?: 0
        ) + 1
        val reps = lastLive?.reps ?: lastPlanned?.targetReps ?: 0
        val weight = lastLive?.weightKg ?: lastPlanned?.weightKg ?: 0.0
        val rest = lastLive?.restSeconds ?: lastPlanned?.restSeconds ?: 90
        val note = lastLive?.loadNote ?: lastPlanned?.loadNote
        val plannedId = UUID.randomUUID().toString()
        db.withTransaction {
            plannedSets.upsert(
                PlannedSet(
                    id = plannedId,
                    routineExerciseId = routineExerciseId,
                    setNumber = nextNumber,
                    targetReps = reps,
                    weightKg = weight,
                    restSeconds = rest,
                    loadNote = note
                ).toEntity(now)
            )
            entries.upsert(
                SetEntry(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    exerciseId = re.exerciseId,
                    plannedSetId = plannedId,
                    setNumber = nextNumber,
                    reps = reps,
                    weightKg = weight,
                    loadNote = note,
                    restSeconds = rest,
                    done = false,
                    updatedAt = now
                ).toEntity(now)
            )
        }
        Log.i(TAG, "addLiveSet: sessionId=$sessionId routineExerciseId=$routineExerciseId setNumber=$nextNumber")
    }

    /**
     * Borrado doble atómico (spec 016): solo pendientes (la hecha se ignora
     * con `Log`). Marca `deleted` en la viva y en su planificada dentro de
     * la misma transacción.
     */
    override suspend fun deleteLiveSet(sessionId: String, entryId: String) {
        val cur = entries.observeBySession(sessionId).first()
            .firstOrNull { it.id == entryId }?.toDomain()
        if (cur == null) {
            Log.w(TAG, "deleteLiveSet: entrada inexistente sessionId=$sessionId entryId=$entryId")
            return
        }
        if (cur.done) {
            Log.w(TAG, "deleteLiveSet: serie ya hecha, no se borra entryId=$entryId")
            return
        }
        val now = System.currentTimeMillis()
        val planned = cur.plannedSetId?.let { plannedSets.getById(it)?.toDomain() }
        if (cur.plannedSetId != null && planned == null) {
            Log.w(TAG, "deleteLiveSet: planificada inexistente plannedSetId=${cur.plannedSetId}")
        }
        db.withTransaction {
            entries.upsert(cur.copy(deleted = true).toEntity(now))
            if (planned != null) plannedSets.upsert(planned.copy(deleted = true).toEntity(now))
        }
        Log.i(TAG, "deleteLiveSet: sessionId=$sessionId entryId=$entryId")
    }

    /**
     * Spec 021: sustituye el ejercicio del slot de rutina y reescribe el
     * `exerciseId` de TODAS las entradas de esa sesión del old al nuevo
     * (hechas incluidas) en la misma transacción, para que la sesión viva
     * no pierda la agrupación por `exerciseId`. Sin migraciones: solo
     * `@Upsert` sobre tablas existentes (DB v3).
     */
    override suspend fun replaceExercise(
        sessionId: String,
        routineExerciseId: String,
        newExerciseId: String
    ) {
        if (sessions.observeById(sessionId).first() == null) {
            Log.w(TAG, "replaceExercise: sesión inexistente sessionId=$sessionId")
            return
        }
        val re = routineExercises.getById(routineExerciseId)?.toDomain()
        if (re == null) {
            Log.w(TAG, "replaceExercise: ejercicio inexistente routineExerciseId=$routineExerciseId")
            return
        }
        val oldExerciseId = re.exerciseId
        if (oldExerciseId == newExerciseId) return
        val now = System.currentTimeMillis()
        db.withTransaction {
            // 1) Slot de rutina: mismo id/position, nuevo exerciseId.
            routineExercises.upsert(
                re.copy(exerciseId = newExerciseId, updatedAt = now).toEntity(now)
            )
            // 2) Entradas de esta sesión con el viejo exerciseId → nuevo.
            entries.observeBySession(sessionId).first()
                .filter { it.exerciseId == oldExerciseId }
                .map { it.toDomain().copy(exerciseId = newExerciseId, updatedAt = now) }
                .forEach { entries.upsert(it.toEntity(now)) }
        }
        Log.i(
            TAG,
            "replaceExercise: sessionId=$sessionId routineExerciseId=$routineExerciseId " +
                "$oldExerciseId -> $newExerciseId"
        )
    }

    override suspend fun finishSession(sessionId: String) {
        val cur = sessions.observeById(sessionId).first()?.toDomain()
        if (cur == null) {
            Log.w(TAG, "finishSession: sesión inexistente sessionId=$sessionId")
            return
        }
        val now = System.currentTimeMillis()
        val elapsed = cur.timerElapsedSec(now)
        db.withTransaction {
            sessions.upsert(cur.copy(endedAt = now).toEntity(now))
            // Spec 023: si la rutina es AUTO, adopta el tiempo real medido (mín. 1).
            val routine = routines.getById(cur.routineId)?.toDomain()
            if (routine != null && !routine.durationManual) {
                val realMin = RoutineDurationRules.fromRealSeconds(elapsed)
                routines.upsert(routine.copy(durationMin = realMin).toEntity())
            }
        }
        // Spec 016 paso 6: lo hecho manda sobre lo apuntado. El apunte del
        // día (fecha local del inicio, como el spec 015) pasa a ser la rutina
        // realizada; `setScheduled` sustituye en la misma fila, sin duplicar.
        schedule.setScheduled(HistoryCalcs.dateKeyOf(cur.startedAt, zone), cur.routineId)
        Log.i(TAG, "finishSession: sessionId=$sessionId elapsedSec=$elapsed")
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

    override suspend fun timerStart(sessionId: String, nowMs: Long) {
        val cur = sessions.observeById(sessionId).first()?.toDomain()
        if (cur == null) {
            Log.w(TAG, "timerStart: sesión inexistente sessionId=$sessionId")
            return
        }
        if (cur.runningSince != null) {
            Log.w(TAG, "timerStart: ya está corriendo sessionId=$sessionId")
            return
        }
        sessions.upsert(cur.copy(runningSince = nowMs).toEntity(nowMs))
        Log.i(TAG, "timerStart: sessionId=$sessionId runningSince=$nowMs")
    }

    override suspend fun timerPause(sessionId: String, nowMs: Long) {
        val cur = sessions.observeById(sessionId).first()?.toDomain()
        if (cur == null) {
            Log.w(TAG, "timerPause: sesión inexistente sessionId=$sessionId")
            return
        }
        if (cur.runningSince == null) {
            Log.w(TAG, "timerPause: no está corriendo sessionId=$sessionId")
            return
        }
        val acumulado = cur.timerBaseSec + ((nowMs - cur.runningSince) / 1000).toInt()
        sessions.upsert(
            cur.copy(timerBaseSec = acumulado.coerceAtLeast(0), runningSince = null).toEntity(nowMs)
        )
        Log.i(TAG, "timerPause: sessionId=$sessionId timerBaseSec=$acumulado")
    }

    override suspend fun timerSync(sessionId: String, nowMs: Long) {
        val cur = sessions.observeById(sessionId).first()?.toDomain()
        if (cur == null) {
            Log.w(TAG, "timerSync: sesión inexistente sessionId=$sessionId")
            return
        }
        if (cur.runningSince == null) {
            Log.w(TAG, "timerSync: no está corriendo sessionId=$sessionId")
            return
        }
        val acumulado = cur.timerBaseSec + ((nowMs - cur.runningSince) / 1000).toInt()
        sessions.upsert(
            cur.copy(timerBaseSec = acumulado.coerceAtLeast(0), runningSince = nowMs).toEntity(nowMs)
        )
        Log.i(TAG, "timerSync: sessionId=$sessionId timerBaseSec=$acumulado runningSince=$nowMs")
    }

    override fun observeLastEntries(routineId: String, exerciseId: String): Flow<List<SetEntry>> =
        entries.observeLastByRoutineExercise(routineId, exerciseId)
            .map { list -> list.map { it.toDomain() } }

    companion object {
        /** Tag de diagnóstico interno (spec 009: log sin UI de error). */
        private const val TAG = "WorkoutSession"
    }
}
