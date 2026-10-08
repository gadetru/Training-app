package com.mytrainingplan.app.data.repository

import com.mytrainingplan.app.domain.model.Exercise
import com.mytrainingplan.app.domain.model.ExerciseFilter
import com.mytrainingplan.app.domain.model.ExerciseSource
import java.text.Normalizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Firma futura del repositorio de ejercicios (Fase B: Room, Fase C: catálogo).
 * El fake de Fase A implementa esta misma firma.
 */
interface ExerciseRepository {
    fun observeExercises(filter: ExerciseFilter): Flow<List<Exercise>>
    suspend fun getById(id: String): Exercise?
}

/**
 * Repo fake en memoria (spec 003, Fase A).
 * Sin Room, sin Hilt, sin Retrofit, sin descarga JSON. Dataset fijo con la
 * forma exacta del JSON real del catálogo (camelCase).
 *
 * URLs: prefijo upstream `JahelCuadrado/ExerciseGymGifsDB@v1.1.0` por jsDelivr
 * (el fork `gadetru` aún sin tag; al taggearlo se sustituye SOLO el prefijo
 * [CATALOG_BASE], la forma no cambia). Entradas de quads verificadas contra
 * `api/es/muscles/quads.json`; el resto sigue el mismo patrón
 * `<base>/<muscle>/<slug>.gif` y Coil muestra placeholder/error si alguna
 * URL no resuelve, sin crash.
 */
class FakeExerciseRepository : ExerciseRepository {

    private val state = MutableStateFlow(SAMPLE_EXERCISES)

    override fun observeExercises(filter: ExerciseFilter): Flow<List<Exercise>> =
        state.asStateFlow().map { list -> list.applyFilter(filter) }

    override suspend fun getById(id: String): Exercise? =
        state.value.find { it.id == id }

    companion object {
        /** Prefijo jsDelivr pineado. Cambiar solo aquí al taggear el fork. */
        const val CATALOG_BASE =
            "https://cdn.jsdelivr.net/gh/JahelCuadrado/ExerciseGymGifsDB@v1.1.0"

        fun defaultExercises(): List<Exercise> = SAMPLE_EXERCISES
    }
}

/** Filtrado en memoria: texto normalizado + intersección músculo/equipamiento. */
internal fun List<Exercise>.applyFilter(filter: ExerciseFilter): List<Exercise> {
    val query = filter.query.normalize()
    return this
        .filter { exercise ->
            if (filter.muscles.isNotEmpty() && exercise.muscle !in filter.muscles) return@filter false
            if (filter.equipment.isNotEmpty() && exercise.equipment !in filter.equipment) return@filter false
            if (query.isBlank()) return@filter true
            exercise.name.normalize().contains(query) ||
                exercise.muscle.normalize().contains(query) ||
                exercise.equipment.normalize().contains(query)
        }
}

/** Minúsculas + sin acentos para búsqueda insensible. */
internal fun String.normalize(): String =
    Normalizer.normalize(this.lowercase(), Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")

private fun gif(muscle: String, slug: String): String =
    "${FakeExerciseRepository.CATALOG_BASE}/$muscle/$slug.gif"

private val SAMPLE_EXERCISES: List<Exercise> = listOf(
    Exercise(
        id = "quads/barbell-bench-squat",
        slug = "barbell-bench-squat",
        name = "Sentadilla con barra",
        muscle = "quads",
        bodyPart = "legs",
        equipment = "barbell",
        category = "strength",
        secondaryMuscles = listOf("glutes", "hamstrings", "calves"),
        gifUrl = gif("quads", "barbell-bench-squat"),
        source = ExerciseSource.CATALOG
    ),
    Exercise(
        id = "quads/lever-alternate-leg-press",
        slug = "lever-alternate-leg-press",
        name = "Prensa de piernas",
        muscle = "quads",
        bodyPart = "legs",
        equipment = "lever",
        category = "strength",
        secondaryMuscles = listOf("glutes", "hamstrings"),
        gifUrl = gif("quads", "lever-alternate-leg-press"),
        source = ExerciseSource.CATALOG
    ),
    Exercise(
        id = "quads/lever-leg-extension",
        slug = "lever-leg-extension",
        name = "Extensión de cuádriceps",
        muscle = "quads",
        bodyPart = "legs",
        equipment = "lever",
        category = "strength",
        secondaryMuscles = listOf("glutes", "hamstrings"),
        gifUrl = gif("quads", "lever-leg-extension"),
        source = ExerciseSource.CATALOG
    ),
    Exercise(
        id = "hamstrings/lying-leg-curl",
        slug = "lying-leg-curl",
        name = "Curl femoral tumbado",
        muscle = "hamstrings",
        bodyPart = "legs",
        equipment = "lever",
        category = "strength",
        secondaryMuscles = listOf("calves", "glutes"),
        gifUrl = gif("hamstrings", "lying-leg-curl"),
        source = ExerciseSource.CATALOG
    ),
    Exercise(
        id = "quads/dumbbell-single-leg-split-squat",
        slug = "dumbbell-single-leg-split-squat",
        name = "Búlgara con mancuernas",
        muscle = "quads",
        bodyPart = "legs",
        equipment = "dumbbell",
        category = "strength",
        secondaryMuscles = listOf("glutes", "hamstrings", "calves"),
        gifUrl = gif("quads", "dumbbell-single-leg-split-squat"),
        source = ExerciseSource.CATALOG
    ),
    Exercise(
        id = "hamstrings/barbell-romanian-deadlift",
        slug = "barbell-romanian-deadlift",
        name = "Peso muerto rumano",
        muscle = "hamstrings",
        bodyPart = "legs",
        equipment = "barbell",
        category = "strength",
        secondaryMuscles = listOf("glutes", "spine"),
        gifUrl = gif("hamstrings", "barbell-romanian-deadlift"),
        source = ExerciseSource.CATALOG
    ),
    Exercise(
        id = "calves/lever-standing-calf-raise",
        slug = "lever-standing-calf-raise",
        name = "Elevación de talones",
        muscle = "calves",
        bodyPart = "legs",
        equipment = "lever",
        category = "strength",
        secondaryMuscles = emptyList(),
        gifUrl = gif("calves", "lever-standing-calf-raise"),
        source = ExerciseSource.CATALOG
    ),
    Exercise(
        id = "pectorals/barbell-bench-press",
        slug = "barbell-bench-press",
        name = "Press banca",
        muscle = "pectorals",
        bodyPart = "chest",
        equipment = "barbell",
        category = "strength",
        secondaryMuscles = listOf("triceps", "delts"),
        gifUrl = gif("pectorals", "barbell-bench-press"),
        source = ExerciseSource.CATALOG
    ),
    Exercise(
        id = "lats/pull-up",
        slug = "pull-up",
        name = "Dominadas",
        muscle = "lats",
        bodyPart = "back",
        equipment = "bodyweight",
        category = "strength",
        secondaryMuscles = listOf("biceps", "forearms"),
        gifUrl = gif("lats", "pull-up"),
        source = ExerciseSource.CATALOG
    ),
    Exercise(
        id = "biceps/barbell-curl",
        slug = "barbell-curl",
        name = "Curl con barra",
        muscle = "biceps",
        bodyPart = "arms",
        equipment = "barbell",
        category = "strength",
        secondaryMuscles = listOf("forearms"),
        instructions = listOf(
            "De pie, agarra la barra con las manos a la anchura de los hombros.",
            "Flexiona los codos y sube la barra hasta los hombros sin balancear el cuerpo.",
            "Baja despacio hasta estirar los brazos del todo."
        ),
        gifUrl = gif("biceps", "barbell-curl"),
        source = ExerciseSource.CATALOG
    ),
    Exercise(
        id = "delts/barbell-military-press",
        slug = "barbell-military-press",
        name = "Press militar",
        muscle = "delts",
        bodyPart = "shoulders",
        equipment = "barbell",
        category = "strength",
        secondaryMuscles = listOf("triceps"),
        gifUrl = gif("delts", "barbell-military-press"),
        source = ExerciseSource.CATALOG
    ),
    Exercise(
        id = "abs/plank",
        slug = "plank",
        name = "Plancha",
        muscle = "abs",
        bodyPart = "core",
        equipment = "bodyweight",
        category = "strength",
        secondaryMuscles = emptyList(),
        gifUrl = gif("abs", "plank"),
        source = ExerciseSource.CATALOG
    ),
    Exercise(
        id = "quads/dumbbell-goblet-squat",
        slug = "dumbbell-goblet-squat",
        name = "Sentadilla goblet",
        muscle = "quads",
        bodyPart = "legs",
        equipment = "dumbbell",
        category = "strength",
        secondaryMuscles = listOf("glutes", "hamstrings", "calves"),
        gifUrl = gif("quads", "dumbbell-goblet-squat"),
        source = ExerciseSource.CATALOG
    ),
    Exercise(
        id = "quads/resistance-band-leg-extension",
        slug = "resistance-band-leg-extension",
        name = "Extensión con banda",
        muscle = "quads",
        bodyPart = "legs",
        equipment = "band",
        category = "strength",
        secondaryMuscles = listOf("glutes", "hamstrings"),
        gifUrl = gif("quads", "resistance-band-leg-extension"),
        source = ExerciseSource.CATALOG
    ),
    Exercise(
        id = "pectorals/cable-standing-fly",
        slug = "cable-standing-fly",
        name = "Aperturas en polea",
        muscle = "pectorals",
        bodyPart = "chest",
        equipment = "cable",
        category = "strength",
        secondaryMuscles = listOf("delts"),
        gifUrl = gif("pectorals", "cable-standing-fly"),
        source = ExerciseSource.CATALOG
    )
)
