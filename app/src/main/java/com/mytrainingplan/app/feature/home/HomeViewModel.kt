package com.mytrainingplan.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mytrainingplan.app.data.repository.HomeRepository
import com.mytrainingplan.app.data.repository.RoutineRepository
import com.mytrainingplan.app.data.repository.WorkoutRepository
import com.mytrainingplan.app.domain.model.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Home con Room (Paso 11 spec 006, Fase B; feed único spec 008).
 * Expone `observeHome()` tal cual: una sola fuente (`RoomHomeRepository`).
 * El borrado desde el menú ··· va directo a [RoutineRepository.deleteRoutine]
 * (borrado lógico + renumber; el feed se actualiza solo vía Flow).
 * Spec 022: `onFinishLive`/`onDiscardLive` cierran la sesión viva desde Home.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: HomeRepository,
    private val routineRepository: RoutineRepository,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = repository.observeHome()
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    /** Borrado confirmado desde el diálogo (menú ··· → Eliminar → Sí). */
    fun onDeleteRoutine(routineId: String) {
        viewModelScope.launch { routineRepository.deleteRoutine(routineId) }
    }

    /** Spec 022 §13: Terminar la sesión viva desde Home (sin entrar a la sesión). */
    fun onFinishLive() {
        val sessionId = uiState.value.liveSessionId ?: return
        viewModelScope.launch { workoutRepository.finishSession(sessionId) }
    }

    /** Spec 022 §14: Descartar la sesión viva desde Home (con confirmación en la UI). */
    fun onDiscardLive() {
        val sessionId = uiState.value.liveSessionId ?: return
        viewModelScope.launch { workoutRepository.discardSession(sessionId) }
    }
}
