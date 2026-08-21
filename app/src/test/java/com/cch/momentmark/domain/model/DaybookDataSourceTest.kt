package com.cch.momentmark.domain.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DaybookDataSourceTest {
    private val today = LocalDate.of(2026, 8, 21)

    @Test
    fun monthAndDateQueriesKeepSystemAndUserSourcesSeparate() {
        val userEvent = TimeEvent(
            id = "walk",
            title = "晚饭后散步",
            timeType = EventTimeType.ALL_DAY,
            dateLabel = "",
            relativeLabel = "",
            icon = "●",
            colorRole = EventColorRole.FUTURE,
            cardPaletteKey = EventCardPaletteKey.BLUE_WHITE,
            localDate = today,
            isPinned = true,
        )
        val source = MockDaybookDataSource(listOf(userEvent), today)

        val dateEvents = source.eventsForDate(today)

        assertTrue(dateEvents.any { it.source == DaybookEventSource.USER && it.title == "晚饭后散步" })
        assertTrue(dateEvents.any { it.id == "prototype-daybook-note" })
        assertEquals(3, source.eventsForMonth(java.time.YearMonth.of(2026, 8)).size)
    }

    @Test
    fun pinnedUserEventIsEligibleForMilestoneSyncButPrototypeIsNot() {
        val userEvent = TimeEvent(
            id = "anniversary",
            title = "相识纪念日",
            timeType = EventTimeType.ALL_DAY,
            dateLabel = "",
            relativeLabel = "",
            icon = "✦",
            colorRole = EventColorRole.FUTURE,
            cardPaletteKey = EventCardPaletteKey.BLUE_WHITE,
            localDate = today,
            isPinned = true,
        )
        val events = MockDaybookDataSource(listOf(userEvent), today).eventsForDate(today)

        assertTrue(events.first { it.id == "user-anniversary" }.showInMilestone)
        assertEquals(false, events.first { it.id == "prototype-daybook-note" }.showInMilestone)
    }
}
