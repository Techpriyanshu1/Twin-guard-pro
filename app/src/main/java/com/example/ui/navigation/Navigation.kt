package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Camera : Screen("camera")
    object Monitor : Screen("monitor/{ip}/{port}/{token}") {
        fun createRoute(ip: String, port: Int, token: String): String = "monitor/$ip/$port/$token"
    }
    object QrPairing : Screen("qr_pairing")
    object LaptopGuide : Screen("laptop_guide")
    object Recordings : Screen("recordings")
    object MotionEvents : Screen("motion_events")
    object Diagnostics : Screen("diagnostics")
    object Settings : Screen("settings")
}
