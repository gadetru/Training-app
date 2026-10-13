package com.mytrainingplan.app.feature.history

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mytrainingplan.app.domain.model.MonthDay
import com.mytrainingplan.app.domain.model.Routine
import org.junit.Rule
import org.junit.Test

/**
 * Smoke del mes en emulador Pixel 6 API 34 (Paso 10 spec 015).
 * Sin Hilt: ataca CalendarContent stateless con estado fake.
 * Apuntar/cambiar/quitar/hecho a nivel repo van en `ScheduleFakeTest` (JVM);
 * aquí se verifica la rejilla, la hoja y sus callbacks.
 */
class CalendarSmokeTest {

    @get:Rule
    val rule = createComposeRule()

    private fun day(
        d: Int,
        scheduledId: String? = null,
        scheduledName: String? = null,
        doneId: String? = null,
        doneName: String? = null
    ) = MonthDay(
        date = "2026-10-%02d".format(d),
        dayOfMonth = d,
        isCurrentMonth = true,
        isToday = d == 13,
        scheduledRoutineId = scheduledId,
        scheduledRoutineName = scheduledName,
        doneRoutineId = doneId,
        doneRoutineName = doneName
    )

    private fun october(
        selectedDate: String? = null,
        routines: List<Routine> = listOf(
            Routine(id = "t1", name = "Torso", durationMin = 45),
            Routine(id = "p1", name = "Pierna", durationMin = 55)
        )
    ) = CalendarUiState(
        year = 2026,
        month = 10,
        days = (1..31).map { d ->
            when (d) {
                13 -> day(d, doneId = "t1", doneName = "Torso")
                15 -> day(d, scheduledId = "t1", scheduledName = "Torso")
                else -> day(d)
            }
        },
        routines = routines,
        selectedDate = selectedDate
    )

    @Test
    fun mes_muestra_apunte_y_hecho() {
        rule.setContent {
            MaterialTheme {
                CalendarContent(uiState = october())
            }
        }

        rule.onNodeWithTag("calendarRoot").assertIsDisplayed()
        rule.onNodeWithTag("dayCell:2026-10-15").assertIsDisplayed()
        rule.onNodeWithText("Torso").assertIsDisplayed()
        rule.onNodeWithText("✓ Torso").assertIsDisplayed()
    }

    @Test
    fun tap_dia_avisa_con_su_fecha() {
        var clicked: String? = null
        rule.setContent {
            MaterialTheme {
                CalendarContent(
                    uiState = october(),
                    onDayClick = { clicked = it }
                )
            }
        }

        rule.onNodeWithTag("dayCell:2026-10-15").performClick()
        assert(clicked == "2026-10-15")
    }

    @Test
    fun hoja_seleccionar_rutina_llama_con_fecha_e_id() {
        var selected: Pair<String, String>? = null
        rule.setContent {
            MaterialTheme {
                CalendarContent(
                    uiState = october(selectedDate = "2026-10-15"),
                    onSelectRoutine = { date, id -> selected = date to id }
                )
            }
        }

        rule.onNodeWithTag("scheduleSheet").assertIsDisplayed()
        rule.onNodeWithTag("scheduleRoutine:p1").performClick()
        assert(selected == ("2026-10-15" to "p1"))
    }

    @Test
    fun hoja_quitar_llama_con_la_fecha() {
        var cleared: String? = null
        rule.setContent {
            MaterialTheme {
                CalendarContent(
                    uiState = october(selectedDate = "2026-10-15"),
                    onClearScheduled = { cleared = it }
                )
            }
        }

        rule.onNodeWithTag("scheduleClear").performClick()
        assert(cleared == "2026-10-15")
    }

    @Test
    fun hoja_sin_rutinas_lleva_al_constructor() {
        var created = false
        rule.setContent {
            MaterialTheme {
                CalendarContent(
                    uiState = october(selectedDate = "2026-10-20", routines = emptyList()),
                    onCreate = { created = true }
                )
            }
        }

        rule.onNodeWithTag("scheduleCreate").performClick()
        assert(created)
    }
}
