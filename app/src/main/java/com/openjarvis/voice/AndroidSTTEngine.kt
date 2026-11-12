package com.openjarvis.voice

import android.content.Context
import androidx.annotation.MainThread
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class AndroidSTTEngine(private val context: Context) : STTEngine {

    companion object {
        const val SILENCE_TIMEOUT_MS = 1500L
    }

    private var recognizer: SpeechRecognizer? = null
    private var _isListening = MutableStateFlow(false)
    val isListeningState: StateFlow<Boolean> = _isListening

    private var currentCallback: ((String?) -> Unit)? = null
    private var currentErrorCallback: ((String) -> Unit)? = null
    private var pendingResult: String? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        mainHandler.post {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    // Android SpeechRecognizer manages speech endpoint detection.
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Missing permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                        else -> "Unknown error"
                    }
                    if (pendingResult.isNullOrBlank()) {
                        currentErrorCallback?.invoke(errorMessage)
                    }
                    currentCallback = null
                    currentErrorCallback = null
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.trim()
                    if (!text.isNullOrBlank()) {
                        pendingResult = text
                        currentCallback?.invoke(text)
                    } else {
                        currentErrorCallback?.invoke("No speech detected")
                    }
                    currentCallback = null
                    currentErrorCallback = null
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    @MainThread
    override fun startListening(onResult: (String?) -> Unit, onError: (String) -> Unit) {
        currentCallback = onResult
        currentErrorCallback = onError
        pendingResult = null
        if (context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            currentCallback = null
            currentErrorCallback = null
            onError("Microphone permission is not granted")
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            currentCallback = null
            currentErrorCallback = null
            onError("Speech recognition is unavailable on this device")
            return
        }
        val activeRecognizer = recognizer
        if (activeRecognizer == null) {
            currentCallback = null
            currentErrorCallback = null
            onError("Speech recognition is still initializing. Try again.")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, SILENCE_TIMEOUT_MS)
        }

        try {
            activeRecognizer.startListening(intent)
        } catch (e: SecurityException) {
            currentCallback = null
            currentErrorCallback = null
            onError("Microphone permission was denied")
        } catch (e: IllegalStateException) {
            currentCallback = null
            currentErrorCallback = null
            onError("Speech recognizer is busy. Try again.")
        }
    }

    @MainThread
    override fun stop(): String? {
        // Android delivers the final transcript asynchronously through onResults.
        // Keep callbacks registered so a press-to-talk release cannot discard it.
        recognizer?.stopListening()

        val result = pendingResult
        pendingResult = null
        return result
    }

    override fun isListening(): Boolean = _isListening.value

    @MainThread
    override fun release() {
        stop()
        recognizer?.destroy()
        recognizer = null
    }
}
