package com.cch.momentmark.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

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
    private val systemEvents: List<DaybookEvent> = buildList {
        fun addDate(
            id: String,
            date: LocalDate,
            title: String,
            subtitle: String = "",
            type: DaybookEventType = DaybookEventType.FESTIVAL,
        ) {
            add(
                DaybookEvent(
                    id = id,
                    date = date,
                    title = title,
                    subtitle = subtitle,
                    source = DaybookEventSource.SYSTEM,
                    eventType = type,
                ),
            )
        }

        fun add(
            id: String,
            month: Int,
            day: Int,
            title: String,
            subtitle: String = "",
            type: DaybookEventType = DaybookEventType.FESTIVAL,
        ) = addDate(id, LocalDate.of(today.year, month, day), title, subtitle, type)

        // ── 公历节日 ──
        add("system-new-year", 1, 1, "元旦")
        add("system-valentine", 2, 14, "情人节")
        add("system-women", 3, 8, "妇女节")
        add("system-arbor", 3, 12, "植树节")
        add("system-fool", 4, 1, "愚人节")
        add("system-qingming", 4, 5, "清明节", "祭祖踏青")
        add("system-labor", 5, 1, "劳动节")
        add("system-youth", 5, 4, "青年节")
        add("system-child", 6, 1, "儿童节")
        add("system-teacher", 9, 10, "教师节")
        add("system-national", 10, 1, "国庆节", "黄金周")
        add("system-halloween", 10, 31, "万圣节")
        add("system-double-11", 11, 11, "双11", "购物狂欢节")
        add("system-christmas", 12, 25, "圣诞节")

        // ── 购物节 ──
        add("system-618", 6, 18, "618", "年中购物节")
        add("system-double-12", 12, 12, "双12", "年终钜惠")

        // ── 农历节日（按当年公历日期内置，雏形数据，接入真实农历库后可替换） ──
        add("system-laba", 1, 26, "腊八节", "农历十二月初八")
        add("system-new-year-eve", 2, 16, "除夕", "辞旧岁")
        add("system-spring-festival", 2, 17, "春节", "农历正月初一")
        add("system-lantern", 3, 3, "元宵节", "农历正月十五")
        add("system-dragon-boat", 6, 19, "端午节", "农历五月初五")
        add("system-qixi", 8, 19, "七夕", "农历七月初七")
        add("system-mid-autumn", 9, 25, "中秋节", "农历八月十五")
        add("system-chongyang", 10, 18, "重阳节", "农历九月初九")
        add("system-winter-solstice", 12, 21, "冬至", "数九寒天始", DaybookEventType.SOLAR_TERM)

        // ── 趣味/网络节日 ──
        add("system-girls-day", 3, 7, "女生节")
        add("system-white-day", 3, 14, "白色情人节", "也是圆周率日 π")
        add("system-earth-day", 4, 22, "世界地球日")
        add("system-reading-day", 4, 23, "世界读书日")
        add("system-520", 5, 20, "520表白日", "网络情人节")
        add("system-kiss-day", 7, 6, "国际接吻日")
        add("system-papa-day", 8, 8, "爸爸节", "八八节")
        add("system-programmer-day", 10, 24, "程序员节", "1024")
        add("system-christmas-eve", 12, 24, "平安夜")

        // ── 二十四节气（常见公历日期，个别年份可能相差一天） ──
        add("system-minor-cold", 1, 5, "小寒", type = DaybookEventType.SOLAR_TERM)
        add("system-major-cold", 1, 20, "大寒", type = DaybookEventType.SOLAR_TERM)
        add("system-spring-begins", 2, 4, "立春", type = DaybookEventType.SOLAR_TERM)
        add("system-rain-water", 2, 19, "雨水", type = DaybookEventType.SOLAR_TERM)
        add("system-insects-awaken", 3, 5, "惊蛰", type = DaybookEventType.SOLAR_TERM)
        add("system-spring-equinox", 3, 20, "春分", type = DaybookEventType.SOLAR_TERM)
        add("system-grain-rain", 4, 20, "谷雨", type = DaybookEventType.SOLAR_TERM)
        add("system-summer-begins", 5, 5, "立夏", type = DaybookEventType.SOLAR_TERM)
        add("system-grain-buds", 5, 21, "小满", type = DaybookEventType.SOLAR_TERM)
        add("system-grain-in-ear", 6, 5, "芒种", type = DaybookEventType.SOLAR_TERM)
        add("system-summer-solstice", 6, 21, "夏至", type = DaybookEventType.SOLAR_TERM)
        add("system-minor-heat", 7, 7, "小暑", type = DaybookEventType.SOLAR_TERM)
        add("system-major-heat", 7, 22, "大暑", type = DaybookEventType.SOLAR_TERM)
        add("system-autumn-begins", 8, 7, "立秋", type = DaybookEventType.SOLAR_TERM)
        add("system-end-of-heat", 8, 23, "处暑", type = DaybookEventType.SOLAR_TERM)
        add("system-white-dew", 9, 7, "白露", type = DaybookEventType.SOLAR_TERM)
        add("system-autumn-equinox", 9, 23, "秋分", type = DaybookEventType.SOLAR_TERM)
        add("system-cold-dew", 10, 8, "寒露", type = DaybookEventType.SOLAR_TERM)
        add("system-frost-descends", 10, 23, "霜降", type = DaybookEventType.SOLAR_TERM)
        add("system-winter-begins", 11, 7, "立冬", type = DaybookEventType.SOLAR_TERM)
        add("system-minor-snow", 11, 22, "小雪", type = DaybookEventType.SOLAR_TERM)
        add("system-major-snow", 12, 7, "大雪", type = DaybookEventType.SOLAR_TERM)

        // ── 计算型节日 ──
        addDate(
            id = "system-mothers-day",
            date = LocalDate.of(today.year, 5, 1)
                .with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.SUNDAY)),
            title = "母亲节",
            subtitle = "五月第二个周日",
        )
        addDate(
            id = "system-fathers-day",
            date = LocalDate.of(today.year, 6, 1)
                .with(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.SUNDAY)),
            title = "父亲节",
            subtitle = "六月第三个周日",
        )
        addDate(
            id = "system-thanksgiving",
            date = LocalDate.of(today.year, 11, 1)
                .with(TemporalAdjusters.dayOfWeekInMonth(4, DayOfWeek.THURSDAY)),
            title = "感恩节",
            subtitle = "十一月第四个周四",
        )
        addDate(
            id = "system-earth-hour",
            date = LocalDate.of(today.year, 3, 1)
                .with(TemporalAdjusters.lastInMonth(DayOfWeek.SATURDAY)),
            title = "地球一小时",
            subtitle = "三月最后一个周六 20:30 熄灯",
        )
        addDate(
            id = "system-black-friday",
            date = LocalDate.of(today.year, 11, 1)
                .with(TemporalAdjusters.dayOfWeekInMonth(4, DayOfWeek.THURSDAY))
                .plusDays(1),
            title = "黑色星期五",
            subtitle = "海外购物狂欢",
        )
    }

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
