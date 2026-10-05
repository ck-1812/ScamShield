package com.scamshield.core

import com.scamshield.core.ai.risk.RiskEngine
import com.scamshield.core.ai.signal.ScamSignalExtractor
import com.scamshield.core.domain.model.RiskLevel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FalsePositiveTest {

    private lateinit var riskEngine: RiskEngine
    private lateinit var extractor: ScamSignalExtractor

    @BeforeEach
    fun setUp() {
        riskEngine = RiskEngine()
        extractor = ScamSignalExtractor()
    }

    @Test
    fun `normal conversation 'How are you' must stay LOW`() {
        val signals = extractor.extractSignals("Hello, how are you doing today? Are we meeting for lunch?")
        val score = riskEngine.processSignals(signals)

        assertEquals(0, score.score)
        assertEquals(RiskLevel.LOW, score.level)
        assertTrue(score.activeSignals.isEmpty())
    }

    @Test
    fun `casual mention 'Your bank branch closes at 5 PM' must NOT trigger major scam warning`() {
        val signals = extractor.extractSignals("Please note that your bank branch closes at 5 PM on weekdays.")
        val score = riskEngine.processSignals(signals)

        assertTrue(score.score <= 15, "Score was ${score.score}, expected <= 15")
        assertEquals(RiskLevel.LOW, score.level)
    }

    @Test
    fun `routine official statement does not trigger critical alarm`() {
        val signals = extractor.extractSignals("The income tax portal requires login before filing season.")
        val score = riskEngine.processSignals(signals)

        assertTrue(score.score < 30, "Score was ${score.score}")
        assertEquals(RiskLevel.LOW, score.level)
    }

    @Test
    fun `scam phrase 'Your CBI case requires you to transfer money immediately' must become HIGH or CRITICAL`() {
        val signals = extractor.extractSignals("Your CBI case requires you to transfer money immediately.")
        val score = riskEngine.processSignals(signals)

        assertTrue(score.score >= 60, "Expected >= 60, but was ${score.score}")
        assertTrue(score.level == RiskLevel.HIGH || score.level == RiskLevel.CRITICAL)
    }
}
