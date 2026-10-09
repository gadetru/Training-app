package com.mytrainingplan.app.data.repository

import com.mytrainingplan.app.domain.model.ExerciseFilter
import com.mytrainingplan.app.domain.model.MuscleGroups
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Filtro del buscador con etiquetas ES + slug (spec 012, paso 8).
 * Vive en su espejo (`data/repository`, spec 011) y tira del fake en memoria
 * (misma firma y mismo `applyFilter` que el Room). Sin
 * `kotlinx-coroutines-test` (no declarado): basta `runBlocking` porque el
 * fake resuelve al instante (patrón de `RoutineRulesTest`).
 */
class ExerciseFilterTest {

    private fun repo() = FakeExerciseRepository()

    private suspend fun search(
        query: String = "",
        muscles: Set<String> = emptySet(),
        equipment: Set<String> = emptySet()
    ) = repo().observeExercises(ExerciseFilter(query, muscles, equipment)).first()

    @Test
    fun sentadilla_muestra_sus_filas() = runBlocking {
        val results = search("sentadilla")

        assertEquals(
            setOf("quads/barbell-bench-squat", "quads/dumbbell-goblet-squat"),
            results.map { it.id }.toSet()
        )
    }

    @Test
    fun pierna_encuentra_ejercicios_de_pierna_por_etiqueta() = runBlocking {
        val results = search("pierna")

        assertEquals(9, results.size)
        assertTrue(results.all { it.muscle in setOf("quads", "hamstrings", "calves") })
    }

    @Test
    fun press_mas_chip_pecho_filtra_combinado() = runBlocking {
        val results = search("press", muscles = MuscleGroups.slugsFor("pecho"))

        assertEquals(listOf("pectorals/barbell-bench-press"), results.map { it.id })
    }

    @Test
    fun barra_casa_por_etiqueta_de_equipamiento() = runBlocking {
        val results = search("barra")

        assertEquals(5, results.size)
        assertTrue(results.all { it.equipment == "barbell" })
    }

    @Test
    fun maquina_sin_acento_casa_por_etiqueta() = runBlocking {
        val results = search("maquina")

        assertEquals(4, results.size)
        assertTrue(results.all { it.equipment == "lever" })
    }

    @Test
    fun slug_api_casa_texto_libre() = runBlocking {
        val results = search("pull-up")

        assertEquals(listOf("lats/pull-up"), results.map { it.id })
    }

    @Test
    fun consulta_vacia_con_chip_devuelve_todo_el_grupo() = runBlocking {
        val results = search(muscles = MuscleGroups.slugsFor("pierna"))

        assertEquals(9, results.size)
    }
}
