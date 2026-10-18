package com.mytrainingplan.app.feature.home

import com.mytrainingplan.app.data.repository.HomeRepository
import com.mytrainingplan.app.domain.model.AccentColor
import com.mytrainingplan.app.domain.model.FootKind
import com.mytrainingplan.app.domain.model.HomeUiState
import com.mytrainingplan.app.domain.model.RoutineSummary
import com.mytrainingplan.app.domain.model.WeeklyProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking

/**
 * Spec 022 §61: flags del feed de Home con fakes (sesión viva, programada
 * hoy, contador). JVM-testable, sin Android ni Room.
 */
class HomeLiveTest {

    private class TestHomeRepository(initial: HomeUiState) : HomeRepository {
        private val state = MutableStateFlow(initial)
        override fun observeHome(): Flow<HomeUiState> {
            return state.asStateFlow()
        }
        override suspend fun getHome(): HomeUiState {
            return state.value
        }
        fun setState(ui: HomeUiState) {
            state.value = ui
        }
    }

    private fun routine(id: String, title: String, accent: AccentColor = AccentColor.ORANGE) =
        RoutineSummary(
            id = id, title = title, tags = emptyList(), durationMin = 45, exerciseCount = 3,
            accent = accent, footNote = "", footKind = FootKind.INFO
        )

    @Test
    fun sin_sesion_viva_flags_en_cero() = runBlocking {
        val repo = TestHomeRepository(
            HomeUiState(
                routines = listOf(routine("r1", "Torso")),
                progress = WeeklyProgress(),
                liveRoutineId = null,
                liveElapsedSec = 0,
                scheduledTodayId = null,
                liveSessionId = null
            )
        )
        val ui = repo.getHome()
        assertFalse(ui.hasLiveSession)
        assertEquals(null, ui.liveRoutineId)
        assertEquals(0, ui.liveElapsedSec)
        assertEquals(null, ui.scheduledTodayId)
    }

    @Test
    fun sesion_viva_marca_contador_y_bloquea() = runBlocking {
        val repo = TestHomeRepository(
            HomeUiState(
                routines = listOf(routine("r1", "Torso"), routine("r2", "Pierna")),
                progress = WeeklyProgress(),
                liveRoutineId = "r1",
                liveSessionId = "s-abc",
                liveElapsedSec = 3661,
                scheduledTodayId = "r2"
            )
        )
        val ui = repo.getHome()
        assertTrue(ui.hasLiveSession)
        assertEquals("r1", ui.liveRoutineId)
        assertEquals("s-abc", ui.liveSessionId)
        assertEquals(3661, ui.liveElapsedSec) // 01:01:01
        assertEquals("r2", ui.scheduledTodayId)
        // El resto de rutinas (r2) está programada hoy -> primary naranja.
        val hoy = ui.routines.find { it.id == "r2" }!!
        assertEquals("r2", hoy.id)
    }

    @Test
    fun observe_home_emite_cambios_de_viva() = runBlocking {
        val repo = TestHomeRepository(
            HomeUiState(routines = emptyList(), progress = WeeklyProgress(), liveRoutineId = null)
        )
        val first = repo.getHome()
        assertFalse(first.hasLiveSession)

        repo.setState(
            HomeUiState(
                routines = emptyList(), progress = WeeklyProgress(),
                liveRoutineId = "r1", liveSessionId = "s1", liveElapsedSec = 90,
                scheduledTodayId = null
            )
        )
        val second = repo.getHome()
        assertTrue(second.hasLiveSession)
        assertEquals("r1", second.liveRoutineId)
        assertEquals(90, second.liveElapsedSec)
    }
}
