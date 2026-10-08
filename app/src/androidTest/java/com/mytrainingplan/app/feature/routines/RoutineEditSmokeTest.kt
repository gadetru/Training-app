package com.mytrainingplan.app.feature.routines

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.mytrainingplan.app.domain.model.Exercise
import com.mytrainingplan.app.domain.model.PlannedSet
import com.mytrainingplan.app.domain.model.RoutineEditUiState
import com.mytrainingplan.app.domain.model.RoutineExercise
import com.mytrainingplan.app.domain.model.RoutineExerciseUi
import org.junit.Rule
import org.junit.Test

/**
 * Smoke Editar Rutina en emulador Pixel 6 API 34 (movido a su espejo
 * `feature/routines` en el paso 13 del spec 011, sin cambiar lógica).
 * Sin Hilt: ataca RoutineEditContent stateless con estado fake.
 */
class RoutineEditSmokeTest {

    @get:Rule
    val rule = createComposeRule()

    private fun fakeState() = RoutineEditUiState(
        routineId = "r1",
        name = "Pierna & Glúteos",
        durationMin = 45,
        tags = listOf("Pierna"),
        exercises = listOf(
            RoutineExerciseUi(
                routineExercise = RoutineExercise(id = "re1", routineId = "r1", exerciseId = "quads/squat"),
                exercise = Exercise(
                    id = "quads/squat",
                    slug = "squat",
                    name = "Sentadilla con barra",
                    muscle = "quads",
                    bodyPart = "legs",
                    equipment = "barbell",
                    category = "strength",
                    gifUrl = ""
                ),
                sets = listOf(
                    PlannedSet(id = "s1", routineExerciseId = "re1", setNumber = 1, targetReps = 12, weightKg = 60.0, restSeconds = 90),
                    PlannedSet(id = "s2", routineExerciseId = "re1", setNumber = 2, targetReps = 10, weightKg = 80.0, restSeconds = 120)
                ),
                expanded = true
            )
        )
    )

    @Test
    fun routineEdit_muestra_nombre_ejercicio_y_footer() {
        rule.setContent {
            MaterialTheme {
                RoutineEditContent(uiState = fakeState())
            }
        }

        rule.onNodeWithTag("routineEditRoot").assertIsDisplayed()
        rule.onNodeWithText("Pierna & Glúteos").assertIsDisplayed()
        rule.onNodeWithText("Sentadilla con barra").assertIsDisplayed()
        rule.onNodeWithTag("routineEditAddExercise").assertIsDisplayed()
        rule.onNodeWithText("+ Añadir Nuevo Ejercicio a la Rutina").assertIsDisplayed()
        rule.onNodeWithTag("routineEditSave").assertIsDisplayed()
        rule.onNodeWithText("Finalizar y Guardar Rutina").assertIsDisplayed()
        rule.onNodeWithText("1 ejercicios • 2 series totales").assertIsDisplayed()
    }

    @Test
    fun routineEdit_estado_vacio_invita_a_anadir() {
        rule.setContent {
            MaterialTheme {
                RoutineEditContent(uiState = RoutineEditUiState(name = "Nueva rutina"))
            }
        }

        rule.onNodeWithTag("routineEditRoot").assertIsDisplayed()
        rule.onNodeWithText("Nueva rutina").assertIsDisplayed()
        rule.onNodeWithText("Sin ejercicios: añade uno para ver los grupos musculares").assertIsDisplayed()
        rule.onNodeWithTag("routineEditAddExercise").assertIsDisplayed()
    }
}
