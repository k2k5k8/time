package com.cch.momentmark.ui

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

private enum class AppScreen {
    HOME,
    DAYBOOK,
    EVENT_DETAIL,
    RELATED_EDITOR,
    EVENT_DETAIL_EDIT,
    SETTINGS,
    GROUP_MANAGEMENT,
    EVENT_SETTINGS,
    EVENT_CREATE,
}

internal enum class EventFilter(val label: String) {
    ALL("全部"),
    FUTURE("未来"),
    PAST("过去"),
    PINNED("置顶"),
}

private val CardTemplateOptions = listOf(
    EventCardTemplateKey.CLASSIC,
    EventCardTemplateKey.TRAVEL_MINIMAL_EDITORIAL,
    EventCardTemplateKey.TRAVEL_SUNSET_GLASS,
    EventCardTemplateKey.TRAVEL_SCRAPBOOK,
)

private fun cardTemplateLabel(template: EventCardTemplateKey): String = eventTemplateLabel(template)

private fun travelIconLabel(icon: TravelCardIcon): String = when (icon) {
    TravelCardIcon.CALENDAR -> "日历"
    TravelCardIcon.CLOCK -> "时钟"
    TravelCardIcon.HEART -> "爱心"
    TravelCardIcon.AIRPLANE -> "飞机"
}

private fun parseTravelDate(value: String): LocalDate? = runCatching {
    LocalDate.parse(value)
}.getOrNull()

private data class EventCardPalette(
    val header: Color,
    val headerContent: Color,
    val body: Color,
    val bodyContent: Color,
    val footer: Color,
    val footerContent: Color,
)

private val CardBlue = Color(0xFF5ED1FF)
private val CardOrange = Color(0xFFFFBC00)
private val BlueHeaderContent = Color(0xFF0C2B38)
private val OrangeHeaderContent = Color(0xFF3B2A00)

private const val HomeGridColumns = 2

/** 拖拽浮起时的放大增量（设计方案 §6.3：移动过程中不再缩放，只在拿/放时刻变化）。 */
private const val CardLiftScaleDelta = 0.055f

/**
 * 大卡（跨两列）的最大宽度。宽屏/平板上大卡不会被拉得过宽，
 * 手机上依旧自然占满整行。
 */
private val WideCardMaxWidth = 400.dp

private val LantingheiTcHeavy = FontFamily(
    Font(com.cch.momentmark.R.font.lantinghei_tc_heavy, FontWeight.Black),
)

@Composable
fun MomentMarkApp() {
    val context = LocalContext.current
    val database = remember(context) { MomentMarkDatabase.create(context) }
    val repository = remember(database) { TimeEventRepository(database.timeEventDao()) }
    val settingsStore = remember(context) { MomentMarkSettingsStore(context) }
    val groupStore = remember(context) { MomentMarkGroupStore(context) }
    val detailStore = remember(context) { EventDetailStore(context) }
    var themeMode by rememberSaveable { mutableStateOf(ThemeMode.SYSTEM) }
    var persistedEvents by remember { mutableStateOf<List<TimeEvent>?>(null) }
    var savedGroups by remember { mutableStateOf<List<String>?>(null) }
    var selectedFilter by rememberSaveable { mutableStateOf(EventFilter.ALL) }
    var selectedGroup by rememberSaveable { mutableStateOf<String?>(null) }
    var screen by rememberSaveable { mutableStateOf(AppScreen.HOME) }
    var selectedDaybookDateText by rememberSaveable { mutableStateOf<String?>(null) }
    var eventCreateReturnScreenName by rememberSaveable { mutableStateOf(AppScreen.HOME.name) }
    var selectedEventId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedRelatedId by rememberSaveable { mutableStateOf<String?>(null) }
    var templateOverrides by remember {
        mutableStateOf<Map<String, EventCardTemplateKey>>(emptyMap())
    }
    var travelConfigOverrides by remember {
        mutableStateOf<Map<String, TravelCardConfig>>(emptyMap())
    }
    BackHandler(enabled = screen != AppScreen.HOME) {
        screen = when (screen) {
            AppScreen.RELATED_EDITOR,
            AppScreen.EVENT_DETAIL_EDIT,
            -> AppScreen.EVENT_DETAIL
            AppScreen.EVENT_CREATE -> runCatching {
                AppScreen.valueOf(eventCreateReturnScreenName)
            }.getOrDefault(AppScreen.HOME)
            else -> AppScreen.HOME
        }
    }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    // Layout editing lives above HomeScreen so the drawer gesture can be
    // disabled while the user is arranging cards.
    var isLayoutEditing by rememberSaveable { mutableStateOf(false) }
    var pendingUndoId by rememberSaveable { mutableStateOf<String?>(null) }

    DisposableEffect(database) {
        onDispose { database.close() }
    }
    LaunchedEffect(repository) {
        repository.seedIfEmpty(SampleEvents.all)
        repository.observeActive().collect { entities ->
            persistedEvents = entities.map { it.toDomain() }
        }
    }
    LaunchedEffect(settingsStore) {
        settingsStore.themeMode.collect { themeMode = it }
    }
    LaunchedEffect(groupStore) {
        groupStore.groups.collect { savedGroups = it }
    }
    LaunchedEffect(pendingUndoId) {
        if (pendingUndoId != null) {
            // Give the user a brief window to tap undo without lingering on screen.
            delay(4_000)
            pendingUndoId = null
        }
    }
    val homeEvents = persistedEvents ?: emptyList()
    val groupItems = drawerGroups(homeEvents, savedGroups.orEmpty())
    LaunchedEffect(selectedGroup, groupItems) {
        if (selectedGroup != null && groupItems.none { it.name == selectedGroup }) {
            selectedGroup = null
        }
    }

    MomentMarkTheme(themeMode = themeMode) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            scrimColor = Color(0x47463B34),
            drawerContent = {
                CategoryDrawer(
                    events = homeEvents,
                    groups = groupItems,
                    selectedFilter = selectedFilter,
                    selectedGroup = selectedGroup,
                    onSelectFilter = { filter ->
                        selectedFilter = filter
                    },
                    onSelectGroup = { selectedGroup = it },
                    onManageGroups = {
                        scope.launch { drawerState.close() }
                        screen = AppScreen.GROUP_MANAGEMENT
                    },
                    onClose = { scope.launch { drawerState.close() } },
                )
            },
            gesturesEnabled = screen == AppScreen.HOME && !isLayoutEditing,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = screen,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        (fadeIn() + scaleIn(initialScale = 0.985f) + slideInHorizontally { it / 14 }) togetherWith
                            (fadeOut() + slideOutHorizontally { -it / 14 })
                    },
                    label = "page-transition",
                ) { activeScreen ->
                when (activeScreen) {
                AppScreen.HOME -> HomeFeature(
                    events = homeEvents,
                    selectedFilter = selectedFilter,
                    selectedGroup = selectedGroup,
                    templateOverrides = templateOverrides,
                    travelConfigOverrides = travelConfigOverrides,
                    isLayoutEditing = isLayoutEditing,
                    onLayoutEditingChange = { isLayoutEditing = it },
                    onOpenGroups = { scope.launch { drawerState.open() } },
                    onOpenSettings = { screen = AppScreen.SETTINGS },
                    onOpenCreateEvent = {
                        selectedDaybookDateText = null
                        eventCreateReturnScreenName = AppScreen.HOME.name
                        screen = AppScreen.EVENT_CREATE
                    },
                    onOpenDaybook = { screen = AppScreen.DAYBOOK },
                    onOpenEventSettings = { event ->
                        selectedEventId = event.id
                        screen = AppScreen.EVENT_DETAIL
                    },
                )

                AppScreen.DAYBOOK -> DaybookFeature(
                    userEvents = homeEvents,
                    onOpenBigEvents = { screen = AppScreen.HOME },
                    onOpenCreateEvent = { date ->
                        selectedDaybookDateText = date.toString()
                        eventCreateReturnScreenName = AppScreen.DAYBOOK.name
                        screen = AppScreen.EVENT_CREATE
                    },
                    onOpenEventDetail = { event ->
                        selectedEventId = event.id
                        screen = AppScreen.EVENT_DETAIL
                    },
                )

                AppScreen.EVENT_DETAIL -> {
                    val selectedEvent = homeEvents.firstOrNull { it.id == selectedEventId }
                    if (selectedEvent == null) {
                        screen = AppScreen.HOME
                    } else {
                        EventDetailFeature(
                            event = selectedEvent,
                            detailStore = detailStore,
                            onBack = { screen = AppScreen.HOME },
                            onEdit = { screen = AppScreen.EVENT_DETAIL_EDIT },
                            onAddRelated = {
                                selectedRelatedId = null
                                screen = AppScreen.RELATED_EDITOR
                            },
                            onEditRelated = { item ->
                                selectedRelatedId = item.id
                                screen = AppScreen.RELATED_EDITOR
                            },
                        )
                    }
                }

                AppScreen.RELATED_EDITOR -> {
                    val parentId = selectedEventId
                    if (parentId == null) {
                        screen = AppScreen.HOME
                    } else {
                        val relatedItems by detailStore.relatedCountdowns(parentId).collectAsState(initial = emptyList())
                        val item = relatedItems.firstOrNull { it.id == selectedRelatedId }
                        RelatedCountdownEditorFeature(
                            item = item,
                            onBack = { screen = AppScreen.EVENT_DETAIL },
                            onSave = { saved ->
                                scope.launch {
                                    detailStore.saveRelated(parentId, saved)
                                    screen = AppScreen.EVENT_DETAIL
                                }
                            },
                            onDelete = if (item == null) null else {
                                {
                                    scope.launch {
                                        detailStore.deleteRelated(parentId, item.id)
                                        screen = AppScreen.EVENT_DETAIL
                                    }
                                }
                            },
                        )
                    }
                }

                AppScreen.EVENT_DETAIL_EDIT -> {
                    val selectedEvent = homeEvents.firstOrNull { it.id == selectedEventId }
                    if (selectedEvent == null) {
                        screen = AppScreen.HOME
                    } else {
                        EventCreateFeature(
                            initialEvent = selectedEvent,
                            onBack = { screen = AppScreen.EVENT_DETAIL },
                            onSave = { updated ->
                                scope.launch {
                                    repository.save(updated)
                                    screen = AppScreen.EVENT_DETAIL
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    val deletedId = selectedEvent.id
                                    val deletedAt = System.currentTimeMillis()
                                    repository.softDelete(
                                        id = deletedId,
                                        deletedAt = deletedAt,
                                        updatedAt = deletedAt,
                                    )
                                    selectedEventId = null
                                    screen = AppScreen.HOME
                                    pendingUndoId = deletedId
                                }
                            },
                        )
                    }
                }

                AppScreen.SETTINGS -> SettingsFeature(
                    themeMode = themeMode,
                    onThemeModeChange = {
                        themeMode = it
                        scope.launch { settingsStore.setThemeMode(it) }
                    },
                    onBack = { screen = AppScreen.HOME },
                )


                AppScreen.GROUP_MANAGEMENT -> GroupManagementScreen(
                    groups = groupItems,
                    onBack = { screen = AppScreen.HOME },
                    onCreate = { name ->
                        val updated = (savedGroups.orEmpty() + name).distinct()
                        savedGroups = updated
                        scope.launch { groupStore.saveGroups(updated) }
                    },
                    onRename = { oldName, newName ->
                        val updated = savedGroups.orEmpty().map { if (it == oldName) newName else it }
                            .distinct()
                        savedGroups = updated
                        if (selectedGroup == oldName) selectedGroup = newName
                        scope.launch {
                            repository.renameGroup(oldName, newName, System.currentTimeMillis())
                            groupStore.saveGroups(updated)
                        }
                    },
                    onDelete = { name ->
                        val updated = savedGroups.orEmpty().filterNot { it == name }
                        savedGroups = updated
                        if (selectedGroup == name) selectedGroup = null
                        scope.launch {
                            repository.clearGroup(name, System.currentTimeMillis())
                            groupStore.saveGroups(updated)
                        }
                    },
                )

                AppScreen.EVENT_SETTINGS -> {
                    val selectedEvent = (homeEvents + SampleEvents.templateGallery)
                        .firstOrNull { it.id == selectedEventId }
                        ?.let { event ->
                            val config = travelConfigOverrides[event.id] ?: event.travelCardConfig
                            event.copy(
                                cardTemplateKey = templateOverrides[event.id]
                                    ?: event.cardTemplateKey,
                                travelCardConfig = config,
                                localDate = if (config != null && event.timeType == com.cch.momentmark.domain.model.EventTimeType.ALL_DAY) {
                                    config.targetDate
                                } else {
                                    event.localDate
                                },
                            )
                        }
                    if (selectedEvent == null) {
                        screen = AppScreen.HOME
                    } else {
                        EventSettingsFeature(
                            event = selectedEvent,
                            onBack = { screen = AppScreen.HOME },
                            onTitleChange = { title ->
                                if (homeEvents.any { it.id == selectedEvent.id }) {
                                    scope.launch {
                                        repository.save(
                                            selectedEvent.copy(
                                                title = title,
                                                travelCardConfig = selectedEvent.travelCardConfig
                                                    ?.copy(title = title),
                                            ),
                                        )
                                    }
                                }
                            },
                            onSubtitleChange = { subtitle ->
                                if (homeEvents.any { it.id == selectedEvent.id }) {
                                    scope.launch {
                                        repository.save(
                                            selectedEvent.copy(
                                                subtitle = subtitle,
                                                travelCardConfig = selectedEvent.travelCardConfig
                                                    ?.copy(badgeLabel = subtitle),
                                            ),
                                        )
                                    }
                                }
                            },
                            onGroupLabelChange = { groupLabel ->
                                if (homeEvents.any { it.id == selectedEvent.id }) {
                                    scope.launch {
                                        repository.save(
                                            selectedEvent.copy(
                                                groupLabel = groupLabel,
                                                travelCardConfig = selectedEvent.travelCardConfig
                                                    ?.copy(groupLabel = groupLabel),
                                            ),
                                        )
                                    }
                                }
                            },
                            onTemplateChange = { template ->
                                templateOverrides = templateOverrides + (selectedEvent.id to template)
                                if (homeEvents.any { it.id == selectedEvent.id }) {
                                    scope.launch {
                                        repository.save(selectedEvent.copy(cardTemplateKey = template))
                                    }
                                }
                            },
                            onTravelConfigChange = { config ->
                                travelConfigOverrides = travelConfigOverrides + (selectedEvent.id to config)
                                if (homeEvents.any { it.id == selectedEvent.id }) {
                                    scope.launch {
                                        repository.save(
                                            selectedEvent.copy(
                                                localDate = if (selectedEvent.timeType == com.cch.momentmark.domain.model.EventTimeType.ALL_DAY) {
                                                    config.targetDate
                                                } else {
                                                    selectedEvent.localDate
                                                },
                                                travelCardConfig = config.copy(
                                                    title = selectedEvent.title,
                                                    badgeLabel = selectedEvent.subtitle,
                                                    groupLabel = selectedEvent.groupLabel,
                                                ),
                                            ),
                                        )
                                    }
                                }
                            },
                            onTogglePinned = {
                                if (homeEvents.any { it.id == selectedEvent.id }) {
                                    scope.launch {
                                        repository.setPinned(
                                            id = selectedEvent.id,
                                            pinned = !selectedEvent.isPinned,
                                            updatedAt = System.currentTimeMillis(),
                                        )
                                    }
                                }
                            },
                            onArchive = {
                                if (homeEvents.any { it.id == selectedEvent.id }) {
                                    scope.launch {
                                        repository.setArchived(
                                            id = selectedEvent.id,
                                            archived = true,
                                            updatedAt = System.currentTimeMillis(),
                                        )
                                        selectedEventId = null
                                        screen = AppScreen.HOME
                                    }
                                }
                            },
                            onDelete = {
                                if (homeEvents.any { it.id == selectedEvent.id }) {
                                    scope.launch {
                                        val deletedId = selectedEvent.id
                                        val deletedAt = System.currentTimeMillis()
                                        repository.softDelete(
                                            id = deletedId,
                                            deletedAt = deletedAt,
                                            updatedAt = deletedAt,
                                        )
                                        selectedEventId = null
                                        screen = AppScreen.HOME
                                        pendingUndoId = deletedId
                                    }
                                }
                            },
                        )
                    }
                }

                    AppScreen.EVENT_CREATE -> EventCreateFeature(
                        initialDate = selectedDaybookDateText?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                        onBack = {
                            selectedDaybookDateText = null
                            screen = runCatching {
                                AppScreen.valueOf(eventCreateReturnScreenName)
                            }.getOrDefault(AppScreen.HOME)
                        },
                        onSave = { event ->
                            scope.launch {
                                repository.save(event)
                                val returnScreen = runCatching {
                                    AppScreen.valueOf(eventCreateReturnScreenName)
                                }.getOrDefault(AppScreen.HOME)
                                selectedDaybookDateText = null
                                screen = returnScreen
                            }
                        },
                    )
                }
                }
                pendingUndoId?.let { deletedId ->
                    UndoDeleteToast(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 156.dp),
                        onUndo = {
                            pendingUndoId = null
                            scope.launch {
                                repository.restoreDeleted(
                                    id = deletedId,
                                    updatedAt = System.currentTimeMillis(),
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

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

@Composable
internal fun EventCard(
    event: TimeEvent,
    onClick: (() -> Unit)? = null,
) {
    val accessibilityLabel = eventAccessibilityLabel(event)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .semantics(mergeDescendants = true) {
                            contentDescription = accessibilityLabel
                            role = Role.Button
                        }
                        .clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier.semantics {
                        contentDescription = accessibilityLabel
                    }
                },
            )
    ) {
        when (event.cardTemplateKey) {
            EventCardTemplateKey.CLASSIC -> ClassicEventCard(event = event)
            EventCardTemplateKey.TRAVEL_MINIMAL_EDITORIAL -> MinimalEditorialTravelCard(event = event)
            EventCardTemplateKey.TRAVEL_SUNSET_GLASS -> SunsetTravelCard(event = event)
            EventCardTemplateKey.TRAVEL_SCRAPBOOK,
            EventCardTemplateKey.TRAVEL_COUNTDOWN -> TravelCountdownCard(event = event)
        }
    }
}

private fun eventAccessibilityLabel(event: TimeEvent): String = buildString {
    val fields = event.cardFields()
    append(fields.title)
    if (fields.subtitle.isNotBlank()) append("，${fields.subtitle}")
    if (fields.countdownLabel.isNotBlank()) append("，${fields.countdownLabel}")
    if (fields.startTimeLabel.isNotBlank()) append("，${fields.startTimeLabel}")
    if (fields.groupLabel.isNotBlank()) append("，分组：${fields.groupLabel}")
    if (event.cardTemplateKey != EventCardTemplateKey.CLASSIC) append("，旅行事件")
}

private val TravelCardShape = RoundedCornerShape(18.dp)
private val TravelCardBackground = Color(0xFFFBF7EF)
private val TravelTextColor = Color(0xFF4E4139)
private val TravelMutedColor = Color(0xFF9B8D80)
private val TravelPink = Color(0xFFE98591)
private val TravelOrange = Color(0xFFE0A06E)
private val TravelBlue = Color(0xFF9CB9D6)
private val TravelChocolate = Color(0xFF6A4B3B)
private val TravelLineColor = Color(0xFFCAB8A8)

@Composable
private fun MinimalEditorialTravelCard(
    event: TimeEvent,
    onClick: (() -> Unit)? = null,
) {
    val config = event.travelCardConfig ?: TravelCardConfig(title = event.title)
    val presentation = rememberTimeCardPresentation(event)
    val fields = presentation.fields
    val countdown = presentation.countdown
    val compact = config.size == TravelCardSize.SMALL
    val shape = RoundedCornerShape(if (compact) 22.dp else 28.dp)
    val contentColor = Color(0xFF4A3B31)
    val mutedColor = Color(0xFF857568)
    val lineColor = Color(0xFFD7C8B8)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(if (compact) 0.82f else 0.98f)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = Color(0xFFF9F2E8),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.92f)),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // 以卡片实际宽度驱动全部尺寸：小卡 / 大卡 / 宽屏共用同一套比例，
            // 不再依赖写死的字号，文字也不会因换行破坏版式。
            val cardWidth = maxWidth.value
            val horizontalPadding = (cardWidth * 0.103f).coerceIn(16f, 30f).dp
            val verticalPadding = (cardWidth * 0.115f).coerceIn(16f, 30f).dp
            val titleFontSize = (cardWidth * 0.12f).coerceIn(18f, 33f).sp
            val titleLineHeight = (cardWidth * 0.145f).coerceIn(23f, 40f).sp
            val metaFontSize = (cardWidth * 0.052f).coerceIn(9f, 11.5f).sp
            val headlineGap = (cardWidth * 0.098f).coerceIn(12f, 26f).dp
            val sectionGap = (cardWidth * 0.069f).coerceIn(10f, 18f).dp
            val panelGap = (cardWidth * 0.08f).coerceIn(11f, 22f).dp
            // 位数越多数字越收敛，保证倒计时永远单行完整展示。
            val digitFactor = when (countdown.amount.toString().length) {
                1, 2, 3 -> 1f
                4 -> 0.8f
                else -> 0.66f
            }
            val numberFontSize = (cardWidth * 0.38f * digitFactor).coerceIn(56f, 112f).sp
            val unitFontSize = (cardWidth * 0.098f).coerceIn(15f, 25f).sp
            val unitStartPadding = (cardWidth * 0.046f).coerceIn(6f, 11f).dp
            val unitBottomPadding = (cardWidth * 0.04f).coerceIn(6f, 15f).dp
            MinimalCreamBackground()
            Icon(
                imageVector = Icons.Outlined.FlightTakeoff,
                contentDescription = "旅行装饰",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = (cardWidth * 0.115f).coerceIn(18f, 30f).dp,
                        end = (cardWidth * 0.103f).coerceIn(16f, 30f).dp,
                    )
                    .size((cardWidth * 0.098f).coerceIn(14f, 22f).dp)
                    .rotate(18f),
                tint = Color(0xFFB89D78).copy(alpha = 0.58f),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MinimalBadge(
                        label = fields.subtitle,
                        icon = travelIconVector(config.badgeIcon),
                        compact = compact,
                    )
                    Spacer(Modifier.weight(1f))
                    if (fields.groupLabel.isNotBlank()) {
                        Text(
                            text = fields.groupLabel,
                            color = mutedColor,
                            fontSize = metaFontSize,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.height(headlineGap))
                Text(
                    text = fields.title,
                    color = contentColor,
                    fontSize = titleFontSize,
                    lineHeight = titleLineHeight,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(sectionGap))
                TemplateDivider(showDot = true, compact = compact, color = lineColor)
                Spacer(Modifier.height(panelGap))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = countdown.amount.toString(),
                        style = TextStyle(
                            fontFamily = LantingheiTcHeavy,
                            fontSize = numberFontSize,
                            lineHeight = numberFontSize,
                            fontWeight = FontWeight.Black,
                            color = contentColor,
                        ),
                        maxLines = 1,
                        softWrap = false,
                    )
                    Text(
                        text = config.countdownUnit,
                        modifier = Modifier.padding(
                            start = unitStartPadding,
                            bottom = unitBottomPadding,
                        ),
                        color = contentColor,
                        fontSize = unitFontSize,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
                Spacer(Modifier.weight(1f))
                TemplateDivider(showDot = false, compact = compact, color = lineColor)
                Spacer(Modifier.height(sectionGap))
                TemplateDateRow(
                    dateLabel = fields.startTimeLabel,
                    dateIcon = travelIconVector(config.dateIcon),
                    compact = compact,
                    color = contentColor,
                )
            }
        }
    }
}

@Composable
private fun MinimalBadge(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    compact: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xFFEADCCB).copy(alpha = 0.82f),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 9.dp else 12.dp,
                vertical = if (compact) 5.dp else 7.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(if (compact) 14.dp else 17.dp),
                tint = Color(0xFF5F5044),
            )
            Spacer(Modifier.width(if (compact) 5.dp else 7.dp))
            Text(
                text = label,
                color = Color(0xFF5F5044),
                fontSize = if (compact) 10.sp else 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MinimalCreamBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFFBF4),
                    Color(0xFFF8F0E5),
                    Color(0xFFEFE5D8),
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h),
            ),
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.34f),
            radius = w * 0.42f,
            center = Offset(w * 0.10f, h * 0.04f),
        )
        val flightPath = Path().apply {
            moveTo(w * 0.54f, h * 0.22f)
            cubicTo(w * 0.66f, h * 0.30f, w * 0.73f, h * 0.15f, w * 0.84f, h * 0.10f)
        }
        drawPath(
            path = flightPath,
            color = Color(0xFFC8B59B).copy(alpha = 0.64f),
            style = Stroke(
                width = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx()), 0f),
            ),
        )
    }
}

/** 保留上一版的黄昏玻璃模板，供已有事件和用户继续使用。 */
@Composable
private fun SunsetTravelCard(
    event: TimeEvent,
    onClick: (() -> Unit)? = null,
) {
    val config = event.travelCardConfig ?: TravelCardConfig(title = event.title)
    val presentation = rememberTimeCardPresentation(event)
    val fields = presentation.fields
    val countdown = presentation.countdown
    val compact = config.size == TravelCardSize.SMALL
    val shape = RoundedCornerShape(18.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(if (compact) 0.84f else 1.02f)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.86f)),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // 尺寸随卡片宽度缩放，小卡与大卡共用同一套比例，避免写死。
            val cardWidth = maxWidth.value
            val horizontalPadding = (cardWidth * 0.08f).coerceIn(12f, 26f).dp
            val verticalPadding = (cardWidth * 0.075f).coerceIn(11f, 22f).dp
            val titleFontSize = (cardWidth * 0.09f).coerceIn(15f, 28f).sp
            val metaFontSize = (cardWidth * 0.052f).coerceIn(9f, 11f).sp
            val metaGap = (cardWidth * 0.028f).coerceIn(3f, 10f).dp
            val noMetaGap = (cardWidth * 0.046f).coerceIn(6f, 16f).dp
            val titleGap = (cardWidth * 0.04f).coerceIn(6f, 12f).dp
            val dividerGap = (cardWidth * 0.02f).coerceIn(3f, 7f).dp
            val footerGap = (cardWidth * 0.034f).coerceIn(4f, 12f).dp
            val unitFontSize = (cardWidth * 0.07f).coerceIn(13f, 22f).sp
            val digitFactor = when (countdown.amount.toString().length) {
                1, 2, 3 -> 1f
                4 -> 0.8f
                else -> 0.66f
            }
            val numberFontSize = (cardWidth * 0.27f * digitFactor).coerceIn(40f, 96f).sp
            Image(
                painter = painterResource(com.cch.momentmark.R.drawable.tokyo_sunset_soft),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFFFBF5).copy(alpha = 0.82f),
                                Color(0xFFFFF7F0).copy(alpha = 0.46f),
                                Color(0xFFE9E5F3).copy(alpha = 0.16f),
                            ),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            ) {
                TemplateBadge(
                    fields.subtitle,
                    travelIconVector(config.badgeIcon),
                    compact,
                    glass = true,
                )
                if (fields.groupLabel.isNotBlank()) {
                    Spacer(Modifier.height(metaGap))
                    Text(
                        text = fields.groupLabel,
                        color = Color(0xFF8E7480),
                        fontSize = metaFontSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(metaGap * 0.6f))
                } else {
                    Spacer(Modifier.height(noMetaGap))
                }
                Text(
                    text = fields.title,
                    color = Color(0xFF5C4C58),
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                )
                Spacer(Modifier.height(titleGap))
                TemplateDivider(showDot = true, compact = compact, color = Color.White.copy(alpha = 0.58f))
                Spacer(Modifier.height(dividerGap))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = countdown.amount.toString(),
                        style = TextStyle(
                            fontFamily = LantingheiTcHeavy,
                            fontSize = numberFontSize,
                            lineHeight = numberFontSize,
                            fontWeight = FontWeight.Light,
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFF5B536C), Color(0xFFD08080)),
                            ),
                        ),
                        maxLines = 1,
                        softWrap = false,
                    )
                    Text(
                        text = config.countdownUnit,
                        modifier = Modifier.padding(
                            start = footerGap * 0.6f,
                            bottom = footerGap,
                        ),
                        color = Color(0xFFB56F75),
                        fontSize = unitFontSize,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
                Spacer(Modifier.weight(1f))
                TemplateDivider(showDot = false, compact = compact, color = Color.White.copy(alpha = 0.58f))
                Spacer(Modifier.height(footerGap))
                TemplateDateRow(
                    dateLabel = fields.startTimeLabel,
                    dateIcon = travelIconVector(config.dateIcon),
                    compact = compact,
                    color = Color(0xFF5C4C58),
                )
            }
        }
    }
}

private fun travelIconVector(icon: TravelCardIcon): androidx.compose.ui.graphics.vector.ImageVector = when (icon) {
    TravelCardIcon.CALENDAR -> Icons.Outlined.CalendarMonth
    TravelCardIcon.CLOCK -> Icons.Outlined.AccessTime
    TravelCardIcon.HEART -> Icons.Outlined.FavoriteBorder
    TravelCardIcon.AIRPLANE -> Icons.Outlined.FlightTakeoff
}

@Composable
private fun TemplateBadge(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    compact: Boolean,
    glass: Boolean = false,
) {
    Surface(
        modifier = Modifier.wrapContentWidth(),
        shape = RoundedCornerShape(50),
        color = if (glass) Color.White.copy(alpha = 0.68f) else Color(0xFFE8DDCF).copy(alpha = 0.82f),
        border = if (glass) BorderStroke(1.dp, Color.White.copy(alpha = 0.72f)) else null,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 9.dp else 13.dp,
                vertical = if (compact) 5.dp else 8.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(if (compact) 15.dp else 18.dp),
                tint = if (glass) Color(0xFFC9797A) else TravelChocolate,
            )
            Spacer(Modifier.width(if (compact) 5.dp else 8.dp))
            Text(
                text = label,
                color = if (glass) Color(0xFF5C4C58) else TravelChocolate,
                fontSize = if (compact) 11.sp else 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TemplateDivider(
    showDot: Boolean,
    compact: Boolean,
    color: Color = TravelLineColor,
) {
    Row(
        modifier = Modifier.fillMaxWidth(if (showDot && !compact) 0.82f else 1f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(color.copy(alpha = 0.76f)),
        )
        if (showDot) {
            Spacer(Modifier.width(5.dp))
            Box(
                modifier = Modifier
                    .size(if (compact) 3.dp else 4.dp)
                    .background(color, CircleShape),
            )
        }
    }
}

@Composable
private fun TemplateDateRow(
    dateLabel: String,
    dateIcon: androidx.compose.ui.graphics.vector.ImageVector,
    compact: Boolean,
    color: Color,
) {
    val dateParts = dateLabel.split(" ", limit = 2)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = dateIcon,
            contentDescription = null,
            modifier = Modifier.size(if (compact) 14.dp else 19.dp),
            tint = color.copy(alpha = 0.82f),
        )
        Spacer(Modifier.width(if (compact) 5.dp else 8.dp))
        Text(
            text = dateParts.firstOrNull().orEmpty(),
            color = color.copy(alpha = 0.88f),
            fontSize = if (compact) 9.sp else 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
        if (dateParts.size > 1) {
            Spacer(Modifier.width(if (compact) 5.dp else 8.dp))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(if (compact) 12.dp else 18.dp)
                    .background(color.copy(alpha = 0.35f)),
            )
            Spacer(Modifier.width(if (compact) 5.dp else 8.dp))
            Text(
                text = dateParts[1],
                color = color.copy(alpha = 0.84f),
                fontSize = if (compact) 9.sp else 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SunsetGlassBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.linearGradient(
                listOf(Color(0xFFFFE7CF), Color(0xFFFFD8D2), Color(0xFFDADCF0)),
                start = Offset(0f, h),
                end = Offset(w, 0f),
            ),
        )
        drawCircle(Color(0xFFFFF4D4).copy(alpha = 0.66f), w * 0.24f, Offset(w * 0.54f, h * 0.58f))
        val mountain = Path().apply {
            moveTo(w * 0.30f, h * 0.86f)
            lineTo(w * 0.56f, h * 0.56f)
            lineTo(w * 0.82f, h * 0.86f)
            close()
        }
        drawPath(mountain, Color(0xFFB6B4D1).copy(alpha = 0.54f))
        repeat(13) { index ->
            val x = w * (0.32f + index / 32f)
            val buildingHeight = h * (0.05f + (index % 4) * 0.018f)
            drawRoundRect(
                color = Color(0xFF6E7089).copy(alpha = 0.22f),
                topLeft = Offset(x, h * 0.92f - buildingHeight),
                size = androidx.compose.ui.geometry.Size(w * 0.035f, buildingHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f),
            )
        }
        val towerX = w * 0.84f
        drawLine(Color(0xFFB96C74).copy(alpha = 0.55f), Offset(towerX, h * 0.44f), Offset(towerX, h * 0.94f), 3f)
        drawLine(Color.White.copy(alpha = 0.42f), Offset(towerX - 18f, h * 0.94f), Offset(towerX, h * 0.44f), 2f)
        drawLine(Color.White.copy(alpha = 0.34f), Offset(towerX + 18f, h * 0.94f), Offset(towerX, h * 0.44f), 2f)
    }
}

@Composable
private fun TravelCountdownCard(
    event: TimeEvent,
    onClick: (() -> Unit)? = null,
) {
    val config = event.travelCardConfig ?: TravelCardConfig(title = event.title)
    val presentation = rememberTimeCardPresentation(event)
    val fields = presentation.fields
    val countdown = presentation.countdown
    val badgeIcon = travelIconVector(config.badgeIcon)
    val dateIcon = travelIconVector(config.dateIcon)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(if (config.size == TravelCardSize.WIDE) 1.02f else 0.84f)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = TravelCardShape,
        color = TravelCardBackground,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.82f)),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            TravelScrapbookBackground(
                preset = config.backgroundPreset,
                compact = config.size == TravelCardSize.SMALL,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.Transparent,
                                Color(0xFFE9DCCB).copy(alpha = 0.12f),
                            ),
                        ),
                    ),
            )
            if (config.size == TravelCardSize.WIDE) {
                WideTravelCardContent(
                    config = config,
                    fields = fields,
                    countdown = countdown,
                    badgeIcon = badgeIcon,
                    dateIcon = dateIcon,
                )
            } else {
                SmallTravelCardContent(
                    config = config,
                    fields = fields,
                    countdown = countdown,
                    badgeIcon = badgeIcon,
                    dateIcon = dateIcon,
                )
            }
        }
    }
}

@Composable
private fun WideTravelCardContent(
    config: TravelCardConfig,
    fields: TimeCardFields,
    countdown: com.cch.momentmark.domain.time.CountdownResult,
    badgeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    dateIcon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // 尺寸随卡片宽度缩放，宽屏大卡不会出现写死字号导致的空旷或溢出。
        val cardWidth = maxWidth.value
        val horizontalPadding = (cardWidth * 0.057f).coerceIn(16f, 24f).dp
        val verticalPadding = (cardWidth * 0.055f).coerceIn(14f, 20f).dp
        val titleFontSize = (cardWidth * 0.074f).coerceIn(20f, 28f).sp
        val titleRowHeight = (cardWidth * 0.097f).coerceIn(28f, 38f).dp
        val rowGap = (cardWidth * 0.02f).coerceIn(5f, 8f).dp
        val waveHeight = (cardWidth * 0.034f).coerceIn(10f, 14f).dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        ) {
            TravelTapeBadge(fields.subtitle, badgeIcon)
            Spacer(Modifier.height(rowGap * 0.75f))
            Box(modifier = Modifier.fillMaxWidth().height(titleRowHeight)) {
                Text(
                    text = fields.title,
                    modifier = Modifier.fillMaxWidth(),
                    color = TravelTextColor,
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                )
                Text(
                    text = "♥",
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 18.dp),
                    color = TravelPink,
                    fontSize = 13.sp,
                )
                Text(
                    text = "〰",
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 22.dp),
                    color = TravelPink.copy(alpha = 0.75f),
                    fontSize = 17.sp,
                )
            }
            HandDrawnWave(modifier = Modifier.fillMaxWidth().height(waveHeight))
            Spacer(Modifier.height(rowGap))
            TravelCountdownPanel(
                modifier = Modifier.fillMaxWidth().weight(1f),
                countdown = countdown,
                unit = config.countdownUnit,
                compact = false,
            )
            Spacer(Modifier.height(rowGap))
            TravelDateSticker(
                dateLabel = fields.startTimeLabel,
                dateIcon = dateIcon,
                compact = false,
            )
        }
    }
}

@Composable
private fun SmallTravelCardContent(
    config: TravelCardConfig,
    fields: TimeCardFields,
    countdown: com.cch.momentmark.domain.time.CountdownResult,
    badgeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    dateIcon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // 小卡内容同样按宽度缩放，窄屏设备上不会溢出或换行。
        val cardWidth = maxWidth.value
        val horizontalPadding = (cardWidth * 0.063f).coerceIn(8f, 14f).dp
        val verticalPadding = (cardWidth * 0.057f).coerceIn(8f, 12f).dp
        val titleFontSize = (cardWidth * 0.086f).coerceIn(12f, 17f).sp
        val rowGap = (cardWidth * 0.017f).coerceIn(2f, 4f).dp
        val waveHeight = (cardWidth * 0.046f).coerceIn(6f, 9f).dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        ) {
            TravelTapeBadge(fields.subtitle, badgeIcon, compact = true)
            Spacer(Modifier.height(rowGap))
            Text(
                text = fields.title,
                modifier = Modifier.fillMaxWidth(),
                color = TravelTextColor,
                fontSize = titleFontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
            )
            HandDrawnWave(modifier = Modifier.fillMaxWidth().height(waveHeight), compact = true)
            Spacer(Modifier.height(rowGap))
            TravelCountdownPanel(
                modifier = Modifier.fillMaxWidth().weight(1f),
                countdown = countdown,
                unit = config.countdownUnit,
                compact = true,
            )
            Spacer(Modifier.height(rowGap))
            TravelDateSticker(
                dateLabel = fields.startTimeLabel,
                dateIcon = dateIcon,
                compact = true,
            )
        }
    }
}

@Composable
private fun TravelTapeBadge(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    compact: Boolean = false,
) {
    Box(
        modifier = Modifier
            .wrapContentWidth()
            .height(if (compact) 24.dp else 31.dp)
            .rotate(if (compact) -1.5f else -2.5f),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val tear = 4.dp.toPx()
            val tape = Path().apply {
                moveTo(tear, 0f)
                lineTo(size.width - tear, 0f)
                lineTo(size.width, size.height * 0.24f)
                lineTo(size.width - tear * 0.8f, size.height * 0.52f)
                lineTo(size.width, size.height * 0.82f)
                lineTo(size.width - tear, size.height)
                lineTo(tear, size.height)
                lineTo(0f, size.height * 0.78f)
                lineTo(tear * 0.8f, size.height * 0.49f)
                lineTo(0f, size.height * 0.20f)
                close()
            }
            drawPath(tape, TravelPink.copy(alpha = 0.96f))
            drawLine(
                color = Color.White.copy(alpha = 0.44f),
                start = Offset(8.dp.toPx(), size.height * 0.22f),
                end = Offset(size.width - 8.dp.toPx(), size.height * 0.22f),
                strokeWidth = 1.dp.toPx(),
            )
        }
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 8.dp else 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "纪念日",
                modifier = Modifier.size(if (compact) 12.dp else 16.dp),
                tint = Color.White,
            )
            Spacer(Modifier.width(if (compact) 4.dp else 6.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = if (compact) 10.sp else 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HandDrawnWave(
    modifier: Modifier,
    compact: Boolean = false,
) {
    Canvas(modifier = modifier) {
        val wave = Path().apply {
            moveTo(size.width * 0.16f, size.height * 0.52f)
            cubicTo(
                size.width * 0.30f, if (compact) size.height * 0.10f else size.height * 0.04f,
                size.width * 0.44f, size.height * 0.92f,
                size.width * 0.58f, size.height * 0.50f,
            )
            cubicTo(
                size.width * 0.70f, size.height * 0.16f,
                size.width * 0.80f, size.height * 0.80f,
                size.width * 0.86f, size.height * 0.48f,
            )
        }
        drawPath(
            path = wave,
            color = TravelPink.copy(alpha = 0.86f),
            style = Stroke(width = if (compact) 1.2f else 1.7f),
        )
    }
}

@Composable
private fun TravelCountdownPanel(
    modifier: Modifier,
    countdown: com.cch.momentmark.domain.time.CountdownResult,
    unit: String,
    compact: Boolean,
) {
    val panelShape = RoundedCornerShape(if (compact) 15.dp else 22.dp)
    Box(
        modifier = modifier
            .shadow(2.dp, panelShape)
            .clip(panelShape)
            .background(Color.White.copy(alpha = 0.87f)),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRoundRect(
                color = TravelOrange.copy(alpha = 0.62f),
                style = Stroke(
                    width = 1.4.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(5.dp.toPx(), 4.dp.toPx()),
                        0f,
                    ),
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                    if (compact) 15.dp.toPx() else 22.dp.toPx(),
                ),
            )
        }
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            // 位数越多数字越收敛，倒计时永远单行完整展示在面板内。
            val digitFactor = when (countdown.amount.toString().length) {
                1, 2, 3 -> 1f
                4 -> 0.8f
                else -> 0.66f
            }
            val numberSize = if (compact) {
                (maxWidth.value * 0.28f * digitFactor).coerceIn(38f, 50f).sp
            } else {
                (maxWidth.value * 0.29f * digitFactor).coerceIn(78f, 116f).sp
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = countdown.statusLabel,
                    color = TravelMutedColor,
                    fontSize = if (compact) 9.sp else 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = countdown.amount.toString(),
                        style = TextStyle(
                            fontFamily = LantingheiTcHeavy,
                            fontSize = numberSize,
                            lineHeight = numberSize,
                            fontWeight = FontWeight.Black,
                            color = TravelPink,
                        ),
                        maxLines = 1,
                        softWrap = false,
                    )
                    Text(
                        text = unit,
                        modifier = Modifier.padding(
                            start = if (compact) 4.dp else 8.dp,
                            bottom = if (compact) 4.dp else 11.dp,
                        ),
                        color = TravelPink,
                        fontSize = if (compact) 14.sp else 23.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun TravelDateSticker(
    dateLabel: String,
    dateIcon: androidx.compose.ui.graphics.vector.ImageVector,
    compact: Boolean,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val shape = RoundedCornerShape(50)
        Box(
            modifier = Modifier
                .shadow(1.dp, shape)
                .clip(shape)
                .background(Color(0xFFFFFCF7).copy(alpha = 0.92f)),
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRoundRect(
                    color = TravelOrange.copy(alpha = 0.56f),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(4.dp.toPx(), 4.dp.toPx()),
                            0f,
                        ),
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
                )
            }
            Row(
                modifier = Modifier.padding(
                    horizontal = if (compact) 8.dp else 13.dp,
                    vertical = if (compact) 4.dp else 7.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = dateIcon,
                    contentDescription = "日期",
                    modifier = Modifier.size(if (compact) 13.dp else 17.dp),
                    tint = TravelPink,
                )
                Spacer(Modifier.width(if (compact) 4.dp else 7.dp))
                val dateParts = dateLabel.split(" ", limit = 2)
                Text(
                    text = dateParts.firstOrNull().orEmpty(),
                    color = TravelTextColor,
                    fontSize = if (compact) 9.sp else 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                if (dateParts.size > 1) {
                    Spacer(Modifier.width(if (compact) 5.dp else 8.dp))
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(if (compact) 12.dp else 17.dp)
                            .background(TravelOrange.copy(alpha = 0.58f)),
                    )
                    Spacer(Modifier.width(if (compact) 5.dp else 8.dp))
                    Text(
                        text = dateParts[1],
                        color = TravelTextColor,
                        fontSize = if (compact) 9.sp else 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun TravelScrapbookBackground(
    preset: TravelBackgroundPreset,
    compact: Boolean,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val backgroundColors = when (preset) {
            TravelBackgroundPreset.SCRAPBOOK_CREAM -> listOf(
                Color(0xFFFFFCF4),
                Color(0xFFF8EEDC),
                Color(0xFFF1DDC9),
            )
            TravelBackgroundPreset.TOKYO_SUNSET -> listOf(
                Color(0xFFFFFBF2),
                Color(0xFFF8EBDD),
                Color(0xFFF0DCC8),
            )
        }

        drawRect(
            brush = Brush.linearGradient(
                colors = backgroundColors,
                start = Offset(0f, h),
                end = Offset(w, 0f),
            ),
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.42f),
            radius = w * 0.38f,
            center = Offset(w * 0.08f, h * 0.06f),
        )
        repeat(if (compact) 12 else 26) { index ->
            val x = w * (((index * 37) % 100) / 100f)
            val y = h * (((index * 61 + 13) % 100) / 100f)
            drawCircle(
                color = TravelChocolate.copy(alpha = 0.035f),
                radius = if (index % 3 == 0) 1.2f else 0.7f,
                center = Offset(x, y),
            )
        }

        val inset = if (compact) 9.dp.toPx() else 14.dp.toPx()
        drawRoundRect(
            color = TravelOrange.copy(alpha = 0.66f),
            topLeft = Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(w - inset * 2f, h - inset * 2f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(15.dp.toPx()),
            style = Stroke(
                width = 1.2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(4.dp.toPx(), 4.dp.toPx()),
                    0f,
                ),
            ),
        )

        val flightPath = Path().apply {
            moveTo(w * 0.54f, h * 0.22f)
            cubicTo(w * 0.66f, h * 0.32f, w * 0.73f, h * 0.18f, w * 0.83f, h * 0.12f)
        }
        drawPath(
            path = flightPath,
            color = TravelChocolate.copy(alpha = 0.46f),
            style = Stroke(
                width = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(5.dp.toPx(), 6.dp.toPx()),
                    0f,
                ),
            ),
        )

        // 飞机白色贴纸轮廓与浅蓝手绘机身。
        val plane = Path().apply {
            moveTo(w * 0.80f, h * 0.10f)
            lineTo(w * 0.92f, h * 0.07f)
            lineTo(w * 0.85f, h * 0.14f)
            lineTo(w * 0.91f, h * 0.18f)
            lineTo(w * 0.82f, h * 0.16f)
            lineTo(w * 0.75f, h * 0.21f)
            lineTo(w * 0.80f, h * 0.14f)
            close()
        }
        drawPath(plane, Color.White.copy(alpha = 0.92f), style = Stroke(width = 5.dp.toPx()))
        drawPath(plane, TravelBlue.copy(alpha = 0.88f))
        drawCircle(
            color = TravelChocolate.copy(alpha = 0.34f),
            radius = 2.dp.toPx(),
            center = Offset(w * 0.54f, h * 0.22f),
        )

        if (!compact) {
            // 左侧复古相机贴纸。
            val cameraX = w * 0.08f
            val cameraY = h * 0.48f
            drawRoundRect(
                color = Color.White.copy(alpha = 0.94f),
                topLeft = Offset(cameraX - 7.dp.toPx(), cameraY - 6.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(w * 0.17f, h * 0.14f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()),
            )
            drawRoundRect(
                color = Color(0xFFECCB9D),
                topLeft = Offset(cameraX, cameraY),
                size = androidx.compose.ui.geometry.Size(w * 0.15f, h * 0.11f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
            )
            drawCircle(
                color = TravelBlue.copy(alpha = 0.88f),
                radius = w * 0.034f,
                center = Offset(cameraX + w * 0.075f, cameraY + h * 0.055f),
            )
            drawCircle(
                color = TravelChocolate.copy(alpha = 0.45f),
                radius = w * 0.018f,
                center = Offset(cameraX + w * 0.075f, cameraY + h * 0.055f),
            )
            drawRoundRect(
                color = TravelPink.copy(alpha = 0.82f),
                topLeft = Offset(cameraX + w * 0.024f, cameraY - h * 0.018f),
                size = androidx.compose.ui.geometry.Size(w * 0.045f, h * 0.022f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
            )

            // 右下粉色行李箱贴纸。
            val luggageX = w * 0.78f
            val luggageY = h * 0.78f
            drawRoundRect(
                color = Color.White.copy(alpha = 0.95f),
                topLeft = Offset(luggageX - 7.dp.toPx(), luggageY - 8.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(w * 0.16f, h * 0.20f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
            )
            drawRoundRect(
                color = TravelPink.copy(alpha = 0.92f),
                topLeft = Offset(luggageX, luggageY),
                size = androidx.compose.ui.geometry.Size(w * 0.14f, h * 0.17f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
            )
            drawLine(
                color = TravelChocolate.copy(alpha = 0.62f),
                start = Offset(luggageX + w * 0.07f, luggageY),
                end = Offset(luggageX + w * 0.07f, luggageY - h * 0.055f),
                strokeWidth = 2.dp.toPx(),
            )
            drawLine(
                color = TravelChocolate.copy(alpha = 0.62f),
                start = Offset(luggageX + w * 0.045f, luggageY - h * 0.055f),
                end = Offset(luggageX + w * 0.095f, luggageY - h * 0.055f),
                strokeWidth = 2.dp.toPx(),
            )
            drawCircle(
                color = TravelChocolate.copy(alpha = 0.72f),
                radius = 2.2.dp.toPx(),
                center = Offset(luggageX + w * 0.035f, luggageY + h * 0.175f),
            )
            drawCircle(
                color = TravelChocolate.copy(alpha = 0.72f),
                radius = 2.2.dp.toPx(),
                center = Offset(luggageX + w * 0.105f, luggageY + h * 0.175f),
            )
            drawCircle(
                color = Color(0xFFFFE39B),
                radius = 3.dp.toPx(),
                center = Offset(luggageX + w * 0.04f, luggageY + h * 0.05f),
            )
        }

        // 左下蓝白格纹胶带、爱心与小星星。
        val tapeLeft = w * 0.08f
        val tapeTop = h * 0.89f
        drawRoundRect(
            color = TravelBlue.copy(alpha = 0.72f),
            topLeft = Offset(tapeLeft, tapeTop),
            size = androidx.compose.ui.geometry.Size(w * 0.22f, h * 0.07f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
        )
        repeat(3) { index ->
            drawLine(
                color = Color.White.copy(alpha = 0.62f),
                start = Offset(tapeLeft + w * (0.035f + index * 0.07f), tapeTop),
                end = Offset(tapeLeft + w * (0.035f + index * 0.07f), tapeTop + h * 0.07f),
                strokeWidth = 1.dp.toPx(),
            )
        }
        drawLine(
            color = Color.White.copy(alpha = 0.62f),
            start = Offset(tapeLeft, tapeTop + h * 0.035f),
            end = Offset(tapeLeft + w * 0.22f, tapeTop + h * 0.035f),
            strokeWidth = 1.dp.toPx(),
        )
        drawCircle(
            color = TravelPink.copy(alpha = 0.70f),
            radius = 3.dp.toPx(),
            center = Offset(w * 0.26f, h * 0.38f),
        )
        drawLine(
            color = TravelOrange.copy(alpha = 0.82f),
            start = Offset(w * 0.18f, h * 0.26f),
            end = Offset(w * 0.18f, h * 0.30f),
            strokeWidth = 1.5.dp.toPx(),
        )
        drawLine(
            color = TravelOrange.copy(alpha = 0.82f),
            start = Offset(w * 0.16f, h * 0.28f),
            end = Offset(w * 0.20f, h * 0.28f),
            strokeWidth = 1.5.dp.toPx(),
        )
    }
}

@Composable
private fun ClassicEventCard(
    event: TimeEvent,
    onClick: (() -> Unit)? = null,
) {
    val palette = when (event.cardPaletteKey) {
        EventCardPaletteKey.BLUE_WHITE -> EventCardPalette(
            header = CardBlue,
            headerContent = BlueHeaderContent,
            body = MaterialTheme.colorScheme.surface,
            bodyContent = MaterialTheme.colorScheme.onSurface,
            footer = MaterialTheme.colorScheme.surfaceVariant,
            footerContent = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        EventCardPaletteKey.ORANGE_WHITE -> EventCardPalette(
            header = CardOrange,
            headerContent = OrangeHeaderContent,
            body = MaterialTheme.colorScheme.surface,
            bodyContent = MaterialTheme.colorScheme.onSurface,
            footer = MaterialTheme.colorScheme.surfaceVariant,
            footerContent = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    val presentation = rememberTimeCardPresentation(event)
    val countdown = presentation.countdown
    val fields = presentation.fields
    val statusWord = countdown.statusLabel
    val amount = countdown.amount.toString()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(
                when (event.travelCardConfig?.size) {
                    TravelCardSize.WIDE -> 1.02f
                    // 无旅行配置的普通事件默认落在单列小卡槽位，比例与小卡一致，
                    // 不再出现 1.28 的扁平比例。
                    TravelCardSize.SMALL -> 0.90f
                    null -> 0.90f
                },
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
        shape = RoundedCornerShape(18.dp),
        color = palette.body,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // 头部高度与内边距随卡片宽度缩放，大小卡共用同一比例。
            val cardWidth = maxWidth.value
            val headerHeight = (cardWidth * 0.125f).coerceIn(38f, 52f).dp
            val headerPadding = (cardWidth * 0.09f).coerceIn(10f, 20f).dp
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeight)
                        .background(palette.header)
                        .padding(horizontal = headerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = event.icon,
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.headerContent,
                            maxLines = 1,
                        )
                        Spacer(Modifier.width(headerPadding * 0.5f))
                        Text(
                            text = fields.title,
                            // 标题占据剩余空间并省略，状态词永远不会被挤出屏幕。
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.headerContent,
                        )
                        Spacer(Modifier.width(headerPadding * 0.5f))
                        Text(
                            text = statusWord,
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.headerContent,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.body)
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // 位数越多数字越收敛；宽卡上字号随宽度放大，不再写死 38sp。
                    val digitFactor = when (amount.length) {
                        1, 2, 3 -> 1f
                        4 -> 0.85f
                        else -> 0.72f
                    }
                    val baseFontSize = when {
                        maxWidth < 120.dp -> 26f
                        maxWidth < 170.dp -> 32f
                        else -> (maxWidth.value * 0.115f).coerceIn(38f, 56f)
                    }
                    Text(
                        text = amount,
                        fontFamily = LantingheiTcHeavy,
                        fontSize = (baseFontSize * digitFactor).sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.bodyContent,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        softWrap = false,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.footer)
                        .padding(horizontal = headerPadding, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = fields.startTimeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.footerContent,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

private enum class LegacyTimelineDestination {
    BIG_EVENT,
    DAYBOOK,
}

@Composable
private fun TouchBar(
    palette: AdaptiveBackgroundPalette,
    modifier: Modifier = Modifier,
    selectedDestination: LegacyTimelineDestination,
    onSelectBigEvents: () -> Unit,
    onSelectDaybook: () -> Unit,
) {
    val selectedProgress by animateFloatAsState(
        targetValue = if (selectedDestination == LegacyTimelineDestination.BIG_EVENT) 0f else 1f,
        animationSpec = tween(durationMillis = 220),
        label = "timeline-selection",
    )
    val activeColor = palette.environmentColor.copy(alpha = if (palette.isDarkScene) 0.82f else 0.66f)
    val contentColor = palette.cardContentColor
    val railColor = contentColor.copy(alpha = 0.20f)
    val surfaceShape = RoundedCornerShape(30.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = 10.dp,
                shape = surfaceShape,
                ambientColor = palette.ambientShadowColor.copy(alpha = 0.14f),
                spotColor = palette.ambientShadowColor.copy(alpha = 0.18f),
            ),
        shape = surfaceShape,
        color = Color.Transparent,
        contentColor = contentColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.14f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            palette.cardHighlightColor.copy(alpha = 0.12f),
                            palette.transitionColor.copy(alpha = 0.18f),
                            palette.uiBaseColor.copy(alpha = 0.34f),
                        ),
                    ),
                    shape = surfaceShape,
                )
                .padding(horizontal = 16.dp, vertical = 9.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .padding(horizontal = 54.dp),
                ) {
                    val centerY = size.height / 2f
                    val startX = 0f
                    val endX = size.width
                    val centerX = (startX + endX) / 2f
                    val selectedX = startX + (endX - startX) * selectedProgress
                    drawLine(
                        color = railColor.copy(alpha = 0.48f),
                        start = Offset(startX, centerY),
                        end = Offset(endX, centerY),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                    drawLine(
                        color = activeColor.copy(alpha = 0.72f),
                        start = Offset(centerX, centerY),
                        end = Offset(
                            centerX + (startX - centerX) * (1f - selectedProgress),
                            centerY,
                        ),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                    drawLine(
                        color = activeColor.copy(alpha = 0.72f),
                        start = Offset(centerX, centerY),
                        end = Offset(
                            centerX + (endX - centerX) * selectedProgress,
                            centerY,
                        ),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                    drawCircle(
                        color = activeColor.copy(alpha = 0.28f),
                        radius = 5.dp.toPx(),
                        center = Offset(selectedX, centerY),
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    TimelineNode(
                        title = "大事件",
                        description = "大事件，当前主页入口",
                        selected = selectedDestination == LegacyTimelineDestination.BIG_EVENT,
                        activeColor = activeColor,
                        contentColor = contentColor,
                        onClick = onSelectBigEvents,
                    )
                    TimelineNode(
                        title = "日子簿",
                        description = "日子簿，打开日子簿页面",
                        selected = selectedDestination == LegacyTimelineDestination.DAYBOOK,
                        activeColor = activeColor,
                        contentColor = contentColor,
                        onClick = onSelectDaybook,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineNode(
    title: String,
    description: String,
    selected: Boolean,
    activeColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    val nodeColor by animateColorAsState(
        targetValue = if (selected) activeColor else contentColor.copy(alpha = 0.46f),
        animationSpec = tween(durationMillis = 220),
        label = "timeline-node-color",
    )
    val nodeSize by animateDpAsState(
        targetValue = if (selected) 18.dp else 13.dp,
        animationSpec = tween(durationMillis = 220),
        label = "timeline-node-size",
    )
    Column(
        modifier = Modifier
            .width(92.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(nodeColor.copy(alpha = 0.13f), CircleShape),
                )
            }
            Box(
                modifier = Modifier
                    .size(nodeSize)
                    .background(nodeColor, CircleShape)
                    .then(
                        if (selected) {
                            Modifier.border(2.dp, Color.White.copy(alpha = 0.72f), CircleShape)
                        } else {
                            Modifier
                        },
                    ),
            )
        }
        Text(
            text = title,
            color = contentColor.copy(alpha = if (selected) 0.96f else 0.66f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    onClearSearch: (() -> Unit)? = null,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (searchQuery.isBlank()) "这里还没有事件" else "没有找到匹配的事件",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (searchQuery.isBlank()) "从一个重要的日期开始记录时间。"
                else "试试更短的关键词，或清空搜索继续浏览。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (searchQuery.isNotBlank() && onClearSearch != null) {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onClearSearch) { Text("清空搜索") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EventSettingsScreen(
    event: TimeEvent,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onSubtitleChange: (String) -> Unit,
    onGroupLabelChange: (String) -> Unit,
    onTemplateChange: (EventCardTemplateKey) -> Unit,
    onTravelConfigChange: (TravelCardConfig) -> Unit,
    onTogglePinned: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    val paletteLabel = when (event.cardPaletteKey) {
        EventCardPaletteKey.BLUE_WHITE -> "蓝白"
        EventCardPaletteKey.ORANGE_WHITE -> "橙白"
    }
    val templateLabel = cardTemplateLabel(event.cardTemplateKey)
    val sizeLabel = eventSizeLabel(event.travelCardConfig?.size)
    val travelConfig = event.travelCardConfig ?: TravelCardConfig(title = event.title)
    var dateText by remember(event.id, travelConfig.targetDate) {
        mutableStateOf(travelConfig.targetDate.toString())
    }
    var showDeleteConfirmation by rememberSaveable(event.id) { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 6.dp,
            ) {
                TopAppBar(
                    title = { Text("事件设置") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回首页",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EventCard(event)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = event.isPinned,
                    onClick = onTogglePinned,
                    label = { Text(if (event.isPinned) "已置顶" else "置顶") },
                )
                TextButton(onClick = onArchive) { Text("归档") }
                TextButton(onClick = { showDeleteConfirmation = true }) { Text("删除") }
            }
            Text(
                "当前事件",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                value = event.title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("大标题") },
                singleLine = true,
            )
            OutlinedTextField(
                value = event.subtitle,
                onValueChange = onSubtitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("小标题（可选，部分样式隐藏）") },
                singleLine = true,
            )
            OutlinedTextField(
                value = event.groupLabel,
                onValueChange = onGroupLabelChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("分组（可选）") },
                singleLine = true,
            )
            Text(
                "选择卡片模板",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            CardTemplateOptions.forEach { template ->
                FilterChip(
                    modifier = Modifier.fillMaxWidth(),
                    selected = event.cardTemplateKey == template,
                    onClick = { onTemplateChange(template) },
                    label = { Text(cardTemplateLabel(template)) },
                )
            }
            if (event.cardTemplateKey != EventCardTemplateKey.CLASSIC) {
                Text(
                    "模板内容（实时预览）",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { value ->
                        dateText = value
                        parseTravelDate(value)?.let { date ->
                            onTravelConfigChange(travelConfig.copy(targetDate = date))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("目标日期（yyyy-MM-dd）") },
                    supportingText = { Text("卡片会自动显示为 yyyy.MM.dd 星期几") },
                    singleLine = true,
                )
                Text("顶部 icon", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TravelCardIcon.entries.forEach { icon ->
                        FilterChip(
                            selected = travelConfig.badgeIcon == icon,
                            onClick = { onTravelConfigChange(travelConfig.copy(badgeIcon = icon)) },
                            label = { Text(travelIconLabel(icon)) },
                            leadingIcon = {
                                Icon(travelIconVector(icon), contentDescription = null)
                            },
                        )
                    }
                }
                Text("尺寸", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TravelCardSize.entries.forEach { size ->
                        FilterChip(
                            selected = travelConfig.size == size,
                            onClick = { onTravelConfigChange(travelConfig.copy(size = size)) },
                            label = {
                                Text(if (size == TravelCardSize.WIDE) "Wide · 跨两列" else "Small · 单格")
                            },
                        )
                    }
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("事件名称") },
                        supportingContent = { Text(event.title) },
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("小标题") },
                        supportingContent = { Text(event.subtitle.ifBlank { "未设置（当前样式可隐藏）" }) },
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("起始时间") },
                        supportingContent = { Text(event.dateLabel.ifBlank { "未设置" }) },
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("分组") },
                        supportingContent = { Text(event.groupLabel.ifBlank { "未分组" }) },
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("卡片配色") },
                        supportingContent = { Text("当前：$paletteLabel，编辑入口后续接入") },
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("卡片模板") },
                        supportingContent = { Text("当前：$templateLabel，可在上方切换") },
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("主页尺寸") },
                        supportingContent = { Text("当前：$sizeLabel，编辑入口后续接入") },
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("模板组件") },
                        supportingContent = { Text("背景、时间、地点、图标均已预留为可替换字段") },
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("分类与分组") },
                        supportingContent = { Text("分类抽屉中配置，当前仅展示入口") },
                    )
                }
            }
            Text(
                "模板和旅行字段会立即反映到首页并保存到本地；完整新增、删除恢复和分组管理将在后续阶段开放。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (showDeleteConfirmation) {
        DeleteConfirmationDialog(
            title = "删除这个事件？",
            message = "事件会移入回收状态，不再显示在首页。",
            onConfirm = {
                showDeleteConfirmation = false
                onDelete()
            },
            onDismiss = { showDeleteConfirmation = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("显示", style = MaterialTheme.typography.titleMedium)
            Text("主题模式", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { onThemeModeChange(mode) },
                        label = {
                            Text(
                                when (mode) {
                                    ThemeMode.SYSTEM,
                                    ThemeMode.LIGHT,
                                    ThemeMode.DARK -> themeModeLabel(mode)
                                },
                            )
                        },
                    )
                }
            }
            Text("卡片排列", style = MaterialTheme.typography.titleMedium)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
            ) {
                ListItem(
                    headlineContent = { Text("主页模板布局") },
                    supportingContent = { Text("Small 每排 2 张；Wide 自动跨满 2 列") },
                )
            }
            Text(
                "模板尺寸由卡片自身控制：Small 是单格预览，Wide 是跨两列预览。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "卡片配色和字体颜色已预留结构，编辑入口将在后续事件编辑阶段接入。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onBack) { Text("返回首页") }
        }
    }
}

private val UndoToastSurface = Color(0xFFFFFCF9)
private val UndoToastInk = Color(0xFF4F4036)
private val UndoToastMuted = Color(0xFF9E8D7D)
private val UndoToastAccent = Color(0xFFCC6B4F)
private val UndoToastLine = Color(0xFFE9DCD0)
private val UndoToastFont = FontFamily(
    Font(com.cch.momentmark.R.font.noto_serif_sc_vf, FontWeight.Normal),
)

@Composable
private fun UndoDeleteToast(
    modifier: Modifier = Modifier,
    onUndo: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(UndoToastSurface)
            .border(0.5.dp, UndoToastLine, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 13.dp)
            .semantics {
                contentDescription = "事件已删除，可撤销"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "事件已删除",
            fontFamily = UndoToastFont,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = UndoToastInk,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(0.5.dp, UndoToastLine, RoundedCornerShape(12.dp))
                .clickable(onClick = onUndo)
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = "撤销删除"
                },
        ) {
            Text(
                "撤销",
                fontFamily = UndoToastFont,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = UndoToastAccent,
            )
        }
    }
}
