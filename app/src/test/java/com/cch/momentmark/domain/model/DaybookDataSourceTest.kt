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
        val source = PrototypeDaybookDataSource(listOf(userEvent), today)

        val dateEvents = source.eventsForDate(today)

        assertTrue(dateEvents.any { it.source == DaybookEventSource.USER && it.title == "晚饭后散步" })
        assertTrue(dateEvents.any { it.id == "prototype-daybook-note" })
        assertEquals(6, source.eventsForMonth(java.time.YearMonth.of(2026, 8)).size)
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
        val events = PrototypeDaybookDataSource(listOf(userEvent), today).eventsForDate(today)

        assertTrue(events.first { it.id == "user-anniversary" }.showInMilestone)
        assertEquals(false, events.first { it.id == "prototype-daybook-note" }.showInMilestone)
    }

    @Test
    fun timedUserEventMapsToItsLocalDate() {
        val userEvent = TimeEvent(
            id = "timed",
            title = "晚间提醒",
            timeType = EventTimeType.TIMED,
            dateLabel = "",
            relativeLabel = "",
            icon = "●",
            colorRole = EventColorRole.FUTURE,
            cardPaletteKey = EventCardPaletteKey.BLUE_WHITE,
            targetInstant = java.time.Instant.parse("2026-08-21T04:30:00Z"),
            zoneId = "Asia/Shanghai",
        )

        val event = PrototypeDaybookDataSource(listOf(userEvent), today)
            .eventsForDate(today)
            .first { it.id == "user-timed" }

        assertEquals(today, event.date)
        assertEquals(false, event.isAllDay)
        assertEquals(DaybookEventType.PERSONAL, event.eventType)
    }
}
