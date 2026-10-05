package com.scamshield.core.ai.stt

class MockSpeechEngine(
    private val cannedSegments: List<String> = defaultSegments
) : LocalSpeechEngine {

    private var currentIndex = 0
    private var isRunning = false

    override fun start() {
        isRunning = true
    }

    override fun stop() {
        isRunning = false
    }

    override fun transcribe(audioChunk: ByteArray): String {
        if (!isRunning || cannedSegments.isEmpty()) return ""
        val text = cannedSegments[currentIndex % cannedSegments.size]
        currentIndex++
        return text
    }

    override fun reset() {
        currentIndex = 0
        isRunning = false
    }

    override fun isMock(): Boolean = true

    companion object {
        val defaultSegments = listOf(
            "Hello, I am calling from the CBI cyber crime department.",
            "Your Aadhaar has been linked to a 23 crore money laundering case.",
            "A non-bailable arrest warrant has been issued against you.",
            "Do not tell your family. This is a secret investigation.",
            "Stay on this video call. Do not turn off your camera.",
            "You need to transfer two lakh rupees to a safe RBI verification account immediately."
        )
    }
}
