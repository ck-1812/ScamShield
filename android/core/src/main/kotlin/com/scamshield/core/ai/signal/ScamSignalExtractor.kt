package com.scamshield.core.ai.signal

import com.scamshield.core.domain.model.ScamSignal
import com.scamshield.core.domain.model.ScamType
import java.util.UUID

class ScamSignalExtractor(
    private val rules: List<RulePattern> = IndianScamRules.rules
) {
    /**
     * Extracts scam signals from an incoming text segment.
     * Checks against Indian scam taxonomies and extracts the matched evidence snippet.
     */
    fun extractSignals(text: String, timestamp: Long = System.currentTimeMillis()): List<ScamSignal> {
        if (text.isBlank()) return emptyList()

        val detected = mutableListOf<ScamSignal>()
        val matchedTypes = mutableSetOf<ScamType>()

        for (rule in rules) {
            val match = rule.regex.find(text)
            if (match != null && !matchedTypes.contains(rule.type)) {
                matchedTypes.add(rule.type)
                
                // Extract surrounding context snippet (up to 60 characters)
                val start = (match.range.first - 15).coerceAtLeast(0)
                val end = (match.range.last + 15).coerceAtMost(text.length)
                val evidence = text.substring(start, end).trim()

                detected.add(
                    ScamSignal(
                        id = UUID.randomUUID().toString(),
                        type = rule.type,
                        confidence = rule.confidence,
                        severity = rule.baseSeverity,
                        evidenceText = if (evidence.isNotEmpty()) evidence else match.value,
                        timestamp = timestamp
                    )
                )
            }
        }

        return detected
    }
}
