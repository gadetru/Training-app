package com.mytrainingplan.app.feature.workout

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.mytrainingplan.app.ui.theme.AppColors
import com.mytrainingplan.app.ui.theme.AppDimens
import com.mytrainingplan.app.ui.theme.AppTextSizes
import androidx.hilt.navigation.compose.hiltViewModel
import com.mytrainingplan.app.domain.model.Exercise
import com.mytrainingplan.app.domain.model.PlannedSet
import com.mytrainingplan.app.domain.model.RoutineExercise
import com.mytrainingplan.app.domain.model.SetEntry
import com.mytrainingplan.app.domain.model.WorkoutExerciseUi
import com.mytrainingplan.app.domain.model.WorkoutUiState
import kotlinx.coroutines.delay

// Colores y medidas desde ui/theme (spec 011): sin tokens locales.

/**
 * Desviación conocida vs spec (code.html): el spec pide
 * timer/pause/flag/remove vía material-icons, pero
 * material-icons-core (única dep declarada) no los trae
 * (verificado en el sources.jar 1.7.8: 49 iconos, sin Timer ni Pause
 * ni Flag ni Remove). Fallbacks del core: Refresh (cronómetro),
 * Menu (pausar, decorativo: el texto lleva el significado),
 * Done (finalizar) y texto −/+ en los steppers.
 * TODO: si se quiere fidelidad exacta, añadir material-icons-extended
 * (propuesta en el informe del paso, requiere aprobación Gradle).
 */

private fun formatClock(totalSec: Int): String {
    val s = totalSec.coerceAtLeast(0)
    return "%02d:%02d".format(s / 60, s % 60)
}

private fun formatWeight(weightKg: Double): String =
    if (weightKg % 1.0 == 0.0) weightKg.toInt().toString() else weightKg.toString()

/**
 * Sesión en vivo (spec 005, Fase A).
 * La sesión la crea el ViewModel con `startSession(routineId)`; por eso solo
 * se pide `routineId` (sin `sessionId`: reanudar/time-travel queda para futuro
 * spec). Sin `routineId` o id inexistente: estado vacío sin crash con vuelta.
 */
@Composable
fun WorkoutScreen(
    routineId: String? = null,
    viewModel: WorkoutViewModel = hiltViewModel(),
    onFinished: () -> Unit = {},
    onDiscard: () -> Unit = {},
    onBack: () -> Unit = {},
    // Spec 010: la ficha abre desde la sesión (nombre del ejercicio).
    onExerciseClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(routineId) { viewModel.openSession(routineId) }
    val restVisible = uiState.restRemainingSec != null
    // Descanso bloqueante: consume el atrás del sistema mientras está visible.
    // Atrás sin finalizar descarta la sesión (sin fantasma); minimizar (botón)
    // mantiene la sesión viva en el repo.
    BackHandler {
        if (!restVisible) viewModel.onDiscard(onDiscard)
    }
    // Espera breve antes de declarar "no encontrada" (el startSession en
    // memoria resuelve casi al instante; el id inexistente queda vacío).
    var waited by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(1_500)
        waited = true
    }
    Box(modifier = Modifier.fillMaxSize().background(AppColors.Bg)) {
        if (uiState.exercises.isEmpty() && waited) {
            EmptySession(onBack = onBack)
        } else {
            WorkoutContent(
                uiState = uiState,
                onToggleExpanded = viewModel::onToggleExpanded,
                onUpdateEntry = viewModel::onUpdateEntry,
                onToggleDone = viewModel::onToggleDone,
                onPauseToggle = viewModel::onPauseToggle,
                onFinish = { viewModel.onFinish(onFinished) },
                onDiscard = { viewModel.onDiscard(onDiscard) },
                onBack = onBack,
                onExerciseClick = onExerciseClick
            )
        }
        if (restVisible) {
            RestOverlay(
                remainingSec = uiState.restRemainingSec ?: 0,
                totalSec = uiState.restTotalSec,
                onPlus = { viewModel.onRestAdjust(10) },
                onMinus = { viewModel.onRestAdjust(-10) },
                onFinish = viewModel::onRestFinish
            )
        }
    }
}

@Composable
fun WorkoutContent(
    uiState: WorkoutUiState,
    onToggleExpanded: (String) -> Unit = {},
    onUpdateEntry: (String, Double, Int, Int) -> Unit = { _, _, _, _ -> },
    onToggleDone: (String) -> Unit = {},
    onPauseToggle: () -> Unit = {},
    onFinish: () -> Unit = {},
    onDiscard: () -> Unit = {},
    onBack: () -> Unit = {},
    onExerciseClick: (String) -> Unit = {}
) {
    // Confirmación de descarte: overlay propio (AlertDialog gestiona insets solo).
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    // Spec 013 paso 1: qué serie se edita en el modal (id, sobrevive a rotación).
    // El objeto fresco se resuelve desde uiState en cada recomposición.
    var editingEntryId by rememberSaveable { mutableStateOf<String?>(null) }
    val editingEntry = remember(uiState, editingEntryId) {
        editingEntryId?.let { id ->
            uiState.exercises.flatMap { it.entries }.find { it.id == id }
        }
    }
    val editingPlanned = remember(uiState, editingEntry) {
        editingEntry?.let { entry ->
            uiState.exercises.flatMap { it.planned }.find { it.id == entry.plannedSetId }
        }
    }
    val editingRestSec = editingEntry?.restSeconds ?: editingPlanned?.restSeconds ?: 90
    Box(modifier = Modifier.fillMaxSize().background(AppColors.Bg).testTag("workoutRoot")) {
        Column(modifier = Modifier.fillMaxSize()) {
            SessionHeader(
                routineName = uiState.routineName,
                elapsedSec = uiState.elapsedSec,
                isPaused = uiState.isPaused,
                onBack = onBack
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = AppDimens.ScreenHorizontal)
                    // Hueco para que la barra fija no tape contenido.
                    .padding(top = AppDimens.SpaceXs, bottom = AppDimens.WorkoutBottom),
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceXl)
            ) {
                item {
                    ProgressCard(
                        current = uiState.currentExerciseIndex,
                        total = uiState.exercises.size,
                        fraction = uiState.progressFraction
                    )
                }
                itemsIndexed(
                    items = uiState.exercises,
                    key = { _, item -> item.routineExercise.id }
                ) { index, item ->
                    ExerciseAccordion(
                        index = index,
                        item = item,
                        isActive = index + 1 == uiState.currentExerciseIndex,
                        timerRunning = !uiState.isPaused,
                        onToggleExpanded = { onToggleExpanded(item.routineExercise.id) },
                        onToggleDone = onToggleDone,
                        onExerciseClick = onExerciseClick,
                        onEditClick = { entryId -> editingEntryId = entryId }
                    )
                }
                item {
                    DiscardButton(onClick = { showDiscardDialog = true })
                }
            }
        }
        SessionBar(
            isPaused = uiState.isPaused,
            elapsedSec = uiState.elapsedSec,
            isSaving = uiState.isSaving,
            onPauseToggle = onPauseToggle,
            onFinish = onFinish,
            onBack = onBack,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        if (showDiscardDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                containerColor = AppColors.Card,
                shape = RoundedCornerShape(AppDimens.RadiusCard),
                title = {
                    Text(
                        "¿Descartar entrenamiento?",
                        color = AppColors.TextPrimary,
                        fontSize = AppTextSizes.Headline,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "La sesión se descarta sin guardar y no contará como prefill.",
                        color = AppColors.TextMuted,
                        fontSize = AppTextSizes.TitleSm
                    )
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDiscardDialog = false },
                        shape = RoundedCornerShape(AppDimens.RadiusPill),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = AppColors.CardHigh,
                            contentColor = AppColors.TextPrimary
                        )
                    ) {
                        Text("Seguir", fontSize = AppTextSizes.Body, fontWeight = FontWeight.Bold)
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDiscardDialog = false
                            onDiscard()
                        },
                        shape = RoundedCornerShape(AppDimens.RadiusPill),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = AppColors.ErrorRed,
                            contentColor = AppColors.OnOrange
                        )
                    ) {
                        Text("Descartar", fontSize = AppTextSizes.Body, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
        // Spec 013 paso 2: guardado de golpe vía onUpdateEntry (KG con signo,
        // REPS/PAUSA); cancelar no guarda (onDismiss).
        val editing = editingEntry
        if (editing != null) {
            SetEditDialog(
                entry = editing,
                restSec = editingRestSec,
                onDismiss = { editingEntryId = null },
                onConfirm = { kg, reps, rest ->
                    onUpdateEntry(editing.id, kg, reps, rest)
                    editingEntryId = null
                }
            )
        }
    }
}

@Composable
private fun EmptySession(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = AppDimens.ScreenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Sesión no encontrada",
            color = AppColors.TextPrimary,
            fontSize = AppTextSizes.DisplaySm,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(AppDimens.SpaceMd))
        Text(
            "La rutina no existe o el id es inválido.",
            color = AppColors.TextMuted,
            fontSize = AppTextSizes.TitleSm,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(AppDimens.SpaceHuge))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(AppDimens.RadiusPill))
                .background(AppColors.Orange)
                .clickable(onClick = onBack)
                .padding(horizontal = AppDimens.ScreenHorizontal, vertical = AppDimens.SpaceXl)
        ) {
            Text("Volver", color = AppColors.OnOrange, fontSize = AppTextSizes.TitleSm, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SessionHeader(
    routineName: String,
    elapsedSec: Int,
    isPaused: Boolean,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Bg)
            .statusBarsPadding()
            .padding(horizontal = AppDimens.ScreenHorizontal, vertical = AppDimens.SpaceXl),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceLg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Minimizar/volver (mantiene la sesión viva en el repo).
            Box(
                modifier = Modifier
                    .size(AppDimens.TouchMin)
                    .clip(RoundedCornerShape(AppDimens.RadiusPill))
                    .background(AppColors.Card)
                    .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusPill))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Minimizar", tint = AppColors.TextPrimary)
            }
            // Cronómetro de sesión activo.
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(AppDimens.RadiusPill))
                    .background(AppColors.Card)
                    .border(AppDimens.BorderThin, AppColors.Orange.copy(alpha = 0.3f), RoundedCornerShape(AppDimens.RadiusPill))
                    .padding(horizontal = AppDimens.SpaceXxl, vertical = AppDimens.SpaceMd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(AppDimens.StatusDot)
                        .clip(CircleShape)
                        .background(if (isPaused) AppColors.TextMuted else AppColors.Orange)
                )
                Spacer(Modifier.width(AppDimens.SpaceMd))
                // Timer no está en material-icons-core → fallback Refresh.
                Icon(Icons.Filled.Refresh, contentDescription = null, tint = AppColors.Orange, modifier = Modifier.size(AppDimens.IconInline))
                Spacer(Modifier.width(AppDimens.SpaceSm))
                Text(
                    formatClock(elapsedSec),
                    color = AppColors.TextPrimary,
                    fontSize = AppTextSizes.TitleSm,
                    fontWeight = FontWeight.Bold
                )
            }
            // Ajustes (TODO visual sin crash).
            Box(
                modifier = Modifier
                    .size(AppDimens.TouchMin)
                    .clip(RoundedCornerShape(AppDimens.RadiusPill))
                    .background(AppColors.Card)
                    .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusPill))
                    .clickable(onClick = {}),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Ajustes", tint = AppColors.TextPrimary)
            }
        }
        Column {
            // Spec 013 paso 5: coherente con SessionBar por construcción —
            // EN PAUSA justo cuando la barra ofrece Comenzar/Reanudar,
            // ACTIVO justo cuando ofrece Pausar (misma fuente: uiState).
            Text(
                if (isPaused) "ENTRENAMIENTO EN PAUSA" else "ENTRENAMIENTO ACTIVO",
                color = AppColors.Orange,
                fontSize = AppTextSizes.Caption,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (routineName.isBlank()) "Sesión" else routineName,
                color = AppColors.TextPrimary,
                fontSize = AppTextSizes.Display,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProgressCard(
    current: Int,
    total: Int,
    fraction: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.RadiusCard))
            .background(AppColors.Card)
            .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusCard))
            .padding(AppDimens.SpaceXxl),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMd)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Ejercicio $current de $total",
                color = AppColors.TextPrimary,
                fontSize = AppTextSizes.BodyLg,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(AppDimens.SpaceMd))
            Box(
                modifier = Modifier
                    .size(AppDimens.SpaceXs)
                    .clip(CircleShape)
                    .background(AppColors.BorderSubtle)
            )
            Spacer(Modifier.width(AppDimens.SpaceMd))
            Text(
                "${(fraction * 100).toInt()}% completado",
                color = AppColors.TextMuted,
                fontSize = AppTextSizes.Body
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.SpaceMd)
                .clip(RoundedCornerShape(AppDimens.RadiusPill))
                .background(AppColors.CardHigh)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(AppDimens.SpaceMd)
                    .clip(RoundedCornerShape(AppDimens.RadiusPill))
                    .background(AppColors.Orange)
            )
        }
    }
}

@Composable
private fun ExerciseAccordion(
    index: Int,
    item: WorkoutExerciseUi,
    isActive: Boolean,
    timerRunning: Boolean = true,
    onToggleExpanded: () -> Unit,
    onToggleDone: (String) -> Unit,
    onExerciseClick: (String) -> Unit = {},
    onEditClick: (String) -> Unit = {}
) {
    val doneCount = item.entries.count { it.done }
    val total = item.entries.size
    val completed = total > 0 && doneCount == total
    if (item.expanded) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppDimens.RadiusCard))
                .background(AppColors.Card)
                .border(
                    AppDimens.BorderThin,
                    if (isActive && !completed) AppColors.Orange.copy(alpha = 0.4f) else AppColors.BorderSubtle,
                    RoundedCornerShape(AppDimens.RadiusCard)
                )
        ) {
            ExerciseHeader(
                index = index,
                name = item.exercise.name,
                subtitle = if (completed) {
                    val maxW = item.entries.maxOfOrNull { it.weightKg } ?: 0.0
                    "$doneCount de $total series completadas • ${formatWeight(maxW)} kg máx"
                } else {
                    "$doneCount de $total series • ${item.entries.firstOrNull()?.let { "${formatWeight(it.weightKg)} kg" } ?: ""}"
                },
                completed = completed,
                expanded = true,
                onToggleExpanded = onToggleExpanded,
                onNameClick = { onExerciseClick(item.exercise.id) }
            )
            Column(modifier = Modifier.padding(AppDimens.SpaceXl)) {
                SetsHeaderRow()
                Spacer(Modifier.height(AppDimens.SpaceSm))
                // Se calcula una vez por composición (no por fila): idéntico
                // resultado que el filter+minBy por fila, sin coste O(n²).
                val nextPendingId = item.entries
                    .filter { !it.done }
                    .minByOrNull { it.setNumber }?.id
                item.entries.forEach { entry ->
                    val planned = item.planned.find { it.id == entry.plannedSetId }
                        ?: item.planned.find { it.setNumber == entry.setNumber }
                    SetRow(
                        entry = entry,
                        planned = planned,
                        isNext = !entry.done && entry.id == nextPendingId,
                        timerRunning = timerRunning,
                        onToggleDone = { onToggleDone(entry.id) },
                        onEditClick = { onEditClick(entry.id) }
                    )
                    Spacer(Modifier.height(AppDimens.SpaceMd))
                }
            }
        }
    } else {
        // Spec 013 paso 4: un solo clickable por tarjeta (1 tap expande);
        // el nombre lleva su propio tap a la ficha y consume el evento
        // (abre el detalle sin expandir a la vez).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppDimens.RadiusCard))
                .background(AppColors.Card)
                .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusCard))
                .clickable(onClick = onToggleExpanded)
                .testTag("exerciseCard:${item.routineExercise.id}")
                .padding(AppDimens.SpaceXl),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (completed) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = AppColors.Volt,
                    modifier = Modifier.size(AppDimens.SetCell)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(AppDimens.SetCell)
                        .clip(RoundedCornerShape(AppDimens.RadiusLg))
                        .background(AppColors.CardHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${index + 1}",
                        color = AppColors.TextPrimary,
                        fontSize = AppTextSizes.TitleSm,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(AppDimens.SpaceLg))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${index + 1}. ${item.exercise.name}",
                    color = AppColors.TextPrimary,
                    fontSize = AppTextSizes.Title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // Spec 010: el nombre abre la ficha (el resto expande).
                    modifier = Modifier.clickable(onClick = { onExerciseClick(item.exercise.id) })
                )
                Spacer(Modifier.height(AppDimens.SpaceXxs))
                Text(
                    if (completed) {
                        val maxW = item.entries.maxOfOrNull { it.weightKg } ?: 0.0
                        "$doneCount de $total series completadas • ${formatWeight(maxW)} kg máx"
                    } else {
                        "$total series programadas"
                    },
                    color = AppColors.TextMuted,
                    fontSize = AppTextSizes.Body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier.size(AppDimens.TouchMin),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Expandir", tint = AppColors.TextMuted)
            }
        }
    }
}

@Composable
private fun ExerciseHeader(
    index: Int,
    name: String,
    subtitle: String,
    completed: Boolean,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onNameClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.CardHigh.copy(alpha = 0.4f))
            .padding(AppDimens.SpaceXl),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(AppDimens.SetBadge)
                .clip(RoundedCornerShape(AppDimens.RadiusMd))
                .background(if (completed) AppColors.Volt.copy(alpha = 0.2f) else AppColors.Orange),
            contentAlignment = Alignment.Center
        ) {
            if (completed) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = AppColors.Volt, modifier = Modifier.size(AppDimens.IconInline))
            } else {
                Text("${index + 1}", color = AppColors.OnOrange, fontSize = AppTextSizes.BodyLg, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(AppDimens.SpaceLg))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${index + 1}. $name",
                color = AppColors.TextPrimary,
                fontSize = AppTextSizes.Title,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                // Spec 010: el nombre abre la ficha.
                modifier = Modifier.clickable(onClick = onNameClick)
            )
            Spacer(Modifier.height(AppDimens.SpaceXxs))
            Text(subtitle, color = AppColors.TextMuted, fontSize = AppTextSizes.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            modifier = Modifier
                .size(AppDimens.TouchMin)
                .clickable(onClick = onToggleExpanded),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "Colapsar" else "Expandir",
                tint = AppColors.TextMuted
            )
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
        Text("SET", color = AppColors.TextMuted, fontSize = AppTextSizes.Caption, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(AppDimens.SetNumWidth))
        Text("OBJETIVO", color = AppColors.TextMuted, fontSize = AppTextSizes.Caption, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text("KG", color = AppColors.TextMuted, fontSize = AppTextSizes.Caption, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        Text("REPS", color = AppColors.TextMuted, fontSize = AppTextSizes.Caption, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        Text("PAUSA", color = AppColors.TextMuted, fontSize = AppTextSizes.Caption, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.width(AppDimens.PauseWidth))
        Spacer(Modifier.width(AppDimens.TouchMin))
    }
}

@Composable
private fun SetRow(
    entry: SetEntry,
    planned: PlannedSet?,
    isNext: Boolean,
    timerRunning: Boolean = true,
    onToggleDone: () -> Unit,
    onEditClick: () -> Unit
) {
    val restSec = entry.restSeconds ?: planned?.restSeconds ?: 90
    val objective = if (planned != null) {
        "${formatWeight(planned.weightKg)} kg x ${planned.targetReps}"
    } else {
        "${formatWeight(entry.weightKg)} kg x ${entry.reps}"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.RadiusXl))
            .background(
                if (!entry.done && isNext) AppColors.CardHigh.copy(alpha = 0.9f)
                else AppColors.InputBg
            )
            .border(
                AppDimens.BorderThin,
                if (entry.done) AppColors.Volt.copy(alpha = 0.35f) else AppColors.BorderSubtle,
                RoundedCornerShape(AppDimens.RadiusXl)
            )
            .padding(AppDimens.SpaceMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Nº de serie.
        Box(
            modifier = Modifier
                .size(AppDimens.SetBadge)
                .clip(RoundedCornerShape(AppDimens.RadiusPill))
                .background(
                    when {
                        entry.done -> AppColors.Volt.copy(alpha = 0.2f)
                        isNext -> AppColors.Orange
                        else -> AppColors.CardHigh
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (entry.done) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = AppColors.Volt, modifier = Modifier.size(AppDimens.IconMd))
            } else {
                Text(
                    "${entry.setNumber}",
                    color = if (isNext) AppColors.OnOrange else AppColors.TextPrimary,
                    fontSize = AppTextSizes.Body,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.width(AppDimens.SpaceSm))
        // Objetivo planificado.
        Text(
            objective,
            color = AppColors.TextMuted,
            fontSize = AppTextSizes.Small,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        // Spec 013: celdas de valor pulsables (abren el modal). Sin steppers.
        ValueCell(
            value = formatWeight(entry.weightKg),
            contentDescription = "Editar peso serie ${entry.setNumber}",
            onClick = onEditClick,
            modifier = Modifier
                .weight(1f)
                .testTag("setKg:${entry.id}")
        )
        Spacer(Modifier.width(AppDimens.SpaceXs))
        ValueCell(
            value = "${entry.reps}",
            contentDescription = "Editar repeticiones serie ${entry.setNumber}",
            onClick = onEditClick,
            modifier = Modifier
                .weight(1f)
                .testTag("setReps:${entry.id}")
        )
        Spacer(Modifier.width(AppDimens.SpaceXs))
        ValueCell(
            value = "${restSec}s",
            contentDescription = "Editar pausa serie ${entry.setNumber}",
            small = true,
            onClick = onEditClick,
            modifier = Modifier
                .width(AppDimens.PauseWidth)
                .testTag("setRest:${entry.id}")
        )
        // Estado: pendiente → botón naranja `done` (atenuado si el
        // cronómetro está parado: marcar exige contador activo); hecha →
        // check volt (desmarcar siempre permitido).
        Box(
            modifier = Modifier
                .size(AppDimens.TouchMin)
                .clickable(enabled = entry.done || timerRunning, onClick = onToggleDone),
            contentAlignment = Alignment.Center
        ) {
            if (entry.done) {
                Box(
                    modifier = Modifier
                        .size(AppDimens.SetCell)
                        .clip(RoundedCornerShape(AppDimens.RadiusLg))
                        .background(AppColors.Volt.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "Desmarcar serie", tint = AppColors.Volt, modifier = Modifier.size(AppDimens.IconXl))
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(AppDimens.SetCell)
                        .clip(RoundedCornerShape(AppDimens.RadiusLg))
                        .background(
                            if (timerRunning) AppColors.Orange
                            else AppColors.TextMuted.copy(alpha = 0.4f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Done, contentDescription = "Completar serie", tint = AppColors.OnOrange, modifier = Modifier.size(AppDimens.IconXl))
                }
            }
        }
    }
}

/**
 * Spec 013 paso 1: celda de valor pulsable (KG/REPS/PAUSA). Hit target
 * ≥ 48dp vía `heightIn(min = TouchMin)`.
 */
@Composable
private fun ValueCell(
    value: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    small: Boolean = false
) {
    Box(
        modifier = modifier
            .heightIn(min = AppDimens.TouchMin)
            .clip(RoundedCornerShape(AppDimens.RadiusMd))
            .background(AppColors.CardHigh)
            .clickable(onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            value,
            color = if (small) AppColors.TextMuted else AppColors.TextPrimary,
            fontSize = if (small) AppTextSizes.Small else AppTextSizes.BodyLg,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * Spec 013 paso 1: modal de edición de serie (KG con signo, REPS entero,
 * PAUSA en segundos) con teclado numérico y confirmación. `AlertDialog`
 * gestiona insets solo (regla edge-to-edge). Estado solo de pantalla;
 * el guardado lo hace el llamador en `onConfirm` (paso 2: rutas reales).
 */
@Composable
private fun SetEditDialog(
    entry: SetEntry,
    restSec: Int,
    onDismiss: () -> Unit,
    onConfirm: (kg: Double, reps: Int, rest: Int) -> Unit
) {
    var kgText by rememberSaveable(entry.id) { mutableStateOf(formatWeight(entry.weightKg)) }
    var repsText by rememberSaveable(entry.id) { mutableStateOf("${entry.reps}") }
    var restText by rememberSaveable(entry.id) { mutableStateOf("$restSec") }
    val kg = kgText.replace(',', '.').toDoubleOrNull()
    val reps = repsText.toIntOrNull()
    val rest = restText.toIntOrNull()
    val valid = kg != null && reps != null && reps >= 0 && rest != null && rest >= 0
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.Card,
        shape = RoundedCornerShape(AppDimens.RadiusCard),
        title = {
            Text(
                "Editar serie ${entry.setNumber}",
                color = AppColors.TextPrimary,
                fontSize = AppTextSizes.Headline,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceXl)) {
                SetEditField(
                    label = "KG (con signo: + lastre, - ayuda)",
                    value = kgText,
                    onValue = { kgText = it },
                    keyboardType = KeyboardType.Decimal,
                    testTag = "setEditKg"
                )
                SetEditField(
                    label = "REPS",
                    value = repsText,
                    onValue = { repsText = it },
                    keyboardType = KeyboardType.Number,
                    testTag = "setEditReps"
                )
                SetEditField(
                    label = "PAUSA (segundos)",
                    value = restText,
                    onValue = { restText = it },
                    keyboardType = KeyboardType.Number,
                    testTag = "setEditRest"
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(AppDimens.RadiusPill),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = AppColors.CardHigh,
                    contentColor = AppColors.TextPrimary
                )
            ) {
                Text("Cancelar", fontSize = AppTextSizes.Body, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (valid) onConfirm(kg!!, reps!!, rest!!)
                },
                enabled = valid,
                shape = RoundedCornerShape(AppDimens.RadiusPill),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = AppColors.Orange,
                    contentColor = AppColors.OnOrange,
                    disabledContainerColor = AppColors.CardHigh,
                    disabledContentColor = AppColors.TextMuted
                )
            ) {
                Text("Guardar", fontSize = AppTextSizes.Body, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun SetEditField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    keyboardType: KeyboardType,
    testTag: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSm)) {
        Text(
            label,
            color = AppColors.TextMuted,
            fontSize = AppTextSizes.Caption,
            fontWeight = FontWeight.SemiBold
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValue,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = AppColors.TextPrimary,
                fontSize = AppTextSizes.TitleSm,
                fontWeight = FontWeight.Bold
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AppColors.InputBg,
                unfocusedContainerColor = AppColors.InputBg,
                focusedBorderColor = AppColors.Orange,
                unfocusedBorderColor = AppColors.BorderSubtle,
                cursorColor = AppColors.Orange
            ),
            shape = RoundedCornerShape(AppDimens.RadiusMd),
            modifier = Modifier.fillMaxWidth().testTag(testTag)
        )
    }
}

@Composable
private fun DiscardButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(AppDimens.RadiusXl))
                .clickable(onClick = onClick)
                .padding(horizontal = AppDimens.SpaceHuge, vertical = AppDimens.SpaceXl),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Close, contentDescription = null, tint = AppColors.ErrorRed, modifier = Modifier.size(AppDimens.IconInline))
            Spacer(Modifier.width(AppDimens.SpaceSm))
            Text("Descartar Entrenamiento", color = AppColors.ErrorRed, fontSize = AppTextSizes.BodyLg, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SessionBar(
    isPaused: Boolean,
    isSaving: Boolean,
    onPauseToggle: () -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    // Spec 013 paso 3: 0 y en pausa = aún no empezado → "Comenzar".
    elapsedSec: Int = 0
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = AppDimens.ScreenHorizontal)
            .padding(bottom = AppDimens.SpaceHuge)
            .clip(RoundedCornerShape(AppDimens.RadiusDock))
            .background(AppColors.Card)
            .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusDock))
            .padding(AppDimens.SpaceLg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceMd)
    ) {
        // Spec 013 paso 3: Comenzar (sin arrancar) → Pausar → Reanudar.
        // Sin arrancar = 0 y en pausa; el tick solo avanza sin pausa.
        val notStarted = isPaused && elapsedSec <= 0
        val pauseLabel = if (notStarted) "Comenzar" else if (isPaused) "Reanudar" else "Pausar"
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(AppDimens.RadiusPill))
                .background(AppColors.CardHigh)
                .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusPill))
                .clickable(onClick = onPauseToggle)
                .padding(horizontal = AppDimens.SpaceHuge, vertical = AppDimens.SpaceXl)
                .testTag("workoutPauseToggle"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pause no está en material-icons-core → fallback Menu.
            Icon(
                if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Menu,
                contentDescription = pauseLabel,
                tint = AppColors.TextPrimary,
                modifier = Modifier.size(AppDimens.IconXl)
            )
            Spacer(Modifier.width(AppDimens.SpaceSm))
            Text(
                pauseLabel,
                color = AppColors.TextPrimary,
                fontSize = AppTextSizes.Body,
                fontWeight = FontWeight.Bold
            )
        }
        // Finalizar y Guardar (CTA principal).
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(AppDimens.RadiusPill))
                .background(AppColors.Orange)
                .clickable(onClick = onFinish)
                .padding(vertical = AppDimens.SpaceXl)
                .testTag("workoutFinish"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flag no está en material-icons-core → fallback Done.
            Icon(Icons.Filled.Done, contentDescription = null, tint = AppColors.OnOrange, modifier = Modifier.size(AppDimens.IconXl))
            Spacer(Modifier.width(AppDimens.SpaceSm))
            Text(
                if (isSaving) "Guardando…" else "Finalizar y Guardar",
                color = AppColors.OnOrange,
                fontSize = AppTextSizes.BodyLg,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // Minimizar (TODO visual: vuelve atrás manteniendo la sesión viva).
        Box(
            modifier = Modifier
                .size(AppDimens.TouchMin)
                .clip(RoundedCornerShape(AppDimens.RadiusPill))
                .background(AppColors.CardHigh)
                .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusPill))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Minimizar sesión", tint = AppColors.TextMuted)
        }
    }
}

/**
 * Overlay de descanso flotante y bloqueante (plantilla-contador-descanso):
 * no se puede quitar por toque fuera (el scrim consume los taps) ni por
 * atrás (BackHandler consumido en [WorkoutScreen]); solo al llegar a 0 o
 * pulsar `Terminar descanso`. Hit targets >= 48dp.
 * Pública (no private) para poder cubrirla con smoke en emulador sin Hilt.
 */
@Composable
fun RestOverlay(
    remainingSec: Int,
    totalSec: Int,
    onPlus: () -> Unit,
    onMinus: () -> Unit,
    onFinish: () -> Unit
) {
    val fraction = if (totalSec <= 0) 0f else remainingSec.toFloat() / totalSec.toFloat()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Scrim)
            .clickable(onClick = {})
            .testTag("restOverlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.RestHorizontal)
                .clip(RoundedCornerShape(AppDimens.RadiusDock))
                .background(AppColors.Card)
                .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusDock))
                .padding(AppDimens.DialogPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "DESCANSO ACTIVO",
                color = AppColors.Volt,
                fontSize = AppTextSizes.Small,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(AppDimens.SpaceMd))
            Text(
                formatClock(remainingSec),
                color = AppColors.TextPrimary,
                fontSize = AppTextSizes.RestClock,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(AppDimens.SpaceXl))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppDimens.SpaceMd)
                    .clip(RoundedCornerShape(AppDimens.RadiusPill))
                    .background(AppColors.CardHigh)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(AppDimens.SpaceMd)
                        .clip(RoundedCornerShape(AppDimens.RadiusPill))
                        .background(AppColors.Volt)
                )
            }
            Spacer(Modifier.height(AppDimens.SpaceHuge))
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceXl),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(AppDimens.RadiusPill))
                        .background(AppColors.CardHigh)
                        .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusPill))
                        .clickable(onClick = onMinus)
                        .padding(horizontal = AppDimens.ScreenHorizontal, vertical = AppDimens.SpaceXxl),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("-10s", color = AppColors.TextPrimary, fontSize = AppTextSizes.TitleSm, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(AppDimens.RadiusPill))
                        .background(AppColors.CardHigh)
                        .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusPill))
                        .clickable(onClick = onPlus)
                        .padding(horizontal = AppDimens.ScreenHorizontal, vertical = AppDimens.SpaceXxl),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("+10s", color = AppColors.TextPrimary, fontSize = AppTextSizes.TitleSm, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(AppDimens.SpaceHuge))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppDimens.RadiusPill))
                    .background(AppColors.Orange)
                    .clickable(onClick = onFinish)
                    .padding(vertical = AppDimens.SpaceXxl),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Done, contentDescription = null, tint = AppColors.OnOrange, modifier = Modifier.size(AppDimens.IconXl))
                Spacer(Modifier.width(AppDimens.SpaceMd))
                Text("Terminar descanso", color = AppColors.OnOrange, fontSize = AppTextSizes.TitleSm, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111316)
@Composable
private fun WorkoutContentPreview() {
    val press = Exercise(
        id = "pectorals/barbell-bench-press",
        slug = "barbell-bench-press",
        name = "Press banca",
        muscle = "pectorals",
        bodyPart = "chest",
        equipment = "barbell",
        category = "strength"
    )
    val militar = Exercise(
        id = "delts/barbell-military-press",
        slug = "barbell-military-press",
        name = "Press militar con barra",
        muscle = "delts",
        bodyPart = "shoulders",
        equipment = "barbell",
        category = "strength"
    )
    androidx.compose.material3.MaterialTheme {
        WorkoutContent(
            uiState = WorkoutUiState(
                sessionId = "preview",
                routineName = "Torso - Fuerza & Hipertrofia",
                elapsedSec = 1725,
                isPaused = false,
                exercises = listOf(
                    WorkoutExerciseUi(
                        routineExercise = RoutineExercise(id = "re1", routineId = "r1", exerciseId = press.id),
                        exercise = press,
                        entries = listOf(
                            SetEntry(id = "e1", sessionId = "preview", exerciseId = press.id, plannedSetId = "p1", setNumber = 1, reps = 12, weightKg = 40.0, restSeconds = 60, done = true),
                            SetEntry(id = "e2", sessionId = "preview", exerciseId = press.id, plannedSetId = "p2", setNumber = 2, reps = 10, weightKg = 60.0, restSeconds = 90, done = true)
                        ),
                        planned = listOf(
                            PlannedSet(id = "p1", routineExerciseId = "re1", setNumber = 1, targetReps = 12, weightKg = 40.0, restSeconds = 60),
                            PlannedSet(id = "p2", routineExerciseId = "re1", setNumber = 2, targetReps = 10, weightKg = 60.0, restSeconds = 90)
                        ),
                        expanded = false
                    ),
                    WorkoutExerciseUi(
                        routineExercise = RoutineExercise(id = "re2", routineId = "r1", exerciseId = militar.id),
                        exercise = militar,
                        entries = listOf(
                            SetEntry(id = "e3", sessionId = "preview", exerciseId = militar.id, plannedSetId = "p3", setNumber = 1, reps = 12, weightKg = 40.0, restSeconds = 60, done = true),
                            SetEntry(id = "e4", sessionId = "preview", exerciseId = militar.id, plannedSetId = "p4", setNumber = 2, reps = 8, weightKg = 70.0, restSeconds = 90, done = false)
                        ),
                        planned = listOf(
                            PlannedSet(id = "p3", routineExerciseId = "re2", setNumber = 1, targetReps = 12, weightKg = 40.0, restSeconds = 60),
                            PlannedSet(id = "p4", routineExerciseId = "re2", setNumber = 2, targetReps = 8, weightKg = 70.0, restSeconds = 90)
                        ),
                        expanded = true
                    )
                ),
                doneCount = 3,
                totalCount = 4
            )
        )
    }
}
