package com.cch.momentmark.data

import android.content.Context
import com.cch.momentmark.data.local.MomentMarkDatabase
import com.cch.momentmark.data.repository.TimeEventRepository
import com.cch.momentmark.data.settings.EventDetailStore
import com.cch.momentmark.data.settings.MomentMarkGroupStore
import com.cch.momentmark.data.settings.MomentMarkSettingsStore
import java.io.Closeable

/** Application-scoped construction boundary for local persistence dependencies. */
class AppContainer(context: Context) : Closeable {
    private val database = MomentMarkDatabase.create(context.applicationContext)

    val eventRepository = TimeEventRepository(database.timeEventDao())
    val settingsStore = MomentMarkSettingsStore(context)
    val groupStore = MomentMarkGroupStore(context)
    val detailStore = EventDetailStore(context)

    override fun close() {
        database.close()
    }
}
