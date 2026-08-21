package com.cch.momentmark.ui.daybook

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.cch.momentmark.domain.model.DaybookEvent
import com.cch.momentmark.domain.model.MockDaybookDataSource
import com.cch.momentmark.domain.model.TimeEvent
import com.cch.momentmark.ui.components.TimelineDestination
import com.cch.momentmark.ui.components.TimelineNavigation
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

private const val CalendarPageSpan = 600

private fun monthsBetween(from: YearMonth, to: YearMonth): Int =
    (to.year - from.year) * 12 + (to.monthValue - from.monthValue)

/**
 * 日子簿页面边界。
 *
 * 入口签名、数据源、翻页逻辑与状态管理完全保持不变。
 * UI 层拆分为 CalendarHeader / CalendarGrid /
 * CustomRecordCard（含节日与自定义记录）等独立组件，
 * 底部导航与首页共用 TimelineNavigation。
 */
@Composable
internal fun DaybookFeature(
    userEvents: List<TimeEvent>,
    onOpenCreateEvent: (LocalDate) -> Unit,
    onOpenBigEvents: () -> Unit,
    onOpenEventDetail: (TimeEvent) -> Unit = {},
) {
    val today = remember { LocalDate.now() }
    val dataSource = remember(userEvents, today) { MockDaybookDataSource(userEvents, today) }
    var selectedDateText by rememberSaveable { mutableStateOf(today.toString()) }
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    val selectedDate = runCatching { LocalDate.parse(selectedDateText) }.getOrDefault(today)
    val visibleMonth = runCatching { YearMonth.parse(monthText) }.getOrDefault(YearMonth.from(today))
    val selectedEvents = remember(dataSource, selectedDate) { dataSource.eventsForDate(selectedDate) }
    val scope = rememberCoroutineScope()

    // 无限翻页日历：基准月固定为 today 所在月份，页号 = 相对基准月的偏移。
    val baseMonth = remember { YearMonth.from(today) }
    val pagerState = rememberPagerState(
        initialPage = (CalendarPageSpan + monthsBetween(baseMonth, visibleMonth))
            .coerceIn(0, CalendarPageSpan * 2),
        pageCount = { CalendarPageSpan * 2 },
    )

    // 翻页（滑动或箭头）时同步月份标题与选中日期
    LaunchedEffect(pagerState, baseMonth) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                val month = baseMonth.plusMonths((page - CalendarPageSpan).toLong())
                if (month != visibleMonth) {
                    monthText = month.toString()
                    val day = selectedDate.dayOfMonth.coerceAtMost(month.lengthOfMonth())
                    selectedDateText = month.atDay(day).toString()
                }
            }
    }

    // 外部改变月份时驱动 pager 平滑跳转
    LaunchedEffect(monthText, baseMonth) {
        val targetPage = (CalendarPageSpan + monthsBetween(baseMonth, visibleMonth))
            .coerceIn(0, CalendarPageSpan * 2)
        if (targetPage != pagerState.currentPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // 卡片入场动画
    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animationTriggered = true
    }
    val headerAlpha by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "header_alpha",
    )
    val calendarAlpha by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 100),
        label = "calendar_alpha",
    )
    val recordAlpha by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 200),
        label = "record_alpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DaybookBackground)
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = DaybookHorizontalPadding,
                    top = 8.dp,
                    end = DaybookHorizontalPadding,
                    bottom = 88.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(DaybookCardSpacing),
        ) {
            // 顶部标题区域
            CalendarHeader(
                visibleMonth = visibleMonth,
                onPreviousMonth = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                },
                onNextMonth = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                modifier = Modifier.alpha(headerAlpha),
            )

            // 日历主体卡片（横向翻页），页间留出空隙
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(calendarAlpha),
                pageSpacing = 10.dp,
                beyondViewportPageCount = 1,
                flingBehavior = PagerDefaults.flingBehavior(
                    state = pagerState,
                    pagerSnapDistance = PagerSnapDistance.atMost(1),
                    snapAnimationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                ),
            ) { page ->
                val month = baseMonth.plusMonths((page - CalendarPageSpan).toLong())
                val monthEvents = remember(dataSource, month) {
                    dataSource.eventsForMonth(month)
                }
                CalendarGrid(
                    visibleMonth = month,
                    selectedDate = selectedDate,
                    today = today,
                    monthEvents = monthEvents,
                    onDateSelected = { date ->
                        selectedDateText = date.toString()
                        monthText = YearMonth.from(date).toString()
                    },
                )
            }

            // 当日记录卡片：节日记录在上，自定义记录在下
            CustomRecordCard(
                selectedDate = selectedDate,
                events = selectedEvents,
                onAdd = { onOpenCreateEvent(selectedDate) },
                onOpenEvent = { record ->
                    // 用户记录的 id 形如 "user-<TimeEvent.id>"，还原后进入详情
                    val eventId = record.id.removePrefix("user-")
                    userEvents.firstOrNull { it.id == eventId }?.let(onOpenEventDetail)
                },
                modifier = Modifier.alpha(recordAlpha),
            )
        }

        // 底部导航：与首页共用的胶囊导航
        TimelineNavigation(
            selectedDestination = TimelineDestination.DAYBOOK,
            onSelectBigEvents = onOpenBigEvents,
            onSelectDaybook = null,
            onCreateEvent = { onOpenCreateEvent(selectedDate) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
