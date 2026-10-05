package com.scamshield.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ScamShieldLightColorScheme = lightColorScheme(
    primary = PrimaryAccent,
    onPrimary = LightSurface,
    primaryContainer = PrimaryAccentLight,
    onPrimaryContainer = PrimaryAccent,
    secondary = TextSecondary,
    onSecondary = LightSurface,
    background = LightBgPrimary,
    onBackground = TextPrimary,
    surface = LightSurface,
    onSurface = TextPrimary,
    surfaceVariant = LightSurfaceSecondary,
    onSurfaceVariant = TextSecondary,
    outline = BorderLight,
    error = RiskCritical,
    onError = LightSurface
)

@Composable
fun ScamShieldTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ScamShieldLightColorScheme,
        content = content
    )
}
