package com.mytrainingplan.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mytrainingplan.app.data.repository.ProfileRepository
import com.mytrainingplan.app.domain.model.Profile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Perfil con Room (Paso 11 spec 006, Fase B).
 * Lee/escribe la fila `me` vía repo. La Screen sigue igual por fuera.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository
) : ViewModel() {

    val uiState: StateFlow<Profile> = repository.observeProfile()
        .map { it ?: Profile() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Profile()
        )

    fun onSave(profile: Profile) {
        viewModelScope.launch { repository.save(profile) }
    }
}
