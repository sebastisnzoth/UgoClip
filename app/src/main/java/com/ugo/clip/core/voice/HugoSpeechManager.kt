package com.ugo.clip.core.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class HugoSpeechManager(
    private val context: Context,
    private val onTranscript: (String) -> Unit,
    private val onPartialTranscript: (String) -> Unit = {},
    private val onRmsChanged: (Float) -> Unit,
    private val onStateChange: (HugoVoiceState) -> Unit,
    private val onErrorMsg: (String) -> Unit
) {
    companion object {
        private const val TAG = "HugoSpeechManager"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isContinuous = false
    private var isPaused = false
    private var isMutedForTts = false
    private var isCurrentlyListening = false

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "onReadyForSpeech")
            isCurrentlyListening = true
            if (!isPaused && !isMutedForTts) {
                onStateChange(HugoVoiceState.LISTENING)
            }
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech")
            if (!isPaused && !isMutedForTts) {
                onStateChange(HugoVoiceState.LISTENING)
            }
        }

        override fun onRmsChanged(rmsdB: Float) {
            if (!isPaused && !isMutedForTts && isCurrentlyListening) {
                // RMS range in Android SpeechRecognizer is typically -2dB to 10dB (or 0 to 12)
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                onRmsChanged(normalized)
            }
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech")
            isCurrentlyListening = false
            onRmsChanged(0f)
            if (!isPaused && !isMutedForTts) {
                onStateChange(HugoVoiceState.PROCESSING)
            }
        }

        override fun onError(error: Int) {
            isCurrentlyListening = false
            onRmsChanged(0f)
            val errorMessage = getErrorDescription(error)
            Log.w(TAG, "SpeechRecognizer error: $error ($errorMessage)")

            if (isPaused || isMutedForTts) return

            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    // Normal silence/no-input interval in continuous speech mode
                    if (isContinuous) {
                        mainHandler.postDelayed({
                            restartListeningSafely()
                        }, 400)
                    } else {
                        onStateChange(HugoVoiceState.LISTENING)
                    }
                }
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    onStateChange(HugoVoiceState.ERROR)
                    onErrorMsg("Permissão de microfone necessária.")
                }
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    mainHandler.postDelayed({
                        restartListeningSafely()
                    }, 500)
                }
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                    onErrorMsg("Conexão instável para reconhecimento de voz.")
                    if (isContinuous) {
                        mainHandler.postDelayed({
                            restartListeningSafely()
                        }, 1200)
                    }
                }
                else -> {
                    if (isContinuous) {
                        mainHandler.postDelayed({
                            restartListeningSafely()
                        }, 800)
                    } else {
                        onStateChange(HugoVoiceState.LISTENING)
                    }
                }
            }
        }

        override fun onResults(results: Bundle?) {
            isCurrentlyListening = false
            onRmsChanged(0f)
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedText = matches?.firstOrNull()?.trim().orEmpty()
            Log.d(TAG, "SpeechRecognizer onResults: $recognizedText")

            if (recognizedText.isNotBlank()) {
                onTranscript(recognizedText)
            }

            if (isContinuous && !isPaused && !isMutedForTts) {
                mainHandler.postDelayed({
                    restartListeningSafely()
                }, 350)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partialList = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = partialList?.firstOrNull()?.trim().orEmpty()
            if (partial.isNotBlank() && !isPaused && !isMutedForTts) {
                Log.d(TAG, "onPartialResults: $partial")
                onPartialTranscript(partial)
                onStateChange(HugoVoiceState.LISTENING)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun getErrorDescription(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Erro de gravação de áudio."
            SpeechRecognizer.ERROR_CLIENT -> "Erro no cliente do dispositivo."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissões insuficientes."
            SpeechRecognizer.ERROR_NETWORK -> "Falha de rede."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tempo limite de rede."
            SpeechRecognizer.ERROR_NO_MATCH -> "Nenhuma correspondência de fala."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconhecedor de voz ocupado."
            SpeechRecognizer.ERROR_SERVER -> "Erro no servidor de voz."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tempo de fala esgotado."
            else -> "Código de erro de reconhecimento: $errorCode"
        }
    }

    fun startListening(continuous: Boolean = true) {
        isContinuous = continuous
        isPaused = false
        isMutedForTts = false
        mainHandler.post {
            ensureRecognizer()
            startListeningInternal()
        }
    }

    fun pauseListening() {
        isPaused = true
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {}
            isCurrentlyListening = false
            onStateChange(HugoVoiceState.PAUSED)
            onRmsChanged(0f)
        }
    }

    fun resumeListening() {
        isPaused = false
        isMutedForTts = false
        mainHandler.post {
            restartListeningSafely()
        }
    }

    fun setMutedForTts(muted: Boolean) {
        isMutedForTts = muted
        if (muted) {
            mainHandler.post {
                try {
                    speechRecognizer?.stopListening()
                } catch (_: Exception) {}
                isCurrentlyListening = false
                onRmsChanged(0f)
            }
        } else {
            if (isContinuous && !isPaused) {
                mainHandler.postDelayed({
                    restartListeningSafely()
                }, 300)
            }
        }
    }

    fun stopListening() {
        isContinuous = false
        isPaused = false
        isCurrentlyListening = false
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (_: Exception) {}
            onStateChange(HugoVoiceState.OFF)
            onRmsChanged(0f)
        }
    }

    fun destroy() {
        isContinuous = false
        isCurrentlyListening = false
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
        }
    }

    private fun ensureRecognizer() {
        if (speechRecognizer != null) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "SpeechRecognizer.isRecognitionAvailable returned false")
            onErrorMsg("Reconhecimento de fala não disponível no sistema.")
            return
        }

        try {
            speechRecognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
            ) {
                Log.d(TAG, "Using on-device SpeechRecognizer")
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else {
                Log.d(TAG, "Using default SpeechRecognizer")
                SpeechRecognizer.createSpeechRecognizer(context)
            }
            speechRecognizer?.setRecognitionListener(recognitionListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create SpeechRecognizer", e)
            onErrorMsg("Falha ao inicializar o microfone de voz.")
        }
    }

    private fun startListeningInternal() {
        if (isPaused || isMutedForTts) return
        ensureRecognizer()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra("android.speech.extra.DICTATION_MODE", true)
        }

        try {
            speechRecognizer?.startListening(intent)
            onStateChange(HugoVoiceState.LISTENING)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech listening", e)
        }
    }

    private fun restartListeningSafely() {
        if (!isContinuous || isPaused || isMutedForTts) return
        try {
            speechRecognizer?.cancel()
            startListeningInternal()
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling recognizer, re-creating...", e)
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
            ensureRecognizer()
            startListeningInternal()
        }
    }
}
