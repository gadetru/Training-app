package com.mytrainingplan.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mytrainingplan.app.data.local.entity.RoutineEntity
import kotlinx.coroutines.flow.Flow

/**
 * Acceso a rutinas (Paso 2 spec 006).
 * El feed de Home solo ve no-borradas ordenadas por `position`.
 */
@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines WHERE deleted = 0 ORDER BY position")
    fun observeAll(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE id = :id AND deleted = 0")
    fun observeById(id: String): Flow<RoutineEntity?>

    @Query("SELECT * FROM routines WHERE id = :id AND deleted = 0")
    suspend fun getById(id: String): RoutineEntity?

    /**
     * Todas incluidas borradas (Paso 5 spec 015): el historial de una rutina
     * borrada sobrevive y el mes resuelve su nombre aunque ya no esté en el
     * feed. Los apuntes futuros de borradas se ocultan en el repositorio.
     */
    @Query("SELECT * FROM routines")
    fun observeAllIncludingDeleted(): Flow<List<RoutineEntity>>

    @Upsert
    suspend fun upsert(routine: RoutineEntity)
}
