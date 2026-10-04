package com.mytrainingplan.app.data.local.entity

/**
 * Rutina (Paso 1 spec 006, Fase B).
 * Espejo de [com.mytrainingplan.app.domain.model.Routine].
 * Incluye `durationMin` del dominio (docs/MODELO_DE_DATOS.md lo omite,
 * se sigue al código según AGENTS.md).
 *
 * TODO Fase B: `@Entity(tableName = "routines")` + `@PrimaryKey`
 * cuando se apruebe Room (Context7
 * `/websites/developer_android_training_data-storage_room`
 * https://developer.android.com/training/data-storage/room/defining-data).
 */
data class RoutineEntity(
    val id: String,
    val name: String = "",
    val durationMin: Int = 45,
    val position: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deleted: Boolean = false
)
