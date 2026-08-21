package com.cch.momentmark.domain.time

import com.cch.momentmark.domain.model.TimeEvent
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

data class InterestingCard(
    val title: String,
    val value: String,
    val detail: String,
)

private data class TimeSnapshot(
    val targetDate: LocalDate,
    val today: LocalDate,
    val signedDays: Long,
    val elapsedDays: Long,
    val remainingDays: Long,
) {
    val elapsedPeriod: Period
        get() = Period.between(targetDate, today)

    val remainingPeriod: Period
        get() = Period.between(today, targetDate)
}

private typealias InterestingRule = (TimeSnapshot) -> InterestingCard

/** Pure, extensible rules for the detail-page time-discovery cards. */
object TimeInterestingGenerator {
    /** Rules for an event that has already happened. Values describe accumulated facts. */
    private val pastRules: List<InterestingRule> = listOf(
        { s -> InterestingCard("已经过了多久", "已经过去 ${s.elapsedDays} 天", "从 ${s.targetDate} 到今天") },
        { s -> anniversaryCard(s) },
        { s -> milestoneDayCard(s) },
        { s -> InterestingCard("星期数量", "已经过去 ${s.elapsedDays / 7} 个星期", "距离下一个整百星期还有 ${daysUntilNextHundredWeek(s)} 天") },
        { s -> InterestingCard("吃饭次数", "理论已经吃了 ${s.elapsedDays * 3} 顿", "按每天三顿估算；距离吃满 40000 顿还需要 ${((40_000 - s.elapsedDays * 3).coerceAtLeast(0) / 3)} 天") },
        { s -> InterestingCard("睡觉次数", "理论已经睡了 ${s.elapsedDays} 次", "按一天睡一次估算") },
        { s -> InterestingCard("心跳次数", "这段时间心跳约 ${formatLargeNumber(heartbeats(s.elapsedDays))} 次", "按成年人平均 75 次 / 分钟趣味估算") },
        { s -> InterestingCard("秒数", "已经过去 ${s.elapsedDays * 86_400} 秒", "${s.elapsedDays * 24} 小时 · ${s.elapsedDays * 24 * 60} 分钟") },
        { s -> InterestingCard("月份数量", "已经经历 ${s.elapsedPeriod.toTotalMonths().coerceAtLeast(0)} 个月", "下一个整百月节点仍会继续追踪") },
        { s -> InterestingCard("季节变化", "经历约 ${(s.elapsedDays / 91.3).roundToLong()} 个春夏秋冬", "按一年四季的平均长度趣味估算") },
        { s -> anniversaryMilestoneCard(s) },
        { s -> specialDayCard(s) },
        { s -> weekdayCard(s) },
    )

    /** Rules for an event that has not happened. Values describe what remains. */
    private val futureRules: List<InterestingRule> = listOf(
        { s -> InterestingCard("距离目标还有多久", "还有 ${s.remainingDays} 天", "目标日：${s.targetDate}") },
        { s -> InterestingCard("还需要吃饭几顿", "目标日前理论还需要吃 ${s.remainingDays * 3} 顿", "按每天三顿估算，不把未来时间误写成已经吃过") },
        { s -> InterestingCard("还需要睡几次", "目标日前理论还需要睡 ${s.remainingDays} 次", "按一天睡一次估算") },
        { s -> InterestingCard("未来心跳估算", "到目标日前还会心跳约 ${formatLargeNumber(heartbeats(s.remainingDays))} 次", "按成年人平均 75 次 / 分钟趣味估算") },
        { s -> InterestingCard("秒数倒计时", "距离目标还有 ${s.remainingDays * 86_400} 秒", "${s.remainingDays * 24} 小时 · ${s.remainingDays * 24 * 60} 分钟") },
        { s -> futureMonthsCard(s) },
        { s -> InterestingCard("未来季节变化", "到目标日前约经历 ${(s.remainingDays / 91.3).roundToLong()} 个春夏秋冬", "按一年四季的平均长度趣味估算") },
        { s -> futureAnniversaryCard(s) },
        { s -> futureMilestoneDayCard(s) },
        { s -> futureSpecialDayCard(s) },
        { s -> weekdayCard(s) },
    )

    /** A small third pool avoids displaying zero-valued past facts on the target day itself. */
    private val todayRules: List<InterestingRule> = listOf(
        { s -> InterestingCard("今天就是目标日", "目标日已到达", "从今天开始，详情页会记录之后的周年和时间节点") },
        { s -> InterestingCard("周年纪念", "今天是第 0 周年起点", "下一周年日期：${safeAnniversary(s.targetDate, 1)}") },
        { s -> InterestingCard("万日节点", "10000 天纪念日：${s.targetDate.plusDays(10_000)}", "从今天开始继续累计时间") },
        { s -> InterestingCard("日期巧合", "目标日是星期${weekday(s.targetDate)}", "今天和目标日是同一天") },
    )

    fun generate(event: TimeEvent, today: LocalDate): List<InterestingCard> {
        val target = eventTargetDate(event) ?: return emptyList()
        return generate(target, today)
    }

    fun generate(targetDate: LocalDate, today: LocalDate): List<InterestingCard> {
        val signedDays = ChronoUnit.DAYS.between(today, targetDate)
        val snapshot = TimeSnapshot(
            targetDate = targetDate,
            today = today,
            signedDays = signedDays,
            elapsedDays = (-signedDays).coerceAtLeast(0),
            remainingDays = signedDays.coerceAtLeast(0),
        )
        val rules = when {
            signedDays < 0 -> pastRules
            signedDays > 0 -> futureRules
            else -> todayRules
        }
        return rules.map { it(snapshot) }
    }

    fun eventTargetDate(event: TimeEvent): LocalDate? = event.localDate
        ?: event.targetInstant?.atZone(ZoneId.of(event.zoneId ?: "Asia/Shanghai"))?.toLocalDate()

    private fun anniversaryCard(s: TimeSnapshot): InterestingCard {
        val years = s.elapsedPeriod.years
        val nextYears = years + 1
        val nextDate = safeAnniversary(s.targetDate, nextYears)
        return InterestingCard(
            "周年纪念",
            if (years == 0) "还未满 1 周年" else "已经过去 $years 周年",
            "距离 $nextYears 周年还有 ${ChronoUnit.DAYS.between(s.today, nextDate).coerceAtLeast(0)} 天",
        )
    }

    private fun milestoneDayCard(s: TimeSnapshot): InterestingCard {
        val tenThousand = s.targetDate.plusDays(10_000)
        return if (s.today.isBefore(tenThousand)) {
            InterestingCard("万日节点", "距离 10000 天还有 ${ChronoUnit.DAYS.between(s.today, tenThousand)} 天", "纪念日：$tenThousand")
        } else {
            val twentyThousand = s.targetDate.plusDays(20_000)
            InterestingCard("万日节点", "10000 天纪念日：$tenThousand", "距离 20000 天还有 ${ChronoUnit.DAYS.between(s.today, twentyThousand).coerceAtLeast(0)} 天")
        }
    }

    private fun futureMonthsCard(s: TimeSnapshot): InterestingCard {
        val months = s.remainingPeriod.toTotalMonths().coerceAtLeast(0)
        return InterestingCard("月份倒计时", "距离目标还有 ${if (months == 0L) "不足 1 个完整月" else "$months 个月"}", "目标日：${s.targetDate}")
    }

    private fun anniversaryMilestoneCard(s: TimeSnapshot): InterestingCard {
        val years = listOf(1, 5, 10, 20, 25, 50)
        val next = years.firstOrNull { safeAnniversary(s.targetDate, it) >= s.today }
            ?: years.last()
        val date = safeAnniversary(s.targetDate, next)
        return InterestingCard("生日 / 周年节点", "距离 $next 周年还有 ${ChronoUnit.DAYS.between(s.today, date).coerceAtLeast(0)} 天", "节点日期：$date")
    }

    private fun futureAnniversaryCard(s: TimeSnapshot): InterestingCard {
        val date = safeAnniversary(s.targetDate, 1)
        return InterestingCard("生日 / 周年节点", "目标日后的 1 周年是 $date", "距离这个节点还有 ${ChronoUnit.DAYS.between(s.today, date)} 天")
    }

    private fun futureMilestoneDayCard(s: TimeSnapshot): InterestingCard {
        val date = s.targetDate.plusDays(10_000)
        return InterestingCard("万日节点", "目标日后的 10000 天是 $date", "从今天到这个纪念日还有 ${ChronoUnit.DAYS.between(s.today, date)} 天")
    }

    private fun specialDayCard(s: TimeSnapshot): InterestingCard {
        val next = listOf(11_111L, 12_345L, 20_000L).firstOrNull { s.targetDate.plusDays(it) >= s.today }
            ?: 20_000L
        val date = s.targetDate.plusDays(next)
        return InterestingCard("下一个特殊数字日", "距离 $next 天还有 ${ChronoUnit.DAYS.between(s.today, date).coerceAtLeast(0)} 天", "日期：$date")
    }

    private fun futureSpecialDayCard(s: TimeSnapshot): InterestingCard {
        val next = listOf(11_111L, 12_345L, 20_000L).minByOrNull { s.targetDate.plusDays(it).toEpochDay() } ?: 11_111L
        val date = s.targetDate.plusDays(next)
        return InterestingCard("下一个特殊数字日", "从目标日开始的 $next 天节点是 $date", "距离这个节点还有 ${ChronoUnit.DAYS.between(s.today, date)} 天")
    }

    private fun weekdayCard(s: TimeSnapshot): InterestingCard = InterestingCard(
        "日期巧合",
        "目标日是星期${weekday(s.targetDate)}",
        if (s.targetDate.dayOfWeek == s.today.dayOfWeek) "今天和目标日都是星期${weekday(s.today)}" else "今天是星期${weekday(s.today)}，目标日是星期${weekday(s.targetDate)}",
    )

    private fun daysUntilNextHundredWeek(s: TimeSnapshot): Long {
        val currentWeeks = s.elapsedDays / 7
        val nextDate = s.targetDate.plusDays(((currentWeeks / 100 + 1) * 100 * 7))
        return ChronoUnit.DAYS.between(s.today, nextDate).coerceAtLeast(0)
    }

    private fun heartbeats(days: Long): Double = days.toDouble() * 24 * 60 * 75

    private fun safeAnniversary(start: LocalDate, years: Int): LocalDate = start.plusYears(years.toLong())

    private fun formatLargeNumber(value: Double): String = if (value >= 100_000_000) {
        "%.2f 亿".format(value / 100_000_000)
    } else {
        "%.0f".format(value)
    }

    private fun weekday(date: LocalDate): String = when (date.dayOfWeek.value) {
        1 -> "一"; 2 -> "二"; 3 -> "三"; 4 -> "四"; 5 -> "五"; 6 -> "六"; else -> "日"
    }
}
