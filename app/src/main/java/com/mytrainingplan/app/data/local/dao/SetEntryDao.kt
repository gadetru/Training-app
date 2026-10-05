package com.mytrainingplan.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mytrainingplan.app.data.local.entity.SetEntryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Acceso a series realizadas (Paso 2 spec 006).
 * Alimenta el prefill última-vs-plan (`docs/MODELO_DE_DATOS.md:86`).
 */
@Dao
interface SetEntryDao {
    @Query("SELECT * FROM set_entries WHERE sessionId = :sessionId AND deleted = 0 ORDER BY setNumber")
    fun observeBySession(sessionId: String): Flow<List<SetEntryEntity>>

    @Query("SELECT * FROM set_entries WHERE sessionId IN (SELECT id FROM workout_sessions WHERE routineId = :routineId AND deleted = 0) AND exerciseId = :exerciseId AND deleted = 0 ORDER BY setNumber")
    fun observeLastByRoutineExercise(routineId: String, exerciseId: String): Flow<List<SetEntryEntity>>

    @Upsert
    suspend fun upsert(entry: SetEntryEntity)

    @Upsert
    suspend fun upsertAll(entries: List<SetEntryEntity>)
}
