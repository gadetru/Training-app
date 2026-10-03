package com.mytrainingplan.app.feature.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mytrainingplan.app.data.repository.FakeRoutineRepository
import com.mytrainingplan.app.data.repository.RoutineRepository
import com.mytrainingplan.app.domain.model.RoutineEditUiState
import com.mytrainingplan.app.domain.model.RoutineExerciseUi
import com.mytrainingplan.app.domain.model.RoutineMuscleLabels
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Campo editable de una serie en la matriz del constructor. */
enum class SetField {
    TARGET_REPS,
    WEIGHT_KG,
    REST_SECONDS,
    LOAD_NOTE
}

/**
 * Constructor de rutina fake Fase A (spec 004).
 * Expone StateFlow<RoutineEditUiState> combinando el borrador del repo
 * (fuente de verdad de entidades) con estado solo de pantalla (`expanded`,
 * `handEdited`, que jamás se persiste).
 *
 * Sin Hilt: instanciación con viewModel() de lifecycle-viewmodel-compose o
 * directa. Comparte [FakeRoutineRepository.shared] para ver el mismo store
 * que el feed de Home. La pantalla llama [openRoutine] una vez (null =
 * rutina nueva, o id existente para la edición futura).
 * TODO Fase B: inyectar repositorio real (Room) con Hilt.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RoutineEditViewModel(
    private val repository: RoutineRepository = FakeRoutineRepository.shared
) : ViewModel() {

    private val routineId = MutableStateFlow<String?>(null)
    private val expanded = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val handEdited = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    private val saving = MutableStateFlow(false)

    val uiState: StateFlow<RoutineEditUiState> = combine(
        routineId.flatMapLatest { id ->
            if (id == null) flowOf(null) else repository.observeRoutine(id)
        },
        expanded,
        handEdited,
        saving
    ) { detail, exp, edited, isSaving ->
        if (detail == null) {
            RoutineEditUiState(isSaving = isSaving)
        } else {
            RoutineEditUiState(
                routineId = detail.routine.id,
                name = detail.routine.name,
                durationMin = detail.routine.durationMin,
                tags = RoutineMuscleLabels.tagsFor(detail.items.map { it.exercise }),
                exercises = detail.items.map { item ->
                    RoutineExerciseUi(
                        routineExercise = item.routineExercise,
                        exercise = item.exercise,
                        sets = item.sets.sortedBy { it.setNumber },
                        expanded = exp[item.routineExercise.id] ?: true,
                        handEdited = edited[item.routineExercise.id] ?: emptySet()
                    )
                },
                isSaving = isSaving
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RoutineEditUiState()
    )

    /**
     * Abre el constructor: crea un borrador vacío si [id] es null o carga el
     * existente. Idempotente (rotación no duplica borradores: el ViewModel
     * sobrevive y [routineId] ya está fijado).
     */
    fun openRoutine(id: String?) {
        if (routineId.value != null) return
        viewModelScope.launch {
            routineId.value = id ?: repository.createRoutine()
        }
    }

    fun onNameChange(value: String) {
        val id = routineId.value ?: return
        val duration = uiState.value.durationMin
        viewModelScope.launch { repository.updateMeta(id, value, duration) }
    }

    fun onDurationChange(minutes: Int) {
        val id = routineId.value ?: return
        val name = uiState.value.name
        viewModelScope.launch { repository.updateMeta(id, name, minutes.coerceAtLeast(0)) }
    }

    fun onAddExercises(ids: List<String>) {
        val id = routineId.value ?: return
        if (ids.isEmpty()) return
        viewModelScope.launch { repository.addExercises(id, ids) }
    }

    fun onToggleExpanded(routineExerciseId: String) {
        expanded.update { current ->
            current + (routineExerciseId to !(current[routineExerciseId] ?: true))
        }
    }

    fun onDeleteExercise(routineExerciseId: String) {
        viewModelScope.launch { repository.deleteExercise(routineExerciseId) }
        expanded.update { it - routineExerciseId }
        handEdited.update { it - routineExerciseId }
    }

    /**
     * Edita una celda con propagación (`docs/MODELO_DE_DATOS.md:82`): el valor
     * se copia a las series siguientes aún no editadas a mano; la celda
     * editada queda marcada y ya no se sobrescribe. El RIR ([onRirChange])
     * es por fila y no propaga.
     */
    fun onSetFieldChange(routineExerciseId: String, setId: String, field: SetField, raw: String) {
        val item = uiState.value.exercises.find { it.routineExercise.id == routineExerciseId }
            ?: return
        val edited = item.sets.find { it.id == setId } ?: return
        val newReps = if (field == SetField.TARGET_REPS) {
            raw.trim().toIntOrNull()?.coerceAtLeast(0) ?: return
        } else {
            edited.targetReps
        }
        val newWeight = if (field == SetField.WEIGHT_KG) {
            raw.trim().replace(',', '.').toDoubleOrNull() ?: return
        } else {
            edited.weightKg
        }
        val newRest = if (field == SetField.REST_SECONDS) {
            raw.trim().toIntOrNull()?.coerceAtLeast(0) ?: return
        } else {
            edited.restSeconds
        }
        val newNote = if (field == SetField.LOAD_NOTE) {
            raw.ifBlank { null }
        } else {
            edited.loadNote
        }
        if (newReps == edited.targetReps && newWeight == edited.weightKg &&
            newRest == edited.restSeconds && newNote == edited.loadNote
        ) {
            return
        }
        viewModelScope.launch {
            val key = handKey(setId, field)
            handEdited.update { current ->
                current + (routineExerciseId to ((current[routineExerciseId] ?: emptySet()) + key))
            }
            val editedKeys = handEdited.value[routineExerciseId] ?: emptySet()
            // Fila editada.
            repository.updateSet(
                routineExerciseId = routineExerciseId,
                setId = setId,
                targetReps = newReps,
                weightKg = newWeight,
                restSeconds = newRest,
                loadNote = newNote,
                rir = edited.rir
            )
            // Solo siguientes no editadas a mano.
            item.sets
                .filter { it.setNumber > edited.setNumber && handKey(it.id, field) !in editedKeys }
                .forEach { following ->
                    repository.updateSet(
                        routineExerciseId = routineExerciseId,
                        setId = following.id,
                        targetReps = newReps,
                        weightKg = newWeight,
                        restSeconds = newRest,
                        loadNote = newNote,
                        rir = following.rir
                    )
                }
        }
    }

    /**
     * Cambia el RIR de una serie (`null` calentamiento, `0` al fallo, `n` RIR n).
     * Es por fila y no propaga (la referencia muestra un RIR distinto por serie).
     */
    fun onRirChange(routineExerciseId: String, setId: String, rir: Int?) {
        val item = uiState.value.exercises.find { it.routineExercise.id == routineExerciseId }
            ?: return
        val set = item.sets.find { it.id == setId } ?: return
        if (set.rir == rir) return
        viewModelScope.launch {
            repository.updateSet(
                routineExerciseId = routineExerciseId,
                setId = setId,
                targetReps = set.targetReps,
                weightKg = set.weightKg,
                restSeconds = set.restSeconds,
                loadNote = set.loadNote,
                rir = rir
            )
        }
    }

    fun onAddSet(routineExerciseId: String) {
        viewModelScope.launch { repository.addSet(routineExerciseId) }
    }

    fun onDeleteSet(routineExerciseId: String, setId: String) {
        viewModelScope.launch { repository.deleteSet(routineExerciseId, setId) }
        handEdited.update { current ->
            val keys = current[routineExerciseId] ?: return@update current
            val prefix = "$setId:"
            current + (routineExerciseId to keys.filterNot { it.startsWith(prefix) }.toSet())
        }
    }

    fun onSave(onSaved: () -> Unit) {
        val id = routineId.value ?: return
        viewModelScope.launch {
            saving.value = true
            try {
                repository.saveRoutine(id)
            } finally {
                saving.value = false
            }
            onSaved()
        }
    }

    fun onDiscard(onDone: () -> Unit) {
        val id = routineId.value
        viewModelScope.launch {
            if (id != null) repository.discard(id)
            onDone()
        }
    }

    companion object {
        private fun handKey(setId: String, field: SetField): String = "$setId:${field.name}"
    }
}
