package com.scamshield.app.audio

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * AudioPlaybackManager
 * Plays simulated caller audio through the device speaker in real time.
 * Provides controls for play, pause, resume, stop, and elapsed/total time tracking.
 *
 * CRITICAL AUDIO SAFETY:
 * Audio playback is strictly one-way (Assets -> MediaPlayer -> Phone Speaker).
 * Microphone capture is NEVER routed back into the speaker to prevent acoustic feedback loops.
 */
class AudioPlaybackManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _totalDurationMs = MutableStateFlow(0L)
    val totalDurationMs: StateFlow<Long> = _totalDurationMs.asStateFlow()

    private val _formattedProgress = MutableStateFlow("00:00 / 00:00")
    val formattedProgress: StateFlow<String> = _formattedProgress.asStateFlow()

    private val _currentTurnIndex = MutableStateFlow(0)
    val currentTurnIndex: StateFlow<Int> = _currentTurnIndex.asStateFlow()

    private var currentFiles: List<String> = emptyList()
    private var fileIndex = 0
    private var onTurnStartCallback: ((Int) -> Unit)? = null
    private var onTurnEndCallback: ((Int) -> Unit)? = null
    private var onCompleteCallback: (() -> Unit)? = null

    fun playScenario(
        scenarioId: String,
        onTurnStart: (Int) -> Unit = {},
        onTurnEnd: (Int) -> Unit = {},
        onComplete: () -> Unit = {}
    ) {
        stop()

        onTurnStartCallback = onTurnStart
        onTurnEndCallback = onTurnEnd
        onCompleteCallback = onComplete

        currentFiles = when (scenarioId) {
            "legit_bank_call" -> listOf("audio/bank_legit_01.wav")
            "fake_police_hindi" -> listOf(
                "audio/caller_hindi_01.wav", "audio/caller_hindi_02.wav", "audio/caller_hindi_03.wav",
                "audio/caller_hindi_04.wav", "audio/caller_hindi_05.wav"
            )
            else -> listOf(
                "audio/caller_01.wav", "audio/caller_02.wav", "audio/caller_03.wav",
                "audio/caller_04.wav", "audio/caller_05.wav", "audio/caller_06.wav"
            )
        }

        fileIndex = 0
        _currentTurnIndex.value = 0
        calculateTotalDuration()
        playNextTurn()
    }

    private fun calculateTotalDuration() {
        // Approximate total duration based on turns (~4-6s per turn + 2s gap)
        val estDuration = currentFiles.size * 5500L
        _totalDurationMs.value = estDuration
    }

    private fun playNextTurn() {
        if (fileIndex >= currentFiles.size) {
            stop()
            onCompleteCallback?.invoke()
            return
        }

        val assetPath = currentFiles[fileIndex]
        _currentTurnIndex.value = fileIndex
        onTurnStartCallback?.invoke(fileIndex)

        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )

                val afd: AssetFileDescriptor = context.assets.openFd(assetPath)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()

                prepare()
                start()
            }

            _isPlaying.value = true
            _isPaused.value = false

            mediaPlayer?.setOnCompletionListener {
                onTurnEndCallback?.invoke(fileIndex)
                fileIndex++
                // 2.0 second natural pause between scammer statements
                scope.launch(Dispatchers.Main) {
                    delay(2000L)
                    if (_isPlaying.value && !_isPaused.value) {
                        playNextTurn()
                    }
                }
            }

            startProgressTracker()

        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: advance turn if asset cannot be opened
            onTurnEndCallback?.invoke(fileIndex)
            fileIndex++
            scope.launch(Dispatchers.Main) {
                delay(2500L)
                if (_isPlaying.value && !_isPaused.value) {
                    playNextTurn()
                }
            }
        }
    }

    fun pause() {
        if (_isPlaying.value && !_isPaused.value) {
            mediaPlayer?.pause()
            _isPaused.value = true
        }
    }

    fun resume() {
        if (_isPlaying.value && _isPaused.value) {
            mediaPlayer?.start()
            _isPaused.value = false
        }
    }

    fun stop() {
        _isPlaying.value = false
        _isPaused.value = false
        progressJob?.cancel()
        progressJob = null

        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Ignored
        } finally {
            mediaPlayer = null
            _currentPositionMs.value = 0L
            _formattedProgress.value = "00:00 / 00:00"
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Default) {
            var elapsedSoFar = 0L
            while (isActive && _isPlaying.value) {
                if (!_isPaused.value) {
                    val turnPosition = try {
                        mediaPlayer?.currentPosition?.toLong() ?: 0L
                    } catch (e: Exception) {
                        0L
                    }
                    val totalElapsed = (fileIndex * 5000L) + turnPosition
                    _currentPositionMs.value = totalElapsed

                    val curSec = (totalElapsed / 1000).coerceAtLeast(0)
                    val totSec = (_totalDurationMs.value / 1000).coerceAtLeast(curSec)
                    val curStr = String.format("%02d:%02d", curSec / 60, curSec % 60)
                    val totStr = String.format("%02d:%02d", totSec / 60, totSec % 60)
                    _formattedProgress.value = "$curStr / $totStr"
                }
                delay(200L)
            }
        }
    }

    fun release() {
        stop()
    }
}
