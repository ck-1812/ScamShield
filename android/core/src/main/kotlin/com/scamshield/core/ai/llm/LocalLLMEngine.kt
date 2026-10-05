package com.scamshield.core.ai.llm

import com.scamshield.core.domain.model.RiskScore
import com.scamshield.core.domain.model.ScamSignal

interface LocalLLMEngine {
    fun analyzeConversationContext(transcriptWindow: String, detectedSignals: List<ScamSignal>): String
    fun generateExplanation(score: RiskScore): String
    fun summarizeEvidence(signals: List<ScamSignal>): String
    fun isMock(): Boolean
}
