package com.mytrainingplan.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mytrainingplan.app.data.repository.FakeHomeRepository
import com.mytrainingplan.app.data.repository.FakeRoutineRepository
import com.mytrainingplan.app.data.repository.HomeRepository
import com.mytrainingplan.app.data.repository.RoutineDetail
import com.mytrainingplan.app.data.repository.RoutineRepository
import com.mytrainingplan.app.domain.model.AccentColor
import com.mytrainingplan.app.domain.model.FootKind
import com.mytrainingplan.app.domain.model.HomeUiState
import com.mytrainingplan.app.domain.model.RoutineMuscleLabels
import com.mytrainingplan.app.domain.model.RoutineSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Home fake Fase A (spec 002 + 004).
 * Expone StateFlow<HomeUiState> alimentado por el repo fake de Home más las
 * rutinas creadas en el constructor (spec 004): el feed combina las 3 fijas
 * del 002 (se mantienen) con las guardadas en [RoutineRepository].
 * Comparte [FakeRoutineRepository.shared] para ver el mismo store en memoria.
 * Sin Hilt: instanciación directa o viewModel() de lifecycle-viewmodel-compose.
 * No se crea ProfileViewModel: el saludo viene en HomeUiState.firstName
 * ("Carlos", igual que Profile.firstName por defecto de 001).
 * TODO Fase B: inyectar repositorios reales (Room) con Hilt.
 */
class HomeViewModel(
    repository: HomeRepository = FakeHomeRepository(),
    routines: RoutineRepository = FakeRoutineRepository.shared
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeHome(),
        routines.observeDetails()
    ) { home, details ->
        home.copy(
            routines = home.routines + details.mapIndexed { index, detail ->
                detail.toSummary(index)
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FakeHomeRepository.defaultHome()
    )
}

/**
 * Mapeo Fase A de detalle guardado a card del feed (tags = músculos reales,
 * duración y nº de ejercicios reales). En Fase B vivirá en `data/mapper`.
 */
private fun RoutineDetail.toSummary(index: Int): RoutineSummary =
    RoutineSummary(
        id = routine.id,
        title = routine.name.ifBlank { "Rutina sin título" },
        tags = RoutineMuscleLabels.tagsFor(items.map { it.exercise }),
        durationMin = routine.durationMin,
        exerciseCount = items.size,
        lastDoneLabel = null,
        accent = when (index % 3) {
            0 -> AccentColor.ORANGE
            1 -> AccentColor.VOLT
            else -> AccentColor.CYAN
        },
        footNote = "Recién creada",
        footKind = FootKind.INFO,
        updatedAt = routine.updatedAt
    )
