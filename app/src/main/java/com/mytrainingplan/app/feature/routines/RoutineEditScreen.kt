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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import com.mytrainingplan.app.ui.theme.AppColors
import com.mytrainingplan.app.ui.theme.AppDimens
import com.mytrainingplan.app.ui.theme.AppTextSizes
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.decode.BitmapFactoryDecoder
import coil.request.ImageRequest
import com.mytrainingplan.app.domain.model.Exercise
import com.mytrainingplan.app.domain.model.PlannedSet
import com.mytrainingplan.app.domain.model.RoutineEditUiState
import com.mytrainingplan.app.domain.model.RoutineExercise
import com.mytrainingplan.app.domain.model.RoutineExerciseUi
import com.mytrainingplan.app.domain.model.RoutineMuscleLabels
import com.mytrainingplan.app.feature.exercises.ExercisePickerSheet
import kotlinx.coroutines.launch

// Colores y medidas desde ui/theme (spec 011): sin tokens locales.

private fun formatWeight(weightKg: Double): String =
    if (weightKg % 1.0 == 0.0) weightKg.toInt().toString() else weightKg.toString()

private fun Modifier.dashedBorder(
    color: Color,
    strokeWidth: Dp = AppDimens.BorderThick,
    cornerRadius: Dp = AppDimens.RadiusXl,
    dash: Dp = AppDimens.SpaceMd,
    gap: Dp = AppDimens.SpaceSm
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
        onExerciseNoteChange = viewModel::onExerciseNoteChange,
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
    onExerciseNoteChange: (String, String) -> Unit = { _, _ -> },
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
            .background(AppColors.Bg)
            .testTag("routineEditRoot")
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
                    // Con el teclado abierto el rango de scroll sube sobre él;
                    // si no, "Añadir Serie" y el campo enfocado quedan tapados.
                    .imePadding()
                    .padding(horizontal = AppDimens.ScreenHorizontal)
                    .padding(top = AppDimens.SpaceXs, bottom = AppDimens.WorkoutBottom),
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceHuge)
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
                        onExerciseNoteChange = { note ->
                            onExerciseNoteChange(item.routineExercise.id, note)
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
            .background(AppColors.Bg)
            .statusBarsPadding()
            .padding(horizontal = AppDimens.ScreenHorizontal, vertical = AppDimens.SpaceXl),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Atrás (hit target 48dp).
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
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(AppDimens.RadiusPill))
                .background(AppColors.Card)
                .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusPill))
                .padding(horizontal = AppDimens.SpaceXl, vertical = AppDimens.SpaceSm)
        ) {
            Text("MODO EDICIÓN", color = AppColors.Orange, fontSize = AppTextSizes.Caption, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(AppDimens.RadiusPill))
                .background(AppColors.Orange)
                .clickable(onClick = onSave)
                .padding(horizontal = AppDimens.SpaceHuge, vertical = AppDimens.SpaceLg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (isSaving) "Guardando…" else "Guardar",
                color = AppColors.OnOrange,
                fontSize = AppTextSizes.Body,
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
            .clip(RoundedCornerShape(AppDimens.RadiusCard))
            .background(AppColors.Card)
            .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusCard))
            .padding(AppDimens.SpaceHuge)
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
                color = if (name.isEmpty()) AppColors.TextMuted else AppColors.TextPrimary,
                fontSize = AppTextSizes.DisplaySm,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(AppDimens.SpaceMd))
            // Lápiz con hit target >= 48dp: abre el modal de nombre.
            Box(
                modifier = Modifier
                    .size(AppDimens.TouchMin)
                    .clip(RoundedCornerShape(AppDimens.RadiusPill))
                    .background(AppColors.Card2)
                    .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusPill))
                    .clickable(onClick = onEditNameClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "Editar nombre", tint = AppColors.Orange)
            }
        }
        Spacer(Modifier.height(AppDimens.SpaceSm))
        Text(
            "$exerciseCount ejercicios configurados • ~$durationMin min estimados",
            color = AppColors.TextMuted,
            fontSize = AppTextSizes.Body
        )
        Spacer(Modifier.height(AppDimens.SpaceXl))
        // Duración editable + tags automáticos por músculo (no editables).
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Duración", color = AppColors.TextMuted, fontSize = AppTextSizes.Body)
            Spacer(Modifier.width(AppDimens.SpaceMd))
            BasicTextField(
                value = durationMin.toString(),
                onValueChange = { raw -> raw.toIntOrNull()?.let(onDurationChange) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                    color = AppColors.TextPrimary,
                    fontSize = AppTextSizes.TitleSm,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .width(AppDimens.DurationWidth)
                    .clip(RoundedCornerShape(AppDimens.RadiusMd))
                    .background(AppColors.CardHigh)
                    .padding(vertical = AppDimens.SpaceMd, horizontal = AppDimens.SpaceXs)
            )
            Spacer(Modifier.width(AppDimens.SpaceSm))
            Text("min", color = AppColors.TextMuted, fontSize = AppTextSizes.Body)
        }
        Spacer(Modifier.height(AppDimens.SpaceXl))
        if (tags.isEmpty()) {
            Text(
                "Sin ejercicios: añade uno para ver los grupos musculares",
                color = AppColors.TextMuted,
                fontSize = AppTextSizes.Small
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSm)
            ) {
                tags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(AppDimens.RadiusMd))
                            .background(AppColors.Card2)
                            .padding(horizontal = AppDimens.SpaceLg, vertical = AppDimens.SpaceSm)
                    ) {
                        Text(tag, color = AppColors.TextPrimary, fontSize = AppTextSizes.Small, fontWeight = FontWeight.SemiBold)
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
        containerColor = AppColors.Card,
        shape = RoundedCornerShape(AppDimens.RadiusCard),
        title = {
            Text(
                "Nombre de la rutina",
                color = AppColors.TextPrimary,
                fontSize = AppTextSizes.Headline,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            TextField(
                value = field,
                onValueChange = { field = it },
                singleLine = true,
                placeholder = { Text("Ej. Pierna & Glúteos", color = AppColors.TextMuted, fontSize = AppTextSizes.TitleSm) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { if (canAccept) onAccept(field.text.trim()) }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = AppColors.InputBg,
                    unfocusedContainerColor = AppColors.InputBg,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = AppColors.Orange,
                    focusedTextColor = AppColors.TextPrimary,
                    unfocusedTextColor = AppColors.TextPrimary
                ),
                shape = RoundedCornerShape(AppDimens.RadiusXl),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        },
        dismissButton = {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(AppDimens.RadiusPill))
                    .background(AppColors.CardHigh)
                    .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusPill))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = AppDimens.SpaceHuge, vertical = AppDimens.SpaceLg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cancelar", color = AppColors.TextPrimary, fontSize = AppTextSizes.Body, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(AppDimens.RadiusPill))
                    .background(if (canAccept) AppColors.Orange else AppColors.CardHigh)
                    .clickable(enabled = canAccept, onClick = { onAccept(field.text.trim()) })
                    .padding(horizontal = AppDimens.SpaceHuge, vertical = AppDimens.SpaceLg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Aceptar",
                    color = if (canAccept) AppColors.OnOrange else AppColors.TextMuted,
                    fontSize = AppTextSizes.Body,
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
            .dashedBorder(color = AppColors.Orange.copy(alpha = 0.5f))
            .clip(RoundedCornerShape(AppDimens.RadiusXl))
            .background(AppColors.Card)
            .clickable(onClick = onClick)
            .padding(vertical = AppDimens.SpaceXxl, horizontal = AppDimens.SpaceHuge)
            .testTag("routineEditAddExercise"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.AddCircle, contentDescription = null, tint = AppColors.Orange, modifier = Modifier.size(AppDimens.IconXl))
        Spacer(Modifier.width(AppDimens.SpaceMd))
        Text(
            "+ Añadir Nuevo Ejercicio a la Rutina",
            color = AppColors.Orange,
            fontSize = AppTextSizes.TitleSm,
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
    onExerciseNoteChange: (String) -> Unit,
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
            onExerciseNoteChange = onExerciseNoteChange,
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
    onExerciseNoteChange: (String) -> Unit,
    onAddSet: () -> Unit,
    onDeleteSet: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.RadiusCard))
            .background(AppColors.Card)
            .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusCard))
    ) {
        // Cabecera con thumbnail Coil 56dp (patrón ExerciseRow del 003).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.CardHigh.copy(alpha = 0.4f))
                .padding(AppDimens.SpaceXl),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(AppDimens.SetNumber)
                    .clip(RoundedCornerShape(AppDimens.RadiusSm))
                    .background(AppColors.Orange),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${index + 1}",
                    color = AppColors.OnOrange,
                    fontSize = AppTextSizes.Body,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(AppDimens.SpaceLg))
            Box(
                modifier = Modifier
                    .size(AppDimens.Thumb)
                    .clip(RoundedCornerShape(AppDimens.RadiusXl))
                    .background(AppColors.CardHigh)
                    // Spec 010: la miniatura abre la ficha.
                    .clickable(onClick = { onExerciseClick(item.exercise.id) }),
                contentAlignment = Alignment.BottomEnd
            ) {
                // Miniatura quieta (spec 012, paso 7): primer frame estático;
                // fuera de este spec animar lista/editar/sesión.
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.exercise.gifUrl)
                        .decoderFactory(BitmapFactoryDecoder.Factory())
                        .build(),
                    contentDescription = item.exercise.name,
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
            Spacer(Modifier.width(AppDimens.SpaceLg))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.exercise.name,
                    color = AppColors.TextPrimary,
                    fontSize = AppTextSizes.Title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    // Spec 010: el nombre abre la ficha.
                    modifier = Modifier.clickable(onClick = { onExerciseClick(item.exercise.id) })
                )
                Spacer(Modifier.height(AppDimens.SpaceXxs))
                Text(
                    "${RoutineMuscleLabels.labelFor(item.exercise.muscle)} • ${item.sets.size} series",
                    color = AppColors.TextMuted,
                    fontSize = AppTextSizes.Small,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .size(AppDimens.TouchMin)
                    .clickable(onClick = onToggleExpanded),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Colapsar", tint = AppColors.TextMuted)
            }
            Box(
                modifier = Modifier
                    .size(AppDimens.TouchMin)
                    .clickable(onClick = onDeleteExercise),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar ejercicio", tint = AppColors.ErrorRed)
            }
        }
        Column(modifier = Modifier.padding(AppDimens.SpaceXl)) {
            SetsHeaderRow()
            Spacer(Modifier.height(AppDimens.SpaceSm))
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
                    onDelete = { onDeleteSet(set.id) }
                )
                Spacer(Modifier.height(AppDimens.SpaceMd))
            }
            // Nota única del ejercicio (con adopción de notas viejas por
            // serie); antes había un campo por serie.
            ExerciseNoteField(
                note = effectiveExerciseNote(item),
                onNoteChange = onExerciseNoteChange,
                modifier = Modifier.testTag("exerciseNote:${item.routineExercise.id}")
            )
            Spacer(Modifier.height(AppDimens.SpaceMd))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppDimens.RadiusXl))
                    .background(AppColors.Bg)
                    .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusXl))
                    .clickable(onClick = onAddSet)
                    .padding(vertical = AppDimens.SpaceXl),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = AppColors.Orange, modifier = Modifier.size(AppDimens.IconMd))
                Spacer(Modifier.width(AppDimens.SpaceSm))
                Text("Añadir Serie", color = AppColors.TextPrimary, fontSize = AppTextSizes.Body, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SetsHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.SpaceXs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "SERIE",
            color = AppColors.TextMuted,
            fontSize = AppTextSizes.Caption,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(AppDimens.HeaderMetaWidth)
        )
        Spacer(Modifier.width(AppDimens.SpaceSm))
        Text(
            "RIR",
            color = AppColors.TextMuted,
            fontSize = AppTextSizes.Caption,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(AppDimens.RirWidth)
        )
        Spacer(Modifier.width(AppDimens.SpaceSm))
        Text(
            "KG",
            color = AppColors.TextMuted,
            fontSize = AppTextSizes.Caption,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(AppDimens.SpaceSm))
        Text(
            "REPS",
            color = AppColors.TextMuted,
            fontSize = AppTextSizes.Caption,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(AppDimens.SpaceSm))
        Text(
            "DESC",
            color = AppColors.TextMuted,
            fontSize = AppTextSizes.Caption,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(AppDimens.TouchMin))
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
            .clip(RoundedCornerShape(AppDimens.RadiusMd))
            .background(AppColors.CardHigh)
            .border(
                AppDimens.BorderThin,
                if (isFailure) AppColors.Orange.copy(alpha = 0.6f) else Color.Transparent,
                RoundedCornerShape(AppDimens.RadiusMd)
            )
            .clickable(onClick = { expanded = true })
            .padding(vertical = AppDimens.SpaceLg, horizontal = AppDimens.SpaceXs),
        contentAlignment = Alignment.Center
    ) {
        Text(
            rirLabel(rir),
            color = when {
                isFailure -> AppColors.Orange
                rir == null -> AppColors.TextMuted
                else -> AppColors.TextPrimary
            },
            fontSize = AppTextSizes.BodyLg,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(AppColors.Card2)
        ) {
            RIR_OPTIONS.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            rirOptionLabel(option),
                            color = if (option == rir) AppColors.Orange else AppColors.TextPrimary,
                            fontSize = AppTextSizes.BodyLg,
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
    onDelete: () -> Unit
) {
    val isFailure = rir == 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.RadiusXl))
            .background(AppColors.Bg)
            .border(
                AppDimens.BorderThin,
                if (isFailure) AppColors.Orange.copy(alpha = 0.5f) else AppColors.BorderSubtle,
                RoundedCornerShape(AppDimens.RadiusXl)
            )
            .padding(AppDimens.SpaceMd)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(AppDimens.SetBadge)
                    .clip(RoundedCornerShape(AppDimens.RadiusPill))
                    .background(if (isFailure) AppColors.Orange else AppColors.CardHigh),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$setNumber",
                    color = if (isFailure) AppColors.OnOrange else AppColors.TextPrimary,
                    fontSize = AppTextSizes.Body,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(AppDimens.SpaceSm))
            RirCell(
                rir = rir,
                onRirChange = onRirChange,
                modifier = Modifier.width(AppDimens.RirWidth)
            )
            Spacer(Modifier.width(AppDimens.SpaceSm))
            CellInput(
                value = weightText,
                onValueChange = onWeightChange,
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(AppDimens.SpaceSm))
            CellInput(
                value = repsText,
                onValueChange = onRepsChange,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(AppDimens.SpaceSm))
            CellInput(
                value = restText,
                onValueChange = onRestChange,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(AppDimens.TouchMin)
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Eliminar serie", tint = AppColors.TextMuted)
            }
        }
    }
}

/**
 * Nota efectiva del ejercicio: la propia (`RoutineExercise.note`); si está
 * vacía se adopta la primera nota no vacía de sus series (notas viejas por
 * serie). Al guardar se espeja a todas (`onExerciseNoteChange`).
 */
private fun effectiveExerciseNote(item: RoutineExerciseUi): String {
    if (item.routineExercise.note.isNotBlank()) return item.routineExercise.note
    return item.sets.firstNotNullOfOrNull { it.loadNote?.ifBlank { null } }.orEmpty()
}

/**
 * Campo único de nota por ejercicio (antes uno por serie). Trae a la vista
 * el campo al enfocarlo, para que el teclado no lo tape.
 * Borrador local: el texto vive en el campo mientras se edita y solo se
 * persiste al perder el foco o pulsar Done (antes cada letra era una
 * escritura Room + recomposición desde el flow, lo que rompía el tecleo).
 */
@Composable
private fun ExerciseNoteField(
    note: String,
    onNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val bringIntoView = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    var draft by remember(note) { mutableStateOf(note) }
    BasicTextField(
        value = draft,
        onValueChange = { draft = it },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = { if (draft != note) onNoteChange(draft) }
        ),
        textStyle = TextStyle(color = AppColors.TextPrimary, fontSize = AppTextSizes.Body),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.RadiusMd))
            .background(AppColors.CardHigh)
            .bringIntoViewRequester(bringIntoView)
            .onFocusChanged {
                if (it.isFocused) {
                    scope.launch { bringIntoView.bringIntoView() }
                } else if (draft != note) {
                    onNoteChange(draft)
                }
            }
            .padding(vertical = AppDimens.SpaceMd, horizontal = AppDimens.SpaceLg),
        decorationBox = { inner ->
            Box {
                if (draft.isEmpty()) {
                    Text("Nota del ejercicio (ej. goma amarilla)", color = AppColors.TextMuted, fontSize = AppTextSizes.Body)
                }
                inner()
            }
        }
    )
}

@Composable
private fun CellInput(
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier
) {
    val bringIntoView = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    // Borrador local (igual que la nota): el texto vive en el campo y solo
    // se vuelca al perder el foco, para no escribir en Room por cada letra.
    // En blanco se revierte al valor guardado (nunca es válido y el parser
    // lo rechazaría, dejando el campo y la DB divergentes).
    var draft by remember(value) { mutableStateOf(value) }
    BasicTextField(
        value = draft,
        onValueChange = { draft = it },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = TextStyle(
            color = AppColors.TextPrimary,
            fontSize = AppTextSizes.BodyLg,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        ),
        modifier = modifier
            .clip(RoundedCornerShape(AppDimens.RadiusMd))
            .background(AppColors.CardHigh)
            .bringIntoViewRequester(bringIntoView)
            .onFocusChanged {
                if (it.isFocused) {
                    scope.launch { bringIntoView.bringIntoView() }
                } else if (draft != value) {
                    if (draft.isBlank()) draft = value
                    else onValueChange(draft)
                }
            }
            .padding(vertical = AppDimens.SpaceLg, horizontal = AppDimens.SpaceXs),
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
            .clip(RoundedCornerShape(AppDimens.RadiusCard))
            .background(AppColors.Card)
            .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusCard))
            .clickable(onClick = onToggleExpanded)
            .padding(AppDimens.SpaceXl),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(AppDimens.SetNumber)
                .clip(RoundedCornerShape(AppDimens.RadiusSm))
                .background(AppColors.CardHigh),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${index + 1}",
                color = AppColors.TextPrimary,
                fontSize = AppTextSizes.Body,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(AppDimens.SpaceLg))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.exercise.name,
                color = AppColors.TextPrimary,
                fontSize = AppTextSizes.Title,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // Spec 010: el nombre abre la ficha (el resto expande).
                modifier = Modifier.clickable(onClick = { onExerciseClick(item.exercise.id) })
            )
            Spacer(Modifier.height(AppDimens.SpaceXxs))
            Text(summary, color = AppColors.TextMuted, fontSize = AppTextSizes.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            modifier = Modifier
                .size(AppDimens.TouchMin)
                .clickable(onClick = onDeleteExercise),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Delete, contentDescription = "Eliminar ejercicio", tint = AppColors.TextMuted)
        }
        Box(
            modifier = Modifier.size(AppDimens.TouchMin),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Expandir", tint = AppColors.TextMuted)
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
            .padding(horizontal = AppDimens.ScreenHorizontal)
            .padding(bottom = AppDimens.SpaceHuge)
            .clip(RoundedCornerShape(AppDimens.RadiusCard))
            .background(AppColors.Card)
            .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusCard))
            .padding(AppDimens.SpaceXl)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.SpaceXs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$exerciseCount ejercicios • $totalSets series totales",
                color = AppColors.TextMuted,
                fontSize = AppTextSizes.Body
            )
            Text("~$durationMin min", color = AppColors.Volt, fontSize = AppTextSizes.Body, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(AppDimens.SpaceLg))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppDimens.RadiusXl))
                .background(AppColors.Orange)
                .clickable(onClick = onSave)
                .padding(vertical = AppDimens.SpaceXxl)
                .testTag("routineEditSave"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = AppColors.OnOrange,
                modifier = Modifier.size(AppDimens.IconXl)
            )
            Spacer(Modifier.width(AppDimens.SpaceMd))
            Text(
                if (isSaving) "Guardando…" else "Finalizar y Guardar Rutina",
                color = AppColors.OnOrange,
                fontSize = AppTextSizes.TitleSm,
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
