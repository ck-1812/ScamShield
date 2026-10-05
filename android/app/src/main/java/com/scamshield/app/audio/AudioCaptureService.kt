package com.scamshield.app.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

class AudioCaptureService(
    private val context: Context,
    private val onChunkCaptured: (ByteArray) -> Unit = {}
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null
    private val isRecording = AtomicBoolean(false)
    private val rollingBuffer = RollingAudioBuffer(maxCapacitySeconds = 4)

    val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _isInputDetected = MutableStateFlow(false)
    val isInputDetected: StateFlow<Boolean> = _isInputDetected.asStateFlow()

    private val _waveformBars = MutableStateFlow(List(16) { 0.12f })
    val waveformBars: StateFlow<List<Float>> = _waveformBars.asStateFlow()

    private val barHistory = ArrayList<Float>(List(16) { 0.12f })

    fun isSpeakerphoneActive(): Boolean {
        return audioManager.isSpeakerphoneOn
    }

    @SuppressLint("MissingPermission")
    fun startCapture(scope: CoroutineScope) {
        if (isRecording.get()) return

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.DEFAULT,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
            }

            if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord?.startRecording()
                isRecording.set(true)
            } else {
                isRecording.set(false)
            }

            captureJob = scope.launch(Dispatchers.IO) {
                val tempBuffer = ByteArray(2048)
                while (isActive && isRecording.get()) {
                    val readBytes = audioRecord?.read(tempBuffer, 0, tempBuffer.size) ?: 0
                    if (readBytes > 0) {
                        // 1. Calculate Real RMS amplitude from 16-bit PCM samples
                        var sumSquares = 0.0
                        val sampleCount = readBytes / 2
                        for (i in 0 until readBytes step 2) {
                            val sample = (tempBuffer[i].toInt() and 0xFF) or (tempBuffer[i + 1].toInt() shl 8)
                            val normalized = sample.toShort() / 32768.0f
                            sumSquares += normalized * normalized
                        }
                        val rms = if (sampleCount > 0) sqrt(sumSquares / sampleCount).toFloat() else 0f
                        
                        // Amplify for visual responsiveness (0.0 to 1.0)
                        val visualLevel = (rms * 4.5f).coerceIn(0f, 1f)
                        _amplitude.value = visualLevel
                        _isInputDetected.value = visualLevel > 0.015f

                        // Update waveform history
                        synchronized(barHistory) {
                            barHistory.removeAt(0)
                            barHistory.add(visualLevel.coerceAtLeast(0.12f))
                            _waveformBars.value = barHistory.toList()
                        }

                        val chunkCopy = tempBuffer.copyOf(readBytes)
                        rollingBuffer.push(AudioChunk(chunkCopy))

                        // Push accumulated frames every 2 seconds (~64000 bytes)
                        if (rollingBuffer.sizeInBytes() >= sampleRate * 2 * 2) {
                            val flushed = rollingBuffer.getCombinedBytesAndFlush()
                            if (flushed.isNotEmpty()) {
                                onChunkCaptured(flushed)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopCapture()
        }
    }

    fun stopCapture() {
        isRecording.set(false)
        captureJob?.cancel()
        captureJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignored
        } finally {
            audioRecord = null
            rollingBuffer.clear()
            _amplitude.value = 0f
            _isInputDetected.value = false
            synchronized(barHistory) {
                barHistory.clear()
                barHistory.addAll(List(16) { 0.12f })
                _waveformBars.value = barHistory.toList()
            }
        }
    }

    fun isCapturing(): Boolean = isRecording.get()
}
