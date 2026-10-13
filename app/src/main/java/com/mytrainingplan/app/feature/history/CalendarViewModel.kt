package com.mytrainingplan.app.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mytrainingplan.app.data.repository.RoutineRepository
import com.mytrainingplan.app.data.repository.ScheduleRepository
import com.mytrainingplan.app.domain.model.MonthDay
import com.mytrainingplan.app.domain.model.Routine
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado UI del mes (Paso 6 spec 015).
 * `selectedDate` null = hoja cerrada; no-null = hoja abierta ese día.
 */
data class CalendarUiState(
    val year: Int = 0,
    val month: Int = 0,
    val days: List<MonthDay> = emptyList(),
    /** Feed de rutinas creadas ordenado por `position` (para la hoja). */
    val routines: List<Routine> = emptyList(),
    val selectedDate: String? = null
) {
    val selectedDay: MonthDay?
        get() = days.find { it.date == selectedDate }

    val isSheetOpen: Boolean
        get() = selectedDate != null
}

/**
 * Mes con Room (Paso 6 spec 015).
 * Expone el mes resuelto (apunte + hecho) y el feed para la hoja;
 * apuntar/cambiar/quitar van directo a [ScheduleRepository].
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val schedules: ScheduleRepository,
    routines: RoutineRepository
) : ViewModel() {

    private val start = YearMonth.now()
    private val month = MutableStateFlow(start)
    private val selected = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CalendarUiState> = month.flatMapLatest { ym ->
        combine(
            schedules.observeMonthDays(ym.year, ym.monthValue),
            routines.observeRoutines(),
            selected
        ) { days, feed, sel ->
            CalendarUiState(
                year = ym.year,
                month = ym.monthValue,
                days = days,
                routines = feed,
                selectedDate = sel
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalendarUiState(year = start.year, month = start.monthValue)
    )

    fun onDayClick(date: String) {
        selected.value = date
    }

    fun onDismissSheet() {
        selected.value = null
    }

    fun onPrevMonth() {
        month.update { it.minusMonths(1) }
    }

    fun onNextMonth() {
        month.update { it.plusMonths(1) }
    }

    /** Apunta (o cambia) y confirma en la misma vista: cierra la hoja. */
    fun onSelectRoutine(date: String, routineId: String) {
        viewModelScope.launch {
            schedules.setScheduled(date, routineId)
            selected.value = null
        }
    }

    /** Quita el apunte (lógico) y confirma en la misma vista. */
    fun onClearScheduled(date: String) {
        viewModelScope.launch {
            schedules.clearScheduled(date)
            selected.value = null
        }
    }
}
