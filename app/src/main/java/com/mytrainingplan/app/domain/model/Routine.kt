package com.mytrainingplan.app.domain.model

/**
 * Rutina en memoria (Paso 1 spec 004, Fase A).
 * Modelos limpios de dominio: sin dependencias Android / Room / Retrofit.
 * La UI solo ve estos modelos.
 *
 * - IDs de usuario = UUID generados en cliente; ejercicios de catálogo
 *   conservan su `id` (`músculo/slug`, ej. `biceps/barbell-curl`).
 * - Toda entidad de usuario lleva `updatedAt` + `deleted` (borrado lógico,
 *   sync-ready Fase 2) desde el día uno.
 * - `weightKg` con signo (`+` lastre, `0` corporal, `-` ayuda) + `loadNote`
 *   libre; `restSeconds` vive en cada serie, no en el ejercicio.
 * - `handEdited` es solo estado de pantalla (qué celdas tocó el usuario),
 *   jamás se persiste (ni siquiera en el fake).
 */
/**
 * Intensidad RIR por serie (referencia `plantilla-editar-rutina/code.html`):
 * `null` = calentamiento (W), `0` = al fallo, `n > 0` = RIR n.
 * Un solo nullable cubre los 3 casos y mapea directo a una columna Room en Fase B.
 */

data class Routine(
    /** UUID generado en cliente. */
    val id: String,
    val name: String,
    val durationMin: Int,
    val position: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deleted: Boolean = false
)

data class RoutineExercise(
    /** UUID generado en cliente. */
    val id: String,
    val routineId: String,
    /** Id de catálogo `músculo/slug` o UUID de propio. */
    val exerciseId: String,
    val position: Int = 0,
    val note: String = "",
    val updatedAt: Long = 0L,
    val deleted: Boolean = false
)

data class PlannedSet(
    /** UUID generado en cliente. */
    val id: String,
    val routineExerciseId: String,
    val setNumber: Int,
    val targetReps: Int = 0,
    /** Con signo: `+` lastre, `0` corporal, `-` ayuda. */
    val weightKg: Double = 0.0,
    /** Descanso de ESTA serie (nunca global del ejercicio). */
    val restSeconds: Int = 90,
    val loadNote: String? = null,
    /** RIR de ESTA serie: `null` calentamiento, `0` al fallo, `n` RIR n. */
    val rir: Int? = null,
    val updatedAt: Long = 0L,
    val deleted: Boolean = false
)

/**
 * Fila del acordeón del constructor: ejercicio resuelto + sus series.
 * `handEdited` guarda claves `"${setId}:${campo}"` (ej. `"uuid:targetReps"`)
 * solo para la propagación de pantalla (`docs/MODELO_DE_DATOS.md:81-87`).
 */
data class RoutineExerciseUi(
    val routineExercise: RoutineExercise,
    val exercise: Exercise,
    val sets: List<PlannedSet> = emptyList(),
    val expanded: Boolean = true,
    val handEdited: Set<String> = emptySet()
)

/**
 * Estado UI del constructor de rutina (Fase A en memoria).
 * El repo fake expone este tipo; el futuro repositorio real mantendrá la firma.
 */
data class RoutineEditUiState(
    val routineId: String = "",
    val name: String = "",
    val durationMin: Int = 45,
    val tags: List<String> = emptyList(),
    val exercises: List<RoutineExerciseUi> = emptyList(),
    val isSaving: Boolean = false
)

/**
 * Etiquetas ES por slug de músculo API (espejo de `MUSCLE_LABELS` privado de
 * `ExercisePickerSheet.kt`, que no se toca en este spec). Se duplica aquí
 * para que la UI solo vea `domain/model` al calcular los tags automáticos.
 */
object RoutineMuscleLabels {
    val LABELS: Map<String, String> = mapOf(
        "quads" to "Pierna",
        "hamstrings" to "Pierna",
        "glutes" to "Pierna",
        "calves" to "Pierna",
        "abductors" to "Pierna",
        "adductors" to "Pierna",
        "pectorals" to "Pecho",
        "lats" to "Espalda",
        "traps" to "Espalda",
        "upper-back" to "Espalda",
        "spine" to "Espalda",
        "delts" to "Hombros",
        "abs" to "Core",
        "biceps" to "Brazo",
        "triceps" to "Brazo",
        "forearms" to "Brazo"
    )

    fun labelFor(muscleSlug: String): String = LABELS[muscleSlug] ?: muscleSlug

    /** Tags automáticos del constructor: músculos únicos en orden de aparición. */
    fun tagsFor(exercises: List<Exercise>): List<String> {
        val seen = LinkedHashSet<String>()
        exercises.forEach { seen.add(labelFor(it.muscle)) }
        return seen.toList()
    }
}
