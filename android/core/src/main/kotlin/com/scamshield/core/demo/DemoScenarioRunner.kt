package com.scamshield.core.demo

import com.scamshield.core.ai.risk.RiskEngine
import com.scamshield.core.ai.signal.ScamSignalExtractor
import com.scamshield.core.domain.model.RiskScore
import com.scamshield.core.domain.model.ScamSignal
import com.scamshield.core.domain.model.TranscriptSegment
import com.scamshield.core.privacy.RollingTranscriptBuffer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class DemoStepResult(
    val turn: DemoTurn,
    val segment: TranscriptSegment,
    val newSignals: List<ScamSignal>,
    val currentRiskScore: RiskScore
)

class DemoScenarioRunner(
    private val signalExtractor: ScamSignalExtractor = ScamSignalExtractor(),
    private val riskEngine: RiskEngine = RiskEngine(),
    private val transcriptBuffer: RollingTranscriptBuffer = RollingTranscriptBuffer()
) {

    /**
     * Executes a demo scenario turn-by-turn as a cold Flow with configurable delays.
     */
    fun runScenario(
        scenario: DemoScenario,
        applyDelay: Boolean = true
    ): Flow<DemoStepResult> = flow {
        riskEngine.reset()
        transcriptBuffer.clear()

        for (turn in scenario.turns) {
            if (applyDelay && turn.delayMs > 0) {
                delay(turn.delayMs)
            }

            val timestamp = System.currentTimeMillis()
            val segment = TranscriptSegment(
                text = turn.text,
                speaker = turn.speaker,
                timestamp = timestamp
            )
            transcriptBuffer.addSegment(segment, timestamp)

            val detectedSignals = signalExtractor.extractSignals(turn.text, timestamp)
            val updatedScore = riskEngine.processSignals(detectedSignals, timestamp)

            emit(
                DemoStepResult(
                    turn = turn,
                    segment = segment,
                    newSignals = detectedSignals,
                    currentRiskScore = updatedScore
                )
            )
        }
    }
}
