package com.mytrainingplan.app.data.local.entity

/**
 * Serie realizada (Paso 1 spec 006, Fase B).
 * Espejo de [com.mytrainingplan.app.domain.model.SetEntry].
 * Incluye `done` del dominio (parte de "lo hecho"; `expanded`/`handEdited`/
 * pausa del cronómetro siguen solo en pantalla y no se persisten).
 *
 * TODO Fase B: `@Entity(tableName = "set_entries")` + `@PrimaryKey`
 * cuando se apruebe Room (Context7
 * `/websites/developer_android_training_data-storage_room`
 * https://developer.android.com/training/data-storage/room/defining-data).
 */
data class SetEntryEntity(
    val id: String,
    val sessionId: String = "",
    val exerciseId: String = "",
    val plannedSetId: String? = null,
    val setNumber: Int = 1,
    val reps: Int = 0,
    val weightKg: Double = 0.0,
    val loadNote: String? = null,
    val restSeconds: Int? = null,
    val done: Boolean = false,
    val updatedAt: Long = 0L,
    val deleted: Boolean = false
)
