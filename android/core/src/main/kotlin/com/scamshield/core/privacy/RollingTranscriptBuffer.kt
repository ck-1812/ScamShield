package com.scamshield.core.privacy

import com.scamshield.core.domain.model.TranscriptSegment

class RollingTranscriptBuffer(
    private val maxWindowDurationMs: Long = 45_000L // 45 seconds rolling privacy window
) {
    private val segments = mutableListOf<TranscriptSegment>()

    @Synchronized
    fun addSegment(segment: TranscriptSegment, currentTimestamp: Long = System.currentTimeMillis()) {
        segments.add(segment)
        pruneExpiredSegments(currentTimestamp)
    }

    @Synchronized
    fun getActiveSegments(currentTimestamp: Long = System.currentTimeMillis()): List<TranscriptSegment> {
        pruneExpiredSegments(currentTimestamp)
        return segments.toList()
    }

    @Synchronized
    fun getFullWindowText(currentTimestamp: Long = System.currentTimeMillis()): String {
        pruneExpiredSegments(currentTimestamp)
        return segments.joinToString(" ") { it.text }
    }

    @Synchronized
    private fun pruneExpiredSegments(currentTimestamp: Long) {
        val cutoff = currentTimestamp - maxWindowDurationMs
        segments.removeAll { it.timestamp < cutoff }
    }

    @Synchronized
    fun clear() {
        segments.clear()
    }

    @Synchronized
    fun size(): Int = segments.size
}
