package com.cch.momentmark.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

private val Context.momentMarkGroupDataStore by preferencesDataStore(name = "moment_mark_groups")

/** Stores empty/user-created groups; groups attached to events are still derived from the event source. */
class MomentMarkGroupStore(context: Context) {
    private val dataStore = context.applicationContext.momentMarkGroupDataStore

    val groups: Flow<List<String>> = dataStore.data.map { preferences ->
        runCatching {
            val values = JSONArray(preferences[Keys.groups].orEmpty())
            (0 until values.length()).mapNotNull { index ->
                values.optString(index).trim().takeIf(String::isNotBlank)
            }
        }.getOrDefault(emptyList())
    }

    suspend fun saveGroups(groups: List<String>) {
        val normalized = groups.map(String::trim).filter(String::isNotBlank).distinct()
        dataStore.edit { preferences ->
            preferences[Keys.groups] = JSONArray(normalized).toString()
        }
    }

    private object Keys {
        val groups = stringPreferencesKey("groups")
    }
}
