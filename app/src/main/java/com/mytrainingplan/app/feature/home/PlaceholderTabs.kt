package com.mytrainingplan.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.mytrainingplan.app.ui.theme.AppColors
import com.mytrainingplan.app.ui.theme.AppDimens
import com.mytrainingplan.app.ui.theme.AppTextSizes

// Colores y medidas desde ui/theme (spec 011): sin tokens locales.

/**
 * Placeholders de tabs Fase A (spec 002, paso 4).
 * El tab Perfil reutiliza ProfileScreen (se cablea en MainActivity, paso 5).
 * TODO Fase B: Calendario/Progreso reales desde sesiones (feature/history).
 */
@Composable
fun CalendarPlaceholder() {
    TabPlaceholder(
        title = "Calendario",
        subtitle = "La vista mensual llega en Fase B con tus sesiones reales.",
        icon = Icons.Filled.DateRange
    )
}

@Composable
fun ProgressPlaceholder() {
    TabPlaceholder(
        title = "Progreso",
        subtitle = "Las gráficas de volumen y racha llegan en Fase B.",
        icon = Icons.Filled.Star
    )
}

@Composable
private fun TabPlaceholder(title: String, subtitle: String, icon: ImageVector) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Bg)
            .padding(AppDimens.ScreenHorizontal),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = AppColors.Orange, modifier = Modifier.size(AppDimens.PlaceholderIcon))
            Spacer(Modifier.height(AppDimens.SpaceXl))
            Text(title, color = AppColors.TextPrimary, fontSize = AppTextSizes.DisplaySm, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(AppDimens.SpaceSm))
            Text(
                subtitle,
                color = AppColors.TextMuted,
                fontSize = AppTextSizes.BodyLg,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111316)
@Composable
private fun CalendarPlaceholderPreview() {
    androidx.compose.material3.MaterialTheme { CalendarPlaceholder() }
}

@Preview(showBackground = true, backgroundColor = 0xFF111316)
@Composable
private fun ProgressPlaceholderPreview() {
    androidx.compose.material3.MaterialTheme { ProgressPlaceholder() }
}
