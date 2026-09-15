package com.ugo.clip.service

import com.ugo.clip.core.actions.ActionRouter
import com.ugo.clip.core.actions.ExecutionResult
import com.ugo.clip.core.ai.HugoOrchestrator
import com.ugo.clip.core.ai.OrchestratedIntent
import com.ugo.clip.core.security.SecurityLevel
import com.ugo.clip.core.security.Sentinel
import com.ugo.clip.core.security.SentinelResult
import com.ugo.clip.core.voice.HugoUiState
import com.ugo.clip.core.voice.HugoVoiceState
import com.ugo.clip.core.voice.PendingConfirmation
import com.ugo.clip.data.repository.HugoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object HugoServiceBridge {

    private val _uiState = MutableStateFlow(HugoUiState())
    val uiState: StateFlow<HugoUiState> = _uiState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)
    private var repository: HugoRepository? = null
    private var actionRouter: ActionRouter? = null

    // Callbacks to the running Foreground Service
    var onServicePauseRequested: (() -> Unit)? = null
    var onServiceResumeRequested: (() -> Unit)? = null
    var onServiceStopRequested: (() -> Unit)? = null
    var onServiceSpeakRequested: ((String) -> Unit)? = null
    var onServiceListenOnceRequested: (() -> Unit)? = null

    fun initialize(repo: HugoRepository, router: ActionRouter) {
        this.repository = repo
        this.actionRouter = router
        scope.launch {
            repo.seedInitialDataIfNeeded()
        }
    }

    fun updateVoiceState(state: HugoVoiceState) {
        _uiState.update { it.copy(serviceState = state) }
    }

    fun updateAudioLevel(rms: Float) {
        _uiState.update { it.copy(audioLevelRms = rms) }
    }

    fun updateTranscript(text: String) {
        _uiState.update { it.copy(transcript = text) }
    }

    fun setErrorMessage(msg: String?) {
        _uiState.update { it.copy(errorMessage = msg) }
    }

    fun toggleBleClip() {
        _uiState.update { it.copy(isBleClipConnected = !it.isBleClipConnected) }
    }

    /**
     * Simulates pressing the physical wearable clip button:
     * - Single tap: toggle speak/listen
     * - Double tap: toggle pause
     * - Long press: emergency stop
     */
    fun onClipButtonSingleTap() {
        val currentState = _uiState.value.serviceState
        if (currentState == HugoVoiceState.OFF) {
            // activate
            _uiState.update { it.copy(isClipMicActive = true) }
            onServiceResumeRequested?.invoke()
        } else if (currentState == HugoVoiceState.PAUSED) {
            resumeHugo()
        } else {
            // Quick talk trigger
            onServiceListenOnceRequested?.invoke()
        }
    }

    fun onClipButtonDoubleTap() {
        val currentState = _uiState.value.serviceState
        if (currentState == HugoVoiceState.PAUSED) {
            resumeHugo()
        } else if (currentState != HugoVoiceState.OFF) {
            pauseHugo()
        }
    }

    fun onClipButtonLongPress() {
        stopHugo()
    }

    fun pauseHugo() {
        onServicePauseRequested?.invoke()
        _uiState.update {
            it.copy(
                serviceState = HugoVoiceState.PAUSED,
                lastResponse = "Modo HUGO pausado.",
                isClipMicActive = false
            )
        }
    }

    fun resumeHugo() {
        onServiceResumeRequested?.invoke()
        _uiState.update {
            it.copy(
                serviceState = HugoVoiceState.LISTENING,
                lastResponse = "HUGO escutando novamente.",
                isClipMicActive = true
            )
        }
    }

    fun stopHugo() {
        onServiceStopRequested?.invoke()
        _uiState.update {
            it.copy(
                serviceState = HugoVoiceState.OFF,
                lastResponse = "Modo HUGO desativado.",
                isClipMicActive = false,
                audioLevelRms = 0f
            )
        }
    }

    /**
     * Dispatches user speech or typed command through Orchestrator and Sentinel
     */
    fun processCommand(transcript: String) {
        val clean = transcript.trim()
        if (clean.isBlank()) return

        // If there is a pending confirmation, check for "sim", "confirmo", "não", "cancela"
        val pending = _uiState.value.pendingConfirmation
        if (pending != null) {
            val lower = clean.lowercase()
            if (lower.contains("sim") || lower.contains("confirmo") || lower.contains("pode enviar") || lower.contains("confirma")) {
                confirmPendingAction()
                return
            } else if (lower.contains("não") || lower.contains("cancela") || lower.contains("parar")) {
                cancelPendingAction()
                return
            }
        }

        _uiState.update {
            it.copy(
                transcript = clean,
                serviceState = HugoVoiceState.PROCESSING
            )
        }

        val orchestrated = HugoOrchestrator.parse(clean)

        // Handle internal system control directly
        if (orchestrated.intentName == "SYSTEM_PAUSE") {
            pauseHugo()
            speak(orchestrated.voiceReply)
            logCommand(clean, orchestrated.intentName, orchestrated.voiceReply, "LEVEL_A", "SUCCESS")
            return
        }
        if (orchestrated.intentName == "SYSTEM_RESUME") {
            resumeHugo()
            speak(orchestrated.voiceReply)
            logCommand(clean, orchestrated.intentName, orchestrated.voiceReply, "LEVEL_A", "SUCCESS")
            return
        }
        if (orchestrated.intentName == "SYSTEM_STOP") {
            stopHugo()
            speak(orchestrated.voiceReply)
            logCommand(clean, orchestrated.intentName, orchestrated.voiceReply, "LEVEL_A", "SUCCESS")
            return
        }

        // Sentinel security layer evaluation
        when (val decision = Sentinel.evaluate(orchestrated.intentName, orchestrated.parameters)) {
            is SentinelResult.Blocked -> {
                _uiState.update {
                    it.copy(
                        lastResponse = decision.reason,
                        serviceState = HugoVoiceState.LISTENING
                    )
                }
                speak(decision.reason)
                logCommand(clean, orchestrated.intentName, decision.reason, "LEVEL_C", "BLOCKED")
            }
            is SentinelResult.RequiresConfirmation -> {
                val pendingAction = PendingConfirmation(
                    id = System.currentTimeMillis().toString(),
                    title = decision.actionTitle,
                    prompt = decision.confirmationPrompt,
                    onConfirmText = "Confirmar",
                    onConfirmAction = {
                        executeConfirmedIntent(clean, orchestrated)
                    },
                    onCancelAction = {
                        _uiState.update {
                            it.copy(
                                pendingConfirmation = null,
                                lastResponse = "Ação cancelada pelo usuário.",
                                serviceState = HugoVoiceState.LISTENING
                            )
                        }
                        speak("Ação cancelada.")
                        logCommand(clean, orchestrated.intentName, "Ação cancelada pelo usuário", "LEVEL_B", "CANCELLED")
                    }
                )

                _uiState.update {
                    it.copy(
                        pendingConfirmation = pendingAction,
                        lastResponse = decision.confirmationPrompt,
                        serviceState = HugoVoiceState.SPEAKING
                    )
                }
                speak(decision.confirmationPrompt)
            }
            is SentinelResult.Allowed -> {
                executeDirectIntent(clean, orchestrated)
            }
        }
    }

    private fun executeDirectIntent(transcript: String, intent: OrchestratedIntent) {
        val router = actionRouter ?: return
        if (intent.intentName == "SUMMARIZE_TRANSCRIPT" || intent.intentName == "ANALYZE_TRANSCRIPT" || intent.intentName == "GENERAL_QUERY") {
            _uiState.update { it.copy(isGeminiLoading = true) }
        }
        scope.launch {
            val result = router.execute(intent)
            _uiState.update {
                it.copy(
                    lastResponse = result.vocalResponse,
                    serviceState = HugoVoiceState.LISTENING,
                    geminiSummary = result.geminiSummary ?: it.geminiSummary,
                    geminiAnalysis = result.geminiAnalysis ?: it.geminiAnalysis,
                    isGeminiLoading = false
                )
            }
            speak(result.vocalResponse)
            logCommand(
                transcript = transcript,
                intent = intent.intentName,
                response = result.vocalResponse,
                securityLevel = "LEVEL_A",
                status = if (result.success) "SUCCESS" else "ERROR"
            )
        }
    }

    private fun executeConfirmedIntent(transcript: String, intent: OrchestratedIntent) {
        val router = actionRouter ?: return
        scope.launch {
            _uiState.update { it.copy(pendingConfirmation = null) }
            val result = router.execute(intent)
            _uiState.update {
                it.copy(
                    lastResponse = result.vocalResponse,
                    serviceState = HugoVoiceState.LISTENING,
                    geminiSummary = result.geminiSummary ?: it.geminiSummary,
                    geminiAnalysis = result.geminiAnalysis ?: it.geminiAnalysis,
                    isGeminiLoading = false
                )
            }
            speak(result.vocalResponse)
            logCommand(
                transcript = transcript,
                intent = intent.intentName,
                response = result.vocalResponse,
                securityLevel = "LEVEL_B",
                status = "CONFIRMED"
            )
        }
    }

    fun summarizeCurrentTranscript(customText: String? = null) {
        val textToUse = customText ?: _uiState.value.transcript.ifBlank { _uiState.value.lastResponse }
        if (textToUse.isBlank()) return

        _uiState.update { it.copy(isGeminiLoading = true) }
        scope.launch {
            val result = actionRouter?.execute(
                OrchestratedIntent(
                    intentName = "SUMMARIZE_TRANSCRIPT",
                    parameters = mapOf("content" to textToUse, "rawInput" to textToUse),
                    userFriendlySummary = "Resumir com Gemini 3.5 Flash",
                    voiceReply = "Sintetizando resumo com inteligência Gemini."
                )
            )
            if (result != null) {
                _uiState.update {
                    it.copy(
                        lastResponse = result.vocalResponse,
                        geminiSummary = result.geminiSummary ?: it.geminiSummary,
                        geminiAnalysis = result.geminiAnalysis ?: it.geminiAnalysis,
                        isGeminiLoading = false,
                        serviceState = HugoVoiceState.LISTENING
                    )
                }
                speak(result.vocalResponse)
                logCommand(
                    transcript = textToUse,
                    intent = "SUMMARIZE_TRANSCRIPT",
                    response = result.vocalResponse,
                    securityLevel = "LEVEL_A",
                    status = if (result.success) "SUCCESS" else "ERROR"
                )
            } else {
                _uiState.update { it.copy(isGeminiLoading = false) }
            }
        }
    }

    fun analyzeCurrentTranscript(customText: String? = null) {
        val textToUse = customText ?: _uiState.value.transcript.ifBlank { _uiState.value.lastResponse }
        if (textToUse.isBlank()) return

        _uiState.update { it.copy(isGeminiLoading = true) }
        scope.launch {
            val result = actionRouter?.execute(
                OrchestratedIntent(
                    intentName = "ANALYZE_TRANSCRIPT",
                    parameters = mapOf("content" to textToUse, "rawInput" to textToUse),
                    userFriendlySummary = "Analisar com Gemini 3.5 Flash",
                    voiceReply = "Analisando transcrição com Gemini."
                )
            )
            if (result != null) {
                _uiState.update {
                    it.copy(
                        lastResponse = result.vocalResponse,
                        geminiSummary = result.geminiSummary ?: it.geminiSummary,
                        geminiAnalysis = result.geminiAnalysis ?: it.geminiAnalysis,
                        isGeminiLoading = false,
                        serviceState = HugoVoiceState.LISTENING
                    )
                }
                speak(result.vocalResponse)
                logCommand(
                    transcript = textToUse,
                    intent = "ANALYZE_TRANSCRIPT",
                    response = result.vocalResponse,
                    securityLevel = "LEVEL_A",
                    status = if (result.success) "SUCCESS" else "ERROR"
                )
            } else {
                _uiState.update { it.copy(isGeminiLoading = false) }
            }
        }
    }

    fun clearGeminiAnalysis() {
        _uiState.update {
            it.copy(
                geminiSummary = null,
                geminiAnalysis = null
            )
        }
    }

    fun confirmPendingAction() {
        val pending = _uiState.value.pendingConfirmation ?: return
        scope.launch {
            pending.onConfirmAction()
        }
    }

    fun cancelPendingAction() {
        val pending = _uiState.value.pendingConfirmation ?: return
        scope.launch {
            pending.onCancelAction?.invoke()
        }
    }

    private fun speak(text: String) {
        onServiceSpeakRequested?.invoke(text)
    }

    private fun logCommand(
        transcript: String,
        intent: String,
        response: String,
        securityLevel: String,
        status: String
    ) {
        scope.launch {
            repository?.logCommand(
                transcript = transcript,
                intent = intent,
                response = response,
                securityLevel = securityLevel,
                status = status
            )
        }
    }
}
