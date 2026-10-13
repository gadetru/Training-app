package com.mytrainingplan.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mytrainingplan.app.data.local.AppDatabase
import com.mytrainingplan.app.data.local.entity.ScheduledDayEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Base en memoria de apuntes (Paso 9 spec 015): hermética, mismo patrón
 * que `ExerciseDaoTest` (sin dependencias nuevas, `runBlocking` basta).
 */
@RunWith(AndroidJUnit4::class)
class ScheduledDayDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ScheduledDayDao

    @Before
    fun crea_base_en_memoria() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.scheduledDayDao()
    }

    @After
    fun cierra_base() {
        db.close()
    }

    private fun entity(
        id: String = "d1",
        routineId: String = "r-torso",
        date: String = "2026-10-15",
        deleted: Boolean = false
    ) = ScheduledDayEntity(
        id = id,
        routineId = routineId,
        date = date,
        updatedAt = 1L,
        deleted = deleted
    )

    @Test
    fun upsert_y_getByDate_devuelven_el_apunte() = runBlocking {
        dao.upsert(entity())

        val got = dao.getByDate("2026-10-15")!!

        assertEquals("r-torso", got.routineId)
        assertEquals("2026-10-15", got.date)
    }

    @Test
    fun segundo_upsert_en_misma_fecha_sustituye_sin_duplicar() = runBlocking {
        dao.upsert(entity())
        dao.upsert(entity().copy(routineId = "r-pierna"))

        assertEquals("r-pierna", dao.getByDate("2026-10-15")!!.routineId)
        assertEquals(1, dao.observeBetween("2026-10-01", "2026-11-01").first().size)
    }

    @Test
    fun observeBetween_solo_trae_el_rango() = runBlocking {
        dao.upsert(entity(id = "d1", date = "2026-10-15"))
        dao.upsert(entity(id = "d2", date = "2026-11-02"))

        assertEquals(
            listOf("2026-10-15"),
            dao.observeBetween("2026-10-01", "2026-11-01").first().map { it.date }
        )
    }

    @Test
    fun borrado_logico_filtra_en_lecturas() = runBlocking {
        dao.upsert(entity(deleted = true))

        assertNull(dao.getByDate("2026-10-15"))
        assertEquals(0, dao.observeBetween("2026-10-01", "2026-11-01").first().size)
    }
}
