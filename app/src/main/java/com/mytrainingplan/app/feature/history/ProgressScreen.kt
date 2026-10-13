package com.mytrainingplan.app.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.mytrainingplan.app.domain.model.WeeklyProgress
import com.mytrainingplan.app.ui.theme.AppColors
import com.mytrainingplan.app.ui.theme.AppDimens
import com.mytrainingplan.app.ui.theme.AppTextSizes

// Colores y medidas desde ui/theme (spec 011): sin tokens locales.

private val PROGRESS_WEEKDAYS = listOf("L", "M", "X", "J", "V", "S", "D")

@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val progress by viewModel.uiState.collectAsState()
    ProgressContent(progress = progress)
}

@Composable
fun ProgressContent(
    progress: WeeklyProgress
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Bg)
            .testTag("progressRoot")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                "Progreso",
                color = AppColors.TextPrimary,
                fontSize = AppTextSizes.DisplayLg,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.Bg)
                    .statusBarsPadding()
                    .padding(horizontal = AppDimens.ScreenHorizontal, vertical = AppDimens.SpaceXl)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppDimens.ScreenHorizontal)
                    // Hueco para que el contenido no quede bajo el dock flotante.
                    .padding(top = AppDimens.ContentTop, bottom = AppDimens.HomeBottom),
                verticalArrangement = Arrangement.spacedBy(AppDimens.ScreenHorizontal)
            ) {
                StreakCard(progress = progress)
                SessionsCard(progress = progress)
                WeekCard(progress = progress)
            }
        }
    }
}

@Composable
private fun StreakCard(progress: WeeklyProgress) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.RadiusCard))
            .background(AppColors.Card)
            .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusCard))
            .padding(AppDimens.SpaceXxl)
    ) {
        Text(
            "🔥 Racha ${progress.streakDays} días",
            color = AppColors.Orange,
            fontSize = AppTextSizes.TitleLg,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.padding(top = AppDimens.SpaceSm))
        Text(
            "Días seguidos entrenando",
            color = AppColors.TextMuted,
            fontSize = AppTextSizes.Small
        )
    }
}

@Composable
private fun SessionsCard(progress: WeeklyProgress) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.RadiusCard))
            .background(AppColors.Card)
            .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusCard))
            .padding(AppDimens.SpaceXxl)
    ) {
        Text("Sesiones", color = AppColors.TextMuted, fontSize = AppTextSizes.Small)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "${progress.sessionsDone}",
                color = AppColors.TextPrimary,
                fontSize = AppTextSizes.DisplayLg,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.padding(start = AppDimens.SpaceXs))
            Text(
                "/ ${progress.sessionsGoal} objetivo",
                color = AppColors.TextMuted,
                fontSize = AppTextSizes.Small
            )
        }
        Spacer(modifier = Modifier.padding(top = AppDimens.SpaceMd))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.ProgressBarThin)
                .clip(RoundedCornerShape(AppDimens.RadiusPill))
                .background(AppColors.Card2)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.progressFraction.coerceIn(0f, 1f))
                    .height(AppDimens.ProgressBarThin)
                    .clip(RoundedCornerShape(AppDimens.RadiusPill))
                    .background(AppColors.Volt)
            )
        }
        Spacer(modifier = Modifier.padding(top = AppDimens.SpaceLg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Tiempo:", color = AppColors.TextMuted, fontSize = AppTextSizes.Small)
            Text(
                "${progress.minutes} min",
                color = AppColors.TextPrimary,
                fontSize = AppTextSizes.Small,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun WeekCard(progress: WeeklyProgress) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.RadiusCard))
            .background(AppColors.Card)
            .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusCard))
            .padding(AppDimens.SpaceXxl)
    ) {
        Text(
            "Esta semana",
            color = AppColors.TextPrimary,
            fontSize = AppTextSizes.BodyLg,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.padding(top = AppDimens.SpaceLg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PROGRESS_WEEKDAYS.forEachIndexed { i, d ->
                val done = progress.weekChecks.getOrElse(i) { false }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        d,
                        color = AppColors.TextMuted,
                        fontSize = AppTextSizes.Tiny,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.padding(top = AppDimens.SpaceXs))
                    Box(
                        modifier = Modifier
                            .size(AppDimens.DayDot)
                            .clip(CircleShape)
                            .background(if (done) AppColors.Volt else AppColors.Card2)
                            .border(
                                AppDimens.BorderThin,
                                if (done) Color.Transparent else AppColors.BorderSubtle,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (done) {
                            Text(
                                "✓",
                                color = AppColors.OnVolt,
                                fontSize = AppTextSizes.Caption,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111316)
@Composable
private fun ProgressContentPreview() {
    androidx.compose.material3.MaterialTheme {
        ProgressContent(
            progress = WeeklyProgress(
                streakDays = 3,
                sessionsDone = 2,
                sessionsGoal = 4,
                minutes = 95,
                weekChecks = listOf(true, false, true, false, false, false, false)
            )
        )
    }
}
