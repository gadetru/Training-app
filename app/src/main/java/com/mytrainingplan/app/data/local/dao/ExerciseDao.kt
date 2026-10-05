package com.mytrainingplan.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mytrainingplan.app.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

/**
 * Acceso a ejercicios (Paso 2 spec 006).
 * Catálogo y propios comparten tabla con `source` (`CATALOG`/`CUSTOM`).
 * La sync (paso 8) solo toca `source = 'CATALOG'`.
 */
@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE deleted = 0 ORDER BY name")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id AND deleted = 0")
    suspend fun getById(id: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE source = :source AND deleted = 0 ORDER BY name")
    fun observeBySource(source: String): Flow<List<ExerciseEntity>>

    @Upsert
    suspend fun upsert(exercise: ExerciseEntity)

    @Upsert
    suspend fun upsertAll(exercises: List<ExerciseEntity>)
}
