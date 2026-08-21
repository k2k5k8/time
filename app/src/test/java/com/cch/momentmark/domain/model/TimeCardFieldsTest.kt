package com.cch.momentmark.domain.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeCardFieldsTest {
    @Test
    fun cardFieldsExposeTheSharedFiveContentSlots() {
        val event = TimeEvent(
            id = "exam",
            title = "研究生考试",
            subtitle = "目标",
            groupLabel = "学习与成长",
            timeType = EventTimeType.ALL_DAY,
            dateLabel = "2026.12.26 星期六",
            relativeLabel = "还有 128 天",
            icon = "✦",
            colorRole = EventColorRole.FUTURE,
            cardPaletteKey = EventCardPaletteKey.BLUE_WHITE,
            localDate = LocalDate.of(2026, 12, 26),
        )

        assertEquals(
            TimeCardFields(
                title = "研究生考试",
                subtitle = "目标",
                countdownLabel = "还有 128 天",
                startTimeLabel = "2026.12.26 星期六",
                groupLabel = "学习与成长",
            ),
            event.cardFields(),
        )
    }

    @Test
    fun travelConfigOnlyFillsBlankSharedTextFields() {
        val event = TimeEvent(
            id = "trip",
            title = "",
            subtitle = "",
            groupLabel = "",
            timeType = EventTimeType.ALL_DAY,
            dateLabel = "2026.09.16 星期三",
            relativeLabel = "还有 28 天",
            icon = "✈",
            colorRole = EventColorRole.FUTURE,
            cardPaletteKey = EventCardPaletteKey.BLUE_WHITE,
            travelCardConfig = TravelCardConfig(
                title = "东京旅行",
                badgeLabel = "纪念日",
                groupLabel = "旅行与生活",
            ),
        )

        val fields = event.cardFields()

        assertEquals("东京旅行", fields.title)
        assertEquals("纪念日", fields.subtitle)
        assertEquals("旅行与生活", fields.groupLabel)
    }
}
