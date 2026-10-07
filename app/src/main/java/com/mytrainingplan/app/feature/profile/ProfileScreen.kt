package com.mytrainingplan.app.feature.profile

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mytrainingplan.app.domain.model.Profile
import com.mytrainingplan.app.domain.model.TrainingGoal
import com.mytrainingplan.app.domain.model.TrainingLevel

// Tokens references/plantilla-usuario/DESIGN.md + code.html
private val Bg = Color(0xFF111316)
private val Card = Color(0xFF1A1C1F)
private val Card2 = Color(0xFF1E2023)
private val Orange = Color(0xFFFF5E00)
private val Volt = Color(0xFFCCFF00)
private val TextPrimary = Color(0xFFF5F7FA)
private val TextMuted = Color(0xFF8B95A5)
private val BorderSubtle = Color(0xFF282E37)
private val InputBg = Color(0xFF0C0E11)

@Composable
fun ProfileScreen(
    initial: Profile? = null,
    onBack: () -> Unit = {},
    onSave: (Profile) -> Unit = {}
) {
    var displayName by remember(initial) { mutableStateOf(initial?.displayName ?: "Carlos Mendoza") }
    var ageText by remember(initial) { mutableStateOf((initial?.age ?: 28).toString()) }
    var heightText by remember(initial) { mutableStateOf((initial?.heightCm ?: 178).toString()) }
    var weightText by remember(initial) { mutableStateOf((initial?.weightKg ?: 78.5).toString()) }
    var level by remember(initial) { mutableStateOf(initial?.level ?: TrainingLevel.INTERMEDIO) }
    var goal by remember(initial) { mutableStateOf(initial?.goal ?: TrainingGoal.FUERZA_POTENCIA) }
    // avatarUri se cableará con picker + Coil en el siguiente paso.
    // Por ahora placeholder con iniciales para la réplica visual.
    var avatarUri by remember(initial) { mutableStateOf(initial?.avatarUri) }

    val profile = Profile(
        displayName = displayName.ifBlank { "Atleta" },
        age = ageText.toIntOrNull(),
        heightCm = heightText.toIntOrNull(),
        weightKg = weightText.toDoubleOrNull(),
        level = level,
        goal = goal,
        avatarUri = avatarUri
    )

    ProfileContent(
        profile = profile,
        displayName = displayName,
        ageText = ageText,
        heightText = heightText,
        weightText = weightText,
        onDisplayName = { displayName = it },
        onAge = { ageText = it.filter { c -> c.isDigit() }.take(3) },
        onHeight = { heightText = it.filter { c -> c.isDigit() }.take(3) },
        onWeight = {
            // decimal simple con un solo punto
            val filtered = it.filter { c -> c.isDigit() || c == '.' || c == ',' }
                .replace(',', '.')
            val parts = filtered.split('.')
            val normalized = if (parts.size <= 2) filtered
            else parts[0] + "." + parts.drop(1).joinToString("")
            weightText = normalized.take(6)
        },
        onLevel = { level = it },
        onGoal = { goal = it },
        onAvatarClick = { /* TODO picker + Coil */ },
        onBack = onBack,
        onSave = { onSave(profile) }
    )
}

@Composable
private fun ProfileContent(
    profile: Profile,
    displayName: String,
    ageText: String,
    heightText: String,
    weightText: String,
    onDisplayName: (String) -> Unit,
    onAge: (String) -> Unit,
    onHeight: (String) -> Unit,
    onWeight: (String) -> Unit,
    onLevel: (TrainingLevel) -> Unit,
    onGoal: (TrainingGoal) -> Unit,
    onAvatarClick: () -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .testTag("profileRoot")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header sticky (con colchón sobre la barra de estado: edge-to-edge).
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
                        .clip(CircleShape)
                        .background(Card)
                        .clickable(
                            onClickLabel = "Volver",
                            role = Role.Button,
                            onClick = onBack
                        )
                        .semantics { contentDescription = "Volver" },
                    contentAlignment = Alignment.Center
                ) {
                    Text("←", color = TextPrimary, fontSize = 20.sp)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Tu Perfil de Atleta",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "CONFIGURACIÓN INICIAL",
                        color = Orange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Card2)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("1/1", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Contenido scroll
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                // Hero avatar
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // Halo
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Orange, Color(0x4DFFB599), Color.Transparent)
                                    )
                                )
                        )
                        // Foto placeholder (Coil en siguiente paso)
                        Box(
                            modifier = Modifier
                                .size(112.dp)
                                .clip(CircleShape)
                                .background(Card2)
                                .border(2.dp, Orange, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val initials = profile.displayName.trim().split(" ")
                                .mapNotNull { it.firstOrNull()?.uppercase() }
                                .take(2).joinToString("")
                                .ifEmpty { "AT" }
                            Text(initials, color = TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Orange)
                                .border(2.dp, Bg, CircleShape)
                                .clickable(
                                    onClickLabel = "Cambiar foto de perfil",
                                    role = Role.Button,
                                    onClick = onAvatarClick
                                )
                                .semantics { contentDescription = "Cambiar foto de perfil" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📷", fontSize = 16.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Cambiar foto ✎",
                        color = Orange,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(
                            onClickLabel = "Cambiar foto de perfil",
                            role = Role.Button,
                            onClick = onAvatarClick
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Añade una foto para personalizar tu experiencia y calibrar tu panel de métricas.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // Nombre
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Card)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Nombre o Apodo de Atleta", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = onDisplayName,
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = InputBg,
                                unfocusedContainerColor = InputBg,
                                focusedBorderColor = Orange,
                                unfocusedBorderColor = BorderSubtle,
                                cursorColor = Orange
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(end = 36.dp)
                        )
                        if (displayName.isNotBlank()) {
                            Text(
                                "✔",
                                color = Orange,
                                fontSize = 20.sp,
                                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)
                                    .semantics { contentDescription = "Nombre válido" }
                            )
                        }
                    }
                }

                // Bento 3 métricas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        label = "EDAD",
                        value = ageText,
                        unit = "años",
                        keyboardType = KeyboardType.Number,
                        onValue = onAge,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        label = "ESTATURA",
                        value = heightText,
                        unit = "cm",
                        keyboardType = KeyboardType.Number,
                        onValue = onHeight,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        label = "PESO",
                        value = weightText,
                        unit = "kg",
                        unitColor = Volt,
                        keyboardType = KeyboardType.Decimal,
                        onValue = onWeight,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Nivel + Enfoque
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Card)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚡ Nivel de Rendimiento", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x1FCCFF00))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Atleta Activo", color = Volt, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TrainingLevel.entries.forEach { lv ->
                            val selected = lv == profile.level
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) Color(0x26FF5E00) else Card2)
                                    .border(
                                        if (selected) 2.dp else 1.dp,
                                        if (selected) Orange else BorderSubtle,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable(
                                        onClickLabel = "Seleccionar nivel ${lv.displayName}",
                                        role = Role.Button
                                    ) { onLevel(lv) }
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    lv.displayName,
                                    color = if (selected) Orange else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(lv.detail, color = if (selected) Orange else TextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(BorderSubtle)
                    )
                    Text("ENFOQUE DE ENTRENAMIENTO", color = TextMuted, fontSize = 10.sp, letterSpacing = 1.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TrainingGoal.entries.forEach { g ->
                            val selected = g == profile.goal
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (selected) Orange else Card2)
                                    .border(1.dp, if (selected) Orange else BorderSubtle, RoundedCornerShape(999.dp))
                                    .clickable(
                                        onClickLabel = "Seleccionar enfoque ${g.displayName}",
                                        role = Role.Button
                                    ) { onGoal(g) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    g.displayName,
                                    color = if (selected) Color(0xFF1A0A00) else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Preview live
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("👁 VISTA PREVIA EN CABECERA DE INICIO", color = TextMuted, fontSize = 10.sp)
                        Text("En tiempo real", color = Orange, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Card2)
                            .border(1.dp, Color(0x4DFF5E00), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Bg)
                                .border(1.dp, Orange, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                profile.firstName.firstOrNull()?.uppercase() ?: "A",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(profile.greeting, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(6.dp))
                                Text("🔥", fontSize = 14.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Racha: 4 días", color = TextMuted, fontSize = 12.sp)
                                Spacer(Modifier.width(8.dp))
                                Box(Modifier.size(4.dp).clip(CircleShape).background(TextMuted))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${weightText.ifBlank { "—" }} kg",
                                    color = Volt,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Card)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(999.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Crear Rutina +", color = Orange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // CTA fijo (dentro de Column para no usar Scaffold aún; con colchón
            // sobre la barra de navegación: edge-to-edge).
            Surface(
                color = Color(0xF21A1C1F),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(containerColor = Orange),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .height(52.dp)
                        .testTag("profileSave")
                ) {
                    Text("Guardar y Continuar  →", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    unit: String,
    keyboardType: KeyboardType,
    onValue: (String) -> Unit,
    modifier: Modifier = Modifier,
    unitColor: Color = TextMuted
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Card)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, color = TextMuted, fontSize = 10.sp, letterSpacing = 1.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValue,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = InputBg,
                unfocusedContainerColor = InputBg,
                focusedBorderColor = Orange,
                unfocusedBorderColor = BorderSubtle,
                cursorColor = Orange
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Text(unit, color = unitColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111316)
@Composable
private fun ProfileScreenPreview() {
    MaterialPreviewWrapper {
        ProfileScreen()
    }
}

@Composable
private fun MaterialPreviewWrapper(content: @Composable () -> Unit) {
    androidx.compose.material3.MaterialTheme {
        content()
    }
}
