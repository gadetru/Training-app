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
 * Estado solo de pantalla (jamás se persiste): cronómetro, pausa, descanso,
 * plegado y guardado. Vive en un único [MutableStateFlow] para que el
 * [combine] con la sesión sea de 2 flujos (las sobrecargas tipadas de más
 * aridad no existen en la versión de coroutines del proyecto).
 */
private data class Ephemeral(
    val elapsedSec: Int = 0,
    val isPaused: Boolean = false,
    val restRemainingSec: Int? = null,
    val restTotalSec: Int = 0,
    val restEntryId: String? = null,
    val expanded: Map<String, Boolean> = emptyMap(),
    val isSaving: Boolean = false
)

/**
 * Sesión en vivo fake Fase A (spec 005).
 * Expone StateFlow<WorkoutUiState> combinando la sesión del repo (fuente de
 * verdad de entradas) con estado solo de pantalla ([Ephemeral]).
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
    private val ephemeral = MutableStateFlow(Ephemeral())

    private val detail: StateFlow<WorkoutSessionDetail?> =
        sessionId.flatMapLatest { id: String? ->
            if (id == null) flowOf(null) else repository.observeSession(id)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val uiState: StateFlow<WorkoutUiState> = combine(detail, ephemeral) { d, e ->
        if (d == null) {
            WorkoutUiState(
                elapsedSec = e.elapsedSec,
                isPaused = e.isPaused,
                restRemainingSec = e.restRemainingSec,
                restTotalSec = e.restTotalSec,
                isSaving = e.isSaving
            )
        } else {
            // Por defecto solo el primer ejercicio pendiente va expandido
            // (completados colapsados, pendientes contraídos); el mapa solo
            // guarda los toques del usuario.
            val firstPending = d.items.indexOfFirst { item ->
                item.entries.any { !it.done }
            }
            val items = d.items.mapIndexed { index, item ->
                WorkoutExerciseUi(
                    routineExercise = item.routineExercise,
                    exercise = item.exercise,
                    entries = item.entries.sortedBy { it.setNumber },
                    planned = item.planned.sortedBy { it.setNumber },
                    expanded = e.expanded[item.routineExercise.id] ?: (index == firstPending)
                )
            }
            val all = items.flatMap { it.entries }
            WorkoutUiState(
                sessionId = d.session.id,
                routineName = d.routineName,
                elapsedSec = e.elapsedSec,
                isPaused = e.isPaused,
                exercises = items,
                doneCount = all.count { it.done },
                totalCount = all.size,
                restRemainingSec = e.restRemainingSec,
                restTotalSec = e.restTotalSec,
                isSaving = e.isSaving
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
                ephemeral.update { cur ->
                    if (cur.isPaused) return@update cur
                    val ticked = if (sessionId.value != null) {
                        cur.copy(elapsedSec = cur.elapsedSec + 1)
                    } else {
                        cur
                    }
                    val rest = ticked.restRemainingSec
                    if (rest != null) {
                        // A 0 se cierra solo, sin crash.
                        if (rest <= 1) ticked.copy(restRemainingSec = null, restEntryId = null)
                        else ticked.copy(restRemainingSec = rest - 1)
                    } else {
                        ticked
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
        ephemeral.update { cur ->
            cur.copy(
                expanded = cur.expanded +
                    (routineExerciseId to !(cur.expanded[routineExerciseId] ?: true))
            )
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
                    ephemeral.update {
                        it.copy(
                            restTotalSec = total,
                            restRemainingSec = total,
                            restEntryId = entryId
                        )
                    }
                }
            }
        }
    }

    /** Botones `+10s/-10s` del descanso: ajustan de 10 en 10. */
    fun onRestAdjust(deltaSec: Int) {
        val sid = sessionId.value ?: return
        val entryId = ephemeral.value.restEntryId ?: return
        ephemeral.update {
            it.copy(restRemainingSec = ((it.restRemainingSec ?: 0) + deltaSec).coerceAtLeast(0))
        }
        viewModelScope.launch { repository.adjustRest(sid, entryId, deltaSec) }
    }

    /** `Terminar descanso`: cierra el overlay sin crash. */
    fun onRestFinish() {
        ephemeral.update { it.copy(restRemainingSec = null, restEntryId = null) }
    }

    fun onPauseToggle() {
        ephemeral.update { it.copy(isPaused = !it.isPaused) }
    }

    fun onFinish(onDone: () -> Unit) {
        val sid = sessionId.value ?: return
        viewModelScope.launch {
            ephemeral.update { it.copy(isSaving = true) }
            try {
                repository.finishSession(sid)
            } finally {
                ephemeral.update { it.copy(isSaving = false) }
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
