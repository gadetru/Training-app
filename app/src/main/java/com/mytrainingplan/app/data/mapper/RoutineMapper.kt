package com.mytrainingplan.app.data.mapper

import com.mytrainingplan.app.data.local.entity.PlannedSetEntity
import com.mytrainingplan.app.data.local.entity.RoutineEntity
import com.mytrainingplan.app.data.local.entity.RoutineExerciseEntity
import com.mytrainingplan.app.domain.model.PlannedSet
import com.mytrainingplan.app.domain.model.Routine
import com.mytrainingplan.app.domain.model.RoutineExercise

/**
 * Conversión Rutinas (Paso 4 spec 006).
 * `weightKg` con signo y `restSeconds` por serie se copian tal cual;
 * `handEdited`/`expanded` no se persisten (solo pantalla).
 */
fun RoutineEntity.toDomain(): Routine = Routine(
    id = id,
    name = name,
    durationMin = durationMin,
    position = position,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deleted = deleted
)

fun Routine.toEntity(now: Long = System.currentTimeMillis()): RoutineEntity = RoutineEntity(
    id = id,
    name = name,
    durationMin = durationMin,
    position = position,
    createdAt = createdAt,
    updatedAt = now,
    deleted = deleted
)

fun RoutineExerciseEntity.toDomain(): RoutineExercise = RoutineExercise(
    id = id,
    routineId = routineId,
    exerciseId = exerciseId,
    position = position,
    note = note,
    updatedAt = updatedAt,
    deleted = deleted
)

fun RoutineExercise.toEntity(now: Long = System.currentTimeMillis()): RoutineExerciseEntity =
    RoutineExerciseEntity(
        id = id,
        routineId = routineId,
        exerciseId = exerciseId,
        position = position,
        note = note,
        updatedAt = now,
        deleted = deleted
    )

fun PlannedSetEntity.toDomain(): PlannedSet = PlannedSet(
    id = id,
    routineExerciseId = routineExerciseId,
    setNumber = setNumber,
    targetReps = targetReps,
    weightKg = weightKg,
    restSeconds = restSeconds,
    loadNote = loadNote,
    rir = rir,
    updatedAt = updatedAt,
    deleted = deleted
)

fun PlannedSet.toEntity(now: Long = System.currentTimeMillis()): PlannedSetEntity = PlannedSetEntity(
    id = id,
    routineExerciseId = routineExerciseId,
    setNumber = setNumber,
    targetReps = targetReps,
    weightKg = weightKg,
    restSeconds = restSeconds,
    loadNote = loadNote,
    rir = rir,
    updatedAt = now,
    deleted = deleted
)
