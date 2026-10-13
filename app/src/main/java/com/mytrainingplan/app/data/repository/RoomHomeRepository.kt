package com.mytrainingplan.app.data.repository

import com.mytrainingplan.app.data.local.dao.WorkoutSessionDao
import com.mytrainingplan.app.data.mapper.toDomain
import com.mytrainingplan.app.domain.model.AccentColor
import com.mytrainingplan.app.domain.model.FootKind
import com.mytrainingplan.app.domain.model.HistoryCalcs
import com.mytrainingplan.app.domain.model.HomeUiState
import com.mytrainingplan.app.domain.model.RoutineMuscleLabels
import com.mytrainingplan.app.domain.model.RoutineSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * Repo real de Home (Paso 10 spec 006, cierra los "4 reales"; progreso real
 * Paso 5 spec 015: etiquetas Hoy/Ayer/Hace X y semana desde finalizadas).
 * Feed desde rutinas guardadas; saludo desde el perfil real.
 */
class RoomHomeRepository(
    private val routines: RoutineRepository,
    private val profile: ProfileRepository,
    private val sessions: WorkoutSessionDao
) : HomeRepository {

    override fun observeHome(): Flow<HomeUiState> =
        combine(
            routines.observeDetails(),
            profile.observeProfile(),
            sessions.observeFinished()
        ) { details, prof, finished ->
            val now = System.currentTimeMillis()
            val lastByRoutine = finished.groupBy { it.routineId }
                .mapValues { (_, list) -> list.maxOf { it.startedAt } }
            val domainSessions = finished.map { it.toDomain() }
            val accents = listOf(AccentColor.ORANGE, AccentColor.VOLT, AccentColor.CYAN)
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
                progress = HistoryCalcs.buildProgress(domainSessions, now)
            )
        }

    override suspend fun getHome(): HomeUiState = observeHome().first()
}
