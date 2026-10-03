package com.mytrainingplan.app.domain.model

/**
 * Ejercicio en memoria (Paso 1 spec 003, Fase A).
 * Espejo campo a campo del JSON real del fork `gadetru/ExerciseGymGifsDB`
 * (camelCase: `bodyPart`, `secondaryMuscles`, `gifUrl`) para que Fase B/C
 * lo reutilice sin cambios de forma. Modelo limpio de dominio: sin
 * dependencias Android / Room / Retrofit. La UI solo ve este modelo.
 *
 * - `id` conserva `músculo/slug` del catálogo (ej. `biceps/barbell-curl`).
 * - `source` distingue catálogo de propios desde el día uno.
 * - `updatedAt` + `deleted` = borrado lógico sync-ready (Fase 2).
 */
enum class ExerciseSource {
    CATALOG,
    CUSTOM
}

data class Exercise(
    /** Id de catálogo `músculo/slug` (ej. `quads/barbell-squat`). */
    val id: String,
    val slug: String,
    val name: String,
    /** Slug de músculo API (ej. `quads`, `pectorals`, `lats`). */
    val muscle: String,
    /** Parte corporal API (arms/legs/chest/back/core/shoulders/cardio). */
    val bodyPart: String,
    /** Equipamiento API (barbell/dumbbell/cable/machine/bodyweight/...). */
    val equipment: String,
    /** Categoría API (strength/stretching/cardio/plyometrics). */
    val category: String,
    val secondaryMuscles: List<String> = emptyList(),
    val gifUrl: String = "",
    val source: ExerciseSource = ExerciseSource.CATALOG,
    // Sync-ready Fase B: toda entidad de usuario llevará estos campos.
    val updatedAt: Long = 0L,
    val deleted: Boolean = false
)

/**
 * Grupos ES de la fila de chips de músculo → slugs API (spec 003 §4).
 * La clave es la que guarda [ExercisesUiState.selectedMuscle] (null = `Todos`).
 */
object MuscleGroups {
    val GROUPS: Map<String, Set<String>> = mapOf(
        "pierna" to setOf("quads", "hamstrings", "glutes", "calves", "abductors", "adductors"),
        "pecho" to setOf("pectorals"),
        "espalda" to setOf("lats", "traps", "upper-back", "spine"),
        "hombros" to setOf("delts"),
        "core" to setOf("abs"),
        "brazo" to setOf("biceps", "triceps", "forearms")
    )

    fun slugsFor(key: String?): Set<String> =
        key?.let { GROUPS[it] ?: setOf(it) } ?: emptySet()
}

/**
 * Grupos ES de la fila de chips de equipamiento → valores API (spec 003 §4).
 * `maquina` cubre `machine` + `lever` (palanca); null = `Todos`.
 */
object EquipmentGroups {
    val GROUPS: Map<String, Set<String>> = mapOf(
        "barra" to setOf("barbell"),
        "mancuernas" to setOf("dumbbell"),
        "maquina" to setOf("machine", "lever"),
        "corporal" to setOf("bodyweight"),
        "polea" to setOf("cable"),
        "banda" to setOf("band")
    )

    fun slugsFor(key: String?): Set<String> =
        key?.let { GROUPS[it] ?: setOf(it) } ?: emptySet()
}

/**
 * Filtro en memoria (Fase A). En Fase B/C el repositorio real
 * mantendrá esta misma firma.
 */
data class ExerciseFilter(
    val query: String = "",
    val muscles: Set<String> = emptySet(),
    val equipment: Set<String> = emptySet()
)

/**
 * Estado UI del selector de ejercicios (Fase A en memoria).
 * Selección única por fila de chips (null = `Todos`); el repo fake
 * expone este tipo y el futuro repositorio real mantendrá la firma.
 */
data class ExercisesUiState(
    val query: String = "",
    val selectedMuscle: String? = null,
    val selectedEquipment: String? = null,
    val results: List<Exercise> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val isLoading: Boolean = false
) {
    val selectedCount: Int
        get() = selectedIds.size

    /** Vista como [ExerciseFilter] para el repositorio (expande grupos ES). */
    val filter: ExerciseFilter
        get() = ExerciseFilter(
            query = query,
            muscles = MuscleGroups.slugsFor(selectedMuscle),
            equipment = EquipmentGroups.slugsFor(selectedEquipment)
        )
}
