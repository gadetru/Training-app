package com.mytrainingplan.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Sesión realizada (Paso 1 spec 006, Fase B).
 * Espejo de [com.mytrainingplan.app.domain.model.WorkoutSession].
 * Incluye `pausedAccumSec` del dominio (docs/MODELO_DE_DATOS.md lo omite,
 * se sigue al código según AGENTS.md). `routineId` no-nulo como en dominio.
 * v4 spec 022: `timerBaseSec` (segundos contados) + `runningSince` (marca
 * de arranque en ms, null = parado) para el cronómetro persistente.
 */
@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val routineId: String = "",
    val startedAt: Long = 0L,
    val endedAt: Long? = null,
    /** Segundos acumulados en pausa (histórico; el timer vivo usa timerBaseSec). */
    val pausedAccumSec: Int = 0,
    /** Segundos ya contados del cronómetro (0 = sin empezar). */
    val timerBaseSec: Int = 0,
    /** Marca de arranque en ms (null = parado). Snapshot del reloj real. */
    val runningSince: Long? = null,
    val updatedAt: Long = 0L,
    val deleted: Boolean = false
)
