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
    /**
     * 默认首页事件是否已注入过一次（一次性标记）。未接线实现默认视为「已注入」，
     * 避免误触发清理/回填。
     */
    val defaultMomentsSeeded: Flow<Boolean>
        get() = flowOf(true)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setAutoPurgeEnabled(enabled: Boolean) = Unit
    /** 落「默认首页事件已注入」标记；此后不再清理历史示例行、也不再回填默认事件。 */
    suspend fun markDefaultMomentsSeeded() = Unit
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

    override val defaultMomentsSeeded: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[Keys.defaultMomentsSeeded] ?: false
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences -> preferences[Keys.themeMode] = mode.name }
    }

    override suspend fun setAutoPurgeEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[Keys.autoPurgeEnabled] = enabled }
    }

    override suspend fun markDefaultMomentsSeeded() {
        dataStore.edit { preferences -> preferences[Keys.defaultMomentsSeeded] = true }
    }

    /** Test/reset hook for the settings boundary; no UI calls this in stage 3. */
    suspend fun clear() {
        dataStore.edit { preferences -> preferences.clear() }
    }

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val autoPurgeEnabled = booleanPreferencesKey("auto_purge_enabled")
        val defaultMomentsSeeded = booleanPreferencesKey("default_moments_seeded")
    }
}
