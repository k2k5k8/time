package com.cch.momentmark.domain.time

import java.time.LocalDate
import java.time.Year

/** Read-only milestone projection for an achievement Moment.
 *
 * Milestones are deliberately configuration owned by the product, not by the
 * user.  The calculator receives a natural day so that UI and tests never call
 * LocalDate.now() directly and no derived state is persisted.
 */
enum class MilestoneStatus { ACHIEVED, NEXT, EXPEDITION }

data class Milestone(
    val label: String,
    val targetDate: LocalDate,
    val dayCount: Long?,
    val status: MilestoneStatus,
    val daysRemaining: Long = 0L,
    val progressFraction: Float? = null,
) {
    val achievedDate: LocalDate? get() = targetDate.takeIf { status == MilestoneStatus.ACHIEVED }
}

object MilestoneCalculator {
    // Product-fixed day nodes.  Nodes after 2,000 continue in fixed 1,000-day
    // steps; callers cannot inject or edit this list.
    private val fixedDayNodes = listOf(100L, 1_000L, 1_200L, 2_000L)

    fun calculate(anchorDate: LocalDate, today: LocalDate): List<Milestone> {
        // Future Moments have no achievement milestones yet; keeping this a
        // total function makes it safe for shared detail presenters.
        if (today.isBefore(anchorDate)) return emptyList()
        val candidates = buildList {
            fixedDayNodes.forEach { days ->
                add(MilestoneCandidate("$days 天", anchorDate.plusDays(days), days))
            }
            // Keep a useful, deterministic runway after the explicitly named
            // nodes without exposing a custom milestone setting.
            var days = 3_000L
            repeat(8) {
                add(MilestoneCandidate("$days 天", anchorDate.plusDays(days), days))
                days += 1_000L
            }
            // Annual milestones are date based.  A Feb-29 anchor intentionally
            // has no anniversary in a non-leap year (the product boundary).
            val maxYear = today.year + 9
            for (year in (anchorDate.year + 1)..maxYear) {
                val date = anniversaryDate(anchorDate, year) ?: continue
                val years = year - anchorDate.year
                add(MilestoneCandidate("${years}周年", date, years.toLong() * 365L))
            }
        }.distinctBy { it.targetDate to it.dayCount }
            .sortedWith(compareBy<MilestoneCandidate> { it.targetDate }.thenBy { it.dayCount ?: Long.MAX_VALUE })

        var nextSeen = false
        return candidates.map { candidate ->
            when {
                !candidate.targetDate.isAfter(today) -> Milestone(
                    label = candidate.label,
                    targetDate = candidate.targetDate,
                    dayCount = candidate.dayCount,
                    status = MilestoneStatus.ACHIEVED,
                )
                !nextSeen -> {
                    nextSeen = true
                    val total = java.time.temporal.ChronoUnit.DAYS.between(anchorDate, candidate.targetDate)
                    val elapsed = java.time.temporal.ChronoUnit.DAYS.between(anchorDate, today)
                    Milestone(
                        label = candidate.label,
                        targetDate = candidate.targetDate,
                        dayCount = candidate.dayCount,
                        status = MilestoneStatus.NEXT,
                        daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(today, candidate.targetDate),
                        progressFraction = (elapsed.toDouble() / total.coerceAtLeast(1L)).toFloat().coerceIn(0f, 1f),
                    )
                }
                else -> Milestone(
                    label = candidate.label,
                    targetDate = candidate.targetDate,
                    dayCount = candidate.dayCount,
                    status = MilestoneStatus.EXPEDITION,
                )
            }
        }
    }

    private fun anniversaryDate(anchor: LocalDate, year: Int): LocalDate? {
        if (anchor.monthValue == 2 && anchor.dayOfMonth == 29 && !Year.isLeap(year.toLong())) return null
        return anchor.withYear(year)
    }

    private data class MilestoneCandidate(val label: String, val targetDate: LocalDate, val dayCount: Long?)
}
