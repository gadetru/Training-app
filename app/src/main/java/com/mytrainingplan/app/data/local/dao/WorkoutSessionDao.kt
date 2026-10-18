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

    /**
     * Finalizadas por rango (Paso 3 spec 015): el hecho, la racha y la semana
     * se deducen de `endedAt != null`. El rango en milisegundos lo calcula el
     * repositorio desde el día local (`yyyy-MM-dd`), nunca SQL con horas.
     */
    @Query("SELECT * FROM workout_sessions WHERE endedAt IS NOT NULL AND deleted = 0 ORDER BY startedAt DESC")
    fun observeFinished(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE endedAt IS NOT NULL AND deleted = 0 AND startedAt >= :fromMillis AND startedAt < :toMillis ORDER BY startedAt DESC")
    fun observeFinishedBetween(fromMillis: Long, toMillis: Long): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE endedAt IS NOT NULL AND deleted = 0 AND startedAt >= :fromMillis AND startedAt < :toMillis ORDER BY startedAt DESC")
    suspend fun getFinishedBetween(fromMillis: Long, toMillis: Long): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_sessions WHERE routineId = :routineId AND endedAt IS NOT NULL AND deleted = 0 ORDER BY startedAt DESC LIMIT 1")
    fun observeLastFinishedByRoutine(routineId: String): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE routineId = :routineId AND endedAt IS NOT NULL AND deleted = 0 ORDER BY startedAt DESC LIMIT 1")
    suspend fun getLastFinishedByRoutine(routineId: String): WorkoutSessionEntity?

    /**
     * Sesiones vivas (endedAt IS NULL, no borradas), más reciente primero.
     * Solo una viva por definición (guardia en el repo).
     */
    @Query("SELECT * FROM workout_sessions WHERE endedAt IS NULL AND deleted = 0 ORDER BY startedAt DESC")
    fun observeLive(): Flow<List<WorkoutSessionEntity>>

    @Upsert
    suspend fun upsert(session: WorkoutSessionEntity)
}
