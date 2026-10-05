package com.mytrainingplan.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Ejercicio dentro de una rutina (Paso 1 spec 006, Fase B).
 * Espejo de [com.mytrainingplan.app.domain.model.RoutineExercise].
 */
@Entity(tableName = "routine_exercises")
data class RoutineExerciseEntity(
    @PrimaryKey val id: String,
    val routineId: String = "",
    val exerciseId: String = "",
    val position: Int = 0,
    val note: String = "",
    val updatedAt: Long = 0L,
    val deleted: Boolean = false
)
