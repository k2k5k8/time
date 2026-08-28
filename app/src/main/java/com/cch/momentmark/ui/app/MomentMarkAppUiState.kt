package com.cch.momentmark.ui.app

import com.cch.momentmark.domain.model.TimeEvent
import com.cch.momentmark.ui.theme.ThemeMode

data class MomentMarkAppUiState(
    val events: List<TimeEvent> = emptyList(),
    val deletedEvents: List<TimeEvent> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val groups: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
