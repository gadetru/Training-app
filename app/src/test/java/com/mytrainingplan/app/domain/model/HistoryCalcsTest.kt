package com.mytrainingplan.app.domain.model

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cálculo puro del mes/progreso (Paso 9 spec 015): racha, semana L–D,
 * etiquetas Hoy/Ayer/Hace X y minutos. Sin Android ni Room.
 */
class HistoryCalcsTest {

    private val utc: ZoneId = ZoneId.of("UTC")

    private fun millis(iso: String): Long =
        java.time.Instant.parse(iso).toEpochMilli()

    // Etiquetas por rutina.

    @Test
    fun sin_sesion_nunca_etiqueta_nula() {
        assertNull(HistoryCalcs.lastDoneLabel(null, millis("2026-10-13T12:00:00Z"), utc))
    }

    @Test
    fun mismo_dia_es_hoy() {
        assertEquals(
            "Hoy",
            HistoryCalcs.lastDoneLabel(
                millis("2026-10-13T08:00:00Z"),
                millis("2026-10-13T12:00:00Z"),
                utc
            )
        )
    }

    @Test
    fun dia_anterior_es_ayer() {
        assertEquals(
            "Ayer",
            HistoryCalcs.lastDoneLabel(
                millis("2026-10-12T20:00:00Z"),
                millis("2026-10-13T12:00:00Z"),
                utc
            )
        )
    }

    @Test
    fun cinco_dias_es_hace_5() {
        assertEquals(
            "Hace 5 días",
            HistoryCalcs.lastDoneLabel(
                millis("2026-10-08T10:00:00Z"),
                millis("2026-10-13T12:00:00Z"),
                utc
            )
        )
    }

    // Racha.

    @Test
    fun racha_cuenta_dias_seguidos_hasta_hoy() {
        val done = setOf("2026-10-13", "2026-10-12", "2026-10-11")
        assertEquals(3, HistoryCalcs.streakDays(done, LocalDate.parse("2026-10-13")))
    }

    @Test
    fun racha_sin_entrenar_hoy_cuenta_desde_ayer() {
        val done = setOf("2026-10-12", "2026-10-11")
        assertEquals(2, HistoryCalcs.streakDays(done, LocalDate.parse("2026-10-13")))
    }

    @Test
    fun racha_se_rompe_con_hueco() {
        val done = setOf("2026-10-13", "2026-10-11")
        assertEquals(1, HistoryCalcs.streakDays(done, LocalDate.parse("2026-10-13")))
    }

    @Test
    fun racha_vacia_es_cero() {
        assertEquals(0, HistoryCalcs.streakDays(emptySet(), LocalDate.parse("2026-10-13")))
    }

    // Semana lunes-domingo.

    @Test
    fun lunes_de_martes_y_de_domingo() {
        assertEquals(LocalDate.parse("2026-10-12"), HistoryCalcs.mondayOf(LocalDate.parse("2026-10-13")))
        assertEquals(LocalDate.parse("2026-10-05"), HistoryCalcs.mondayOf(LocalDate.parse("2026-10-11")))
    }

    @Test
    fun semana_marca_solo_dias_entrenados() {
        val done = setOf("2026-10-12", "2026-10-14")
        assertEquals(
            listOf(true, false, true, false, false, false, false),
            HistoryCalcs.weekChecks(done, LocalDate.parse("2026-10-12"))
        )
    }

    // Minutos.

    @Test
    fun minutos_suman_fin_menos_inicio_e_ignoran_raras() {
        assertEquals(90, HistoryCalcs.minutesOf(0L, 90L * 60_000L))
        assertEquals(0, HistoryCalcs.minutesOf(0L, null))
        assertEquals(0, HistoryCalcs.minutesOf(100L, 50L))
    }

    // Progreso agregado.

    private fun session(id: String, routine: String, startIso: String, endIso: String?) =
        WorkoutSession(
            id = id,
            routineId = routine,
            startedAt = millis(startIso),
            endedAt = endIso?.let { millis(it) }
        )

    @Test
    fun progreso_reune_racha_semana_y_minutos() {
        val done = listOf(
            session("s0", "r1", "2026-10-11T20:00:00Z", "2026-10-11T20:30:00Z"),
            session("s1", "r1", "2026-10-12T10:00:00Z", "2026-10-12T10:45:00Z"),
            session("s2", "r2", "2026-10-13T10:00:00Z", "2026-10-13T10:30:00Z"),
            session("s3", "r1", "2026-10-13T18:00:00Z", null)
        )
        val p = HistoryCalcs.buildProgress(done, millis("2026-10-13T12:00:00Z"), utc)

        assertEquals(3, p.streakDays)
        assertEquals(2, p.sessionsDone)
        assertEquals(4, p.sessionsGoal)
        assertEquals(75, p.minutes)
        assertEquals(
            listOf(true, true, false, false, false, false, false),
            p.weekChecks
        )
    }

    // Mes resuelto.

    @Test
    fun mes_muestra_apunte_y_lo_hecho_manda() {
        val days = HistoryCalcs.resolveMonthDays(
            year = 2026,
            month = 10,
            todayKey = "2026-10-13",
            scheduled = mapOf(
                "2026-10-15" to ("t1" to "Torso"),
                "2026-10-16" to ("p1" to "Pierna")
            ),
            done = mapOf("2026-10-15" to ("p1" to "Pierna"))
        )

        assertEquals(31, days.size)
        val d15 = days.first { it.dayOfMonth == 15 }
        assertTrue(d15.isDone)
        assertEquals("Pierna", d15.displayName)
        val d16 = days.first { it.dayOfMonth == 16 }
        assertFalse(d16.isDone)
        assertEquals("Pierna", d16.displayName)
        val d17 = days.first { it.dayOfMonth == 17 }
        assertNull(d17.displayName)
        assertTrue(days.first { it.dayOfMonth == 13 }.isToday)
    }

    @Test
    fun rango_del_mes_cubre_octubre_en_utc() {
        val (from, to) = HistoryCalcs.monthBoundsMillis(2026, 10, utc)
        assertEquals(millis("2026-10-01T00:00:00Z"), from)
        assertEquals(millis("2026-11-01T00:00:00Z"), to)
    }
}
