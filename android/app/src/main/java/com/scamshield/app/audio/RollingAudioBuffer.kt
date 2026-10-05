package com.scamshield.app.audio

import java.util.ArrayDeque

/**
 * Ephemeral In-Memory Audio Buffer enforcing RULE 4 & RULE 5:
 * - Raw audio is NEVER written to flash storage.
 * - Rolling memory holds at most [maxCapacitySeconds] of 16kHz 16-bit PCM (approx 160KB).
 * - Chunks are immediately cleared / overwritten.
 */
class RollingAudioBuffer(
    private val maxCapacitySeconds: Int = 5,
    private val sampleRate: Int = 16000,
    private val bytesPerSample: Int = 2 // 16-bit mono
) {
    private val maxBytes = maxCapacitySeconds * sampleRate * bytesPerSample
    private val bufferQueue = ArrayDeque<AudioChunk>()
    private var currentByteCount = 0

    @Synchronized
    fun push(chunk: AudioChunk) {
        bufferQueue.addLast(chunk)
        currentByteCount += chunk.data.size

        while (currentByteCount > maxBytes && bufferQueue.isNotEmpty()) {
            val evicted = bufferQueue.removeFirst()
            // Securely zero out evicted PCM memory
            evicted.data.fill(0)
            currentByteCount -= evicted.data.size
        }
    }

    @Synchronized
    fun getCombinedBytesAndFlush(): ByteArray {
        val totalSize = bufferQueue.sumOf { it.data.size }
        val combined = ByteArray(totalSize)
        var offset = 0

        while (bufferQueue.isNotEmpty()) {
            val chunk = bufferQueue.removeFirst()
            System.arraycopy(chunk.data, 0, combined, offset, chunk.data.size)
            offset += chunk.data.size
            // Discard and zero out immediately (RULE 5)
            chunk.data.fill(0)
        }

        currentByteCount = 0
        return combined
    }

    @Synchronized
    fun clear() {
        while (bufferQueue.isNotEmpty()) {
            val chunk = bufferQueue.removeFirst()
            chunk.data.fill(0)
        }
        currentByteCount = 0
    }

    @Synchronized
    fun sizeInBytes(): Int = currentByteCount
}
