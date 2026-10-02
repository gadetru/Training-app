package com.mytrainingplan.app.domain.model

/**
 * Home en memoria (Paso 1 spec 002, Fase A).
 * Modelos limpios de dominio: sin dependencias Android / Room / Retrofit.
 * La UI solo ve estos modelos.
 */

/** Color de accent bar lateral de cada routine card. */
enum class AccentColor {
    ORANGE,
    VOLT,
    CYAN
}

/** Tipo de nota en la bottom-bar de la card. */
enum class FootKind {
    /** Récord / PR (texto volt). */
    PR,
    /** Nota informativa (texto muted). */
    INFO
}

data class RoutineSummary(
    val id: String,
    val title: String,
    val tags: List<String>,
    val durationMin: Int,
    val exerciseCount: Int,
    /** Etiqueta "Hace 2 días" / "Ayer" / null si no aplica. */
    val lastDoneLabel: String? = null,
    val accent: AccentColor = AccentColor.ORANGE,
    val footNote: String = "",
    val footKind: FootKind = FootKind.INFO,
    // Sync-ready Fase B: toda entidad de usuario llevará estos campos.
    val updatedAt: Long = 0L,
    val deleted: Boolean = false
)

data class WeeklyProgress(
    val streakDays: Int = 4,
    val sessionsDone: Int = 3,
    val sessionsGoal: Int = 4,
    val minutes: Int = 185,
    val volumeDeltaPct: Int = 12,
    /** L–D, true = día entrenado. Fijo Fase A: L–J ✓. */
    val weekChecks: List<Boolean> = listOf(true, true, true, true, false, false, false)
) {
    val progressFraction: Float
        get() = if (sessionsGoal <= 0) 0f else sessionsDone.toFloat() / sessionsGoal.toFloat()
}

/**
 * Estado UI de la Home (Fase A en memoria).
 * El repo fake expone este tipo; el futuro repositorio real mantendrá la firma.
 */
data class HomeUiState(
    val firstName: String = "Carlos",
    val routines: List<RoutineSummary> = emptyList(),
    val progress: WeeklyProgress = WeeklyProgress(),
    val isLoading: Boolean = false
) {
    val greeting: String
        get() = "Hola, $firstName"
}
