package com.mytrainingplan.app.feature.exercises

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.mytrainingplan.app.ui.theme.AppColors
import com.mytrainingplan.app.ui.theme.AppDimens
import com.mytrainingplan.app.ui.theme.AppTextSizes
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mytrainingplan.app.domain.model.Exercise

// Colores y medidas desde ui/theme (spec 011): sin tokens locales.

/** Etiquetas ES (mismo mapa que el buscador, spec 003 §4). */
private val MUSCLE_LABELS: Map<String, String> = mapOf(
    "quads" to "Pierna",
    "hamstrings" to "Pierna",
    "glutes" to "Pierna",
    "calves" to "Pierna",
    "abductors" to "Pierna",
    "adductors" to "Pierna",
    "pectorals" to "Pecho",
    "lats" to "Espalda",
    "traps" to "Espalda",
    "upper-back" to "Espalda",
    "spine" to "Espalda",
    "delts" to "Hombros",
    "abs" to "Core",
    "biceps" to "Brazo",
    "triceps" to "Brazo",
    "forearms" to "Brazo"
)

private val EQUIPMENT_LABELS: Map<String, String> = mapOf(
    "barbell" to "Barra",
    "dumbbell" to "Mancuernas",
    "machine" to "Máquina",
    "lever" to "Máquina",
    "bodyweight" to "Corporal",
    "cable" to "Polea",
    "band" to "Banda",
    "kettlebell" to "Kettlebell",
    "smith" to "Smith",
    "ez-bar" to "Barra Z",
    "other" to "Otro"
)

private fun muscleLabel(slug: String): String = MUSCLE_LABELS[slug] ?: slug
private fun equipmentLabel(value: String): String = EQUIPMENT_LABELS[value] ?: value

/**
 * Ficha de ejercicio (spec 010): GIF grande (Coil, caché sin red) + nombre,
 * músculo, equipo, secundarios e instrucciones numeradas en español.
 * Solo ve `domain/model` (nunca Entity ni DTO). Sin ejercicio (id nulo o
 * inexistente) muestra aviso corto sin caerse; volver cierra sin duplicar.
 */
@Composable
fun ExerciseDetailScreen(
    exerciseId: String? = null,
    viewModel: ExerciseDetailViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val exercise by viewModel.exercise.collectAsState()
    LaunchedEffect(exerciseId) { viewModel.openExercise(exerciseId) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Bg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DetailHeader(onBack = onBack)
            if (exercise == null) {
                MissingExercise(modifier = Modifier.weight(1f))
            } else {
                DetailBody(
                    exercise = exercise!!,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        DetailFooter(
            onBack = onBack,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun DetailHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Bg)
            .statusBarsPadding()
            .padding(horizontal = AppDimens.ScreenHorizontal, vertical = AppDimens.SpaceXl),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(AppDimens.TouchMin)
                .clip(RoundedCornerShape(AppDimens.RadiusPill))
                .background(AppColors.Card)
                .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusPill))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = AppColors.TextPrimary)
        }
        Spacer(Modifier.width(AppDimens.SpaceLg))
        Text(
            "Detalle de ejercicio",
            color = AppColors.TextPrimary,
            fontSize = AppTextSizes.Headline,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DetailBody(
    exercise: Exercise,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppDimens.ScreenHorizontal)
            // Hueco para que el pie fijo no tape las instrucciones.
            .padding(top = AppDimens.ContentTop, bottom = AppDimens.DetailBottom),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceXxl)
    ) {
        // GIF grande animado (spec 012, paso 7): usa el loader global de
        // TrainingApp con decodificador GIF; sin red tira de caché.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.DetailHeroHeight)
                .clip(RoundedCornerShape(AppDimens.RadiusCard))
                .background(AppColors.CardHigh),
            contentAlignment = Alignment.BottomEnd
        ) {
            AsyncImage(
                model = exercise.gifUrl,
                contentDescription = exercise.name,
                placeholder = ColorPainter(AppColors.CardHigh),
                error = ColorPainter(AppColors.CardHigh),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .padding(AppDimens.SpaceMd)
                    .clip(RoundedCornerShape(AppDimens.RadiusSm))
                    .background(AppColors.BadgeBg)
                    .padding(horizontal = AppDimens.SpaceMd, vertical = AppDimens.SpaceTiny)
            ) {
                Text("GIF", color = AppColors.Orange, fontSize = AppTextSizes.Caption, fontWeight = FontWeight.Bold)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceXs)) {
            Text(
                exercise.name,
                color = AppColors.TextPrimary,
                fontSize = AppTextSizes.Display,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${muscleLabel(exercise.muscle)} · ${equipmentLabel(exercise.equipment)}",
                color = AppColors.TextMuted,
                fontSize = AppTextSizes.BodyLg
            )
        }
        if (exercise.secondaryMuscles.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMd)) {
                Text(
                    "SECUNDARIOS",
                    color = AppColors.TextMuted,
                    fontSize = AppTextSizes.Caption,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSm)
                ) {
                    exercise.secondaryMuscles.forEach { secondary ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(AppDimens.RadiusMd))
                                .background(AppColors.Card)
                                .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusMd))
                                .padding(horizontal = AppDimens.SpaceLg, vertical = AppDimens.SpaceSm)
                        ) {
                            Text(
                                muscleLabel(secondary),
                                color = AppColors.TextPrimary,
                                fontSize = AppTextSizes.Small,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMd)) {
            Text(
                "INSTRUCCIONES",
                color = AppColors.Orange,
                fontSize = AppTextSizes.Caption,
                fontWeight = FontWeight.Bold
            )
            if (exercise.instructions.isEmpty()) {
                Text(
                    "Sin instrucciones guardadas para este ejercicio.",
                    color = AppColors.TextMuted,
                    fontSize = AppTextSizes.BodyLg
                )
            } else {
                exercise.instructions.forEachIndexed { index, step ->
                    InstructionRow(number = index + 1, text = step)
                }
            }
        }
    }
}

@Composable
private fun InstructionRow(number: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.RadiusXl))
            .background(AppColors.Card)
            .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusXl))
            .padding(AppDimens.SpaceXl),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(AppDimens.SetBadge)
                .clip(RoundedCornerShape(AppDimens.RadiusPill))
                .background(AppColors.Orange),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$number",
                color = AppColors.OnOrange,
                fontSize = AppTextSizes.BodyLg,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(AppDimens.SpaceLg))
        Text(
            text,
            color = AppColors.TextPrimary,
            fontSize = AppTextSizes.BodyLg,
            modifier = Modifier
                .weight(1f)
                .padding(top = AppDimens.SpaceXs)
        )
    }
}

@Composable
private fun MissingExercise(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.ScreenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Ejercicio no encontrado",
            color = AppColors.TextPrimary,
            fontSize = AppTextSizes.DisplaySm,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(AppDimens.SpaceMd))
        Text(
            "El ejercicio no existe en este móvil.",
            color = AppColors.TextMuted,
            fontSize = AppTextSizes.TitleSm,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DetailFooter(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = AppDimens.ScreenHorizontal)
            .padding(bottom = AppDimens.SpaceHuge)
            .clip(RoundedCornerShape(AppDimens.RadiusPill))
            .background(AppColors.Orange)
            .clickable(onClick = onBack)
            .padding(vertical = AppDimens.SpaceXxl),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Volver",
            color = AppColors.OnOrange,
            fontSize = AppTextSizes.TitleSm,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111316)
@Composable
private fun ExerciseDetailPreview() {
    androidx.compose.material3.MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Bg)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                DetailHeader(onBack = {})
                DetailBody(
                    exercise = Exercise(
                        id = "biceps/barbell-curl",
                        slug = "barbell-curl",
                        name = "Curl con barra",
                        muscle = "biceps",
                        bodyPart = "arms",
                        equipment = "barbell",
                        category = "strength",
                        secondaryMuscles = listOf("forearms"),
                        instructions = listOf(
                            "De pie, agarra la barra con las manos a la anchura de los hombros.",
                            "Flexiona los codos y sube la barra hasta los hombros sin balancear el cuerpo.",
                            "Baja despacio hasta estirar los brazos del todo."
                        ),
                        gifUrl = ""
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
            DetailFooter(
                onBack = {},
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
