package com.scamshield.app.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scamshield.app.presentation.theme.*
import com.scamshield.core.domain.model.RiskLevel

@Composable
fun RiskMeter(
    score: Int,
    level: RiskLevel,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp
) {
    // Smooth animated progress fraction
    val animatedProgress by animateFloatAsState(
        targetValue = (score.coerceIn(0, 100) / 100f),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "riskProgress"
    )

    // Smooth animated number counting
    val animatedScore by animateIntAsState(
        targetValue = score.coerceIn(0, 100),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "riskScoreNumber"
    )

    val targetColor = when (level) {
        RiskLevel.CRITICAL -> RiskCritical
        RiskLevel.HIGH -> RiskHigh
        RiskLevel.SUSPICIOUS -> RiskSuspicious
        RiskLevel.LOW -> RiskLow
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 400),
        label = "riskColor"
    )

    val badgeBg = when (level) {
        RiskLevel.CRITICAL -> RiskCriticalLight
        RiskLevel.HIGH -> RiskHighLight
        RiskLevel.SUSPOSIOUS_FALLBACK() -> RiskSuspiciousLight
        RiskLevel.LOW -> RiskLowLight
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = 12.dp.toPx()
            
            // Clean neutral track
            drawArc(
                color = BorderLight,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Animated semantic progress
            if (animatedProgress > 0f) {
                drawArc(
                    color = animatedColor,
                    startAngle = 135f,
                    sweepAngle = 270f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$animatedScore",
                color = TextPrimary,
                fontSize = 50.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = "out of 100",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(badgeBg)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = level.label,
                    color = animatedColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

private fun RiskLevel.Companion.SUSPOSIOUS_FALLBACK() = RiskLevel.SUSPICIOUS
