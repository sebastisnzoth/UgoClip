package com.ugo.clip.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val category: String = "Geral",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val isCompleted: Boolean = false,
    val dueDate: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val timeDescription: String,
    val isDone: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "memories")
data class MemoryFactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,
    val value: String,
    val category: String = "OBJECT_LOCATION", // OBJECT_LOCATION, PREFERENCE, GENERAL
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "command_logs")
data class CommandLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transcript: String,
    val intent: String,
    val response: String,
    val securityLevel: String = "LEVEL_A", // LEVEL_A, LEVEL_B, LEVEL_C
    val status: String = "SUCCESS", // SUCCESS, CONFIRMED, CANCELLED, BLOCKED, ERROR
    val timestamp: Long = System.currentTimeMillis()
)
