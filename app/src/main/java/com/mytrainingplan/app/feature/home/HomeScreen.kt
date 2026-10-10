package com.mytrainingplan.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mytrainingplan.app.ui.theme.AppColors
import com.mytrainingplan.app.ui.theme.AppDimens
import com.mytrainingplan.app.ui.theme.AppTextSizes
import androidx.hilt.navigation.compose.hiltViewModel
import com.mytrainingplan.app.domain.model.AccentColor
import com.mytrainingplan.app.domain.model.FootKind
import com.mytrainingplan.app.domain.model.HomeUiState
import com.mytrainingplan.app.domain.model.RoutineSummary
import com.mytrainingplan.app.domain.model.WeeklyProgress

// Colores y medidas desde ui/theme (spec 011): sin tokens locales.

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
    AccentColor.ORANGE -> AppColors.Orange
    AccentColor.VOLT -> AppColors.Volt
    AccentColor.CYAN -> AppColors.Cyan
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
            title = { Text("Eliminar rutina", color = AppColors.TextPrimary) },
            text = {
                Text(
                    "¿Eliminar \"${target.title}\"? Se quitará de tu lista.",
                    color = AppColors.TextMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onDeleteRoutine(target.id)
                        pendingDelete = null
                    }
                ) { Text("Sí, eliminar", color = AppColors.Orange) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancelar", color = AppColors.TextMuted)
                }
            },
            containerColor = AppColors.Card
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
            .background(AppColors.Bg)
            .testTag("homeRoot")
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
                    .padding(horizontal = AppDimens.ScreenHorizontal)
                    // Hueco para que el contenido no quede bajo el dock flotante.
                    .padding(top = AppDimens.ContentTop, bottom = AppDimens.HomeBottom),
                verticalArrangement = Arrangement.spacedBy(AppDimens.ScreenHorizontal)
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
            .background(AppColors.Bg)
            .statusBarsPadding()
            .padding(horizontal = AppDimens.ScreenHorizontal, vertical = AppDimens.SpaceXl),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar con borde táctico + punto verde (iniciales, como en 001).
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(AppDimens.Avatar)
                    .clip(CircleShape)
                    .background(AppColors.Card2)
                    .border(AppDimens.BorderThick, AppColors.Orange, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    firstName.firstOrNull()?.uppercase() ?: "A",
                    color = AppColors.TextPrimary,
                    fontSize = AppTextSizes.Headline,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(AppDimens.PresenceDot)
                    .clip(CircleShape)
                    .background(AppColors.Volt)
                    .border(AppDimens.BorderThick, AppColors.Bg, CircleShape)
            )
        }
        Spacer(Modifier.width(AppDimens.SpaceXl))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                greeting,
                color = AppColors.TextPrimary,
                fontSize = AppTextSizes.Headline,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(AppDimens.SpaceSm)
                        .clip(CircleShape)
                        .background(AppColors.Orange)
                )
                Spacer(Modifier.width(AppDimens.SpaceSm))
                Text(
                    "Listo para entrenar",
                    color = AppColors.Orange,
                    fontSize = AppTextSizes.Small,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        // CTA Crear Rutina + (TODO sin crash).
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(AppDimens.RadiusPill))
                .background(AppColors.Orange)
                .clickable(role = Role.Button, onClickLabel = "Crear rutina", onClick = onCreate)
                .padding(horizontal = AppDimens.SpaceXxl, vertical = AppDimens.SpaceLg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = AppColors.OnOrange, modifier = Modifier.size(AppDimens.IconMd))
            Spacer(Modifier.width(AppDimens.SpaceXs))
            Text("Crear Rutina +", color = AppColors.OnOrange, fontSize = AppTextSizes.Body, fontWeight = FontWeight.Bold)
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
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceXl)) {
        Row(
            modifier = Modifier.fillMaxWidth().testTag("routineFeedHeader"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tus Rutinas", color = AppColors.TextPrimary, fontSize = AppTextSizes.DisplayLg, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(AppDimens.SpaceMd))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(AppDimens.RadiusMd))
                        .background(AppColors.Card2)
                        .padding(horizontal = AppDimens.SpaceMd, vertical = AppDimens.SpaceXs)
                ) {
                    Text("${routines.size} activas", color = AppColors.Orange, fontSize = AppTextSizes.Small, fontWeight = FontWeight.Bold)
                }
            }
            // Botón sort (TODO sin crash, hit target 48dp).
            Box(
                modifier = Modifier
                    .size(AppDimens.TouchMin)
                    .clickable(role = Role.Button, onClickLabel = "Ordenar rutinas", onClick = onSort),
                contentAlignment = Alignment.Center
            ) {
                // Sort no está en material-icons-core → fallback List.
                Icon(Icons.Filled.List, contentDescription = "Ordenar", tint = AppColors.TextMuted, modifier = Modifier.size(AppDimens.IconLg))
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

@OptIn(ExperimentalLayoutApi::class)
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
            .clip(RoundedCornerShape(AppDimens.RadiusCard))
            .background(AppColors.Card)
            .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusCard))
            .padding(AppDimens.SpaceHuge)
            .testTag("routineCard:${routine.id}")
    ) {
        // Accent bar lateral.
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(AppDimens.AccentBarWidth)
                .height(AppDimens.AccentBarHeight)
                .clip(RoundedCornerShape(0.dp, AppDimens.RadiusXs, AppDimens.RadiusXs, 0.dp))
                .background(accent)
        )
        Column(modifier = Modifier.padding(start = AppDimens.SpaceXl)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(routine.title, color = AppColors.TextPrimary, fontSize = AppTextSizes.TitleLg, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(AppDimens.SpaceMd))
                    // FlowRow con wrap: con 5 grupos salta a 2 líneas en vez
                    // de recortar el último tag fuera de la tarjeta.
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSm),
                        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSm)
                    ) {
                        routine.tags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(AppDimens.RadiusMd))
                                    .background(AppColors.Card2)
                                    .padding(horizontal = AppDimens.SpaceMd, vertical = AppDimens.SpaceXs)
                            ) {
                                Text(tag, color = AppColors.TextMuted, fontSize = AppTextSizes.Small)
                            }
                        }
                    }
                    Spacer(Modifier.height(AppDimens.SpaceLg))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.DateRange, contentDescription = null, tint = AppColors.Orange, modifier = Modifier.size(AppDimens.IconSm))
                        Spacer(Modifier.width(AppDimens.SpaceXs))
                        Text("${routine.durationMin} min", color = AppColors.TextMuted, fontSize = AppTextSizes.Body)
                        Text("  •  ", color = AppColors.TextMuted, fontSize = AppTextSizes.Body)
                        Icon(Icons.Filled.List, contentDescription = null, tint = AppColors.Cyan, modifier = Modifier.size(AppDimens.IconSm))
                        Spacer(Modifier.width(AppDimens.SpaceXs))
                        Text("${routine.exerciseCount} ejercicios", color = AppColors.TextMuted, fontSize = AppTextSizes.Body)
                        if (routine.lastDoneLabel != null) {
                            Text("  •  ", color = AppColors.TextMuted, fontSize = AppTextSizes.Body)
                            Text(routine.lastDoneLabel, color = AppColors.TextMuted, fontSize = AppTextSizes.Body)
                        }
                    }
                }
                // Botón ··· con menú Editar / Eliminar (hit target 48dp).
                // El ModalBottomSheet/diálogos gestionan insets solos; el
                // DropdownMenu se ancla a este Box.
                var menuExpanded by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier.size(AppDimens.TouchMin),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .size(AppDimens.TouchMin)
                            .clickable(role = Role.Button, onClickLabel = "Opciones de rutina", onClick = { menuExpanded = true }),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Opciones", tint = AppColors.TextMuted, modifier = Modifier.size(AppDimens.IconLg))
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(AppColors.Card2)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar", color = AppColors.TextPrimary) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar", color = AppColors.Orange) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(AppDimens.SpaceXl))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppDimens.Divider)
                    .background(AppColors.BorderSubtle)
            )
            Spacer(Modifier.height(AppDimens.SpaceXl))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val footColor = if (routine.footKind == FootKind.PR) AppColors.Volt else AppColors.TextMuted
                    val footIcon = when {
                        routine.footKind == FootKind.PR -> Icons.Filled.Star
                        routine.title.contains("Sentadilla") -> Icons.Filled.Info
                        else -> Icons.Filled.Check
                    }
                    Icon(footIcon, contentDescription = null, tint = footColor, modifier = Modifier.size(AppDimens.IconSm))
                    Spacer(Modifier.width(AppDimens.SpaceSm))
                    Text(
                        routine.footNote,
                        color = footColor,
                        fontSize = AppTextSizes.Small,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(AppDimens.SpaceMd))
                // Primera card con CTA naranja, resto secundario (como code.html).
                val primary = routine.accent == AccentColor.ORANGE
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(AppDimens.RadiusPill))
                        .background(if (primary) AppColors.Orange else AppColors.Card2)
                        .border(AppDimens.BorderThin, if (primary) AppColors.Orange else AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusPill))
                        .clickable(role = Role.Button, onClickLabel = "Iniciar rutina", onClick = onStart)
                        .padding(horizontal = AppDimens.SpaceHuge, vertical = AppDimens.SpaceLg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = if (primary) AppColors.OnOrange else AppColors.TextPrimary,
                        modifier = Modifier.size(AppDimens.IconMd)
                    )
                    Spacer(Modifier.width(AppDimens.SpaceXs))
                    Text(
                        "Iniciar",
                        color = if (primary) AppColors.OnOrange else AppColors.TextPrimary,
                        fontSize = AppTextSizes.Body,
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
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceXl)
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
            .clip(RoundedCornerShape(AppDimens.RadiusCard))
            .background(AppColors.Card)
            .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusCard))
            .padding(AppDimens.SpaceXxl)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Calendario", color = AppColors.TextPrimary, fontSize = AppTextSizes.BodyLg, fontWeight = FontWeight.Bold)
            Icon(Icons.Filled.DateRange, contentDescription = null, tint = AppColors.Orange, modifier = Modifier.size(AppDimens.IconMd))
        }
        Spacer(Modifier.height(AppDimens.SpaceSm))
        Text("🔥 Racha ${progress.streakDays} días", color = AppColors.Orange, fontSize = AppTextSizes.Small, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(AppDimens.SpaceLg))
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
                        color = if (today) AppColors.Orange else AppColors.TextMuted,
                        fontSize = AppTextSizes.Tiny,
                        fontWeight = if (today) FontWeight.Bold else FontWeight.Medium
                    )
                    Spacer(Modifier.height(AppDimens.SpaceXs))
                    Box(
                        modifier = Modifier
                            .size(AppDimens.DayDot)
                            .clip(CircleShape)
                            .background(
                                when {
                                    today && done -> AppColors.Orange
                                    done -> AppColors.Volt
                                    else -> AppColors.Card2
                                }
                            )
                            .border(
                                AppDimens.BorderThin,
                                if (done) Color.Transparent else AppColors.BorderSubtle,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (done) {
                            Text(
                                "✓",
                                color = if (today) AppColors.OnOrange else AppColors.OnVolt,
                                fontSize = AppTextSizes.Caption,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(AppDimens.SpaceLg))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.Divider)
                .background(AppColors.BorderSubtle)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClickLabel = "Ver mes", onClick = onSeeMonth)
                .padding(vertical = AppDimens.SpaceMd),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ver mes", color = AppColors.Orange, fontSize = AppTextSizes.Small)
            Spacer(Modifier.width(AppDimens.SpaceXs))
            Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = AppColors.Orange, modifier = Modifier.size(AppDimens.IconSm))
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
            .clip(RoundedCornerShape(AppDimens.RadiusCard))
            .background(AppColors.Card)
            .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusCard))
            .padding(AppDimens.SpaceXxl)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Progreso", color = AppColors.TextPrimary, fontSize = AppTextSizes.BodyLg, fontWeight = FontWeight.Bold)
            // Insights no está en core → fallback Star.
            Icon(Icons.Filled.Star, contentDescription = null, tint = AppColors.Volt, modifier = Modifier.size(AppDimens.IconMd))
        }
        Spacer(Modifier.height(AppDimens.SpaceSm))
        Text("Sesiones", color = AppColors.TextMuted, fontSize = AppTextSizes.Small)
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${progress.sessionsDone}", color = AppColors.TextPrimary, fontSize = AppTextSizes.DisplayLg, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(AppDimens.SpaceXs))
            Text("/ ${progress.sessionsGoal} objetivo", color = AppColors.TextMuted, fontSize = AppTextSizes.Small)
        }
        Spacer(Modifier.height(AppDimens.SpaceMd))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.ProgressBarThin)
                .clip(RoundedCornerShape(AppDimens.RadiusPill))
                .background(AppColors.Card2)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.progressFraction.coerceIn(0f, 1f))
                    .height(AppDimens.ProgressBarThin)
                    .clip(RoundedCornerShape(AppDimens.RadiusPill))
                    .background(AppColors.Volt)
            )
        }
        Spacer(Modifier.height(AppDimens.SpaceLg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Tiempo:", color = AppColors.TextMuted, fontSize = AppTextSizes.Small)
            Text("${progress.minutes} min", color = AppColors.TextPrimary, fontSize = AppTextSizes.Small, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(AppDimens.SpaceLg))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.Divider)
                .background(AppColors.BorderSubtle)
        )
        Spacer(Modifier.height(AppDimens.SpaceMd))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Carga:", color = AppColors.TextMuted, fontSize = AppTextSizes.Small)
            Text("+${progress.volumeDeltaPct}% vol", color = AppColors.Orange, fontSize = AppTextSizes.Small, fontWeight = FontWeight.Bold)
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
            .padding(horizontal = AppDimens.ScreenHorizontal)
            .padding(bottom = AppDimens.SpaceHuge)
            .clip(RoundedCornerShape(AppDimens.RadiusDock))
            .background(AppColors.DockBg)
            .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusDock))
            .padding(horizontal = AppDimens.SpaceMd, vertical = AppDimens.SpaceSm)
            .testTag("homeDock"),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HomeTab.entries.forEach { tab ->
            val active = tab == selectedTab
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(AppDimens.RadiusCard))
                    .clickable(role = Role.Tab, onClickLabel = tab.label, onClick = { onTabSelected(tab) })
                    .padding(horizontal = AppDimens.SpaceXxl)
                    // Hit target >= 48dp.
                    .height(AppDimens.TabHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    tab.icon,
                    contentDescription = tab.label,
                    tint = if (active) AppColors.Orange else AppColors.TextMuted,
                    modifier = Modifier.size(AppDimens.IconLg)
                )
                Text(
                    tab.label,
                    color = if (active) AppColors.Orange else AppColors.TextMuted,
                    fontSize = AppTextSizes.Caption,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .padding(top = AppDimens.SpaceXxs)
                        .size(AppDimens.SpaceXs)
                        .clip(CircleShape)
                        .background(if (active) AppColors.Orange else Color.Transparent)
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
