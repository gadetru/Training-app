package com.mytrainingplan.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Ejercicio de catálogo o propio (Paso 1 spec 006, Fase B).
 * Espejo de [com.mytrainingplan.app.domain.model.Exercise].
 *
 * - `id` conserva `músculo/slug` del catálogo, UUID en propios.
 * - `source` = `CATALOG`/`CUSTOM` (solo `CATALOG` se pisa en sync).
 * - `secondaryMuscles` requerirá `TypeConverter` JSON (paso 3).
 */
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val slug: String = "",
    val name: String = "",
    val muscle: String = "",
    val bodyPart: String = "",
    val equipment: String = "",
    val category: String = "",
    val secondaryMuscles: List<String> = emptyList(),
    /** Pasos ES del catálogo (spec 010); reutiliza el conversor JSON actual. */
    val instructions: List<String> = emptyList(),
    val gifUrl: String = "",
    val source: String = "CATALOG",
    val updatedAt: Long = 0L,
    val deleted: Boolean = false
)
