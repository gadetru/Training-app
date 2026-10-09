package com.mytrainingplan.app.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reglas de rutinas (spec 008; spec 011, paso 9): posición sin huecos y
 * descarte seguro, con falsos en memoria (misma firma que los Room).
 * Romper la posición o el descarte hace fallar su prueba.
 * Sin `kotlinx-coroutines-test` (no declarado): `runBlocking` de
 * `kotlinx-coroutines-core` (transitivo) basta porque los falsos resuelven
 * al instante.
 */
class RoutineRulesTest {

    private fun repo() = FakeRoutineRepository(exercises = FakeExerciseRepository())

    @Test
    fun crear_tres_rutinas_feed_ordenado_sin_huecos() = runBlocking {
        val r = repo()
        val a = r.createRoutine(name = "A")
        val b = r.createRoutine(name = "B")
        val c = r.createRoutine(name = "C")
        r.saveRoutine(a)
        r.saveRoutine(b)
        r.saveRoutine(c)

        val feed = r.observeRoutines().first()

        assertEquals(listOf(a, b, c), feed.map { it.id })
        assertEquals(listOf(0, 1, 2), feed.map { it.position })
    }

    @Test
    fun borrar_del_medio_renumera_sin_huecos() = runBlocking {
        val r = repo()
        val a = r.createRoutine(name = "A")
        val b = r.createRoutine(name = "B")
        val c = r.createRoutine(name = "C")
        r.saveRoutine(a)
        r.saveRoutine(b)
        r.saveRoutine(c)

        r.deleteRoutine(b)

        val feed = r.observeRoutines().first()
        assertEquals(listOf(a, c), feed.map { it.id })
        assertEquals(listOf(0, 1), feed.map { it.position })
    }

    @Test
    fun descartar_borrador_vacio_no_deja_fantasma() = runBlocking {
        val r = repo()
        val draft = r.createRoutine(name = "Borrador")

        r.discard(draft)

        assertTrue(r.observeRoutines().first().isEmpty())
        assertNull(r.observeRoutine(draft).first())
    }

    @Test
    fun descartar_guardada_con_contenido_queda_intacta() = runBlocking {
        val r = repo()
        val id = r.createRoutine(name = "Con contenido")
        r.addExercises(id, listOf("quads/barbell-bench-squat"))
        r.saveRoutine(id)

        r.discard(id)

        val feed = r.observeRoutines().first()
        assertEquals(1, feed.size)
        assertEquals(id, feed.single().id)
        assertEquals(1, r.observeRoutine(id).first()!!.items.size)
    }

    @Test
    fun anadir_ejercicios_en_dos_tandas_continua_posicion() = runBlocking {
        val r = repo()
        val id = r.createRoutine(name = "Tandas")
        r.addExercises(id, listOf("quads/barbell-bench-squat"))
        r.addExercises(
            id,
            listOf("quads/lever-leg-extension", "hamstrings/lying-leg-curl")
        )

        val items = r.observeRoutine(id).first()!!.items

        assertEquals(listOf(0, 1, 2), items.map { it.routineExercise.position })
    }

    @Test
    fun nota_ejercicio_guarda_y_espeja_a_todas_las_series() = runBlocking {
        val r = repo()
        val id = r.createRoutine(name = "Nota")
        r.addExercises(id, listOf("quads/barbell-bench-squat"))
        val reId = r.observeRoutine(id).first()!!.items.single().routineExercise.id
        r.addSet(reId)

        r.updateExerciseNote(reId, "magnesio")

        val item = r.observeRoutine(id).first()!!.items.single()
        assertEquals("magnesio", item.routineExercise.note)
        assertEquals(2, item.sets.size)
        assertTrue(item.sets.all { it.loadNote == "magnesio" })

        r.updateExerciseNote(reId, "")

        val cleared = r.observeRoutine(id).first()!!.items.single()
        assertEquals("", cleared.routineExercise.note)
        assertTrue(cleared.sets.all { it.loadNote == null })
    }
}
