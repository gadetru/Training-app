package com.mytrainingplan.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mytrainingplan.app.data.local.entity.PlannedSetEntity
import kotlinx.coroutines.flow.Flow

/**
 * Acceso a series planificadas (Paso 2 spec 006).
 * `weightKg` con signo, `restSeconds` por serie, `loadNote` libre.
 */
@Dao
interface PlannedSetDao {
    @Query("SELECT * FROM planned_sets WHERE routineExerciseId = :routineExerciseId AND deleted = 0 ORDER BY setNumber")
    fun observeByExercise(routineExerciseId: String): Flow<List<PlannedSetEntity>>

    @Upsert
    suspend fun upsert(set: PlannedSetEntity)

    @Upsert
    suspend fun upsertAll(sets: List<PlannedSetEntity>)
}
