package com.mytrainingplan.app.feature.workout

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.mytrainingplan.app.domain.model.Exercise
import com.mytrainingplan.app.domain.model.PlannedSet
import com.mytrainingplan.app.domain.model.RoutineExercise
import com.mytrainingplan.app.domain.model.SetEntry
import com.mytrainingplan.app.domain.model.WorkoutExerciseUi
import com.mytrainingplan.app.domain.model.WorkoutUiState
import org.junit.Rule
import org.junit.Test

/**
 * Smoke del spec 020: el modal de serie limpia ceros a la izquierda mientras
 * se teclea. `WorkoutContent` es stateless (sin Hilt), se alimenta con un
 * `WorkoutUiState` de prueba (misma patrón que `Workout013InteractionTest`).
 */
class NumberInputSmokeTest {

    @get:Rule
    val rule = createComposeRule()

    private val press = Exercise(
        id = "pectorals/bench", slug = "bench", name = "Press banca",
        muscle = "pectorals", bodyPart = "chest", equipment = "barbell", category = "strength"
    )

    private fun entry(id: String, n: Int, weight: Double = 60.0, reps: Int = 10, rest: Int = 90) =
        SetEntry(id = id, sessionId = "s1", exerciseId = press.id, plannedSetId = "p$n",
            setNumber = n, reps = reps, weightKg = weight, restSeconds = rest, done = false)

    private fun singleSetState(weight: Double = 60.0, reps: Int = 10, rest: Int = 90) = WorkoutUiState(
        sessionId = "s1", routineName = "Rutina 2", elapsedSec = 0, isPaused = true,
        exercises = listOf(
            WorkoutExerciseUi(
                routineExercise = RoutineExercise(id = "re1", routineId = "r1", exerciseId = press.id),
                exercise = press,
                entries = listOf(entry("e1", 1, weight, reps, rest)),
                planned = listOf(PlannedSet(id = "p1", routineExerciseId = "re1", setNumber = 1, targetReps = reps, weightKg = weight, restSeconds = rest)),
                expanded = true
            )
        ), doneCount = 0, totalCount = 1
    )

    @Test
    fun modal_reps_y_kg_limpiaron_ceros_iniciales_al_teclear() {
        rule.setContent {
            MaterialTheme {
                WorkoutContent(uiState = singleSetState(), onUpdateEntry = { _, _, _, _ -> })
            }
        }

        rule.onNodeWithTag("setKg:e1").performClick()
        rule.onNodeWithText("Editar serie 1").assertIsDisplayed()

        // REPS: 012 -> 12
        rule.onNodeWithTag("setEditReps").performTextClearance()
        rule.onNodeWithTag("setEditReps").performTextInput("012")
        rule.onNodeWithTag("setEditReps").assertTextContains("12")

        // KG: 0,5 -> 0.5
        rule.onNodeWithTag("setEditKg").performTextClearance()
        rule.onNodeWithTag("setEditKg").performTextInput("0,5")
        rule.onNodeWithTag("setEditKg").assertTextContains("0.5")
    }
}
