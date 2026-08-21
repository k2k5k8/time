package com.cch.momentmark.domain.model

import java.time.LocalDate
import java.time.YearMonth

/** 日子簿事件的来源，系统事件与用户记录在 UI 上保持轻微区分。 */
enum class DaybookEventSource {
    SYSTEM,
    USER,
}

enum class DaybookEventType {
    FESTIVAL,
    SOLAR_TERM,
    ANNIVERSARY,
    PERSONAL,
    REMINDER,
}

/**
 * 日子簿的展示层事件契约。
 *
 * 这里不替代 [TimeEvent]，而是为日历页提供一个可以同时承载系统节日、
 * 用户记录和后续农历/节气数据源的轻量接口。用户事件可通过
 * [showInMilestone] 选择是否进入“大事件”筛选流，默认不进入。
 */
data class DaybookEvent(
    val id: String,
    val date: LocalDate,
    val title: String,
    val subtitle: String = "",
    val source: DaybookEventSource,
    val eventType: DaybookEventType,
    val showInMilestone: Boolean = false,
    val isAllDay: Boolean = true,
)

/** 日历页与数据源之间的最小边界，后续可替换为 Room/节日库实现。 */
interface DaybookDataSource {
    fun eventsForMonth(month: YearMonth): List<DaybookEvent>
    fun eventsForDate(date: LocalDate): List<DaybookEvent>
}

/**
 * 雏形阶段的数据源：系统事件使用内置的轻量年度表，用户事件从已有 TimeEvent
 * 映射而来。真实节日库和 Daybook Room 表接入时只需替换此实现。
 */
class MockDaybookDataSource(
    userEvents: List<TimeEvent>,
    private val today: LocalDate = LocalDate.now(),
) : DaybookDataSource {
    private val systemEvents = listOf(
        DaybookEvent("system-new-year", LocalDate.of(today.year, 1, 1), "元旦", source = DaybookEventSource.SYSTEM, eventType = DaybookEventType.FESTIVAL),
        DaybookEvent("system-valentine", LocalDate.of(today.year, 2, 14), "情人节", source = DaybookEventSource.SYSTEM, eventType = DaybookEventType.FESTIVAL),
        DaybookEvent("system-women", LocalDate.of(today.year, 3, 8), "妇女节", source = DaybookEventSource.SYSTEM, eventType = DaybookEventType.FESTIVAL),
        DaybookEvent("system-labor", LocalDate.of(today.year, 5, 1), "劳动节", source = DaybookEventSource.SYSTEM, eventType = DaybookEventType.FESTIVAL),
        DaybookEvent("system-child", LocalDate.of(today.year, 6, 1), "儿童节", source = DaybookEventSource.SYSTEM, eventType = DaybookEventType.FESTIVAL),
        DaybookEvent("system-qixi", LocalDate.of(today.year, 8, 19), "七夕", source = DaybookEventSource.SYSTEM, eventType = DaybookEventType.FESTIVAL),
        DaybookEvent("system-mid-autumn", LocalDate.of(today.year, 9, 25), "中秋节", source = DaybookEventSource.SYSTEM, eventType = DaybookEventType.FESTIVAL),
        DaybookEvent("system-national", LocalDate.of(today.year, 10, 1), "国庆节", source = DaybookEventSource.SYSTEM, eventType = DaybookEventType.FESTIVAL),
        DaybookEvent("system-winter-solstice", LocalDate.of(today.year, 12, 21), "冬至", source = DaybookEventSource.SYSTEM, eventType = DaybookEventType.SOLAR_TERM),
    )

    private val userDaybookEvents = userEvents.mapNotNull { event ->
        val date = event.localDate ?: event.targetInstant?.atZone(java.time.ZoneId.systemDefault())?.toLocalDate()
        date?.let {
            DaybookEvent(
                id = "user-${event.id}",
                date = it,
                title = event.title,
                subtitle = event.subtitle,
                source = DaybookEventSource.USER,
                eventType = if (event.isRepeat) DaybookEventType.ANNIVERSARY else DaybookEventType.PERSONAL,
                showInMilestone = event.isPinned,
                isAllDay = event.timeType == EventTimeType.ALL_DAY,
            )
        }
    }

    // 给雏形一个当天可观察的用户记录，真实数据接入后可移除这条 mock。
    private val prototypeEvent = DaybookEvent(
        id = "prototype-daybook-note",
        date = today,
        title = "留一点时间给今天",
        subtitle = "日子簿示例记录",
        source = DaybookEventSource.USER,
        eventType = DaybookEventType.PERSONAL,
    )

    private val allEvents: List<DaybookEvent> = (systemEvents + userDaybookEvents + prototypeEvent)
        .distinctBy { "${it.id}-${it.date}" }

    override fun eventsForMonth(month: YearMonth): List<DaybookEvent> = allEvents.filter { YearMonth.from(it.date) == month }

    override fun eventsForDate(date: LocalDate): List<DaybookEvent> = allEvents.filter { it.date == date }
}
