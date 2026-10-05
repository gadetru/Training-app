package com.mytrainingplan.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mytrainingplan.app.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * Acceso a perfil (Paso 2 spec 006).
 * Solo `@Upsert`, nunca borrado-y-recreado. Observación por [Flow].
 * Tabla `profile` (futura `@Entity` del paso 1 pendiente de anotar).
 */
@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = :id AND deleted = 0")
    fun observeById(id: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = :id AND deleted = 0")
    suspend fun getById(id: String): ProfileEntity?

    @Upsert
    suspend fun upsert(profile: ProfileEntity)
}
