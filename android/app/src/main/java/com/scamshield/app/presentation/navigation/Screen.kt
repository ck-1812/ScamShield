package com.scamshield.app.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object ProtectCall : Screen("protect_call")
    object ActiveProtection : Screen("active_protection")
    object IncidentSummary : Screen("incident_summary")
    object PrivacyCenter : Screen("privacy_center")
    object ScanMessage : Screen("scan_message")
    object ScanResult : Screen("scan_result")
    object DemoMode : Screen("demo_mode")
    object Settings : Screen("settings")
}
