package com.mytrainingplan.app.data.mapper

import com.mytrainingplan.app.data.local.entity.ScheduledDayEntity
import com.mytrainingplan.app.domain.model.ScheduledDay

/**
 * Conversión Apuntes (Paso 5 spec 015).
 * La UI solo ve [ScheduledDay]; Room solo ve [ScheduledDayEntity].
 */
fun ScheduledDayEntity.toDomain(): ScheduledDay = ScheduledDay(
    id = id,
    routineId = routineId,
    date = date,
    updatedAt = updatedAt,
    deleted = deleted
)

fun ScheduledDay.toEntity(now: Long = System.currentTimeMillis()): ScheduledDayEntity =
    ScheduledDayEntity(
        id = id,
        routineId = routineId,
        date = date,
        updatedAt = now,
        deleted = deleted
    )
