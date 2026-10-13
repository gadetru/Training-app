package com.mytrainingplan.app.data.repository

import com.mytrainingplan.app.domain.model.MonthDay
import com.mytrainingplan.app.domain.model.ScheduledDay
import com.mytrainingplan.app.domain.model.WeeklyProgress
import kotlinx.coroutines.flow.Flow

/**
 * Firma del repositorio de apuntes + historial (Paso 5 spec 015).
 * Misma forma que los actuales: observar con Flow, guardar/borrar con
 * borrado lógico. Lo "hecho" se deduce de sesiones finalizadas
 * (`endedAt != null`), nunca se guarda aparte.
 */
interface ScheduleRepository {
    /** Apuntes activos en `[fromDate, toDate)` (`yyyy-MM-dd`). */
    fun observeScheduledBetween(fromDate: String, toDate: String): Flow<List<ScheduledDay>>

    /** Apunte activo del día o null. */
    fun observeScheduledByDate(date: String): Flow<ScheduledDay?>

    /**
     * Apunta la rutina al día; si ya había apunte lo sustituye en la misma
     * fila (cambiar no duplica).
     */
    suspend fun setScheduled(date: String, routineId: String)

    /** Quita el apunte del día (borrado lógico, sin borrar nada más). */
    suspend fun clearScheduled(date: String)

    /** Días del mes resueltos (apunte + hecho con nombres, manda lo hecho). */
    fun observeMonthDays(year: Int, month: Int): Flow<List<MonthDay>>

    /** Racha, sesiones y minutos de la semana y semana L–D reales. */
    fun observeProgress(): Flow<WeeklyProgress>

    /** Último inicio de sesión finalizada de la rutina (null si nunca). */
    fun observeLastDoneAt(routineId: String): Flow<Long?>
}
