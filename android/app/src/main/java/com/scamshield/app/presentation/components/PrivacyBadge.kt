package com.scamshield.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scamshield.app.presentation.theme.*

@Composable
fun PrivacyBadge(
    modifier: Modifier = Modifier,
    isMicActive: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LightSurface)
            .border(1.dp, BorderLight, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusColumn(label = "Microphone", value = if (isMicActive) "Active" else "Off", isHighlight = isMicActive)
        StatusColumn(label = "Processing", value = "On-Device", isHighlight = true)
        StatusColumn(label = "Network", value = "Not Required", isHighlight = true)
    }
}

@Composable
private fun StatusColumn(label: String, value: String, isHighlight: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextMuted,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            color = if (isHighlight) RiskLow else TextSecondary,
            fontWeight = FontWeight.Bold
        )
    }
}
