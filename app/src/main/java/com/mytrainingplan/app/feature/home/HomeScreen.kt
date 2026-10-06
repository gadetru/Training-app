package com.mytrainingplan.app.feature.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mytrainingplan.app.domain.model.AccentColor
import com.mytrainingplan.app.domain.model.FootKind
import com.mytrainingplan.app.domain.model.HomeUiState
import com.mytrainingplan.app.domain.model.RoutineSummary
import com.mytrainingplan.app.domain.model.WeeklyProgress

// Tokens references/plantilla-vista-principal/DESIGN.md + patrón ProfileScreen (001).
// Tipografías de sistema en Fase A (Outfit/Plus Jakarta/Space Grotesk van en Futuros).
private val Bg = Color(0xFF111316)
private val Card = Color(0xFF1A1C1F)
private val Card2 = Color(0xFF1E2023)
private val Orange = Color(0xFFFF5E00)
private val Volt = Color(0xFFCCFF00)
private val Cyan = Color(0xFF00E5FF)
private val TextPrimary = Color(0xFFF5F7FA)
private val TextMuted = Color(0xFF8B95A5)
private val BorderSubtle = Color(0xFF282E37)

/**
 * Tabs del dock inferior.
 * Desviación conocida vs spec (code.html:381): el spec pide
 * fitness_center/calendar_today/insights/person vía material-icons, pero
 * material-icons-core (única dep declarada) solo trae Person y DateRange
 * (verificado en el sources.jar 1.7.8: 49 iconos, sin FitnessCenter ni
 * CalendarToday ni Insights). Fallbacks del core: List / DateRange / Star.
 * TODO: si se quiere fidelidad exacta, añadir material-icons-extended
 * (propuesta en el informe del paso, requiere aprobación Gradle).
 */
enum class HomeTab(val label: String, val icon: ImageVector) {
    RUTINAS("Rutinas", Icons.Filled.List),
    CALENDARIO("Calendario", Icons.Filled.DateRange),
    PROGRESO("Progreso", Icons.Filled.Star),
    PERFIL("Perfil", Icons.Filled.Person)
}

private fun AccentColor.toCompose(): Color = when (this) {
    AccentColor.ORANGE -> Orange
    AccentColor.VOLT -> Volt
    AccentColor.CYAN -> Cyan
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onStart: (routineId: String) -> Unit = {},
    onCreate: () -> Unit = {},
    onEdit: (routineId: String) -> Unit = {},
    onSort: () -> Unit = {},
    onSeeMonth: () -> Unit = {},
    selectedTab: HomeTab = HomeTab.RUTINAS,
    onTabSelected: (HomeTab) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    // Rutina pendiente de confirmar borrado (menú ··· → Eliminar).
    var pendingDelete by remember { mutableStateOf<RoutineSummary?>(null) }
    HomeContent(
        uiState = uiState,
        onStart = onStart,
        onCreate = onCreate,
        onEdit = onEdit,
        onDeleteRequest = { pendingDelete = it },
        onSort = onSort,
        onSeeMonth = onSeeMonth,
        selectedTab = selectedTab,
        onTabSelected = onTabSelected
    )
    // Confirmación de borrado: los diálogos de Material3 gestionan insets solos.
    val target = pendingDelete
    if (target != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminar rutina", color = TextPrimary) },
            text = {
                Text(
                    "¿Eliminar \"${target.title}\"? Se quitará de tu lista.",
                    color = TextMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onDeleteRoutine(target.id)
                        pendingDelete = null
                    }
                ) { Text("Sí, eliminar", color = Orange) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancelar", color = TextMuted)
                }
            },
            containerColor = Card
        )
    }
}

@Composable
fun HomeContent(
    uiState: HomeUiState,
    onStart: (routineId: String) -> Unit = {},
    onCreate: () -> Unit = {},
    onEdit: (routineId: String) -> Unit = {},
    onDeleteRequest: (RoutineSummary) -> Unit = {},
    onSort: () -> Unit = {},
    onSeeMonth: () -> Unit = {},
    selectedTab: HomeTab = HomeTab.RUTINAS,
    onTabSelected: (HomeTab) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                firstName = uiState.firstName,
                greeting = uiState.greeting,
                onCreate = onCreate
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    // Hueco para que el contenido no quede bajo el dock flotante.
                    .padding(top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                RoutineFeed(
                    routines = uiState.routines,
                    onStart = onStart,
                    onEdit = onEdit,
                    onDeleteRequest = onDeleteRequest,
                    onSort = onSort
                )
                DashboardRow(
                    progress = uiState.progress,
                    onSeeMonth = onSeeMonth
                )
            }
        }
        BottomDock(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun TopBar(
    firstName: String,
    greeting: String,
    onCreate: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Bg)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar con borde táctico + punto verde (iniciales, como en 001).
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Card2)
                    .border(2.dp, Orange, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    firstName.firstOrNull()?.uppercase() ?: "A",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Volt)
                    .border(2.dp, Bg, CircleShape)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                greeting,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Orange)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Listo para entrenar",
                    color = Orange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        // CTA Crear Rutina + (TODO sin crash).
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Orange)
                .clickable(role = Role.Button, onClickLabel = "Crear rutina", onClick = onCreate)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Color(0xFF1A0A00), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Crear Rutina +", color = Color(0xFF1A0A00), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RoutineFeed(
    routines: List<RoutineSummary>,
    onStart: (routineId: String) -> Unit,
    onEdit: (routineId: String) -> Unit,
    onDeleteRequest: (RoutineSummary) -> Unit,
    onSort: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tus Rutinas", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Card2)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("${routines.size} activas", color = Orange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            // Botón sort (TODO sin crash, hit target 48dp).
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(role = Role.Button, onClickLabel = "Ordenar rutinas", onClick = onSort),
                contentAlignment = Alignment.Center
            ) {
                // Sort no está en material-icons-core → fallback List.
                Icon(Icons.Filled.List, contentDescription = "Ordenar", tint = TextMuted, modifier = Modifier.size(22.dp))
            }
        }
        routines.forEach { routine ->
            RoutineCard(
                routine = routine,
                onStart = { onStart(routine.id) },
                onEdit = { onEdit(routine.id) },
                onDelete = { onDeleteRequest(routine) }
            )
        }
    }
}

@Composable
private fun RoutineCard(
    routine: RoutineSummary,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = routine.accent.toCompose()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Card)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Accent bar lateral.
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(4.dp)
                .height(88.dp)
                .clip(RoundedCornerShape(0.dp, 4.dp, 4.dp, 0.dp))
                .background(accent)
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(routine.title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        routine.tags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Card2)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(tag, color = TextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.DateRange, contentDescription = null, tint = Orange, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${routine.durationMin} min", color = TextMuted, fontSize = 12.sp)
                        Text("  •  ", color = TextMuted, fontSize = 12.sp)
                        Icon(Icons.Filled.List, contentDescription = null, tint = Cyan, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${routine.exerciseCount} ejercicios", color = TextMuted, fontSize = 12.sp)
                        if (routine.lastDoneLabel != null) {
                            Text("  •  ", color = TextMuted, fontSize = 12.sp)
                            Text(routine.lastDoneLabel, color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
                // Botón ··· con menú Editar / Eliminar (hit target 48dp).
                // El ModalBottomSheet/diálogos gestionan insets solos; el
                // DropdownMenu se ancla a este Box.
                var menuExpanded by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(role = Role.Button, onClickLabel = "Opciones de rutina", onClick = { menuExpanded = true }),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Opciones", tint = TextMuted, modifier = Modifier.size(22.dp))
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(Card2)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar", color = TextPrimary) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar", color = Orange) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(BorderSubtle)
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val footColor = if (routine.footKind == FootKind.PR) Volt else TextMuted
                    val footIcon = when {
                        routine.footKind == FootKind.PR -> Icons.Filled.Star
                        routine.title.contains("Sentadilla") -> Icons.Filled.Info
                        else -> Icons.Filled.Check
                    }
                    Icon(footIcon, contentDescription = null, tint = footColor, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        routine.footNote,
                        color = footColor,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                // Primera card con CTA naranja, resto secundario (como code.html).
                val primary = routine.accent == AccentColor.ORANGE
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (primary) Orange else Card2)
                        .border(1.dp, if (primary) Orange else BorderSubtle, RoundedCornerShape(999.dp))
                        .clickable(role = Role.Button, onClickLabel = "Iniciar rutina", onClick = onStart)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = if (primary) Color(0xFF1A0A00) else TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Iniciar",
                        color = if (primary) Color(0xFF1A0A00) else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardRow(
    progress: WeeklyProgress,
    onSeeMonth: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CalendarWidget(progress = progress, onSeeMonth = onSeeMonth, modifier = Modifier.weight(1f))
        ProgressWidget(progress = progress, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CalendarWidget(
    progress: WeeklyProgress,
    onSeeMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val days = listOf("L", "M", "X", "J", "V", "S", "D")
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Card)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Calendario", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Filled.DateRange, contentDescription = null, tint = Orange, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text("🔥 Racha ${progress.streakDays} días", color = Orange, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEachIndexed { i, d ->
                val done = progress.weekChecks.getOrElse(i) { false }
                val today = i == 3
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        d,
                        color = if (today) Orange else TextMuted,
                        fontSize = 9.sp,
                        fontWeight = if (today) FontWeight.Bold else FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    today && done -> Orange
                                    done -> Volt
                                    else -> Card2
                                }
                            )
                            .border(
                                1.dp,
                                if (done) Color.Transparent else BorderSubtle,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (done) {
                            Text(
                                "✓",
                                color = if (today) Color(0xFF1A0A00) else Color(0xFF1A2A00),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(BorderSubtle)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClickLabel = "Ver mes", onClick = onSeeMonth)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ver mes", color = Orange, fontSize = 11.sp)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = Orange, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun ProgressWidget(
    progress: WeeklyProgress,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Card)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Progreso", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            // Insights no está en core → fallback Star.
            Icon(Icons.Filled.Star, contentDescription = null, tint = Volt, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text("Sesiones", color = TextMuted, fontSize = 11.sp)
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${progress.sessionsDone}", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(4.dp))
            Text("/ ${progress.sessionsGoal} objetivo", color = TextMuted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Card2)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.progressFraction.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Volt)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Tiempo:", color = TextMuted, fontSize = 11.sp)
            Text("${progress.minutes} min", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(BorderSubtle)
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Carga:", color = TextMuted, fontSize = 11.sp)
            Text("+${progress.volumeDeltaPct}% vol", color = Orange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BottomDock(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xE61E2023))
            .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HomeTab.entries.forEach { tab ->
            val active = tab == selectedTab
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(role = Role.Tab, onClickLabel = tab.label, onClick = { onTabSelected(tab) })
                    .padding(horizontal = 14.dp)
                    // Hit target >= 48dp.
                    .height(56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    tab.icon,
                    contentDescription = tab.label,
                    tint = if (active) Orange else TextMuted,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    tab.label,
                    color = if (active) Orange else TextMuted,
                    fontSize = 10.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (active) Orange else Color.Transparent)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111316)
@Composable
private fun HomeContentPreview() {
    androidx.compose.material3.MaterialTheme {
        // Solo domain/model: sin dependencias de data/repository en la feature.
        HomeContent(
            uiState = HomeUiState(
                firstName = "Carlos",
                routines = listOf(
                    RoutineSummary(
                        id = "preview-1",
                        title = "Torso - Fuerza & Hipertrofia",
                        tags = listOf("Pecho", "Espalda"),
                        durationMin = 45,
                        exerciseCount = 6,
                        lastDoneLabel = "Hace 2 días",
                        accent = AccentColor.ORANGE,
                        footNote = "Récord en Press Banca",
                        footKind = FootKind.PR
                    ),
                    RoutineSummary(
                        id = "preview-2",
                        title = "Pierna & Core Explosivo",
                        tags = listOf("Cuádriceps", "Abdomen"),
                        durationMin = 55,
                        exerciseCount = 7,
                        accent = AccentColor.VOLT,
                        footNote = "Enfoque: Sentadilla profunda",
                        footKind = FootKind.INFO
                    )
                ),
                progress = WeeklyProgress()
            )
        )
    }
}
