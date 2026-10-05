package com.scamshield.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.scamshield.app.presentation.MainViewModel
import com.scamshield.app.presentation.active.ActiveProtectionScreen
import com.scamshield.app.presentation.demo.DemoModeScreen
import com.scamshield.app.presentation.home.HomeScreen
import com.scamshield.app.presentation.incident.IncidentSummaryScreen
import com.scamshield.app.presentation.onboarding.OnboardingScreen
import com.scamshield.app.presentation.privacy.PrivacyCenterScreen
import com.scamshield.app.presentation.protect.ProtectCallScreen
import com.scamshield.app.presentation.scan.ScanResultScreen
import com.scamshield.app.presentation.scan.ScanSuspiciousMessageScreen
import com.scamshield.app.presentation.settings.SettingsScreen
import com.scamshield.app.presentation.splash.SplashScreen
import com.scamshield.app.presentation.theme.*
import com.scamshield.core.domain.model.ProtectionSession

@Composable
fun NavGraph(
    viewModel: MainViewModel,
    navController: NavHostController = rememberNavController()
) {
    val currentRiskScore by viewModel.currentRiskScore.collectAsState()
    val recentTranscripts by viewModel.recentTranscripts.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val privacyState by viewModel.privacyState.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()
    val shareTranscript by viewModel.shareTranscriptWithDashboard.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavRoutes = listOf(
        Screen.Home.route,
        Screen.IncidentSummary.route,
        Screen.ScanMessage.route,
        Screen.PrivacyCenter.route
    )

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomNavRoutes) {
                NavigationBar(
                    containerColor = LightSurface,
                    tonalElevation = 4.dp
                ) {
                    NavigationBarItem(
                        selected = currentRoute == Screen.Home.route,
                        onClick = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Rounded.Home, contentDescription = "Home") },
                        label = { Text("Home", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryAccent,
                            selectedTextColor = PrimaryAccent,
                            indicatorColor = PrimaryAccentLight,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )

                    NavigationBarItem(
                        selected = currentRoute == Screen.IncidentSummary.route,
                        onClick = {
                            navController.navigate(Screen.IncidentSummary.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Rounded.History, contentDescription = "Activity") },
                        label = { Text("Activity", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryAccent,
                            selectedTextColor = PrimaryAccent,
                            indicatorColor = PrimaryAccentLight,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )

                    NavigationBarItem(
                        selected = currentRoute == Screen.ScanMessage.route,
                        onClick = {
                            navController.navigate(Screen.ScanMessage.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Rounded.DocumentScanner, contentDescription = "Scan") },
                        label = { Text("Scan", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryAccent,
                            selectedTextColor = PrimaryAccent,
                            indicatorColor = PrimaryAccentLight,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )

                    NavigationBarItem(
                        selected = currentRoute == Screen.PrivacyCenter.route,
                        onClick = {
                            navController.navigate(Screen.PrivacyCenter.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Rounded.Lock, contentDescription = "Privacy") },
                        label = { Text("Privacy", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryAccent,
                            selectedTextColor = PrimaryAccent,
                            indicatorColor = PrimaryAccentLight,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onSplashComplete = {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onGetStarted = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    onProtectCall = { navController.navigate(Screen.ProtectCall.route) },
                    onPrivacyCenter = { navController.navigate(Screen.PrivacyCenter.route) },
                    onDemoMode = { navController.navigate(Screen.DemoMode.route) },
                    onScanMessage = { navController.navigate(Screen.ScanMessage.route) },
                    onSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(Screen.ProtectCall.route) {
                ProtectCallScreen(
                    onStartActiveProtection = {
                        viewModel.startCallProtection()
                        navController.navigate(Screen.ActiveProtection.route) {
                            popUpTo(Screen.ProtectCall.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ActiveProtection.route) {
                ActiveProtectionScreen(
                    riskScore = currentRiskScore,
                    recentTranscript = recentTranscripts,
                    onStopProtection = {
                        viewModel.stopCallProtection()
                        navController.navigate(Screen.IncidentSummary.route) {
                            popUpTo(Screen.ActiveProtection.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.IncidentSummary.route) {
                val sessionToDisplay = activeSession ?: ProtectionSession(peakRiskScore = currentRiskScore)
                IncidentSummaryScreen(
                    session = sessionToDisplay,
                    onDeleteSession = {
                        viewModel.deleteAllSessionData()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.IncidentSummary.route) { inclusive = true }
                        }
                    },
                    onSaveSummary = { /* User opted in to save */ },
                    onDone = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.IncidentSummary.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.PrivacyCenter.route) {
                PrivacyCenterScreen(
                    privacyState = privacyState,
                    onDeleteAllData = { viewModel.deleteAllSessionData() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ScanMessage.route) {
                ScanSuspiciousMessageScreen(
                    onAnalyzeText = { text ->
                        viewModel.analyzeScamLensNotice(text)
                        navController.navigate(Screen.ScanResult.route)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ScanResult.route) {
                scanResult?.let { result ->
                    ScanResultScreen(
                        result = result,
                        onBackToScan = { navController.popBackStack() },
                        onHome = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    )
                }
            }

            composable(Screen.DemoMode.route) {
                DemoModeScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    localIpAddress = viewModel.dashboardTransport.getLocalIpAddress(),
                    isTranscriptSharingEnabled = shareTranscript,
                    isDashboardConnected = viewModel.dashboardTransport.isConnected(),
                    onToggleTranscriptSharing = { viewModel.setTranscriptSharing(it) },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
