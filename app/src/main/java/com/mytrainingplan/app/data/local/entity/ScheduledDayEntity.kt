package com.mytrainingplan.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Apunte de rutina a un día (Paso 3 spec 015).
 * Espejo de [com.mytrainingplan.app.domain.model.ScheduledDay].
 * Un apunte por día concreto: `date` en `yyyy-MM-dd` local del dispositivo.
 * Lo "hecho" no se guarda aquí: se deduce de sesiones finalizadas.
 */
@Entity(tableName = "scheduled_days")
data class ScheduledDayEntity(
    @PrimaryKey val id: String,
    val routineId: String = "",
    val date: String = "",
    val updatedAt: Long = 0L,
    val deleted: Boolean = false
)
