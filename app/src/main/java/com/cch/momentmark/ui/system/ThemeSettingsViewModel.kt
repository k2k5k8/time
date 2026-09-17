package com.cch.momentmark.ui.system

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cch.momentmark.data.settings.MomentMarkSettingsStore
import com.cch.momentmark.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** Keeps the app-wide display preference outside Composables and the retired event feed. */
class ThemeSettingsViewModel(
    private val settingsStore: MomentMarkSettingsStore,
) : ViewModel() {
    val themeMode: Flow<ThemeMode> = settingsStore.themeMode

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsStore.setThemeMode(mode) }
    }
}

class ThemeSettingsViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val applicationContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ThemeSettingsViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return ThemeSettingsViewModel(MomentMarkSettingsStore(applicationContext)) as T
    }
}
