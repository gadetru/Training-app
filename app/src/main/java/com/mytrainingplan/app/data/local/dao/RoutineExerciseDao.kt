package com.mytrainingplan.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mytrainingplan.app.data.local.entity.RoutineExerciseEntity
import kotlinx.coroutines.flow.Flow

/**
 * Acceso a ejercicios de rutina (Paso 2 spec 006).
 */
@Dao
interface RoutineExerciseDao {
    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId AND deleted = 0 ORDER BY position")
    fun observeByRoutine(routineId: String): Flow<List<RoutineExerciseEntity>>

    @Query("SELECT * FROM routine_exercises WHERE id = :id AND deleted = 0")
    suspend fun getById(id: String): RoutineExerciseEntity?

    @Upsert
    suspend fun upsert(item: RoutineExerciseEntity)

    @Upsert
    suspend fun upsertAll(items: List<RoutineExerciseEntity>)
}
