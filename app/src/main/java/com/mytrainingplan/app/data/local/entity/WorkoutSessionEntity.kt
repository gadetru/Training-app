package com.mytrainingplan.app.data.local.entity

/**
 * Sesión realizada (Paso 1 spec 006, Fase B).
 * Espejo de [com.mytrainingplan.app.domain.model.WorkoutSession].
 * Incluye `pausedAccumSec` del dominio (docs/MODELO_DE_DATOS.md lo omite,
 * se sigue al código según AGENTS.md). `routineId` no-nulo como en dominio.
 *
 * TODO Fase B: `@Entity(tableName = "workout_sessions")` + `@PrimaryKey`
 * cuando se apruebe Room (Context7
 * `/websites/developer_android_training_data-storage_room`
 * https://developer.android.com/training/data-storage/room/defining-data).
 */
data class WorkoutSessionEntity(
    val id: String,
    val routineId: String = "",
    val startedAt: Long = 0L,
    val endedAt: Long? = null,
    val pausedAccumSec: Int = 0,
    val updatedAt: Long = 0L,
    val deleted: Boolean = false
)
