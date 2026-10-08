package com.mytrainingplan.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mytrainingplan.app.data.local.AppDatabase
import com.mytrainingplan.app.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Base en memoria de ejercicios (spec 011, paso 11): hermética, sin
 * dependencias nuevas (`Room.inMemoryDatabaseBuilder` es de `room-runtime`,
 * ya declarado). Sin `kotlinx-coroutines-test`: `runBlocking` basta.
 */
@RunWith(AndroidJUnit4::class)
class ExerciseDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ExerciseDao

    @Before
    fun crea_base_en_memoria() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.exerciseDao()
    }

    @After
    fun cierra_base() {
        db.close()
    }

    private fun entity(
        id: String = "biceps/barbell-curl",
        source: String = "CATALOG",
        deleted: Boolean = false
    ) = ExerciseEntity(
        id = id,
        slug = "barbell-curl",
        name = "Curl con barra",
        muscle = "biceps",
        bodyPart = "arms",
        equipment = "barbell",
        category = "strength",
        secondaryMuscles = listOf("forearms"),
        instructions = listOf("Sube la barra hasta los hombros."),
        gifUrl = "https://example.invalid/biceps/barbell-curl.gif",
        source = source,
        updatedAt = 1L,
        deleted = deleted
    )

    @Test
    fun upsert_y_getById_devuelven_la_fila_con_listas() = runBlocking {
        dao.upsert(entity())

        val got = dao.getById("biceps/barbell-curl")!!

        assertEquals("Curl con barra", got.name)
        assertEquals(listOf("forearms"), got.secondaryMuscles)
        assertEquals(listOf("Sube la barra hasta los hombros."), got.instructions)
        assertEquals("CATALOG", got.source)
    }

    @Test
    fun segundo_upsert_actualiza_sin_duplicar() = runBlocking {
        dao.upsert(entity())
        dao.upsert(entity().copy(name = "Curl con barra 2"))

        assertEquals("Curl con barra 2", dao.getById("biceps/barbell-curl")!!.name)
        assertEquals(1, dao.observeAll().first().size)
    }

    @Test
    fun observeBySource_separa_catalog_de_propio() = runBlocking {
        dao.upsert(entity(id = "biceps/barbell-curl", source = "CATALOG"))
        dao.upsert(entity(id = "custom-1", source = "CUSTOM"))

        assertEquals(
            listOf("biceps/barbell-curl"),
            dao.observeBySource("CATALOG").first().map { it.id }
        )
        assertEquals(
            listOf("custom-1"),
            dao.observeBySource("CUSTOM").first().map { it.id }
        )
    }

    @Test
    fun borrado_logico_filtra_en_lecturas() = runBlocking {
        dao.upsert(entity(deleted = true))

        assertNull(dao.getById("biceps/barbell-curl"))
        assertEquals(0, dao.observeAll().first().size)
    }
}
