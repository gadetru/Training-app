package com.mytrainingplan.app.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Mes e historial (Paso 2 spec 015).
 * Modelos limpios de dominio: sin dependencias Android / Room / Retrofit.
 * La UI solo ve estos modelos (mapping en `data/mapper` en pasos posteriores).
 *
 * - Apunte = qué rutina toca un día concreto (`yyyy-MM-dd` local).
 * - Lo "hecho" NO se guarda: se deduce de sesiones finalizadas (`endedAt != null`).
 * - IDs de usuario = UUID generados en cliente; `updatedAt` + `deleted`
 *   con borrado lógico, sync-ready Fase 2.
 */

/** Apunte de rutina a un día concreto (tabla `scheduled_days` en pasos 3-4). */
data class ScheduledDay(
    /** UUID generado en cliente. */
    val id: String,
    val routineId: String,
    /** Día local del dispositivo en `yyyy-MM-dd`. */
    val date: String,
    val updatedAt: Long = 0L,
    val deleted: Boolean = false
)

/**
 * Un día del mes con su estado resuelto.
 * Si hay hecho y apunte de otra rutina, manda lo hecho (spec §4).
 */
data class MonthDay(
    /** Día local en `yyyy-MM-dd`. */
    val date: String,
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean = true,
    val isToday: Boolean = false,
    val scheduledRoutineId: String? = null,
    val scheduledRoutineName: String? = null,
    val doneRoutineId: String? = null,
    val doneRoutineName: String? = null
) {
    val isDone: Boolean
        get() = doneRoutineId != null

    /** Nombre a mostrar en la casilla: hecho manda sobre apunte. */
    val displayName: String?
        get() = doneRoutineName ?: scheduledRoutineName
}

/** Cálculos puros del mes/progreso (JVM-testables, sin Android ni Room). */
object HistoryCalcs {

    val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun todayKey(zone: ZoneId = ZoneId.systemDefault()): String =
        LocalDate.now(zone).format(DATE_FMT)

    fun dateKeyOf(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate().format(DATE_FMT)

    fun parseKey(key: String): LocalDate = LocalDate.parse(key, DATE_FMT)

    /**
     * Etiqueta por rutina: `Hoy`, `Ayer`, `Hace X días`, null si nunca se hizo.
     * Diferencia en días naturales locales, no en bloques de 24 h.
     */
    fun lastDoneLabel(
        lastDoneAt: Long?,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): String? {
        if (lastDoneAt == null) return null
        val done = dateKeyOf(lastDoneAt, zone).let(::parseKey)
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val diff = now.toEpochDay() - done.toEpochDay()
        return when {
            diff <= 0 -> "Hoy"
            diff == 1L -> "Ayer"
            else -> "Hace $diff días"
        }
    }

    /**
     * Racha = días seguidos con al menos una sesión finalizada hasta hoy.
     * Si hoy aún no se entrenó, cuenta desde ayer (spec §4).
     */
    fun streakDays(
        doneDayKeys: Set<String>,
        today: LocalDate = LocalDate.now()
    ): Int {
        var cursor = today
        if (!doneDayKeys.contains(cursor.format(DATE_FMT))) {
            cursor = cursor.minusDays(1)
        }
        var streak = 0
        while (doneDayKeys.contains(cursor.format(DATE_FMT))) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    /** Lunes de la semana natural (lunes-domingo) que contiene [date]. */
    fun mondayOf(date: LocalDate = LocalDate.now()): LocalDate {
        var cursor = date
        while (cursor.dayOfWeek.value != 1) {
            cursor = cursor.minusDays(1)
        }
        return cursor
    }

    /** Claves `yyyy-MM-dd` L–D de la semana que empieza en [monday]. */
    fun weekKeys(monday: LocalDate): List<String> =
        (0..6).map { monday.plusDays(it.toLong()).format(DATE_FMT) }

    /** Semana L–D: true = día con al menos una sesión finalizada. */
    fun weekChecks(
        doneDayKeys: Set<String>,
        monday: LocalDate = mondayOf()
    ): List<Boolean> = weekKeys(monday).map { doneDayKeys.contains(it) }

    /** Minutos de una sesión finalizada (fin menos inicio, sin negativos). */
    fun minutesOf(startedAt: Long, endedAt: Long?): Int {
        if (endedAt == null) return 0
        val diff = endedAt - startedAt
        if (diff <= 0) return 0
        return (diff / 60_000L).toInt()
    }

    /** Suma de minutos de sesiones finalizadas. */
    fun sumMinutes(sessions: List<WorkoutSession>): Int =
        sessions.filter { it.endedAt != null && !it.deleted }
            .sumOf { minutesOf(it.startedAt, it.endedAt) }

    /** Inicio del día local en milisegundos (rango día para el repositorio). */
    fun startOfDayMillis(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    /**
     * Rango del mes en milisegundos `[primero, primero-del-siguiente)`.
     * El repositorio filtra sesiones por `startedAt` con este rango.
     */
    fun monthBoundsMillis(
        year: Int,
        month: Int,
        zone: ZoneId = ZoneId.systemDefault()
    ): Pair<Long, Long> {
        val first = LocalDate.of(year, month, 1)
        return startOfDayMillis(first, zone) to
            startOfDayMillis(first.plusMonths(1), zone)
    }

    /** Clave del día 1 del mes (`yyyy-MM-dd`), para filtrar apuntes. */
    fun monthFirstKey(year: Int, month: Int): String =
        LocalDate.of(year, month, 1).format(DATE_FMT)

    /** Clave del día 1 del mes siguiente (límite exclusivo de apuntes). */
    fun nextMonthFirstKey(year: Int, month: Int): String =
        LocalDate.of(year, month, 1).plusMonths(1).format(DATE_FMT)

    /**
     * Resuelve los días del mes combinando apuntes y hecho.
     * `scheduled`: fecha → (routineId, nombre o null si se oculta por borrada).
     * `done`: fecha → (routineId, nombre; incluye borradas: el historial sobrevive).
     */
    fun resolveMonthDays(
        year: Int,
        month: Int,
        todayKey: String,
        scheduled: Map<String, Pair<String, String?>>,
        done: Map<String, Pair<String, String>>
    ): List<MonthDay> {
        val length = YearMonth.of(year, month).lengthOfMonth()
        return (1..length).map { day ->
            val key = LocalDate.of(year, month, day).format(DATE_FMT)
            val s = scheduled[key]
            val d = done[key]
            MonthDay(
                date = key,
                dayOfMonth = day,
                isCurrentMonth = true,
                isToday = key == todayKey,
                scheduledRoutineId = s?.first,
                scheduledRoutineName = s?.second,
                doneRoutineId = d?.first,
                doneRoutineName = d?.second
            )
        }
    }

    /**
     * Progreso real desde sesiones finalizadas (spec §4): racha hasta hoy,
     * sesiones y minutos de la semana lunes-domingo, semana L–D.
     * El objetivo sigue fijo en 4 y `volumeDeltaPct` sin calcular (no entra).
     */
    fun buildProgress(
        finished: List<WorkoutSession>,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): WeeklyProgress {
        val valid = finished.filter { it.endedAt != null && !it.deleted }
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val doneKeys = valid.map { dateKeyOf(it.startedAt, zone) }.toSet()
        val monday = mondayOf(now)
        val weekStart = startOfDayMillis(monday, zone)
        val weekEnd = startOfDayMillis(monday.plusDays(7), zone)
        val week = valid.filter { it.startedAt >= weekStart && it.startedAt < weekEnd }
        return WeeklyProgress(
            streakDays = streakDays(doneKeys, now),
            sessionsDone = week.size,
            sessionsGoal = 4,
            minutes = sumMinutes(week),
            volumeDeltaPct = 12,
            weekChecks = weekChecks(doneKeys, monday)
        )
    }
}
