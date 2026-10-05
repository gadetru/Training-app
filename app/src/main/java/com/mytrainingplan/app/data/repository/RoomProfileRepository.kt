package com.mytrainingplan.app.data.repository

import com.mytrainingplan.app.data.local.dao.ProfileDao
import com.mytrainingplan.app.data.mapper.toDomain
import com.mytrainingplan.app.data.mapper.toEntity
import com.mytrainingplan.app.domain.model.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repo real de perfil (Paso 5 spec 006).
 * Misma forma observable que la UI espera; fila única `me` en Room.
 */
interface ProfileRepository {
    fun observeProfile(): Flow<Profile?>
    suspend fun save(profile: Profile)
}

class RoomProfileRepository(
    private val dao: ProfileDao
) : ProfileRepository {
    override fun observeProfile(): Flow<Profile?> =
        dao.observeById("me").map { it?.toDomain() }

    override suspend fun save(profile: Profile) {
        dao.upsert(profile.copy(id = "me").toEntity())
    }
}
