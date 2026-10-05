package com.mytrainingplan.app.feature.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mytrainingplan.app.data.repository.ExerciseRepository
import com.mytrainingplan.app.domain.model.EquipmentGroups
import com.mytrainingplan.app.domain.model.ExerciseFilter
import com.mytrainingplan.app.domain.model.ExercisesUiState
import com.mytrainingplan.app.domain.model.MuscleGroups
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Selector de ejercicios con Room (Paso 11 spec 006, Fase B).
 * Misma forma por fuera; repo real inyectado con Hilt.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExercisesViewModel @Inject constructor(
    private val repository: ExerciseRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val selectedMuscle = MutableStateFlow<String?>(null)
    private val selectedEquipment = MutableStateFlow<String?>(null)
    private val selectedIds = MutableStateFlow<Set<String>>(emptySet())

    private data class FilterSelection(
        val query: String,
        val muscle: String?,
        val equipment: String?,
        val selectedIds: Set<String>
    )

    private val selection: Flow<FilterSelection> = combine(
        query,
        selectedMuscle,
        selectedEquipment,
        selectedIds
    ) { q, m, e, ids -> FilterSelection(q, m, e, ids) }

    val uiState: StateFlow<ExercisesUiState> = selection
        .flatMapLatest { s ->
            val filter = ExerciseFilter(
                query = s.query,
                muscles = MuscleGroups.slugsFor(s.muscle),
                equipment = EquipmentGroups.slugsFor(s.equipment)
            )
            repository.observeExercises(filter).map { results ->
                ExercisesUiState(
                    query = s.query,
                    selectedMuscle = s.muscle,
                    selectedEquipment = s.equipment,
                    results = results,
                    selectedIds = s.selectedIds
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ExercisesUiState()
        )

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onMuscleSelected(muscle: String?) {
        selectedMuscle.value = muscle
    }

    fun onEquipmentSelected(equipment: String?) {
        selectedEquipment.value = equipment
    }

    fun onToggleSelected(id: String) {
        selectedIds.update { current ->
            if (id in current) current - id else current + id
        }
    }

    fun onClearSelection() {
        selectedIds.value = emptySet()
    }
}
