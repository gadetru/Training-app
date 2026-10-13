package com.mytrainingplan.app.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mytrainingplan.app.data.repository.ScheduleRepository
import com.mytrainingplan.app.domain.model.WeeklyProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Progreso con Room (Paso 7 spec 015).
 * Expone el progreso real (racha, sesiones y minutos de la semana,
 * semana L–D) tal cual lo calcula [ScheduleRepository].
 */
@HiltViewModel
class ProgressViewModel @Inject constructor(
    schedules: ScheduleRepository
) : ViewModel() {

    val uiState: StateFlow<WeeklyProgress> = schedules.observeProgress()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WeeklyProgress(
                streakDays = 0,
                sessionsDone = 0,
                sessionsGoal = 4,
                minutes = 0,
                weekChecks = List(7) { false }
            )
        )
}
