package com.mytrainingplan.app.feature.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mytrainingplan.app.data.repository.FakeExerciseRepository
import com.mytrainingplan.app.domain.model.Exercise
import com.mytrainingplan.app.domain.model.ExercisesUiState

// Tokens references/plantilla-lista-ejercicios/DESIGN.md + patrón HomeScreen (002).
// Tipografías de sistema en Fase A (Outfit/Plus Jakarta/Space Grotesk van en Futuros).
private val SheetBg = Color(0xFF1A1C1F)
private val Card = Color(0xFF1E2023)
private val CardHigh = Color(0xFF282A2D)
private val Orange = Color(0xFFFF5E00)
private val Volt = Color(0xFFCCFF00)
private val OnOrange = Color(0xFF1A0A00)
private val OnVolt = Color(0xFF1A2A00)
private val TextPrimary = Color(0xFFF5F7FA)
private val TextMuted = Color(0xFF8B95A5)
private val BorderSubtle = Color(0xFF282E37)
private val InputBg = Color(0xFF0C0E11)

/** Chip de músculo: clave de [selectedMuscle] + etiqueta ES (spec 003 §4). */
private val MUSCLE_CHIPS: List<Pair<String?, String>> = listOf(
    null to "Todos",
    "pierna" to "Pierna",
    "pecho" to "Pecho",
    "espalda" to "Espalda",
    "hombros" to "Hombros",
    "core" to "Core",
    "brazo" to "Brazo"
)

/** Chip de equipamiento: clave de [selectedEquipment] + etiqueta ES. */
private val EQUIPMENT_CHIPS: List<Pair<String?, String>> = listOf(
    null to "Todos",
    "barra" to "Barra",
    "mancuernas" to "Mancuernas",
    "maquina" to "Máquina",
    "corporal" to "Corporal",
    "polea" to "Polea",
    "banda" to "Banda"
)

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerSheet(
    viewModel: ExercisesViewModel = hiltViewModel(),
    onConfirm: (List<String>) -> Unit = {},
    onDismiss: () -> Unit = {},
    // Spec 010: pulsar una fila abre la ficha del ejercicio.
    onExerciseClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SheetBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 48.dp, height = 6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(CardHigh)
            )
        }
    ) {
        ExercisePickerContent(
            uiState = uiState,
            onQueryChange = viewModel::onQueryChange,
            onMuscleSelected = viewModel::onMuscleSelected,
            onEquipmentSelected = viewModel::onEquipmentSelected,
            onToggleSelected = viewModel::onToggleSelected,
            onConfirm = { onConfirm(uiState.selectedIds.toList()) },
            onDismiss = onDismiss,
            onExerciseClick = onExerciseClick,
            modifier = Modifier.fillMaxHeight(0.88f)
        )
    }
}

@Composable
fun ExercisePickerContent(
    uiState: ExercisesUiState,
    onQueryChange: (String) -> Unit = {},
    onMuscleSelected: (String?) -> Unit = {},
    onEquipmentSelected: (String?) -> Unit = {},
    onToggleSelected: (String) -> Unit = {},
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {},
    onExerciseClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SheetBg)
    ) {
        SheetTitleRow(onDismiss = onDismiss)
        SearchBar(
            query = uiState.query,
            onQueryChange = onQueryChange
        )
        ChipsRow(
            chips = MUSCLE_CHIPS,
            selected = uiState.selectedMuscle,
            onSelected = onMuscleSelected
        )
        ChipsRow(
            chips = EQUIPMENT_CHIPS,
            selected = uiState.selectedEquipment,
            onSelected = onEquipmentSelected
        )
        ResultsCount(count = uiState.results.size)
        if (uiState.results.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Sin resultados: ajusta la búsqueda o los filtros",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.results, key = { it.id }) { exercise ->
                    ExerciseRow(
                        exercise = exercise,
                        selected = exercise.id in uiState.selectedIds,
                        onToggle = { onToggleSelected(exercise.id) },
                        // Spec 010: la fila abre la ficha; la píldora añade.
                        onDetail = { onExerciseClick(exercise.id) }
                    )
                }
            }
        }
        ConfirmFooter(
            selectedCount = uiState.selectedCount,
            onConfirm = onConfirm
        )
    }
}

@Composable
private fun SheetTitleRow(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Orange)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "Seleccionar Ejercicio",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        // Cerrar/X: descarta sin emitir (hit target 48dp).
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Card)
                .border(1.dp, BorderSubtle, RoundedCornerShape(999.dp))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = TextMuted)
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { Text("Buscar ejercicio (ej. Sentadilla, Prensa...)", color = TextMuted, fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Orange) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(onClick = { onQueryChange("") }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Borrar búsqueda", tint = TextMuted)
                }
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = InputBg,
            unfocusedContainerColor = InputBg,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = Orange,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
    )
}

@Composable
private fun ChipsRow(
    chips: List<Pair<String?, String>>,
    selected: String?,
    onSelected: (String?) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(chips, key = { it.second }) { (key, label) ->
            val active = key == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (active) Orange else Card)
                    .border(1.dp, if (active) Orange else BorderSubtle, RoundedCornerShape(999.dp))
                    .clickable(onClick = { onSelected(if (active) null else key) })
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .sizeIn(minWidth = 48.dp, minHeight = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (active) OnOrange else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ResultsCount(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("EJERCICIOS", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "$count DISPONIBLES",
            color = Orange,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ExerciseRow(
    exercise: Exercise,
    selected: Boolean,
    onToggle: () -> Unit,
    onDetail: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Card)
            .border(
                1.dp,
                if (selected) Volt else BorderSubtle,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onDetail)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail GIF real vía Coil (Coil sin coil-gif: primer frame estático).
        // Placeholder y error de color local si la carga falla.
        // Docs: /coil-kt/coil (coil-compose AsyncImage + placeholder/error).
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
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
                    .padding(2.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xCC0C0E11))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text("GIF", color = Orange, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                exercise.name,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${muscleLabel(exercise.muscle)} · ${equipmentLabel(exercise.equipment)}",
                color = if (selected) Volt else TextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        // Añadir ↔ Añadido volt (área táctil >= 48dp, píldora compacta).
        Box(
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (selected) Volt else CardHigh)
                    .border(1.dp, if (selected) Volt else BorderSubtle, RoundedCornerShape(999.dp))
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (selected) Icons.Filled.Check else Icons.Filled.Add,
                    contentDescription = null,
                    tint = if (selected) OnVolt else Orange,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (selected) "Añadido" else "Añadir",
                    color = if (selected) OnVolt else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ConfirmFooter(
    selectedCount: Int,
    onConfirm: () -> Unit
) {
    Button(
        onClick = onConfirm,
        enabled = selectedCount > 0,
        colors = ButtonDefaults.buttonColors(
            containerColor = Orange,
            disabledContainerColor = CardHigh,
            contentColor = OnOrange,
            disabledContentColor = TextMuted
        ),
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .height(52.dp)
    ) {
        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            "Listo ($selectedCount seleccionados)",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1C1F)
@Composable
private fun ExercisePickerContentPreview() {
    androidx.compose.material3.MaterialTheme {
        ExercisePickerContent(
            uiState = ExercisesUiState(
                results = FakeExerciseRepository.defaultExercises(),
                selectedIds = setOf(
                    "quads/lever-leg-extension",
                    "hamstrings/lying-leg-curl"
                )
            )
        )
    }
}
