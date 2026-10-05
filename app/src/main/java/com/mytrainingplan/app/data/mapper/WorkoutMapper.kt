package com.mytrainingplan.app.data.mapper

import com.mytrainingplan.app.data.local.entity.SetEntryEntity
import com.mytrainingplan.app.data.local.entity.WorkoutSessionEntity
import com.mytrainingplan.app.domain.model.SetEntry
import com.mytrainingplan.app.domain.model.WorkoutSession

/**
 * Conversión Sesiones (Paso 4 spec 006).
 * `done` se persiste (parte de "lo hecho"); `expanded`, pausa del
 * cronómetro y celdas tocadas no se persisten.
 */
fun WorkoutSessionEntity.toDomain(): WorkoutSession = WorkoutSession(
    id = id,
    routineId = routineId,
    startedAt = startedAt,
    endedAt = endedAt,
    pausedAccumSec = pausedAccumSec,
    updatedAt = updatedAt,
    deleted = deleted
)

fun WorkoutSession.toEntity(now: Long = System.currentTimeMillis()): WorkoutSessionEntity =
    WorkoutSessionEntity(
        id = id,
        routineId = routineId,
        startedAt = startedAt,
        endedAt = endedAt,
        pausedAccumSec = pausedAccumSec,
        updatedAt = now,
        deleted = deleted
    )

fun SetEntryEntity.toDomain(): SetEntry = SetEntry(
    id = id,
    sessionId = sessionId,
    exerciseId = exerciseId,
    plannedSetId = plannedSetId,
    setNumber = setNumber,
    reps = reps,
    weightKg = weightKg,
    loadNote = loadNote,
    restSeconds = restSeconds,
    done = done,
    updatedAt = updatedAt,
    deleted = deleted
)

fun SetEntry.toEntity(now: Long = System.currentTimeMillis()): SetEntryEntity = SetEntryEntity(
    id = id,
    sessionId = sessionId,
    exerciseId = exerciseId,
    plannedSetId = plannedSetId,
    setNumber = setNumber,
    reps = reps,
    weightKg = weightKg,
    loadNote = loadNote,
    restSeconds = restSeconds,
    done = done,
    updatedAt = now,
    deleted = deleted
)
