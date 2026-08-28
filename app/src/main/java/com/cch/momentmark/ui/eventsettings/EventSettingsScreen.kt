package com.cch.momentmark.ui.eventsettings

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
import com.cch.momentmark.ui.home.EventCard
import com.cch.momentmark.ui.home.travelIconVector
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
