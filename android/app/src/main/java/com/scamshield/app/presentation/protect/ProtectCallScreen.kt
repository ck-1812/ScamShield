package com.scamshield.app.presentation.protect

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.scamshield.app.presentation.components.SpeakerphoneNotice
import com.scamshield.app.presentation.theme.*

@Composable
fun ProtectCallScreen(
    onStartActiveProtection: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var permissionDeniedExplanation by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            onStartActiveProtection()
        } else {
            permissionDeniedExplanation = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBgPrimary)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top App Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                text = "Protect a call",
                fontSize = 17.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        // Center Content Checklist
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Preparation Checklist",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "ScamShield analyses the sound picked up by your phone microphone while speakerphone is enabled.",
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            // Step Cards (Section 14)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LightSurface)
                    .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StepItem(step = "STEP 1", title = "Enable speakerphone", desc = "Turn on device speaker during the call.")
                StepItem(step = "STEP 2", title = "Grant microphone access", desc = "Allow audio analysis when prompted.")
                StepItem(step = "STEP 3", title = "Start protection", desc = "ScamShield monitors for scam patterns.")
            }

            // Mandatory Speakerphone notice
            SpeakerphoneNotice()

            if (permissionDeniedExplanation) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(RiskCriticalLight)
                        .border(1.dp, RiskCriticalBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Microphone permission is required to analyze call acoustics locally. Please allow microphone access to proceed.",
                        color = RiskCritical,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // Privacy Statement
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "No raw audio is sent to the dashboard.",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        // Action Button: [ ALLOW MICROPHONE ] or [ PROTECT THIS CALL ]
        Button(
            onClick = {
                if (hasMicPermission) {
                    onStartActiveProtection()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
        ) {
            Icon(
                imageVector = if (hasMicPermission) Icons.Rounded.Shield else Icons.Rounded.Mic,
                contentDescription = null,
                tint = LightSurface,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (hasMicPermission) "PROTECT THIS CALL" else "ALLOW MICROPHONE",
                color = LightSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun StepItem(step: String, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(LightSurfaceSecondary)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = step,
                color = PrimaryAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(text = desc, color = TextSecondary, fontSize = 12.sp)
        }
    }
}
