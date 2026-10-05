package com.mytrainingplan.app.data.repository

import com.mytrainingplan.app.data.local.dao.ExerciseDao
import com.mytrainingplan.app.data.mapper.toDomain
import com.mytrainingplan.app.domain.model.Exercise
import com.mytrainingplan.app.domain.model.ExerciseFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repo real de ejercicios (Paso 5 spec 006).
 * Misma firma que [ExerciseRepository]; filtra en memoria sobre lo
 * observado en Room (el fake ya exponía esta firma).
 */
class RoomExerciseRepository(
    private val dao: ExerciseDao
) : ExerciseRepository {
    override fun observeExercises(filter: ExerciseFilter): Flow<List<Exercise>> =
        dao.observeAll().map { list -> list.map { it.toDomain() }.applyFilter(filter) }

    override suspend fun getById(id: String): Exercise? =
        dao.getById(id)?.toDomain()
}
