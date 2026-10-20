package com.cch.momentmark.ui.components

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Frozen reference, last valid candidate and raw input survive form restoration together. */
data class DayOffsetSelection(
    val reference: LocalDate,
    val days: Long,
    val maximum: Long,
    val input: String = days.toString(),
) : java.io.Serializable {
    init { require(days in 0..maximum); require(maximum <= ChronoUnit.DAYS.between(reference, LocalDate.MAX)) }
    val date: LocalDate get() = reference.plusDays(days)
    val inputDays: Long? get() = input.takeIf { it.isNotEmpty() && it.all { c -> c in '0'..'9' } }
        ?.toLongOrNull()?.takeIf { it in 0..maximum }
    val confirmedDate: LocalDate? get() = inputDays?.let(reference::plusDays)
    fun withInput(value: String): DayOffsetSelection {
        val next = copy(input = value)
        return next.copy(days = next.inputDays ?: days)
    }
    fun withDays(value: Long) = if (value in 0..maximum) copy(days = value, input = value.toString()) else this
    companion object {
        fun initial(reference: LocalDate, current: LocalDate): DayOffsetSelection {
            val offset = ChronoUnit.DAYS.between(reference, current).coerceAtLeast(0)
            val max = ChronoUnit.DAYS.between(reference, LocalDate.MAX)
            return DayOffsetSelection(reference, if (offset > 2) offset else minOf(3L, max), max)
        }
    }
}

/** Ordinary edits retain absolute dates, including dates before today. */
data class TaskDateInputSelection(
    val reference: LocalDate,
    val year: String,
    val month: String,
    val day: String,
) : java.io.Serializable {
    val date: LocalDate? get() {
        fun number(text: String) = text.takeIf { it.isNotEmpty() && it.all { c -> c in '0'..'9' } }?.toIntOrNull()
        return runCatching { LocalDate.of(number(year) ?: return null, number(month) ?: return null, number(day) ?: return null) }.getOrNull()
    }
    fun withDate(value: LocalDate) = copy(year = value.year.toString(), month = value.monthValue.toString(), day = value.dayOfMonth.toString())
    fun preset(days: Long) = runCatching { withDate(reference.plusDays(days)) }.getOrNull()
    companion object {
        fun initial(reference: LocalDate, date: LocalDate) = TaskDateInputSelection(reference, date.year.toString(), date.monthValue.toString(), date.dayOfMonth.toString())
    }
}
