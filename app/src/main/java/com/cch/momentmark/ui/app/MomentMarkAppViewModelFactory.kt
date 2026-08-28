package com.cch.momentmark.ui.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cch.momentmark.data.AppContainer
import com.cch.momentmark.domain.model.TimeEvent

class MomentMarkAppViewModelFactory(
    context: Context,
    private val seedEvents: List<TimeEvent> = com.cch.momentmark.data.SampleEvents.all,
) : ViewModelProvider.Factory {
    private val applicationContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MomentMarkAppViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        val container = AppContainer(applicationContext)
        return MomentMarkAppViewModel(
            eventRepository = container.eventRepository,
            settingsStore = container.settingsStore,
            groupStore = container.groupStore,
            seedEvents = seedEvents,
            detailStore = container.detailStore,
            onClearedCallback = container::close,
        ) as T
    }
}
