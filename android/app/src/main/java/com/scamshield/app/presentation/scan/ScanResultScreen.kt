package com.scamshield.app.presentation.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scamshield.app.presentation.theme.*
import com.scamshield.core.domain.model.RiskLevel
import com.scamshield.core.domain.model.ScamLensResult

@Composable
fun ScanResultScreen(
    result: ScamLensResult,
    onBackToScan: () -> Unit,
    onHome: () -> Unit
) {
    val isCritical = result.riskScore.level == RiskLevel.CRITICAL
    val accentColor = when (result.riskScore.level) {
        RiskLevel.CRITICAL -> RiskCritical
        RiskLevel.HIGH -> RiskHigh
        RiskLevel.SUSPICIOUS -> RiskSuspicious
        RiskLevel.LOW -> RiskLow
    }

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
            IconButton(onClick = onBackToScan) {
                Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(
                text = "RISK ASSESSMENT",
                fontSize = 13.sp,
                color = TextMuted,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        // Risk Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(LightSurface)
                .border(1.5.dp, if (isCritical) RiskCriticalBorder else BorderLight, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ASSESSED RISK SCORE",
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = result.riskScore.level.label,
                        color = accentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "${result.riskScore.score} / 100",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = result.riskScore.explanation,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        // Extracted Scam Tactics Checklist
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
                text = "IDENTIFIED FRAUD PATTERNS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted
            )

            result.detectedTactics.forEach { signal ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = signal.type.displayName,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "+${signal.severity} pts",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Action Recommendation Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RiskSuspiciousLight)
                .border(1.dp, RiskSuspiciousBorder, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Recommendation",
                    color = RiskSuspicious,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = result.recommendedAction,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Button(
            onClick = onHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
        ) {
            Text("Done / Return Home", color = LightSurface, fontWeight = FontWeight.Bold)
        }
    }
}
