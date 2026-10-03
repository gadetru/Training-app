package com.mytrainingplan.app.data.repository

import com.mytrainingplan.app.domain.model.Exercise
import com.mytrainingplan.app.domain.model.PlannedSet
import com.mytrainingplan.app.domain.model.Routine
import com.mytrainingplan.app.domain.model.RoutineExercise
import com.mytrainingplan.app.domain.model.SetType
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Detalle de un ejercicio dentro del constructor: fila de rutina resuelta
 * con su [Exercise] de catálogo + series planificadas. Solo compone modelos
 * de `domain/model`; la UI lo verá mapeado a `RoutineExerciseUi` en el
 * ViewModel (que añade `expanded` + `handEdited`, solo pantalla).
 */
data class RoutineExerciseDetail(
    val routineExercise: RoutineExercise,
    val exercise: Exercise,
    val sets: List<PlannedSet> = emptyList()
)

/**
 * Rutina con sus ejercicios resueltos (borrador o guardada).
 * El futuro repositorio real (Room) devolverá esta misma forma.
 */
data class RoutineDetail(
    val routine: Routine,
    val items: List<RoutineExerciseDetail> = emptyList()
)

/**
 * Firma futura del repositorio de rutinas (Fase B: Room).
 * El fake de Fase A implementa esta misma firma.
 */
interface RoutineRepository {
    /** Solo rutinas guardadas (el feed de Home nunca ve borradores). */
    fun observeRoutines(): Flow<List<Routine>>

    /** Detalles guardados (para mapear el feed con tags/nº reales). */
    fun observeDetails(): Flow<List<RoutineDetail>>

    /** Borrador en edición o guardada (para el constructor). */
    fun observeRoutine(id: String): Flow<RoutineDetail?>

    /** Crea un borrador vacío (0 ejercicios) y devuelve su UUID. */
    suspend fun createRoutine(name: String = "", durationMin: Int = 45): String

    suspend fun updateMeta(routineId: String, name: String, durationMin: Int)

    /** Añade un ejercicio por cada id resuelto, cada uno con 1 serie vacía. */
    suspend fun addExercises(routineId: String, ids: List<String>)

    suspend fun updateSet(
        routineExerciseId: String,
        setId: String,
        targetReps: Int,
        weightKg: Double,
        restSeconds: Int,
        loadNote: String?,
        setType: SetType
    )

    /** Nueva serie copiando los valores de la 1ª. */
    suspend fun addSet(routineExerciseId: String)

    suspend fun deleteSet(routineExerciseId: String, setId: String)

    suspend fun deleteExercise(routineExerciseId: String)

    /** Publica el borrador al feed. */
    suspend fun saveRoutine(routineId: String)

    /** Descarta el borrador; la guardada (si la había) queda intacta. */
    suspend fun discard(routineId: String)
}

/**
 * Repo fake en memoria (spec 004, Fase A).
 * Sin Room, sin Hilt, sin red. Borradores separados de guardadas para que
 * salir atrás sin guardar nunca deje rutinas fantasma en el feed.
 *
 * Sin DI en Fase A: los ViewModels comparten [shared] para ver el mismo
 * store en memoria. En Fase B se inyectará el repositorio real con Hilt
 * manteniendo la firma de [RoutineRepository].
 */
class FakeRoutineRepository(
    private val exercises: ExerciseRepository = FakeExerciseRepository()
) : RoutineRepository {

    private val drafts = MutableStateFlow<Map<String, RoutineDetail>>(emptyMap())
    private val saved = MutableStateFlow<Map<String, RoutineDetail>>(emptyMap())

    override fun observeRoutines(): Flow<List<Routine>> =
        saved.map { map -> map.values.sortedBy { it.routine.position }.map { it.routine } }

    override fun observeDetails(): Flow<List<RoutineDetail>> =
        saved.map { map -> map.values.sortedBy { it.routine.position } }

    override fun observeRoutine(id: String): Flow<RoutineDetail?> =
        combine(drafts, saved) { dr, sv -> dr[id] ?: sv[id] }

    override suspend fun createRoutine(name: String, durationMin: Int): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val detail = RoutineDetail(
            routine = Routine(
                id = id,
                name = name,
                durationMin = durationMin,
                position = saved.value.size + drafts.value.size,
                createdAt = now,
                updatedAt = now
            ),
            items = emptyList()
        )
        drafts.update { it + (id to detail) }
        return id
    }

    override suspend fun updateMeta(routineId: String, name: String, durationMin: Int) {
        ensureDraft(routineId) ?: return
        drafts.update { map ->
            val current = map[routineId] ?: return@update map
            map + (routineId to current.copy(routine = current.routine.copy(name = name, durationMin = durationMin)))
        }
    }

    override suspend fun addExercises(routineId: String, ids: List<String>) {
        if (ids.isEmpty()) return
        ensureDraft(routineId) ?: return
        val resolved = ids.mapNotNull { exercises.getById(it) }
        if (resolved.isEmpty()) return
        drafts.update { map ->
            val current = map[routineId] ?: return@update map
            var nextPos = (current.items.maxOfOrNull { it.routineExercise.position } ?: -1) + 1
            val newItems = resolved.map { exercise ->
                val reId = UUID.randomUUID().toString()
                RoutineExerciseDetail(
                    routineExercise = RoutineExercise(
                        id = reId,
                        routineId = routineId,
                        exerciseId = exercise.id,
                        position = nextPos++
                    ),
                    exercise = exercise,
                    sets = listOf(emptyPlannedSet(reId, 1))
                )
            }
            map + (routineId to current.copy(items = current.items + newItems))
        }
    }

    override suspend fun updateSet(
        routineExerciseId: String,
        setId: String,
        targetReps: Int,
        weightKg: Double,
        restSeconds: Int,
        loadNote: String?,
        setType: SetType
    ) {
        if (!ensureDraftForItem(routineExerciseId)) return
        mutateItem(routineExerciseId) { detail ->
            detail.copy(
                sets = detail.sets.map { set ->
                    if (set.id == setId) {
                        set.copy(
                            targetReps = targetReps,
                            weightKg = weightKg,
                            restSeconds = restSeconds,
                            loadNote = loadNote,
                            setType = setType
                        )
                    } else {
                        set
                    }
                }
            )
        }
    }

    override suspend fun addSet(routineExerciseId: String) {
        if (!ensureDraftForItem(routineExerciseId)) return
        mutateItem(routineExerciseId) { detail ->
            val first = detail.sets.minByOrNull { it.setNumber }
            val nextNumber = (detail.sets.maxOfOrNull { it.setNumber } ?: 0) + 1
            val newSet = if (first != null) {
                first.copy(id = UUID.randomUUID().toString(), setNumber = nextNumber)
            } else {
                emptyPlannedSet(routineExerciseId, nextNumber)
            }
            detail.copy(sets = detail.sets + newSet)
        }
    }

    override suspend fun deleteSet(routineExerciseId: String, setId: String) {
        if (!ensureDraftForItem(routineExerciseId)) return
        mutateItem(routineExerciseId) { detail ->
            detail.copy(
                sets = detail.sets
                    .filterNot { it.id == setId }
                    .sortedBy { it.setNumber }
                    .mapIndexed { index, set -> set.copy(setNumber = index + 1) }
            )
        }
    }

    override suspend fun deleteExercise(routineExerciseId: String) {
        val routineId = findRoutineId(routineExerciseId) ?: return
        ensureDraft(routineId) ?: return
        drafts.update { map ->
            val current = map[routineId] ?: return@update map
            val items = current.items
                .filterNot { it.routineExercise.id == routineExerciseId }
                .sortedBy { it.routineExercise.position }
                .mapIndexed { index, item ->
                    item.copy(routineExercise = item.routineExercise.copy(position = index))
                }
            map + (routineId to current.copy(items = items))
        }
    }

    override suspend fun saveRoutine(routineId: String) {
        val draft = drafts.value[routineId] ?: return
        val now = System.currentTimeMillis()
        val published = draft.copy(
            routine = draft.routine.copy(
                position = saved.value[routineId]?.routine?.position ?: saved.value.size,
                updatedAt = now,
                createdAt = if (draft.routine.createdAt == 0L) now else draft.routine.createdAt
            )
        )
        saved.update { it + (routineId to published) }
        drafts.update { it - routineId }
    }

    override suspend fun discard(routineId: String) {
        drafts.update { it - routineId }
    }

    /** Copia la guardada a borrador si aún no hay sesión de edición. */
    private fun ensureDraft(routineId: String): RoutineDetail? {
        drafts.value[routineId]?.let { return it }
        val base = saved.value[routineId] ?: return null
        drafts.update { it + (routineId to base) }
        return base
    }

    private fun findRoutineId(routineExerciseId: String): String? =
        drafts.value.entries
            .find { (_, detail) -> detail.items.any { it.routineExercise.id == routineExerciseId } }
            ?.key
            ?: saved.value.entries
                .find { (_, detail) -> detail.items.any { it.routineExercise.id == routineExerciseId } }
                ?.key

    private fun ensureDraftForItem(routineExerciseId: String): Boolean {
        val routineId = findRoutineId(routineExerciseId) ?: return false
        return ensureDraft(routineId) != null
    }

    private inline fun mutateItem(
        routineExerciseId: String,
        transform: (RoutineExerciseDetail) -> RoutineExerciseDetail
    ) {
        drafts.update { map ->
            val entry = map.entries
                .find { (_, detail) -> detail.items.any { it.routineExercise.id == routineExerciseId } }
                ?: return@update map
            val updated = entry.value.copy(
                items = entry.value.items.map { item ->
                    if (item.routineExercise.id == routineExerciseId) transform(item) else item
                }
            )
            map + (entry.key to updated)
        }
    }

    companion object {
        /** Serie vacía por defecto: corporal, 90 s, tipo normal. */
        private fun emptyPlannedSet(routineExerciseId: String, number: Int): PlannedSet =
            PlannedSet(
                id = UUID.randomUUID().toString(),
                routineExerciseId = routineExerciseId,
                setNumber = number,
                targetReps = 0,
                weightKg = 0.0,
                restSeconds = 90,
                loadNote = null,
                setType = SetType.NORMAL
            )

        /** Store compartido en memoria para Fase A sin DI. */
        val shared: FakeRoutineRepository by lazy { FakeRoutineRepository() }
    }
}
