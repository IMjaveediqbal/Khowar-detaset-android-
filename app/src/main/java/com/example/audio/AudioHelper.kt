package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
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
                setAudioChannels(1)
                setAudioEncodingBitRate(128000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
                startTimeMillis = SystemClock.elapsedRealtime()
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
        val duration = (SystemClock.elapsedRealtime() - startTimeMillis) / 1000.0
        var completed = false
        try { recorder?.stop(); completed = true }
        catch (e: Exception) { currentFilePath?.let { File(it).delete() } }
        finally {
            runCatching { recorder?.release() }
            recorder = null
            _isRecording.value = false
            _recordingDurationSeconds.value = if (completed) duration else 0.0
        }
        return if (completed) duration else 0.0
    }

    fun cancelRecording() {
        try {
            recorder?.stop()
        } catch (e: Exception) {
            // ignore
        } finally {
            runCatching { recorder?.release() }
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

    private var requestId: String = ""
    fun playAudio(filePath: String, onCompletion: () -> Unit = {}) {
        stopAudio()
        if (filePath.startsWith("cloud:")) {
            val token = UUID.randomUUID().toString()
            requestId = token
            val destination = File.createTempFile("khowar-playback-", ".m4a")
            FirebaseStorage.getInstance().reference.child(filePath.removePrefix("cloud:")).getFile(destination)
                .addOnSuccessListener { if (requestId == token) playAudio(destination.absolutePath) { destination.delete(); onCompletion() } else destination.delete() }
                .addOnFailureListener { destination.delete(); Log.e("AudioPlayerHelper", "Unable to download authorized recording", it) }
            return
        }
        val file = File(filePath)
        if (!file.exists()) {
            Log.e("AudioPlayerHelper", "Audio file not found: $filePath")
            return
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                setOnPreparedListener { player ->
                    _duration.value = player.duration
                    player.start()
                    _isPlaying.value = true
                }
                setOnErrorListener { _, _, _ -> stopAudio(); true }
                prepareAsync()
                setOnCompletionListener {
                    _isPlaying.value = false
                    onCompletion()
                }
            }
        } catch (e: Exception) {
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
        requestId = ""
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
