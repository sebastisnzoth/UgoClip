package com.ugo.clip.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity dedicated to persisting transcribed speech alongside the corresponding
 * Gemini AI response (summary, analysis, structured insights, model name, etc.)
 * so users can browse, review, and search their AI interaction history.
 */
@Entity(tableName = "gemini_interactions")
data class GeminiInteractionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transcribedText: String,
    val aiResponse: String,
    val interactionType: String = "QUERY", // QUERY, SUMMARY, ANALYSIS
    val keyPointsJson: String = "", // JSON or bullet points string
    val actionItemsJson: String = "", // JSON or bullet points string
    val modelName: String = "gemini-3.5-flash",
    val isRealAi: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)
