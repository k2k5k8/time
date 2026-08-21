package com.cch.momentmark.ui

import com.cch.momentmark.domain.model.EventCardPaletteKey
import com.cch.momentmark.domain.model.EventColorRole
import com.cch.momentmark.domain.model.EventTimeType
import com.cch.momentmark.domain.model.TimeEvent
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryDrawerFilterTest {
    private val events = listOf(
        event("future-trip", "旅行与生活", EventColorRole.FUTURE, pinned = true),
        event("past-trip", "旅行与生活", EventColorRole.PAST),
        event("future-study", "学习与成长", EventColorRole.FUTURE),
    )

    @Test
    fun statusAndGroupFiltersCompose() {
        assertEquals(
            listOf("future-trip"),
            filterEventsByScope(events, EventFilter.FUTURE, "旅行与生活").map { it.id },
        )
        assertEquals(
            listOf("past-trip"),
            filterEventsByScope(events, EventFilter.PAST, "旅行与生活").map { it.id },
        )
    }

    @Test
    fun selectingTheSameGroupAgainRemovesTheGroupConstraint() {
        assertEquals(
            listOf("future-trip", "past-trip"),
            filterEventsByScope(events, EventFilter.ALL, "旅行与生活").map { it.id },
        )
        assertEquals(3, filterEventsByScope(events, EventFilter.ALL, null).size)
    }

    @Test
    fun groupCountsAreDerivedFromEventsAndIncludeEmptySavedGroups() {
        assertEquals(
            listOf("旅行与生活" to 2, "工作" to 0, "学习与成长" to 1),
            drawerGroups(events, listOf("旅行与生活", "工作")).map { it.name to it.count },
        )
    }

    private fun event(id: String, group: String, role: EventColorRole, pinned: Boolean = false) =
        TimeEvent(
            id = id,
            title = id,
            groupLabel = group,
            timeType = EventTimeType.ALL_DAY,
            dateLabel = "2026.08.20",
            relativeLabel = "还有 1 天",
            icon = "●",
            colorRole = role,
            cardPaletteKey = EventCardPaletteKey.BLUE_WHITE,
            isPinned = pinned,
            localDate = LocalDate.of(2026, 8, 21),
        )
}
