package com.cch.momentmark.domain.time

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class EventTimeCalculatorTest {
    private val clock = Clock.fixed(Instant.parse("2026-02-28T12:00:00Z"), ZoneOffset.UTC)

    @Test fun `目标日前显示未来倒数`() {
        val result = EventTimeCalculator.allDay(LocalDate.of(2026, 3, 1), clock)
        assertEquals(1, result.amount)
        assertEquals(EventTimeStatus.FUTURE, result.status)
    }

    @Test fun `目标日显示今天`() {
        val result = EventTimeCalculator.allDay(LocalDate.of(2026, 2, 28), clock)
        assertEquals(0, result.amount)
        assertEquals(EventTimeStatus.TODAY, result.status)
    }

    @Test fun `目标日次日显示过去成就`() {
        val result = EventTimeCalculator.allDay(LocalDate.of(2026, 2, 27), clock)
        assertEquals(1, result.amount)
        assertEquals(EventTimeStatus.PAST, result.status)
    }

    @Test fun `闰日跨月按自然日计算`() {
        val result = EventTimeCalculator.countdown(LocalDate.of(2024, 3, 1), LocalDate.of(2024, 2, 29))
        assertEquals(1, result.amount)
        assertEquals(EventTimeStatus.FUTURE, result.status)
    }
}
