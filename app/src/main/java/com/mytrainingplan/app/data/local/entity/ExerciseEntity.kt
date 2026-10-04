package com.mytrainingplan.app.data.local.entity

/**
 * Ejercicio de catálogo o propio (Paso 1 spec 006, Fase B).
 * Espejo de [com.mytrainingplan.app.domain.model.Exercise].
 *
 * - `id` conserva `músculo/slug` del catálogo, UUID en propios.
 * - `source` = `CATALOG`/`CUSTOM` (solo `CATALOG` se pisa en sync).
 * - `secondaryMuscles` requerirá `TypeConverter` JSON (paso 3).
 *
 * TODO Fase B: `@Entity(tableName = "exercises")` + `@PrimaryKey`
 * cuando se apruebe Room (Context7
 * `/websites/developer_android_training_data-storage_room`
 * https://developer.android.com/training/data-storage/room/defining-data).
 */
data class ExerciseEntity(
    val id: String,
    val slug: String = "",
    val name: String = "",
    val muscle: String = "",
    val bodyPart: String = "",
    val equipment: String = "",
    val category: String = "",
    val secondaryMuscles: List<String> = emptyList(),
    val gifUrl: String = "",
    val source: String = "CATALOG",
    val updatedAt: Long = 0L,
    val deleted: Boolean = false
)
