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
internal fun EmptyState(
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

private val UndoToastSurface = Color(0xFFFFFCF9)
private val UndoToastInk = Color(0xFF4F4036)
private val UndoToastMuted = Color(0xFF9E8D7D)
private val UndoToastAccent = Color(0xFFCC6B4F)
private val UndoToastLine = Color(0xFFE9DCD0)
private val UndoToastFont = FontFamily(
    Font(com.cch.momentmark.R.font.noto_serif_sc_vf, FontWeight.Normal),
)

@Composable
internal fun UndoDeleteToast(
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
