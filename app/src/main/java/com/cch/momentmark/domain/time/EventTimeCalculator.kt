package com.cch.momentmark.domain.time

import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

enum class EventTimeStatus { TODAY, FUTURE, PAST }

data class CountdownResult(
    val amount: Long,
    val status: EventTimeStatus,
    val duration: Duration,
    val zoneId: ZoneId,
) {
    val isFuture get() = status == EventTimeStatus.FUTURE
    val isPast get() = status == EventTimeStatus.PAST
}

/** P0's single LocalDate time kernel for Moment display state and day counts. */
object EventTimeCalculator {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

    fun today(clock: Clock, zoneId: ZoneId = clock.zone): LocalDate =
        LocalDate.now(clock.withZone(zoneId))

    fun allDay(targetDate: LocalDate, clock: Clock, zoneId: ZoneId = clock.zone): CountdownResult =
        countdown(targetDate, today(clock, zoneId), zoneId)

    fun countdown(
        targetDate: LocalDate,
        today: LocalDate,
        zoneId: ZoneId = ZoneId.of("UTC"),
    ): CountdownResult {
        val signedDays = ChronoUnit.DAYS.between(today, targetDate)
        val status = when {
            signedDays > 0L -> EventTimeStatus.FUTURE
            signedDays < 0L -> EventTimeStatus.PAST
            else -> EventTimeStatus.TODAY
        }
        return CountdownResult(abs(signedDays), status, Duration.ofDays(abs(signedDays)), zoneId)
    }

    fun dateLabel(date: LocalDate): String = "${date.format(dateFormatter)} ${weekdayLabel(date)}"

    private fun weekdayLabel(date: LocalDate): String = when (date.dayOfWeek.value) {
        1 -> "星期一"
        2 -> "星期二"
        3 -> "星期三"
        4 -> "星期四"
        5 -> "星期五"
        6 -> "星期六"
        else -> "星期日"
    }
}
