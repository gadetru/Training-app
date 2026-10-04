package com.mytrainingplan.app.data.local.entity

/**
 * Ejercicio dentro de una rutina (Paso 1 spec 006, Fase B).
 * Espejo de [com.mytrainingplan.app.domain.model.RoutineExercise].
 *
 * TODO Fase B: `@Entity(tableName = "routine_exercises")` + `@PrimaryKey`
 * cuando se apruebe Room (Context7
 * `/websites/developer_android_training_data-storage_room`
 * https://developer.android.com/training/data-storage/room/defining-data).
 */
data class RoutineExerciseEntity(
    val id: String,
    val routineId: String = "",
    val exerciseId: String = "",
    val position: Int = 0,
    val note: String = "",
    val updatedAt: Long = 0L,
    val deleted: Boolean = false
)
