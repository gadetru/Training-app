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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.mytrainingplan.app.ui.theme.AppColors
import com.mytrainingplan.app.ui.theme.AppDimens
import com.mytrainingplan.app.ui.theme.AppTextSizes
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.decode.BitmapFactoryDecoder
import coil.request.ImageRequest
import com.mytrainingplan.app.data.repository.FakeExerciseRepository
import com.mytrainingplan.app.domain.model.Exercise
import com.mytrainingplan.app.domain.model.ExercisesUiState

// Colores y medidas desde ui/theme (spec 011): sin tokens locales.

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
        containerColor = AppColors.SheetBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = AppDimens.SpaceXl, bottom = AppDimens.SpaceXs)
                    .size(width = AppDimens.TouchMin, height = AppDimens.SpaceSm)
                    .clip(RoundedCornerShape(AppDimens.RadiusPill))
                    .background(AppColors.CardHigh)
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
            .background(AppColors.SheetBg)
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
                    color = AppColors.TextMuted,
                    fontSize = AppTextSizes.BodyLg
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = AppDimens.SpaceHuge, vertical = AppDimens.SpaceMd),
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceLg)
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
            .padding(horizontal = AppDimens.ScreenHorizontal, vertical = AppDimens.SpaceMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(AppDimens.TitleDot)
                .clip(RoundedCornerShape(AppDimens.RadiusPill))
                .background(AppColors.Orange)
        )
        Spacer(Modifier.width(AppDimens.SpaceLg))
        Text(
            "Seleccionar Ejercicio",
            color = AppColors.TextPrimary,
            fontSize = AppTextSizes.Headline,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        // Cerrar/X: descarta sin emitir (hit target 48dp).
        Box(
            modifier = Modifier
                .size(AppDimens.TouchMin)
                .clip(RoundedCornerShape(AppDimens.RadiusPill))
                .background(AppColors.Card2)
                .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusPill))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = AppColors.TextMuted)
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    // Spec 012, paso 3: la ayuda se oculta al enfocar, antes de escribir.
    var focused by remember { mutableStateOf(false) }
    TextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = {
            if (!focused) {
                Text("Buscar ejercicio (ej. Sentadilla, Prensa...)", color = AppColors.TextMuted, fontSize = AppTextSizes.TitleSm)
            }
        },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = AppColors.Orange) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(AppDimens.TouchMin)
                        .clickable(onClick = { onQueryChange("") }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Borrar búsqueda", tint = AppColors.TextMuted)
                }
            }
        },
        shape = RoundedCornerShape(AppDimens.RadiusXl),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = AppColors.InputBg,
            unfocusedContainerColor = AppColors.InputBg,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = AppColors.Orange,
            focusedTextColor = AppColors.TextPrimary,
            unfocusedTextColor = AppColors.TextPrimary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.ScreenHorizontal, vertical = AppDimens.SpaceXs)
            .onFocusChanged { focused = it.isFocused }
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
        contentPadding = PaddingValues(horizontal = AppDimens.ScreenHorizontal, vertical = AppDimens.SpaceSm),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceMd)
    ) {
        items(chips, key = { it.second }) { (key, label) ->
            val active = key == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(AppDimens.RadiusPill))
                    .background(if (active) AppColors.Orange else AppColors.Card2)
                    .border(AppDimens.BorderThin, if (active) AppColors.Orange else AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusPill))
                    .clickable(onClick = { onSelected(if (active) null else key) })
                    .padding(horizontal = AppDimens.SpaceXxl, vertical = AppDimens.SpaceLg)
                    .sizeIn(minWidth = AppDimens.TouchMin, minHeight = AppDimens.ChipMinHeight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (active) AppColors.OnOrange else AppColors.TextMuted,
                    fontSize = AppTextSizes.Body,
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
            .padding(horizontal = AppDimens.ScreenHorizontal, vertical = AppDimens.SpaceSm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("EJERCICIOS", color = AppColors.TextMuted, fontSize = AppTextSizes.Caption, fontWeight = FontWeight.SemiBold)
        Text(
            "$count DISPONIBLES",
            color = AppColors.Orange,
            fontSize = AppTextSizes.Caption,
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
            .clip(RoundedCornerShape(AppDimens.RadiusCard))
            .background(AppColors.Card2)
            .border(
                AppDimens.BorderThin,
                if (selected) AppColors.Volt else AppColors.BorderSubtle,
                RoundedCornerShape(AppDimens.RadiusCard)
            )
            .clickable(onClick = onDetail)
            .padding(AppDimens.SpaceLg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Miniatura quieta (spec 012, paso 7): primer frame estático forzando
        // BitmapFactoryDecoder por petición; el hero de la ficha anima con el
        // loader global (TrainingApp). Placeholder/error de color si falla.
        // Docs: /coil-kt/coil (coil-compose AsyncImage + placeholder/error).
        Box(
            modifier = Modifier
                .size(AppDimens.Thumb)
                .clip(RoundedCornerShape(AppDimens.RadiusXl))
                .background(AppColors.CardHigh),
            contentAlignment = Alignment.BottomEnd
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(exercise.gifUrl)
                    .decoderFactory(BitmapFactoryDecoder.Factory())
                    .build(),
                contentDescription = exercise.name,
                placeholder = ColorPainter(AppColors.CardHigh),
                error = ColorPainter(AppColors.CardHigh),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .padding(AppDimens.SpaceXxs)
                    .clip(RoundedCornerShape(AppDimens.RadiusXs))
                    .background(AppColors.BadgeBg)
                    .padding(horizontal = AppDimens.SpaceXs, vertical = AppDimens.Divider)
            ) {
                Text("GIF", color = AppColors.Orange, fontSize = AppTextSizes.Badge, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(AppDimens.SpaceXl))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                exercise.name,
                color = AppColors.TextPrimary,
                fontSize = AppTextSizes.TitleSm,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(AppDimens.SpaceXxs))
            Text(
                "${muscleLabel(exercise.muscle)} · ${equipmentLabel(exercise.equipment)}",
                color = if (selected) AppColors.Volt else AppColors.TextMuted,
                fontSize = AppTextSizes.Body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(AppDimens.SpaceMd))
        // Añadir ↔ Añadido volt (área táctil >= 48dp, píldora compacta).
        Box(
            modifier = Modifier
                .sizeIn(minWidth = AppDimens.TouchMin, minHeight = AppDimens.TouchMin),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(AppDimens.RadiusPill))
                    .background(if (selected) AppColors.Volt else AppColors.CardHigh)
                    .border(AppDimens.BorderThin, if (selected) AppColors.Volt else AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusPill))
                    .clickable(onClick = onToggle)
                    .padding(horizontal = AppDimens.SpaceXl, vertical = AppDimens.SpaceMd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (selected) Icons.Filled.Check else Icons.Filled.Add,
                    contentDescription = null,
                    tint = if (selected) AppColors.OnVolt else AppColors.Orange,
                    modifier = Modifier.size(AppDimens.IconSm)
                )
                Spacer(Modifier.width(AppDimens.SpaceXs))
                Text(
                    if (selected) "Añadido" else "Añadir",
                    color = if (selected) AppColors.OnVolt else AppColors.TextPrimary,
                    fontSize = AppTextSizes.Small,
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
            containerColor = AppColors.Orange,
            disabledContainerColor = AppColors.CardHigh,
            contentColor = AppColors.OnOrange,
            disabledContentColor = AppColors.TextMuted
        ),
        shape = RoundedCornerShape(AppDimens.RadiusPill),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.ScreenHorizontal, vertical = AppDimens.SpaceHuge)
            .height(AppDimens.ConfirmHeight)
    ) {
        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(AppDimens.IconMd))
        Spacer(Modifier.width(AppDimens.SpaceMd))
        Text(
            "Listo ($selectedCount seleccionados)",
            fontSize = AppTextSizes.TitleSm,
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
