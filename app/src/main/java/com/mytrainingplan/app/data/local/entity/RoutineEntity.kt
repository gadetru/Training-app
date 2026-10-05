package com.mytrainingplan.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Rutina (Paso 1 spec 006, Fase B).
 * Espejo de [com.mytrainingplan.app.domain.model.Routine].
 * Incluye `durationMin` del dominio (docs/MODELO_DE_DATOS.md lo omite,
 * se sigue al código según AGENTS.md).
 */
@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey val id: String,
    val name: String = "",
    val durationMin: Int = 45,
    val position: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deleted: Boolean = false
)
