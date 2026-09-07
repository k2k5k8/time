package com.cch.momentmark.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cch.momentmark.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

private val Context.momentMarkDataStore by preferencesDataStore(name = "moment_mark_settings")

interface MomentMarkSettingsStorePort {
    val themeMode: Flow<ThemeMode>
    val autoPurgeEnabled: Flow<Boolean>
        get() = flowOf(false)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setAutoPurgeEnabled(enabled: Boolean) = Unit
}

class MomentMarkSettingsStore(
    context: Context,
) : MomentMarkSettingsStorePort {
    private val dataStore = context.applicationContext.momentMarkDataStore

    override val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        preferences[Keys.themeMode]
            ?.let { value -> runCatching { ThemeMode.valueOf(value) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    }

    override val autoPurgeEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[Keys.autoPurgeEnabled] ?: false
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences -> preferences[Keys.themeMode] = mode.name }
    }

    override suspend fun setAutoPurgeEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[Keys.autoPurgeEnabled] = enabled }
    }

    /** Test/reset hook for the settings boundary; no UI calls this in stage 3. */
    suspend fun clear() {
        dataStore.edit { preferences -> preferences.clear() }
    }

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val autoPurgeEnabled = booleanPreferencesKey("auto_purge_enabled")
    }
}
