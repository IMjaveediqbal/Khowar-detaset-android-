package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException

class AudioRecorderHelper(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var currentFilePath: String? = null
    private var startTimeMillis: Long = 0L

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0.0)
    val recordingDurationSeconds: StateFlow<Double> = _recordingDurationSeconds.asStateFlow()

    fun startRecording(): String? {
        stopRecording()
        val audioDir = File(context.filesDir, "speech_corpus").apply { mkdirs() }
        val outputFile = File(audioDir, "khowar_audio_${System.currentTimeMillis()}.m4a")
        currentFilePath = outputFile.absolutePath

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            try {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
                startTimeMillis = System.currentTimeMillis()
                _isRecording.value = true
            } catch (e: Exception) {
                Log.e("AudioRecorderHelper", "Failed to start recording: ${e.message}")
                release()
                recorder = null
                _isRecording.value = false
                return null
            }
        }
        return currentFilePath
    }

    fun stopRecording(): Double {
        if (!_isRecording.value) return 0.0
        val duration = (System.currentTimeMillis() - startTimeMillis) / 1000.0
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorderHelper", "Error stopping recorder: ${e.message}")
        } finally {
            recorder = null
            _isRecording.value = false
            _recordingDurationSeconds.value = duration
        }
        return duration
    }

    fun cancelRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // ignore
        } finally {
            recorder = null
            _isRecording.value = false
            currentFilePath?.let {
                val f = File(it)
                if (f.exists()) f.delete()
            }
            currentFilePath = null
        }
    }
}

class AudioPlayerHelper {
    private var mediaPlayer: MediaPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration.asStateFlow()

    fun playAudio(filePath: String, onCompletion: () -> Unit = {}) {
        stopAudio()
        val file = File(filePath)
        if (!file.exists()) {
            Log.e("AudioPlayerHelper", "Audio file not found: $filePath")
            return
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                _duration.value = this.duration
                setOnCompletionListener {
                    _isPlaying.value = false
                    onCompletion()
                }
                start()
                _isPlaying.value = true
            }
        } catch (e: IOException) {
            Log.e("AudioPlayerHelper", "Playback error: ${e.message}")
            stopAudio()
        }
    }

    fun pauseAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
            }
        }
    }

    fun resumeAudio() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                _isPlaying.value = true
            }
        }
    }

    fun stopAudio() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            // ignore
        } finally {
            mediaPlayer = null
            _isPlaying.value = false
            _currentPosition.value = 0
        }
    }
}
