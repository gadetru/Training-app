package com.mytrainingplan.app.feature.workout

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.mytrainingplan.app.data.repository.FakeWorkoutRepository
import com.mytrainingplan.app.domain.model.Exercise
import com.mytrainingplan.app.domain.model.PlannedSet
import com.mytrainingplan.app.domain.model.RoutineExercise
import com.mytrainingplan.app.domain.model.SetEntry
import com.mytrainingplan.app.domain.model.WorkoutExerciseUi
import com.mytrainingplan.app.domain.model.WorkoutUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Prueba de interacción del spec 013 en dispositivo (fallback `adb` del
 * `/verifier`: la inyección del agente MCP está denegada en este POCO con
 * `INJECT_EVENTS`, pero la regla de Compose actúa en proceso y sí puede
 * pulsar). Sin Hilt: `WorkoutContent` es stateless (no toca `hiltViewModel()`),
 * se alimenta con fakes + `WorkoutViewModel` real con `FakeWorkoutRepository`
 * para el test del cronómetro.
 */
class Workout013InteractionTest {

    @get:Rule
    val rule = createComposeRule()

    private val press = Exercise(
        id = "pectorals/bench",
        slug = "bench",
        name = "Press banca",
        muscle = "pectorals",
        bodyPart = "chest",
        equipment = "barbell",
        category = "strength"
    )
    private val row = Exercise(
        id = "back/dumbbell-row",
        slug = "dumbbell-row",
        name = "Remo mancuerna",
        muscle = "back",
        bodyPart = "back",
        equipment = "dumbbell",
        category = "strength"
    )

    private fun entry(
        id: String,
        n: Int,
        weight: Double = 60.0,
        reps: Int = 10,
        rest: Int = 90,
        done: Boolean = false
    ) = SetEntry(
        id = id, sessionId = "s1", exerciseId = press.id, plannedSetId = "p$n",
        setNumber = n, reps = reps, weightKg = weight, restSeconds = rest, done = done
    )

    private fun singleSetState(
        paused: Boolean = true,
        elapsed: Int = 0,
        weight: Double = 60.0,
        reps: Int = 10,
        rest: Int = 90
    ) = WorkoutUiState(
        sessionId = "s1",
        routineName = "Rutina 2",
        elapsedSec = elapsed,
        isPaused = paused,
        exercises = listOf(
            WorkoutExerciseUi(
                routineExercise = RoutineExercise(id = "re1", routineId = "r1", exerciseId = press.id),
                exercise = press,
                entries = listOf(entry("e1", 1, weight, reps, rest)),
                planned = listOf(
                    PlannedSet(id = "p1", routineExerciseId = "re1", setNumber = 1, targetReps = reps, weightKg = weight, restSeconds = rest)
                ),
                expanded = true
            )
        ),
        doneCount = 0,
        totalCount = 1
    )

    private fun twoExerciseState() = WorkoutUiState(
        sessionId = "s1",
        routineName = "Rutina 2",
        elapsedSec = 0,
        isPaused = true,
        exercises = listOf(
            WorkoutExerciseUi(
                routineExercise = RoutineExercise(id = "reA", routineId = "r1", exerciseId = press.id),
                exercise = press,
                entries = listOf(
                    entry("eA1", 1).copy(exerciseId = press.id),
                    entry("eA2", 2).copy(exerciseId = press.id)
                ),
                planned = listOf(
                    PlannedSet(id = "pA1", routineExerciseId = "reA", setNumber = 1, targetReps = 10, weightKg = 60.0, restSeconds = 90),
                    PlannedSet(id = "pA2", routineExerciseId = "reA", setNumber = 2, targetReps = 10, weightKg = 60.0, restSeconds = 90)
                ),
                expanded = true
            ),
            WorkoutExerciseUi(
                routineExercise = RoutineExercise(id = "reB", routineId = "r1", exerciseId = row.id),
                exercise = row,
                entries = listOf(
                    SetEntry(id = "eB1", sessionId = "s1", exerciseId = row.id, plannedSetId = "pB1", setNumber = 1, reps = 12, weightKg = 20.0, restSeconds = 60),
                    SetEntry(id = "eB2", sessionId = "s1", exerciseId = row.id, plannedSetId = "pB2", setNumber = 2, reps = 12, weightKg = 20.0, restSeconds = 60)
                ),
                planned = listOf(
                    PlannedSet(id = "pB1", routineExerciseId = "reB", setNumber = 1, targetReps = 12, weightKg = 20.0, restSeconds = 60),
                    PlannedSet(id = "pB2", routineExerciseId = "reB", setNumber = 2, targetReps = 12, weightKg = 20.0, restSeconds = 60)
                ),
                expanded = false
            )
        ),
        doneCount = 0,
        totalCount = 4
    )

    @Test
    fun modal_kg_abre_con_valor_y_guardar_propaga_signo_reps_pausa() {
        var captured: Triple<Double, Int, Int>? = null
        rule.setContent {
            MaterialTheme {
                WorkoutContent(
                    uiState = singleSetState(),
                    onUpdateEntry = { _, kg, reps, rest -> captured = Triple(kg, reps, rest) }
                )
            }
        }

        // Criterios 1-2: el tap en KG abre el modal con el valor actual.
        rule.onNodeWithTag("setKg:e1").performClick()
        rule.onNodeWithText("Editar serie 1").assertIsDisplayed()
        rule.onNodeWithTag("setEditKg").assertTextContains("60")
        rule.onNodeWithTag("setEditReps").assertTextContains("10")
        rule.onNodeWithTag("setEditRest").assertTextContains("90")

        // KG con signo + REPS + PAUSA en el mismo modal, guardado de golpe.
        rule.onNodeWithTag("setEditKg").performTextClearance()
        rule.onNodeWithTag("setEditKg").performTextInput("-10")
        rule.onNodeWithTag("setEditReps").performTextClearance()
        rule.onNodeWithTag("setEditReps").performTextInput("12")
        rule.onNodeWithTag("setEditRest").performTextClearance()
        rule.onNodeWithTag("setEditRest").performTextInput("75")
        rule.onNodeWithText("Guardar").performClick()

        assertEquals(Triple(-10.0, 12, 75), captured)
        rule.onNodeWithText("Editar serie 1").assertIsNotDisplayed()
    }

    @Test
    fun modal_cancelar_no_guarda() {
        var captured: Triple<Double, Int, Int>? = null
        rule.setContent {
            MaterialTheme {
                WorkoutContent(
                    uiState = singleSetState(),
                    onUpdateEntry = { _, kg, reps, rest -> captured = Triple(kg, reps, rest) }
                )
            }
        }

        rule.onNodeWithTag("setReps:e1").performClick()
        rule.onNodeWithText("Editar serie 1").assertIsDisplayed()
        rule.onNodeWithTag("setEditKg").performTextClearance()
        rule.onNodeWithTag("setEditKg").performTextInput("99")
        rule.onNodeWithText("Cancelar").performClick()

        assertEquals(null, captured)
        rule.onNodeWithText("Editar serie 1").assertIsNotDisplayed()
    }

    @Test
    fun arranque_detenido_comenzar_y_textos_coherentes() {
        var toggles = 0
        rule.setContent {
            MaterialTheme {
                WorkoutContent(
                    uiState = singleSetState(paused = true, elapsed = 0),
                    onPauseToggle = { toggles++ }
                )
            }
        }

        // Criterio 3: 00:00 quieto + Comenzar + EN PAUSA coherentes.
        rule.onNodeWithText("00:00").assertIsDisplayed()
        rule.onNodeWithText("Comenzar").assertIsDisplayed()
        rule.onNodeWithText("ENTRENAMIENTO EN PAUSA").assertIsDisplayed()
        rule.onNodeWithText("Comenzar").performClick()
        assertEquals(1, toggles)
    }

    @Test
    fun en_marcha_pausar_y_activo_coherentes() {
        rule.setContent {
            MaterialTheme {
                WorkoutContent(
                    uiState = singleSetState(paused = false, elapsed = 5)
                )
            }
        }

        // En marcha: Pausar + ACTIVO coherentes.
        rule.onNodeWithText("Pausar").assertIsDisplayed()
        rule.onNodeWithText("ENTRENAMIENTO ACTIVO").assertIsDisplayed()
    }

    @Test
    fun colapsado_un_tap_expande() {
        val toggled = mutableListOf<String>()
        rule.setContent {
            MaterialTheme {
                WorkoutContent(
                    uiState = twoExerciseState(),
                    onToggleExpanded = { toggled.add(it) }
                )
            }
        }

        // Criterio 6a: 1 tap en la tarjeta colapsada expande. Tap de dedo
        // real vía hit-testing (offset en la zona de la flecha, sin tap
        // propio): `performClick()` sobre el nodo fusionado no atraviesa
        // igual la jerarquía que un dedo. Scroll primero por el dock.
        rule.onNodeWithTag("exerciseCard:reB").performScrollTo()
        val card = rule.onNodeWithTag("exerciseCard:reB").fetchSemanticsNode()
        rule.onNodeWithTag("exerciseCard:reB").performTouchInput {
            click(
                position = Offset(
                    card.size.width * 0.9f,
                    card.size.height * 0.5f
                )
            )
        }
        assertEquals(listOf("reB"), toggled)
    }

    @Test
    fun nombre_abre_ficha_sin_expandir() {
        val toggled = mutableListOf<String>()
        val opened = mutableListOf<String>()
        rule.setContent {
            MaterialTheme {
                WorkoutContent(
                    uiState = twoExerciseState(),
                    onToggleExpanded = { toggled.add(it) },
                    onExerciseClick = { opened.add(it) }
                )
            }
        }

        // Criterio 6b: el nombre abre la ficha y no expande a la vez.
        rule.onNodeWithText("Remo mancuerna", substring = true).performClick()
        assertEquals(listOf(row.id), opened)
        assertTrue(toggled.isEmpty())
    }

    @Test
    fun viewmodel_arranque_detenido_tick_pausa_reanuda() {
        val vm = WorkoutViewModel(FakeWorkoutRepository())
        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        val job = scope.launch { vm.uiState.collect {} }
        try {
            vm.openSession("rutina-inexistente-013")
            val deadline = System.currentTimeMillis() + 10_000
            while (vm.uiState.value.sessionId.isEmpty() && System.currentTimeMillis() < deadline) {
                Thread.sleep(100)
            }
            // Criterio 3 a nivel ViewModel: arranca detenido en 0.
            assertTrue(vm.uiState.value.sessionId.isNotEmpty())
            assertTrue(vm.uiState.value.isPaused)
            assertEquals(0, vm.uiState.value.elapsedSec)

            // Criterio 4: Comenzar arranca el conteo.
            vm.onPauseToggle()
            Thread.sleep(3_000)
            assertFalse(vm.uiState.value.isPaused)
            assertTrue(vm.uiState.value.elapsedSec >= 2)

            // Criterio 5: Pausar congela.
            vm.onPauseToggle()
            val frozen = vm.uiState.value.elapsedSec
            Thread.sleep(1_500)
            assertTrue(vm.uiState.value.isPaused)
            assertEquals(frozen, vm.uiState.value.elapsedSec)
        } finally {
            job.cancel()
            scope.cancel()
        }
    }
}
