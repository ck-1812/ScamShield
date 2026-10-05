package com.scamshield.core.ai.llm

import com.scamshield.core.domain.model.RiskLevel
import com.scamshield.core.domain.model.RiskScore
import com.scamshield.core.domain.model.ScamSignal
import com.scamshield.core.domain.model.ScamType

class MockLocalLLMEngine : LocalLLMEngine {

    override fun analyzeConversationContext(
        transcriptWindow: String,
        detectedSignals: List<ScamSignal>
    ): String {
        val types = detectedSignals.map { it.type }.toSet()
        val hasArrest = types.contains(ScamType.LEGAL_THREAT)
        val hasCbi = types.contains(ScamType.AUTHORITY_IMPERSONATION)
        val hasMoney = types.contains(ScamType.FINANCIAL_REQUEST) || types.contains(ScamType.SAFE_ACCOUNT_SCAM)

        return when {
            hasCbi && hasArrest && hasMoney ->
                "The caller is systematically combining CBI impersonation with arrest threats and demands for financial transfer. This is a trademark Digital Arrest scam pattern."
            hasMoney ->
                "The caller is pressuring for money transfers under coercive pretenses."
            hasArrest ->
                "The caller is using aggressive threats of legal arrest to induce fear and compliance."
            else ->
                "Conversational patterns indicate potential social engineering tactics."
        }
    }

    override fun generateExplanation(score: RiskScore): String {
        return when (score.level) {
            RiskLevel.CRITICAL ->
                "CRITICAL: The conversation matches known Indian cyber extortion scripts. Authentic police or government officers never ask for money or hold people on video calls."
            RiskLevel.HIGH ->
                "HIGH RISK: Intimidation tactics and high-pressure demands detected. Verify the caller's credentials independently before taking any action."
            RiskLevel.SUSPICIOUS ->
                "SUSPICIOUS: The conversation exhibits red flags often seen in initial fraud stages. Stay cautious."
            RiskLevel.LOW ->
                "Normal conversation flow. No high-confidence scam markers observed."
        }
    }

    override fun summarizeEvidence(signals: List<ScamSignal>): String {
        if (signals.isEmpty()) return "No suspicious evidence identified."
        return signals.joinToString("; ") { signal ->
            "${signal.type.displayName}: \"${signal.evidenceText}\""
        }
    }

    override fun isMock(): Boolean = true
}
