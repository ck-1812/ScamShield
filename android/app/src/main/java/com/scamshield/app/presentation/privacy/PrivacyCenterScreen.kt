package com.scamshield.app.presentation.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scamshield.app.presentation.theme.*
import com.scamshield.core.domain.model.PrivacyState

@Composable
fun PrivacyCenterScreen(
    privacyState: PrivacyState,
    onDeleteAllData: () -> Unit,
    onBack: () -> Unit
) {
    var showClearedConfirmation by remember { mutableStateOf(false) }

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
                text = "Privacy Center",
                fontSize = 17.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        // Privacy Commitment Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(LightSurface)
                .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PrimaryAccentLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = PrimaryAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Privacy by Design",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "ScamShield does not capture microphone input while Protection is OFF. Application temporary audio buffers are discarded after processing.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Checklist of Privacy Subsystems (Section 30)
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
                text = "PRIVACY SUBSYSTEMS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted
            )

            PrivacyCheckItem(
                title = "MICROPHONE",
                status = if (privacyState.microphoneActive) "Active during protection" else "Protection inactive",
                desc = "ScamShield does not capture microphone input while Protection is OFF.",
                isPositive = true
            )

            PrivacyCheckItem(
                title = "AUDIO STORAGE",
                status = "Temporary in-memory processing",
                desc = "No intentional permanent audio storage. Application audio buffers are discarded after processing.",
                isPositive = true
            )

            PrivacyCheckItem(
                title = "AI PROCESSING",
                status = "Local-first",
                desc = "Scam analysis is executed locally on-device. No cloud AI endpoints.",
                isPositive = true
            )

            PrivacyCheckItem(
                title = "NETWORK",
                status = "No raw audio transmission",
                desc = "Audio is never streamed externally over cellular or cloud interfaces.",
                isPositive = true
            )

            PrivacyCheckItem(
                title = "DASHBOARD TELEMETRY",
                status = if (privacyState.dashboardSharing) "Derived metrics only" else "OFF by default",
                desc = "Sends only numeric risk score and tactic enums to the local companion dashboard.",
                isPositive = true
            )

            PrivacyCheckItem(
                title = "TRANSCRIPT SHARING",
                status = if (privacyState.transcriptSharing) "Active (Opt-in)" else "OFF by default",
                desc = "Transcript sharing requires explicit user activation in settings.",
                isPositive = !privacyState.transcriptSharing
            )

            PrivacyCheckItem(
                title = "SESSION DATA",
                status = "Temporary until deleted",
                desc = "Rolling 45-second window. Session data is cleared upon explicit deletion.",
                isPositive = true
            )
        }

        // Action Button: [ DELETE ALL SESSION DATA ] (Section 30)
        Button(
            onClick = {
                onDeleteAllData()
                showClearedConfirmation = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RiskCritical)
        ) {
            Icon(imageVector = Icons.Rounded.DeleteSweep, contentDescription = null, tint = LightSurface, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "DELETE ALL SESSION DATA",
                color = LightSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        if (showClearedConfirmation) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RiskLowLight)
                    .border(1.dp, RiskLowBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Session data cleared.",
                    color = RiskLow,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PrivacyCheckItem(
    title: String,
    status: String,
    desc: String,
    isPositive: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isPositive) RiskLow else RiskSuspicious)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = status,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPositive) RiskLow else RiskSuspicious
                )
            }
        }
        Text(
            text = desc,
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}
