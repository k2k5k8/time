package com.cch.momentmark.ui.components

import java.time.LocalDate
import java.time.YearMonth

/** The visible date may be clamped, while the user's day-of-month intent survives. */
data class DateWheelSelection(val date: LocalDate, val intendedDay: Int = date.dayOfMonth) {
    init { require(intendedDay in 1..31) }

    fun withYear(year: Int) = inMonth(YearMonth.of(year, date.monthValue))
    fun withMonth(month: Int) = inMonth(YearMonth.of(date.year, month))
    fun withDay(day: Int) = DateWheelSelection(date.withDayOfMonth(day), day)
    fun inMonth(month: YearMonth) = copy(date = month.atDay(intendedDay.coerceAtMost(month.lengthOfMonth())))
}

fun dateWheelYearRange(today: LocalDate, selectedDate: LocalDate, recordYears: Iterable<Int> = emptyList()): IntRange {
    var minimum = minOf(1900, today.year, selectedDate.year)
    var maximum = maxOf(2100, today.year, selectedDate.year)
    recordYears.forEach { minimum = minOf(minimum, it); maximum = maxOf(maximum, it) }
    return minimum..maximum
}
