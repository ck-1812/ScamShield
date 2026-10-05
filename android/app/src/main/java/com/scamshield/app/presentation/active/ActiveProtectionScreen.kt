package com.scamshield.app.presentation.active

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scamshield.app.presentation.components.HighRiskWarningDialog
import com.scamshield.app.presentation.components.PrivacyBadge
import com.scamshield.app.presentation.components.RiskMeter
import com.scamshield.app.presentation.theme.*
import com.scamshield.core.domain.model.RiskLevel
import com.scamshield.core.domain.model.RiskScore
import com.scamshield.core.domain.model.ScamSignal
import com.scamshield.core.domain.model.TranscriptSegment

@Composable
fun ActiveProtectionScreen(
    riskScore: RiskScore,
    recentTranscript: List<TranscriptSegment>,
    onStopProtection: () -> Unit
) {
    var showCriticalDialog by remember { mutableStateOf(false) }

    LaunchedEffect(riskScore.score) {
        if (riskScore.level == RiskLevel.CRITICAL) {
            showCriticalDialog = true
        }
    }

    if (showCriticalDialog && riskScore.level == RiskLevel.CRITICAL) {
        HighRiskWarningDialog(
            score = riskScore.score,
            signals = riskScore.activeSignals,
            onDismiss = { showCriticalDialog = false },
            onHangUp = {
                showCriticalDialog = false
                onStopProtection()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBgPrimary)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CALL PROTECTION",
                    fontSize = 14.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(RiskLowLight)
                        .border(1.dp, RiskLowBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(RiskLow)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PROTECTION ACTIVE",
                        color = RiskLow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Real-time Privacy Badge
            PrivacyBadge(isMicActive = true)
        }

        // Main Central Risk Card (Section 15)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(LightSurface)
                .border(1.dp, BorderLight, RoundedCornerShape(20.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RiskMeter(
                    score = riskScore.score,
                    level = riskScore.level,
                    size = 180.dp
                )

                Text(
                    text = riskScore.level.description,
                    color = when (riskScore.level) {
                        RiskLevel.CRITICAL -> RiskCritical
                        RiskLevel.HIGH -> RiskHigh
                        RiskLevel.SUSPICIOUS -> RiskSuspicious
                        RiskLevel.LOW -> RiskLow
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Live Signals Drawer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(LightSurface)
                .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE SIGNALS (${riskScore.activeSignals.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Rolling 45s",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            if (riskScore.activeSignals.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Listening for suspicious conversation patterns...",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(riskScore.activeSignals) { signal ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically()
                        ) {
                            SignalCardLight(signal)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // STOP Button: [ STOP PROTECTION ] (Section 15)
        Button(
            onClick = onStopProtection,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RiskCritical)
        ) {
            Icon(
                imageVector = Icons.Rounded.Stop,
                contentDescription = null,
                tint = LightSurface,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "STOP PROTECTION",
                color = LightSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun SignalCardLight(signal: ScamSignal) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(LightSurfaceSecondary)
            .border(1.dp, BorderLight, RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = signal.type.displayName,
                color = RiskCritical,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "\"${signal.evidenceText}\"",
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(RiskCriticalLight)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = "+${signal.severity}",
                color = RiskCritical,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}
