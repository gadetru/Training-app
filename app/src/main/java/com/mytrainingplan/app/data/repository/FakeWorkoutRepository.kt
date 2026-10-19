package com.mytrainingplan.app.data.repository

import com.mytrainingplan.app.domain.model.Exercise
import com.mytrainingplan.app.domain.model.HistoryCalcs
import com.mytrainingplan.app.domain.model.PlannedSet
import com.mytrainingplan.app.domain.model.RoutineDurationRules
import com.mytrainingplan.app.domain.model.RoutineExercise
import com.mytrainingplan.app.domain.model.SetEntry
import com.mytrainingplan.app.domain.model.WorkoutSession
import com.mytrainingplan.app.domain.model.timerElapsedSec
import java.time.ZoneId
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

    /**
     * Añade una serie viva calcada de la última (nº siguiente, sin marcar)
     * y su serie planificada en la rutina (doble escritura, spec 016).
     * Reutiliza `SetEntry`/`PlannedSet` y borrado lógico; sin cambio de esquema.
     */
    suspend fun addLiveSet(sessionId: String, routineExerciseId: String)

    /**
     * Borra una serie viva pendiente y su planificada (borrado lógico en
     * ambas, spec 016). Solo pendientes: la hecha se ignora.
     */
    suspend fun deleteLiveSet(sessionId: String, entryId: String)

    /**
      * Sustituye el ejercicio de una rutina en sesión (spec 021): mismo
      * `id`/`position` del slot, nuevo `exerciseId`, y reescribe el
      * `exerciseId` de TODAS las entradas de esa sesión del old al nuevo
      * (hechas incluidas) en la misma transacción. Duplicados permitidos.
      */
    suspend fun replaceExercise(
        sessionId: String,
        routineExerciseId: String,
        newExerciseId: String
    )

    /**
     * Arranca el cronómetro de la sesión (desde 0 o desde el base congelado).
     * `nowMs` = marca de arranque real (para que sobreviva al matar la app).
     */
    suspend fun timerStart(sessionId: String, nowMs: Long)

    /**
     * Congela el cronómetro: acumula el tramo corrido en `timerBaseSec` y
     * desancla `runningSince`.
     */
    suspend fun timerPause(sessionId: String, nowMs: Long)

    /**
     * Re-ancla el reloj sin perder lo ya contado (tick de 15 s kill-safe o
     * reanudar): `timerBaseSec += ahora - runningSince` y `runningSince = nowMs`.
     */
    suspend fun timerSync(sessionId: String, nowMs: Long)

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
    private val exercises: ExerciseRepository = FakeExerciseRepository(),
    // Spec 016 paso 6: espejo del auto-reemplazo (opcional para no romper los
    // tests existentes que no cablean apuntes).
    private val schedule: ScheduleRepository? = null,
    private val zone: ZoneId = ZoneId.systemDefault()
) : WorkoutRepository {

    private val sessions = MutableStateFlow<Map<String, WorkoutSessionDetail>>(emptyMap())

    /** Sesión viva actual (guardia anti-doble: solo una viva por definición). */
    private var liveSessionId: String? = null

    /** Historial de finalizadas por `(routineId, exerciseId)`. */
    private val history = MutableStateFlow<Map<Pair<String, String>, List<SetEntry>>>(emptyMap())

    override fun observeSession(id: String): Flow<WorkoutSessionDetail?> =
        sessions.map { map -> map[id] }

    override fun observeLastEntries(routineId: String, exerciseId: String): Flow<List<SetEntry>> =
        history.map { map -> map[routineId to exerciseId] ?: emptyList() }

    override suspend fun startSession(routineId: String): String {
        // Guardia anti-doble (spec 022): si ya hay una sesión viva, no crear
        // fantasma; devolver la viva.
        liveSessionId?.let { return it }
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
        liveSessionId = sessionId
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

    /**
     * Espejo falso de la doble escritura (spec 016, opción B: vive en el
     * repo, no en el ViewModel). Planificada vía `addSet` + `updateSet`
     * existentes del repo de rutinas (se publica con `saveRoutine` si la
     * rutina ya estaba guardada, como en Room donde no hay borrador);
     * viva calcada de la última (reps, kg, pausa, nota).
     */
    override suspend fun addLiveSet(sessionId: String, routineExerciseId: String) {
        val detail = sessions.value[sessionId] ?: return
        val item = detail.items.find { it.routineExercise.id == routineExerciseId } ?: return
        val last = item.entries.maxByOrNull { it.setNumber } ?: return
        val now = System.currentTimeMillis()
        val nextNumber = (item.entries.maxOfOrNull { it.setNumber } ?: 0) + 1
        val routineId = detail.session.routineId
        val wasSaved = routines.observeRoutines().firstOrNull()?.any { it.id == routineId } == true
        // 1) Planificada: `addSet` copia la 1ª; se calca de la última viva.
        routines.addSet(routineExerciseId)
        val created = routines.observeRoutine(routineId).firstOrNull()
            ?.items?.find { it.routineExercise.id == routineExerciseId }
            ?.sets?.maxByOrNull { it.setNumber }
        if (created != null) {
            routines.updateSet(
                routineExerciseId = routineExerciseId,
                setId = created.id,
                targetReps = last.reps,
                weightKg = last.weightKg,
                restSeconds = last.restSeconds ?: 90,
                loadNote = last.loadNote,
                rir = created.rir
            )
        }
        if (wasSaved) routines.saveRoutine(routineId)
        val plannedId = routines.observeRoutine(routineId).firstOrNull()
            ?.items?.find { it.routineExercise.id == routineExerciseId }
            ?.sets?.maxByOrNull { it.setNumber }?.id
        // 2) Viva: nº siguiente, sin marcar, calcada de la última.
        val entry = SetEntry(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            exerciseId = item.exercise.id,
            plannedSetId = plannedId,
            setNumber = nextNumber,
            reps = last.reps,
            weightKg = last.weightKg,
            loadNote = last.loadNote,
            restSeconds = last.restSeconds,
            done = false,
            updatedAt = now
        )
        sessions.update { map ->
            val cur = map[sessionId] ?: return@update map
            map + (sessionId to cur.copy(
                items = cur.items.map {
                    if (it.routineExercise.id == routineExerciseId) it.copy(entries = it.entries + entry)
                    else it
                }
            ))
        }
    }

    /**
     * Espejo falso del borrado doble (spec 016): solo pendientes (la hecha
     * se ignora). Planificada vía `deleteSet` existente + `saveRoutine` si
     * estaba guardada; viva filtrada (el Room marca `deleted`, el fake
     * filtra como su `discardSession`).
     */
    override suspend fun deleteLiveSet(sessionId: String, entryId: String) {
        val detail = sessions.value[sessionId] ?: return
        val item = detail.items.find { it.entries.any { e -> e.id == entryId } } ?: return
        val entry = item.entries.find { it.id == entryId } ?: return
        if (entry.done) return
        if (entry.plannedSetId != null) {
            routines.deleteSet(item.routineExercise.id, entry.plannedSetId)
            val routineId = detail.session.routineId
            if (routines.observeRoutines().firstOrNull()?.any { it.id == routineId } == true) {
                routines.saveRoutine(routineId)
            }
        }
        sessions.update { map ->
            val cur = map[sessionId] ?: return@update map
            map + (sessionId to cur.copy(
                items = cur.items.map {
                    if (it.routineExercise.id == item.routineExercise.id) {
                        it.copy(entries = it.entries.filterNot { e -> e.id == entryId })
                    } else {
                        it
                    }
                }
            ))
        }
    }

    /**
     * Spec 021: espejo en memoria de la doble escritura. Cambia el ejercicio
     * del slot de rutina y reescribe el exerciseId de todas las entradas de
     * esa sesión del old al nuevo (hechas incluidas). La ficha muestra el
     * nombre nuevo vía `item.exercise`.
     */
    override suspend fun replaceExercise(
        sessionId: String,
        routineExerciseId: String,
        newExerciseId: String
    ) {
        val detail = sessions.value[sessionId] ?: return
        val item = detail.items.find { it.routineExercise.id == routineExerciseId } ?: return
        val oldExerciseId = item.exercise.id
        if (oldExerciseId == newExerciseId) return
        val now = System.currentTimeMillis()
        val newExercise = exercises.getById(newExerciseId) ?: return
        // 1) Slot de rutina: mismo id/position, nuevo exerciseId (persistido).
        routines.updateExercise(routineExerciseId, newExerciseId)
        // 2) Entradas de esta sesión del viejo al nuevo (hechas incluidas).
        sessions.update { map ->
            val cur = map[sessionId] ?: return@update map
            map + (sessionId to cur.copy(
                items = cur.items.map { it ->
                    if (it.routineExercise.id == routineExerciseId) {
                        it.copy(
                            routineExercise = it.routineExercise.copy(
                                exerciseId = newExerciseId,
                                updatedAt = now
                            ),
                            exercise = newExercise,
                            entries = it.entries.map { entry ->
                                if (entry.exerciseId == oldExerciseId) {
                                    entry.copy(exerciseId = newExerciseId, updatedAt = now)
                                } else entry
                            }
                        )
                    } else it
                }
            ))
        }
    }

    override suspend fun timerStart(sessionId: String, nowMs: Long) {
        sessions.update { map ->
            val cur = map[sessionId] ?: return@update map
            val s = cur.session
            if (s.runningSince != null) return@update map
            map + (sessionId to cur.copy(session = s.copy(runningSince = nowMs)))
        }
    }

    override suspend fun timerPause(sessionId: String, nowMs: Long) {
        sessions.update { map ->
            val cur = map[sessionId] ?: return@update map
            val s = cur.session
            if (s.runningSince == null) return@update map
            val acumulado = s.timerBaseSec + ((nowMs - s.runningSince) / 1000).toInt()
            map + (sessionId to cur.copy(
                session = s.copy(timerBaseSec = acumulado.coerceAtLeast(0), runningSince = null)
            ))
        }
    }

    override suspend fun timerSync(sessionId: String, nowMs: Long) {
        sessions.update { map ->
            val cur = map[sessionId] ?: return@update map
            val s = cur.session
            if (s.runningSince == null) return@update map
            val acumulado = s.timerBaseSec + ((nowMs - s.runningSince) / 1000).toInt()
            map + (sessionId to cur.copy(
                session = s.copy(timerBaseSec = acumulado.coerceAtLeast(0), runningSince = nowMs)
            ))
        }
    }

    override suspend fun finishSession(sessionId: String) {
        val detail = sessions.value[sessionId] ?: return
        val now = System.currentTimeMillis()
        // Spec 023: si la rutina es AUTO, adopta el tiempo real medido (mín. 1).
        val elapsed = detail.session.timerElapsedSec(now)
        val routineDetail = routines.observeRoutine(detail.session.routineId).firstOrNull()
        if (routineDetail != null && !routineDetail.routine.durationManual) {
            val realMin = RoutineDurationRules.fromRealSeconds(elapsed)
            routines.updateMeta(detail.session.routineId, routineDetail.routine.name, realMin, false)
        }
        // Spec 016 paso 6 (espejo): lo hecho manda sobre lo apuntado.
        if (schedule != null) {
            schedule.setScheduled(
                HistoryCalcs.dateKeyOf(detail.session.startedAt, zone),
                detail.session.routineId
            )
        }
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
        if (liveSessionId == sessionId) liveSessionId = null
    }

    override suspend fun discardSession(sessionId: String) {
        sessions.update { it - sessionId }
        if (liveSessionId == sessionId) liveSessionId = null
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
