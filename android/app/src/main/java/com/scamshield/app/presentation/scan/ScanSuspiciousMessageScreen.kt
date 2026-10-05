package com.scamshield.app.presentation.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scamshield.app.presentation.theme.*

@Composable
fun ScanSuspiciousMessageScreen(
    onAnalyzeText: (String) -> Unit,
    onBack: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    val sampleNotices = listOf(
        "CBI NOTICE:\nYour Aadhaar has been linked to an illegal parcel. Transfer ₹2,00,000 immediately to safe RBI verification account or non-bailable arrest warrant will be executed.",
        "URGENT ELECTRICITY ALERT:\nDear consumer, your electricity connection will be disconnected tonight at 9:30 PM due to pending bill. Call electricity officer immediately at 9876543210 or install AnyDesk.",
        "BANK KYC WARNING:\nDear SBI Customer, your bank account is blocked. Click http://sbi-kyc-update.apk to download verification app and enter your UPI PIN right now."
    )

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
                text = "Scan Suspicious Message",
                fontSize = 17.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Text(
            text = "ScamLens Risk Assessment",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = "Paste text from letters, SMS, or WhatsApp notices to extract suspicious scam patterns on-device.",
            color = TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

        // Text input area
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            placeholder = { Text("Paste letter text, SMS, or WhatsApp notice here...", color = TextMuted) },
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryAccent,
                unfocusedBorderColor = BorderLight,
                focusedContainerColor = LightSurface,
                unfocusedContainerColor = LightSurface
            )
        )

        Button(
            onClick = {
                if (inputText.isNotBlank()) {
                    onAnalyzeText(inputText)
                }
            },
            enabled = inputText.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
        ) {
            Icon(imageVector = Icons.Rounded.DocumentScanner, contentDescription = null, tint = LightSurface, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Analyze Message", color = LightSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Preset Sample Notices for Quick Demo
        Text(
            text = "OR CHOOSE A KNOWN SCAM SAMPLE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted
        )

        sampleNotices.forEachIndexed { index, sample ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(LightSurface)
                    .border(1.dp, BorderLight, RoundedCornerShape(14.dp))
                    .clickable {
                        inputText = sample
                        onAnalyzeText(sample)
                    }
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = when (index) {
                            0 -> "Fake CBI / Digital Arrest Notice"
                            1 -> "Electricity Power Cutoff Threat"
                            else -> "Bank Account Freeze APK Phishing"
                        },
                        color = RiskSuspicious,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = sample,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
