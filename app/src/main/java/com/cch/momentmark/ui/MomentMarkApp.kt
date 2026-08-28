package com.cch.momentmark.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
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
import com.cch.momentmark.domain.model.PrototypeDaybookDataSource
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
import com.cch.momentmark.ui.recyclebin.RecycleBinFeature
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
import com.cch.momentmark.ui.home.UndoDeleteToast
import com.cch.momentmark.ui.app.MomentMarkAppViewModel
import com.cch.momentmark.ui.app.MomentMarkAppViewModelFactory
import androidx.lifecycle.ViewModelProvider
private enum class AppScreen {
    HOME,
    DAYBOOK,
    EVENT_DETAIL,
    RELATED_EDITOR,
    EVENT_DETAIL_EDIT,
    SETTINGS,
    GROUP_MANAGEMENT,
    RECYCLE_BIN,
    EVENT_SETTINGS,
    EVENT_CREATE,
}

internal enum class EventFilter(val label: String) {
    ALL("全部"),
    FUTURE("未来"),
    PAST("过去"),
    PINNED("置顶"),
}

private fun Context.requireComponentActivity(): ComponentActivity {
    var current: Context = this
    while (true) {
        if (current is ComponentActivity) return current
        val base = (current as? ContextWrapper)?.baseContext ?: break
        if (base === current) break
        current = base
    }
    error("MomentMarkApp must be hosted by a ComponentActivity")
}

@Composable
fun MomentMarkApp() {
    val context = LocalContext.current
    val activity = remember(context) { context.requireComponentActivity() }
    val appViewModel = remember(activity) {
        ViewModelProvider(
            activity,
            MomentMarkAppViewModelFactory(context.applicationContext),
        )[MomentMarkAppViewModel::class.java]
    }
    val uiState by appViewModel.uiState.collectAsState()
    val detailStore = appViewModel.detailStore
        ?: error("MomentMarkAppViewModel is missing EventDetailStore")
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

    LaunchedEffect(pendingUndoId) {
        if (pendingUndoId != null) {
            // Give the user a brief window to tap undo without lingering on screen.
            delay(4_000)
            pendingUndoId = null
        }
    }
    val homeEvents = uiState.events
    val daybookDataSource = remember(homeEvents) {
        PrototypeDaybookDataSource(homeEvents)
    }
    val groupItems = drawerGroups(homeEvents, uiState.groups)
    LaunchedEffect(selectedGroup, groupItems) {
        if (selectedGroup != null && groupItems.none { it.name == selectedGroup }) {
            selectedGroup = null
        }
    }

    MomentMarkTheme(themeMode = uiState.themeMode) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            scrimColor = Color(0x47463B34),
            drawerContent = {
                CategoryDrawer(
                    events = homeEvents,
                    deletedCount = uiState.deletedEvents.size,
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
                    onOpenRecycleBin = {
                        scope.launch { drawerState.close() }
                        screen = AppScreen.RECYCLE_BIN
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
                    dataSource = daybookDataSource,
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
                                appViewModel.saveEvent(updated)
                                screen = AppScreen.EVENT_DETAIL
                            },
                            onDelete = {
                                val deletedId = selectedEvent.id
                                appViewModel.softDeleteEvent(deletedId)
                                selectedEventId = null
                                screen = AppScreen.HOME
                                pendingUndoId = deletedId
                            },
                        )
                    }
                }

                AppScreen.SETTINGS -> SettingsFeature(
                    themeMode = uiState.themeMode,
                    onThemeModeChange = { appViewModel.setThemeMode(it) },
                    onBack = { screen = AppScreen.HOME },
                )

                AppScreen.RECYCLE_BIN -> RecycleBinFeature(
                    deletedEvents = uiState.deletedEvents,
                    onBack = { screen = AppScreen.HOME },
                    onRestore = { appViewModel.restoreDeleted(it) },
                    onPermanentlyDelete = { appViewModel.permanentlyDelete(it) },
                    onPurge = { appViewModel.purgeDeleted() },
                )


                AppScreen.GROUP_MANAGEMENT -> GroupManagementScreen(
                    groups = groupItems,
                    onBack = { screen = AppScreen.HOME },
                    onCreate = { name -> appViewModel.createGroup(name) },
                    onRename = { oldName, newName ->
                        if (selectedGroup == oldName) selectedGroup = newName
                        appViewModel.renameGroup(oldName, newName)
                    },
                    onDelete = { name ->
                        if (selectedGroup == name) selectedGroup = null
                        appViewModel.deleteGroup(name)
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
                                    appViewModel.saveEvent(
                                        selectedEvent.copy(
                                            title = title,
                                            travelCardConfig = selectedEvent.travelCardConfig
                                                ?.copy(title = title),
                                        ),
                                    )
                                }
                            },
                            onSubtitleChange = { subtitle ->
                                if (homeEvents.any { it.id == selectedEvent.id }) {
                                    appViewModel.saveEvent(
                                        selectedEvent.copy(
                                            subtitle = subtitle,
                                            travelCardConfig = selectedEvent.travelCardConfig
                                                ?.copy(badgeLabel = subtitle),
                                        ),
                                    )
                                }
                            },
                            onGroupLabelChange = { groupLabel ->
                                if (homeEvents.any { it.id == selectedEvent.id }) {
                                    appViewModel.saveEvent(
                                        selectedEvent.copy(
                                            groupLabel = groupLabel,
                                            travelCardConfig = selectedEvent.travelCardConfig
                                                ?.copy(groupLabel = groupLabel),
                                        ),
                                    )
                                }
                            },
                            onTemplateChange = { template ->
                                templateOverrides = templateOverrides + (selectedEvent.id to template)
                                if (homeEvents.any { it.id == selectedEvent.id }) {
                                    appViewModel.saveEvent(selectedEvent.copy(cardTemplateKey = template))
                                }
                            },
                            onTravelConfigChange = { config ->
                                travelConfigOverrides = travelConfigOverrides + (selectedEvent.id to config)
                                if (homeEvents.any { it.id == selectedEvent.id }) {
                                    appViewModel.saveEvent(
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
                            },
                            onTogglePinned = {
                                if (homeEvents.any { it.id == selectedEvent.id }) {
                                    appViewModel.setPinned(
                                        id = selectedEvent.id,
                                        pinned = !selectedEvent.isPinned,
                                    )
                                }
                            },
                            onArchive = {
                                if (homeEvents.any { it.id == selectedEvent.id }) {
                                    appViewModel.archiveEvent(selectedEvent.id)
                                    selectedEventId = null
                                    screen = AppScreen.HOME
                                }
                            },
                            onDelete = {
                                if (homeEvents.any { it.id == selectedEvent.id }) {
                                    val deletedId = selectedEvent.id
                                    appViewModel.softDeleteEvent(deletedId)
                                    selectedEventId = null
                                    screen = AppScreen.HOME
                                    pendingUndoId = deletedId
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
                            appViewModel.saveEvent(event)
                            val returnScreen = runCatching {
                                AppScreen.valueOf(eventCreateReturnScreenName)
                            }.getOrDefault(AppScreen.HOME)
                            selectedDaybookDateText = null
                            screen = returnScreen
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
                            appViewModel.restoreDeleted(deletedId)
                        },
                    )
                }
            }
        }
    }
}
