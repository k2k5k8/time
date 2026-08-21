package com.cch.momentmark.data

import com.cch.momentmark.domain.model.EventColorRole
import com.cch.momentmark.domain.model.EventCardPaletteKey
import com.cch.momentmark.domain.model.EventCardTemplateKey
import com.cch.momentmark.domain.model.EventTimeType
import com.cch.momentmark.domain.model.TravelCardConfig
import com.cch.momentmark.domain.model.TravelBackgroundPreset
import com.cch.momentmark.domain.model.TravelCardIcon
import com.cch.momentmark.domain.model.TimeEvent
import com.cch.momentmark.domain.model.TravelCardSize
import java.time.Instant
import java.time.LocalDate

/** 阶段 1 的内存展示数据；Room 与真实 CRUD 会在后续阶段接入。 */
object SampleEvents {
    val all: List<TimeEvent> = listOf(
        TimeEvent(
            id = "tokyo-trip",
            title = "东京旅行",
            subtitle = "纪念日",
            groupLabel = "旅行与生活",
            timeType = EventTimeType.ALL_DAY,
            dateLabel = "2026.09.16 星期三",
            relativeLabel = "还有 28 天",
            icon = "✈",
            colorRole = EventColorRole.FUTURE,
            cardPaletteKey = EventCardPaletteKey.BLUE_WHITE,
            isPinned = true,
            cardTemplateKey = EventCardTemplateKey.TRAVEL_MINIMAL_EDITORIAL,
            localDate = LocalDate.of(2026, 9, 16),
            travelCardConfig = TravelCardConfig(
                title = "东京旅行",
                groupLabel = "旅行与生活",
                targetDate = java.time.LocalDate.of(2026, 9, 16),
                size = TravelCardSize.WIDE,
            ),
        ),
        TimeEvent(
            id = "learn-kotlin",
            title = "开始学习 Kotlin",
            subtitle = "",
            groupLabel = "长期目标",
            timeType = EventTimeType.TIMED,
            dateLabel = "2026年5月24日 20:30",
            relativeLabel = "已经 86 天",
            note = "每天留一点时间给长期目标。",
            icon = "●",
            colorRole = EventColorRole.PAST,
            cardPaletteKey = EventCardPaletteKey.ORANGE_WHITE,
            targetInstant = Instant.parse("2026-05-24T12:30:00Z"),
            zoneId = "Asia/Shanghai",
        ),
        TimeEvent(
            id = "research-exam",
            title = "研究生考试",
            subtitle = "目标",
            groupLabel = "学习与成长",
            timeType = EventTimeType.ALL_DAY,
            dateLabel = "2026年12月26日 · 全天",
            relativeLabel = "还有 130 天",
            icon = "✦",
            colorRole = EventColorRole.FUTURE,
            cardPaletteKey = EventCardPaletteKey.BLUE_WHITE,
            localDate = LocalDate.of(2026, 12, 26),
            cardTemplateKey = EventCardTemplateKey.TRAVEL_SCRAPBOOK,
            travelCardConfig = TravelCardConfig(
                badgeLabel = "目标",
                title = "研究生考试",
                targetDate = java.time.LocalDate.of(2026, 12, 26),
                locationLabel = "备考",
                size = TravelCardSize.SMALL,
            ),
        ),
        TimeEvent(
            id = "next-journey",
            title = "下一次旅行",
            subtitle = "旅行",
            groupLabel = "旅行与生活",
            timeType = EventTimeType.ALL_DAY,
            dateLabel = "2026.10.12 星期一",
            relativeLabel = "还有 54 天",
            icon = "✈",
            colorRole = EventColorRole.FUTURE,
            cardPaletteKey = EventCardPaletteKey.BLUE_WHITE,
            localDate = LocalDate.of(2026, 10, 12),
            cardTemplateKey = EventCardTemplateKey.TRAVEL_SUNSET_GLASS,
            travelCardConfig = TravelCardConfig(
                badgeLabel = "旅行",
                title = "下一次旅行",
                targetDate = java.time.LocalDate.of(2026, 10, 12),
                locationLabel = "京都",
                size = TravelCardSize.WIDE,
            ),
        ),
    )

    /** 首页模板画廊：每个可见模板都展示 Small 与 Wide 两种尺寸。 */
    val templateGallery: List<TimeEvent> = listOf(
        EventCardTemplateKey.CLASSIC to "经典蓝白 / 橙白",
        EventCardTemplateKey.TRAVEL_MINIMAL_EDITORIAL to "奶油极简编辑式",
        EventCardTemplateKey.TRAVEL_SUNSET_GLASS to "黄昏玻璃风景",
        EventCardTemplateKey.TRAVEL_SCRAPBOOK to "旅行手账贴纸风",
    ).flatMap { (template, _) ->
        listOf(TravelCardSize.SMALL, TravelCardSize.WIDE).map { size ->
            TimeEvent(
                id = "template-preview-${template.name.lowercase()}-${size.name.lowercase()}",
                title = "东京旅行",
                subtitle = "纪念日",
                groupLabel = "",
                timeType = EventTimeType.ALL_DAY,
                dateLabel = "2026.09.16 星期三",
                relativeLabel = "还有 28 天",
                icon = "✈",
                colorRole = EventColorRole.FUTURE,
                cardPaletteKey = EventCardPaletteKey.BLUE_WHITE,
                cardTemplateKey = template,
                travelCardConfig = TravelCardConfig(
                    badgeLabel = "纪念日",
                    title = "东京旅行",
                    // 预览卡不重复展示模板名称；真实事件仍可由用户填写分组。
                    groupLabel = "",
                targetDate = java.time.LocalDate.of(2026, 9, 16),
                    badgeIcon = TravelCardIcon.CALENDAR,
                    dateIcon = TravelCardIcon.CLOCK,
                    backgroundPreset = if (template == EventCardTemplateKey.TRAVEL_SUNSET_GLASS) {
                        TravelBackgroundPreset.TOKYO_SUNSET
                    } else {
                        TravelBackgroundPreset.SCRAPBOOK_CREAM
                    },
                    size = size,
                ),
            )
        }
    }

    val home: List<TimeEvent>
        get() = all + templateGallery
}
