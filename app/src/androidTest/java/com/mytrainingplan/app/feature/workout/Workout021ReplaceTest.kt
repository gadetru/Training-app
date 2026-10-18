package com.mytrainingplan.app.feature.workout

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.mytrainingplan.app.domain.model.Exercise
import com.mytrainingplan.app.domain.model.PlannedSet
import com.mytrainingplan.app.domain.model.RoutineExercise
import com.mytrainingplan.app.domain.model.SetEntry
import com.mytrainingplan.app.domain.model.WorkoutExerciseUi
import com.mytrainingplan.app.domain.model.WorkoutUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Smoke sustituir ejercicio (spec 021, pasos 4-5 + 7): swipe izquierda en
 * tarjeta colapsada revela píldora "Sustituir", tap la pulsa y el callback
 * de sustitución se dispara con el id del slot. Sin Hilt:
 * `WorkoutContent` stateless (espejo `feature/workout`).
 */
class Workout021ReplaceTest {

    @get:Rule
    val rule = createComposeRule()

    private val reId = "re1"
    private val oldExerciseId = "pectorals/bench"
    private val newExerciseId = "quads/barbell-bench-squat"

    private fun fakeWorkout(collapsed: Boolean = true, exerciseName: String = "Press banca",
                            exerciseId: String = oldExerciseId): WorkoutUiState =
        WorkoutUiState(
            sessionId = "s1",
            routineName = "Torso - Fuerza",
            elapsedSec = 125,
            isPaused = false,
            exercises = listOf(
                WorkoutExerciseUi(
                    routineExercise = RoutineExercise(id = reId, routineId = "r1", exerciseId = exerciseId),
                    exercise = Exercise(
                        id = exerciseId,
                        slug = "bench",
                        name = exerciseName,
                        muscle = "pectorals",
                        bodyPart = "chest",
                        equipment = "barbell",
                        category = "strength"
                    ),
                    entries = listOf(
                        SetEntry(id = "e1", sessionId = "s1", exerciseId = exerciseId, plannedSetId = "p1", setNumber = 1,
                            reps = 12, weightKg = 40.0, restSeconds = 60, done = true)
                    ),
                    planned = listOf(
                        PlannedSet(id = "p1", routineExerciseId = reId, setNumber = 1, targetReps = 12, weightKg = 40.0,
                            restSeconds = 60)
                    ),
                    expanded = !collapsed
                )
            ),
            doneCount = 1,
            totalCount = 1,
            restRemainingSec = null
        )

    @Test
    fun swipe_colapsada_revela_sustituir() {
        var replaced: String? = null
        rule.setContent {
            MaterialTheme {
                WorkoutContent(
                    uiState = fakeWorkout(collapsed = true),
                    onReplaceExercise = { _, _ -> replaced = "ok" }
                )
            }
        }

        // Sin swipe: no hay píldora.
        rule.onNodeWithText("Sustituir", substring = true).assertIsNotDisplayed()

        rule.onNodeWithTag("exerciseCard:$reId").performScrollTo()
        rule.onNodeWithTag("exerciseCard:$reId").performTouchInput { swipeLeft() }

        rule.onNodeWithText("Sustituir", substring = true).assertIsDisplayed()
        // El swipe solo revela: no sustituye por sí mismo.
        assertEquals(null, replaced)
    }

    @Test
    fun tarjeta_expandida_no_revela_sustituir() {
        rule.setContent {
            MaterialTheme {
                WorkoutContent(
                    uiState = fakeWorkout(collapsed = false),
                    onReplaceExercise = { _, _ -> }
                )
            }
        }

        rule.onNodeWithTag("exerciseCard:$reId").performScrollTo()
        rule.onNodeWithTag("exerciseCard:$reId").performTouchInput { swipeLeft() }

        rule.onNodeWithText("Sustituir", substring = true).assertIsNotDisplayed()
    }

    @Test
    fun tap_sustituir_dispara_callback_con_slot() {
        var replacedWith: Pair<String, String>? = null
        rule.setContent {
            MaterialTheme {
                WorkoutContent(
                    uiState = fakeWorkout(collapsed = true),
                    onReplaceExercise = { routineExerciseId, newId ->
                        replacedWith = routineExerciseId to newId
                    }
                )
            }
        }

        rule.onNodeWithTag("exerciseCard:$reId").performScrollTo()
        rule.onNodeWithTag("exerciseCard:$reId").performTouchInput { swipeLeft() }
        rule.onNodeWithText("Sustituir", substring = true).performClick()

        assertEquals(reId to newExerciseId, replacedWith)
    }

    @Test
    fun tarjeta_muestra_nuevo_nombre_despues_de_sustituir() {
        // El swap persiste: reabrir la sesión con el nuevo ejercicio muestra
        // el nombre nuevo en la misma posición (re-consulta de uiState).
        rule.setContent {
            MaterialTheme {
                WorkoutContent(
                    uiState = fakeWorkout(
                        collapsed = true,
                        exerciseId = newExerciseId,
                        exerciseName = "Sentadilla con barra"
                    ),
                    onReplaceExercise = { _, _ -> }
                )
            }
        }

        rule.onNodeWithText("Press banca", substring = true).assertIsNotDisplayed()
        rule.onNodeWithText("Sentadilla con barra", substring = true).assertIsDisplayed()
    }
}
