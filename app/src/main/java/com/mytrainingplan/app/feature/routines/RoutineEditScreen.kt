package com.mytrainingplan.app.feature.routines

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mytrainingplan.app.domain.model.Exercise
import com.mytrainingplan.app.domain.model.PlannedSet
import com.mytrainingplan.app.domain.model.RoutineEditUiState
import com.mytrainingplan.app.domain.model.RoutineExercise
import com.mytrainingplan.app.domain.model.RoutineExerciseUi
import com.mytrainingplan.app.domain.model.RoutineMuscleLabels
import com.mytrainingplan.app.domain.model.SetType

// Tokens references/plantilla-editar-rutina/DESIGN.md + patrón HomeScreen (002).
// Tipografías de sistema en Fase A (Outfit/Plus Jakarta/Space Grotesk van en Futuros).
private val Bg = Color(0xFF111316)
private val Card = Color(0xFF1A1C1F)
private val Card2 = Color(0xFF1E2023)
private val CardHigh = Color(0xFF282A2D)
private val Orange = Color(0xFFFF5E00)
private val Volt = Color(0xFFCCFF00)
private val OnOrange = Color(0xFF1A0A00)
private val TextPrimary = Color(0xFFF5F7FA)
private val TextMuted = Color(0xFF8B95A5)
private val BorderSubtle = Color(0xFF282E37)
private val InputBg = Color(0xFF0C0E11)
private val ErrorRed = Color(0xFFFF8A80)

private fun SetType.label(): String = when (this) {
    SetType.WARMUP -> "Calent."
    SetType.NORMAL -> "Normal"
    SetType.FAILURE -> "Al fallo"
}

private fun formatWeight(weightKg: Double): String =
    if (weightKg % 1.0 == 0.0) weightKg.toInt().toString() else weightKg.toString()

private fun Modifier.dashedBorder(
    color: Color,
    strokeWidth: Dp = 2.dp,
    cornerRadius: Dp = 12.dp,
    dash: Dp = 8.dp,
    gap: Dp = 6.dp
): Modifier = drawBehind {
    val stroke = Stroke(
        width = strokeWidth.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx()))
    )
    val inset = strokeWidth.toPx() / 2
    drawRoundRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2, size.height - inset * 2),
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = stroke
    )
}

@Composable
fun RoutineEditScreen(
    routineId: String? = null,
    viewModel: RoutineEditViewModel = viewModel(),
    // TODO paso 5: el sheet del 003 se anida aquí; el botón aún no abre nada.
    onAddClick: () -> Unit = {},
    onSaved: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(routineId) { viewModel.openRoutine(routineId) }
    // Salir atrás sin guardar descarta el borrador (sin rutina fantasma).
    BackHandler { viewModel.onDiscard(onBack) }
    RoutineEditContent(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onDurationChange = viewModel::onDurationChange,
        onAddClick = onAddClick,
        onToggleExpanded = viewModel::onToggleExpanded,
        onDeleteExercise = viewModel::onDeleteExercise,
        onSetFieldChange = viewModel::onSetFieldChange,
        onSetTypeChange = viewModel::onSetTypeChange,
        onAddSet = viewModel::onAddSet,
        onDeleteSet = viewModel::onDeleteSet,
        onSave = { viewModel.onSave(onSaved) },
        onBack = { viewModel.onDiscard(onBack) }
    )
}

@Composable
fun RoutineEditContent(
    uiState: RoutineEditUiState,
    onNameChange: (String) -> Unit = {},
    onDurationChange: (Int) -> Unit = {},
    onAddClick: () -> Unit = {},
    onToggleExpanded: (String) -> Unit = {},
    onDeleteExercise: (String) -> Unit = {},
    onSetFieldChange: (String, String, SetField, String) -> Unit = { _, _, _, _ -> },
    onSetTypeChange: (String, String, SetType) -> Unit = { _, _, _ -> },
    onAddSet: (String) -> Unit = {},
    onDeleteSet: (String, String) -> Unit = { _, _ -> },
    onSave: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            EditHeader(
                onBack = onBack,
                onSave = onSave,
                isSaving = uiState.isSaving
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp, bottom = 150.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MetaCard(
                    name = uiState.name,
                    onNameChange = onNameChange,
                    exerciseCount = uiState.exercises.size,
                    durationMin = uiState.durationMin,
                    onDurationChange = onDurationChange,
                    tags = uiState.tags
                )
                AddExerciseButton(onClick = onAddClick)
                uiState.exercises.forEachIndexed { index, item ->
                    ExerciseAccordion(
                        index = index,
                        item = item,
                        onToggleExpanded = { onToggleExpanded(item.routineExercise.id) },
                        onDeleteExercise = { onDeleteExercise(item.routineExercise.id) },
                        onSetFieldChange = { setId, field, raw ->
                            onSetFieldChange(item.routineExercise.id, setId, field, raw)
                        },
                        onSetTypeChange = { setId, type ->
                            onSetTypeChange(item.routineExercise.id, setId, type)
                        },
                        onAddSet = { onAddSet(item.routineExercise.id) },
                        onDeleteSet = { setId -> onDeleteSet(item.routineExercise.id, setId) }
                    )
                }
            }
        }
        SaveFooter(
            exerciseCount = uiState.exercises.size,
            totalSets = uiState.exercises.sumOf { it.sets.size },
            durationMin = uiState.durationMin,
            isSaving = uiState.isSaving,
            onSave = onSave,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun EditHeader(
    onBack: () -> Unit,
    onSave: () -> Unit,
    isSaving: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Bg)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Atrás (hit target 48dp).
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
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Card)
                .border(1.dp, BorderSubtle, RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("MODO EDICIÓN", color = Orange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Orange)
                .clickable(onClick = onSave)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (isSaving) "Guardando…" else "Guardar",
                color = OnOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MetaCard(
    name: String,
    onNameChange: (String) -> Unit,
    exerciseCount: Int,
    durationMin: Int,
    onDurationChange: (Int) -> Unit,
    tags: List<String>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Card)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = name,
                onValueChange = onNameChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box {
                        if (name.isEmpty()) {
                            Text(
                                "Nombre de la Rutina",
                                color = TextMuted,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        inner()
                    }
                }
            )
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.Edit, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "$exerciseCount ejercicios configurados • ~$durationMin min estimados",
            color = TextMuted,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(12.dp))
        // Duración editable + tags automáticos por músculo (no editables).
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Duración", color = TextMuted, fontSize = 12.sp)
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = durationMin.toString(),
                onValueChange = { raw -> raw.toIntOrNull()?.let(onDurationChange) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .width(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardHigh)
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("min", color = TextMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        if (tags.isEmpty()) {
            Text(
                "Sin ejercicios: añade uno para ver los grupos musculares",
                color = TextMuted,
                fontSize = 11.sp
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Card2)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(tag, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddExerciseButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .dashedBorder(color = Orange.copy(alpha = 0.5f))
            .clip(RoundedCornerShape(12.dp))
            .background(Card)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.AddCircle, contentDescription = null, tint = Orange, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            "+ Añadir Nuevo Ejercicio a la Rutina",
            color = Orange,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ExerciseAccordion(
    index: Int,
    item: RoutineExerciseUi,
    onToggleExpanded: () -> Unit,
    onDeleteExercise: () -> Unit,
    onSetFieldChange: (String, SetField, String) -> Unit,
    onSetTypeChange: (String, SetType) -> Unit,
    onAddSet: () -> Unit,
    onDeleteSet: (String) -> Unit
) {
    if (item.expanded) {
        ExpandedExerciseCard(
            index = index,
            item = item,
            onToggleExpanded = onToggleExpanded,
            onDeleteExercise = onDeleteExercise,
            onSetFieldChange = onSetFieldChange,
            onSetTypeChange = onSetTypeChange,
            onAddSet = onAddSet,
            onDeleteSet = onDeleteSet
        )
    } else {
        CollapsedExerciseCard(
            index = index,
            item = item,
            onToggleExpanded = onToggleExpanded,
            onDeleteExercise = onDeleteExercise
        )
    }
}

@Composable
private fun ExpandedExerciseCard(
    index: Int,
    item: RoutineExerciseUi,
    onToggleExpanded: () -> Unit,
    onDeleteExercise: () -> Unit,
    onSetFieldChange: (String, SetField, String) -> Unit,
    onSetTypeChange: (String, SetType) -> Unit,
    onAddSet: () -> Unit,
    onDeleteSet: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Card)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
    ) {
        // Cabecera con thumbnail Coil 56dp (patrón ExerciseRow del 003).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardHigh.copy(alpha = 0.4f))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Orange),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${index + 1}",
                    color = OnOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardHigh),
                contentAlignment = Alignment.BottomEnd
            ) {
                AsyncImage(
                    model = item.exercise.gifUrl,
                    contentDescription = item.exercise.name,
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
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.exercise.name,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${RoutineMuscleLabels.labelFor(item.exercise.muscle)} • ${item.sets.size} series",
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onToggleExpanded),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Colapsar", tint = TextMuted)
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onDeleteExercise),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar ejercicio", tint = ErrorRed)
            }
        }
        Column(modifier = Modifier.padding(12.dp)) {
            SetsHeaderRow()
            Spacer(Modifier.height(6.dp))
            item.sets.forEach { set ->
                SetRow(
                    setNumber = set.setNumber,
                    repsText = set.targetReps.toString(),
                    onRepsChange = { onSetFieldChange(set.id, SetField.TARGET_REPS, it) },
                    weightText = formatWeight(set.weightKg),
                    onWeightChange = { onSetFieldChange(set.id, SetField.WEIGHT_KG, it) },
                    restText = set.restSeconds.toString(),
                    onRestChange = { onSetFieldChange(set.id, SetField.REST_SECONDS, it) },
                    selectedType = set.setType,
                    onTypeChange = { onSetTypeChange(set.id, it) },
                    noteText = set.loadNote.orEmpty(),
                    onNoteChange = { onSetFieldChange(set.id, SetField.LOAD_NOTE, it) },
                    onDelete = { onDeleteSet(set.id) },
                    highlightFailure = set.setType == SetType.FAILURE
                )
                Spacer(Modifier.height(8.dp))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Bg)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                    .clickable(onClick = onAddSet)
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = Orange, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Añadir Serie", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SetsHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "SERIE",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(44.dp)
        )
        Text(
            "REPS",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Text(
            "KG",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Text(
            "DESC",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(48.dp))
    }
}

@Composable
private fun SetRow(
    setNumber: Int,
    repsText: String,
    onRepsChange: (String) -> Unit,
    weightText: String,
    onWeightChange: (String) -> Unit,
    restText: String,
    onRestChange: (String) -> Unit,
    selectedType: SetType,
    onTypeChange: (SetType) -> Unit,
    noteText: String,
    onNoteChange: (String) -> Unit,
    onDelete: () -> Unit,
    highlightFailure: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Bg)
            .border(
                1.dp,
                if (highlightFailure) Orange.copy(alpha = 0.5f) else BorderSubtle,
                RoundedCornerShape(12.dp)
            )
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (highlightFailure) Orange else CardHigh),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$setNumber",
                    color = if (highlightFailure) OnOrange else TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(6.dp))
            CellInput(
                value = repsText,
                onValueChange = onRepsChange,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(6.dp))
            CellInput(
                value = weightText,
                onValueChange = onWeightChange,
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(6.dp))
            CellInput(
                value = restText,
                onValueChange = onRestChange,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Eliminar serie", tint = TextMuted)
            }
        }
        Spacer(Modifier.height(8.dp))
        // Selector de tipo por fila (sin propagación).
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SetType.entries.forEach { type ->
                val selected = type == selectedType
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selected) Orange else CardHigh)
                        .border(
                            1.dp,
                            if (selected) Orange else BorderSubtle,
                            RoundedCornerShape(999.dp)
                        )
                        .clickable(onClick = { onTypeChange(type) })
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        type.label(),
                        color = if (selected) OnOrange else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = noteText,
            onValueChange = onNoteChange,
            singleLine = true,
            textStyle = TextStyle(color = TextPrimary, fontSize = 12.sp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CardHigh)
                .padding(vertical = 8.dp, horizontal = 10.dp),
            decorationBox = { inner ->
                Box {
                    if (noteText.isEmpty()) {
                        Text("Nota (ej. goma amarilla)", color = TextMuted, fontSize = 12.sp)
                    }
                    inner()
                }
            }
        )
    }
}

@Composable
private fun CellInput(
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = TextStyle(
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        ),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CardHigh)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.Center) {
                inner()
            }
        }
    )
}

@Composable
private fun CollapsedExerciseCard(
    index: Int,
    item: RoutineExerciseUi,
    onToggleExpanded: () -> Unit,
    onDeleteExercise: () -> Unit
) {
    val summary = if (item.sets.isEmpty()) {
        "Sin series"
    } else {
        val maxWeight = item.sets.maxOf { it.weightKg }
        val rest = item.sets.first().restSeconds
        "${item.sets.size} series configuradas • ${formatWeight(maxWeight)} kg • ${rest}s descanso"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Card)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .clickable(onClick = onToggleExpanded)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(CardHigh),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${index + 1}",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.exercise.name,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(summary, color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onDeleteExercise),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Delete, contentDescription = "Eliminar ejercicio", tint = TextMuted)
        }
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Expandir", tint = TextMuted)
        }
    }
}

@Composable
private fun SaveFooter(
    exerciseCount: Int,
    totalSets: Int,
    durationMin: Int,
    isSaving: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Card)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$exerciseCount ejercicios • $totalSets series totales",
                color = TextMuted,
                fontSize = 12.sp
            )
            Text("~$durationMin min", color = Volt, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Orange)
                .clickable(onClick = onSave)
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = OnOrange,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (isSaving) "Guardando…" else "Finalizar y Guardar Rutina",
                color = OnOrange,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111316)
@Composable
private fun RoutineEditContentPreview() {
    val sentadilla = Exercise(
        id = "quads/barbell-bench-squat",
        slug = "barbell-bench-squat",
        name = "Sentadilla con barra",
        muscle = "quads",
        bodyPart = "legs",
        equipment = "barbell",
        category = "strength",
        gifUrl = ""
    )
    val prensa = Exercise(
        id = "quads/lever-alternate-leg-press",
        slug = "lever-alternate-leg-press",
        name = "Prensa de piernas",
        muscle = "quads",
        bodyPart = "legs",
        equipment = "lever",
        category = "strength",
        gifUrl = ""
    )
    androidx.compose.material3.MaterialTheme {
        RoutineEditContent(
            uiState = RoutineEditUiState(
                routineId = "preview",
                name = "Pierna & Glúteos Hipertrofia",
                durationMin = 45,
                tags = listOf("Pierna"),
                exercises = listOf(
                    RoutineExerciseUi(
                        routineExercise = RoutineExercise(id = "re1", routineId = "preview", exerciseId = sentadilla.id),
                        exercise = sentadilla,
                        sets = listOf(
                            PlannedSet(id = "s1", routineExerciseId = "re1", setNumber = 1, targetReps = 12, weightKg = 60.0, restSeconds = 90, setType = SetType.WARMUP),
                            PlannedSet(id = "s2", routineExerciseId = "re1", setNumber = 2, targetReps = 10, weightKg = 90.0, restSeconds = 90),
                            PlannedSet(id = "s3", routineExerciseId = "re1", setNumber = 3, targetReps = 6, weightKg = 105.0, restSeconds = 120, setType = SetType.FAILURE)
                        ),
                        expanded = true
                    ),
                    RoutineExerciseUi(
                        routineExercise = RoutineExercise(id = "re2", routineId = "preview", exerciseId = prensa.id),
                        exercise = prensa,
                        sets = listOf(
                            PlannedSet(id = "s4", routineExerciseId = "re2", setNumber = 1, targetReps = 10, weightKg = 120.0, restSeconds = 90)
                        ),
                        expanded = false
                    )
                )
            )
        )
    }
}
