package com.scamshield.app.presentation.demo

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scamshield.app.audio.AudioCaptureService
import com.scamshield.app.audio.AudioPlaybackManager
import com.scamshield.app.presentation.components.HighRiskWarningDialog
import com.scamshield.app.presentation.components.RiskMeter
import com.scamshield.app.presentation.theme.*
import com.scamshield.core.ai.risk.RiskEngine
import com.scamshield.core.ai.signal.ScamSignalExtractor
import com.scamshield.core.demo.DemoScenario
import com.scamshield.core.demo.ScamScriptRepository
import com.scamshield.core.domain.model.RiskLevel
import com.scamshield.core.domain.model.RiskScore
import com.scamshield.core.domain.model.ScamSignal
import com.scamshield.core.domain.model.TranscriptSegment
import com.scamshield.core.privacy.RollingTranscriptBuffer
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SynchronizedTurnResult(
    val turnNumber: Int,
    val segment: TranscriptSegment,
    val signals: List<ScamSignal>,
    val scoreDelta: Int,
    val newScore: Int,
    val timeLabel: String,
    val evidenceMapping: List<Pair<String, String>>
)

@Composable
fun DemoModeScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedScenario by remember { mutableStateOf(ScamScriptRepository.digitalArrestScenario) }
    var currentRiskScore by remember { mutableStateOf(RiskScore.SAFE) }
    var synchronizedTurns by remember { mutableStateOf(listOf<SynchronizedTurnResult>()) }
    var isRunning by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var demoJob by remember { mutableStateOf<Job?>(null) }
    var showCriticalAlert by remember { mutableStateOf(false) }
    var activeSpeakingTurn by remember { mutableStateOf<Int?>(null) }
    var showDiagnostics by remember { mutableStateOf(false) }
    var sessionPurgedNotice by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Live Transcript, 1: Timeline

    // Core engines
    val signalExtractor = remember { ScamSignalExtractor() }
    val riskEngine = remember { RiskEngine() }
    val transcriptBuffer = remember { RollingTranscriptBuffer() }

    // Hardware Audio Services (Real Speaker Playback + Real Mic Capture)
    val audioCaptureService = remember { AudioCaptureService(context) }
    val playbackManager = remember { AudioPlaybackManager(context, coroutineScope) }

    val rawAmplitude by audioCaptureService.amplitude.collectAsState()
    val waveformBars by audioCaptureService.waveformBars.collectAsState()
    val isInputDetected by audioCaptureService.isInputDetected.collectAsState()
    val formattedProgress by playbackManager.formattedProgress.collectAsState()
    val isVoicePlaying by playbackManager.isPlaying.collectAsState()

    val inputPercent = (rawAmplitude * 100).toInt().coerceIn(0, 100)
    val dbfsApprox = (-60 + (rawAmplitude * 55)).toInt()

    DisposableEffect(Unit) {
        onDispose {
            playbackManager.stop()
            audioCaptureService.stopCapture()
            demoJob?.cancel()
        }
    }

    fun stopSimulation() {
        demoJob?.cancel()
        demoJob = null
        playbackManager.stop()
        audioCaptureService.stopCapture()
        isRunning = false
        isPaused = false
        activeSpeakingTurn = null
        riskEngine.reset()
        transcriptBuffer.clear()
        sessionPurgedNotice = true
    }

    fun startSimulation(scenario: DemoScenario) {
        stopSimulation()
        sessionPurgedNotice = false
        isRunning = true
        isPaused = false
        currentRiskScore = RiskScore.SAFE
        synchronizedTurns = emptyList()
        showCriticalAlert = false

        // 1. Begin real microphone capture using AudioRecord (MediaRecorder.AudioSource.MIC)
        audioCaptureService.startCapture(coroutineScope)

        // 2. Begin sequential audible caller playback through device speaker
        var prevScore = 0
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        demoJob = coroutineScope.launch {
            playbackManager.playScenario(
                scenarioId = scenario.scenarioId,
                onTurnStart = { turnIdx ->
                    activeSpeakingTurn = turnIdx + 1
                },
                onTurnEnd = { turnIdx ->
                    // Synchronized: Statement finished playing aloud in speaker
                    activeSpeakingTurn = null
                    if (turnIdx < scenario.turns.size) {
                        val turn = scenario.turns[turnIdx]
                        val timestamp = System.currentTimeMillis()
                        val segment = TranscriptSegment(
                            text = turn.text,
                            speaker = turn.speaker,
                            timestamp = timestamp
                        )
                        transcriptBuffer.addSegment(segment, timestamp)

                        val detectedSignals = signalExtractor.extractSignals(turn.text, timestamp)
                        val updatedScore = riskEngine.processSignals(detectedSignals, timestamp)

                        val delta = (updatedScore.score - prevScore).coerceAtLeast(0)
                        prevScore = updatedScore.score

                        // Map evidence phrases to tactics for explainable UI (Section 12)
                        val evidenceList = detectedSignals.map { sig ->
                            Pair(sig.evidenceSnippet.ifEmpty { sig.type.displayName }, sig.type.displayName)
                        }

                        val result = SynchronizedTurnResult(
                            turnNumber = turnIdx + 1,
                            segment = segment,
                            signals = detectedSignals,
                            scoreDelta = delta,
                            newScore = updatedScore.score,
                            timeLabel = timeFormat.format(Date(timestamp)),
                            evidenceMapping = evidenceList
                        )

                        currentRiskScore = updatedScore
                        synchronizedTurns = synchronizedTurns + result

                        if (updatedScore.level == RiskLevel.CRITICAL) {
                            showCriticalAlert = true
                        }
                    }
                },
                onComplete = {
                    isRunning = false
                    activeSpeakingTurn = null
                }
            )
        }
    }

    fun togglePause() {
        if (isPaused) {
            playbackManager.resume()
            isPaused = false
        } else {
            playbackManager.pause()
            isPaused = true
        }
    }

    if (showCriticalAlert && currentRiskScore.level == RiskLevel.CRITICAL) {
        HighRiskWarningDialog(
            score = currentRiskScore.score,
            signals = currentRiskScore.activeSignals,
            onDismiss = { showCriticalAlert = false },
            onHangUp = {
                showCriticalAlert = false
                stopSimulation()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBgPrimary)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // App Bar & Status Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                stopSimulation()
                onBack()
            }) {
                Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                // Audio Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isVoicePlaying) RiskSuspiciousLight else LightSurfaceSecondary)
                        .border(1.dp, if (isVoicePlaying) RiskSuspiciousBorder else BorderLight, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isVoicePlaying) "DEMO SIMULATION · VOICE ACTIVE" else "DEMO SIMULATION",
                        color = if (isVoicePlaying) RiskSuspicious else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                // Microphone Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isRunning) SuccessGreenLight else LightSurfaceSecondary)
                        .border(1.dp, if (isRunning) SuccessGreenBorder else BorderLight, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isRunning) "MIC · LISTENING" else "MIC · IDLE",
                        color = if (isRunning) SuccessGreen else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Scenario Selector Chips (Section 16, 17, 18)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ScenarioChipLight(
                title = "Digital Arrest (CBI)",
                isSelected = selectedScenario.scenarioId == ScamScriptRepository.digitalArrestScenario.scenarioId,
                modifier = Modifier.weight(1.1f),
                onClick = {
                    selectedScenario = ScamScriptRepository.digitalArrestScenario
                    stopSimulation()
                }
            )
            ScenarioChipLight(
                title = "Police (Hindi)",
                isSelected = selectedScenario.scenarioId == ScamScriptRepository.fakePoliceHindiScenario.scenarioId,
                modifier = Modifier.weight(1f),
                onClick = {
                    selectedScenario = ScamScriptRepository.fakePoliceHindiScenario
                    stopSimulation()
                }
            )
            ScenarioChipLight(
                title = "Legit Call (Bank)",
                isSelected = selectedScenario.scenarioId == ScamScriptRepository.legitBankScenario.scenarioId,
                modifier = Modifier.weight(1.1f),
                onClick = {
                    selectedScenario = ScamScriptRepository.legitBankScenario
                    stopSimulation()
                }
            )
        }

        // Audio Path Explanation Card (Section 6)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(LightSurfaceSecondary)
                .border(1.dp, BorderLight, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AUDIO PATH: Caller Voice → Phone Speaker → Room Sound → Microphone → Analysis",
                    fontSize = 9.5.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Live Risk Meter Card (Section 8 & 9)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(LightSurface)
                .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RiskMeter(
                    score = currentRiskScore.score,
                    level = currentRiskScore.level,
                    size = 130.dp
                )

                // Explainable score update badge (Section 9)
                if (synchronizedTurns.isNotEmpty()) {
                    val lastTurn = synchronizedTurns.last()
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (lastTurn.scoreDelta > 0) RiskSuspiciousLight else SuccessGreenLight)
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (lastTurn.scoreDelta > 0) "+${lastTurn.scoreDelta} pts (${lastTurn.signals.firstOrNull()?.type?.displayName ?: "Risk indicator"})" else "Score stable (No threats)",
                            color = if (lastTurn.scoreDelta > 0) RiskSuspicious else SuccessGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // LIVE CALL ANALYSIS & REAL AUDIO MONITOR (Section 5)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(LightSurface)
                .border(1.dp, BorderLight, RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE CALL ANALYSIS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isVoicePlaying) SuccessGreen else TextMuted)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Speaker", fontSize = 10.sp, color = TextSecondary)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isRunning) PrimaryAccent else TextMuted)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Mic", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }

                // Audio Input Percentage Bar (Section 5)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AUDIO INPUT",
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$inputPercent% ($dbfsApprox dBFS)",
                        fontSize = 10.sp,
                        color = if (isInputDetected) PrimaryAccent else TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                }

                LinearProgressIndicator(
                    progress = { rawAmplitude.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isInputDetected) PrimaryAccent else TextMuted,
                    trackColor = LightSurfaceSecondary
                )

                // Dynamic Waveform driven by actual microphone PCM amplitude
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(LightSurfaceSecondary)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    waveformBars.forEach { barHeight ->
                        val animatedHeight by animateFloatAsState(targetValue = (barHeight * 24f).coerceIn(3f, 24f), label = "waveform")
                        val barColor by animateColorAsState(
                            targetValue = if (isInputDetected) PrimaryAccent else TextMuted,
                            label = "barColor"
                        )
                        Box(
                            modifier = Modifier
                                .width(3.5.dp)
                                .height(animatedHeight.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(barColor)
                        )
                    }
                }

                // Input State Indicator & Diagnostic toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isInputDetected) {
                        Text(
                            text = "● INPUT DETECTED",
                            color = SuccessGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = if (isRunning) "LOW AUDIO INPUT — Check speaker volume" else "AUDIO MONITOR IDLE",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (showDiagnostics) "Hide diag ▲" else "Diag ▼",
                            fontSize = 10.sp,
                            color = PrimaryAccent,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { showDiagnostics = !showDiagnostics }
                        )

                        Text(
                            text = formattedProgress,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                // Expandable Hardware Diagnostics (Section 7)
                if (showDiagnostics) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(LightSurfaceSecondary)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        DiagRow(label = "Audio source", value = "Microphone (AudioRecord PCM)")
                        DiagRow(label = "Sample rate", value = "16,000 Hz (16 kHz mono)")
                        DiagRow(label = "Input level", value = "$dbfsApprox dBFS (RMS: $inputPercent%)")
                        DiagRow(label = "Input detected", value = if (isInputDetected) "YES (Acoustic audio present)" else "NO (Silence)")
                        DiagRow(label = "Playback path", value = if (isVoicePlaying) "ACTIVE (Device Speaker USAGE_MEDIA)" else "IDLE")
                    }
                }
            }
        }

        // Session Purged Notice (Section 23)
        if (sessionPurgedNotice && !isRunning) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SuccessGreenLight)
                    .padding(vertical = 6.dp, horizontal = 10.dp)
            ) {
                Text(
                    text = "SESSION ENDED: Temporary session data cleared. Protection is OFF.",
                    color = SuccessGreen,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Tab Navigation: Live Transcript vs Timeline (Section 10 & 11)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                label = { Text("LIVE TRANSCRIPT (${synchronizedTurns.size})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                label = { Text("DETECTION TIMELINE", fontSize = 11.sp) }
            )
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(LightSurface)
                .border(1.dp, BorderLight, RoundedCornerShape(14.dp))
                .padding(10.dp)
        ) {
            if (synchronizedTurns.isEmpty() && activeSpeakingTurn == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Tap 'START LIVE DEMO' to play audible scammer voice.",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            } else if (selectedTab == 0) {
                // LIVE TRANSCRIPT STREAM (Section 11 & 12)
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(synchronizedTurns) { item ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(LightSurfaceSecondary)
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.segment.speaker,
                                                color = PrimaryAccent,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(LightSurface)
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "DEMO STT",
                                                    color = TextMuted,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Text(text = item.timeLabel, fontSize = 10.sp, color = TextMuted)
                                    }

                                    Text(
                                        text = "\"${item.segment.text}\"",
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )

                                    // Matched evidence keywords linked beside transcript (Section 12)
                                    if (item.evidenceMapping.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            item.evidenceMapping.forEach { (keyword, tactic) ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(RiskCriticalLight)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "$keyword → $tactic",
                                                        color = RiskCritical,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Turn speaking indicator
                    if (activeSpeakingTurn != null) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PrimaryAccentLight)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "🔊 Caller speaking Turn $activeSpeakingTurn out loud through speaker...",
                                    color = PrimaryAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            } else {
                // DETECTION TIMELINE (Section 10)
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(synchronizedTurns) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (item.signals.isNotEmpty()) RiskCritical else SuccessGreen)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (item.signals.isNotEmpty()) item.signals.joinToString(", ") { it.type.displayName } else "Normal conversational greeting",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Turn ${item.turnNumber} • ${item.timeLabel} • Risk: ${item.newScore}/100",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    if (currentRiskScore.level == RiskLevel.CRITICAL) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(RiskCritical)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "🚨 CRITICAL INTERVENTION — System advised: DO NOT TRANSFER MONEY",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RiskCritical
                                )
                            }
                        }
                    }
                }
            }
        }

        // Demo Controls: Play / Pause / Stop (Section 15)
        if (!isRunning) {
            Button(
                onClick = { startSimulation(selectedScenario) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
            ) {
                Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null, tint = LightSurface, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "START LIVE DEMO (PLAY VOICE)",
                    color = LightSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { togglePause() },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isPaused) "▶ RESUME" else "Ⅱ PAUSE",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = { stopSimulation() },
                    modifier = Modifier
                        .weight(1.2f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RiskCritical)
                ) {
                    Icon(imageVector = Icons.Rounded.Stop, contentDescription = null, tint = LightSurface, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "STOP SIMULATION",
                        color = LightSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 9.sp, color = TextMuted)
        Text(text = value, fontSize = 9.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ScenarioChipLight(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) PrimaryAccentLight else LightSurface)
            .border(
                1.dp,
                if (isSelected) PrimaryAccent else BorderLight,
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 10.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) PrimaryAccent else TextSecondary,
            maxLines = 1
        )
    }
}
