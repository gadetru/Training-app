package com.mytrainingplan.app.data.repository

import com.mytrainingplan.app.domain.model.HistoryCalcs
import com.mytrainingplan.app.domain.model.MonthDay
import com.mytrainingplan.app.domain.model.ScheduledDay
import com.mytrainingplan.app.domain.model.WeeklyProgress
import com.mytrainingplan.app.domain.model.WorkoutSession
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Espejo falso de [ScheduleRepository] (Paso 5 spec 015, para tests JVM).
 * Sin Room, sin Hilt. Apuntes por fecha (cambiar no duplica, quitar es
 * lógico) + finalizadas sembradas para el mes, la racha y la semana.
 *
 * Sin DI: los tests lo construyen con su [RoutineRepository] propio.
 */
class FakeScheduleRepository(
    private val routines: RoutineRepository,
    initialScheduled: List<ScheduledDay> = emptyList(),
    initialFinished: List<WorkoutSession> = emptyList(),
    private val zone: ZoneId = ZoneId.systemDefault()
) : ScheduleRepository {

    private val sched = MutableStateFlow(initialScheduled.associateBy { it.date })
    private val finished = MutableStateFlow(initialFinished)

    override fun observeScheduledBetween(fromDate: String, toDate: String): Flow<List<ScheduledDay>> =
        sched.map { map ->
            map.values.filter { !it.deleted && it.date >= fromDate && it.date < toDate }
                .sortedBy { it.date }
        }

    override fun observeScheduledByDate(date: String): Flow<ScheduledDay?> =
        sched.map { map -> map[date]?.takeIf { !it.deleted } }

    override suspend fun setScheduled(date: String, routineId: String) {
        val now = System.currentTimeMillis()
        sched.update { map ->
            val existing = map[date]
            if (existing != null && !existing.deleted && existing.routineId == routineId) return@update map
            if (existing != null) {
                map + (date to existing.copy(routineId = routineId, updatedAt = now, deleted = false))
            } else {
                map + (date to ScheduledDay(
                    id = UUID.randomUUID().toString(),
                    routineId = routineId,
                    date = date,
                    updatedAt = now
                ))
            }
        }
    }

    override suspend fun clearScheduled(date: String) {
        sched.update { map ->
            val existing = map[date] ?: return@update map
            map + (date to existing.copy(deleted = true, updatedAt = System.currentTimeMillis()))
        }
    }

    override fun observeMonthDays(year: Int, month: Int): Flow<List<MonthDay>> {
        val fromDate = HistoryCalcs.monthFirstKey(year, month)
        val toDate = HistoryCalcs.nextMonthFirstKey(year, month)
        val (fromMillis, toMillis) = HistoryCalcs.monthBoundsMillis(year, month, zone)
        val todayKey = HistoryCalcs.todayKey(zone)
        return combine(
            observeScheduledBetween(fromDate, toDate),
            finished,
            routines.observeDetails()
        ) { schedList, done, details ->
            val activeNames = details.associate { it.routine.id to it.routine.name }
            val schedMap = schedList.mapNotNull { row ->
                val name = activeNames[row.routineId] ?: return@mapNotNull null
                row.date to (row.routineId to name as String?)
            }.toMap()
            val doneMap = done
                .filter { it.endedAt != null && !it.deleted && it.startedAt >= fromMillis && it.startedAt < toMillis }
                .groupBy { HistoryCalcs.dateKeyOf(it.startedAt, zone) }
                .mapValues { (_, list) ->
                    val latest = list.maxByOrNull { it.startedAt }!!
                    latest.routineId to (activeNames[latest.routineId] ?: "(eliminada)")
                }
            HistoryCalcs.resolveMonthDays(year, month, todayKey, schedMap, doneMap)
        }
    }

    override fun observeProgress(): Flow<WeeklyProgress> =
        finished.map { list -> HistoryCalcs.buildProgress(list, System.currentTimeMillis(), zone) }

    override fun observeLastDoneAt(routineId: String): Flow<Long?> =
        finished.map { list ->
            list.filter { it.routineId == routineId && it.endedAt != null && !it.deleted }
                .maxOfOrNull { it.startedAt }
        }

    /**
     * Siembra una sesión finalizada (espejo de `finishSession` del Room:
     * solo para tests).
     */
    suspend fun addFinished(session: WorkoutSession) {
        finished.update { it + session }
    }
}
