package com.ugo.clip.data.repository

import com.ugo.clip.data.db.HugoDao
import com.ugo.clip.data.model.CommandLogEntity
import com.ugo.clip.data.model.GeminiInteractionEntity
import com.ugo.clip.data.model.MemoryFactEntity
import com.ugo.clip.data.model.NoteEntity
import com.ugo.clip.data.model.ReminderEntity
import com.ugo.clip.data.model.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class HugoRepository(private val dao: HugoDao) {

    val notes: Flow<List<NoteEntity>> = dao.getAllNotes()
    val tasks: Flow<List<TaskEntity>> = dao.getAllTasks()
    val reminders: Flow<List<ReminderEntity>> = dao.getAllReminders()
    val memories: Flow<List<MemoryFactEntity>> = dao.getAllMemories()
    val commandLogs: Flow<List<CommandLogEntity>> = dao.getRecentCommandLogs()
    val geminiInteractions: Flow<List<GeminiInteractionEntity>> = dao.getAllGeminiInteractions()

    suspend fun insertNote(title: String, content: String, category: String = "Geral"): Long =
        withContext(Dispatchers.IO) {
            dao.insertNote(NoteEntity(title = title, content = content, category = category))
        }

    suspend fun deleteNote(note: NoteEntity) = withContext(Dispatchers.IO) {
        dao.deleteNote(note)
    }

    suspend fun insertTask(title: String, dueDate: String? = null): Long =
        withContext(Dispatchers.IO) {
            dao.insertTask(TaskEntity(title = title, dueDate = dueDate))
        }

    suspend fun toggleTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        dao.updateTask(task.copy(isCompleted = !task.isCompleted))
    }

    suspend fun deleteTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        dao.deleteTask(task)
    }

    suspend fun insertReminder(title: String, timeDescription: String): Long =
        withContext(Dispatchers.IO) {
            dao.insertReminder(ReminderEntity(title = title, timeDescription = timeDescription))
        }

    suspend fun toggleReminder(reminder: ReminderEntity) = withContext(Dispatchers.IO) {
        dao.updateReminder(reminder.copy(isDone = !reminder.isDone))
    }

    suspend fun deleteReminder(reminder: ReminderEntity) = withContext(Dispatchers.IO) {
        dao.deleteReminder(reminder)
    }

    suspend fun saveMemory(subject: String, value: String, category: String = "OBJECT_LOCATION"): Long =
        withContext(Dispatchers.IO) {
            dao.insertMemory(MemoryFactEntity(subject = subject.trim(), value = value.trim(), category = category))
        }

    suspend fun searchMemory(query: String): List<MemoryFactEntity> =
        withContext(Dispatchers.IO) {
            dao.searchMemory(query.trim())
        }

    suspend fun deleteMemory(memory: MemoryFactEntity) = withContext(Dispatchers.IO) {
        dao.deleteMemory(memory)
    }

    suspend fun logCommand(
        transcript: String,
        intent: String,
        response: String,
        securityLevel: String = "LEVEL_A",
        status: String = "SUCCESS"
    ): Long = withContext(Dispatchers.IO) {
        dao.insertCommandLog(
            CommandLogEntity(
                transcript = transcript,
                intent = intent,
                response = response,
                securityLevel = securityLevel,
                status = status
            )
        )
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        dao.clearCommandLogs()
    }

    suspend fun logGeminiInteraction(
        transcribedText: String,
        aiResponse: String,
        interactionType: String = "QUERY",
        keyPoints: List<String> = emptyList(),
        actionItems: List<String> = emptyList(),
        modelName: String = "gemini-3.5-flash",
        isRealAi: Boolean = true
    ): Long = withContext(Dispatchers.IO) {
        dao.insertGeminiInteraction(
            GeminiInteractionEntity(
                transcribedText = transcribedText.trim(),
                aiResponse = aiResponse.trim(),
                interactionType = interactionType,
                keyPointsJson = keyPoints.joinToString("\n"),
                actionItemsJson = actionItems.joinToString("\n"),
                modelName = modelName,
                isRealAi = isRealAi
            )
        )
    }

    suspend fun deleteGeminiInteraction(interaction: GeminiInteractionEntity) = withContext(Dispatchers.IO) {
        dao.deleteGeminiInteraction(interaction)
    }

    suspend fun clearGeminiInteractions() = withContext(Dispatchers.IO) {
        dao.clearGeminiInteractions()
    }

    fun searchGeminiInteractions(query: String): Flow<List<GeminiInteractionEntity>> =
        dao.searchGeminiInteractions(query.trim())

    /**
     * Seeds initial data if the database is clean, matching the specification examples
     */
    suspend fun seedInitialDataIfNeeded() = withContext(Dispatchers.IO) {
        val existingReminders = reminders.first()
        if (existingReminders.isEmpty()) {
            dao.insertReminder(ReminderEntity(title = "Reunião de alinhamento", timeDescription = "Hoje às 09:00"))
            dao.insertReminder(ReminderEntity(title = "Cobrar o João", timeDescription = "Quinta-feira às 09:00"))
            dao.insertTask(TaskEntity(title = "Comprar dois disjuntores", dueDate = "Hoje"))
            dao.insertTask(TaskEntity(title = "Comprar pão francês", dueDate = "Amanhã"))
            dao.insertMemory(MemoryFactEntity(subject = "furadeira", value = "armário azul", category = "OBJECT_LOCATION"))
            dao.insertMemory(MemoryFactEntity(subject = "chave reserva", value = "gaveta do escritório", category = "OBJECT_LOCATION"))
            dao.insertNote(NoteEntity(title = "Ideia Projeto Clip", content = "Secretário de voz offline-first com hardware BLE mínimo e Sentinel"))
            dao.insertCommandLog(
                CommandLogEntity(
                    transcript = "Modo HUGO inicializado",
                    intent = "SYSTEM_START",
                    response = "HUGO pronto. Microfone e sentinela ativos.",
                    securityLevel = "LEVEL_A",
                    status = "SUCCESS"
                )
            )
        }
    }
}
