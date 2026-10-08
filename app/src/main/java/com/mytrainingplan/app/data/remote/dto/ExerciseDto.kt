package com.mytrainingplan.app.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.mytrainingplan.app.domain.model.Exercise
import com.mytrainingplan.app.domain.model.ExerciseSource

/**
 * Espejo del JSON del catálogo (Paso 6 spec 006, Fase B/C; alternates
 * camelCase del spec 007: el JSON real del fork usa `bodyPart`,
 * `secondaryMuscles` y `gifUrl`, no snake_case).
 * La UI nunca ve este tipo (mapeo a `domain/model` aquí mismo, capa data).
 */
data class ExerciseDto(
    @SerializedName("slug") val slug: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("muscle") val muscle: String = "",
    @SerializedName(value = "body_part", alternate = ["bodyPart"]) val bodyPart: String = "",
    @SerializedName("equipment") val equipment: String = "",
    @SerializedName("category") val category: String = "",
    @SerializedName(value = "secondary_muscles", alternate = ["secondaryMuscles"]) val secondaryMuscles: List<String> = emptyList(),
    @SerializedName(value = "gif_url", alternate = ["gifUrl"]) val gifUrl: String = "",
    @SerializedName("instructions") val instructions: List<String> = emptyList()
) {
    /** `id` de catálogo `músculo/slug`, igual que en dominio. */
    val catalogId: String get() = "$muscle/$slug"
}

/**
 * Envoltorio del `api/{lang}/exercises.json` real del fork
 * (`{"count": N, "exercises": [...]}`), spec 007.
 * Las claves extra de cada entrada (`id`, `file`, `thumbUrl`) las ignora
 * Gson; el `id` se deriva como `músculo/slug` en [toDomain].
 */
data class CatalogResponse(
    @SerializedName("exercises") val exercises: List<ExerciseDto> = emptyList(),
    @SerializedName("count") val count: Int = 0
)

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
    instructions = instructions,
    gifUrl = gifUrl,
    source = ExerciseSource.CATALOG,
    updatedAt = now,
    deleted = false
)
