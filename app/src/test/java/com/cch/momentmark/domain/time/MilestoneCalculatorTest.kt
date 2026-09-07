package com.cch.momentmark.domain.time

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MilestoneCalculatorTest {
    @Test
    fun `fixed nodes expose achieved next and expedition with progress`() {
        val anchor = LocalDate.of(2023, 5, 20)
        val today = LocalDate.of(2026, 8, 28)
        val milestones = MilestoneCalculator.calculate(anchor, today)
        assertEquals(MilestoneStatus.ACHIEVED, milestones.first { it.label == "100 天" }.status)
        val next = milestones.first { it.status == MilestoneStatus.NEXT }
        assertEquals("1200 天", next.label)
        assertEquals(4L, next.daysRemaining)
        assertTrue((next.progressFraction ?: 0f) > 0.9f)
        assertTrue(milestones.any { it.label == "2000 天" && it.status == MilestoneStatus.EXPEDITION })
    }

    @Test
    fun `february 29 anniversary is omitted in non leap years`() {
        val anchor = LocalDate.of(2024, 2, 29)
        val milestones = MilestoneCalculator.calculate(anchor, LocalDate.of(2025, 3, 1))
        assertFalse(milestones.any { it.label == "1周年" })
    }

    @Test
    fun `annual milestone uses anchor month and day`() {
        val anchor = LocalDate.of(2023, 5, 20)
        val oneYear = MilestoneCalculator.calculate(anchor, LocalDate.of(2024, 5, 20))
            .first { it.label == "1周年" }
        assertEquals(LocalDate.of(2024, 5, 20), oneYear.targetDate)
        assertEquals(MilestoneStatus.ACHIEVED, oneYear.status)
    }
}
