package com.scamshield.core

import com.scamshield.core.ai.signal.ScamSignalExtractor
import com.scamshield.core.domain.model.ScamType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ScamSignalExtractorTest {

    private lateinit var extractor: ScamSignalExtractor

    @BeforeEach
    fun setUp() {
        extractor = ScamSignalExtractor()
    }

    @Test
    fun `detects English authority and CBI impersonation`() {
        val signals = extractor.extractSignals("I am calling from the Central Bureau of Investigation cyber crime branch.")
        assertTrue(signals.any { it.type == ScamType.AUTHORITY_IMPERSONATION })
    }

    @Test
    fun `detects Hindi and Hinglish authority impersonation`() {
        val signals = extractor.extractSignals("Main Mumbai Crime Branch se Senior Inspector Sharma bol raha hoon.")
        assertTrue(signals.any { it.type == ScamType.AUTHORITY_IMPERSONATION || it.type == ScamType.POLICE_CBI_RBI_IMPERSONATION })
    }

    @Test
    fun `detects Hindi arrest threat`() {
        val signals = extractor.extractSignals("Aapko turant arrest kiya jayega aur jail hogi.")
        assertTrue(signals.any { it.type == ScamType.LEGAL_THREAT })
    }

    @Test
    fun `detects Hindi secrecy command`() {
        val signals = extractor.extractSignals("Kisi ko mat batana, apne parivar ko bhi nahi.")
        assertTrue(signals.any { it.type == ScamType.SECRECY })
    }

    @Test
    fun `detects Hindi video confinement`() {
        val signals = extractor.extractSignals("Video call mat cut karna, camera on rakho.")
        assertTrue(signals.any { it.type == ScamType.VIDEO_CONFINEMENT })
    }

    @Test
    fun `detects AnyDesk remote access coercion`() {
        val signals = extractor.extractSignals("Install AnyDesk app from play store and give me the 9-digit code.")
        assertTrue(signals.any { it.type == ScamType.REMOTE_ACCESS_REQUEST })
    }

    @Test
    fun `detects Safe Account scam phrase`() {
        val signals = extractor.extractSignals("Transfer funds to the safe RBI verification account. It is refundable after audit.")
        assertTrue(signals.any { it.type == ScamType.SAFE_ACCOUNT_SCAM })
    }

    @Test
    fun `detects Aadhaar identity document threat`() {
        val signals = extractor.extractSignals("Your Aadhaar has been linked to a money laundering case.")
        assertTrue(signals.any { it.type == ScamType.IDENTITY_DOCUMENT_THREAT })
    }

    @Test
    fun `detects video confinement stay on this video call`() {
        val signals = extractor.extractSignals("Stay on this video call.")
        assertTrue(signals.any { it.type == ScamType.VIDEO_CONFINEMENT })
    }

    @Test
    fun `detects financial transfer request`() {
        val signals = extractor.extractSignals("You need to transfer two lakh rupees immediately.")
        assertTrue(signals.any { it.type == ScamType.FINANCIAL_REQUEST })
    }

    @Test
    fun `detects credential and OTP request`() {
        val signals = extractor.extractSignals("A 6-digit OTP has been sent. Share the OTP right now.")
        assertTrue(signals.any { it.type == ScamType.CREDENTIAL_REQUEST })
    }

    @Test
    fun `detects artificial urgency`() {
        val signals = extractor.extractSignals("Your bank account will be blocked within 1 hour immediately.")
        assertTrue(signals.any { it.type == ScamType.URGENCY })
    }

    @Test
    fun `detects payment link or QR lure`() {
        val signals = extractor.extractSignals("Click this payment link or scan this QR to avoid penalty.")
        assertTrue(signals.any { it.type == ScamType.PAYMENT_LINK })
    }
}
