package com.cch.momentmark.ui.daybook

import com.cch.momentmark.domain.model.TaskPlanPeriod
import com.cch.momentmark.domain.model.TaskRepeatBreakKind
import com.cch.momentmark.domain.model.TaskRepeatRule
import com.cch.momentmark.domain.model.TaskRepeatSchedule
import java.time.Clock
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Read-only manager copy. Dates and status follow the rule's own timezone. */
internal data class RepeatSeriesSummary(val status: String, val schedule: String, val range: String)

internal fun repeatSeriesSummary(rule: TaskRepeatRule, breakKind: TaskRepeatBreakKind?, clock: Clock): RepeatSeriesSummary {
    val today = TaskRepeatSchedule.today(rule, clock)
    val status = when (breakKind) {
        TaskRepeatBreakKind.PAUSE -> "暂停"
        TaskRepeatBreakKind.STOP -> "停止"
        null -> when {
            today < rule.effectiveFrom -> "未开始"
            rule.endDate?.let { it < today } == true -> "结束"
            else -> "执行中"
        }
    }
    val cycle = if (rule.weekdays.size == 7) "每天" else
        "每周" + rule.weekdays.sortedBy { it.value }.joinToString("、") { "一二三四五六日"[it.ordinal].toString() }
    val plan = rule.plan.time?.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)) ?: when (rule.plan.period) {
        TaskPlanPeriod.ANYTIME -> "随时"
        TaskPlanPeriod.MORNING -> "早上"
        TaskPlanPeriod.MIDDAY -> "中午"
        TaskPlanPeriod.EVENING -> "晚上"
    }
    val includeYear = rule.startDate.year != today.year ||
        rule.endDate?.let { it.year != today.year || it.year != rule.startDate.year } == true
    val format = DateTimeFormatter.ofPattern(if (includeYear) "uuuu.MM.dd" else "MM.dd", Locale.ROOT)
    val end = rule.endDate?.let { "结束 ${it.format(format)}" } ?: "无结束日"
    return RepeatSeriesSummary(status, "$cycle · $plan", "开始 ${rule.startDate.format(format)} — $end")
}
