package com.mytrainingplan.app.data.local.entity

/**
 * Fila `me` de perfil (Paso 1 spec 006, Fase B).
 * Espejo de [com.mytrainingplan.app.domain.model.Profile].
 *
 * TODO Fase B: anotar `@Entity(tableName = "profile")` + `@PrimaryKey`
 * cuando se apruebe la dependencia Room (ver Context7
 * `/websites/developer_android_training_data-storage_room`
 * https://developer.android.com/training/data-storage/room/defining-data).
 * Sin anotaciones a propósito para no romper `assembleDebug`
 * sin la dependencia (AGENTS.md: proponer líneas y esperar).
 * `level`/`goal` se guardan como `name` del enum; `updatedAt` + `deleted`
 * = borrado lógico sync-ready.
 */
data class ProfileEntity(
    val id: String = "me",
    val displayName: String = "",
    val age: Int? = null,
    val heightCm: Int? = null,
    val weightKg: Double? = null,
    val level: String = "INTERMEDIO",
    val goal: String = "FUERZA_POTENCIA",
    val avatarUri: String? = null,
    val updatedAt: Long = 0L,
    val deleted: Boolean = false
)
