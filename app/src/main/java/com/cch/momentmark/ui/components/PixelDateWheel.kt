package com.cch.momentmark.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MomentMarkTokens
import java.time.LocalDate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class DateWheelCopy(
    val title: String, val preview: String? = null, val description: String? = null, val note: String? = null,
    val year: String, val month: String, val day: String,
    val today: String, val cancel: String, val confirm: String,
)

/** Controlled presentation only. Dialog isolates touch/focus/back; no repository dependency. */
@Composable
fun PixelDateWheel(
    visible: Boolean,
    selection: DateWheelSelection,
    yearRange: IntRange,
    copy: DateWheelCopy,
    onYearSelected: (Int) -> Unit,
    onMonthSelected: (Int) -> Unit,
    onDaySelected: (Int) -> Unit,
    onToday: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onClosed: () -> Unit = {},
) {
    val transition = remember { MutableTransitionState(false) }
    var stopping by remember { mutableStateOf(false) }
    var wasOpen by remember { mutableStateOf(false) }
    LaunchedEffect(visible) { if (visible) { wasOpen = true; stopping = false }; transition.targetState = visible }
    val liveVisible by rememberUpdatedState(visible)
    fun dismiss() { if (liveVisible && !stopping) { stopping = true; onDismiss() } }
    fun confirm() { if (liveVisible && !stopping) { stopping = true; onConfirm() } }
    val closed by rememberUpdatedState(onClosed)
    LaunchedEffect(transition.isIdle, transition.currentState) {
        if (transition.isIdle && !transition.currentState && wasOpen) { wasOpen = false; closed() }
    }
    if (!transition.currentState && !transition.targetState) return
    // Dialog creates another window; retain the caller's PixelCanvas density explicitly.
    val canvasDensity = LocalDensity.current
    val scale = canvasDensity.fontScale
    val rows = if (scale >= 1.5f) 3 else 5
    val rowHeight = if (scale >= 1.5f) MomentMarkTokens.DateWheelLargeRowHeight else MomentMarkTokens.DateWheelRowHeight
    val busy = remember { mutableStateMapOf<String, Boolean>() }
    val alpha by animateFloatAsState(if (visible) .5f else 0f, tween(MomentMarkTokens.MotionPageEnterMs), label = "date-wheel-scrim")
    Dialog(
        onDismissRequest = { dismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnClickOutside = false),
    ) {
        CompositionLocalProvider(LocalDensity provides canvasDensity) {
        val dialogWindow = (LocalView.current.parent as DialogWindowProvider).window
        SideEffect { dialogWindow.setDimAmount(0f); dialogWindow.setWindowAnimations(0) }
        val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.outline.copy(alpha = alpha))
                .semantics { contentDescription = copy.cancel }
                .clickable(interactionSource = null, indication = null) { dismiss() })
            AnimatedVisibility(
                visibleState = transition,
                enter = slideInVertically(tween(MomentMarkTokens.DateWheelMotionMs)) { it },
                exit = slideOutVertically(tween(MomentMarkTokens.DateWheelMotionMs)) { it },
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                PixelPanel(Modifier.fillMaxWidth().padding(horizontal = MomentMarkTokens.DateWheelSheetMargin)
                    .heightIn(max = (maxHeight - topInset - MomentMarkTokens.SpaceInner).coerceAtLeast(0.dp))
                    .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
                    .semantics { paneTitle = copy.title }) {
                    Column {
                        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                            Row(Modifier.fillMaxWidth().padding(start = MomentMarkTokens.DateWheelContentPadding, end = MomentMarkTokens.DateWheelContentPadding, top = 12.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(copy.title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium.copy(fontSize = MomentMarkTokens.DateWheelTitleSize, fontWeight = FontWeight.ExtraBold), modifier = Modifier.weight(1f))
                                WheelAction(copy.today, { if (liveVisible && !stopping) onToday() }, Modifier.width(if (scale >= 1.5f) 72.dp else 52.dp), primary = false, compact = true, enabled = visible && !stopping)
                            }
                            Box(Modifier.fillMaxWidth().height(MomentMarkTokens.PxThinBorderWidth).background(MaterialTheme.colorScheme.outline))
                            if (copy.preview != null || copy.description != null) Column(Modifier.fillMaxWidth().padding(start = MomentMarkTokens.DateWheelContentPadding, end = MomentMarkTokens.DateWheelContentPadding, top = 10.dp, bottom = 6.dp)
                                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
                                copy.preview?.let { Text(it, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge.copy(fontFamily = MaterialTheme.typography.labelMedium.fontFamily, letterSpacing = MomentMarkTokens.MonoLetterSpacingNone, fontSize = MomentMarkTokens.DateWheelPreviewSize, fontWeight = FontWeight.ExtraBold)) }
                                copy.description?.let { Text(it, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 14.4.sp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                            Row(Modifier.fillMaxWidth().padding(start = MomentMarkTokens.DateWheelContentPadding, end = MomentMarkTokens.DateWheelContentPadding, top = 6.dp), horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.DateWheelColumnGap)) {
                                listOf(copy.year to 1.3f, copy.month to 1f, copy.day to 1f).forEach { (label, weight) ->
                                    Text(label, Modifier.weight(weight).padding(bottom = 6.dp), textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Box(Modifier.fillMaxWidth().padding(horizontal = MomentMarkTokens.DateWheelContentPadding).background(LocalMmExtendedColors.current.inputSurface)) {
                                Box(Modifier.fillMaxWidth().padding(top = rowHeight * (rows / 2)).height(rowHeight)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(MomentMarkTokens.PxThinBorderWidth, MaterialTheme.colorScheme.tertiary))
                                Row(Modifier.fillMaxWidth().border(MomentMarkTokens.PxThinBorderWidth, MaterialTheme.colorScheme.outline), horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.DateWheelColumnGap)) {
                                    WheelColumn(copy.year, selection.date.year, yearRange, rows, rowHeight, { if (liveVisible && !stopping) onYearSelected(it) }, { busy["year"] = it }, Modifier.weight(1.3f), true, visible && !stopping)
                                    WheelColumn(copy.month, selection.date.monthValue, 1..12, rows, rowHeight, { if (liveVisible && !stopping) onMonthSelected(it) }, { busy["month"] = it }, Modifier.weight(1f), enabled = visible && !stopping)
                                    WheelColumn(copy.day, selection.date.dayOfMonth, 1..selection.date.lengthOfMonth(), rows, rowHeight, { if (liveVisible && !stopping) onDaySelected(it) }, { busy["day"] = it }, Modifier.weight(1f), enabled = visible && !stopping)
                                }
                            }
                            copy.note?.let { Text(it, Modifier.padding(horizontal = MomentMarkTokens.DateWheelContentPadding, vertical = 8.dp)
                                .semantics { liveRegion = LiveRegionMode.Polite },
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = MomentMarkTokens.DateWheelContentPadding, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            WheelAction(copy.cancel, { dismiss() }, Modifier.weight(1f), false, enabled = visible && !stopping, icon = PixelButtonIcon.Close)
                            WheelAction(copy.confirm, { confirm() }, Modifier.weight(1.5f), true, visible && !stopping && busy.values.none { it }, icon = PixelButtonIcon.Check)
                        }
                        Spacer(Modifier.fillMaxWidth().navigationBarsPadding())
                    }
                }
            }
        }
        }
    }
}

@Composable
internal fun WheelAction(label: String, action: () -> Unit, modifier: Modifier, primary: Boolean, enabled: Boolean = true, compact: Boolean = false, icon: PixelButtonIcon? = null) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pointerFeedback = rememberPixelPressFeedback(enabled)
    val activePress = (pointerFeedback.pressed || pressed) && enabled
    val shift = if (activePress && !compact) MomentMarkTokens.PxShadowOffset else 0.dp
    val background = if (!enabled || compact) colors.surfaceVariant else if (primary) colors.tertiary else colors.surface
    val foreground = if (!enabled) colors.onSurfaceVariant else if (primary) colors.onTertiary else colors.onSurface
    // Keep the input box fixed while only the panel body/shadow moves.
    Box(modifier.semantics { role = Role.Button }
        .pixelPressFeedback(pointerFeedback, enabled)
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = action)) {
        PixelPanel(Modifier.offset { val pixels = shift.roundToPx(); IntOffset(pixels, pixels) },
            backgroundColor = if (activePress) foreground else background,
            variant = if (compact) PixelPanelVariant.Flat else PixelPanelVariant.Raised,
            borderWidth = if (compact) MomentMarkTokens.PxThinBorderWidth else MomentMarkTokens.PxBorderWidth,
            shadowOffset = MomentMarkTokens.PxShadowOffset - shift,
        ) {
            Box(Modifier.fillMaxWidth().heightIn(min = if (compact) MomentMarkTokens.TouchTargetMin else 48.dp), contentAlignment = Alignment.Center) {
                PixelButtonLabel(label, icon, if (activePress) background else foreground,
                    MaterialTheme.typography.labelLarge.copy(fontSize = MomentMarkTokens.DateWheelActionSize, lineHeight = 18.4.sp, fontWeight = FontWeight.ExtraBold),
                    PaddingValues(horizontal = 4.dp, vertical = 12.dp))
            }
        }
    }
}

@Composable
internal fun WheelColumn(
    label: String, value: Int, range: IntRange, rows: Int, rowHeight: androidx.compose.ui.unit.Dp,
    onSelected: (Int) -> Unit, onBusy: (Boolean) -> Unit, modifier: Modifier, isYear: Boolean = false, enabled: Boolean = true,
    valueLabel: (Int) -> String = { if (isYear) it.toString() else "%02d".format(it) },
    semanticValue: (Int) -> String = { it.toString() },
) {
    val count = range.last - range.first + 1
    val padding = rows / 2
    val state = rememberLazyListState(value - range.first)
    val selected by rememberUpdatedState(onSelected)
    val reportBusy by rememberUpdatedState(onBusy)
    var syncing by remember { mutableStateOf(false) }
    var firstSync by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    if (isYear) LaunchedEffect(Unit) { focus.requestFocus() }
    val rowPixels = with(LocalDensity.current) { rowHeight.toPx() }
    LaunchedEffect(enabled) { if (!enabled) { state.stopScroll(); reportBusy(false) } }
    LaunchedEffect(value, range, rows, rowHeight) {
        syncing = true
        reportBusy(true)
        try {
            if (firstSync) state.scrollToItem(value - range.first) else state.animateScrollToItem(value - range.first)
            firstSync = false
        } finally { syncing = false; reportBusy(false) }
    }
    LaunchedEffect(state, range, rowPixels) {
        var userScroll = false
        snapshotFlow { state.isScrollInProgress to syncing }.distinctUntilChanged().collect { (scrolling, programmatic) ->
            reportBusy(scrolling || programmatic)
            if (scrolling && !programmatic) userScroll = true
            if (!scrolling && !programmatic && userScroll) {
                userScroll = false
                val index = (state.firstVisibleItemIndex + (state.firstVisibleItemScrollOffset / rowPixels).roundToInt()).coerceIn(0, count - 1)
                selected(range.first + index)
            }
        }
    }
    DisposableEffect(Unit) { onDispose { reportBusy(false) } }
    fun pick(target: Int): Boolean { if (!enabled) return false; selected(target.coerceIn(range)); return true }
    LazyColumn(
        state = state,
        userScrollEnabled = enabled,
        flingBehavior = rememberSnapFlingBehavior(state, snapPosition = SnapPosition.Start),
        modifier = modifier.height(rowHeight * rows).focusRequester(focus).onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) false else when (event.key) {
                Key.DirectionUp -> pick(value - 1)
                Key.DirectionDown -> pick(value + 1)
                Key.PageUp -> pick(value - 10)
                Key.PageDown -> pick(value + 10)
                Key.MoveHome -> pick(range.first)
                Key.MoveEnd -> pick(range.last)
                else -> false
            }
        }.focusable().semantics {
            contentDescription = "$label，当前${semanticValue(value)}"
            stateDescription = "${semanticValue(range.first)} 至 ${semanticValue(range.last)}"
            customActions = listOf(CustomAccessibilityAction("增加$label") { pick(value + 1) }, CustomAccessibilityAction("减少$label") { pick(value - 1) })
            setProgress { pick(it.roundToInt()) }
            progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), range.first.toFloat()..range.last.toFloat(), (count - 2).coerceAtLeast(0))
        },
    ) {
        items(count + padding * 2) { index ->
            val actual = range.first + index - padding
            Box(Modifier.fillMaxWidth().height(rowHeight)
                .then(if (actual in range) Modifier.pixelClickable(enabled = enabled) { pick(actual) } else Modifier)
                .clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
                if (actual in range) Text(valueLabel(actual),
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = MaterialTheme.typography.labelMedium.fontFamily, letterSpacing = MomentMarkTokens.MonoLetterSpacingNone, fontSize = if (actual == value) MomentMarkTokens.DateWheelValueSize else MomentMarkTokens.DateWheelNeighborSize, lineHeight = if (actual == value) 28.sp else 24.5.sp, fontWeight = if (actual == value) FontWeight.ExtraBold else FontWeight.Bold),
                    color = if (actual == value) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clearAndSetSemantics {}, maxLines = 1)
            }
        }
    }
}
