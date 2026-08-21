package com.cch.momentmark.ui.settings

import androidx.compose.runtime.Composable
import com.cch.momentmark.ui.SettingsScreen
import com.cch.momentmark.ui.theme.ThemeMode

/** Settings feature boundary; theme persistence is provided by the app data layer. */
@Composable
fun SettingsFeature(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onBack: () -> Unit,
) {
    SettingsScreen(
        themeMode = themeMode,
        onThemeModeChange = onThemeModeChange,
        onBack = onBack,
    )
}
