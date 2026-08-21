package com.cch.momentmark.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.DayOfWeek

enum class EventTimeType {
    ALL_DAY,
    TIMED,
}

enum class EventCalendarType {
    SOLAR,
    LUNAR,
}

enum class RepeatType {
    YEARLY,
    MONTHLY,
    WEEKLY,
    CUSTOM,
}

enum class RepeatCustomUnit {
    DAY,
    WEEK,
}

data class RepeatRule(
    val type: RepeatType,
    val interval: Int = 1,
    val customUnit: RepeatCustomUnit = RepeatCustomUnit.DAY,
    val weekday: DayOfWeek? = null,
)

data class ReminderConfig(
    val offsetMinutes: Int,
)

enum class EventCoverPreset {
    DEFAULT,
    CREAM,
    SUNSET,
}

enum class NotificationMethod {
    IN_APP,
    SYSTEM,
}

data class TimeEvent(
    val id: String,
    val title: String,
    /** Shared large title used by every countdown-card template. */
    val subtitle: String = "",
    /** Shared grouping field; a template may choose to hide it visually. */
    val groupLabel: String = "",
    val timeType: EventTimeType,
    val dateLabel: String,
    val relativeLabel: String,
    val note: String = "",
    val icon: String,
    val colorRole: EventColorRole,
    val cardPaletteKey: EventCardPaletteKey,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val cardTemplateKey: EventCardTemplateKey = EventCardTemplateKey.CLASSIC,
    val travelCardConfig: TravelCardConfig? = null,
    /** ALL_DAY 的领域输入；展示文案由 EventTimeCalculator 派生。 */
    val localDate: LocalDate? = null,
    /** TIMED 的绝对时间；展示时使用 [zoneId]。 */
    val targetInstant: Instant? = null,
    val zoneId: String? = null,
    val calendarType: EventCalendarType = EventCalendarType.SOLAR,
    val isRepeat: Boolean = false,
    val repeatRule: RepeatRule? = null,
    val reminder: ReminderConfig? = null,
    val coverPreset: EventCoverPreset = EventCoverPreset.DEFAULT,
    val notificationMethods: Set<NotificationMethod> = setOf(NotificationMethod.IN_APP),
)

/** A detail-page-only countdown. It is intentionally not included in the home Room feed. */
data class RelatedCountdown(
    val id: String,
    val title: String,
    val targetDate: LocalDate,
    val isRepeat: Boolean = false,
    val repeatType: String = "YEARLY",
)

/**
 * 所有倒计时卡片共用的内容契约。
 *
 * 模板只负责布局、字体与装饰；必须从此结构读取大标题、小标题、倒计时、
 * 起始时间和分组。小标题或分组可由视觉样式选择隐藏，但不得改为模板私有字段。
 */
data class TimeCardFields(
    val title: String,
    val subtitle: String,
    val countdownLabel: String,
    val startTimeLabel: String,
    val groupLabel: String,
)

fun TimeEvent.cardFields(): TimeCardFields {
    val travelConfig = travelCardConfig
    return TimeCardFields(
        title = title.ifBlank { travelConfig?.title.orEmpty() },
        subtitle = subtitle.ifBlank { travelConfig?.badgeLabel.orEmpty() },
        countdownLabel = relativeLabel,
        startTimeLabel = dateLabel,
        groupLabel = groupLabel.ifBlank { travelConfig?.groupLabel.orEmpty() },
    )
}

enum class EventColorRole {
    FUTURE,
    PAST,
}

/** 预留给后续事件编辑页的整套卡片配色选择。 */
enum class EventCardPaletteKey {
    BLUE_WHITE,
    ORANGE_WHITE,
}

enum class EventCardTemplateKey {
    CLASSIC,
    TRAVEL_MINIMAL_EDITORIAL,
    TRAVEL_SUNSET_GLASS,
    TRAVEL_SCRAPBOOK,
    /** 兼容之前已经保存的旅行卡数据，继续按手帐贴纸风渲染。 */
    TRAVEL_COUNTDOWN,
}

/**
 * 旅行倒计时模板的可替换内容；后续编辑页可直接将这些字段接入本地存储。
 */
data class TravelCardConfig(
    val badgeLabel: String = "纪念日",
    val title: String = "东京旅行",
    val groupLabel: String = "旅行与生活",
    val countdownUnit: String = "天",
    val targetDate: LocalDate = LocalDate.of(2026, 9, 16),
    val locationLabel: String = "东京",
    val backgroundPreset: TravelBackgroundPreset = TravelBackgroundPreset.SCRAPBOOK_CREAM,
    val badgeIcon: TravelCardIcon = TravelCardIcon.CALENDAR,
    val dateIcon: TravelCardIcon = TravelCardIcon.CLOCK,
    val size: TravelCardSize = TravelCardSize.WIDE,
)

enum class TravelBackgroundPreset {
    SCRAPBOOK_CREAM,
    TOKYO_SUNSET,
}

enum class TravelCardIcon {
    CALENDAR,
    CLOCK,
    HEART,
    AIRPLANE,
}

enum class TravelCardSize {
    SMALL,
    WIDE,
}
