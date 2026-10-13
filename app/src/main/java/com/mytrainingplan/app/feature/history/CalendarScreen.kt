package com.mytrainingplan.app.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.mytrainingplan.app.domain.model.MonthDay
import com.mytrainingplan.app.domain.model.Routine
import com.mytrainingplan.app.ui.theme.AppColors
import com.mytrainingplan.app.ui.theme.AppDimens
import com.mytrainingplan.app.ui.theme.AppTextSizes
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

// Colores y medidas desde ui/theme (spec 011): sin tokens locales.

private val WEEKDAYS = listOf("L", "M", "X", "J", "V", "S", "D")

/** "Octubre de 2026" (lunes primer día, coherente con el heatmap L–D). */
private fun monthLabel(year: Int, month: Int): String {
    if (year <= 0 || month !in 1..12) return ""
    val name = Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, Locale("es"))
    return name.replaceFirstChar { it.uppercase() } + " de $year"
}

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel(),
    onCreate: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    CalendarContent(
        uiState = uiState,
        onDayClick = viewModel::onDayClick,
        onPrevMonth = viewModel::onPrevMonth,
        onNextMonth = viewModel::onNextMonth,
        onSelectRoutine = viewModel::onSelectRoutine,
        onClearScheduled = viewModel::onClearScheduled,
        onDismissSheet = viewModel::onDismissSheet,
        onCreate = onCreate
    )
}

@Composable
fun CalendarContent(
    uiState: CalendarUiState,
    onDayClick: (date: String) -> Unit = {},
    onPrevMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onSelectRoutine: (date: String, routineId: String) -> Unit = { _, _ -> },
    onClearScheduled: (date: String) -> Unit = {},
    onDismissSheet: () -> Unit = {},
    onCreate: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Bg)
            .testTag("calendarRoot")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CalendarHeader(
                year = uiState.year,
                month = uiState.month,
                onPrevMonth = onPrevMonth,
                onNextMonth = onNextMonth
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppDimens.ScreenHorizontal)
                    // Hueco para que el contenido no quede bajo el dock flotante.
                    .padding(top = AppDimens.ContentTop, bottom = AppDimens.HomeBottom),
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSm)
            ) {
                WeekdayHeader()
                MonthGrid(days = uiState.days, onDayClick = onDayClick)
            }
        }
    }
    val selected = uiState.selectedDay
    if (uiState.isSheetOpen && selected != null) {
        ScheduleSheet(
            day = selected,
            month = uiState.month,
            routines = uiState.routines,
            onSelectRoutine = onSelectRoutine,
            onClearScheduled = onClearScheduled,
            onDismiss = onDismissSheet,
            onCreate = onCreate
        )
    }
}

@Composable
private fun CalendarHeader(
    year: Int,
    month: Int,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Bg)
            .statusBarsPadding()
            .padding(horizontal = AppDimens.ScreenHorizontal, vertical = AppDimens.SpaceXl)
    ) {
        Text(
            "Calendario",
            color = AppColors.TextPrimary,
            fontSize = AppTextSizes.DisplayLg,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.padding(top = AppDimens.SpaceMd))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "‹",
                color = AppColors.Orange,
                fontSize = AppTextSizes.DisplayLg,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(AppDimens.RadiusMd))
                    .clickable(role = Role.Button, onClickLabel = "Mes anterior", onClick = onPrevMonth)
                    .padding(horizontal = AppDimens.SpaceXxl, vertical = AppDimens.SpaceSm)
                    .testTag("calendarPrev")
            )
            Text(
                monthLabel(year, month),
                color = AppColors.TextPrimary,
                fontSize = AppTextSizes.TitleLg,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "›",
                color = AppColors.Orange,
                fontSize = AppTextSizes.DisplayLg,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(AppDimens.RadiusMd))
                    .clickable(role = Role.Button, onClickLabel = "Mes siguiente", onClick = onNextMonth)
                    .padding(horizontal = AppDimens.SpaceXxl, vertical = AppDimens.SpaceSm)
                    .testTag("calendarNext")
            )
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        WEEKDAYS.forEach { d ->
            Text(
                d,
                color = AppColors.TextMuted,
                fontSize = AppTextSizes.Caption,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MonthGrid(
    days: List<MonthDay>,
    onDayClick: (date: String) -> Unit
) {
    if (days.isEmpty()) return
    val first = LocalDate.parse(days.first().date)
    // Lunes primero: huecos antes del día 1.
    val offset = (first.dayOfWeek.value - 1) % 7
    val cells: List<MonthDay?> = List(offset) { null } + days
    cells.chunked(7).forEach { week ->
        Row(modifier = Modifier.fillMaxWidth()) {
            for (day in week) {
                if (day == null) {
                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    DayCell(
                        day = day,
                        onDayClick = onDayClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            // Fila incompleta (última semana): relleno para alinear.
            for (i in week.size until 7) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DayCell(
    day: MonthDay,
    onDayClick: (date: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val name = day.displayName
    val hasContent = name != null
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .heightIn(min = AppDimens.TouchMin)
            .clip(RoundedCornerShape(AppDimens.RadiusSm))
            .background(if (hasContent) AppColors.Card else AppColors.Bg)
            .border(
                AppDimens.BorderThin,
                if (day.isToday) AppColors.Orange else AppColors.BorderSubtle,
                RoundedCornerShape(AppDimens.RadiusSm)
            )
            .clickable(
                role = Role.Button,
                onClickLabel = "Día ${day.dayOfMonth}",
                onClick = { onDayClick(day.date) }
            )
            .padding(horizontal = AppDimens.SpaceXs, vertical = AppDimens.SpaceSm)
            .testTag("dayCell:${day.date}")
    ) {
        Text(
            "${day.dayOfMonth}",
            color = if (day.isToday) AppColors.Orange else AppColors.TextPrimary,
            fontSize = AppTextSizes.Body,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium
        )
        if (day.isDone) {
            Text(
                "✓ $name",
                color = AppColors.Volt,
                fontSize = AppTextSizes.Tiny,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        } else if (name != null) {
            Text(
                name,
                color = AppColors.TextMuted,
                fontSize = AppTextSizes.Tiny,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleSheet(
    day: MonthDay,
    month: Int,
    routines: List<Routine>,
    onSelectRoutine: (date: String, routineId: String) -> Unit,
    onClearScheduled: (date: String) -> Unit,
    onDismiss: () -> Unit,
    onCreate: () -> Unit
) {
    // Los diálogos/sheets de Material3 gestionan insets solos.
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColors.SheetBg,
        modifier = Modifier.testTag("scheduleSheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.ScreenHorizontal)
                .padding(bottom = AppDimens.SpaceHuge),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMd)
        ) {
            val monthName = if (month in 1..12) {
                Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, Locale("es"))
            } else {
                ""
            }
            Text(
                "Día ${day.dayOfMonth} de $monthName",
                color = AppColors.TextPrimary,
                fontSize = AppTextSizes.TitleLg,
                fontWeight = FontWeight.Bold
            )
            when {
                day.isDone -> Text(
                    "✓ Hecha: ${day.displayName}",
                    color = AppColors.Volt,
                    fontSize = AppTextSizes.BodyLg,
                    fontWeight = FontWeight.SemiBold
                )
                day.displayName != null -> Text(
                    "Apuntada: ${day.displayName}",
                    color = AppColors.Orange,
                    fontSize = AppTextSizes.BodyLg,
                    fontWeight = FontWeight.SemiBold
                )
                else -> Text(
                    "Sin rutina apuntada",
                    color = AppColors.TextMuted,
                    fontSize = AppTextSizes.BodyLg
                )
            }
            if (routines.isEmpty()) {
                Text(
                    "Aún no tienes rutinas. Crea una para poder apuntarla.",
                    color = AppColors.TextMuted,
                    fontSize = AppTextSizes.Body
                )
                Button(
                    onClick = onCreate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Orange,
                        contentColor = AppColors.OnOrange
                    ),
                    modifier = Modifier.testTag("scheduleCreate")
                ) {
                    Text("Crear rutina", fontWeight = FontWeight.Bold)
                }
            } else {
                routines.forEach { routine ->
                    val current = routine.id == day.scheduledRoutineId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(AppDimens.RadiusMd))
                            .background(if (current) AppColors.CardHigh else AppColors.Card)
                            .clickable(
                                role = Role.Button,
                                onClickLabel = "Apuntar ${routine.name.ifBlank { "Rutina" }}",
                                onClick = { onSelectRoutine(day.date, routine.id) }
                            )
                            .padding(
                                horizontal = AppDimens.SpaceXl,
                                vertical = AppDimens.SpaceLg
                            )
                            .testTag("scheduleRoutine:${routine.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (current) "✓ " else "",
                            color = AppColors.Volt,
                            fontSize = AppTextSizes.Body,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            routine.name.ifBlank { "Rutina" },
                            color = AppColors.TextPrimary,
                            fontSize = AppTextSizes.BodyLg,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (day.scheduledRoutineId != null) {
                    TextButton(
                        onClick = { onClearScheduled(day.date) },
                        modifier = Modifier.testTag("scheduleClear")
                    ) {
                        Text("Quitar apunte", color = AppColors.Orange)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111316)
@Composable
private fun CalendarContentPreview() {
    androidx.compose.material3.MaterialTheme {
        CalendarContent(
            uiState = CalendarUiState(
                year = 2026,
                month = 10,
                days = (1..31).map { d ->
                    MonthDay(
                        date = "2026-10-%02d".format(d),
                        dayOfMonth = d,
                        isToday = d == 13,
                        scheduledRoutineId = if (d == 15) "t1" else null,
                        scheduledRoutineName = if (d == 15) "Torso" else null,
                        doneRoutineId = if (d == 13) "t1" else null,
                        doneRoutineName = if (d == 13) "Torso" else null
                    )
                },
                routines = listOf(
                    Routine(id = "t1", name = "Torso", durationMin = 45),
                    Routine(id = "p1", name = "Pierna", durationMin = 55)
                )
            )
        )
    }
}
