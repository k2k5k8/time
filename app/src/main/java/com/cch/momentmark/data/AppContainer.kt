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

/**
 * Application-scoped construction boundary for local persistence dependencies.
 *
 * 进程内必须共享同一个 [AppContainer]（从而共享同一个 Room 数据库实例）：
 * Room 的失效通知（InvalidationTracker）按连接注册，若表单/详情/回收站各自持有
 * 独立数据库实例，跨实例的写入不会触发首页 `observeActive()` Flow 重新发射，
 * 首页会停留在旧数据上（表现为"内容写死、不随操作刷新"）。因此构造函数私有，
 * 一律通过 [get] 获取共享实例。
 */
class AppContainer private constructor(context: Context) : Closeable {
    private val database = MomentMarkDatabase.create(context.applicationContext)

    /** 展示派生统一走同一时钟；测试注入固定 Clock。 */
    val clock: Clock = Clock.systemDefaultZone()

    val momentRepository = MomentRepository(database.momentDao(), clock)
    val taskRepeatRepository = com.cch.momentmark.data.repository.TaskRepeatRepository(database, clock)
    val taskPlanningRepository = com.cch.momentmark.data.repository.TaskPlanningRepository(database.taskPlanningDao(), clock, taskRepeatRepository::reconcile)
    val taskRepository = TaskRepository(database.taskDao(), clock, taskRepeatRepository)
    val groupRepository = GroupRepository(database, database.groupDao(), database.momentDao(), database.taskDao(), clock::millis)
    val settingsStore = MomentMarkSettingsStore(context)
    val groupStore = MomentMarkGroupStore(context)

    override fun close() {
        database.close()
    }

    companion object {
        private val lock = Any()

        @Volatile
        private var shared: AppContainer? = null

        /** 返回进程级共享容器；首次调用时按 applicationContext 惰性创建。 */
        fun get(context: Context): AppContainer =
            shared ?: synchronized(lock) {
                shared ?: AppContainer(context.applicationContext).also { shared = it }
            }
    }
}
