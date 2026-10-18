package com.mytrainingplan.app.data.repository

import com.mytrainingplan.app.data.local.dao.ScheduledDayDao
import com.mytrainingplan.app.data.local.dao.WorkoutSessionDao
import com.mytrainingplan.app.data.mapper.toDomain
import com.mytrainingplan.app.domain.model.AccentColor
import com.mytrainingplan.app.domain.model.FootKind
import com.mytrainingplan.app.domain.model.HistoryCalcs
import com.mytrainingplan.app.domain.model.HomeUiState
import com.mytrainingplan.app.domain.model.RoutineMuscleLabels
import com.mytrainingplan.app.domain.model.RoutineSummary
import com.mytrainingplan.app.domain.model.timerElapsedSec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.ZoneId

/**
 * Repo real de Home (Paso 10 spec 006, cierra los "4 reales"; progreso real
 * Paso 5 spec 015: etiquetas Hoy/Ayer/Hace X y semana desde finalizadas).
 * Feed desde rutinas guardadas; saludo desde el perfil real.
 */
class RoomHomeRepository(
    private val routines: RoutineRepository,
    private val profile: ProfileRepository,
    private val sessions: WorkoutSessionDao,
    private val scheduleDao: ScheduledDayDao,
    private val zone: ZoneId = ZoneId.systemDefault()
) : HomeRepository {

    override fun observeHome(): Flow<HomeUiState> =
        combine(
            routines.observeDetails(),
            profile.observeProfile(),
            sessions.observeFinished(),
            sessions.observeLive(),
            scheduleDao.observeAll()
        ) { details, prof, finished, live, scheduledDays ->
            val now = System.currentTimeMillis()
            val todayKey = HistoryCalcs.dateKeyOf(now, zone)
            val lastByRoutine = finished.groupBy { it.routineId }
                .mapValues { (_, list) -> list.maxOf { it.startedAt } }
            val domainSessions = finished.map { it.toDomain() }
            val accents = listOf(AccentColor.ORANGE, AccentColor.VOLT, AccentColor.CYAN)
            // Spec 022 §50: sesión viva (más reciente manda) + su contador.
            val liveEntity = live.firstOrNull()
            val liveSessionId = liveEntity?.id
            val liveRoutineId = liveEntity?.routineId
            val liveElapsedSec = liveEntity?.let { it.toDomain().timerElapsedSec(now) } ?: 0
            // Spec 022 §50: primera fila programada para hoy.
            val scheduledTodayId = scheduledDays.firstOrNull { it.date == todayKey }?.routineId
            HomeUiState(
                firstName = prof?.firstName ?: "Atleta",
                routines = details.mapIndexed { i, d ->
                    RoutineSummary(
                        id = d.routine.id,
                        title = d.routine.name.ifBlank { "Rutina ${i + 1}" },
                        tags = RoutineMuscleLabels.tagsFor(d.items.map { it.exercise }),
                        durationMin = d.routine.durationMin,
                        exerciseCount = d.items.size,
                        lastDoneLabel = lastByRoutine[d.routine.id]?.let {
                            HistoryCalcs.lastDoneLabel(it, now)
                        },
                        accent = accents[i % accents.size],
                        footNote = "",
                        footKind = FootKind.INFO,
                        updatedAt = d.routine.updatedAt,
                        deleted = d.routine.deleted
                    )
                },
                progress = HistoryCalcs.buildProgress(domainSessions, now),
                liveRoutineId = liveRoutineId,
                liveElapsedSec = liveElapsedSec,
                scheduledTodayId = scheduledTodayId,
                liveSessionId = liveSessionId
            )
        }

    override suspend fun getHome(): HomeUiState = observeHome().first()
}
