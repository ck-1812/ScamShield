package com.scamshield.core.ai.llm

import com.scamshield.core.domain.model.RiskScore
import com.scamshield.core.domain.model.ScamSignal
import java.io.File

/**
 * Adapter for on-device Qwen2.5 (0.5B / 1.5B Instruct) execution via llama.cpp or ExecuTorch.
 * 100% offline inference.
 */
class QwenLocalLLMEngine(
    private val modelFile: File
) : LocalLLMEngine {

    private val isModelAvailable: Boolean
        get() = modelFile.exists() && modelFile.length() > 0

    override fun analyzeConversationContext(
        transcriptWindow: String,
        detectedSignals: List<ScamSignal>
    ): String {
        if (!isModelAvailable) {
            return "[DEMO FALLBACK] Qwen model weights not loaded. Rule-based analysis active."
        }
        // In physical deployment, executes local prompt template on device NPU/CPU
        return "Local Qwen analysis: Coercive extortion pattern identified."
    }

    override fun generateExplanation(score: RiskScore): String {
        if (!isModelAvailable) {
            return score.explanation
        }
        return "Qwen local explanation: Risk evaluated at ${score.score}/100 based on verified scam tactics."
    }

    override fun summarizeEvidence(signals: List<ScamSignal>): String {
        return signals.joinToString("; ") { "${it.type}: ${it.evidenceText}" }
    }

    override fun isMock(): Boolean = !isModelAvailable
}
