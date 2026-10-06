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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mytrainingplan.app.domain.model.Exercise
import com.mytrainingplan.app.domain.model.PlannedSet
import com.mytrainingplan.app.domain.model.RoutineExercise
import com.mytrainingplan.app.domain.model.SetEntry
import com.mytrainingplan.app.domain.model.WorkoutExerciseUi
import com.mytrainingplan.app.domain.model.WorkoutUiState
import kotlinx.coroutines.delay

// Tokens references/plantilla-rutina/DESIGN.md + patrón RoutineEditScreen (004).
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
private val ErrorRed = Color(0xFFFF8A80)

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
    Box(modifier = Modifier.fillMaxSize().background(Bg)) {
        if (uiState.exercises.isEmpty() && waited) {
            EmptySession(onBack = onBack)
        } else {
            WorkoutContent(
                uiState = uiState,
                onToggleExpanded = viewModel::onToggleExpanded,
                onKgChange = viewModel::onKgChange,
                onRepsChange = viewModel::onRepsChange,
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
    onKgChange: (String, Double) -> Unit = { _, _ -> },
    onRepsChange: (String, Int) -> Unit = { _, _ -> },
    onToggleDone: (String) -> Unit = {},
    onPauseToggle: () -> Unit = {},
    onFinish: () -> Unit = {},
    onDiscard: () -> Unit = {},
    onBack: () -> Unit = {},
    onExerciseClick: (String) -> Unit = {}
) {
    // Confirmación de descarte: overlay propio (AlertDialog gestiona insets solo).
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize().background(Bg)) {
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
                    .padding(horizontal = 20.dp)
                    // Hueco para que la barra fija no tape contenido.
                    .padding(top = 4.dp, bottom = 170.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        onToggleExpanded = { onToggleExpanded(item.routineExercise.id) },
                        onKgChange = onKgChange,
                        onRepsChange = onRepsChange,
                        onToggleDone = onToggleDone,
                        onExerciseClick = onExerciseClick
                    )
                }
                item {
                    DiscardButton(onClick = { showDiscardDialog = true })
                }
            }
        }
        SessionBar(
            isPaused = uiState.isPaused,
            isSaving = uiState.isSaving,
            onPauseToggle = onPauseToggle,
            onFinish = onFinish,
            onBack = onBack,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        if (showDiscardDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                containerColor = Card,
                shape = RoundedCornerShape(16.dp),
                title = {
                    Text(
                        "¿Descartar entrenamiento?",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "La sesión se descarta sin guardar y no contará como prefill.",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDiscardDialog = false },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = CardHigh,
                            contentColor = TextPrimary
                        )
                    ) {
                        Text("Seguir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDiscardDialog = false
                            onDiscard()
                        },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = ErrorRed,
                            contentColor = Color(0xFF1A0A00)
                        )
                    ) {
                        Text("Descartar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
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
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Sesión no encontrada",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "La rutina no existe o el id es inválido.",
            color = TextMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Orange)
                .clickable(onClick = onBack)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text("Volver", color = OnOrange, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
            .background(Bg)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Minimizar/volver (mantiene la sesión viva en el repo).
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Card)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(999.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Minimizar", tint = TextPrimary)
            }
            // Cronómetro de sesión activo.
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Card)
                    .border(1.dp, Orange.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isPaused) TextMuted else Orange)
                )
                Spacer(Modifier.width(8.dp))
                // Timer no está en material-icons-core → fallback Refresh.
                Icon(Icons.Filled.Refresh, contentDescription = null, tint = Orange, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    formatClock(elapsedSec),
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            // Ajustes (TODO visual sin crash).
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Card)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(999.dp))
                    .clickable(onClick = {}),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Ajustes", tint = TextPrimary)
            }
        }
        Column {
            Text(
                if (isPaused) "ENTRENAMIENTO EN PAUSA" else "ENTRENAMIENTO ACTIVO",
                color = Orange,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (routineName.isBlank()) "Sesión" else routineName,
                color = TextPrimary,
                fontSize = 22.sp,
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
            .clip(RoundedCornerShape(16.dp))
            .background(Card)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Ejercicio $current de $total",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(BorderSubtle)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "${(fraction * 100).toInt()}% completado",
                color = TextMuted,
                fontSize = 12.sp
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(CardHigh)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Orange)
            )
        }
    }
}

@Composable
private fun ExerciseAccordion(
    index: Int,
    item: WorkoutExerciseUi,
    isActive: Boolean,
    onToggleExpanded: () -> Unit,
    onKgChange: (String, Double) -> Unit,
    onRepsChange: (String, Int) -> Unit,
    onToggleDone: (String) -> Unit,
    onExerciseClick: (String) -> Unit = {}
) {
    val doneCount = item.entries.count { it.done }
    val total = item.entries.size
    val completed = total > 0 && doneCount == total
    if (item.expanded) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Card)
                .border(
                    1.dp,
                    if (isActive && !completed) Orange.copy(alpha = 0.4f) else BorderSubtle,
                    RoundedCornerShape(16.dp)
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
            Column(modifier = Modifier.padding(12.dp)) {
                SetsHeaderRow()
                Spacer(Modifier.height(6.dp))
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
                        onKgChange = { onKgChange(entry.id, it) },
                        onRepsChange = { onRepsChange(entry.id, it) },
                        onToggleDone = { onToggleDone(entry.id) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    } else {
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
            if (completed) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Volt,
                    modifier = Modifier.size(36.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${index + 1}",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${index + 1}. ${item.exercise.name}",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // Spec 010: el nombre abre la ficha (el resto expande).
                    modifier = Modifier.clickable(onClick = { onExerciseClick(item.exercise.id) })
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (completed) {
                        val maxW = item.entries.maxOfOrNull { it.weightKg } ?: 0.0
                        "$doneCount de $total series completadas • ${formatWeight(maxW)} kg máx"
                    } else {
                        "$total series programadas"
                    },
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Expandir", tint = TextMuted)
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
            .background(CardHigh.copy(alpha = 0.4f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (completed) Volt.copy(alpha = 0.2f) else Orange),
            contentAlignment = Alignment.Center
        ) {
            if (completed) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Volt, modifier = Modifier.size(18.dp))
            } else {
                Text("${index + 1}", color = OnOrange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${index + 1}. $name",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                // Spec 010: el nombre abre la ficha.
                modifier = Modifier.clickable(onClick = onNameClick)
            )
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onToggleExpanded),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "Colapsar" else "Expandir",
                tint = TextMuted
            )
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
        Text("SET", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(32.dp))
        Text("OBJETIVO", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text("KG", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        Text("REPS", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        Text("PAUSA", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.width(44.dp))
        Spacer(Modifier.width(48.dp))
    }
}

@Composable
private fun SetRow(
    entry: SetEntry,
    planned: PlannedSet?,
    isNext: Boolean,
    onKgChange: (Double) -> Unit,
    onRepsChange: (Int) -> Unit,
    onToggleDone: () -> Unit
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
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (!entry.done && isNext) CardHigh.copy(alpha = 0.9f)
                else Color(0xFF0C0E11)
            )
            .border(
                1.dp,
                if (entry.done) Volt.copy(alpha = 0.35f) else BorderSubtle,
                RoundedCornerShape(12.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Nº de serie.
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(
                    when {
                        entry.done -> Volt.copy(alpha = 0.2f)
                        isNext -> Orange
                        else -> CardHigh
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (entry.done) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Volt, modifier = Modifier.size(16.dp))
            } else {
                Text(
                    "${entry.setNumber}",
                    color = if (isNext) OnOrange else TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        // Objetivo planificado.
        Text(
            objective,
            color = TextMuted,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        // Stepper KG (±2.5).
        Stepper(
            value = formatWeight(entry.weightKg),
            onMinus = { onKgChange(-KG_STEP) },
            onPlus = { onKgChange(KG_STEP) },
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(4.dp))
        // Stepper REPS (±1).
        Stepper(
            value = "${entry.reps}",
            onMinus = { onRepsChange(-1) },
            onPlus = { onRepsChange(1) },
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "${restSec}s",
            color = TextMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(44.dp)
        )
        // Estado: pendiente → botón naranja `done`; hecha → check volt.
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onToggleDone),
            contentAlignment = Alignment.Center
        ) {
            if (entry.done) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Volt.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "Desmarcar serie", tint = Volt, modifier = Modifier.size(20.dp))
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Orange),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Done, contentDescription = "Completar serie", tint = OnOrange, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun Stepper(
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CardHigh)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Remove no está en material-icons-core → texto −/+ simétrico.
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onMinus),
            contentAlignment = Alignment.Center
        ) {
            Text("−", color = TextMuted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            value,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onPlus),
            contentAlignment = Alignment.Center
        ) {
            Text("+", color = TextMuted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
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
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Close, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Descartar Entrenamiento", color = ErrorRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Card)
            .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Pausar/Reanudar.
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(CardHigh)
                .border(1.dp, BorderSubtle, RoundedCornerShape(999.dp))
                .clickable(onClick = onPauseToggle)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pause no está en material-icons-core → fallback Menu.
            Icon(
                if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Menu,
                contentDescription = if (isPaused) "Reanudar" else "Pausar",
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if (isPaused) "Reanudar" else "Pausar",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        // Finalizar y Guardar (CTA principal).
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(999.dp))
                .background(Orange)
                .clickable(onClick = onFinish)
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flag no está en material-icons-core → fallback Done.
            Icon(Icons.Filled.Done, contentDescription = null, tint = OnOrange, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                if (isSaving) "Guardando…" else "Finalizar y Guardar",
                color = OnOrange,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // Minimizar (TODO visual: vuelve atrás manteniendo la sesión viva).
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(CardHigh)
                .border(1.dp, BorderSubtle, RoundedCornerShape(999.dp))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Minimizar sesión", tint = TextMuted)
        }
    }
}

/**
 * Overlay de descanso flotante y bloqueante (plantilla-contador-descanso):
 * no se puede quitar por toque fuera (el scrim consume los taps) ni por
 * atrás (BackHandler consumido en [WorkoutScreen]); solo al llegar a 0 o
 * pulsar `Terminar descanso`. Hit targets >= 48dp.
 */
@Composable
private fun RestOverlay(
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
            .background(Color(0xBF000000))
            .clickable(onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Card)
                .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "DESCANSO ACTIVO",
                color = Volt,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                formatClock(remainingSec),
                color = TextPrimary,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(CardHigh)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Volt)
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(CardHigh)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(999.dp))
                        .clickable(onClick = onMinus)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("-10s", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(CardHigh)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(999.dp))
                        .clickable(onClick = onPlus)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("+10s", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(Orange)
                    .clickable(onClick = onFinish)
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Done, contentDescription = null, tint = OnOrange, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Terminar descanso", color = OnOrange, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
