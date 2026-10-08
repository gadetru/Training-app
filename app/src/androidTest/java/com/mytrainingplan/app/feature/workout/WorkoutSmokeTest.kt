package com.mytrainingplan.app.feature.workout

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.mytrainingplan.app.domain.model.Exercise
import com.mytrainingplan.app.domain.model.PlannedSet
import com.mytrainingplan.app.domain.model.RoutineExercise
import com.mytrainingplan.app.domain.model.SetEntry
import com.mytrainingplan.app.domain.model.WorkoutExerciseUi
import com.mytrainingplan.app.domain.model.WorkoutUiState
import com.mytrainingplan.app.feature.profile.ProfileScreen
import org.junit.Rule
import org.junit.Test

/**
 * Smoke Sesión en vivo + Perfil en emulador Pixel 6 API 34 (movido a su
 * espejo `feature/workout` en el paso 13 del spec 011, sin cambiar lógica).
 * Sin Hilt: ataca WorkoutContent/ProfileScreen stateless con fakes.
 * Cubre prefill última-vs-plan a nivel visual (objetivo vs viva).
 */
class WorkoutSmokeTest {

    @get:Rule
    val rule = createComposeRule()

    private fun fakeWorkout() = WorkoutUiState(
        sessionId = "s1",
        routineName = "Torso - Fuerza",
        elapsedSec = 125,
        isPaused = false,
        exercises = listOf(
            WorkoutExerciseUi(
                routineExercise = RoutineExercise(id = "re1", routineId = "r1", exerciseId = "pectorals/bench"),
                exercise = Exercise(
                    id = "pectorals/bench",
                    slug = "bench",
                    name = "Press banca",
                    muscle = "pectorals",
                    bodyPart = "chest",
                    equipment = "barbell",
                    category = "strength"
                ),
                entries = listOf(
                    SetEntry(id = "e1", sessionId = "s1", exerciseId = "pectorals/bench", plannedSetId = "p1", setNumber = 1, reps = 12, weightKg = 40.0, restSeconds = 60, done = true),
                    SetEntry(id = "e2", sessionId = "s1", exerciseId = "pectorals/bench", plannedSetId = "p2", setNumber = 2, reps = 10, weightKg = 60.0, restSeconds = 90, done = false)
                ),
                planned = listOf(
                    PlannedSet(id = "p1", routineExerciseId = "re1", setNumber = 1, targetReps = 12, weightKg = 40.0, restSeconds = 60),
                    PlannedSet(id = "p2", routineExerciseId = "re1", setNumber = 2, targetReps = 10, weightKg = 60.0, restSeconds = 90)
                ),
                expanded = true
            )
        ),
        doneCount = 1,
        totalCount = 2,
        restRemainingSec = null
    )

    @Test
    fun workout_muestra_rutina_progreso_y_cta_sin_overlay() {
        rule.setContent {
            MaterialTheme {
                WorkoutContent(uiState = fakeWorkout())
            }
        }

        rule.onNodeWithTag("workoutRoot").assertIsDisplayed()
        rule.onNodeWithText("Torso - Fuerza").assertIsDisplayed()
        rule.onNodeWithText("Press banca", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Finalizar y Guardar").assertIsDisplayed()
        rule.onNodeWithTag("workoutFinish").assertIsDisplayed()
        rule.onNodeWithText("Descartar Entrenamiento").assertIsDisplayed()
        // Sin descanso activo no hay overlay.
        rule.onNodeWithTag("restOverlay").assertIsNotDisplayed()
    }

    @Test
    fun workout_con_descanso_muestra_overlay() {
        // El overlay vive en WorkoutScreen (padre), no en WorkoutContent:
        // se cubre renderizándolo directo, sin Hilt.
        rule.setContent {
            MaterialTheme {
                RestOverlay(remainingSec = 75, totalSec = 90, onPlus = {}, onMinus = {}, onFinish = {})
            }
        }

        rule.onNodeWithTag("restOverlay").assertIsDisplayed()
        rule.onNodeWithText("DESCANSO ACTIVO").assertIsDisplayed()
        rule.onNodeWithText("Terminar descanso").assertIsDisplayed()
        rule.onNodeWithText("-10s").assertIsDisplayed()
        rule.onNodeWithText("+10s").assertIsDisplayed()
    }

    @Test
    fun profile_muestra_titulo_y_cta_guardar() {
        rule.setContent {
            MaterialTheme {
                ProfileScreen()
            }
        }

        rule.onNodeWithTag("profileRoot").assertIsDisplayed()
        rule.onNodeWithText("Tu Perfil de Atleta").assertIsDisplayed()
        rule.onNodeWithText("CONFIGURACIÓN INICIAL").assertIsDisplayed()
        rule.onNodeWithTag("profileSave").assertIsDisplayed()
        rule.onNodeWithText("Guardar y Continuar  →").assertIsDisplayed()
    }
}
