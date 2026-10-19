package com.mytrainingplan.app.feature.workout

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mytrainingplan.app.data.repository.FakeExerciseRepository
import com.mytrainingplan.app.data.repository.FakeRoutineRepository
import com.mytrainingplan.app.data.repository.FakeWorkoutRepository
import com.mytrainingplan.app.domain.model.AccentColor
import com.mytrainingplan.app.domain.model.FootKind
import com.mytrainingplan.app.domain.model.HomeUiState
import com.mytrainingplan.app.domain.model.RoutineSummary
import com.mytrainingplan.app.domain.model.WeeklyProgress
import com.mytrainingplan.app.feature.home.HomeContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Smoke del spec 022 en emulador (patrón stateless de los smokes 011/013,
 * sin Hilt): la card de Home en marcha (EN MARCHA + reloj + Reanudar/
 * Terminar/Descartar con sus tags, resto de Iniciar no clicable) y, a nivel
 * ViewModel con [FakeWorkoutRepository], reanudar conserva series y
 * cronómetro y Terminar reactiva los Iniciar (guardia anti-doble).
 */
class Workout022ResumeTest {

    @get:Rule
    val rule = createComposeRule()

    private fun routine(id: String, title: String, accent: AccentColor) =
        RoutineSummary(
            id = id, title = title, tags = emptyList(), durationMin = 45, exerciseCount = 3,
            accent = accent, footNote = "", footKind = FootKind.INFO
        )

    private fun liveState() = HomeUiState(
        firstName = "Carlos",
        routines = listOf(routine("r1", "Torso", AccentColor.ORANGE), routine("r2", "Pierna", AccentColor.VOLT)),
        progress = WeeklyProgress(),
        liveRoutineId = "r1",
        liveSessionId = "s1",
        liveElapsedSec = 125,
        scheduledTodayId = "r2"
    )

    @Test
    fun home_card_en_marcha_reloj_ctas_y_otros_iniciar_no_clicables() {
        var resumed: Pair<String, String>? = null
        var finished = 0
        var discarded = 0
        var started: String? = null
        rule.setContent {
            MaterialTheme {
                HomeContent(
                    uiState = liveState(),
                    // HomeContent recibe el sessionId de la viva como
                    // parámetro aparte (HomeScreen se lo pasa de uiState).
                    liveSessionId = "s1",
                    onStart = { started = it },
                    onLiveResume = { r, s -> resumed = r to s },
                    onLiveFinish = { finished++ },
                    onLiveDiscard = { discarded++ }
                )
            }
        }

        // Card viva: pie EN MARCHA + reloj 02:05 + 3 CTAs con sus tags.
        rule.onNodeWithText("EN MARCHA").assertIsDisplayed()
        rule.onNodeWithText("02:05").assertIsDisplayed()
        rule.onNodeWithTag("resumeWorkout:r1").assertIsDisplayed()
        rule.onNodeWithTag("finishWorkout:r1").assertIsDisplayed()
        rule.onNodeWithTag("discardWorkout:r1").assertIsDisplayed()

        // Reanudar lleva routineId + sessionId de la viva.
        rule.onNodeWithTag("resumeWorkout:r1").performClick()
        assertEquals("r1" to "s1", resumed)

        // Terminar y Descartar disparan sus callbacks (el diálogo de
        // confirmación vive en HomeScreen, se cubre en la prueba MCP).
        rule.onNodeWithTag("finishWorkout:r1").performClick()
        assertEquals(1, finished)
        rule.onNodeWithTag("discardWorkout:r1").performClick()
        assertEquals(1, discarded)

        // Solo queda un "Iniciar" (el de r2; la viva lo sustituye) y no
        // es clicable con sesión viva. El botón disabled conserva la acción
        // OnClick en semántica pero con [Disabled]: assertIsNotEnabled.
        rule.onAllNodesWithText("Iniciar").assertCountEquals(1)
        started = null
        rule.onAllNodesWithText("Iniciar")[0].assertIsNotEnabled()
        assertEquals(null, started)
    }

    @Test
    fun viewmodel_reanudar_conserva_series_y_cronometro_y_terminar_reactiva() {
        val exercises = FakeExerciseRepository()
        val routines = FakeRoutineRepository(exercises = exercises)
        val workouts = FakeWorkoutRepository(routines = routines, exercises = exercises)
        val routineId = runBlocking {
            val id = routines.createRoutine(name = "Torso")
            // La rutina se crea vacía: añadir un ejercicio para que la
            // sesión tenga series que marcar (id de FakeExerciseRepository).
            routines.addExercises(id, listOf("quads/barbell-bench-squat"))
            id
        }

        // Sesión 1: Comenzar (timer ACTIVO) + marcar la primera serie.
        val vm1 = WorkoutViewModel(workouts)
        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        val job1 = scope.launch { vm1.uiState.collect {} }
        try {
            vm1.openSession(routineId)
            waitUntil { vm1.uiState.value.sessionId.isNotEmpty() }
            val sid = vm1.uiState.value.sessionId
            vm1.onPauseToggle() // SIN_EMPEZAR -> timerStart
            waitUntil { !vm1.uiState.value.isPaused }
            // El detalle de la rutina llega async (repo): esperar a que
            // cargue antes de leer el id de la primera serie.
            waitUntil { vm1.uiState.value.exercises.isNotEmpty() }
            val entryId = vm1.uiState.value.exercises.first().entries.first().id
            vm1.onToggleDone(entryId)
            waitUntil { vm1.uiState.value.doneCount == 1 }

            // "Salir" (la sesión queda viva en el repo) y reanudar con un
            // ViewModel nuevo: mismas series y cronómetro continuado.
            val vm2 = WorkoutViewModel(workouts)
            val job2 = scope.launch { vm2.uiState.collect {} }
            try {
                vm2.openSession(routineId, sid)
                waitUntil { vm2.uiState.value.sessionId == sid }
                assertEquals(1, vm2.uiState.value.doneCount)
                assertTrue(!vm2.uiState.value.isPaused) // ACTIVO: sigue corriendo
                // El cronómetro continúa: esperar a que el reloj llegue a 1s
                // (si estuviera congelado, este waitUntil expiraría).
                waitUntil { vm2.uiState.value.elapsedSec >= 1 }

                // Terminar desde el flujo: la sesión se cierra (el fake la
                // retira del mapa) y un Iniciar posterior crea una NUEVA
                // (la guardia ya no devuelve la vieja).
                vm2.onFinish {}
                waitUntil { runBlocking { workouts.observeSession(sid).firstOrNull() == null } }
                val nueva = runBlocking { workouts.startSession(routineId) }
                assertNotEquals(sid, nueva)
            } finally {
                job2.cancel()
            }
        } finally {
            job1.cancel()
            scope.cancel()
        }
    }

    private fun waitUntil(timeoutMs: Long = 10_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition() && System.currentTimeMillis() < deadline) {
            Thread.sleep(100)
        }
        assertTrue("condición no alcanzada a tiempo", condition())
    }
}
