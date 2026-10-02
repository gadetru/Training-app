package com.mytrainingplan.app.data.repository

import com.mytrainingplan.app.domain.model.AccentColor
import com.mytrainingplan.app.domain.model.FootKind
import com.mytrainingplan.app.domain.model.HomeUiState
import com.mytrainingplan.app.domain.model.RoutineSummary
import com.mytrainingplan.app.domain.model.WeeklyProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Firma futura del repositorio de Home (Fase B: Room).
 * El fake de Fase A implementa esta misma firma.
 */
interface HomeRepository {
    fun observeHome(): Flow<HomeUiState>
    suspend fun getHome(): HomeUiState
}

/**
 * Repo fake en memoria (spec 002, Fase A).
 * Sin Room, sin Hilt, sin red. Datos fijos de references/plantilla-vista-principal.
 */
class FakeHomeRepository : HomeRepository {

    private val state = MutableStateFlow(defaultHome())

    override fun observeHome(): Flow<HomeUiState> = state.asStateFlow()

    override suspend fun getHome(): HomeUiState = state.value

    companion object {
        fun defaultHome(): HomeUiState = HomeUiState(
            // TODO Fase B: saludo conectado al Profile real en Room.
            firstName = "Carlos",
            routines = listOf(
                RoutineSummary(
                    id = "torso-fuerza-hipertrofia",
                    title = "Torso - Fuerza & Hipertrofia",
                    tags = listOf("Pecho", "Espalda", "Hombros"),
                    durationMin = 45,
                    exerciseCount = 6,
                    lastDoneLabel = "Hace 2 días",
                    accent = AccentColor.ORANGE,
                    footNote = "Récord en Press Banca",
                    footKind = FootKind.PR
                ),
                RoutineSummary(
                    id = "pierna-core-explosivo",
                    title = "Pierna & Core Explosivo",
                    tags = listOf("Cuádriceps", "Isquios", "Abdomen"),
                    durationMin = 55,
                    exerciseCount = 7,
                    lastDoneLabel = "Ayer",
                    accent = AccentColor.VOLT,
                    footNote = "Enfoque: Sentadilla profunda",
                    footKind = FootKind.INFO
                ),
                RoutineSummary(
                    id = "full-body-funcional",
                    title = "Full Body Funcional",
                    tags = listOf("Fuerza", "Cardio"),
                    durationMin = 40,
                    exerciseCount = 5,
                    lastDoneLabel = null,
                    accent = AccentColor.CYAN,
                    footNote = "Recuperación activa",
                    footKind = FootKind.INFO
                )
            ),
            progress = WeeklyProgress(
                streakDays = 4,
                sessionsDone = 3,
                sessionsGoal = 4,
                minutes = 185,
                volumeDeltaPct = 12,
                weekChecks = listOf(true, true, true, true, false, false, false)
            )
        )
    }
}
