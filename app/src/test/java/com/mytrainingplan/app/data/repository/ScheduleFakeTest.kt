package com.mytrainingplan.app.data.repository

import com.mytrainingplan.app.domain.model.WorkoutSession
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Apuntes + historial con el espejo falso (Paso 9 spec 015): apuntar,
 * cambiar sin duplicar, quitar sin borrar nada más, hecho tras finalizar
 * y racha que sube con un día nuevo. Misma firma que el Room.
 * Sin `kotlinx-coroutines-test` (no declarado): `runBlocking` basta.
 */
class ScheduleFakeTest {

    private val utc: ZoneId = ZoneId.of("UTC")

    private fun millis(iso: String): Long = Instant.parse(iso).toEpochMilli()

    private data class Fixture(
        val routines: FakeRoutineRepository,
        val schedules: FakeScheduleRepository,
        val torsoId: String,
        val piernaId: String
    )

    private suspend fun fixture(): Fixture {
        val routines = FakeRoutineRepository()
        val torsoId = routines.createRoutine(name = "Torso")
        routines.saveRoutine(torsoId)
        val piernaId = routines.createRoutine(name = "Pierna")
        routines.saveRoutine(piernaId)
        val schedules = FakeScheduleRepository(routines = routines, zone = utc)
        return Fixture(routines, schedules, torsoId, piernaId)
    }

    private suspend fun month(
        f: Fixture,
        year: Int = 2026,
        month: Int = 10
    ) = f.schedules.observeMonthDays(year, month).first()

    @Test
    fun apuntar_torso_al_15_muestra_torso() = runBlocking {
        val f = fixture()

        f.schedules.setScheduled("2026-10-15", f.torsoId)

        assertEquals("Torso", month(f).first { it.dayOfMonth == 15 }.displayName)
    }

    @Test
    fun cambiar_a_pierna_no_duplica() = runBlocking {
        val f = fixture()
        f.schedules.setScheduled("2026-10-15", f.torsoId)

        f.schedules.setScheduled("2026-10-15", f.piernaId)

        assertEquals("Pierna", month(f).first { it.dayOfMonth == 15 }.displayName)
        assertEquals(
            1,
            f.schedules.observeScheduledBetween("2026-10-01", "2026-11-01").first().size
        )
    }

    @Test
    fun quitar_deja_casilla_vacia_sin_borrar_lo_demas() = runBlocking {
        val f = fixture()
        f.schedules.setScheduled("2026-10-15", f.torsoId)
        f.schedules.setScheduled("2026-10-16", f.piernaId)

        f.schedules.clearScheduled("2026-10-15")

        val days = month(f)
        assertNull(days.first { it.dayOfMonth == 15 }.displayName)
        assertEquals("Pierna", days.first { it.dayOfMonth == 16 }.displayName)
    }

    @Test
    fun finalizar_marca_hecho_y_manda_sobre_apunte() = runBlocking {
        val f = fixture()
        f.schedules.setScheduled("2026-10-13", f.piernaId)
        f.schedules.addFinished(
            WorkoutSession(
                id = "s1",
                routineId = f.torsoId,
                startedAt = millis("2026-10-13T10:00:00Z"),
                endedAt = millis("2026-10-13T10:45:00Z")
            )
        )

        val day = month(f).first { it.dayOfMonth == 13 }

        assertTrue(day.isDone)
        assertEquals("Torso", day.displayName)
    }

    @Test
    fun dia_pasado_con_sesion_y_sin_apunte_muestra_hecho() = runBlocking {
        val f = fixture()
        f.schedules.addFinished(
            WorkoutSession(
                id = "s1",
                routineId = f.torsoId,
                startedAt = millis("2026-10-10T10:00:00Z"),
                endedAt = millis("2026-10-10T10:30:00Z")
            )
        )

        val day = month(f).first { it.dayOfMonth == 10 }

        assertTrue(day.isDone)
        assertEquals("Torso", day.displayName)
    }

    @Test
    fun racha_sube_con_primer_entreno_de_dia_nuevo() = runBlocking {
        val f = fixture()
        // Relativo a hoy (el repo usa el reloj real): ayer entrenado, hoy no.
        val today = java.time.LocalDate.now(utc)
        val at = { day: java.time.LocalDate, hour: Long ->
            com.mytrainingplan.app.domain.model.HistoryCalcs.startOfDayMillis(day, utc) +
                hour * 3_600_000L
        }
        f.schedules.addFinished(
            WorkoutSession(
                id = "s1",
                routineId = f.torsoId,
                startedAt = at(today.minusDays(1), 10),
                endedAt = at(today.minusDays(1), 10) + 30 * 60_000L
            )
        )
        val before = f.schedules.observeProgress().first().streakDays

        f.schedules.addFinished(
            WorkoutSession(
                id = "s2",
                routineId = f.torsoId,
                startedAt = at(today, 10),
                endedAt = at(today, 10) + 30 * 60_000L
            )
        )

        assertEquals(before + 1, f.schedules.observeProgress().first().streakDays)
    }
}
