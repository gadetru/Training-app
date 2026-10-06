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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mytrainingplan.app.domain.model.Exercise
import com.mytrainingplan.app.domain.model.PlannedSet
import com.mytrainingplan.app.domain.model.RoutineEditUiState
import com.mytrainingplan.app.domain.model.RoutineExercise
import com.mytrainingplan.app.domain.model.RoutineExerciseUi
import com.mytrainingplan.app.domain.model.RoutineMuscleLabels
import com.mytrainingplan.app.feature.exercises.ExercisePickerSheet

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
    viewModel: RoutineEditViewModel = hiltViewModel(),
    onSaved: () -> Unit = {},
    onBack: () -> Unit = {},
    // Spec 010: la ficha abre desde editar rutina (miniatura/nombre) y
    // desde el buscador (filas del sheet).
    onExerciseClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(routineId) { viewModel.openRoutine(routineId) }
    // Salir atrás sin guardar descarta el borrador (sin rutina fantasma).
    BackHandler { viewModel.onDiscard(onBack) }
    // Sheet del 003 anidado: solo se abre desde el botón de añadir; su estado
    // vive hoisted en esta Screen y el 003 no se toca por dentro.
    var showPicker by rememberSaveable { mutableStateOf(false) }
    RoutineEditContent(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onDurationChange = viewModel::onDurationChange,
        onAddClick = { showPicker = true },
        onToggleExpanded = viewModel::onToggleExpanded,
        onDeleteExercise = viewModel::onDeleteExercise,
        onSetFieldChange = viewModel::onSetFieldChange,
        onRirChange = viewModel::onRirChange,
        onAddSet = viewModel::onAddSet,
        onDeleteSet = viewModel::onDeleteSet,
        onSave = { viewModel.onSave(onSaved) },
        onBack = { viewModel.onDiscard(onBack) },
        onExerciseClick = onExerciseClick
    )
    if (showPicker) {
        ExercisePickerSheet(
            // Cada id confirmado nace con 1 serie vacía (repo paso 2).
            onConfirm = { ids ->
                showPicker = false
                viewModel.onAddExercises(ids)
            },
            // Cerrar/X descarta sin añadir y sin crash.
            onDismiss = { showPicker = false },
            // Spec 010: la ficha abre desde el buscador.
            onExerciseClick = onExerciseClick
        )
    }
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
    onRirChange: (String, String, Int?) -> Unit = { _, _, _ -> },
    onAddSet: (String) -> Unit = {},
    onDeleteSet: (String, String) -> Unit = { _, _ -> },
    onSave: () -> Unit = {},
    onBack: () -> Unit = {},
    onExerciseClick: (String) -> Unit = {}
) {
    // Modal de nombre: se abre desde el lápiz (o el propio título); overlay
    // sobre el constructor, que conserva su posición/scroll al cerrar.
    var showNameDialog by rememberSaveable { mutableStateOf(false) }
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
                    onEditNameClick = { showNameDialog = true },
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
                        onExerciseClick = onExerciseClick,
                        onSetFieldChange = { setId, field, raw ->
                            onSetFieldChange(item.routineExercise.id, setId, field, raw)
                        },
                        onRirChange = { setId, rir ->
                            onRirChange(item.routineExercise.id, setId, rir)
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
        if (showNameDialog) {
            NameDialog(
                currentName = uiState.name,
                onAccept = {
                    showNameDialog = false
                    onNameChange(it)
                },
                onDismiss = { showNameDialog = false }
            )
        }
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
            .statusBarsPadding()
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
    onEditNameClick: () -> Unit,
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
        // Título de solo lectura: el lápiz (y el propio título) abren el modal.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEditNameClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (name.isEmpty()) "Nombre de la Rutina" else name,
                color = if (name.isEmpty()) TextMuted else TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            // Lápiz con hit target >= 48dp: abre el modal de nombre.
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Card2)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(999.dp))
                    .clickable(onClick = onEditNameClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "Editar nombre", tint = Orange)
            }
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

/**
 * Modal de nombre de rutina: se abre desde el lápiz, edita en estado local
 * (sin tocar el repo por tecla) y solo al Aceptar emite el nombre recortado.
 * Cancelar, X, atrás o tap fuera descartan sin cambios. Aceptar deshabilitado
 * en vacío para no borrar el nombre sin querer.
 */
@Composable
private fun NameDialog(
    currentName: String,
    onAccept: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var field by remember {
        mutableStateOf(
            TextFieldValue(currentName, selection = TextRange(0, currentName.length))
        )
    }
    val focusRequester = remember { FocusRequester() }
    val canAccept = field.text.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Card,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                "Nombre de la rutina",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            TextField(
                value = field,
                onValueChange = { field = it },
                singleLine = true,
                placeholder = { Text("Ej. Pierna & Glúteos", color = TextMuted, fontSize = 14.sp) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { if (canAccept) onAccept(field.text.trim()) }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = InputBg,
                    unfocusedContainerColor = InputBg,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Orange,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        },
        dismissButton = {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(CardHigh)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(999.dp))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cancelar", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (canAccept) Orange else CardHigh)
                    .clickable(enabled = canAccept, onClick = { onAccept(field.text.trim()) })
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Aceptar",
                    color = if (canAccept) OnOrange else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
private fun AddExerciseButton(onClick: () -> Unit) {    Row(
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
    onExerciseClick: (String) -> Unit = {},
    onSetFieldChange: (String, SetField, String) -> Unit,
    onRirChange: (String, Int?) -> Unit,
    onAddSet: () -> Unit,
    onDeleteSet: (String) -> Unit
) {
    if (item.expanded) {
        ExpandedExerciseCard(
            index = index,
            item = item,
            onToggleExpanded = onToggleExpanded,
            onDeleteExercise = onDeleteExercise,
            onExerciseClick = onExerciseClick,
            onSetFieldChange = onSetFieldChange,
            onRirChange = onRirChange,
            onAddSet = onAddSet,
            onDeleteSet = onDeleteSet
        )
    } else {
        CollapsedExerciseCard(
            index = index,
            item = item,
            onToggleExpanded = onToggleExpanded,
            onDeleteExercise = onDeleteExercise,
            onExerciseClick = onExerciseClick
        )
    }
}

@Composable
private fun ExpandedExerciseCard(
    index: Int,
    item: RoutineExerciseUi,
    onToggleExpanded: () -> Unit,
    onDeleteExercise: () -> Unit,
    onExerciseClick: (String) -> Unit = {},
    onSetFieldChange: (String, SetField, String) -> Unit,
    onRirChange: (String, Int?) -> Unit,
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
                    .background(CardHigh)
                    // Spec 010: la miniatura abre la ficha.
                    .clickable(onClick = { onExerciseClick(item.exercise.id) }),
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
                    overflow = TextOverflow.Ellipsis,
                    // Spec 010: el nombre abre la ficha.
                    modifier = Modifier.clickable(onClick = { onExerciseClick(item.exercise.id) })
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
                    rir = set.rir,
                    onRirChange = { onRirChange(set.id, it) },
                    weightText = formatWeight(set.weightKg),
                    onWeightChange = { onSetFieldChange(set.id, SetField.WEIGHT_KG, it) },
                    repsText = set.targetReps.toString(),
                    onRepsChange = { onSetFieldChange(set.id, SetField.TARGET_REPS, it) },
                    restText = set.restSeconds.toString(),
                    onRestChange = { onSetFieldChange(set.id, SetField.REST_SECONDS, it) },
                    noteText = set.loadNote.orEmpty(),
                    onNoteChange = { onSetFieldChange(set.id, SetField.LOAD_NOTE, it) },
                    onDelete = { onDeleteSet(set.id) }
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
        Spacer(Modifier.width(6.dp))
        Text(
            "RIR",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(54.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "KG",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "REPS",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(6.dp))
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

/** Opciones RIR de la referencia: calentamiento, 5..1, 0 = fallo. */
private val RIR_OPTIONS: List<Int?> = listOf(null, 5, 4, 3, 2, 1, 0)

private fun rirLabel(rir: Int?): String = if (rir == null) "W" else "$rir"

private fun rirOptionLabel(rir: Int?): String = when (rir) {
    null -> "W · Calentamiento"
    0 -> "0 · Fallo"
    else -> "RIR $rir"
}

/**
 * Cuadrado RIR de la referencia: muestra `W` / número / `0` y abre el
 * desplegable al pulsarlo. El `0` (fallo) se tiñe naranja como en `code.html`.
 */
@Composable
private fun RirCell(
    rir: Int?,
    onRirChange: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val isFailure = rir == 0
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CardHigh)
            .border(
                1.dp,
                if (isFailure) Orange.copy(alpha = 0.6f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = { expanded = true })
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            rirLabel(rir),
            color = when {
                isFailure -> Orange
                rir == null -> TextMuted
                else -> TextPrimary
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Card2)
        ) {
            RIR_OPTIONS.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            rirOptionLabel(option),
                            color = if (option == rir) Orange else TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (option == rir) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        expanded = false
                        onRirChange(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun SetRow(
    setNumber: Int,
    rir: Int?,
    onRirChange: (Int?) -> Unit,
    weightText: String,
    onWeightChange: (String) -> Unit,
    repsText: String,
    onRepsChange: (String) -> Unit,
    restText: String,
    onRestChange: (String) -> Unit,
    noteText: String,
    onNoteChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    val isFailure = rir == 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Bg)
            .border(
                1.dp,
                if (isFailure) Orange.copy(alpha = 0.5f) else BorderSubtle,
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
                    .background(if (isFailure) Orange else CardHigh),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$setNumber",
                    color = if (isFailure) OnOrange else TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(6.dp))
            RirCell(
                rir = rir,
                onRirChange = onRirChange,
                modifier = Modifier.width(54.dp)
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
                value = repsText,
                onValueChange = onRepsChange,
                keyboardType = KeyboardType.Number,
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
    onDeleteExercise: () -> Unit,
    onExerciseClick: (String) -> Unit = {}
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
                overflow = TextOverflow.Ellipsis,
                // Spec 010: el nombre abre la ficha (el resto expande).
                modifier = Modifier.clickable(onClick = { onExerciseClick(item.exercise.id) })
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
            .navigationBarsPadding()
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
                            PlannedSet(id = "s1", routineExerciseId = "re1", setNumber = 1, targetReps = 12, weightKg = 60.0, restSeconds = 90, rir = null),
                            PlannedSet(id = "s2", routineExerciseId = "re1", setNumber = 2, targetReps = 10, weightKg = 90.0, restSeconds = 90, rir = 3),
                            PlannedSet(id = "s3", routineExerciseId = "re1", setNumber = 3, targetReps = 6, weightKg = 105.0, restSeconds = 120, rir = 0)
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
