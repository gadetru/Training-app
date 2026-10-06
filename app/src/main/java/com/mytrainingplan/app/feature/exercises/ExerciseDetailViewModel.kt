package com.mytrainingplan.app.feature.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mytrainingplan.app.data.repository.ExerciseRepository
import com.mytrainingplan.app.domain.model.Exercise
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Ficha de ejercicio (spec 010).
 * Carga un ejercicio por id desde Room (la red solo rellena vía `CatalogSync`;
 * sin red abre con lo local). `null` = no encontrado o id nulo: la pantalla
 * muestra aviso corto sin caerse. Misma forma que `RoutineEditViewModel`:
 * `openExercise(id)` idempotente desde `LaunchedEffect` (la rotación no
 * duplica nada: el ViewModel sobrevive y el id ya está fijado).
 */
@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    private val repository: ExerciseRepository
) : ViewModel() {

    private val exerciseId = MutableStateFlow<String?>(null)
    private val _exercise = MutableStateFlow<Exercise?>(null)
    val exercise: StateFlow<Exercise?> = _exercise.asStateFlow()

    fun openExercise(id: String?) {
        if (exerciseId.value != null) return
        exerciseId.value = id
        if (id == null) return
        viewModelScope.launch {
            _exercise.value = repository.getById(id)
        }
    }
}
