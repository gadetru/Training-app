package com.mytrainingplan.app.feature.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mytrainingplan.app.data.repository.ExerciseRepository
import com.mytrainingplan.app.data.repository.FakeExerciseRepository
import com.mytrainingplan.app.domain.model.ExerciseFilter
import com.mytrainingplan.app.domain.model.ExercisesUiState
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
 * Selector de ejercicios fake Fase A (spec 003).
 * Expone StateFlow<ExercisesUiState> alimentado por el repo fake.
 * Estado neutro al abrir: sin query ni filtros (propuesta Preguntas abiertas).
 * La selección vive en memoria y sobrevive a rotación (ViewModel).
 * Sin Hilt: instanciación directa o viewModel() de lifecycle-viewmodel-compose.
 * TODO Fase B: inyectar ExerciseRepository real (Room) con Hilt.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExercisesViewModel(
    private val repository: ExerciseRepository = FakeExerciseRepository()
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
                muscles = s.muscle?.let { setOf(it) } ?: emptySet(),
                equipment = s.equipment?.let { setOf(it) } ?: emptySet()
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
            initialValue = ExercisesUiState(
                results = FakeExerciseRepository.defaultExercises()
            )
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
