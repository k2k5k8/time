package com.cch.momentmark.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cch.momentmark.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.momentMarkDataStore by preferencesDataStore(name = "moment_mark_settings")

class MomentMarkSettingsStore(
    context: Context,
) {
    private val dataStore = context.applicationContext.momentMarkDataStore

    val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        preferences[Keys.themeMode]
            ?.let { value -> runCatching { ThemeMode.valueOf(value) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences -> preferences[Keys.themeMode] = mode.name }
    }

    /** Test/reset hook for the settings boundary; no UI calls this in stage 3. */
    suspend fun clear() {
        dataStore.edit { preferences -> preferences.clear() }
    }

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
    }
}
