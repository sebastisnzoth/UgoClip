package com.ugo.clip.core.voice

import com.ugo.clip.core.ai.GeminiAnalysis

enum class HugoVoiceState {
    OFF,
    STARTING,
    LISTENING,
    PROCESSING,
    SPEAKING,
    PAUSED,
    ERROR
}

data class PendingConfirmation(
    val id: String,
    val title: String,
    val prompt: String,
    val onConfirmText: String,
    val onConfirmAction: suspend () -> Unit,
    val onCancelAction: (suspend () -> Unit)? = null
)

data class HugoUiState(
    val serviceState: HugoVoiceState = HugoVoiceState.OFF,
    val transcript: String = "",
    val lastResponse: String = "HUGO pronto para ouvir. Toque em 'Ativar Modo HUGO' ou use os comandos.",
    val audioLevelRms: Float = 0f,
    val pendingConfirmation: PendingConfirmation? = null,
    val isBleClipConnected: Boolean = true,
    val clipBatteryPercent: Int = 92,
    val isClipMicActive: Boolean = false,
    val errorMessage: String? = null,
    val geminiSummary: String? = null,
    val geminiAnalysis: GeminiAnalysis? = null,
    val isGeminiLoading: Boolean = false
)
