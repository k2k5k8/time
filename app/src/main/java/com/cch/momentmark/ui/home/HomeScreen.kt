package com.cch.momentmark.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material3.DrawerValue
import com.cch.momentmark.ui.components.DeleteConfirmationDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.cch.momentmark.data.SampleEvents
import com.cch.momentmark.data.local.MomentMarkDatabase
import com.cch.momentmark.data.repository.TimeEventMapper.toDomain
import com.cch.momentmark.data.repository.TimeEventRepository
import com.cch.momentmark.data.settings.MomentMarkSettingsStore
import com.cch.momentmark.data.settings.MomentMarkGroupStore
import com.cch.momentmark.data.settings.EventDetailStore
import com.cch.momentmark.domain.model.EventCardPaletteKey
import com.cch.momentmark.domain.model.EventCardTemplateKey
import com.cch.momentmark.domain.model.EventColorRole
import com.cch.momentmark.domain.model.TravelCardConfig
import com.cch.momentmark.domain.model.TravelBackgroundPreset
import com.cch.momentmark.domain.model.TravelCardIcon
import com.cch.momentmark.domain.model.TravelCardSize
import com.cch.momentmark.domain.model.TimeEvent
import com.cch.momentmark.domain.model.RelatedCountdown
import com.cch.momentmark.domain.model.TimeCardFields
import com.cch.momentmark.domain.model.cardFields
import com.cch.momentmark.ui.components.rememberTimeCardPresentation
import com.cch.momentmark.ui.components.EventCardFeature
import com.cch.momentmark.ui.components.TimelineDestination
import com.cch.momentmark.ui.components.TimelineNavigation
import com.cch.momentmark.ui.eventsettings.eventSizeLabel
import com.cch.momentmark.ui.eventsettings.eventTemplateLabel
import com.cch.momentmark.ui.eventsettings.EventSettingsFeature
import com.cch.momentmark.ui.eventsettings.EventCreateFeature
import com.cch.momentmark.ui.home.filterEventsByTitle
import com.cch.momentmark.ui.home.HomeFeature
import com.cch.momentmark.ui.settings.themeModeLabel
import com.cch.momentmark.ui.settings.SettingsFeature
import com.cch.momentmark.ui.eventdetail.EventDetailFeature
import com.cch.momentmark.ui.eventdetail.RelatedCountdownEditorFeature
import com.cch.momentmark.ui.daybook.DaybookFeature
import com.cch.momentmark.ui.theme.ThemeMode
import com.cch.momentmark.ui.theme.MomentMarkTheme
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.cch.momentmark.ui.home.CollapsibleHeroBackground
import com.cch.momentmark.ui.home.CollapsibleHomeTopBar
import com.cch.momentmark.ui.home.HomeHeroScenes
import com.cch.momentmark.ui.home.AdaptiveBackgroundPalette
import com.cch.momentmark.ui.home.AdaptiveBackgroundPaletteAnalyzer
import com.cch.momentmark.ui.home.AdaptiveCardSurface
import com.cch.momentmark.ui.home.homeHeroCollapseProgress
import com.cch.momentmark.ui.home.CardLayoutStorage
import com.cch.momentmark.ui.home.HomeCardLayout
import com.cch.momentmark.ui.home.cardGridWidth
import com.cch.momentmark.ui.home.defaultCardLayout
import com.cch.momentmark.ui.home.reorderedCardLayouts
import com.cch.momentmark.ui.home.edit.BoardDotBackground
import com.cch.momentmark.ui.home.edit.BoardUndoStack
import com.cch.momentmark.ui.home.edit.DragMotionTracker
import com.cch.momentmark.ui.home.edit.DragSwapGovernor
import com.cch.momentmark.ui.home.edit.GhostCardSlot
import android.view.HapticFeedbackConstants
import com.cch.momentmark.ui.EventFilter
import com.cch.momentmark.ui.filterEventsByScope
private const val HomeGridColumns = 2

/** 拖拽浮起时的放大增量（设计方案 §6.3：移动过程中不再缩放，只在拿/放时刻变化）。 */
private const val CardLiftScaleDelta = 0.055f

/**
 * 大卡（跨两列）的最大宽度。宽屏/平板上大卡不会被拉得过宽，
 * 手机上依旧自然占满整行。
 */
private val WideCardMaxWidth = 400.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    events: List<TimeEvent>,
    selectedFilter: EventFilter,
    selectedGroup: String?,
    templateOverrides: Map<String, EventCardTemplateKey>,
    travelConfigOverrides: Map<String, TravelCardConfig>,
    isLayoutEditing: Boolean,
    onLayoutEditingChange: (Boolean) -> Unit,
    onOpenGroups: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCreateEvent: () -> Unit,
    onOpenDaybook: () -> Unit,
    onOpenEventSettings: (TimeEvent) -> Unit,
) {
    var isSearchVisible by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val boardStorage = remember(context) { CardLayoutStorage(context) }
    val savedBoardLayouts by boardStorage.loadCardLayout.collectAsState(initial = emptyMap())
    var workingBoardLayouts by remember { mutableStateOf<Map<String, HomeCardLayout>>(emptyMap()) }
    var draggedCardId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dropTargetId by remember { mutableStateOf<String?>(null) }
    // Snapshot of the dragged card's bounds at drag start, used for both
    // layout-shift compensation and Y clamping so the card never enters the
    // top-bar / bottom-nav regions.
    var dragStartBounds by remember { mutableStateOf(Rect.Zero) }
    // Bounds are imperative drag hit-test data, not UI state. Keeping them out
    // of Compose snapshot state avoids a recomposition for every card layout
    // change while the grid scrolls.
    val cardBounds = remember { mutableMapOf<String, Rect>() }
    val boardScope = rememberCoroutineScope()
    // Swap decisions (core-rect hit + debounce + hysteresis) live in a plain
    // object so the gesture layer stays free of Compose state machinery.
    val swapGovernor = remember { DragSwapGovernor() }
    val motionTracker = remember { DragMotionTracker() }
    val undoStack = remember { BoardUndoStack<Map<String, HomeCardLayout>>() }
    var canUndoLayout by remember { mutableStateOf(false) }
    var canRedoLayout by remember { mutableStateOf(false) }
    // 1f while the card tracks the finger; animates to 0f on release so the
    // card glides into its (possibly still moving) slot instead of jumping.
    val settleProgress = remember { Animatable(1f) }
    var isSettling by remember { mutableStateOf(false) }
    var dragPointerId by remember { mutableStateOf<PointerId?>(null) }
    var dragUndoRecorded by remember { mutableStateOf(false) }
    var dragUndoSnapshot by remember { mutableStateOf<Map<String, HomeCardLayout>?>(null) }
    val hapticView = LocalView.current
    LaunchedEffect(savedBoardLayouts) {
        if (!isLayoutEditing) workingBoardLayouts = savedBoardLayouts
    }
    BackHandler(enabled = isSearchVisible) {
        isSearchVisible = false
        searchQuery = ""
    }
    LaunchedEffect(isSearchVisible) {
        if (isSearchVisible) searchFocusRequester.requestFocus()
    }
    // Keep the feed state outside the grid content so recomposition from the
    // toolbar, filters, or countdown cards does not reset the user's position.
    val homeGridState = rememberLazyGridState()
    val homeOverscrollEffect = rememberOverscrollEffect()
    // HomeScreen is removed from composition while another app screen is open,
    // so a plain remember gives each return to Home a fresh scene while keeping
    // the image stable during ordinary recomposition.
    val heroScene = remember { HomeHeroScenes.random() }
    val heroPalette = remember(context, heroScene.imageRes) {
        AdaptiveBackgroundPaletteAnalyzer.analyze(context, heroScene.imageRes)
    }
    // Template gallery items are previews, not user-owned date cards. Keep
    // them out of the home CRUD feed so every visible card has a real Room id
    // and can consistently enter detail/edit/delete flows.
    // Scroll progress changes every frame. Keep filtering, copying and sorting
    // outside that hot path so the visible card list stays referentially stable
    // while the finger is moving.
    val displayEvents = remember(events, selectedFilter, selectedGroup, templateOverrides, travelConfigOverrides) {
        filterEventsByScope(
            events = events,
            selectedFilter = selectedFilter,
            selectedGroup = selectedGroup,
        ).map { event ->
            val config = travelConfigOverrides[event.id] ?: event.travelCardConfig
            event.copy(
                cardTemplateKey = templateOverrides[event.id] ?: event.cardTemplateKey,
                travelCardConfig = config,
                localDate = if (config != null && event.timeType == com.cch.momentmark.domain.model.EventTimeType.ALL_DAY) {
                    config.targetDate
                } else {
                    event.localDate
                },
            )
        }
    }
    val visibleEvents = remember(displayEvents, searchQuery) {
        filterEventsByTitle(displayEvents, searchQuery)
    }
    val orderedVisibleEvents = remember(visibleEvents, workingBoardLayouts, displayEvents) {
        val fallbackOrder = displayEvents.mapIndexed { index, event -> event.id to index }.toMap()
        visibleEvents.sortedWith(
            compareBy<TimeEvent> { workingBoardLayouts[it.id]?.order ?: fallbackOrder.getValue(it.id) }
                .thenBy { it.id },
        )
    }
    fun persistBoardLayout() {
        val layouts = displayEvents.mapIndexed { index, event ->
            val saved = workingBoardLayouts[event.id] ?: defaultCardLayout(event, index)
            // Keep the stored width in sync with the renderer's current size so
            // saved boards never carry a stale slot width.
            val width = cardGridWidth(event)
            if (saved.gridWidth != width) saved.copy(gridWidth = width) else saved
        }
        boardScope.launch { boardStorage.saveCardLayout(layouts) }
    }
    // A size change made outside layout editing (event settings / size chips)
    // must reach the board state immediately. Sync the stored slot width so
    // both the in-memory board and DataStore stay aligned with the renderer.
    LaunchedEffect(visibleEvents, savedBoardLayouts) {
        if (isLayoutEditing) return@LaunchedEffect
        val widthById = visibleEvents.associate { it.id to cardGridWidth(it) }
        val stale = savedBoardLayouts.filterValues { layout ->
            widthById[layout.cardId]?.let { it != layout.gridWidth } == true
        }
        if (stale.isEmpty()) return@LaunchedEffect
        workingBoardLayouts = workingBoardLayouts.mapValues { (id, layout) ->
            val width = widthById[id] ?: return@mapValues layout
            if (width != layout.gridWidth) layout.copy(gridWidth = width) else layout
        }
        persistBoardLayout()
    }
    fun refreshUndoFlags() {
        canUndoLayout = undoStack.canUndo
        canRedoLayout = undoStack.canRedo
    }

    /** Commits one swap into [workingBoardLayouts]; records undo once per drag session. */
    fun applyReorder(draggedId: String, targetId: String) {
        val currentLayouts = orderedVisibleEvents.mapIndexed { index, visibleEvent ->
            workingBoardLayouts[visibleEvent.id] ?: defaultCardLayout(visibleEvent, index)
        }
        val reordered = reorderedCardLayouts(currentLayouts, draggedId, targetId)
        if (reordered == currentLayouts) return
        if (!dragUndoRecorded) {
            dragUndoSnapshot?.let { snapshot ->
                undoStack.record(snapshot)
                refreshUndoFlags()
            }
            dragUndoRecorded = true
        }
        workingBoardLayouts = workingBoardLayouts + reordered.associateBy { it.cardId }
    }

    /** Runs the debounced swap decision for the card's current visual center. */
    fun attemptReorder(dragCenter: Offset, nowMs: Long) {
        val draggedId = draggedCardId ?: return
        dropTargetId = swapGovernor.candidateFor(dragCenter, cardBounds, draggedId)
        val targetId = swapGovernor.targetFor(dragCenter, cardBounds, draggedId, nowMs)
        if (targetId != null) applyReorder(draggedId, targetId)
    }

    /**
     * Release: the card animates from wherever it was dropped onto its slot.
     * The slot itself may still be travelling via animateItem, so the
     * translation is scaled by [settleProgress] instead of targeting a fixed
     * end position.
     */
    fun settleDraggedCard(cardId: String) {
        if (draggedCardId != cardId) return
        dropTargetId = null
        isSettling = true
        boardScope.launch {
            settleProgress.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 480f),
            )
            if (draggedCardId == cardId) {
                draggedCardId = null
                dragOffset = Offset.Zero
                dragStartBounds = Rect.Zero
                dragPointerId = null
                isSettling = false
                settleProgress.snapTo(1f)
            }
        }
    }

    fun undoLayout() {
        val previous = undoStack.undo(workingBoardLayouts) ?: return
        workingBoardLayouts = previous
        refreshUndoFlags()
        persistBoardLayout()
    }

    fun redoLayout() {
        val next = undoStack.redo(workingBoardLayouts) ?: return
        workingBoardLayouts = next
        refreshUndoFlags()
        persistBoardLayout()
    }

    fun finishLayoutEditing() {
        draggedCardId = null
        dragOffset = Offset.Zero
        dragStartBounds = Rect.Zero
        dropTargetId = null
        dragPointerId = null
        isSettling = false
        boardScope.launch { settleProgress.snapTo(1f) }
        onLayoutEditingChange(false)
        persistBoardLayout()
    }
    BackHandler(enabled = isLayoutEditing) { finishLayoutEditing() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(heroPalette.uiBaseColor),
    ) {
            // The feed is full-height; the navigator overlays it as a small
            // rounded pill instead of reserving a full-width footer region.
            val contentHeight = maxHeight
            val heroHeight = (contentHeight * 0.36f).coerceIn(280.dp, 360.dp)
            // Preserve the original top-bar-to-grid breathing room after the
            // hero has collapsed; the cards themselves remain untouched.
            val collapsedHeaderHeight = 104.dp
            val heroOverlap = 20.dp
            val collapseDistancePx = with(LocalDensity.current) {
                (heroHeight - collapsedHeaderHeight).toPx()
            }
            val cardDragLiftPx = with(LocalDensity.current) { 6.dp.toPx() }
            val cardDragShadowPx = with(LocalDensity.current) { 25.dp.toPx() }
            val cardEditingShadowPx = with(LocalDensity.current) { 11.dp.toPx() }
            // Keep dragged cards out of the top-bar and bottom-nav zones.
            val contentHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
            val topGuardPx = with(LocalDensity.current) { (collapsedHeaderHeight + 24.dp).toPx() }
            val bottomGuardPx = with(LocalDensity.current) { 200.dp.toPx() }
            // 拖到守卫区附近时开始自动滚动的感应带宽（设计方案 §7.1）。
            val edgeZonePx = with(LocalDensity.current) { 80.dp.toPx() }
            // 进入编辑态时整板轻微缩放，配合点阵渐显形成「进入另一个模式」的体感。
            val boardEditZoom by animateFloatAsState(
                targetValue = if (isLayoutEditing) 0.985f else 1f,
                animationSpec = tween(durationMillis = 260),
                label = "board-edit-zoom",
            )
            val collapseProgress by remember(collapseDistancePx) {
                derivedStateOf {
                    homeHeroCollapseProgress(
                        firstVisibleItemIndex = homeGridState.firstVisibleItemIndex,
                        firstVisibleItemScrollOffset = homeGridState.firstVisibleItemScrollOffset,
                        collapseDistancePx = collapseDistancePx,
                    )
                }
            }

            CollapsibleHeroBackground(
                scene = heroScene,
                palette = heroPalette,
                collapseProgress = collapseProgress,
                heroContentHeight = heroHeight,
                modifier = Modifier
                    // Let the adaptive haze continue behind the full feed.
                    // Ending it at a fixed Hero height exposes a horizontal
                    // image edge between grid rows.
                    .fillMaxSize(),
            )

            // A quiet veil plus the dot lattice sits behind the editable wall.
            // The dots mark the snap units (two per grid column); both soften
            // the photographic scene without covering the cards themselves.
            // AnimatedVisibility owns the cross-fade so the exit also fades.
            AnimatedVisibility(
                visible = isLayoutEditing,
                enter = fadeIn(animationSpec = tween(220)),
                exit = fadeOut(animationSpec = tween(180)),
            ) {
                BoardDotBackground(
                    columns = HomeGridColumns,
                    horizontalPadding = 20.dp,
                    cardSpacing = 12.dp,
                    dotColor = heroPalette.cardContentColor,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(heroPalette.uiBaseColor.copy(alpha = .16f)),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                if (orderedVisibleEvents.isEmpty()) {
                    EmptyState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = heroHeight - heroOverlap),
                        searchQuery = searchQuery,
                        onClearSearch = { searchQuery = "" },
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(HomeGridColumns),
                        state = homeGridState,
                        modifier = Modifier
                            .fillMaxSize()
                            // 编辑态整板缩放只在图形层发生，浏览时不产生额外节点。
                            .graphicsLayer {
                                scaleX = boardEditZoom
                                scaleY = boardEditZoom
                            }
                            .padding(horizontal = 20.dp),
                        contentPadding = PaddingValues(
                            top = heroHeight - heroOverlap,
                            // Leave a true resting area below the cards so
                            // the floating navigator never becomes their hard
                            // visual endpoint.
                            bottom = 188.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        userScrollEnabled = !isLayoutEditing,
                        overscrollEffect = homeOverscrollEffect,
                    ) {
                        items(
                            items = orderedVisibleEvents,
                            key = { it.id },
                            span = { event ->
                                // The renderer's current size drives the slot so a size
                                // change reflows the grid in real time; a stored layout
                                // may still carry the previous width.
                                if (cardGridWidth(event) == HomeGridColumns) {
                                    androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan)
                                } else {
                                    androidx.compose.foundation.lazy.grid.GridItemSpan(1)
                                }
                            },
                            // Cards contain Canvas-based artwork. Reusing the same
                            // composition type while flinging avoids unnecessary
                            // measure/layout work as rows enter the viewport.
                            contentType = { it.cardTemplateKey },
                        ) { event ->
                            val isWideCard = cardGridWidth(event) == HomeGridColumns
                            val isDragged = draggedCardId == event.id
                            val isDropTarget = dropTargetId == event.id && !isDragged
                            // 编辑态的轻微交替倾斜；拖拽中的倾斜量在图形层按
                            // dragOffset 直接计算，避免每帧重组。
                            val editTilt by animateFloatAsState(
                                targetValue = when {
                                    isDragged -> 0f
                                    isLayoutEditing -> if (orderedVisibleEvents.indexOf(event) % 2 == 0) .35f else -.35f
                                    else -> 0f
                                },
                                animationSpec = spring(dampingRatio = .82f, stiffness = 430f),
                                label = "card-board-tilt",
                            )
                            // 大卡跨满两列但限制最大宽度：宽屏设备上居中展示不至于过宽，
                            // 手机上仍自然填满整行。交互与定位 modifier 挂在外层 Box 上，
                            // 与原先 Surface 直接作为网格子项时占据完全相同的几何槽位，
                            // 保证拖拽命中测试的坐标系不变。拖拽变换移到内层 Box，
                            // 让外层槽位可以独立渲染虚线占位符（Ghost）。
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    // Placement animation and the transform layer are useful
                                    // only while arranging the board. Keeping them off normal
                                    // scrolling avoids a second render node and per-item
                                    // placement bookkeeping on every visible card.
                                    .then(
                                        if (isLayoutEditing) {
                                            Modifier.animateItem(
                                                placementSpec = spring(
                                                    dampingRatio = .78f,
                                                    stiffness = 410f,
                                                ),
                                            )
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .onGloballyPositioned { coordinates ->
                                        cardBounds[event.id] = coordinates.boundsInParent()
                                    }
                                    .pointerInput(event.id) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    onLayoutEditingChange(true)
                                                    draggedCardId = event.id
                                                    dragOffset = Offset.Zero
                                                    dragStartBounds = cardBounds[event.id] ?: Rect.Zero
                                                    dropTargetId = null
                                                    dragPointerId = null
                                                    dragUndoRecorded = false
                                                    dragUndoSnapshot = workingBoardLayouts
                                                    isSettling = false
                                                    swapGovernor.begin()
                                                    motionTracker.begin()
                                                    boardScope.launch { settleProgress.snapTo(1f) }
                                                    hapticView.performHapticFeedback(
                                                        HapticFeedbackConstants.LONG_PRESS,
                                                    )
                                                },
                                                onDragCancel = {
                                                    if (draggedCardId == event.id) {
                                                        settleDraggedCard(event.id)
                                                    }
                                                },
                                                onDragEnd = {
                                                    if (draggedCardId == event.id) {
                                                        // 高速甩动：用速度外推预测落点并做最后一次交换，
                                                        // 卡片落在手指去的方向而不是停下的位置。
                                                        if (motionTracker.isFling() && dragStartBounds != Rect.Zero) {
                                                            val predictedCenter = dragStartBounds.center +
                                                                dragOffset + motionTracker.velocity() * 120f
                                                            val targetId = cardBounds.entries.firstOrNull { (id, bounds) ->
                                                                id != event.id && bounds.contains(predictedCenter)
                                                            }?.key
                                                            if (targetId != null) applyReorder(event.id, targetId)
                                                        }
                                                        settleDraggedCard(event.id)
                                                    }
                                                },
                                            ) { change, amount ->
                                                if (draggedCardId != event.id) return@detectDragGesturesAfterLongPress
                                                // 多指触控：只跟随抓取卡片的第一根手指。
                                                val pointer = dragPointerId
                                                if (pointer == null) {
                                                    dragPointerId = change.id
                                                } else if (pointer != change.id) {
                                                    return@detectDragGesturesAfterLongPress
                                                }
                                                change.consume()
                                                // Clamp Y so the card can't enter the top-bar or
                                                // bottom-nav regions.
                                                val minY = if (dragStartBounds != Rect.Zero) {
                                                    topGuardPx - dragStartBounds.top
                                                } else {
                                                    Float.NEGATIVE_INFINITY
                                                }
                                                val maxY = if (dragStartBounds != Rect.Zero) {
                                                    contentHeightPx - bottomGuardPx - dragStartBounds.bottom
                                                } else {
                                                    Float.POSITIVE_INFINITY
                                                }
                                                val newY = (dragOffset.y + amount.y).coerceIn(minY, maxY)
                                                dragOffset = Offset(dragOffset.x + amount.x, newY)
                                                val nowMs = System.currentTimeMillis()
                                                motionTracker.addSample(nowMs, dragOffset)
                                                // Hit-test using the card's visual center (initial
                                                // position + drag offset), independent of layout
                                                // shifts from reordering.
                                                dragStartBounds.takeIf { it != Rect.Zero }?.center?.plus(dragOffset)?.let { pointerCenter ->
                                                    attemptReorder(pointerCenter, nowMs)
                                                }
                                            }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                // 占位符（Ghost）：留在当前槽位的虚线轮廓。槽位随每次预测
                                // 重排实时移动，因此它标记的就是「现在松手会落下的位置」。
                                if (isLayoutEditing && isDragged) {
                                    GhostCardSlot(
                                        alpha = settleProgress.value,
                                        color = heroPalette.cardHighlightColor,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (isLayoutEditing) {
                                                Modifier.graphicsLayer {
                                                    val currentBounds = cardBounds[event.id]
                                                    // Compensate for layout shifts caused by reordering
                                                    // and animateItem so the card stays under the finger;
                                                    // settleProgress scales the whole offset to 0 on
                                                    // release so the card glides onto its slot.
                                                    val settle = settleProgress.value
                                                    val layoutShift = if (isDragged && currentBounds != null && dragStartBounds != Rect.Zero) {
                                                        currentBounds.topLeft - dragStartBounds.topLeft
                                                    } else {
                                                        Offset.Zero
                                                    }
                                                    val lift = if (isDragged) settle else 0f
                                                    translationX = if (isDragged) (dragOffset.x - layoutShift.x) * settle else 0f
                                                    translationY = if (isDragged) (dragOffset.y - layoutShift.y - cardDragLiftPx) * settle else 0f
                                                    scaleX = 1f + CardLiftScaleDelta * lift
                                                    scaleY = 1f + CardLiftScaleDelta * lift
                                                    rotationZ = if (isDragged) {
                                                        editTilt + (dragOffset.x / 110f).coerceIn(-3f, 3f) * settle
                                                    } else {
                                                        editTilt
                                                    }
                                                    shadowElevation = if (isDragged) {
                                                        lerp(cardEditingShadowPx, cardDragShadowPx, lift)
                                                    } else {
                                                        cardEditingShadowPx
                                                    }
                                                }
                                            } else {
                                                Modifier
                                            },
                                        ),
                                ) {
                                    AdaptiveCardSurface(
                                        palette = heroPalette,
                                        modifier = Modifier
                                            .widthIn(max = if (isWideCard) WideCardMaxWidth else Dp.Unspecified)
                                            .fillMaxWidth()
                                    ) {
                                        EventCardFeature(
                                            event = event,
                                            onClick = if (isLayoutEditing) null else ({ onOpenEventSettings(event) }),
                                        )
                                        if (isDropTarget) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(heroPalette.cardHighlightColor.copy(alpha = .12f)),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 拖到顶/底守卫区附近时自动滚动（设计方案 §7.1）：程序滚动不受
                // userScrollEnabled 限制；速度随贴近程度平方加速；滚动中持续
                // 重跑命中检测，让内容在卡片下方流动时实时交换。
                LaunchedEffect(draggedCardId, isSettling) {
                    if (draggedCardId == null || isSettling) return@LaunchedEffect
                    var lastFrameNs = 0L
                    while (draggedCardId != null && !isSettling && dragStartBounds != Rect.Zero) {
                        val frameNs = withFrameNanos { it }
                        val dtMs = if (lastFrameNs == 0L) 16f else (frameNs - lastFrameNs) / 1_000_000f
                        lastFrameNs = frameNs
                        // 卡片视觉位置 = 初始槽位 + 拖拽偏移（补偿使其不随滚动移动）。
                        val visualTop = dragStartBounds.top + dragOffset.y
                        val visualBottom = dragStartBounds.bottom + dragOffset.y
                        val topProximity = ((topGuardPx + edgeZonePx - visualTop) / edgeZonePx).coerceIn(0f, 1f)
                        val bottomProximity =
                            ((visualBottom - (contentHeightPx - bottomGuardPx - edgeZonePx)) / edgeZonePx).coerceIn(0f, 1f)
                        val intensity = maxOf(topProximity, bottomProximity)
                        if (intensity > 0f) {
                            val speed = (4f + 20f * intensity * intensity) * dtMs
                            if (topProximity > 0f) {
                                homeGridState.scrollBy(-speed * topProximity)
                            } else {
                                homeGridState.scrollBy(speed * bottomProximity)
                            }
                        }
                        attemptReorder(dragStartBounds.center + dragOffset, System.currentTimeMillis())
                    }
                }

                CollapsibleHomeTopBar(
                    collapseProgress = collapseProgress,
                    isSearchVisible = isSearchVisible,
                    searchQuery = searchQuery,
                    searchFocusRequester = searchFocusRequester,
                    onSearchQueryChange = { searchQuery = it },
                    onCloseSearch = {
                        isSearchVisible = false
                        searchQuery = ""
                    },
                    onOpenSearch = { isSearchVisible = true },
                    onOpenGroups = onOpenGroups,
                    onOpenSettings = onOpenSettings,
                    onEditLayout = { onLayoutEditingChange(true) },
                    heroTextColor = heroScene.heroTextColor,
                    palette = heroPalette,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                )

                AnimatedVisibility(
                    visible = isLayoutEditing,
                    enter = fadeIn(animationSpec = tween(220)) + slideInVertically(
                        animationSpec = tween(260),
                        initialOffsetY = { -it / 2 },
                    ),
                    exit = fadeOut(animationSpec = tween(180)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 68.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = heroPalette.uiBaseColor.copy(alpha = .88f),
                        contentColor = heroPalette.cardContentColor,
                        border = BorderStroke(1.dp, heroPalette.cardHighlightColor.copy(alpha = .52f)),
                        shadowElevation = 8.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 16.dp, end = 7.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("编辑布局", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "拖动卡片整理布局",
                                color = heroPalette.cardContentColor.copy(alpha = .62f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(
                                onClick = ::undoLayout,
                                enabled = canUndoLayout,
                                contentPadding = PaddingValues(horizontal = 10.dp),
                            ) { Text("撤销") }
                            TextButton(
                                onClick = ::redoLayout,
                                enabled = canRedoLayout,
                                contentPadding = PaddingValues(horizontal = 10.dp),
                            ) { Text("重做") }
                            TextButton(onClick = ::finishLayoutEditing) { Text("完成") }
                        }
                    }
                }

                // Keep the feed full-height. The timeline is an independent
                // floating pill, so only its rounded surface overlaps a card;
                // the grid can still scroll its final card fully above it.
                TimelineNavigation(
                    selectedDestination = TimelineDestination.BIG_EVENT,
                    onSelectBigEvents = {},
                    onSelectDaybook = onOpenDaybook,
                    onCreateEvent = onOpenCreateEvent,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )

            }
    }
}
