package com.scamshield.core

import com.scamshield.core.ai.risk.RiskEngine
import com.scamshield.core.ai.signal.ScamSignalExtractor
import com.scamshield.core.domain.model.RiskLevel
import com.scamshield.core.domain.model.ScamType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RiskEngineTest {

    private lateinit var riskEngine: RiskEngine
    private lateinit var extractor: ScamSignalExtractor

    @BeforeEach
    fun setUp() {
        riskEngine = RiskEngine()
        extractor = ScamSignalExtractor()
    }

    @Test
    fun `initial state is safe and zero risk`() {
        val initialScore = riskEngine.processSignals(emptyList())
        assertEquals(0, initialScore.score)
        assertEquals(RiskLevel.LOW, initialScore.level)
        assertTrue(initialScore.activeSignals.isEmpty())
    }

    @Test
    fun `single authority signal yields low to suspicious score`() {
        val signals = extractor.extractSignals("Hello, I am calling from the CBI cyber crime department.")
        assertTrue(signals.any { it.type == ScamType.AUTHORITY_IMPERSONATION })

        val score = riskEngine.processSignals(signals)
        assertTrue(score.score in 15..28, "Score was ${score.score}")
        assertEquals(RiskLevel.LOW, score.level)
    }

    @Test
    fun `digital arrest progressive escalation reaches critical`() {
        val turn1 = extractor.extractSignals("I am calling from CBI headquarters.")
        val s1 = riskEngine.processSignals(turn1)
        assertTrue(s1.score in 15..28)
        assertEquals(RiskLevel.LOW, s1.level)

        val turn2 = extractor.extractSignals("Your Aadhaar has been linked to a money laundering case.")
        val s2 = riskEngine.processSignals(turn2)
        assertTrue(s2.score in 30..49, "Score was ${s2.score}")
        assertEquals(RiskLevel.SUSPICIOUS, s2.level)

        val turn3 = extractor.extractSignals("A non-bailable arrest warrant has been issued.")
        val s3 = riskEngine.processSignals(turn3)
        assertTrue(s3.score in 50..70, "Score was ${s3.score}")

        val turn4 = extractor.extractSignals("Do not tell your family. This is secret.")
        val s4 = riskEngine.processSignals(turn4)
        assertTrue(s4.score in 65..85, "Score was ${s4.score}")

        val turn5 = extractor.extractSignals("Stay on this video call. Do not disconnect.")
        val s5 = riskEngine.processSignals(turn5)
        assertTrue(s5.score >= 80, "Score was ${s5.score}")
        assertEquals(RiskLevel.CRITICAL, s5.level)

        val turn6 = extractor.extractSignals("Transfer two lakh rupees to a safe RBI verification account immediately.")
        val s6 = riskEngine.processSignals(turn6)
        assertTrue(s6.score >= 95, "Score was ${s6.score}")
        assertEquals(RiskLevel.CRITICAL, s6.level)
        assertTrue(s6.score <= 100, "Score must not exceed 100")
    }

    @Test
    fun `score is strictly capped at 100 even with excessive signals`() {
        val massiveThreat = """
            CBI officer speaking. You will be arrested. Warrant issued. Aadhaar linked to drugs.
            Do not tell anyone. Stay on video call. Transfer money immediately. Safe RBI account.
            Send OTP. Install AnyDesk. Scan QR code.
        """
        val signals = extractor.extractSignals(massiveThreat)
        val score = riskEngine.processSignals(signals)

        assertEquals(100, score.score)
        assertEquals(RiskLevel.CRITICAL, score.level)
    }

    @Test
    fun `credential request directly triggers severe risk`() {
        val signals = extractor.extractSignals("Please send your OTP and UPI PIN immediately.")
        val score = riskEngine.processSignals(signals)

        assertTrue(score.score >= 35, "Score was ${score.score}")
        assertTrue(score.activeSignals.any { it.type == ScamType.CREDENTIAL_REQUEST })
    }
}
