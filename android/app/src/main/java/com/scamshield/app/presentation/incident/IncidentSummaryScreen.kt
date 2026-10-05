package com.scamshield.app.presentation.incident

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scamshield.app.presentation.theme.*
import com.scamshield.core.domain.model.ProtectionSession
import com.scamshield.core.domain.model.RiskLevel

@Composable
fun IncidentSummaryScreen(
    session: ProtectionSession,
    onDeleteSession: () -> Unit,
    onSaveSummary: (notes: String) -> Unit,
    onDone: () -> Unit
) {
    var userNotes by remember { mutableStateOf("") }
    var hasSaved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SCAMSHIELD INCIDENT",
            fontSize = 13.sp,
            color = TextMuted,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        // Peak Risk Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(LightSurface)
                .border(
                    1.dp,
                    if (session.peakRiskScore.level == RiskLevel.CRITICAL) RiskCriticalBorder else BorderLight,
                    RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RISK ASSESSMENT",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = session.peakRiskScore.level.label,
                        color = if (session.peakRiskScore.level == RiskLevel.CRITICAL) RiskCritical else RiskHigh,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Text(
                    text = "${session.peakRiskScore.score} / 100",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = session.peakRiskScore.explanation,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        // Detected Tactics Checklist (Section 31)
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
                text = "DETECTED TACTICS",
                fontSize = 12.sp,
                color = TextMuted,
                fontWeight = FontWeight.Bold
            )

            if (session.signals.isEmpty()) {
                Text("✓ No threat patterns identified during this call.", color = RiskLow, fontSize = 13.sp)
            } else {
                session.signals.distinctBy { it.type }.forEach { sig ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = RiskCritical,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = sig.type.displayName,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Detection Timeline (Section 10)
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
                text = "DETECTION TIMELINE",
                fontSize = 12.sp,
                color = TextMuted,
                fontWeight = FontWeight.Bold
            )

            if (session.signals.isEmpty()) {
                Text("No risk escalation events logged.", color = TextMuted, fontSize = 12.sp)
            } else {
                session.signals.forEachIndexed { idx, sig ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(RiskCritical)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sig.type.displayName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Turn ${idx + 1} • Confidence: ${(sig.confidence * 100).toInt()}% • Evidence: \"${sig.evidenceSnippet.ifEmpty { "Verbal pattern" }}\"",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                if (session.peakRiskScore.level == RiskLevel.CRITICAL) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(RiskCritical)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "CRITICAL INTERVENTION • Final safety assessment triggered",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = RiskCritical
                        )
                    }
                }
            }
        }

        // Safety Recommendation Card (Section 15)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RiskCriticalLight)
                .border(1.dp, RiskCriticalBorder, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "RECOMMENDED ACTION",
                    color = RiskCritical,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = "Do not transfer money or share sensitive credentials. End the conversation and verify the caller independently through official public channels.",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        // Optional Notes
        OutlinedTextField(
            value = userNotes,
            onValueChange = { userNotes = it },
            label = { Text("Incident notes (opt-in)") },
            placeholder = { Text("e.g. Caller claimed to be Inspector Sharma", color = TextMuted) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryAccent,
                unfocusedBorderColor = BorderLight,
                focusedContainerColor = LightSurface,
                unfocusedContainerColor = LightSurface
            )
        )

        // Action Buttons: [ DELETE SESSION ] & [ SAVE SUMMARY ]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onDeleteSession,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RiskCritical)
            ) {
                Icon(imageVector = Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Delete session", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    onSaveSummary(userNotes)
                    hasSaved = true
                },
                enabled = !hasSaved,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
            ) {
                Icon(imageVector = Icons.Rounded.Save, contentDescription = null, tint = LightSurface, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (hasSaved) "Saved ✓" else "Save summary",
                    color = LightSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LightSurfaceSecondary)
        ) {
            Text("Done / Return Home", color = TextPrimary, fontWeight = FontWeight.Medium)
        }
    }
}
