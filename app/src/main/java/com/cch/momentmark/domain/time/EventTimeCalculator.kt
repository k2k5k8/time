package com.cch.momentmark.domain.time

import java.time.LocalDate
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import com.cch.momentmark.domain.model.EventTimeType
import com.cch.momentmark.domain.model.RepeatCustomUnit
import com.cch.momentmark.domain.model.RepeatRule
import com.cch.momentmark.domain.model.RepeatType
import kotlin.math.abs

enum class EventTimeStatus {
    TODAY,
    FUTURE,
    PAST,
}

data class CountdownResult(
    val amount: Long,
    val status: EventTimeStatus,
    val duration: Duration,
    val zoneId: ZoneId,
) {
    val isFuture: Boolean
        get() = status == EventTimeStatus.FUTURE

    val isPast: Boolean
        get() = status == EventTimeStatus.PAST

    val statusLabel: String
        get() = when (status) {
            EventTimeStatus.TODAY -> "今天"
            EventTimeStatus.FUTURE -> "还有"
            EventTimeStatus.PAST -> "已经"
        }
}

object EventTimeCalculator {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")

    fun today(clock: Clock, zoneId: ZoneId = clock.zone): LocalDate =
        LocalDate.now(clock.withZone(zoneId))

    fun allDay(
        targetDate: LocalDate,
        clock: Clock,
        zoneId: ZoneId = clock.zone,
    ): CountdownResult {
        val today = LocalDate.now(clock.withZone(zoneId))
        val signedDays = ChronoUnit.DAYS.between(today, targetDate)
        return CountdownResult(
            amount = abs(signedDays),
            status = statusFor(signedDays),
            duration = Duration.ofDays(abs(signedDays)),
            zoneId = zoneId,
        )
    }

    fun timed(
        targetInstant: Instant,
        clock: Clock,
        zoneId: ZoneId = clock.zone,
    ): CountdownResult {
        val duration = Duration.between(clock.instant(), targetInstant)
        val status = when {
            duration.isZero -> EventTimeStatus.TODAY
            duration.isNegative -> EventTimeStatus.PAST
            else -> EventTimeStatus.FUTURE
        }
        return CountdownResult(
            amount = duration.abs().toDays(),
            status = status,
            duration = duration.abs(),
            zoneId = zoneId,
        )
    }

    fun calculate(
        timeType: EventTimeType,
        localDate: LocalDate? = null,
        targetInstant: Instant? = null,
        repeatRule: RepeatRule? = null,
        clock: Clock,
        zoneId: ZoneId = clock.zone,
    ): CountdownResult = when (timeType) {
        com.cch.momentmark.domain.model.EventTimeType.ALL_DAY -> {
            requireNotNull(localDate) { "ALL_DAY events require localDate" }
            allDay(resolveNextDate(localDate, repeatRule, clock, zoneId), clock, zoneId)
        }

        com.cch.momentmark.domain.model.EventTimeType.TIMED -> {
            requireNotNull(targetInstant) { "TIMED events require targetInstant" }
            timed(resolveNextInstant(targetInstant, repeatRule, clock, zoneId), clock, zoneId)
        }
    }

    fun resolveNextDate(
        targetDate: LocalDate,
        repeatRule: RepeatRule?,
        clock: Clock,
        zoneId: ZoneId = clock.zone,
    ): LocalDate {
        if (repeatRule == null) return targetDate
        val today = today(clock, zoneId)
        var candidate = targetDate
        while (candidate.isBefore(today)) {
            candidate = advance(candidate, repeatRule)
        }
        return candidate
    }

    fun resolveNextInstant(
        targetInstant: Instant,
        repeatRule: RepeatRule?,
        clock: Clock,
        zoneId: ZoneId = clock.zone,
    ): Instant {
        if (repeatRule == null) return targetInstant
        val now = clock.instant()
        var candidate = targetInstant
        while (candidate.isBefore(now)) {
            candidate = advance(candidate.atZone(zoneId).toLocalDateTime(), repeatRule)
                .atZone(zoneId)
                .toInstant()
        }
        return candidate
    }

    private fun advance(date: LocalDate, rule: RepeatRule): LocalDate = when (rule.type) {
        RepeatType.YEARLY -> safeDate(date.year + 1, date.monthValue, date.dayOfMonth)
        RepeatType.MONTHLY -> date.plusMonths(1)
        RepeatType.WEEKLY -> date.plusWeeks(1)
        RepeatType.CUSTOM -> if (rule.customUnit == RepeatCustomUnit.WEEK) {
            date.plusWeeks(rule.interval.coerceAtLeast(1).toLong())
        } else {
            date.plusDays(rule.interval.coerceAtLeast(1).toLong())
        }
    }

    private fun advance(dateTime: LocalDateTime, rule: RepeatRule): LocalDateTime = when (rule.type) {
        RepeatType.YEARLY -> safeDate(dateTime.year + 1, dateTime.monthValue, dateTime.dayOfMonth)
            .atTime(dateTime.toLocalTime())
        RepeatType.MONTHLY -> dateTime.plusMonths(1)
        RepeatType.WEEKLY -> dateTime.plusWeeks(1)
        RepeatType.CUSTOM -> if (rule.customUnit == RepeatCustomUnit.WEEK) {
            dateTime.plusWeeks(rule.interval.coerceAtLeast(1).toLong())
        } else {
            dateTime.plusDays(rule.interval.coerceAtLeast(1).toLong())
        }
    }

    private fun safeDate(year: Int, month: Int, day: Int): LocalDate = runCatching {
        LocalDate.of(year, month, day)
    }.getOrElse {
        LocalDate.of(year, month, 1).withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth())
    }

    /** Compatibility helper for callers that already own a deterministic calendar date. */
    fun countdown(targetDate: LocalDate, today: LocalDate): CountdownResult {
        val zoneId = ZoneId.of("UTC")
        val signedDays = ChronoUnit.DAYS.between(today, targetDate)
        return CountdownResult(
            amount = abs(signedDays),
            status = statusFor(signedDays),
            duration = Duration.ofDays(abs(signedDays)),
            zoneId = zoneId,
        )
    }

    fun formatInstant(targetInstant: Instant, zoneId: ZoneId): String =
        targetInstant.atZone(zoneId).format(dateTimeFormatter)

    private fun statusFor(signedDistance: Long): EventTimeStatus = when {
        signedDistance > 0L -> EventTimeStatus.FUTURE
        signedDistance < 0L -> EventTimeStatus.PAST
        else -> EventTimeStatus.TODAY
    }

    fun dateLabel(date: LocalDate): String {
        val weekday = when (date.dayOfWeek.value) {
            1 -> "星期一"
            2 -> "星期二"
            3 -> "星期三"
            4 -> "星期四"
            5 -> "星期五"
            6 -> "星期六"
            else -> "星期日"
        }
        return "${date.format(dateFormatter)} $weekday"
    }
}
