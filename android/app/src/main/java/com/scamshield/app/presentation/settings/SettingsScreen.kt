package com.scamshield.app.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scamshield.app.presentation.theme.*

@Composable
fun SettingsScreen(
    localIpAddress: String,
    isTranscriptSharingEnabled: Boolean,
    isDashboardConnected: Boolean,
    onToggleTranscriptSharing: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    var shareTranscript by remember { mutableStateOf(isTranscriptSharingEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(
                text = "Settings",
                fontSize = 17.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        // Laptop Command Center Pairing Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(LightSurface)
                .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "LAPTOP COMMAND CENTER (OFFICE KIT)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted
            )

            Text(
                text = "ScamShield broadcasts real-time risk telemetry to your laptop over local Wi-Fi. Zero raw audio is transmitted.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(LightSurfaceSecondary)
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "PHONE WEBSOCKET ENDPOINT",
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "ws://$localIpAddress:8765",
                        color = PrimaryAccent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dashboard Status", color = TextSecondary, fontSize = 13.sp)
                Text(
                    text = if (isDashboardConnected) "CONNECTED" else "DISCONNECTED",
                    color = if (isDashboardConnected) RiskLow else TextMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // Privacy Controls Section (Section 40)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(LightSurface)
                .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "PRIVACY & TELEMETRY CONTROLS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Share Transcript Snippets",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Allows laptop dashboard to display short text snippets. Disabled by default to protect conversation content.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
                Switch(
                    checked = shareTranscript,
                    onCheckedChange = {
                        shareTranscript = it
                        onToggleTranscriptSharing(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = LightSurface,
                        checkedTrackColor = PrimaryAccent,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = LightSurfaceSecondary
                    )
                )
            }
        }

        // On-Device Inference Engine Info (Section 40)
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
                text = "ENGINE SPECIFICATIONS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted
            )

            EngineStatusRowLight("Speech Engine", "MOCK (WHISPER INTEGRATION-READY)")
            EngineStatusRowLight("Reasoning Engine", "RULE-BASED (QWEN INTEGRATION-READY)")
            EngineStatusRowLight("Risk Engine", "DETERMINISTIC (0-100 CAPPED)")
        }

        // About / Legal positioning
        Text(
            text = "ScamShield detects conversational patterns associated with known scam tactics. It is an assistive safety alert tool, not a certified law-enforcement system.",
            color = TextMuted,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun EngineStatusRowLight(title: String, status: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = TextSecondary, fontSize = 13.sp)
        Text(
            status,
            color = PrimaryAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
