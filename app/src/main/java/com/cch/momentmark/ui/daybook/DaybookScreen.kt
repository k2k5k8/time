package com.cch.momentmark.ui.daybook

import com.cch.momentmark.ui.components.pixelClickable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.key
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cch.momentmark.domain.model.Task
import com.cch.momentmark.ui.components.PixelButton
import com.cch.momentmark.ui.components.CalendarDayCell
import com.cch.momentmark.ui.components.DateWheelSelection
import com.cch.momentmark.ui.components.DateWheelCopy
import com.cch.momentmark.ui.components.PixelDateWheel
import com.cch.momentmark.ui.components.NewQuestEntry
import com.cch.momentmark.ui.components.PixelConfirmationDialog
import com.cch.momentmark.ui.components.PixelPanel
import com.cch.momentmark.ui.components.PixelStatusHud
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MmNoFontPadding
import com.cch.momentmark.ui.theme.MomentMarkTokens
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** J ⑤ 日子簿：真实 Task 按所属日期显示，切日只改变查询，绝不迁移过期任务。 */
@Composable
fun DaybookScreen(
    uiState: DaybookUiState,
    onAcceptNewTask: () -> Unit,
    onOpenSettings: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (Long) -> Unit,
    onTaskCompletionToggled: (Task) -> Unit,
    onTaskEditRequested: (String) -> Unit,
    onTaskSealRequested: (Task) -> Unit,
    modifier: Modifier = Modifier,
    onDateConfirmed: (LocalDate, Int) -> Unit = { date, _ -> onDateSelected(date) },
    onTodayRequested: () -> Unit = { onDateSelected(uiState.today) },
    onPickerOpened: () -> Unit = {},
    onPickerDraftChanged: ((DateWheelSelection?) -> Unit)? = null,
    onRetryTasks: () -> Unit = {},
    onRetryCalendar: () -> Unit = {},
    onRepeatSkip: (Task, Boolean) -> Unit = { _, _ -> },
    onRepeatPause: (String, Boolean) -> Unit = { _, _ -> },
    onRepeatResume: (String) -> Unit = {},
    onRepeatEdited: (com.cch.momentmark.domain.model.TaskRepeatRule, () -> Unit) -> Unit = { _, _ -> },
    onTaskMove: (Task, LocalDate) -> Unit = { _, _ -> },
    onPendingAction: ((Task, PendingTaskAction, LocalDate) -> Unit)? = null,
    onRetryPending: () -> Unit = {},
    onTaskAutoDefer: (Task, Boolean) -> Unit = { _, _ -> },
    onMoveUndo: () -> Unit = {},
    onMakeUp: (Task) -> Unit = {},
    onLocate: (LocalDate) -> Unit = {},
    executionPlans: Map<String, TaskExecutionPlan> = emptyMap(),
    clock: java.time.Clock = java.time.Clock.systemDefaultZone(),
    acceptedTaskId: String? = null,
    saveFeedback: String? = null,
    onTaskCompletionRequested: ((Task, (Boolean) -> Unit) -> Unit)? = null,
) {
    val completionTransitions = remember(uiState.selectedDate) { DaybookCompletionTransitions() }
    DaybookCompletionEffects(completionTransitions, uiState.tasks)
    val pageScroll = rememberScrollState()
    LaunchedEffect(uiState.selectedDate) { pageScroll.scrollTo(0) }
    var pickerOpen by rememberSaveable { mutableStateOf(uiState.datePickerSelection != null) }
    val dateSaver = remember { Saver<DateWheelSelection, List<String>>(
        save = { listOf(it.date.toString(), it.intendedDay.toString()) },
        restore = { DateWheelSelection(LocalDate.parse(it[0]), it[1].toInt()) },
    ) }
    var draft by rememberSaveable(stateSaver = dateSaver) { mutableStateOf(uiState.datePickerSelection ?: DateWheelSelection(uiState.selectedDate, uiState.dayIntent)) }
    val pickerVisible = if (onPickerDraftChanged != null) uiState.datePickerSelection != null else pickerOpen
    val pickerSelection = uiState.datePickerSelection ?: draft
    var singleTask by remember(uiState.selectedDate) { mutableStateOf<Task?>(null) }
    var repeatTask by remember(uiState.selectedDate) { mutableStateOf<Task?>(null) }
    var repeatRule by remember(uiState.selectedDate) { mutableStateOf<com.cch.momentmark.domain.model.TaskRepeatRule?>(null) }
    var repeatRuleDate by remember(uiState.selectedDate) { mutableStateOf<LocalDate?>(null) }
    var repeatStop by remember(uiState.selectedDate) { mutableStateOf<String?>(null) }
    var accepting by remember { mutableStateOf(false) }
    fun updateDraft(selection: DateWheelSelection) { draft = selection; onPickerDraftChanged?.invoke(selection) }
    fun closePicker() { draft = pickerSelection; pickerOpen = false; onPickerDraftChanged?.invoke(null) }
    val titleFocus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    var landingDate by remember { mutableStateOf<LocalDate?>(null) }
    // A committed date starts at its own origin in the same composition. Resetting
    // only in LaunchedEffect draws the new month with the old +/-1 offset for a frame.
    var swipe by remember(uiState.selectedDate, uiState.dayIntent, pickerVisible) { mutableStateOf(0f) }
    var swipeWidth by remember { mutableStateOf(1f) }
    val pageGapPx = with(LocalDensity.current) { MomentMarkTokens.DaybookMonthPageGap.roundToPx() }
    var cancelled by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var settling by remember { mutableStateOf(false) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val moving = dragging || settling || swipe != 0f
    LaunchedEffect(moving) { if (moving) completionTransitions.entries.clear() }
    fun settle(delta: Long) {
        settling = true
        settleJob?.cancel()
        settleJob = scope.launch {
            try {
                animate(swipe, -delta.toFloat(), animationSpec = tween(MomentMarkTokens.DaybookMonthSettleMs)) { value, _ -> swipe = value }
                if (delta != 0L) {
                    landingDate = DateWheelSelection(uiState.selectedDate, uiState.dayIntent)
                        .inMonth(uiState.visibleMonth.plusMonths(delta)).date
                    onMonthChanged(delta)
                } else swipe = 0f
            } finally { settling = false }
        }
    }
    LaunchedEffect(uiState.selectedDate, uiState.dayIntent, pickerVisible) {
        settleJob?.cancel(); swipe = 0f; dragging = false; settling = false
    }
    // Keep the handoff identity through the first successful result. Clearing it on
    // query completion would start a second date fade after the horizontal settle.
    LaunchedEffect(uiState.selectedDate) {
        if (landingDate != null && uiState.selectedDate != landingDate) landingDate = null
    }
    BackHandler(moving && !pickerVisible) { cancelled = true; dragging = false; settle(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, completionTransitions) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                completionTransitions.entries.clear()
                cancelled = true; settleJob?.cancel(); swipe = 0f; dragging = false; settling = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val dragModifier = Modifier.pointerInput(uiState.selectedDate, uiState.dayIntent, pickerVisible) {
        val tracker = VelocityTracker()
        detectHorizontalDragGestures(
            onDragStart = { if (!pickerVisible && !settling) { cancelled = false; dragging = true; tracker.resetTracking() } },
            onHorizontalDrag = { change, delta ->
                if (dragging && !cancelled) {
                    change.consume()
                    tracker.addPosition(change.uptimeMillis, change.position)
                    swipe = (swipe + delta / swipeWidth).coerceIn(-1f, 1f)
                }
            },
            onDragCancel = { cancelled = true; dragging = false; settle(0) },
            onDragEnd = {
                if (dragging && !cancelled) {
                    dragging = false
                    val velocity = tracker.calculateVelocity().x
                    val flip = kotlin.math.abs(swipe) >= MomentMarkTokens.DaybookMonthFlipFraction ||
                        (kotlin.math.abs(swipe) > 0.04f && kotlin.math.abs(velocity) / swipeWidth > MomentMarkTokens.DaybookMonthFlingWidthsPerSecond && velocity * swipe > 0)
                    settle(if (!flip) 0 else if (swipe < 0) 1 else -1)
                }
            },
        )
    }
    var pendingSealTask by remember(uiState.selectedDate) { mutableStateOf<Task?>(null) }
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = MomentMarkTokens.HomeStatusHudPadding),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("♥\uFE0E×3", style = MomentMarkTokens.HomeStatusHudStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(Modifier.heightIn(min = MomentMarkTokens.TouchTargetMin).semantics { contentDescription = "打开设置"; role = Role.Button }
                .pixelClickable(onClick = onOpenSettings), contentAlignment = Alignment.Center) {
                Text("DUNGEON MAP ···", style = MomentMarkTokens.HomeStatusHudStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        DaybookTitlePanel(
            month = uiState.visibleMonth,
            swipe = swipe,
            onPreviousMonth = { onMonthChanged(-1) },
            onNextMonth = { onMonthChanged(1) },
            onChooseDate = {
                onPickerOpened()
                accepting = false
                updateDraft(DateWheelSelection(uiState.selectedDate, uiState.dayIntent))
                pickerOpen = true
            },
            onToday = onTodayRequested,
            titleFocus = titleFocus,
        )
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(pageScroll)
                .padding(horizontal = MomentMarkTokens.DaybookScrollHorizontalPadding),
        ) {
            MonthSwipeLayout(
                progress = swipe,
                modifier = Modifier.fillMaxWidth().then(if (moving) Modifier.clipToBounds() else Modifier).testTag("daybook-month-swipe")
                    .onSizeChanged { swipeWidth = (it.width + pageGapPx).toFloat().coerceAtLeast(1f) },
                calendar = { delta ->
                    val selection = DateWheelSelection(uiState.selectedDate, uiState.dayIntent)
                        .inMonth(uiState.visibleMonth.plusMonths(delta.toLong()))
                    val interactive = delta == 0 && !moving
                    Box(Modifier.then(if (interactive) Modifier else Modifier.clearAndSetSemantics {})) {
                        MonthGrid(
                            month = YearMonth.from(selection.date), selectedDate = selection.date,
                            calendarMonths = uiState.calendarMonths, today = uiState.today,
                            onRetry = onRetryCalendar, enabled = interactive,
                            onDateSelected = { if (interactive) onDateSelected(it) },
                            modifier = if (delta == 0) dragModifier else Modifier,
                        )
                    }
                },
                tasks = { delta ->
                    val date = DateWheelSelection(uiState.selectedDate, uiState.dayIntent)
                        .inMonth(uiState.visibleMonth.plusMonths(delta.toLong())).date
                    val preview = uiState.monthPreviews[date]
                    val state = if (delta == 0) {
                        if (landingDate == uiState.selectedDate && uiState.tasksDate != uiState.selectedDate)
                            uiState.copy(tasksDate = uiState.selectedDate, tasks = emptyList(), hasLoadedTasks = false)
                        else uiState
                    } else uiState.copy(
                        selectedDate = date, visibleMonth = YearMonth.from(date), tasksDate = date,
                        tasks = preview?.tasks.orEmpty(), hasLoadedTasks = preview?.tasks != null,
                        isLoading = preview == null, loadError = preview?.error, errorMessage = null,
                    )
                    key(state.visibleMonth) {
                        Box(Modifier.then(if (delta == 0) Modifier else Modifier.clearAndSetSemantics {})) {
                            if (delta == 0 || moving) TaskPanel(
                                uiState = state,
                                firstLoad = !uiState.hasLoadedTasks,
                                onAcceptNewTask = onAcceptNewTask,
                                onTaskCompletionToggled = onTaskCompletionToggled,
                                onTaskCompletionRequested = onTaskCompletionRequested,
                                completionTransitions = if (delta == 0) completionTransitions else null,
                                onTaskEditRequested = { id ->
                                    val task = state.tasks.firstOrNull { it.id == id }
                                    if (task?.repeatRuleId != null) repeatTask = task else singleTask = task
                                },
                                onTaskMove = onTaskMove, onPendingAction = onPendingAction, onRetryPending = onRetryPending,
                                onTaskAutoDefer = onTaskAutoDefer,
                                onMoveUndo = onMoveUndo,
                                onMakeUp = onMakeUp,
                                onLocate = onDateSelected,
                                onTaskSealRequested = { pendingSealTask = it },
                                onRetryTasks = onRetryTasks,
                                executionPlans = executionPlans, clock = clock,
                                acceptedTaskId = if (delta == 0) acceptedTaskId else null,
                                saveFeedback = if (delta == 0) saveFeedback else null,
                                enabled = delta == 0 && !moving,
                                animateDateChanges = landingDate != state.selectedDate,
                            )
                        }
                    }
                },
            )
            // Both managers belong to the page, never a moving month preview.
            DaybookManagementControls(
                uiState, !moving && !uiState.isLoading,
                onTaskCompletionToggled,
                onEditTask = { id ->
                    val task = uiState.tasks.firstOrNull { it.id == id }
                    if (task?.repeatRuleId != null) repeatTask = task else singleTask = task
                },
                onSeal = { pendingSealTask = it },
                onRepeatPause, onRepeatResume, onRepeatEdited, onRetryTasks, clock, executionPlans,
                completionTransitions.entries.keys.map { it.id }.toSet(),
            )
        }
        // design `.form-scroll` 与 `.nav` 之间的 10px 间隔：决定滚动可视区的截断位置。
        Spacer(Modifier.height(MomentMarkTokens.DaybookScrollToNavGap))
    }
    PixelDateWheel(
        visible = pickerVisible,
        selection = pickerSelection,
        yearRange = minOf(uiState.yearRange.first, pickerSelection.date.year)..maxOf(uiState.yearRange.last, pickerSelection.date.year),
        copy = DateWheelCopy(
            title = "▸ 选择日期",
            note = listOfNotNull(
                if (pickerSelection.date.dayOfMonth < pickerSelection.intendedDay)
                    "本月最多 ${pickerSelection.date.lengthOfMonth()} 日，切回长月会恢复 ${pickerSelection.intendedDay} 日。" else null,
                if (uiState.yearRange.first < 1900 || uiState.yearRange.last > 2100)
                    "年份已覆盖 ${uiState.yearRange.first}—${uiState.yearRange.last}。" else null,
            ).takeIf { it.isNotEmpty() }?.joinToString(" "),
            year = "年", month = "月", day = "日", today = "今天", cancel = "取消", confirm = "跳转",
        ),
        onYearSelected = { updateDraft(pickerSelection.withYear(it)) },
        onMonthSelected = { updateDraft(pickerSelection.withMonth(it)) },
        onDaySelected = { updateDraft(pickerSelection.withDay(it)) },
        onToday = { updateDraft(DateWheelSelection(uiState.today)) },
        onConfirm = { if (pickerVisible && !accepting) { accepting = true; closePicker(); onDateConfirmed(pickerSelection.date, pickerSelection.intendedDay) } },
        onDismiss = { closePicker() },
        onClosed = { titleFocus.requestFocus() },
    )
    singleTask?.let { task -> SingleTaskActions(task, uiState.today,
        { singleTask = null; onTaskEditRequested(task.id) },
        { onTaskMove(task, it); singleTask = null },
        { onTaskAutoDefer(task, it); singleTask = null },
        { pendingSealTask = task; singleTask = null }, { singleTask = null }) }
    repeatStop?.let { id -> PixelConfirmationDialog("停止今天及以后的重复任务？", "保留历史；恢复后从恢复日继续。", "停止重复任务", { if (repeatStop == id) { repeatStop = null; repeatTask = null; onRepeatPause(id, true) } }, { repeatStop = null }, "取消") }
    repeatRule?.let { rule -> RepeatRuleEditor(rule, requireNotNull(repeatRuleDate), onRepeatEdited, { repeatRule = null }, uiState.errorMessage) }
    repeatTask?.takeIf { repeatRule == null && repeatStop == null }?.let { openedTask ->
        val task = uiState.tasks.firstOrNull { it.id == openedTask.id } ?: openedTask
        val series = uiState.repeatSeries.firstOrNull { it.rule.id == task.repeatRuleId }
        val ruleToday = task.zoneId?.let { LocalDate.now(clock.withZone(it)) } ?: uiState.today
        val futureEditDate = maxOf(requireNotNull(task.repeatOccurrenceDate), ruleToday)
        RepeatInstanceActions(task, ruleToday, futureEditDate,
            onEdit = { repeatTask = null; onTaskEditRequested(task.id) },
            onFutureEdit = { series?.let { repeatRuleDate = futureEditDate; repeatRule = com.cch.momentmark.domain.model.TaskRepeatSchedule.ruleFor(it.versions, it.rule.id, requireNotNull(repeatRuleDate)) ?: it.rule } },
            onSkip = { onRepeatSkip(task, it); repeatTask = null },
            onPause = { if (it) repeatStop = task.repeatRuleId else { onRepeatPause(requireNotNull(task.repeatRuleId), false); repeatTask = null } },
            onMakeUp = { onMakeUp(task); repeatTask = null }, onSeal = { pendingSealTask = task; repeatTask = null }, onDismiss = { repeatTask = null }, canEditFuture = series != null)
    }
    pendingSealTask?.let { task ->
        PixelConfirmationDialog(
            title = "放弃「${task.title}」？",
            message = "任务描述与完成状态将被封存进回收站，30 天内可复活。",
            confirmLabel = "✕ 确认放弃",
            onConfirm = {
                if (pendingSealTask?.id == task.id) {
                    pendingSealTask = null
                    onTaskSealRequested(task)
                }
            },
            onDismiss = { pendingSealTask = null },
        )
    }
}

@Composable
private fun DaybookTitlePanel(
    month: YearMonth,
    swipe: Float,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onChooseDate: () -> Unit,
    onToday: () -> Unit,
    titleFocus: FocusRequester,
) {
    val large = LocalDensity.current.fontScale >= 1.5f
    PixelPanel(Modifier.fillMaxWidth().padding(
        start = MomentMarkTokens.DaybookTitlePanelMarginHorizontal,
        end = MomentMarkTokens.DaybookTitlePanelMarginHorizontal,
        bottom = MomentMarkTokens.DaybookTitlePanelMarginBottom,
    )) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(MomentMarkTokens.TouchTargetMin).heightIn(min = MomentMarkTokens.TouchTargetMin)
                .semantics { contentDescription = "上个月"; role = Role.Button }.pixelClickable(onClick = onPreviousMonth), contentAlignment = Alignment.Center) {
                Text("◀", fontSize = MomentMarkTokens.DaybookTitleArrowFontSize, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(Modifier.weight(1f).heightIn(min = MomentMarkTokens.TouchTargetMin)
                .testTag("daybook-month-title")
                .clipToBounds().focusRequester(titleFocus).focusable()
                .semantics { contentDescription = "${month.year}年${month.monthValue}月，选择日期"; role = Role.Button }
                .pixelClickable(onClick = onChooseDate), contentAlignment = Alignment.Center) {
                listOf(0, if (swipe < 0) 1 else -1).forEach { delta ->
                    val shown = month.plusMonths(delta.toLong())
                    val label = shown.month.getDisplayName(TextStyle.FULL, Locale.CHINA)
                    Text(if (large) "$label\n${shown.year}" else "$label ${shown.year} ▾",
                        modifier = Modifier.fillMaxWidth().graphicsLayer { translationX = (delta + swipe) * size.width }
                            .clearAndSetSemantics {},
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = MomentMarkTokens.DaybookTitleLabelFontSize, fontWeight = FontWeight.ExtraBold), textAlign = TextAlign.Center)
                }
            }
            Box(Modifier.width(MomentMarkTokens.TouchTargetMin).heightIn(min = MomentMarkTokens.TouchTargetMin)
                .semantics { contentDescription = "下个月"; role = Role.Button }.pixelClickable(onClick = onNextMonth), contentAlignment = Alignment.Center) {
                Text("▶", fontSize = MomentMarkTokens.DaybookTitleArrowFontSize, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(Modifier.width(MomentMarkTokens.TouchTargetMin).heightIn(min = MomentMarkTokens.TouchTargetMin)
                .semantics { contentDescription = "回到今天"; role = Role.Button }.pixelClickable(onClick = onToday), contentAlignment = Alignment.Center) {
                Text("今天", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge.copy(fontSize = MomentMarkTokens.DateWheelActionSize, fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    calendarMonths: Map<YearMonth, CalendarMonthState>,
    onRetry: () -> Unit,
    enabled: Boolean,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth().testTag("daybook-month-grid")) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        top = MomentMarkTokens.DaybookWeekdayHeaderPaddingTop,
                        bottom = MomentMarkTokens.DaybookWeekdayHeaderPaddingBottom,
                    )
                    .padding(bottom = MomentMarkTokens.DaybookWeekdayHeaderMarginBottom),
            ) {
                listOf("SU", "MO", "TU", "WE", "TH", "FR", "SA").forEach { weekday ->
                    Text(
                        text = weekday,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = MomentMarkTokens.DaybookWeekdayHeaderFontSize,
                            lineHeight = MomentMarkTokens.DaybookWeekdayHeaderLineHeight,
                            letterSpacing = MomentMarkTokens.MonoLetterSpacingNone,
                            platformStyle = MmNoFontPadding,
                        ),
                        color = LocalMmExtendedColors.current.labelTertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            val leading = month.atDay(1).dayOfWeek.sundayIndex()
            val rows = (leading + month.lengthOfMonth() + 6) / 7
            val previousMonth = month.minusMonths(1)
            repeat(rows) { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = if (row == 0) 0.dp else MomentMarkTokens.DaybookCalendarGap),
                    horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.DaybookCalendarGap),
                ) {
                    repeat(7) { column ->
                        val cellIndex = row * 7 + column
                        val day = cellIndex - leading + 1
                        val date = when {
                            day < 1 -> previousMonth.atDay(previousMonth.lengthOfMonth() + day)
                            day > month.lengthOfMonth() -> month.plusMonths(1).atDay(day - month.lengthOfMonth())
                            else -> month.atDay(day)
                        }
                        CalendarDayCell(
                            date = date,
                            inVisibleMonth = day in 1..month.lengthOfMonth(),
                            selected = date == selectedDate,
                            summary = calendarMonths[YearMonth.from(date)]?.days?.get(date)
                                ?: if (calendarMonths[YearMonth.from(date)]?.days != null) com.cch.momentmark.domain.model.CalendarDaySummary() else null,
                            today = date == today,
                            onClick = { onDateSelected(date) }, enabled = enabled,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            val calendar = calendarMonths[month]
            if (calendar?.error != null) {
                PixelButton(
                    label = "日历加载失败 · 重试", onClick = onRetry, enabled = enabled,
                    layout = com.cch.momentmark.ui.components.PixelButtonLayout.Compact,
                    modifier = Modifier.testTag("daybook-calendar-retry"),
                )
            }
        }
        if (calendarMonths[month]?.days == null && calendarMonths[month]?.error == null) {
            Text(
                "加载日历…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.TopEnd).background(MaterialTheme.colorScheme.surface).testTag("daybook-calendar-loading"),
            )
        }
    }
}

private data class TaskSnapshot(val date: LocalDate, val tasks: List<Task>)

@Composable
private fun TaskPanel(
    uiState: DaybookUiState,
    onAcceptNewTask: () -> Unit,
    onTaskCompletionToggled: (Task) -> Unit,
    onTaskEditRequested: (String) -> Unit,
    onTaskSealRequested: (Task) -> Unit,
    onRetryTasks: () -> Unit,
    enabled: Boolean = true,
    animateDateChanges: Boolean = true,
    firstLoad: Boolean = !uiState.hasLoadedTasks,
    onTaskMove: (Task, LocalDate) -> Unit = { _, _ -> },
    onPendingAction: ((Task, PendingTaskAction, LocalDate) -> Unit)? = null,
    onRetryPending: () -> Unit = {},
    onTaskAutoDefer: (Task, Boolean) -> Unit = { _, _ -> },
    onMoveUndo: () -> Unit = {},
    onMakeUp: (Task) -> Unit = {},
    onLocate: (LocalDate) -> Unit = {},
    executionPlans: Map<String, TaskExecutionPlan> = emptyMap(),
    clock: java.time.Clock = java.time.Clock.systemDefaultZone(),
    acceptedTaskId: String? = null,
    saveFeedback: String? = null,
    completionTransitions: DaybookCompletionTransitions? = null,
    onTaskCompletionRequested: ((Task, (Boolean) -> Unit) -> Unit)? = null,
) {
    // Only successful results replace content. Pending/error states retain its original date.
    val headingFocus = remember { FocusRequester() }
    val snapshot = if (uiState.hasLoadedTasks) TaskSnapshot(uiState.tasksDate, uiState.tasks) else null
    // This timer only controls feedback; results and task interaction are never delayed.
    val pendingDate = uiState.selectedDate.takeIf { enabled && uiState.isLoading && uiState.loadError == null && !firstLoad }
    var slowRequest by remember { mutableStateOf<Pair<LocalDate, Long>?>(null) }
    val pendingRequest = pendingDate?.let { it to uiState.taskRequestRevision }
    LaunchedEffect(pendingRequest) {
        slowRequest = null
        if (pendingRequest != null) {
            delay(300)
            slowRequest = pendingRequest
        }
    }
    PixelPanel(Modifier.fillMaxWidth().testTag("daybook-task-panel").padding(top = MomentMarkTokens.DaybookTaskPanelMarginTop)) {
        Column(Modifier.fillMaxWidth().padding(
            horizontal = MomentMarkTokens.DaybookTaskPanelPaddingHorizontal,
            vertical = MomentMarkTokens.DaybookTaskPanelPaddingVertical,
        )) {
            if (uiState.moveUndoAvailable) com.cch.momentmark.ui.components.PixelButton("已重新安排 · 撤销", onMoveUndo, Modifier.fillMaxWidth(), com.cch.momentmark.ui.components.PixelButtonVariant.Ghost)
            Box(Modifier.fillMaxWidth()) {
                AnimatedContent(
                    targetState = snapshot,
                    contentKey = { it?.date },
                    transitionSpec = {
                        val duration = if (animateDateChanges) MomentMarkTokens.MotionPageEnterMs else 0
                        val from = initialState?.date
                        val to = targetState?.date
                        if (from != null && to != null && from != to) {
                            val direction = if (to > from) 1 else -1
                            (slideInHorizontally(tween(duration)) { width -> direction * width } togetherWith
                                slideOutHorizontally(tween(duration)) { width -> -direction * width })
                                .using(SizeTransform { _, _ -> tween(duration) })
                        } else {
                            (fadeIn(tween(duration)) togetherWith fadeOut(tween(duration)))
                                .using(SizeTransform { _, _ -> tween(duration) })
                        }
                    },
                    label = "daybookTasks",
                ) { content ->
                    val interactive = enabled && content != null && content.date == uiState.selectedDate &&
                        !uiState.isLoading && uiState.loadError == null
                    Column(Modifier.fillMaxWidth().testTag("daybook-task-content")
                        .then(if (interactive) Modifier else Modifier.clearAndSetSemantics {
                            contentDescription = if (content == null) { if (firstLoad) "首次加载任务" else "${uiState.selectedDate}的任务加载中，暂不可操作" } else "${content.date}的任务预览，暂不可操作"
                        })) {
                        val date = content?.date ?: uiState.selectedDate
                        if (date == uiState.today) DaybookPendingControls(
                            uiState.pendingTasks.filter { it.dueLocalDate < uiState.today },
                            uiState.today, interactive,
                            onMove = { task, target ->
                                if (onPendingAction != null) onPendingAction(task,
                                    if (target == uiState.today) PendingTaskAction.TODAY else PendingTaskAction.TOMORROW, uiState.today)
                                else onTaskMove(task, target)
                            },
                            onComplete = { task ->
                                if (onPendingAction != null) onPendingAction(task, PendingTaskAction.COMPLETE, uiState.today)
                                else onTaskCompletionToggled(task)
                            }, busy = uiState.pendingBusy, errors = uiState.pendingErrors,
                            loadError = uiState.pendingLoadError, onRetry = onRetryPending, loading = !uiState.pendingLoaded,
                            onEmptyDismiss = { headingFocus.requestFocus() })
                        Box(Modifier.focusRequester(headingFocus).focusable()) {
                            DaybookTaskHeading(date, uiState.today, content?.tasks.orEmpty().map { task ->
                                completionTransitions?.entries?.get(CompletionKey(date, task.id))?.task ?: task
                            },
                                feedback = saveFeedback.takeIf { interactive })
                        }
                        when {
                            content == null -> when {
                                uiState.loadError != null -> Text("未能加载这一天的任务。",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                firstLoad -> Text("▸ 正在加载任务…",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                else -> Spacer(Modifier.height(MomentMarkTokens.SpaceUnit * 4))
                            }
                            content.tasks.isEmpty() -> Text("这一天还没有任务掉落。",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = MomentMarkTokens.SpaceUnit * 2))
                            else -> DaybookTaskList(content.date, content.tasks, interactive,
                                onTaskCompletionToggled, onTaskEditRequested, onTaskSealRequested, executionPlans, clock, onLocate, acceptedTaskId,
                                transitions = completionTransitions, requestCompletion = onTaskCompletionRequested,
                                completionBusy = uiState.completionBusy)
                        }
                    }
                }
                // Overlay the trailing corner, clear of the heading/date and the compact pending entry.
                if (pendingRequest != null && slowRequest == pendingRequest) CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.TopEnd).size(12.dp).testTag("daybook-slow-loading")
                        .clearAndSetSemantics { contentDescription = "$pendingDate 加载任务…" },
                    strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = .2f))
            }
            uiState.loadError?.let { error ->
                Text("⚠ ${uiState.selectedDate} 加载失败：$error",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
            if (uiState.loadError != null) Text("重试", style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.heightIn(min = MomentMarkTokens.TouchTargetMin)
                    .pixelClickable(enabled = enabled, role = Role.Button, onClick = onRetryTasks), color = MaterialTheme.colorScheme.primary)
            Box(Modifier.then(if (enabled) Modifier else Modifier.clearAndSetSemantics {})) {
                NewQuestEntry(onClick = onAcceptNewTask, enabled = enabled && !uiState.isLoading)
            }
            uiState.errorMessage?.let { Text("⚠ $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) }
        }
    }
}

private fun DayOfWeek.sundayIndex(): Int = if (this == DayOfWeek.SUNDAY) 0 else value
