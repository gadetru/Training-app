package com.mytrainingplan.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mytrainingplan.app.domain.model.AccentColor
import com.mytrainingplan.app.domain.model.FootKind
import com.mytrainingplan.app.domain.model.HomeUiState
import com.mytrainingplan.app.domain.model.RoutineSummary
import com.mytrainingplan.app.domain.model.WeeklyProgress
import com.mytrainingplan.app.feature.home.HomeContent
import com.mytrainingplan.app.feature.home.HomeTab
import org.junit.Rule
import org.junit.Test

/**
 * Smoke Home en emulador Pixel 6 API 34.
 * Sin Hilt: ataca HomeContent stateless con estado fake.
 */
class HomeNavTest {

    @get:Rule
    val rule = createComposeRule()

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
    fun home_muestra_saludo_rutinas_y_dock() {
        rule.setContent {
            MaterialTheme {
                HomeContent(uiState = fakeState())
            }
        }

        rule.onNodeWithTag("homeRoot").assertIsDisplayed()
        rule.onNodeWithText("Hola, Carlos").assertIsDisplayed()
        rule.onNodeWithText("Tus Rutinas").assertIsDisplayed()
        rule.onNodeWithText("2 activas").assertIsDisplayed()
        rule.onNodeWithText("Torso - Fuerza").assertIsDisplayed()
        rule.onNodeWithText("Pierna & Core").assertIsDisplayed()
        rule.onNodeWithTag("routineCard:r1").assertIsDisplayed()
        rule.onNodeWithTag("homeDock").assertIsDisplayed()
        // "Calendario"/"Progreso" salen dos veces (widget + tab del dock).
        rule.onAllNodesWithText("Calendario").assertCountEquals(2)
        rule.onAllNodesWithText("Progreso").assertCountEquals(2)
        rule.onNodeWithText("Rutinas").assertIsDisplayed()
        rule.onNodeWithText("Perfil").assertIsDisplayed()
        // Un CTA "Iniciar" por card.
        rule.onAllNodesWithText("Iniciar").assertCountEquals(2)
    }

    @Test
    fun home_onStart_se_llama_con_id_de_card() {
        var started: String? = null
        rule.setContent {
            MaterialTheme {
                HomeContent(
                    uiState = fakeState(),
                    onStart = { started = it }
                )
            }
        }

        // Dos CTAs "Iniciar" (uno por card); el primero es el de r1.
        rule.onAllNodesWithText("Iniciar")[0].performClick()
        assert(started == "r1")
    }

    @Test
    fun home_cambio_de_tab_llama_callback() {
        var selected: HomeTab? = null
        rule.setContent {
            MaterialTheme {
                HomeContent(
                    uiState = fakeState(),
                    selectedTab = HomeTab.RUTINAS,
                    onTabSelected = { selected = it }
                )
            }
        }

        rule.onNodeWithText("Perfil").performClick()
        assert(selected == HomeTab.PERFIL)
    }
}
