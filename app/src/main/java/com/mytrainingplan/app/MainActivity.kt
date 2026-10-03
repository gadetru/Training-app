package com.mytrainingplan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mytrainingplan.app.domain.model.Profile
import com.mytrainingplan.app.feature.home.BottomDock
import com.mytrainingplan.app.feature.home.CalendarPlaceholder
import com.mytrainingplan.app.feature.home.HomeScreen
import com.mytrainingplan.app.feature.home.HomeTab
import com.mytrainingplan.app.feature.home.ProgressPlaceholder
import com.mytrainingplan.app.feature.profile.ProfileScreen
import com.mytrainingplan.app.feature.routines.RoutineEditScreen
import com.mytrainingplan.app.ui.theme.MyTrainingPlanTheme

private const val ROUTE_HOME = "home"
private const val ROUTE_CALENDAR = "calendar"
private const val ROUTE_PROGRESS = "progress"
private const val ROUTE_PROFILE = "profile"
private const val ARG_ROUTINE_ID = "routineId"
private const val ROUTE_ROUTINE_EDIT = "routineEdit?$ARG_ROUTINE_ID={$ARG_ROUTINE_ID}"
/** Ruta sin argumentos: crear rutina nueva (el arg queda null por defecto). */
private const val ROUTE_ROUTINE_EDIT_NEW = "routineEdit"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyTrainingPlanTheme(dynamicColor = false, darkTheme = true) {
                TrainingNav()
            }
        }
    }
}

/**
 * NavHost condicional spec 002 (Fase A, sin Hilt/Room).
 * Arranque `¿hay perfil? home:profile` con criterio simple en memoria
 * (nombre no vacío, como en 001; el defecto "Carlos Mendoza" arranca en home).
 * TODO Fase B: criterio real (fila de perfil no borrada en Room).
 */
@Composable
fun TrainingNav() {
    val navController = rememberNavController()
    val startDestination = remember {
        if (Profile().displayName.isNotBlank()) ROUTE_HOME else ROUTE_PROFILE
    }
    NavHost(navController = navController, startDestination = startDestination) {
        composable(ROUTE_HOME) {
            HomeScreen(
                selectedTab = HomeTab.RUTINAS,
                onTabSelected = { navController.navigateToTab(it) },
                // Spec 004: `Crear Rutina +` abre el constructor vacío (nueva
                // rutina); el sheet del 003 era provisional y ahora vive solo
                // en el botón de añadir del constructor. Se navega a la ruta
                // base sin query para que routineId llegue null (navegar al
                // patrón con `{routineId}` literal dejaba el constructor muerto:
                // el ViewModel buscaba un id inexistente y tragaba toda edición).
                onCreate = { navController.navigate(ROUTE_ROUTINE_EDIT_NEW) }
            )
        }
        composable(
            route = ROUTE_ROUTINE_EDIT,
            arguments = listOf(
                navArgument(ARG_ROUTINE_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            RoutineEditScreen(
                routineId = backStackEntry.arguments?.getString(ARG_ROUTINE_ID),
                onSaved = {
                    navController.popBackStack()
                    navController.navigateToTab(HomeTab.RUTINAS)
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(ROUTE_CALENDAR) {
            TabShell(
                selectedTab = HomeTab.CALENDARIO,
                onTabSelected = { navController.navigateToTab(it) }
            ) {
                CalendarPlaceholder()
            }
        }
        composable(ROUTE_PROGRESS) {
            TabShell(
                selectedTab = HomeTab.PROGRESO,
                onTabSelected = { navController.navigateToTab(it) }
            ) {
                ProgressPlaceholder()
            }
        }
        composable(ROUTE_PROFILE) {
            // El tab Perfil reutiliza ProfileScreen de 001 sin dock superpuesto
            // para no tapar su CTA "Guardar y Continuar"; se sale con ← o Guardar.
            ProfileScreen(
                onBack = { navController.navigateToTab(HomeTab.RUTINAS) },
                onSave = { navController.navigateToTab(HomeTab.RUTINAS) }
            )
        }
    }
}

private fun NavHostController.navigateToTab(tab: HomeTab) {
    val route = when (tab) {
        HomeTab.RUTINAS -> ROUTE_HOME
        HomeTab.CALENDARIO -> ROUTE_CALENDAR
        HomeTab.PROGRESO -> ROUTE_PROGRESS
        HomeTab.PERFIL -> ROUTE_PROFILE
    }
    navigate(route) {
        launchSingleTop = true
        popUpTo(graph.startDestinationId) { saveState = true }
        restoreState = true
    }
}

/** Placeholder a pantalla completa + dock flotante compartido. */
@Composable
private fun TabShell(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        BottomDock(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyTrainingPlanTheme {
        Greeting("Android")
    }
}
