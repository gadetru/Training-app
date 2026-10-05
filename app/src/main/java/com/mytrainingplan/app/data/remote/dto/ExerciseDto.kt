package com.mytrainingplan.app.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.mytrainingplan.app.domain.model.Exercise
import com.mytrainingplan.app.domain.model.ExerciseSource

/**
 * Espejo del JSON del catálogo (Paso 6 spec 006, Fase B/C).
 * Nombres con guion bajo tal cual vienen por red (`body_part`, `gif_url`).
 * La UI nunca ve este tipo (mapeo a `domain/model` aquí mismo, capa data).
 */
data class ExerciseDto(
    @SerializedName("slug") val slug: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("muscle") val muscle: String = "",
    @SerializedName("body_part") val bodyPart: String = "",
    @SerializedName("equipment") val equipment: String = "",
    @SerializedName("category") val category: String = "",
    @SerializedName("secondary_muscles") val secondaryMuscles: List<String> = emptyList(),
    @SerializedName("gif_url") val gifUrl: String = "",
    @SerializedName("instructions") val instructions: List<String> = emptyList()
) {
    /** `id` de catálogo `músculo/slug`, igual que en dominio. */
    val catalogId: String get() = "$muscle/$slug"
}

/** DTO -> dominio limpio (el `gifUrl` ya viene absoluto desde jsDelivr). */
fun ExerciseDto.toDomain(now: Long = System.currentTimeMillis()): Exercise = Exercise(
    id = catalogId,
    slug = slug,
    name = name,
    muscle = muscle,
    bodyPart = bodyPart,
    equipment = equipment,
    category = category,
    secondaryMuscles = secondaryMuscles,
    gifUrl = gifUrl,
    source = ExerciseSource.CATALOG,
    updatedAt = now,
    deleted = false
)
