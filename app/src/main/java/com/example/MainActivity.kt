package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.navigation.Screen
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LaptopViewerScreen
import com.example.ui.screens.MonitorScreen
import com.example.ui.screens.MotionEventsScreen
import com.example.ui.screens.QrPairingScreen
import com.example.ui.screens.RecordingsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.TwinGuardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TwinGuardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    TwinGuardAppNav()
                }
            }
        }
    }
}

@Composable
fun TwinGuardAppNav() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToCamera = { navController.navigate(Screen.Camera.route) },
                onNavigateToMonitor = { navController.navigate(Screen.QrPairing.route) },
                onNavigateToLaptopGuide = { navController.navigate(Screen.LaptopGuide.route) },
                onNavigateToRecordings = { navController.navigate(Screen.Recordings.route) },
                onNavigateToMotionEvents = { navController.navigate(Screen.MotionEvents.route) },
                onNavigateToDiagnostics = { navController.navigate(Screen.Diagnostics.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Camera.route) {
            CameraScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.QrPairing.route) {
            QrPairingScreen(
                onPairSuccess = { ip, port, token ->
                    navController.navigate(Screen.Monitor.createRoute(ip, port, token)) {
                        popUpTo(Screen.QrPairing.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Monitor.route,
            arguments = listOf(
                navArgument("ip") { type = NavType.StringType },
                navArgument("port") { type = NavType.IntType; defaultValue = 8080 },
                navArgument("token") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val ip = backStackEntry.arguments?.getString("ip") ?: "127.0.0.1"
            val port = backStackEntry.arguments?.getInt("port") ?: 8080
            val token = backStackEntry.arguments?.getString("token") ?: ""

            MonitorScreen(
                serverIp = ip,
                serverPort = port,
                authToken = token,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.LaptopGuide.route) {
            LaptopViewerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Recordings.route) {
            RecordingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.MotionEvents.route) {
            MotionEventsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Diagnostics.route) {
            DiagnosticsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
