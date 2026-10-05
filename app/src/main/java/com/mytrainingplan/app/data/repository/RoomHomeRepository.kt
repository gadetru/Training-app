package com.mytrainingplan.app.data.repository

import com.mytrainingplan.app.domain.model.AccentColor
import com.mytrainingplan.app.domain.model.FootKind
import com.mytrainingplan.app.domain.model.HomeUiState
import com.mytrainingplan.app.domain.model.RoutineMuscleLabels
import com.mytrainingplan.app.domain.model.RoutineSummary
import com.mytrainingplan.app.domain.model.WeeklyProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * Repo real de Home (Paso 10 spec 006, cierra los "4 reales").
 * Feed desde rutinas guardadas; saludo desde el perfil real.
 * Historial/progreso reales llegan tras sesiones persistidas (futuro).
 */
class RoomHomeRepository(
    private val routines: RoutineRepository,
    private val profile: ProfileRepository
) : HomeRepository {

    override fun observeHome(): Flow<HomeUiState> =
        combine(routines.observeDetails(), profile.observeProfile()) { details, prof ->
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
                        lastDoneLabel = null,
                        accent = accents[i % accents.size],
                        footNote = "",
                        footKind = FootKind.INFO,
                        updatedAt = d.routine.updatedAt,
                        deleted = d.routine.deleted
                    )
                },
                progress = WeeklyProgress()
            )
        }

    override suspend fun getHome(): HomeUiState = observeHome().first()
}
