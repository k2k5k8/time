package com.cch.momentmark.ui.home

import com.cch.momentmark.domain.model.EventTimeType
import com.cch.momentmark.domain.model.TimeEvent
import com.cch.momentmark.domain.model.TravelCardConfig
import com.cch.momentmark.domain.model.TravelCardSize
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CardBoardLayoutTest {
    @Test
    fun reorderedCardLayouts_movesTheDraggedCardAndCompactsOrders() {
        val result = reorderedCardLayouts(
            current = listOf(
                HomeCardLayout("a", 0, 1),
                HomeCardLayout("b", 1, 2),
                HomeCardLayout("c", 2, 1),
            ),
            draggedId = "a",
            targetId = "c",
        )

        assertEquals(listOf("b", "c", "a"), result.map { it.cardId })
        assertEquals(listOf(0, 1, 2), result.map { it.order })
        assertEquals(2, result.single { it.cardId == "b" }.gridWidth)
    }

    @Test
    fun reorderedCardLayouts_ignoresUnknownCards() {
        val original = listOf(HomeCardLayout("a", 0, 1))

        assertEquals(original, reorderedCardLayouts(original, draggedId = "missing", targetId = "a"))
    }

    private fun eventWithSize(size: TravelCardSize?): TimeEvent = TimeEvent(
        id = "card",
        title = "东京之旅",
        subtitle = "TRAVEL",
        timeType = EventTimeType.ALL_DAY,
        dateLabel = "2026.10.01",
        relativeLabel = "还有 41 天",
        icon = "☆",
        colorRole = com.cch.momentmark.domain.model.EventColorRole.FUTURE,
        cardPaletteKey = com.cch.momentmark.domain.model.EventCardPaletteKey.BLUE_WHITE,
        travelCardConfig = size?.let {
            TravelCardConfig(size = it)
        },
    )

    @Test
    fun cardGridWidth_followsTheRendererSizeNotAnyStoredLayout() {
        assertEquals(2, cardGridWidth(eventWithSize(TravelCardSize.WIDE)))
        assertEquals(1, cardGridWidth(eventWithSize(TravelCardSize.SMALL)))
        assertEquals(1, cardGridWidth(eventWithSize(null)))
    }

    @Test
    fun defaultCardLayout_usesTheRendererSizeForItsSlotWidth() {
        assertEquals(2, defaultCardLayout(eventWithSize(TravelCardSize.WIDE), order = 3).gridWidth)
        assertEquals(1, defaultCardLayout(eventWithSize(TravelCardSize.SMALL), order = 3).gridWidth)
    }
}
