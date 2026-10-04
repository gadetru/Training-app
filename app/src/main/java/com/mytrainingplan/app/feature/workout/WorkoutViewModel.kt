package com.mytrainingplan.app.feature.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mytrainingplan.app.data.repository.FakeWorkoutRepository
import com.mytrainingplan.app.data.repository.WorkoutRepository
import com.mytrainingplan.app.data.repository.WorkoutSessionDetail
import com.mytrainingplan.app.domain.model.WorkoutExerciseUi
import com.mytrainingplan.app.domain.model.WorkoutUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Paso de KG de los steppers en vivo. */
const val KG_STEP: Double = 2.5

/**
 * Sesión en vivo fake Fase A (spec 005).
 * Expone StateFlow<WorkoutUiState> combinando la sesión del repo (fuente de
 * verdad de entradas) con estado solo de pantalla (`elapsedSec`, `isPaused`,
 * descanso restante, `expanded`, que jamás se persiste).
 *
 * El timer vive aquí (no en el Composable) para sobrevivir a recomposición
 * y rotación; pausar congela el acumulado, no resetea. El descanso cuenta
 * atrás también se congela en pausa.
 *
 * Sin Hilt: instanciación con viewModel() de lifecycle-viewmodel-compose o
 * directa. Comparte [FakeWorkoutRepository.shared] para ver el mismo store
 * que Home. La pantalla llama [openSession] una vez (null = estado vacío).
 * TODO Fase B: inyectar repositorio real (Room) con Hilt.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(
    private val repository: WorkoutRepository = FakeWorkoutRepository.shared
) : ViewModel() {

    private var opened = false
    private val sessionId = MutableStateFlow<String?>(null)
    private val elapsed = MutableStateFlow(0)
    private val paused = MutableStateFlow(false)
    private val restRemaining = MutableStateFlow<Int?>(null)
    private val restTotal = MutableStateFlow(0)
    private val restEntryId = MutableStateFlow<String?>(null)
    private val expanded = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val saving = MutableStateFlow(false)

    private val detail: StateFlow<WorkoutSessionDetail?> =
        sessionId.flatMapLatest { id: String? ->
            if (id == null) flowOf(null) else repository.observeSession(id)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val uiState: StateFlow<WorkoutUiState> = combine(
        detail, elapsed, paused, restRemaining, restTotal, expanded, saving
    ) { d: WorkoutSessionDetail?, el: Int, pa: Boolean, rest: Int?, total: Int,
        exp: Map<String, Boolean>, sv: Boolean ->
        if (d == null) {
            WorkoutUiState(
                elapsedSec = el,
                isPaused = pa,
                restRemainingSec = rest,
                restTotalSec = total,
                isSaving = sv
            )
        } else {
            val items = d.items.map { item ->
                WorkoutExerciseUi(
                    routineExercise = item.routineExercise,
                    exercise = item.exercise,
                    entries = item.entries.sortedBy { it.setNumber },
                    planned = item.planned.sortedBy { it.setNumber },
                    expanded = exp[item.routineExercise.id] ?: true
                )
            }
            val all = items.flatMap { it.entries }
            WorkoutUiState(
                sessionId = d.session.id,
                routineName = d.routine.name,
                elapsedSec = el,
                isPaused = pa,
                exercises = items,
                doneCount = all.count { it.done },
                totalCount = all.size,
                restRemainingSec = rest,
                restTotalSec = total,
                isSaving = sv
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WorkoutUiState()
    )

    init {
        viewModelScope.launch {
            while (true) {
                delay(1_000)
                if (paused.value) continue
                if (sessionId.value != null) elapsed.update { it + 1 }
                val cur = restRemaining.value
                if (cur != null) {
                    if (cur <= 1) {
                        // A 0 se cierra solo, sin crash.
                        restRemaining.value = null
                        restEntryId.value = null
                    } else {
                        restRemaining.value = cur - 1
                    }
                }
            }
        }
    }

    /**
     * Abre la sesión: crea una con `startSession(routineId)` (prefill
     * última-vs-plan en el repo). Idempotente (rotación no duplica sesiones:
     * el ViewModel sobrevive y [opened] ya está fijado). Con null no crea
     * nada: estado vacío sin crash.
     */
    fun openSession(routineId: String?) {
        if (opened) return
        opened = true
        if (routineId == null) return
        viewModelScope.launch {
            sessionId.value = repository.startSession(routineId)
        }
    }

    fun onToggleExpanded(routineExerciseId: String) {
        expanded.update { current ->
            current + (routineExerciseId to !(current[routineExerciseId] ?: true))
        }
    }

    /** Stepper KG en vivo: cambia solo esa entrada. */
    fun onKgChange(entryId: String, deltaKg: Double) {
        val sid = sessionId.value ?: return
        val entry = findEntry(entryId) ?: return
        val newWeight = (entry.weightKg + deltaKg).coerceAtLeast(0.0)
        viewModelScope.launch { repository.updateEntry(sid, entryId, entry.reps, newWeight) }
    }

    /** Stepper REPS en vivo: cambia solo esa entrada. */
    fun onRepsChange(entryId: String, deltaReps: Int) {
        val sid = sessionId.value ?: return
        val entry = findEntry(entryId) ?: return
        viewModelScope.launch { repository.updateEntry(sid, entryId, (entry.reps + deltaReps).coerceAtLeast(0), entry.weightKg) }
    }

    /**
     * Completa/desmarca una serie: al marcar volt arranca el descanso
     * flotante con la cuenta atrás de `restSeconds` de esa serie (si null,
     * el del plan; si también null, 90 s como el defecto del constructor).
     */
    fun onToggleDone(entryId: String) {
        val d = detail.value ?: return
        val sid = sessionId.value ?: return
        val entry = d.items.flatMap { it.entries }.find { it.id == entryId } ?: return
        viewModelScope.launch {
            repository.toggleSetDone(sid, entryId, !entry.done)
            if (!entry.done) {
                val plannedRest = d.items.flatMap { it.planned }
                    .find { it.id == entry.plannedSetId }?.restSeconds
                val total = entry.restSeconds ?: plannedRest ?: 90
                if (total > 0) {
                    restTotal.value = total
                    restRemaining.value = total
                    restEntryId.value = entryId
                }
            }
        }
    }

    /** Botones `+10s/-10s` del descanso: ajustan de 10 en 10. */
    fun onRestAdjust(deltaSec: Int) {
        val sid = sessionId.value ?: return
        val entryId = restEntryId.value ?: return
        restRemaining.update { ((it ?: 0) + deltaSec).coerceAtLeast(0) }
        viewModelScope.launch { repository.adjustRest(sid, entryId, deltaSec) }
    }

    /** `Terminar descanso`: cierra el overlay sin crash. */
    fun onRestFinish() {
        restRemaining.value = null
        restEntryId.value = null
    }

    fun onPauseToggle() {
        paused.update { !it }
    }

    fun onFinish(onDone: () -> Unit) {
        val sid = sessionId.value ?: return
        viewModelScope.launch {
            saving.value = true
            try {
                repository.finishSession(sid)
            } finally {
                saving.value = false
            }
            onDone()
        }
    }

    fun onDiscard(onDone: () -> Unit) {
        val sid = sessionId.value
        viewModelScope.launch {
            if (sid != null) repository.discardSession(sid)
            onRestFinish()
            onDone()
        }
    }

    private fun findEntry(entryId: String) =
        detail.value?.items?.flatMap { it.entries }?.find { it.id == entryId }

    /** Flujo crudo para previsualización o depuración. */
    internal fun observeDetail(): Flow<WorkoutSessionDetail?> = detail
}
