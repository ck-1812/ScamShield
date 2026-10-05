package com.scamshield.app.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Speed
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
fun OnboardingScreen(
    onGetStarted: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBgPrimary)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp)
        ) {
            Text(
                text = "SCAMSHIELD",
                color = PrimaryAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Private safety layer for suspicious calls.",
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 34.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "On-demand conversational intelligence to alert you of coercion, digital arrest threats, and payment demands.",
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OnboardingCard(
                icon = Icons.Rounded.PowerSettingsNew,
                title = "ON-DEMAND",
                body = "Protection activates only when you choose it."
            )
            OnboardingCard(
                icon = Icons.Rounded.Speed,
                title = "LOCAL-FIRST",
                body = "Detection is designed to run on your device whenever possible."
            )
            OnboardingCard(
                icon = Icons.Rounded.Lock,
                title = "PRIVATE BY DEFAULT",
                body = "ScamShield does not intentionally upload your call audio."
            )
        }

        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
        ) {
            Text(
                text = "Get Started",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = LightSurface
            )
        }
    }
}

@Composable
private fun OnboardingCard(
    icon: ImageVector,
    title: String,
    body: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LightSurface)
            .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(LightSurfaceSecondary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryAccent,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = body,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 17.sp
            )
        }
    }
}
