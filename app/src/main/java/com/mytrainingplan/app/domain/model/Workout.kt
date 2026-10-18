package com.mytrainingplan.app.domain.model

/**
 * Sesión en vivo en memoria (Paso 1 spec 005, Fase A).
 * Modelos limpios de dominio: sin dependencias Android / Room / Retrofit.
 * La UI solo ve estos modelos.
 *
 * - IDs de usuario = UUID generados en cliente.
 * - Toda entidad de usuario lleva `updatedAt` + `deleted` (borrado lógico,
 *   sync-ready Fase 2) desde el día uno.
 * - `weightKg` con signo (`+` lastre, `0` corporal, `-` ayuda) + `loadNote`
 *   libre; `restSeconds` vive en cada serie, no en el ejercicio
 *   (`docs/MODELO_DE_DATOS.md:47-58,62-79`).
 * - Prefill al iniciar (`docs/MODELO_DE_DATOS.md:86`): última sesión de ese
 *   ejercicio en ese día si existe, si no lo planificado.
 */

/** Un entreno realizado, normalmente a partir de una [Routine]. */
data class WorkoutSession(
    /** UUID generado en cliente. */
    val id: String,
    val routineId: String,
    val startedAt: Long = 0L,
    val endedAt: Long? = null,
    /** Segundos acumulados en pausa (el timer no avanza mientras hay pausa). */
    val pausedAccumSec: Int = 0,
    /** Segundos ya contados del cronómetro (0 = sin empezar). */
    val timerBaseSec: Int = 0,
    /** Marca de arranque en ms (null = parado); snapshot del reloj real. */
    val runningSince: Long? = null,
    val updatedAt: Long = 0L,
    val deleted: Boolean = false
)

/** Estado del cronómetro de una sesión. */
enum class TimerState {
    /** Nunca se pulsó Comenzar (base 0, sin arranque). */
    SIN_EMPEZAR,
    /** Estaba corriendo y se paró: tiempo congelado en timerBaseSec. */
    EN_PAUSA,
    /** Corriendo: el reloj avanza desde runningSince. */
    ACTIVO
}

/**
 * Segundos transcurridos del cronómetro en [nowMs].
 * Fórmula del spec 022: `timerBaseSec + (now - runningSince) / 1000`
 * mientras corre; `timerBaseSec` cuando está parado.
 */
fun WorkoutSession.timerElapsedSec(nowMs: Long): Int =
    if (runningSince != null) timerBaseSec + ((nowMs - runningSince) / 1000).toInt()
    else timerBaseSec.coerceAtLeast(0)

/**
 * Estado derivado del cronómetro (puro, sin lecturas de DB).
 * ACTIVO si `runningSince != null`; EN PAUSA si `timerBaseSec > 0` y
 * `runningSince == null`; SIN EMPEZAR si `timerBaseSec == 0` y
 * `runningSince == null`.
 */
val WorkoutSession.timerState: TimerState
    get() = when {
        runningSince != null -> TimerState.ACTIVO
        timerBaseSec > 0 -> TimerState.EN_PAUSA
        else -> TimerState.SIN_EMPEZAR
    }

/** Una serie realizada dentro de una sesión. */
data class SetEntry(
    /** UUID generado en cliente. */
    val id: String,
    val sessionId: String,
    /** Id de catálogo `músculo/slug` o UUID de propio. */
    val exerciseId: String,
    /** De qué serie planificada salió, si venía de una rutina. */
    val plannedSetId: String? = null,
    val setNumber: Int,
    /** Repeticiones realizadas (prefill: última sesión o planificado). */
    val reps: Int = 0,
    /** Con signo: `+` lastre, `0` corporal, `-` ayuda. */
    val weightKg: Double = 0.0,
    val loadNote: String? = null,
    /** Descanso aplicado de ESTA serie (nunca global del ejercicio). */
    val restSeconds: Int? = null,
    val done: Boolean = false,
    val updatedAt: Long = 0L,
    val deleted: Boolean = false
)

/**
 * Fila del acordeón de la sesión: ejercicio de la rutina resuelto + sus
 * entradas en vivo. `expanded` es solo estado de pantalla.
 */
data class WorkoutExerciseUi(
    val routineExercise: RoutineExercise,
    val exercise: Exercise,
    val entries: List<SetEntry> = emptyList(),
    /** Planificado para la columna OBJETIVO (la entrada viva lleva el prefill). */
    val planned: List<PlannedSet> = emptyList(),
    val expanded: Boolean = true
)

/**
 * Estado UI de la sesión en vivo (Fase A en memoria).
 * El repo fake expone este tipo; el futuro repositorio real mantendrá la firma.
 */
data class WorkoutUiState(
    val sessionId: String = "",
    val routineName: String = "",
    val elapsedSec: Int = 0,
    // Spec 013 paso 5: el primer fotograma también es coherente (00:00 +
    // Comenzar + EN PAUSA): el valor inicial ya arranca detenido.
    val isPaused: Boolean = true,
    val exercises: List<WorkoutExerciseUi> = emptyList(),
    val doneCount: Int = 0,
    val totalCount: Int = 0,
    /** Cuenta atrás del descanso flotante; null = sin descanso visible. */
    val restRemainingSec: Int? = null,
    val restTotalSec: Int = 0,
    val isSaving: Boolean = false
) {
    val progressFraction: Float
        get() = if (totalCount <= 0) 0f else doneCount.toFloat() / totalCount.toFloat()

    /**
     * Ejercicio actual (1-based) para `Ejercicio X de N`: el primero con
     * alguna serie sin completar; si todo está hecho o no hay, N.
     */
    val currentExerciseIndex: Int
        get() {
            if (exercises.isEmpty()) return 0
            val firstPending = exercises.indexOfFirst { ex ->
                ex.entries.any { !it.done }
            }
            return if (firstPending == -1) exercises.size else firstPending + 1
        }
}
