package com.mytrainingplan.app.feature.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mytrainingplan.app.data.repository.WorkoutRepository
import com.mytrainingplan.app.data.repository.WorkoutSessionDetail
import com.mytrainingplan.app.domain.model.TimerState
import com.mytrainingplan.app.domain.model.timerElapsedSec
import com.mytrainingplan.app.domain.model.timerState
import com.mytrainingplan.app.domain.model.WorkoutExerciseUi
import com.mytrainingplan.app.domain.model.WorkoutUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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

/**
 * Estado solo de pantalla (jamás se persiste): descanso, plegado y guardado.
 * El cronómetro (`elapsedSec`/`isPaused`) se deriva de [detail] (DB) más un
 * tick local de 1 s para que el reloj se vea moverse en pantalla sin lecturas
 * de DB (spec 022 §48). Vive en un único [MutableStateFlow] para que el
 * [combine] con la sesión sea de 2 flujos.
 */
private data class Ephemeral(
    val restRemainingSec: Int? = null,
    val restTotalSec: Int = 0,
    val restEntryId: String? = null,
    val expanded: Map<String, Boolean> = emptyMap(),
    val isSaving: Boolean = false
)

/**
 * Sesión en vivo con Room (Paso 11 spec 006, Fase B; spec 022: reanudar con
 * `sessionId`, cronómetro persistente, CTA "Finalizar").
 * Misma forma por fuera; repo real inyectado con Hilt.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

    private var opened = false
    private val sessionId = MutableStateFlow<String?>(null)
    private val ephemeral = MutableStateFlow(Ephemeral())

    /** Reloj de pantalla: se inicializa desde detail y avanza por tick de 1 s. */
    private val elapsedSec = MutableStateFlow(0)

    private val detail: StateFlow<WorkoutSessionDetail?> =
        sessionId.flatMapLatest { id: String? ->
            if (id == null) flowOf(null) else repository.observeSession(id)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val uiState: StateFlow<WorkoutUiState> = combine(
        detail,
        combine(ephemeral, elapsedSec) { e, s -> Pair(e, s) }
    ) { d, pair ->
        val (e, secs) = pair
        if (d == null) {
            WorkoutUiState(
                elapsedSec = secs,
                isPaused = true,
                restRemainingSec = e.restRemainingSec,
                restTotalSec = e.restTotalSec,
                isSaving = e.isSaving
            )
        } else {
            // Spec 019 paso 1: todos colapsados al abrir, sin excepción
            // (el mapa solo guarda los toques del usuario).
            val items = d.items.map { item ->
                WorkoutExerciseUi(
                    routineExercise = item.routineExercise,
                    exercise = item.exercise,
                    entries = item.entries.sortedBy { it.setNumber },
                    planned = item.planned.sortedBy { it.setNumber },
                    expanded = WorkoutExpandRules.defaultExpanded(
                        e.expanded, item.routineExercise.id
                    )
                )
            }
            val all = items.flatMap { it.entries }
            val isPaused = d.session.timerState != TimerState.ACTIVO
            WorkoutUiState(
                sessionId = d.session.id,
                routineName = d.routineName,
                elapsedSec = secs,
                isPaused = isPaused,
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
        // (Re)inicializa el reloj de pantalla desde la sesión de DB.
        viewModelScope.launch {
            detail.collect { d ->
                val now = System.currentTimeMillis()
                elapsedSec.value = if (d == null) 0 else d.session.timerElapsedSec(now)
            }
        }

        // Tick de 1 s: avanza el reloj cuando ACTIVO + cuenta atrás del descanso.
        viewModelScope.launch {
            while (true) {
                delay(1_000)
                val now = System.currentTimeMillis()
                val d = detail.value
                if (d != null && d.session.runningSince != null) {
                    elapsedSec.value = d.session.timerElapsedSec(now)
                }
                var closedEntryId: String? = null
                ephemeral.update { cur ->
                    val rest = cur.restRemainingSec
                    if (rest == null) return@update cur
                    // A 0 se cierra solo, sin crash.
                    val next = (rest - 1).coerceAtLeast(0)
                    if (next <= 0) {
                        closedEntryId = cur.restEntryId
                        cur.copy(restRemainingSec = null, restEntryId = null)
                    } else cur.copy(restRemainingSec = next)
                }
                if (closedEntryId != null) autoCollapseAndAdvance(closedEntryId!!)
            }
        }

        // Tick de 15 s: persiste el cronómetro corriendo (kill-safe aproximado, spec 022).
        viewModelScope.launch {
            while (true) {
                delay(15_000)
                val sid = sessionId.value ?: return@launch
                val d = detail.value ?: return@launch
                if (d.session.runningSince != null) {
                    viewModelScope.launch { repository.timerSync(sid, System.currentTimeMillis()) }
                }
            }
        }
    }

    /**
     * Abre la sesión: si `sessionId` != null observa la existente (reanudar);
     * si no, crea una con `startSession(routineId)` (prefill última-vs-plan).
     * Idempotente (rotación no duplica sesiones: el ViewModel sobrevive y
     * [opened] ya está fijado). Con null no crea nada: estado vacío.
     */
    fun openSession(routineId: String?, sessionId: String? = null) {
        if (opened) return
        opened = true
        if (routineId == null && sessionId == null) return
        viewModelScope.launch {
            if (sessionId != null) {
                this@WorkoutViewModel.sessionId.value = sessionId
            } else {
                this@WorkoutViewModel.sessionId.value = repository.startSession(routineId!!)
            }
        }
    }

    /**
     * Spec 013 paso 4 + spec 019 paso 1: niega el estado EFECTIVO (toque
     * previo o defecto colapsado, igual que el `combine`), no `?: true`.
     */
    fun onToggleExpanded(routineExerciseId: String) {
        ephemeral.update { cur ->
            cur.copy(expanded = WorkoutExpandRules.toggle(cur.expanded, routineExerciseId))
        }
    }

    /**
     * Spec 013 paso 4 + spec 019 paso 1 + spec 022: alterna Comenzar → Pausar →
     * Reanudar persistiéndolo en el repo según el estado derivado de DB.
     */
    fun onPauseToggle() {
        val sid = sessionId.value ?: return
        val d = detail.value ?: return
        val now = System.currentTimeMillis()
        when (d.session.timerState) {
            TimerState.ACTIVO -> viewModelScope.launch { repository.timerPause(sid, now) }
            TimerState.EN_PAUSA, TimerState.SIN_EMPEZAR ->
                viewModelScope.launch { repository.timerStart(sid, now) }
        }
    }

    /**
     * Spec 013 paso 2: guardado de golpe desde el modal (KG con signo,
     * REPS ≥ 0, PAUSA ≥ 0) vía rutas existentes: `updateEntry` (KG+REPS) y
     * `adjustRest` con el delta (PAUSA absoluta menos base). Sin Room nuevo.
     */
    fun onUpdateEntry(entryId: String, weightKg: Double, reps: Int, restSeconds: Int) {
        val sid = sessionId.value ?: return
        val d = detail.value ?: return
        val entry = d.items.flatMap { it.entries }.find { it.id == entryId } ?: return
        val safeReps = reps.coerceAtLeast(0)
        val safeRest = restSeconds.coerceAtLeast(0)
        viewModelScope.launch { repository.updateEntry(sid, entryId, safeReps, weightKg) }
        val plannedRest = d.items.flatMap { it.planned }
            .find { it.id == entry.plannedSetId }?.restSeconds
        val baseRest = entry.restSeconds ?: plannedRest ?: 90
        val deltaRest = safeRest - baseRest
        if (deltaRest != 0) {
            viewModelScope.launch { repository.adjustRest(sid, entryId, deltaRest) }
        }
    }

    /**
     * Spec 019 paso 2: al cerrarse el descanso (a 0 o con Terminar) o al
     * marcar la última serie sin descanso, si el ejercicio queda sin
     * pendientes se colapsa y se expande el primer pendiente siguiente en
     * orden. Si no quedan, solo colapsa. Estado solo de pantalla (mapa
     * `expanded`); el tap manual posterior pisa lo automático. La serie
     * disparadora se trata como hecha (el flow de Room puede ir con retardo
     * tras `toggleSetDone`).
     */
    private fun autoCollapseAndAdvance(finishedEntryId: String) {
        val items = detail.value?.items ?: return
        ephemeral.update { cur ->
            val next = WorkoutExpandRules.afterClose(items, cur.expanded, finishedEntryId)
                ?: return@update cur
            cur.copy(expanded = next)
        }
    }

    /**
     * Completa/desmarca una serie: al marcar volt arranca el descanso
     * flotante con la cuenta atrás de `restSeconds` de esa serie (si null,
     * el del plan; si también null, 90 s como el defecto del constructor).
     * Marcar exige cronómetro activo (no en pausa / sin comenzar);
     * desmarcar siempre está permitido.
     */
    fun onToggleDone(entryId: String) {
        val d = detail.value ?: return
        val sid = sessionId.value ?: return
        val entry = d.items.flatMap { it.entries }.find { it.id == entryId } ?: return
        if (!entry.done && d.session.timerState != TimerState.ACTIVO) return
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
                } else {
                    // Spec 019 paso 2: sin descanso (0 s) el colapso y el
                    // avance ocurren al marcar la última serie.
                    autoCollapseAndAdvance(entryId)
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
        // Spec 019 paso 2: al cerrar (Terminar o tick a 0), colapso auto +
        // avance al siguiente si el ejercicio queda sin pendientes.
        val finishedEntryId = ephemeral.value.restEntryId
        ephemeral.update { it.copy(restRemainingSec = null, restEntryId = null) }
        if (finishedEntryId != null) autoCollapseAndAdvance(finishedEntryId)
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

    /**
     * Spec 016 paso 4: añade una serie viva calcada de la última (+ su
     * planificada en la rutina). Sin lógica de datos: la doble escritura
     * vive en el repositorio ([WorkoutRepository.addLiveSet]).
     */
    fun onAddLiveSet(routineExerciseId: String) {
        val sid = sessionId.value ?: return
        viewModelScope.launch { repository.addLiveSet(sid, routineExerciseId) }
    }

    /**
     * Spec 016 paso 5: borra una serie viva pendiente (+ su planificada).
     * Sin lógica de datos: vive en el repositorio
     * ([WorkoutRepository.deleteLiveSet]).
     */
    fun onDeleteLiveSet(entryId: String) {
        val sid = sessionId.value ?: return
        viewModelScope.launch { repository.deleteLiveSet(sid, entryId) }
    }

    /**
     * Spec 021: sustituye el ejercicio del slot de rutina en sesión.
     * Reescribe el exerciseId de todas las entradas (hechas incluidas) en
     * el repo; el flujo de detail recompone solo y la tarjeta muestra el
     * nuevo nombre vía item.exercise.
     */
    fun onReplaceExercise(routineExerciseId: String, newExerciseId: String) {
        val sid = sessionId.value ?: return
        viewModelScope.launch {
            repository.replaceExercise(sid, routineExerciseId, newExerciseId)
        }
    }

    /** Flujo crudo para previsualización o depuración. */
    internal fun observeDetail(): Flow<WorkoutSessionDetail?> = detail
}
