package com.mytrainingplan.app.feature.exercises

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.mytrainingplan.app.data.repository.FakeExerciseRepository
import com.mytrainingplan.app.domain.model.ExerciseFilter
import com.mytrainingplan.app.domain.model.ExercisesUiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

/**
 * Buscador del picker en dispositivo (spec 012, paso 9: `connectedDebugAndroidTest`
 * toca picker + ficha). Sin Hilt: ataca `ExercisePickerContent` stateless con
 * el fake en memoria (misma firma y mismo `applyFilter` que el Room).
 * La animación del hero de la ficha queda para verificación manual (el
 * `DetailBody` no es testeable sin Hilt).
 */
class PickerSearchTest {

    @get:Rule
    val rule = createComposeRule()

    private fun resultsFor(query: String, muscle: String? = null) = runBlocking {
        val filter = ExerciseFilter(
            query = query,
            muscles = if (muscle == null) emptySet() else setOf(muscle)
        )
        FakeExerciseRepository().observeExercises(filter).first()
    }

    @Test
    fun escribir_pierna_muestra_solo_pierna() {
        rule.setContent {
            MaterialTheme {
                ExercisePickerContent(
                    uiState = ExercisesUiState(
                        query = "pierna",
                        results = resultsFor("pierna")
                    )
                )
            }
        }

        rule.onNodeWithText("Prensa de piernas").assertIsDisplayed()
        rule.onAllNodesWithText("Press banca").assertCountEquals(0)
    }

    @Test
    fun teclear_emite_cada_pulsacion() {
        var captured = ""
        rule.setContent {
            MaterialTheme {
                ExercisePickerContent(
                    uiState = ExercisesUiState(
                        query = captured,
                        results = FakeExerciseRepository.defaultExercises()
                    ),
                    onQueryChange = { captured = it }
                )
            }
        }

        rule.onNode(hasSetTextAction()).performTextInput("pierna")

        assert(captured == "pierna")
    }

    @Test
    fun ayuda_desaparece_al_enfocar_antes_de_escribir() {
        rule.setContent {
            MaterialTheme {
                ExercisePickerContent(
                    uiState = ExercisesUiState(
                        results = FakeExerciseRepository.defaultExercises()
                    )
                )
            }
        }

        rule.onNodeWithText("Buscar ejercicio", substring = true).assertIsDisplayed()

        rule.onNode(hasSetTextAction()).performClick()

        rule.onAllNodesWithText("Buscar ejercicio", substring = true).assertCountEquals(0)
    }

    @Test
    fun chip_pecho_llama_con_su_clave() {
        var selected: String? = "centinela"
        rule.setContent {
            MaterialTheme {
                ExercisePickerContent(
                    uiState = ExercisesUiState(
                        results = FakeExerciseRepository.defaultExercises()
                    ),
                    onMuscleSelected = { selected = it }
                )
            }
        }

        rule.onNodeWithText("Pecho").performClick()

        assert(selected == "pecho")
    }
}
