package com.cch.momentmark.ui.daybook

import com.cch.momentmark.domain.model.TaskPlan
import com.cch.momentmark.domain.model.TaskRepeatRule
import java.time.DayOfWeek
import java.time.LocalDate

/** Only editable fields enter the copy; identity, original start and rule zone stay intact. */
internal fun editedRepeatRule(original: TaskRepeatRule, effectiveDate: LocalDate, title: String,
    days: Set<DayOfWeek>, end: LocalDate?, plan: TaskPlan, difficulty: Int?): TaskRepeatRule {
    require(title.isNotBlank()) { "请填写任务标题" }
    require(days.isNotEmpty()) { "至少选择一个执行日" }
    require(end == null || end >= effectiveDate) { "结束日不能早于本次生效日" }
    require((0L..6L).any {
        val date = effectiveDate.plusDays(it)
        (end == null || date <= end) && date.dayOfWeek in days
    }) { "日期范围内没有有效执行日" }
    return original.copy(effectiveFrom = effectiveDate, title = title.trim(), weekdays = days,
        endDate = end, plan = plan, difficulty = difficulty)
}
