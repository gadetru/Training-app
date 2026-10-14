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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Smoke edición en vivo (spec 016, pasos 3-5 + 7): menú ⋮ con sus dos
 * opciones, botón "+ Añadir serie", swipe "¿Borrar?" solo en pendientes y
 * aviso del descarte. Sin Hilt: `WorkoutContent` stateless (espejo
 * `feature/workout`, como `WorkoutSmokeTest`).
 */
class Workout016LiveEditTest {

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
    fun menu_muestra_editar_y_salir() {
        rule.setContent {
            MaterialTheme {
                WorkoutContent(uiState = fakeWorkout())
            }
        }

        rule.onNodeWithTag("workoutMenuButton").performClick()
        rule.onNodeWithText("Editar rutina").assertIsDisplayed()
        rule.onNodeWithText("Salir del entrenamiento").assertIsDisplayed()
    }

    @Test
    fun tap_editar_llama_callback_y_salir_vuelve() {
        var edited = false
        var backed = false
        rule.setContent {
            MaterialTheme {
                WorkoutContent(
                    uiState = fakeWorkout(),
                    onBack = { backed = true },
                    onEditRoutine = { edited = true }
                )
            }
        }

        rule.onNodeWithTag("workoutMenuButton").performClick()
        rule.onNodeWithText("Editar rutina").performClick()
        assertTrue(edited)

        rule.onNodeWithTag("workoutMenuButton").performClick()
        rule.onNodeWithText("Salir del entrenamiento").performClick()
        assertTrue(backed)
    }

    @Test
    fun boton_aniadir_visible_y_llama() {
        var added: String? = null
        rule.setContent {
            MaterialTheme {
                WorkoutContent(
                    uiState = fakeWorkout(),
                    onAddLiveSet = { added = it }
                )
            }
        }

        rule.onNodeWithTag("addLiveSet:re1").performScrollTo()
        rule.onNodeWithTag("addLiveSet:re1").assertIsDisplayed()
        rule.onNodeWithText("+ Añadir serie").assertIsDisplayed()
        rule.onNodeWithTag("addLiveSet:re1").performClick()
        assertEquals("re1", added)
    }

    @Test
    fun swipe_pendiente_muestra_borrar_y_tap_borra() {
        var deleted: String? = null
        rule.setContent {
            MaterialTheme {
                WorkoutContent(
                    uiState = fakeWorkout(),
                    onDeleteLiveSet = { deleted = it }
                )
            }
        }

        rule.onNodeWithTag("setRow:e2").performScrollTo()
        rule.onNodeWithTag("setRow:e2").performTouchInput { swipeLeft() }
        // El swipe solo revela: no borra por sí mismo (doble gesto del spec).
        assertNull(deleted)
        rule.onNodeWithText("¿Borrar?").assertIsDisplayed()
        rule.onNodeWithText("¿Borrar?").performClick()
        assertEquals("e2", deleted)
    }

    @Test
    fun swipe_hecha_no_ofrece_borrar() {
        var deleted: String? = null
        rule.setContent {
            MaterialTheme {
                WorkoutContent(
                    uiState = fakeWorkout(),
                    onDeleteLiveSet = { deleted = it }
                )
            }
        }

        // La hecha no lleva contenedor de swipe (el fondo "¿Borrar?" solo se
        // compone en pendientes): el gesto no hace nada y la fila sigue.
        rule.onNodeWithTag("setReps:e1").performTouchInput { swipeLeft() }
        rule.onNodeWithTag("setRow:e1").assertIsNotDisplayed()
        rule.onNodeWithTag("setReps:e1").assertIsDisplayed()
        assertNull(deleted)
    }

    @Test
    fun descartar_avisa_que_la_rutina_conserva_lo_editado() {
        rule.setContent {
            MaterialTheme {
                WorkoutContent(uiState = fakeWorkout())
            }
        }

        rule.onNodeWithText("Descartar Entrenamiento").performScrollTo()
        rule.onNodeWithText("Descartar Entrenamiento").performClick()
        rule.onNodeWithText("¿Descartar entrenamiento?").assertIsDisplayed()
        rule.onNodeWithText("sí queda en la rutina", substring = true).assertIsDisplayed()
    }
}
