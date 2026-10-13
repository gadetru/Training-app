package com.mytrainingplan.app.data.repository

import com.mytrainingplan.app.data.local.dao.RoutineDao
import com.mytrainingplan.app.data.local.dao.ScheduledDayDao
import com.mytrainingplan.app.data.local.dao.WorkoutSessionDao
import com.mytrainingplan.app.data.local.entity.ScheduledDayEntity
import com.mytrainingplan.app.data.mapper.toDomain
import com.mytrainingplan.app.data.mapper.toEntity
import com.mytrainingplan.app.domain.model.HistoryCalcs
import com.mytrainingplan.app.domain.model.MonthDay
import com.mytrainingplan.app.domain.model.ScheduledDay
import com.mytrainingplan.app.domain.model.WeeklyProgress
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Repo real de apuntes + historial (Paso 5 spec 015).
 * Apuntes en `scheduled_days` (lógico); el mes combina apunte, finalizadas
 * y nombres de rutinas. Los apuntes de rutinas borradas se ocultan; el
 * historial de borradas sobrevive (nombres con todas, incluidas borradas).
 */
class RoomScheduleRepository(
    private val scheduled: ScheduledDayDao,
    private val sessions: WorkoutSessionDao,
    private val routines: RoutineDao,
    private val zone: ZoneId = ZoneId.systemDefault()
) : ScheduleRepository {

    override fun observeScheduledBetween(fromDate: String, toDate: String): Flow<List<ScheduledDay>> =
        scheduled.observeBetween(fromDate, toDate).map { list -> list.map { it.toDomain() } }

    override fun observeScheduledByDate(date: String): Flow<ScheduledDay?> =
        scheduled.observeByDate(date).map { it?.toDomain() }

    override suspend fun setScheduled(date: String, routineId: String) {
        val now = System.currentTimeMillis()
        val existing = scheduled.getByDate(date)
        if (existing != null) {
            if (existing.routineId == routineId) return
            scheduled.upsert(existing.copy(routineId = routineId, updatedAt = now))
        } else {
            scheduled.upsert(
                ScheduledDayEntity(
                    id = UUID.randomUUID().toString(),
                    routineId = routineId,
                    date = date,
                    updatedAt = now
                )
            )
        }
    }

    override suspend fun clearScheduled(date: String) {
        val existing = scheduled.getByDate(date) ?: return
        scheduled.upsert(existing.copy(deleted = true, updatedAt = System.currentTimeMillis()))
    }

    override fun observeMonthDays(year: Int, month: Int): Flow<List<MonthDay>> {
        val fromDate = HistoryCalcs.monthFirstKey(year, month)
        val toDate = HistoryCalcs.nextMonthFirstKey(year, month)
        val (fromMillis, toMillis) = HistoryCalcs.monthBoundsMillis(year, month, zone)
        val todayKey = HistoryCalcs.todayKey(zone)
        return combine(
            scheduled.observeBetween(fromDate, toDate),
            sessions.observeFinished(),
            routines.observeAllIncludingDeleted()
        ) { sched, finished, allRoutines ->
            val activeNames = allRoutines.filter { !it.deleted }.associate { it.id to it.name }
            val anyNames = allRoutines.associate { it.id to it.name }
            val schedMap = sched.mapNotNull { row ->
                val name = activeNames[row.routineId] ?: return@mapNotNull null
                row.date to (row.routineId to name as String?)
            }.toMap()
            val doneMap = finished
                .filter { it.startedAt >= fromMillis && it.startedAt < toMillis }
                .groupBy { HistoryCalcs.dateKeyOf(it.startedAt, zone) }
                .mapValues { (_, list) ->
                    val latest = list.maxByOrNull { it.startedAt }!!
                    latest.routineId to (anyNames[latest.routineId] ?: "(eliminada)")
                }
            HistoryCalcs.resolveMonthDays(year, month, todayKey, schedMap, doneMap)
        }
    }

    override fun observeProgress(): Flow<WeeklyProgress> =
        sessions.observeFinished().map { list ->
            HistoryCalcs.buildProgress(list.map { it.toDomain() }, System.currentTimeMillis(), zone)
        }

    override fun observeLastDoneAt(routineId: String): Flow<Long?> =
        sessions.observeLastFinishedByRoutine(routineId).map { it?.startedAt }
}
