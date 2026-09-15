package com.ugo.clip.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HugoStore(context: Context) {
    private val prefs = context.getSharedPreferences("hugo_memory", Context.MODE_PRIVATE)

    fun addNote(text: String) {
        val array = JSONArray(prefs.getString(KEY_NOTES, "[]"))
        array.put(
            JSONObject()
                .put("text", text)
                .put("createdAt", System.currentTimeMillis())
        )
        prefs.edit().putString(KEY_NOTES, array.toString()).apply()
    }

    fun rememberLocation(subject: String, location: String) {
        val obj = JSONObject(prefs.getString(KEY_LOCATIONS, "{}"))
        obj.put(
            subject.normalizeKey(),
            JSONObject()
                .put("subject", subject)
                .put("location", location)
                .put("updatedAt", System.currentTimeMillis())
        )
        prefs.edit().putString(KEY_LOCATIONS, obj.toString()).apply()
    }

    fun findLocation(subject: String): String? {
        val obj = JSONObject(prefs.getString(KEY_LOCATIONS, "{}"))
        val value = obj.optJSONObject(subject.normalizeKey()) ?: return null
        return value.optString("location").takeIf { it.isNotBlank() }
    }

    fun addAgenda(title: String, startsAt: Long) {
        val array = JSONArray(prefs.getString(KEY_AGENDA, "[]"))
        array.put(
            JSONObject()
                .put("title", title)
                .put("startsAt", startsAt)
                .put("createdAt", System.currentTimeMillis())
        )
        prefs.edit().putString(KEY_AGENDA, array.toString()).apply()
    }

    fun agendaFor(date: LocalDate): List<AgendaItem> {
        val array = JSONArray(prefs.getString(KEY_AGENDA, "[]"))
        val zone = ZoneId.systemDefault()
        val result = mutableListOf<AgendaItem>()

        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val startsAt = obj.optLong("startsAt", -1L)
            if (startsAt <= 0L) continue
            val itemDate = Instant.ofEpochMilli(startsAt).atZone(zone).toLocalDate()
            if (itemDate == date) {
                result += AgendaItem(
                    title = obj.optString("title", "Compromisso"),
                    startsAt = startsAt
                )
            }
        }
        return result.sortedBy { it.startsAt }
    }

    data class AgendaItem(
        val title: String,
        val startsAt: Long
    )

    private fun String.normalizeKey(): String =
        trim().lowercase().replace(Regex("\\s+"), " ")

    companion object {
        private const val KEY_NOTES = "notes"
        private const val KEY_LOCATIONS = "locations"
        private const val KEY_AGENDA = "agenda"
    }
}
