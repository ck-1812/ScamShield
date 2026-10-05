package com.scamshield.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scamshield.app.presentation.theme.*

@Composable
fun HomeScreen(
    onProtectCall: () -> Unit,
    onPrivacyCenter: () -> Unit,
    onDemoMode: () -> Unit,
    onScanMessage: () -> Unit,
    onSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PrimaryAccentLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = "Shield logo",
                        tint = PrimaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "SCAMSHIELD",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Private protection for suspicious calls",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            IconButton(onClick = onSettings) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    tint = TextSecondary
                )
            }
        }

        // Status Card: PROTECTION OFF (Section 13)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(LightSurface)
                .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PROTECTION OFF",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ScamShield is not currently capturing microphone audio.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(LightSurfaceSecondary)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(RiskLow)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "STANDBY",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Primary Action: [ Protect a call ]
        Button(
            onClick = onProtectCall,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
        ) {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                tint = LightSurface,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Protect a call",
                color = LightSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // HOW IT WORKS Section (Section 13)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(LightSurface)
                .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "HOW IT WORKS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 0.5.sp
            )

            HowItWorksRow(number = "1", title = "Start your call", desc = "Answer or dial normally.")
            HowItWorksRow(number = "2", title = "Enable speakerphone", desc = "Required so the phone microphone can capture caller voice.")
            HowItWorksRow(number = "3", title = "Tap Protect This Call", desc = "ScamShield activates its local analysis layer.")
            HowItWorksRow(number = "4", title = "Review detected scam signals", desc = "Receive real-time warnings if coercive tactics appear.")
        }

        // Feature Navigation Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickNavCard(
                icon = Icons.Rounded.Lock,
                title = "Privacy Center",
                subtitle = "Hardware audit & purge",
                modifier = Modifier.weight(1f),
                onClick = onPrivacyCenter
            )

            QuickNavCard(
                icon = Icons.Rounded.PlayArrow,
                title = "Demo Mode",
                subtitle = "Digital arrest sim",
                modifier = Modifier.weight(1f),
                onClick = onDemoMode
            )
        }

        QuickNavCard(
            icon = Icons.Rounded.DocumentScanner,
            title = "Scan suspicious message",
            subtitle = "Analyze CBI notices, power cutoff SMS & fake APK links",
            modifier = Modifier.fillMaxWidth(),
            onClick = onScanMessage
        )
    }
}

@Composable
private fun HowItWorksRow(number: String, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(LightSurfaceSecondary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = PrimaryAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = desc,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun QuickNavCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(LightSurface)
            .border(1.dp, BorderLight, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryAccent,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 15.sp
            )
        }
    }
}
