package com.mytrainingplan.app.feature.home

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.github.takahirom.roborazzi.captureRoboImage
import com.mytrainingplan.app.domain.model.AccentColor
import com.mytrainingplan.app.domain.model.FootKind
import com.mytrainingplan.app.domain.model.HomeUiState
import com.mytrainingplan.app.domain.model.RoutineSummary
import com.mytrainingplan.app.domain.model.WeeklyProgress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Fotos de referencia (spec 011, paso 14): solo Home y tarjeta de rutina.
 * Roborazzi en JVM + Robolectric (sin emulador): cambiar Home o su tarjeta
 * hace fallar su foto. Dorados con `recordRoborazziDebug`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Viewport de Pixel 6 (411x914dp): sin esto Robolectric renderiza enano y
// el texto se parte en vertical.
@Config(sdk = [34], qualifiers = "w411dp-h914dp")
class HomeScreenshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun fakeState() = HomeUiState(
        firstName = "Carlos",
        routines = listOf(
            RoutineSummary(
                id = "r1",
                title = "Torso - Fuerza",
                tags = listOf("Pecho"),
                durationMin = 45,
                exerciseCount = 6,
                lastDoneLabel = "Hace 2 días",
                accent = AccentColor.ORANGE,
                footNote = "Récord en Press Banca",
                footKind = FootKind.PR
            ),
            RoutineSummary(
                id = "r2",
                title = "Pierna & Core",
                tags = listOf("Pierna"),
                durationMin = 55,
                exerciseCount = 7,
                accent = AccentColor.VOLT,
                footNote = "Enfoque: Sentadilla",
                footKind = FootKind.INFO
            )
        ),
        progress = WeeklyProgress()
    )

    @Test
    fun home_igual_que_la_foto() {
        composeRule.setContent {
            MaterialTheme {
                HomeContent(uiState = fakeState())
            }
        }

        composeRule.onNodeWithTag("homeRoot").captureRoboImage()
    }

    @Test
    fun tarjeta_igual_que_la_foto() {
        composeRule.setContent {
            MaterialTheme {
                HomeContent(uiState = fakeState())
            }
        }

        composeRule.onNodeWithTag("routineCard:r1").captureRoboImage()
    }
}
