package com.cch.momentmark.domain.time

import com.cch.momentmark.domain.model.EventTimeType
import com.cch.momentmark.domain.model.RepeatRule
import com.cch.momentmark.domain.model.RepeatType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventTimeCalculatorTest {
    private val utc = ZoneOffset.UTC

    private fun clock(instant: String, zone: ZoneId = utc): Clock =
        Clock.fixed(Instant.parse(instant), zone)

    @Test
    fun allDayTodayIsAnExplicitTodayState() {
        val result = EventTimeCalculator.allDay(
            targetDate = LocalDate.of(2026, 8, 19),
            clock = clock("2026-08-19T08:00:00Z"),
        )

        assertEquals(0L, result.amount)
        assertEquals(EventTimeStatus.TODAY, result.status)
        assertEquals("今天", result.statusLabel)
    }

    @Test
    fun allDayYesterdayAndTomorrowUseCalendarDays() {
        val fixedClock = clock("2026-08-19T23:59:00Z")

        val yesterday = EventTimeCalculator.allDay(LocalDate.of(2026, 8, 18), fixedClock)
        val tomorrow = EventTimeCalculator.allDay(LocalDate.of(2026, 8, 20), fixedClock)

        assertEquals(1L, yesterday.amount)
        assertEquals(EventTimeStatus.PAST, yesterday.status)
        assertEquals(1L, tomorrow.amount)
        assertEquals(EventTimeStatus.FUTURE, tomorrow.status)
    }

    @Test
    fun allDayLeapDayAndMonthEndRemainCalendarBased() {
        val leapDay = EventTimeCalculator.allDay(
            LocalDate.of(2028, 2, 29),
            clock("2028-02-28T12:00:00Z"),
        )
        val monthEnd = EventTimeCalculator.allDay(
            LocalDate.of(2028, 2, 1),
            clock("2028-01-31T12:00:00Z"),
        )

        assertEquals(1L, leapDay.amount)
        assertEquals(1L, monthEnd.amount)
    }

    @Test
    fun calculateDispatchesAllDayAndTimedInputs() {
        val allDay = EventTimeCalculator.calculate(
            timeType = EventTimeType.ALL_DAY,
            localDate = LocalDate.of(2026, 8, 20),
            clock = clock("2026-08-19T00:00:00Z"),
        )
        val timed = EventTimeCalculator.calculate(
            timeType = EventTimeType.TIMED,
            targetInstant = Instant.parse("2026-08-20T00:00:00Z"),
            clock = clock("2026-08-19T00:00:00Z"),
        )

        assertEquals(EventTimeStatus.FUTURE, allDay.status)
        assertEquals(1L, timed.amount)
        assertEquals(EventTimeStatus.FUTURE, timed.status)
    }

    @Test
    fun yearlyRepeatMovesPastDateToNextOccurrence() {
        val rule = RepeatRule(type = RepeatType.YEARLY)
        val result = EventTimeCalculator.calculate(
            timeType = EventTimeType.ALL_DAY,
            localDate = LocalDate.of(2020, 5, 27),
            repeatRule = rule,
            clock = clock("2026-08-20T00:00:00Z"),
        )

        assertEquals(LocalDate.of(2027, 5, 27), EventTimeCalculator.resolveNextDate(
            targetDate = LocalDate.of(2020, 5, 27),
            repeatRule = rule,
            clock = clock("2026-08-20T00:00:00Z"),
        ))
        assertEquals(280L, result.amount)
        assertEquals(EventTimeStatus.FUTURE, result.status)
    }

    @Test
    fun timedExactNowIsToday() {
        val now = Instant.parse("2026-08-19T12:30:00Z")
        val result = EventTimeCalculator.timed(now, clock("2026-08-19T12:30:00Z"))

        assertEquals(0L, result.amount)
        assertEquals(EventTimeStatus.TODAY, result.status)
        assertEquals(java.time.Duration.ZERO, result.duration)
    }

    @Test
    fun timedFutureAndPastUseRealDuration() {
        val now = Instant.parse("2026-08-19T00:00:00Z")
        val future = EventTimeCalculator.timed(
            now.plusSeconds(36 * 60 * 60),
            clock(now.toString()),
        )
        val past = EventTimeCalculator.timed(
            now.minusSeconds(90 * 60 * 60),
            clock(now.toString()),
        )

        assertEquals(1L, future.amount)
        assertEquals(EventTimeStatus.FUTURE, future.status)
        assertEquals(36L * 60 * 60, future.duration.seconds)
        assertEquals(3L, past.amount)
        assertEquals(EventTimeStatus.PAST, past.status)
        assertEquals(90L * 60 * 60, past.duration.seconds)
    }

    @Test
    fun timedSpringForwardDayIs23Hours() {
        val zone = ZoneId.of("America/New_York")
        val start = ZonedDateTime.of(2026, 3, 8, 0, 0, 0, 0, zone).toInstant()
        val end = ZonedDateTime.of(2026, 3, 9, 0, 0, 0, 0, zone).toInstant()

        val result = EventTimeCalculator.timed(end, clock(start.toString(), zone), zone)

        assertEquals(23L * 60 * 60, result.duration.seconds)
    }

    @Test
    fun timedFallBackDayIs25Hours() {
        val zone = ZoneId.of("America/New_York")
        val start = ZonedDateTime.of(2026, 11, 1, 0, 0, 0, 0, zone).toInstant()
        val end = ZonedDateTime.of(2026, 11, 2, 0, 0, 0, 0, zone).toInstant()

        val result = EventTimeCalculator.timed(end, clock(start.toString(), zone), zone)

        assertEquals(25L * 60 * 60, result.duration.seconds)
    }

    @Test
    fun timedDisplayUsesInjectedZoneWithoutChangingInstant() {
        val instant = Instant.parse("2026-08-19T00:30:00Z")

        assertEquals(
            "2026.08.19 08:30",
            EventTimeCalculator.formatInstant(instant, ZoneId.of("Asia/Shanghai")),
        )
        assertEquals(
            "2026.08.18 17:30",
            EventTimeCalculator.formatInstant(instant, ZoneId.of("America/Los_Angeles")),
        )
    }

    @Test
    fun defaultZoneIsNotUsedWhenClockAndZoneAreInjected() {
        val zone = ZoneId.of("Asia/Tokyo")
        val result = EventTimeCalculator.allDay(
            targetDate = LocalDate.of(2026, 8, 21),
            clock = clock("2026-08-19T15:30:00Z", zone),
            zoneId = zone,
        )

        assertTrue(result.isFuture)
        assertEquals(zone, result.zoneId)
    }

    @Test
    fun todayUsesInjectedClockAndZone() {
        val zone = ZoneId.of("Asia/Shanghai")

        assertEquals(
            LocalDate.of(2026, 8, 20),
            EventTimeCalculator.today(clock("2026-08-19T16:30:00Z"), zone),
        )
    }
}
