package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.ExportDetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.StudioWorkspaceScreen
import com.example.ui.theme.MoviesRecapTheme
import com.example.ui.viewmodels.StudioViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoviesRecapTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val studioViewModel: StudioViewModel = viewModel()
                    val navController = rememberNavController()
                    MoviesRecapAppNavigation(
                        navController = navController,
                        viewModel = studioViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun MoviesRecapAppNavigation(
    navController: NavHostController,
    viewModel: StudioViewModel
) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToStudio = {
                    navController.navigate("studio")
                },
                onNavigateToExportDetail = { projectId ->
                    navController.navigate("export/$projectId")
                }
            )
        }

        composable("studio") {
            StudioWorkspaceScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToExportDetail = { projectId ->
                    navController.navigate("export/$projectId")
                }
            )
        }

        composable(
            route = "export/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) {
            ExportDetailScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
