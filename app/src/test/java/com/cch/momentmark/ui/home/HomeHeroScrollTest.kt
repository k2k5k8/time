package com.cch.momentmark.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeHeroScrollTest {

    @Test
    fun `progress follows scroll continuously before the first row leaves`() {
        assertEquals(
            0.5f,
            homeHeroCollapseProgress(
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 150,
                collapseDistancePx = 300f,
            ),
            0.0001f,
        )
    }

    @Test
    fun `progress is clamped at both ends`() {
        assertEquals(
            0f,
            homeHeroCollapseProgress(0, -20, 300f),
            0.0001f,
        )
        assertEquals(
            1f,
            homeHeroCollapseProgress(0, 500, 300f),
            0.0001f,
        )
    }

    @Test
    fun `once the first grid row is gone the hero is fully collapsed`() {
        assertEquals(
            1f,
            homeHeroCollapseProgress(2, 0, 300f),
            0.0001f,
        )
    }

    @Test
    fun `scene pool keeps images and quotes varied`() {
        assertTrue(HomeHeroScenes.size >= 5)
        assertTrue(HomeHeroScenes.map { it.imageRes }.toSet().size >= 5)
        assertEquals(
            HomeHeroScenes.size,
            HomeHeroScenes.map { it.quote }.toSet().size,
        )
    }
}
