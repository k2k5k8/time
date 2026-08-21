package com.cch.momentmark.ui.daybook

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cch.momentmark.domain.model.DaybookEvent
import com.cch.momentmark.domain.model.DaybookEventSource
import com.cch.momentmark.domain.model.DaybookEventType
import com.cch.momentmark.domain.model.MockDaybookDataSource
import com.cch.momentmark.domain.model.TimeEvent
import com.cch.momentmark.ui.components.TimelineDestination
import com.cch.momentmark.ui.components.TimelineNavigation
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val DaybookBackground = Color(0xFFFFF9F2)
private val DaybookSurface = Color(0xFFFFFCF9).copy(alpha = 0.96f)
private val DaybookText = Color(0xFF4F4036)
private val DaybookMuted = Color(0xFF9E8D7D)
private val DaybookLine = Color(0xFFE9DCD0)
private val SystemMark = Color(0xFFE7A58D)
private val UserMark = Color(0xFF7E9BC5)
private val TodayTint = Color(0xFFE9B18F)
private val DaybookDateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日")

private val weekLabels = listOf("一", "二", "三", "四", "五", "六", "日")

/** 日子簿页面边界：当前使用 mock data，导航和日历数据源可独立替换。 */
@Composable
internal fun DaybookFeature(
    userEvents: List<TimeEvent>,
    onOpenCreateEvent: (LocalDate) -> Unit,
    onOpenBigEvents: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    val dataSource = remember(userEvents, today) { MockDaybookDataSource(userEvents, today) }
    var selectedDateText by rememberSaveable { mutableStateOf(today.toString()) }
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    val selectedDate = runCatching { LocalDate.parse(selectedDateText) }.getOrDefault(today)
    val visibleMonth = runCatching { YearMonth.parse(monthText) }.getOrDefault(YearMonth.from(today))
    val monthEvents = remember(dataSource, visibleMonth) { dataSource.eventsForMonth(visibleMonth) }
    val selectedEvents = remember(dataSource, selectedDate) { dataSource.eventsForDate(selectedDate) }
    val calendarDays = remember(visibleMonth) {
        val emptyLeadingDays = visibleMonth.atDay(1).dayOfWeek.value - DayOfWeek.MONDAY.value
        List(emptyLeadingDays) { null } + (1..visibleMonth.lengthOfMonth()).map { visibleMonth.atDay(it) }
    }
    val moveMonth: (Int) -> Unit = { offset ->
        val nextMonth = visibleMonth.plusMonths(offset.toLong())
        val nextDay = selectedDate.dayOfMonth.coerceAtMost(nextMonth.lengthOfMonth())
        monthText = nextMonth.toString()
        selectedDateText = nextMonth.atDay(nextDay).toString()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DaybookBackground)
            // The app draws edge-to-edge. Keep the month switcher below the
            // status/cutout area on punch-hole and notch devices.
            .statusBarsPadding(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                DaybookHeader(
                    visibleMonth = visibleMonth,
                    onPreviousMonth = { moveMonth(-1) },
                    onNextMonth = { moveMonth(1) },
                )
            }
            item {
                DaybookCalendar(
                    visibleMonth = visibleMonth,
                    calendarDays = calendarDays,
                    selectedDate = selectedDate,
                    today = today,
                    monthEvents = monthEvents,
                    onDateSelected = { date ->
                        selectedDateText = date.toString()
                        monthText = YearMonth.from(date).toString()
                    },
                )
            }
            item {
                DaybookDetail(
                    selectedDate = selectedDate,
                    events = selectedEvents,
                    onAdd = { onOpenCreateEvent(selectedDate) },
                )
            }
        }

        TimelineNavigation(
            selectedDestination = TimelineDestination.DAYBOOK,
            onSelectBigEvents = onOpenBigEvents,
            onSelectDaybook = null,
            onCreateEvent = { onOpenCreateEvent(selectedDate) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun DaybookHeader(
    visibleMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(DaybookSurface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier.semantics { contentDescription = "上个月" },
            ) { Icon(Icons.Outlined.ChevronLeft, contentDescription = null, tint = DaybookMuted) }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "${visibleMonth.monthValue}月", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DaybookText)
                Text(text = visibleMonth.year.toString(), fontSize = 12.sp, color = DaybookMuted)
            }
            IconButton(
                onClick = onNextMonth,
                modifier = Modifier.semantics { contentDescription = "下个月" },
            ) { Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = DaybookMuted) }
    }
}

@Composable
private fun DaybookCalendar(
    visibleMonth: YearMonth,
    calendarDays: List<LocalDate?>,
    selectedDate: LocalDate,
    today: LocalDate,
    monthEvents: List<DaybookEvent>,
    onDateSelected: (LocalDate) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = DaybookSurface,
        border = BorderStroke(1.dp, DaybookLine.copy(alpha = 0.72f)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                weekLabels.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = if (label == "日") SystemMark else DaybookMuted,
                    )
                }
            }
            calendarDays.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        if (date == null) {
                            Spacer(modifier = Modifier.weight(1f).height(64.dp))
                        } else {
                            DaybookDateCell(
                                date = date,
                                isSelected = date == selectedDate,
                                isToday = date == today,
                                events = monthEvents.filter { it.date == date },
                                onClick = { onDateSelected(date) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    repeat(7 - week.size) { Spacer(modifier = Modifier.weight(1f).height(64.dp)) }
                }
            }
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CalendarLegend(color = SystemMark, label = "节日 / 特殊日")
                CalendarLegend(color = UserMark, label = "我的记录")
            }
        }
    }
}

@Composable
private fun DaybookDateCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    events: List<DaybookEvent>,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val selectedColor = if (isSelected) TodayTint else Color.Transparent
    Column(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = buildString {
                    append("${date.monthValue}月${date.dayOfMonth}日")
                    if (isToday) append("，今天")
                    if (events.isNotEmpty()) append("，${events.size}条记录")
                }
                role = Role.Button
            }
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(selectedColor)
                .then(if (isToday && !isSelected) Modifier.background(TodayTint.copy(alpha = 0.16f)) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = 14.sp,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else DaybookText,
            )
        }
        Row(
            modifier = Modifier.height(14.dp).padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            events.take(3).forEach { event ->
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (event.source == DaybookEventSource.SYSTEM) SystemMark else UserMark),
                )
            }
            if (events.size > 3) Text(text = "+", fontSize = 9.sp, color = DaybookMuted)
        }
    }
}

@Composable
private fun CalendarLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(modifier = Modifier.width(12.dp).height(3.dp).clip(RoundedCornerShape(50)).background(color))
        Text(text = label, fontSize = 11.sp, color = DaybookMuted)
    }
}

@Composable
private fun DaybookDetail(
    selectedDate: LocalDate,
    events: List<DaybookEvent>,
    onAdd: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "${selectedDate.dayOfMonth}", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = DaybookText)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = selectedDate.format(DaybookDateFormatter), fontSize = 13.sp, color = DaybookMuted)
                Text(text = "星期${weekLabels[selectedDate.dayOfWeek.value - 1]}", fontSize = 12.sp, color = DaybookMuted)
            }
            if (events.isNotEmpty()) {
                IconButton(
                    onClick = onAdd,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(TodayTint.copy(alpha = 0.22f))
                        .semantics {
                            contentDescription = "为${selectedDate.monthValue}月${selectedDate.dayOfMonth}日新增日期记录"
                            role = Role.Button
                        },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        tint = DaybookText,
                    )
                }
            }
        }
        if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(DaybookSurface)
                    .clickable(role = Role.Button, onClick = onAdd)
                    .semantics {
                        contentDescription = "这一天还没有记录，点击添加事件"
                        role = Role.Button
                    }
                    .padding(horizontal = 20.dp, vertical = 24.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "今天还没有记录", fontSize = 16.sp, color = DaybookText)
                    Text(text = "点击这里，为这一天添加一点内容", fontSize = 13.sp, color = DaybookMuted, modifier = Modifier.padding(top = 6.dp))
                }
            }
        } else {
            events.forEach { event -> DaybookEventRow(event) }
        }
    }
}

@Composable
private fun DaybookEventRow(event: DaybookEvent) {
    val sourceLabel = if (event.source == DaybookEventSource.SYSTEM) "系统日子" else "我的记录"
    val typeLabel = when (event.eventType) {
        DaybookEventType.FESTIVAL -> "节日"
        DaybookEventType.SOLAR_TERM -> "节气"
        DaybookEventType.ANNIVERSARY -> "纪念日"
        DaybookEventType.PERSONAL -> "日常"
        DaybookEventType.REMINDER -> "提醒"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DaybookSurface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(4.dp).height(38.dp).clip(RoundedCornerShape(50)).background(if (event.source == DaybookEventSource.SYSTEM) SystemMark else UserMark))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = event.title, fontSize = 16.sp, color = DaybookText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = listOf(sourceLabel, typeLabel, event.subtitle).filter { it.isNotBlank() }.joinToString(" · "), fontSize = 12.sp, color = DaybookMuted, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
