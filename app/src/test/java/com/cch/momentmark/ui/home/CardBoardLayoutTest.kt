package com.cch.momentmark.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

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
}
