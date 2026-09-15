package com.ugo.clip.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ugo.clip.core.actions.ActionRouter
import com.ugo.clip.core.voice.HugoUiState
import com.ugo.clip.data.db.HugoDatabase
import com.ugo.clip.data.model.CommandLogEntity
import com.ugo.clip.data.model.GeminiInteractionEntity
import com.ugo.clip.data.model.MemoryFactEntity
import com.ugo.clip.data.model.NoteEntity
import com.ugo.clip.data.model.ReminderEntity
import com.ugo.clip.data.model.TaskEntity
import com.ugo.clip.data.repository.HugoRepository
import com.ugo.clip.service.HugoServiceBridge
import com.ugo.clip.service.HugoVoiceService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HugoViewModel(application: Application) : AndroidViewModel(application) {

    private val database = HugoDatabase.getDatabase(application)
    val repository = HugoRepository(database.hugoDao())
    val actionRouter = ActionRouter(application, repository)

    init {
        HugoServiceBridge.initialize(repository, actionRouter)
    }

    val uiState: StateFlow<HugoUiState> = HugoServiceBridge.uiState

    val notes: StateFlow<List<NoteEntity>> = repository.notes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = repository.tasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<ReminderEntity>> = repository.reminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<MemoryFactEntity>> = repository.memories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val commandLogs: StateFlow<List<CommandLogEntity>> = repository.commandLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val geminiInteractions: StateFlow<List<GeminiInteractionEntity>> = repository.geminiInteractions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startHugoMode() {
        HugoVoiceService.startService(getApplication())
    }

    fun stopHugoMode() {
        HugoVoiceService.stopService(getApplication())
        HugoServiceBridge.stopHugo()
    }

    fun pauseHugo() {
        HugoServiceBridge.pauseHugo()
    }

    fun resumeHugo() {
        HugoServiceBridge.resumeHugo()
    }

    fun sendVoiceCommand(command: String) {
        HugoServiceBridge.processCommand(command)
    }

    fun summarizeCurrentTranscript(customText: String? = null) {
        HugoServiceBridge.summarizeCurrentTranscript(customText)
    }

    fun analyzeCurrentTranscript(customText: String? = null) {
        HugoServiceBridge.analyzeCurrentTranscript(customText)
    }

    fun clearGeminiAnalysis() {
        HugoServiceBridge.clearGeminiAnalysis()
    }

    fun confirmSentinel() {
        HugoServiceBridge.confirmPendingAction()
    }

    fun cancelSentinel() {
        HugoServiceBridge.cancelPendingAction()
    }

    fun toggleBleClip() {
        HugoServiceBridge.toggleBleClip()
    }

    fun clipButtonSingleTap() {
        HugoServiceBridge.onClipButtonSingleTap()
    }

    fun clipButtonDoubleTap() {
        HugoServiceBridge.onClipButtonDoubleTap()
    }

    fun clipButtonLongPress() {
        HugoServiceBridge.onClipButtonLongPress()
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.toggleTask(task)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun addTask(title: String) {
        viewModelScope.launch {
            repository.insertTask(title)
        }
    }

    fun toggleReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.toggleReminder(reminder)
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
        }
    }

    fun addReminder(title: String, timeDesc: String) {
        viewModelScope.launch {
            repository.insertReminder(title, timeDesc)
        }
    }

    fun addMemory(subject: String, value: String) {
        viewModelScope.launch {
            repository.saveMemory(subject, value)
        }
    }

    fun deleteMemory(memory: MemoryFactEntity) {
        viewModelScope.launch {
            repository.deleteMemory(memory)
        }
    }

    fun addNote(title: String, content: String) {
        viewModelScope.launch {
            repository.insertNote(title, content)
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun clearCommandLogs() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun deleteGeminiInteraction(interaction: GeminiInteractionEntity) {
        viewModelScope.launch {
            repository.deleteGeminiInteraction(interaction)
        }
    }

    fun clearGeminiInteractions() {
        viewModelScope.launch {
            repository.clearGeminiInteractions()
        }
    }
}
