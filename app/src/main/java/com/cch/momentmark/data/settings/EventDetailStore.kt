package com.cch.momentmark.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cch.momentmark.domain.model.RelatedCountdown
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.eventDetailDataStore by preferencesDataStore(name = "moment_mark_event_details")

/** Persistence boundary for state that belongs to a detail page, not the home event fact. */
class EventDetailStore(context: Context) {
    private val dataStore = context.applicationContext.eventDetailDataStore

    fun selectedBackground(eventId: String): Flow<String?> = dataStore.data.map { preferences ->
        preferences[stringPreferencesKey("background_$eventId")]
    }

    suspend fun setBackground(eventId: String, assetName: String) {
        dataStore.edit { it[stringPreferencesKey("background_$eventId")] = assetName }
    }

    fun relatedCountdowns(eventId: String): Flow<List<RelatedCountdown>> = dataStore.data.map { preferences ->
        decodeRelated(preferences[stringPreferencesKey("related_$eventId")].orEmpty())
    }

    suspend fun saveRelated(eventId: String, item: RelatedCountdown) {
        val current = relatedCountdowns(eventId).first().filterNot { it.id == item.id } + item
        dataStore.edit { it[stringPreferencesKey("related_$eventId")] = encodeRelated(current) }
    }

    suspend fun deleteRelated(eventId: String, itemId: String) {
        val current = relatedCountdowns(eventId).first().filterNot { it.id == itemId }
        dataStore.edit { it[stringPreferencesKey("related_$eventId")] = encodeRelated(current) }
    }

    private fun encodeRelated(items: List<RelatedCountdown>): String = JSONArray().apply {
        items.forEach { item ->
            put(JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("targetDate", item.targetDate.toString())
                put("isRepeat", item.isRepeat)
                put("repeatType", item.repeatType)
            })
        }
    }.toString()

    private fun decodeRelated(value: String): List<RelatedCountdown> = runCatching {
        val array = JSONArray(value)
        buildList {
            repeat(array.length()) { index ->
                val item = array.getJSONObject(index)
                add(
                    RelatedCountdown(
                        id = item.optString("id"),
                        title = item.optString("title"),
                        targetDate = LocalDate.parse(item.optString("targetDate")),
                        isRepeat = item.optBoolean("isRepeat"),
                        repeatType = item.optString("repeatType", "YEARLY"),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())
}
