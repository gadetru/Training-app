package com.mytrainingplan.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mytrainingplan.app.data.repository.HomeRepository
import com.mytrainingplan.app.domain.model.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Home con Room (Paso 11 spec 006, Fase B; feed único spec 008).
 * Expone `observeHome()` tal cual: una sola fuente (`RoomHomeRepository`).
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: HomeRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = repository.observeHome()
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )
}
