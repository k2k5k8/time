package com.cch.momentmark.ui.daybook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cch.momentmark.domain.model.DaybookEvent
import com.cch.momentmark.domain.model.DaybookEventSource
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

private val weekLabels = listOf("一", "二", "三", "四", "五", "六", "日")

private fun buildCalendarDays(month: YearMonth): List<LocalDate?> {
    val emptyLeadingDays = month.atDay(1).dayOfWeek.value - DayOfWeek.MONDAY.value
    return List(emptyLeadingDays) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
}

/**
 * 日历主体卡片：大圆角白色卡片，内含星期标题栏、日期网格和底部图例。
 *
 * 日期选择逻辑通过 [onDateSelected] 回调上抛，由 [DaybookFeature] 管理状态。
 */
@Composable
internal fun CalendarGrid(
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    monthEvents: List<DaybookEvent>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val calendarDays = remember(visibleMonth) { buildCalendarDays(visibleMonth) }
    // 预先按日期分组，避免每个日期格在重组时都执行整月过滤
    val eventsByDate = remember(monthEvents) { monthEvents.groupBy { it.date } }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(DaybookCardRadius),
                ambientColor = Color(0x14000000),
                spotColor = Color(0x14000000),
            ),
        shape = RoundedCornerShape(DaybookCardRadius),
        color = DaybookSurface,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 12.dp,
            ),
        ) {
            // 星期标题栏
            WeekLabelRow()

            Spacer(Modifier.height(6.dp))

            // 日期网格
            calendarDays.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    week.forEach { date ->
                        if (date == null) {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(0.9f))
                        } else {
                            DayCell(
                                date = date,
                                isSelected = date == selectedDate,
                                isToday = date == today,
                                events = eventsByDate[date].orEmpty(),
                                onClick = { onDateSelected(date) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    repeat(7 - week.size) {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(0.9f))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // 底部图例
            CalendarLegendRow()
        }
    }
}

@Composable
private fun WeekLabelRow() {
    Row(modifier = Modifier.fillMaxWidth()) {
        weekLabels.forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = DaybookWeekLabelFontSize,
                fontWeight = FontWeight.Medium,
                color = if (label == "日") DaybookPrimary else DaybookText,
            )
        }
    }
}

/**
 * 单个日期格：统一尺寸的圆形日期底。
 *
 * 选中态为橙色实心圆 + 白字；今天为浅橙底；事件小点对所有日期（含选中）可见。
 * 圆形与文字均为固定尺寸，不放大、不越界，避免遮挡上下相邻行的日期与标记。
 */
@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    events: List<DaybookEvent>,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .aspectRatio(0.9f)
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                contentDescription = buildString {
                    append("${date.monthValue}月${date.dayOfMonth}日")
                    if (isToday) append("，今天")
                    if (events.isNotEmpty()) append("，${events.size}条记录")
                }
                role = Role.Button
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // 日期数字 + 圆形背景（尺寸固定，选中不放大）
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .then(
                    when {
                        isSelected -> Modifier.background(DaybookPrimary)
                        isToday -> Modifier.background(DaybookPrimaryTint)
                        else -> Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = if (isSelected) DaybookSelectedDateFontSize else DaybookDateFontSize,
                fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) Color.White else DaybookText,
            )
        }

        Spacer(Modifier.height(3.dp))

        // 事件标记：所有日期（含选中）统一显示横杠
        if (events.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                events.take(3).forEach { event ->
                    Box(
                        modifier = Modifier
                            .size(6.dp, 2.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (event.source == DaybookEventSource.SYSTEM) DaybookSystemMark
                                else DaybookUserMark,
                            ),
                    )
                }
                if (events.size > 3) {
                    Text(text = "+", fontSize = 8.sp, color = DaybookMuted)
                }
            }
        } else {
            // 占位，保持高度一致
            Spacer(Modifier.size(2.dp))
        }
    }
}

@Composable
private fun CalendarLegendRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalendarLegendItem(color = DaybookSystemMark, label = "节日 / 特殊日")
        Spacer(Modifier.width(32.dp))
        CalendarLegendItem(color = DaybookUserMark, label = "我的记录")
    }
}

@Composable
private fun CalendarLegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            fontSize = DaybookLegendFontSize,
            color = DaybookMuted,
        )
    }
}
