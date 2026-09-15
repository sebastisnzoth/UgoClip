package com.ugo.clip.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ugo.clip.data.model.CommandLogEntity
import com.ugo.clip.data.model.GeminiInteractionEntity
import com.ugo.clip.data.model.MemoryFactEntity
import com.ugo.clip.data.model.NoteEntity
import com.ugo.clip.data.model.ReminderEntity
import com.ugo.clip.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HugoDao {

    // Notes
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    // Tasks
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, timestamp DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    // Reminders
    @Query("SELECT * FROM reminders ORDER BY isDone ASC, timestamp DESC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    // Memories (Object location & general facts)
    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<MemoryFactEntity>>

    @Query("SELECT * FROM memories WHERE subject LIKE '%' || :query || '%' OR value LIKE '%' || :query || '%' LIMIT 5")
    suspend fun searchMemory(query: String): List<MemoryFactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryFactEntity): Long

    @Delete
    suspend fun deleteMemory(memory: MemoryFactEntity)

    // Command History / Audit
    @Query("SELECT * FROM command_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentCommandLogs(): Flow<List<CommandLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommandLog(log: CommandLogEntity): Long

    @Query("DELETE FROM command_logs")
    suspend fun clearCommandLogs()

    // Gemini AI Voice & Interaction History
    @Query("SELECT * FROM gemini_interactions ORDER BY timestamp DESC")
    fun getAllGeminiInteractions(): Flow<List<GeminiInteractionEntity>>

    @Query("SELECT * FROM gemini_interactions WHERE transcribedText LIKE '%' || :query || '%' OR aiResponse LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchGeminiInteractions(query: String): Flow<List<GeminiInteractionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeminiInteraction(interaction: GeminiInteractionEntity): Long

    @Delete
    suspend fun deleteGeminiInteraction(interaction: GeminiInteractionEntity)

    @Query("DELETE FROM gemini_interactions")
    suspend fun clearGeminiInteractions()
}
