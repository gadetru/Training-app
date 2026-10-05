package com.mytrainingplan.app.data.mapper

import com.mytrainingplan.app.data.local.entity.ExerciseEntity
import com.mytrainingplan.app.domain.model.Exercise
import com.mytrainingplan.app.domain.model.ExerciseSource

/**
 * Conversión Ejercicios (Paso 4 spec 006).
 * `source` se guarda como `name` del enum; el sync solo pisa `CATALOG`.
 */
fun ExerciseEntity.toDomain(): Exercise = Exercise(
    id = id,
    slug = slug,
    name = name,
    muscle = muscle,
    bodyPart = bodyPart,
    equipment = equipment,
    category = category,
    secondaryMuscles = secondaryMuscles,
    gifUrl = gifUrl,
    source = runCatching { ExerciseSource.valueOf(source) }.getOrDefault(ExerciseSource.CATALOG),
    updatedAt = updatedAt,
    deleted = deleted
)

fun Exercise.toEntity(now: Long = System.currentTimeMillis()): ExerciseEntity = ExerciseEntity(
    id = id,
    slug = slug,
    name = name,
    muscle = muscle,
    bodyPart = bodyPart,
    equipment = equipment,
    category = category,
    secondaryMuscles = secondaryMuscles,
    gifUrl = gifUrl,
    source = source.name,
    updatedAt = now,
    deleted = deleted
)
