package com.mytrainingplan.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mytrainingplan.app.data.local.entity.ScheduledDayEntity
import kotlinx.coroutines.flow.Flow

/**
 * Acceso a apuntes de rutina por día (Paso 3 spec 015).
 * Siempre `@Upsert`, nunca REPLACE. El cambio de apunte no duplica:
 * el repositorio resuelve el activo del día y lo sustituye.
 */
@Dao
interface ScheduledDayDao {
    @Query("SELECT * FROM scheduled_days WHERE deleted = 0 ORDER BY date")
    fun observeAll(): Flow<List<ScheduledDayEntity>>

    @Query("SELECT * FROM scheduled_days WHERE deleted = 0 AND date >= :fromDate AND date < :toDate ORDER BY date")
    fun observeBetween(fromDate: String, toDate: String): Flow<List<ScheduledDayEntity>>

    @Query("SELECT * FROM scheduled_days WHERE date = :date AND deleted = 0 LIMIT 1")
    fun observeByDate(date: String): Flow<ScheduledDayEntity?>

    @Query("SELECT * FROM scheduled_days WHERE date = :date AND deleted = 0 LIMIT 1")
    suspend fun getByDate(date: String): ScheduledDayEntity?

    @Upsert
    suspend fun upsert(day: ScheduledDayEntity)
}
