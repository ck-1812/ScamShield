package com.scamshield.core

import com.scamshield.core.demo.DemoScenarioRunner
import com.scamshield.core.demo.ScamScriptRepository
import com.scamshield.core.domain.model.RiskLevel
import com.scamshield.core.domain.model.ScamType
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DemoScenarioRunnerTest {

    @Test
    fun `deterministic Digital Arrest scenario progresses from low to critical risk`() = runBlocking {
        val runner = DemoScenarioRunner()
        val results = runner.runScenario(ScamScriptRepository.digitalArrestScenario, applyDelay = false).toList()

        assertEquals(6, results.size, "Digital Arrest scenario must have exactly 6 turns")

        // Turn 1: Authority impersonation
        val turn1 = results[0]
        assertTrue(turn1.currentRiskScore.score >= 15, "Turn 1 score should rise from baseline")
        assertTrue(turn1.newSignals.isNotEmpty(), "Turn 1 should detect authority impersonation")

        // Turn 3: Legal threat / arrest
        val turn3 = results[2]
        assertTrue(turn3.currentRiskScore.score >= 45, "Turn 3 should reach suspicious tier")

        // Final Turn 6: Financial request + Safe account -> CRITICAL
        val finalTurn = results[5]
        assertEquals(RiskLevel.CRITICAL, finalTurn.currentRiskScore.level, "Final turn must reach CRITICAL risk level")
        assertTrue(finalTurn.currentRiskScore.score >= 80, "Critical score must be at least 80/100")
        assertTrue(finalTurn.currentRiskScore.explanation.contains("digital arrest", ignoreCase = true) ||
                   finalTurn.currentRiskScore.explanation.contains("warning", ignoreCase = true))
    }

    @Test
    fun `turn-by-turn synchronization ensures sequential risk escalation corresponding to exact turns`() = runBlocking {
        val runner = DemoScenarioRunner()
        val results = runner.runScenario(ScamScriptRepository.digitalArrestScenario, applyDelay = false).toList()

        assertEquals(6, results.size)

        // Turn 1: Authority
        assertEquals(1, results[0].turn.step)
        assertTrue(results[0].newSignals.any { it.type == ScamType.AUTHORITY_IMPERSONATION })

        // Turn 2: Identity threat
        assertEquals(2, results[1].turn.step)
        assertTrue(results[1].newSignals.any { it.type == ScamType.IDENTITY_DOCUMENT_THREAT })

        // Turn 3: Legal threat
        assertEquals(3, results[2].turn.step)
        assertTrue(results[2].newSignals.any { it.type == ScamType.LEGAL_THREAT })

        // Turn 4: Secrecy
        assertEquals(4, results[3].turn.step)
        assertTrue(results[3].newSignals.any { it.type == ScamType.SECRECY })

        // Turn 5: Video confinement
        assertEquals(5, results[4].turn.step)
        assertTrue(results[4].newSignals.any { it.type == ScamType.VIDEO_CONFINEMENT })

        // Turn 6: Financial request
        assertEquals(6, results[5].turn.step)
        assertTrue(results[5].newSignals.any { it.type == ScamType.FINANCIAL_REQUEST })

        // Verify strictly non-decreasing risk progression
        var previousScore = 0
        for (stepResult in results) {
            val currentScore = stepResult.currentRiskScore.score
            assertTrue(currentScore >= previousScore, "Score must be non-decreasing at step ${stepResult.turn.step}")
            previousScore = currentScore
        }
    }

    @Test
    fun `Hindi Police scenario detects corresponding tactics and escalates risk`() = runBlocking {
        val runner = DemoScenarioRunner()
        val results = runner.runScenario(ScamScriptRepository.fakePoliceHindiScenario, applyDelay = false).toList()

        assertEquals(5, results.size, "Hindi scenario must have 5 turns")
        assertTrue(results[0].newSignals.any { it.type == ScamType.AUTHORITY_IMPERSONATION })
        assertTrue(results[1].newSignals.any { it.type == ScamType.IDENTITY_DOCUMENT_THREAT })
        assertTrue(results[2].newSignals.any { it.type == ScamType.LEGAL_THREAT })
        assertTrue(results[3].newSignals.any { it.type == ScamType.SECRECY })
        assertTrue(results[4].newSignals.any { it.type == ScamType.FINANCIAL_REQUEST })

        val finalScore = results.last().currentRiskScore
        assertEquals(RiskLevel.CRITICAL, finalScore.level)
    }

    @Test
    fun `benign scenario validation confirms legitimate bank call stays LOW risk`() = runBlocking {
        val runner = DemoScenarioRunner()
        val results = runner.runScenario(ScamScriptRepository.legitBankScenario, applyDelay = false).toList()

        assertEquals(1, results.size)
        val result = results[0]
        assertEquals(RiskLevel.LOW, result.currentRiskScore.level)
        assertEquals(0, result.currentRiskScore.score)
        assertTrue(result.newSignals.isEmpty(), "No scam indicators should be detected on legitimate bank notice")
    }
}
