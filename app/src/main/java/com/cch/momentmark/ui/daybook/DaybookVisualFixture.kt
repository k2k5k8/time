package com.cch.momentmark.ui.daybook

import com.cch.momentmark.domain.model.Task
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * DEBUG 视觉回归 fixture：仅供 `adb shell setprop debug.momentmark.daybook aug12`
 * 启动时使用，匹配 design.html 第 ⑤ 屏「日子簿 · 副本地图」的示例数据。
 *
 * 这些任务只用于生成与设计稿一致的截图，不会写入生产数据库；release 构建
 * 不应触发该 fixture（调用方必须检查 `BuildConfig.DEBUG`）。
 */
object DaybookVisualFixture {

    /** 设计稿冻结日期：2026-08-12（today + selectedDate）。 */
    val today: LocalDate = LocalDate.of(2026, 8, 12)

    /** 日子簿 fixture：
     * - 08.12 显示 6 条任务（与设计稿源码一致；第 6 条「预约牙医复诊」与
     *   「接受新任务」行在初始视口被滚动区域截断，与 design.png 对比帧一致）。
     * - 08.16 放一条仅用于日历内容标记 🎁 的任务（设计稿「八月」日格 16 号有标记）。
     *   设计稿的 🎓 标记在**七月** 1 号（不可见月），因此「八月」1 号必须无标记，
     *   这里不放置 08.01 的任务。
     */
    val tasks: List<Task> = run {
        val zone = ZoneId.of("Asia/Shanghai")
        fun instant(date: LocalDate, hour: Int, minute: Int): Instant =
            date.atTime(hour, minute).atZone(zone).toInstant()
        fun instant(hour: Int, minute: Int): Instant = instant(today, hour, minute)

        listOf(
            Task(
                id = "fixture-1",
                title = "早起打卡",
                dueLocalDate = today,
                dueInstant = instant(8, 0),
                zoneId = zone,
                note = "",
                taskType = null,
                difficulty = null,
                groupId = null,
                isCompleted = true,
                completedAt = Instant.EPOCH,
                showOnHome = false,
                deletedAt = null,
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            ),
            Task(
                id = "fixture-2",
                title = "抢演唱会门票",
                dueLocalDate = today,
                dueInstant = instant(10, 0),
                zoneId = zone,
                note = "",
                taskType = null,
                difficulty = null,
                groupId = null,
                isCompleted = false,
                completedAt = null,
                showOnHome = false,
                deletedAt = null,
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            ),
            Task(
                id = "fixture-3",
                title = "整理旅行清单",
                dueLocalDate = today,
                dueInstant = instant(13, 0),
                zoneId = zone,
                note = "",
                taskType = null,
                difficulty = null,
                groupId = null,
                isCompleted = true,
                completedAt = Instant.EPOCH,
                showOnHome = false,
                deletedAt = null,
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            ),
            Task(
                id = "fixture-4",
                title = "提交报名表",
                dueLocalDate = today,
                dueInstant = null,
                zoneId = null,
                note = "",
                taskType = null,
                difficulty = null,
                groupId = null,
                isCompleted = false,
                completedAt = null,
                showOnHome = false,
                deletedAt = null,
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            ),
            Task(
                id = "fixture-5",
                title = "晨跑 3 公里",
                dueLocalDate = today,
                dueInstant = instant(7, 0),
                zoneId = zone,
                note = "",
                taskType = null,
                difficulty = null,
                groupId = null,
                isCompleted = true,
                completedAt = Instant.EPOCH,
                showOnHome = false,
                deletedAt = null,
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            ),
            Task(
                id = "fixture-6",
                title = "预约牙医复诊",
                dueLocalDate = today,
                dueInstant = instant(14, 30),
                zoneId = zone,
                note = "",
                taskType = null,
                difficulty = null,
                groupId = null,
                isCompleted = false,
                completedAt = null,
                showOnHome = false,
                deletedAt = null,
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            ),
            // 08.16 的内容标记任务，列表中不可见，只让日历格显示 🎁。
            Task(
                id = "fixture-marker-16",
                title = "交实验报告",
                dueLocalDate = LocalDate.of(2026, 8, 16),
                dueInstant = null,
                zoneId = null,
                note = "",
                taskType = null,
                difficulty = null,
                groupId = null,
                isCompleted = false,
                completedAt = null,
                showOnHome = false,
                deletedAt = null,
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            ),
        )
    }
}
