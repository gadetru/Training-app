package com.mytrainingplan.app.feature.routines

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.mytrainingplan.app.domain.model.Exercise
import com.mytrainingplan.app.domain.model.PlannedSet
import com.mytrainingplan.app.domain.model.RoutineEditUiState
import com.mytrainingplan.app.domain.model.RoutineExercise
import com.mytrainingplan.app.domain.model.RoutineExerciseUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * Spec 026 (feedback): las celdas numéricas deben reflejar el borrador del
 * panel en vivo (antes de OK) y el número debe quedar centrado. Se ataca
 * `RoutineEditContent` en JVM con Robolectric (sin dispositivo) para que la
 * garantía corra en `testDebugUnitTest`/CI.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Mismo viewport que las fotos de Home: sin él Robolectric mide enano.
@Config(sdk = [34], qualifiers = "w411dp-h914dp")
class RoutineEditLiveCellTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val ejercicio = Exercise(
        id = "quads/squat",
        slug = "squat",
        name = "Sentadilla con barra",
        muscle = "quads",
        bodyPart = "legs",
        equipment = "barbell",
        category = "strength",
        gifUrl = ""
    )

    private fun estado(
        kg: Double = 60.0,
        reps: Int = 10,
        rest: Int = 90,
        durationManual: Boolean = false,
        durationMin: Int = 0
    ) = RoutineEditUiState(
        name = "Pierna",
        durationMin = durationMin,
        durationManual = durationManual,
        exercises = listOf(
            RoutineExerciseUi(
                routineExercise = RoutineExercise(
                    id = "re1",
                    routineId = "r1",
                    exerciseId = ejercicio.id
                ),
                exercise = ejercicio,
                sets = listOf(
                    PlannedSet(
                        id = "s1",
                        routineExerciseId = "re1",
                        setNumber = 1,
                        targetReps = reps,
                        weightKg = kg,
                        restSeconds = rest
                    )
                ),
                expanded = true
            )
        )
    )

    private fun conPeso(state: RoutineEditUiState, kg: Double) = state.copy(
        exercises = state.exercises.map { item ->
            item.copy(sets = item.sets.map { set -> set.copy(weightKg = kg) })
        }
    )

    private fun aplicar(state: RoutineEditUiState, field: SetField, raw: String) = state.copy(
        exercises = state.exercises.map { item ->
            item.copy(
                sets = item.sets.map { set ->
                    when (field) {
                        SetField.WEIGHT_KG -> set.copy(
                            weightKg = raw.replace(',', '.').toDoubleOrNull() ?: set.weightKg
                        )
                        SetField.TARGET_REPS -> set.copy(
                            targetReps = raw.toIntOrNull() ?: set.targetReps
                        )
                        SetField.REST_SECONDS -> set.copy(
                            restSeconds = raw.toIntOrNull() ?: set.restSeconds
                        )
                    }
                }
            )
        }
    )

    @Test
    fun al_cambiar_de_celda_autoguarda_y_enfoca_la_nueva() {
        var ui by mutableStateOf(estado())
        rule.setContent {
            MaterialTheme {
                RoutineEditContent(
                    uiState = ui,
                    onSetFieldChange = { _, _, field, raw -> ui = aplicar(ui, field, raw) }
                )
            }
        }

        rule.onNodeWithTag("routineKg:s1").performClick()
        rule.onNodeWithTag("keypadBorrar").performClick()
        rule.onNodeWithTag("routineKg:s1").assertTextEquals("6")

        // Tap en reps sin pulsar OK: guarda kg y deja el foco en reps.
        rule.onNodeWithTag("routineReps:s1").performClick()
        assertEquals(6.0, ui.exercises.first().sets.first().weightKg, 0.0)
        rule.onNodeWithTag("routineReps:s1").assertTextEquals("10")

        // Las teclas ya escriben en reps, no en kg.
        rule.onNodeWithTag("keypad5").performClick()
        rule.onNodeWithTag("routineReps:s1").assertTextEquals("105")
        assertEquals(10, ui.exercises.first().sets.first().targetReps)

        rule.onNodeWithTag("keypadOk").performClick()
        assertEquals(105, ui.exercises.first().sets.first().targetReps)
        assertEquals(6.0, ui.exercises.first().sets.first().weightKg, 0.0)
    }

    @Test
    fun al_cambiar_de_celda_a_duracion_autoguarda_las_dos() {
        var ui by mutableStateOf(estado(durationManual = true, durationMin = 30))
        rule.setContent {
            MaterialTheme {
                RoutineEditContent(
                    uiState = ui,
                    onSetFieldChange = { _, _, field, raw -> ui = aplicar(ui, field, raw) },
                    onDurationChange = { ui = ui.copy(durationMin = it) }
                )
            }
        }

        rule.onNodeWithTag("routineKg:s1").performClick()
        rule.onNodeWithTag("keypadBorrar").performClick()
        rule.onNodeWithTag("routineDurationValue").performClick()

        assertEquals(6.0, ui.exercises.first().sets.first().weightKg, 0.0)
        rule.onNodeWithTag("routineDurationValue").assertTextEquals("30")

        rule.onNodeWithTag("keypadBorrar").performClick()
        rule.onNodeWithTag("routineDurationValue").assertTextEquals("3")
        rule.onNodeWithTag("keypadOk").performClick()
        assertEquals(3, ui.durationMin)
        assertEquals(6.0, ui.exercises.first().sets.first().weightKg, 0.0)
    }

    @Test
    fun kg_se_actualiza_en_vivo_antes_de_ok() {
        var ui by mutableStateOf(estado())
        rule.setContent {
            MaterialTheme {
                RoutineEditContent(
                    uiState = ui,
                    onSetFieldChange = { _, _, _, raw ->
                        ui = conPeso(ui, raw.toDoubleOrNull() ?: 0.0)
                    }
                )
            }
        }

        rule.onNodeWithTag("routineKg:s1").assertTextEquals("60")

        rule.onNodeWithTag("routineKg:s1").performClick()
        rule.onNodeWithTag("keypadBorrar").performClick()
        // Sin pulsar OK la celda ya muestra el borrador del panel.
        rule.onNodeWithTag("routineKg:s1").assertTextEquals("6")

        rule.onNodeWithTag("keypad1").performClick()
        rule.onNodeWithTag("routineKg:s1").assertTextEquals("61")

        // OK confirma y la celda conserva el valor guardado.
        rule.onNodeWithTag("keypadOk").performClick()
        rule.onNodeWithTag("routineKg:s1").assertTextEquals("61")
    }

    @Test
    fun duracion_manual_se_actualiza_en_vivo_antes_de_ok() {
        var ui by mutableStateOf(estado(durationManual = true, durationMin = 30))
        rule.setContent {
            MaterialTheme {
                RoutineEditContent(
                    uiState = ui,
                    onDurationChange = { ui = ui.copy(durationMin = it) }
                )
            }
        }

        rule.onNodeWithTag("routineDurationValue").assertTextEquals("30")

        rule.onNodeWithTag("routineDurationValue").performClick()
        rule.onNodeWithTag("keypadBorrar").performClick()
        rule.onNodeWithTag("routineDurationValue").assertTextEquals("3")

        rule.onNodeWithTag("keypadOk").performClick()
        rule.onNodeWithTag("routineDurationValue").assertTextEquals("3")
    }

    @Test
    fun el_numero_queda_centrado_en_la_celda() {
        rule.setContent {
            MaterialTheme {
                RoutineEditContent(uiState = estado())
            }
        }

        val celda = rule.onNodeWithTag("routineReps:s1").getUnclippedBoundsInRoot()
        val texto = rule.onNode(
            hasText("10") and hasAnyAncestor(hasTestTag("routineReps:s1")),
            useUnmergedTree = true
        ).getUnclippedBoundsInRoot()

        val centroCelda = (celda.left.value + celda.right.value) / 2f
        val centroTexto = (texto.left.value + texto.right.value) / 2f
        val desvio = abs(centroCelda - centroTexto)
        assertTrue("el número debe ir centrado (desvío ${desvio}dp)", desvio <= 8f)
    }
}
