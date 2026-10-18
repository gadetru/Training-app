package com.mytrainingplan.app.feature.routines

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mytrainingplan.app.domain.model.Exercise
import com.mytrainingplan.app.domain.model.PlannedSet
import com.mytrainingplan.app.domain.model.RoutineEditUiState
import com.mytrainingplan.app.domain.model.RoutineExercise
import com.mytrainingplan.app.domain.model.RoutineExerciseUi
import org.junit.Test

/**
 * Smoke Editar Rutina - espec 023 (duración vacía/estimada/real).
 * Ataca `RoutineEditContent` stateless con estado fake (sin Hilt).
 * Los casos de clic del interruptor se cubren con estado local recordado.
 */
class RoutineEdit023DurationTest {

    @get:Rule
    val rule = createComposeRule()

    private val sentadilla = Exercise(
        id = "quads/squat", slug = "squat", name = "Sentadilla con barra",
        muscle = "quads", bodyPart = "legs", equipment = "barbell", category = "strength", gifUrl = ""
    )

    private fun serie(restSeconds: Int) = PlannedSet(
        id = "s1", routineExerciseId = "re1", setNumber = 1,
        targetReps = 10, weightKg = 60.0, restSeconds = restSeconds
    )

    private fun item(sets: List<PlannedSet>) = RoutineExerciseUi(
        routineExercise = RoutineExercise(id = "re1", routineId = "r1", exerciseId = sentadilla.id),
        exercise = sentadilla,
        sets = sets,
        expanded = true
    )

    @Test
    fun rutina_nueva_duracion_vacia_guion() {
        rule.setContent {
            MaterialTheme {
                RoutineEditContent(
                    uiState = RoutineEditUiState(
                        name = "Nueva", durationMin = 0, durationManual = false,
                        exercises = emptyList()
                    ),
                    onDurationChange = {},
                    onDurationModeChange = {}
                )
            }
        }
        rule.onNodeWithText("—").assertIsDisplayed()
        rule.onNodeWithText("AUTO").assertIsDisplayed()
    }

    @Test
    fun auto_con_series_muestra_estimacion() {
        rule.setContent {
            MaterialTheme {
                RoutineEditContent(
                    uiState = RoutineEditUiState(
                        name = "Auto", durationMin = 0, durationManual = false,
                        exercises = listOf(item(listOf(serie(90))))
                    ),
                    onDurationChange = {},
                    onDurationModeChange = {}
                )
            }
        }
        rule.onNodeWithText("≈ 2 min").assertIsDisplayed()
    }

    @Test
    fun manual_muestra_numero_escrito() {
        rule.setContent {
            MaterialTheme {
                RoutineEditContent(
                    uiState = RoutineEditUiState(
                        name = "Manual", durationMin = 30, durationManual = true,
                        exercises = emptyList()
                    ),
                    onDurationChange = {},
                    onDurationModeChange = {}
                )
            }
        }
        rule.onNodeWithText("30 min").assertIsDisplayed()
    }
}
