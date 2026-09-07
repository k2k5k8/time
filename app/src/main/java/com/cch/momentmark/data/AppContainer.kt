package com.cch.momentmark.data

import android.content.Context
import com.cch.momentmark.data.local.MomentMarkDatabase
import com.cch.momentmark.data.repository.MomentRepository
import com.cch.momentmark.data.repository.TaskRepository
import com.cch.momentmark.data.repository.GroupRepository
import com.cch.momentmark.data.settings.MomentMarkGroupStore
import com.cch.momentmark.data.settings.MomentMarkSettingsStore
import java.io.Closeable
import java.time.Clock

/** Application-scoped construction boundary for local persistence dependencies. */
class AppContainer(context: Context) : Closeable {
    private val database = MomentMarkDatabase.create(context.applicationContext)

    /** 展示派生统一走同一时钟；测试注入固定 Clock。 */
    val clock: Clock = Clock.systemDefaultZone()

    val momentRepository = MomentRepository(database.momentDao(), clock)
    val taskRepository = TaskRepository(database.taskDao(), clock)
    val groupRepository = GroupRepository(database, database.groupDao(), database.momentDao(), database.taskDao(), clock::millis)
    val settingsStore = MomentMarkSettingsStore(context)
    val groupStore = MomentMarkGroupStore(context)

    override fun close() {
        database.close()
    }
}
