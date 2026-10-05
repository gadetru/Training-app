package com.mytrainingplan.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Serie planificada (Paso 1 spec 006, Fase B).
 * Espejo de [com.mytrainingplan.app.domain.model.PlannedSet].
 * `weightKg` con signo (+ lastre, 0 corporal, - ayuda), `loadNote` libre,
 * `restSeconds` por serie. Incluye `rir` del dominio (null = calentamiento,
 * 0 = al fallo, n = RIR n).
 */
@Entity(tableName = "planned_sets")
data class PlannedSetEntity(
    @PrimaryKey val id: String,
    val routineExerciseId: String = "",
    val setNumber: Int = 1,
    val targetReps: Int = 0,
    val weightKg: Double = 0.0,
    val restSeconds: Int = 90,
    val loadNote: String? = null,
    val rir: Int? = null,
    val updatedAt: Long = 0L,
    val deleted: Boolean = false
)
