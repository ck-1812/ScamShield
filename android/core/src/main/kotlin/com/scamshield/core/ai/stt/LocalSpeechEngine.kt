package com.scamshield.core.ai.stt

interface LocalSpeechEngine {
    fun start()
    fun stop()
    fun transcribe(audioChunk: ByteArray): String
    fun reset()
    fun isMock(): Boolean
}
