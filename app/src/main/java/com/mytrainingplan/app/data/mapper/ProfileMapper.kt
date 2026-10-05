package com.mytrainingplan.app.data.mapper

import com.mytrainingplan.app.data.local.entity.ProfileEntity
import com.mytrainingplan.app.domain.model.Profile
import com.mytrainingplan.app.domain.model.TrainingGoal
import com.mytrainingplan.app.domain.model.TrainingLevel

/**
 * Conversión Perfil (Paso 4 spec 006).
 * La UI solo ve `domain/model`; Room solo ve `Entity`.
 */
fun ProfileEntity.toDomain(): Profile = Profile(
    id = id,
    displayName = displayName,
    age = age,
    heightCm = heightCm,
    weightKg = weightKg,
    level = runCatching { TrainingLevel.valueOf(level) }.getOrDefault(TrainingLevel.Default),
    goal = runCatching { TrainingGoal.valueOf(goal) }.getOrDefault(TrainingGoal.Default),
    avatarUri = avatarUri,
    updatedAt = updatedAt,
    deleted = deleted
)

fun Profile.toEntity(now: Long = System.currentTimeMillis()): ProfileEntity = ProfileEntity(
    id = id,
    displayName = displayName,
    age = age,
    heightCm = heightCm,
    weightKg = weightKg,
    level = level.name,
    goal = goal.name,
    avatarUri = avatarUri,
    updatedAt = now,
    deleted = deleted
)
