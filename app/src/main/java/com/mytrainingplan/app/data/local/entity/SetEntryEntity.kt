package com.mytrainingplan.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Serie realizada (Paso 1 spec 006, Fase B).
 * Espejo de [com.mytrainingplan.app.domain.model.SetEntry].
 * Incluye `done` del dominio (parte de "lo hecho"; `expanded`/`handEdited`/
 * pausa del cronómetro siguen solo en pantalla y no se persisten).
 */
@Entity(tableName = "set_entries")
data class SetEntryEntity(
    @PrimaryKey val id: String,
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
