package com.mytrainingplan.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Sesión realizada (Paso 1 spec 006, Fase B).
 * Espejo de [com.mytrainingplan.app.domain.model.WorkoutSession].
 * Incluye `pausedAccumSec` del dominio (docs/MODELO_DE_DATOS.md lo omite,
 * se sigue al código según AGENTS.md). `routineId` no-nulo como en dominio.
 */
@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val routineId: String = "",
    val startedAt: Long = 0L,
    val endedAt: Long? = null,
    val pausedAccumSec: Int = 0,
    val updatedAt: Long = 0L,
    val deleted: Boolean = false
)
