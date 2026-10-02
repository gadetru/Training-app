package com.mytrainingplan.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mytrainingplan.app.data.repository.FakeHomeRepository
import com.mytrainingplan.app.data.repository.HomeRepository
import com.mytrainingplan.app.domain.model.HomeUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Home fake Fase A (spec 002).
 * Expone StateFlow<HomeUiState> alimentado por el repo fake.
 * Sin Hilt: instanciación directa o viewModel() de lifecycle-viewmodel-compose.
 * No se crea ProfileViewModel: el saludo viene en HomeUiState.firstName
 * ("Carlos", igual que Profile.firstName por defecto de 001).
 * TODO Fase B: inyectar HomeRepository real (Room) con Hilt.
 */
class HomeViewModel(
    repository: HomeRepository = FakeHomeRepository()
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = repository.observeHome()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FakeHomeRepository.defaultHome()
        )
}
