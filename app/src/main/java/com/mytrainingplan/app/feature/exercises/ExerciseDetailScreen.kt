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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mytrainingplan.app.domain.model.Exercise

// Tokens patrón ExercisePickerSheet (003) + RoutineEditScreen (004).
// Tipografías de sistema (Outfit/Jakarta van en Futuros).
private val Bg = Color(0xFF111316)
private val Card = Color(0xFF1A1C1F)
private val CardHigh = Color(0xFF282A2D)
private val Orange = Color(0xFFFF5E00)
private val OnOrange = Color(0xFF1A0A00)
private val TextPrimary = Color(0xFFF5F7FA)
private val TextMuted = Color(0xFF8B95A5)
private val BorderSubtle = Color(0xFF282E37)

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
            .background(Bg)
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
            .background(Bg)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Card)
                .border(1.dp, BorderSubtle, RoundedCornerShape(999.dp))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "Detalle de ejercicio",
            color = TextPrimary,
            fontSize = 18.sp,
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
            .padding(horizontal = 20.dp)
            // Hueco para que el pie fijo no tape las instrucciones.
            .padding(top = 4.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // GIF grande vía Coil (sin coil-gif: primer frame; sin red usa caché).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CardHigh),
            contentAlignment = Alignment.BottomEnd
        ) {
            AsyncImage(
                model = exercise.gifUrl,
                contentDescription = exercise.name,
                placeholder = ColorPainter(CardHigh),
                error = ColorPainter(CardHigh),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xCC0C0E11))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("GIF", color = Orange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                exercise.name,
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${muscleLabel(exercise.muscle)} · ${equipmentLabel(exercise.equipment)}",
                color = TextMuted,
                fontSize = 13.sp
            )
        }
        if (exercise.secondaryMuscles.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "SECUNDARIOS",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    exercise.secondaryMuscles.forEach { secondary ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Card)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                muscleLabel(secondary),
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "INSTRUCCIONES",
                color = Orange,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            if (exercise.instructions.isEmpty()) {
                Text(
                    "Sin instrucciones guardadas para este ejercicio.",
                    color = TextMuted,
                    fontSize = 13.sp
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
            .clip(RoundedCornerShape(12.dp))
            .background(Card)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Orange),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$number",
                color = OnOrange,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            color = TextPrimary,
            fontSize = 13.sp,
            modifier = Modifier
                .weight(1f)
                .padding(top = 4.dp)
        )
    }
}

@Composable
private fun MissingExercise(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Ejercicio no encontrado",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "El ejercicio no existe en este móvil.",
            color = TextMuted,
            fontSize = 14.sp,
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
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Orange)
            .clickable(onClick = onBack)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Volver",
            color = OnOrange,
            fontSize = 14.sp,
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
                .background(Bg)
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
