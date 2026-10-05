package com.mytrainingplan.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mytrainingplan.app.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Acceso a sesiones (Paso 2 spec 006).
 * Activas vs historial se distinguen por `endedAt` (null = activa).
 */
@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_sessions WHERE id = :id AND deleted = 0")
    fun observeById(id: String): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE routineId = :routineId AND deleted = 0 ORDER BY startedAt DESC")
    fun observeByRoutine(routineId: String): Flow<List<WorkoutSessionEntity>>

    @Upsert
    suspend fun upsert(session: WorkoutSessionEntity)
}
