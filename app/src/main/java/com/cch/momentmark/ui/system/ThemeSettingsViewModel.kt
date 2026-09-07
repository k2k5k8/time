package com.cch.momentmark.ui.system

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cch.momentmark.data.settings.MomentMarkSettingsStore
import com.cch.momentmark.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow

/** Keeps the app-wide display preference outside Composables and the retired event feed. */
class ThemeSettingsViewModel(
    val themeMode: Flow<ThemeMode>,
) : ViewModel()

class ThemeSettingsViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val applicationContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ThemeSettingsViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return ThemeSettingsViewModel(MomentMarkSettingsStore(applicationContext).themeMode) as T
    }
}
