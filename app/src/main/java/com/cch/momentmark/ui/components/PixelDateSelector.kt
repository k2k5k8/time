package com.cch.momentmark.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MomentMarkTokens
import java.time.LocalDate

/**
 * J 表单的三格全天日期选择器；日期事实与方向校验由上层 ViewModel 管理。
 *
 * 与 design.html `.px` 面板 + 年/月/日三个 white box 完全一致：
 *   - 顶部 label 行：左 fieldlabel，右 helperLabel（颜色随方向切换）
 *   - 三格内填：年份 flex 1.2、月份 / 日期 flex 1（容纳 4 位数年份）
 *   - 固定 `[x] 全天` 仅在 FUTURE 模式由上层传入，普通模式不显示
 */
@Composable
fun PixelDateSelector(
    label: String,
    dateText: String,
    year: Int?,
    month: Int?,
    day: Int?,
    onYearSelected: (Int) -> Unit,
    onMonthSelected: (Int) -> Unit,
    onDaySelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    yearRange: IntRange? = null,
    helperLabel: String? = null,
    helperColor: Color = MaterialTheme.colorScheme.secondary,
) {
    val fallback = remember(dateText) { runCatching { LocalDate.parse(dateText) }.getOrNull() }
    val selectedYear = year ?: fallback?.year ?: 2000
    val selectedMonth = month ?: fallback?.monthValue ?: 1
    val selectedDay = day ?: fallback?.dayOfMonth ?: 1
    val years = yearRange ?: (selectedYear - 100..selectedYear + 100)
    PixelPanel(modifier = modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceInner),
            // design.html `margin-top:8px`：label 行与三格之间固定 8dp。
            verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCompact),
        ) {
            // 顶行：左 fieldlabel、右 helperLabel；参考 HTML `display:flex; justify-content:space-between`
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PixelFieldLabel(label, modifier = Modifier.weight(1f))
                helperLabel?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.8.sp,
                            lineHeight = 11.sp,
                        ),
                        color = helperColor,
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCompact)) {
                DatePart(
                    label = "年",
                    value = selectedYear.toString(),
                    options = years.toList(),
                    onSelected = onYearSelected,
                    modifier = Modifier.weight(1.2f),
                )
                DatePart(
                    label = "月",
                    value = "%02d".format(selectedMonth),
                    options = (1..12).toList(),
                    onSelected = onMonthSelected,
                    modifier = Modifier.weight(1f),
                )
                DatePart(
                    label = "日",
                    value = "%02d".format(selectedDay),
                    options = (1..31).toList(),
                    onSelected = onDaySelected,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DatePart(label: String, value: String, options: List<Int>, onSelected: (Int) -> Unit, modifier: Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val pressInteraction = remember { MutableInteractionSource() }
    Box(modifier) {
        PixelPanel(
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = "${label}选择器，当前$value"
                role = Role.Button
            }.clickable(interactionSource = pressInteraction, indication = null, role = Role.Button) { expanded = !expanded },
            backgroundColor = MaterialTheme.colorScheme.surface,
            shadowColor = MaterialTheme.colorScheme.outline,
            pressInteractionSource = pressInteraction,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MomentMarkTokens.FormDateCellPaddingHorizontal,
                        vertical = MomentMarkTokens.FormDateCellPaddingVertical,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = MomentMarkTokens.FormDateCellValueSize,
                        lineHeight = MomentMarkTokens.FormDateCellValueLineHeight,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.8.sp,
                        lineHeight = 11.sp,
                    ),
                    color = LocalMmExtendedColors.current.labelTertiary,
                    modifier = Modifier.padding(start = MomentMarkTokens.SpaceUnit / 2),
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .pixelClickable(role = Role.Button) { onSelected(option); expanded = false }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(if (label == "年") option.toString() else "%02d".format(option))
                }
            }
        }
    }
}
