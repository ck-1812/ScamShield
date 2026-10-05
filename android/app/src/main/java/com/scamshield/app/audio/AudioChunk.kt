package com.scamshield.app.audio

data class AudioChunk(
    val data: ByteArray,
    val timestamp: Long = System.currentTimeMillis(),
    val sampleRate: Int = 16000
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AudioChunk
        return data.contentEquals(other.data) && timestamp == other.timestamp
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}
