package com.cch.momentmark.ui.task

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import com.cch.momentmark.ui.components.DayOffsetSelection
import com.cch.momentmark.ui.components.PixelTextInput
import com.cch.momentmark.ui.components.TaskDateInputSelection
import com.cch.momentmark.ui.daybook.TaskActionDialog
import com.cch.momentmark.ui.theme.MomentMarkTokens

/** Controlled date drafts. The owner freezes the reference, validates and commits absolute dates. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TaskDateInputDialog(
    offset: DayOffsetSelection?, absolute: TaskDateInputSelection?,
    onInput: (String) -> Unit, onDays: (Long) -> Unit,
    onDateField: (String, String) -> Unit, onDatePreset: (Long) -> Unit,
    onConfirm: () -> Unit, onDismiss: () -> Unit,
) {
    if (offset == null && absolute == null) return
    val deviceDensity = LocalView.current.resources.displayMetrics.density
    val inputHeight = with(LocalDensity.current) { (48f * deviceDensity).toDp() }
    val date = offset?.confirmedDate ?: if (offset == null) absolute?.date else null
    val invalid = if (offset != null) offset.input.isNotEmpty() && date == null
        else absolute!!.let { it.year.isNotEmpty() && it.month.isNotEmpty() && it.day.isNotEmpty() && date == null }
    TaskActionDialog("自定义日期", "task-date-input", onDismiss, closeLabel = "关闭自定义日期",
        avoidIme = true, footer = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
                TaskFormAction("取消", onDismiss, Modifier.weight(1f))
                TaskFormAction("确认日期", onConfirm, Modifier.weight(1f), enabled = date != null)
            }
        }) {
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit),
            verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
            listOf(3L, 7L, 14L, 30L).forEach { days ->
                TaskFormAction("${days}天后", { if (offset != null) onDays(days) else onDatePreset(days) },
                    enabled = if (offset != null) days <= offset.maximum else absolute?.preset(days) != null,
                    selected = if (offset != null) offset.inputDays == days else absolute?.preset(days)?.date == date && date != null)
            }
        }
        if (offset != null) {
            PixelTextInput("天后", offset.input, "", "天后数字", onInput, singleLine = true,
                isError = invalid, framed = false, followCursor = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), minInputHeight = inputHeight)
        } else if (absolute != null) {
            @Composable fun field(key: String, label: String, value: String, modifier: Modifier = Modifier) {
                PixelTextInput(label, value, "", "计划日期$label", { onDateField(key, it) }, modifier,
                    singleLine = true, isError = invalid, framed = false, followCursor = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), minInputHeight = inputHeight)
            }
            // Wide years retain readable space; the month/day pair wraps with large fonts.
            field("year", "年", absolute.year)
            if (LocalDensity.current.fontScale > 1.3f) {
                field("month", "月", absolute.month); field("day", "日", absolute.day)
            } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
                field("month", "月", absolute.month, Modifier.weight(1f)); field("day", "日", absolute.day, Modifier.weight(1f))
            }
        }
        Text(date?.toString() ?: if (invalid) {
            if (offset != null) "请输入有效的非负整数天数" else "请输入真实的年月日"
        } else "请填写日期", Modifier.fillMaxWidth().semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = date?.let { "目标日期 $it" } ?: "日期尚未有效"
        }, style = MaterialTheme.typography.bodyMedium,
            color = if (invalid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
    }
}
