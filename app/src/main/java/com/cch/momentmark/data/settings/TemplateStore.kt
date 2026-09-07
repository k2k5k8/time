package com.cch.momentmark.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

private val Context.templateDataStore by preferencesDataStore(name = "moment_mark_templates")

class TemplateStore(context: Context) {
    private val dataStore = context.applicationContext.templateDataStore
    private val favoritesKey = stringPreferencesKey("favorite_templates")

    val favoriteTemplates: Flow<Set<String>> = dataStore.data.map { preferences ->
        runCatching {
            val array = JSONArray(preferences[favoritesKey].orEmpty())
            buildSet { repeat(array.length()) { add(array.getString(it)) } }
        }.getOrDefault(emptySet())
    }

    suspend fun setFavorite(templateId: String, favorite: Boolean) {
        dataStore.edit { preferences ->
            val current = runCatching {
                val array = JSONArray(preferences[favoritesKey].orEmpty())
                buildSet { repeat(array.length()) { add(array.getString(it)) } }
            }.getOrDefault(emptySet()).toMutableSet()
            if (favorite) current += templateId else current -= templateId
            preferences[favoritesKey] = JSONArray(current.toList()).toString()
        }
    }
}
