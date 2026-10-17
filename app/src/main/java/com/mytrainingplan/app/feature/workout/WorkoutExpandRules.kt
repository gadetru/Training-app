package com.mytrainingplan.app.feature.workout

import com.mytrainingplan.app.data.repository.WorkoutSessionItem

/**
 * Reglas puras del colapsado de sesión (spec 019, JVM-testable sin Main).
 * El [WorkoutViewModel] delega aquí; la UI solo ve el mapa `expanded`
 * resultante (estado solo de pantalla, no se persiste).
 */
object WorkoutExpandRules {

    /** Spec 019 paso 1: todos colapsados al abrir, sin excepción. */
    fun defaultExpanded(expanded: Map<String, Boolean>, routineExerciseId: String): Boolean =
        expanded[routineExerciseId] ?: false

    /**
     * Spec 013 paso 4 + 019 paso 1: niega el estado EFECTIVO (toque previo o
     * defecto colapsado). Un tap re-expande un auto-colapsado sin coste extra.
     */
    fun toggle(expanded: Map<String, Boolean>, routineExerciseId: String): Map<String, Boolean> =
        expanded + (routineExerciseId to !(expanded[routineExerciseId] ?: false))

    /**
     * Spec 019 paso 2: mapa tras cerrarse el descanso de [finishedEntryId]
     * (a 0, con Terminar, o al marcar última serie sin descanso). Si el
     * ejercicio queda sin pendientes: colapsa ese y expande el primer
     * pendiente siguiente en orden; si no quedan, solo colapsa. Con
     * pendientes restantes o id desconocido devuelve null (sin cambios).
     * La serie disparadora se cuenta como hecha (el flow puede ir con
     * retardo tras `toggleSetDone`).
     */
    fun afterClose(
        items: List<WorkoutSessionItem>,
        expanded: Map<String, Boolean>,
        finishedEntryId: String
    ): Map<String, Boolean>? {
        val finishedIndex = items.indexOfFirst { item ->
            item.entries.any { it.id == finishedEntryId }
        }
        if (finishedIndex == -1) return null
        val stillPending = items[finishedIndex].entries.any { e ->
            e.id != finishedEntryId && !e.done
        }
        if (stillPending) return null
        val finishedRoutineId = items[finishedIndex].routineExercise.id
        var nextRoutineId: String? = null
        for (i in finishedIndex + 1 until items.size) {
            if (items[i].entries.any { !it.done }) {
                nextRoutineId = items[i].routineExercise.id
                break
            }
        }
        var next = expanded + (finishedRoutineId to false)
        if (nextRoutineId != null) next = next + (nextRoutineId to true)
        return next
    }
}
