package com.scamshield.core.ai.risk

import com.scamshield.core.domain.model.RiskLevel
import com.scamshield.core.domain.model.RiskScore
import com.scamshield.core.domain.model.ScamSignal
import com.scamshield.core.domain.model.ScamType
import kotlin.math.min
import kotlin.math.roundToInt

class RiskEngine(
    private val persistenceWindowMs: Long = 60_000L // 60-second rolling evidence window
) {
    private val signalHistory = mutableListOf<ScamSignal>()

    /**
     * Ingests newly extracted signals and computes an updated explainable RiskScore.
     */
    @Synchronized
    fun processSignals(
        newSignals: List<ScamSignal>,
        currentTimestamp: Long = System.currentTimeMillis()
    ): RiskScore {
        signalHistory.addAll(newSignals)
        
        // Discard signals older than persistenceWindowMs
        val cutoff = currentTimestamp - persistenceWindowMs
        signalHistory.removeAll { it.timestamp < cutoff }

        return evaluateSignals(signalHistory, currentTimestamp)
    }

    /**
     * Pure stateless evaluation of a given set of signals.
     */
    fun evaluateSignals(
        signals: List<ScamSignal>,
        currentTimestamp: Long = System.currentTimeMillis()
    ): RiskScore {
        if (signals.isEmpty()) {
            return RiskScore.SAFE.copy(timestamp = currentTimestamp)
        }

        // Group by ScamType to avoid infinite accumulation of a single repeated keyword
        val groupedByType = signals.groupBy { it.type }
        val distinctSignalTypes = groupedByType.keys

        var accumulatedScore = 0f
        var totalConfidenceWeight = 0f
        var confidenceSum = 0f

        for ((type, signalList) in groupedByType) {
            val highestConfSignal = signalList.maxByOrNull { it.confidence } ?: signalList.first()
            val baseWeight = type.baseWeight
            
            // Primary occurrence gets full weight scaled by confidence
            val typeScore = baseWeight * highestConfSignal.confidence
            accumulatedScore += typeScore

            // Minor repetition bonus (capped at +5 for repeated insistence)
            if (signalList.size > 1) {
                accumulatedScore += min(5f, (signalList.size - 1) * 2.5f)
            }

            confidenceSum += highestConfSignal.confidence
            totalConfidenceWeight += 1f
        }

        // Multi-vector synergy: Scammers rely on multi-stage coercion (Impersonation -> Threat -> Secrecy -> Money)
        val distinctCount = distinctSignalTypes.size
        val synergyBonus = when {
            distinctCount >= 5 -> 25f
            distinctCount >= 4 -> 18f
            distinctCount >= 3 -> 10f
            distinctCount >= 2 -> 4f
            else -> 0f
        }

        // Composite Digital-Arrest Escalation:
        // Detects the high-risk composite pattern: Authority + Legal Threat + Isolation (Secrecy or Confinement) + Financial / Credential Coercion.
        // Applies an evidence-based multi-tactic composite escalation bonus rather than an artificial hard-coded score floor.
        val hasAuthority = distinctSignalTypes.contains(ScamType.AUTHORITY_IMPERSONATION) ||
                distinctSignalTypes.contains(ScamType.POLICE_CBI_RBI_IMPERSONATION)
        val hasThreat = distinctSignalTypes.contains(ScamType.LEGAL_THREAT)
        val hasIsolation = distinctSignalTypes.contains(ScamType.SECRECY) ||
                distinctSignalTypes.contains(ScamType.VIDEO_CONFINEMENT)
        val hasFinancialPressure = distinctSignalTypes.contains(ScamType.FINANCIAL_REQUEST) ||
                distinctSignalTypes.contains(ScamType.SAFE_ACCOUNT_SCAM) ||
                distinctSignalTypes.contains(ScamType.CREDENTIAL_REQUEST)

        val isCompositeDigitalArrest = hasAuthority && hasThreat && hasIsolation && hasFinancialPressure
        val compositeEscalationBonus = if (isCompositeDigitalArrest) 12f else 0f

        val rawTotal = (accumulatedScore + synergyBonus + compositeEscalationBonus).coerceIn(0f, 100f)
        val finalScore = rawTotal.roundToInt()

        val avgConfidence = if (totalConfidenceWeight > 0f) {
            (confidenceSum / totalConfidenceWeight).coerceIn(0.5f, 1.0f)
        } else {
            0.5f
        }

        val level = RiskLevel.fromScore(finalScore)
        val explanation = generateExplanation(level, distinctSignalTypes.toList(), isCompositeDigitalArrest)

        return RiskScore(
            score = finalScore,
            confidence = avgConfidence,
            level = level,
            activeSignals = signals.distinctBy { it.type },
            explanation = explanation,
            timestamp = currentTimestamp
        )
    }

    /**
     * Resets internal rolling history.
     */
    @Synchronized
    fun reset() {
        signalHistory.clear()
    }

    private fun generateExplanation(
        level: RiskLevel,
        types: List<ScamType>,
        isDigitalArrest: Boolean
    ): String {
        if (types.isEmpty()) {
            return "No scam indicators detected. Call appears normal."
        }

        if (isDigitalArrest) {
            return "ALERT: Classic Digital Arrest scam signature detected. Impersonation of authority combined with arrest threats, secrecy demands, and money transfer instructions."
        }

        return when (level) {
            RiskLevel.CRITICAL -> {
                val primaryThreats = types.take(3).joinToString(", ") { it.displayName }
                "CRITICAL WARNING: High-confidence scam patterns detected ($primaryThreats). Do not transfer money, share OTPs, or stay on video calls."
            }
            RiskLevel.HIGH -> {
                val primaryThreats = types.take(2).joinToString(", ") { it.displayName }
                "HIGH RISK: Coercive tactics detected ($primaryThreats). Independent verification required before taking any action."
            }
            RiskLevel.SUSPICIOUS -> {
                val primaryThreat = types.firstOrNull()?.displayName ?: "Unusual behavior"
                "CAUTION: Suspicious conversational element ($primaryThreat). Stay vigilant and do not divulge personal credentials."
            }
            RiskLevel.LOW -> {
                "Low risk. Minor conversational cue observed (${types.firstOrNull()?.displayName}), but no coercive pattern established."
            }
        }
    }
}
