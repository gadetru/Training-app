package com.mytrainingplan.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Fila `me` de perfil (Paso 1 spec 006, Fase B).
 * Espejo de [com.mytrainingplan.app.domain.model.Profile].
 *
 * `level`/`goal` se guardan como `name` del enum; `updatedAt` + `deleted`
 * = borrado lógico sync-ready.
 */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: String = "me",
    val displayName: String = "",
    val age: Int? = null,
    val heightCm: Int? = null,
    val weightKg: Double? = null,
    val level: String = "INTERMEDIO",
    val goal: String = "FUERZA_POTENCIA",
    val avatarUri: String? = null,
    val updatedAt: Long = 0L,
    val deleted: Boolean = false
)
