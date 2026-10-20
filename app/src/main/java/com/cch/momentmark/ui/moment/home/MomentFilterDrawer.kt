package com.cch.momentmark.ui.moment.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.cch.momentmark.data.repository.Group
import com.cch.momentmark.ui.components.PixelPanel
import com.cch.momentmark.ui.components.pixelGroupEmoji
import com.cch.momentmark.ui.components.homeGroupIconKeyForName
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MmNoFontPadding
import com.cch.momentmark.ui.theme.MomentMarkTokens

/**
 * 第⑧屏「分类抽屉 · 搜索」的展示数据（design.html 第 ⑧ 屏）。
 * 全部是只读派生投影：计数、分组与状态都不落库；搜索与筛选行为属 P1（PRD §7），
 * 当前只复刻视觉，不给 chips/分组行接任何筛选逻辑。
 */
data class MapFilterPartyRow(
    val id: String,
    val name: String,
    val iconKey: String,
    val count: Int,
)

/** 抽屉背后 35% 透明度列表的一行：标题 + mono 元数据。 */
data class MapFilterBackdropCard(
    val title: String,
    val meta: String,
)

data class MapFilterDrawerState(
    /** 状态段「全部 ×N」的活动 Moment 总数。 */
    val totalCount: Int,
    val partyRows: List<MapFilterPartyRow>,
    /**
     * 仅视觉回归 fixture 使用：设计稿中被线路色描边选中的分组行（💞 生活 · danger）。
     * 真实运行时无筛选状态，恒为 null。
     */
    val selectedPartyRowId: String? = null,
    /** 仅 fixture：设计稿首行（🎓 学业）使用墨色文字的稿面特殊态。 */
    val prominentPartyRowId: String? = null,
)

/** 第⑧屏整帧冻结数据（背景列表 + 抽屉），仅 DEBUG 视觉回归使用。 */
data class MapFilterDrawerFixture(
    val backdrop: List<MapFilterBackdropCard>,
    val drawerState: MapFilterDrawerState,
)

/**
 * 真实模式的数据装配：Room 分组按 `sortOrder` 排列，计数来自首页 ViewModel 的
 * `filterCounts`（Moment.groupId 目前保存分组名，故按名称匹配）；仍被 Moment 引用
 * 但不在 Room `groups` 中的组名追加在末尾，保证抽屉不丢真实存在的线路。
 */
fun buildMapFilterPartyRows(
    groups: List<Group>,
    counts: Map<String, Int>,
): List<MapFilterPartyRow> {
    val rows = groups.sortedBy { it.sortOrder }.map { group ->
        MapFilterPartyRow(
            id = group.name,
            name = group.name,
            iconKey = group.iconKey,
            count = counts[group.name] ?: 0,
        )
    }
    val knownNames = groups.map { it.name }.toSet()
    val extraNames = counts.keys
        .filter { it.isNotBlank() && it !in knownNames }
        .sorted()
    return rows + extraNames.map { name ->
        MapFilterPartyRow(
            id = name,
            name = name,
            iconKey = homeGroupIconKeyForName(name),
            count = counts[name] ?: 0,
        )
    }
}

/**
 * 搜索栏（design.html 第⑧屏顶部白框）：⌕ 图标 + placeholder + ESC 徽章。
 * 搜索行为属 P1 未设计——输入区只是占位展示，不接收输入；ESC 与 scrim 一样承担关闭。
 */
@Composable
fun MapFilterSearchBar(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extended = LocalMmExtendedColors.current
    Column(
        modifier
            .fillMaxWidth()
            .padding(
                start = MomentMarkTokens.MapFilterSearchContainerPaddingHorizontal,
                end = MomentMarkTokens.MapFilterSearchContainerPaddingHorizontal,
            ),
    ) {
        PixelPanel(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = extended.inputSurface,
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = MomentMarkTokens.MapFilterSearchPanelPaddingVertical,
                        horizontal = MomentMarkTokens.MapFilterSearchPanelPaddingHorizontal,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.MapFilterSearchRowGap),
            ) {
                Text(
                    text = "⌕",
                    style = TextStyle(
                        fontSize = MomentMarkTokens.MapFilterSearchIconFontSize,
                        lineHeight = MomentMarkTokens.MapFilterSearchIconLineHeight,
                        platformStyle = MmNoFontPadding,
                    ),
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = "搜索任务与成就…",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = MomentMarkTokens.MapFilterSearchPlaceholderFontSize,
                        lineHeight = MomentMarkTokens.MapFilterSearchPlaceholderLineHeight,
                        platformStyle = MmNoFontPadding,
                    ),
                    color = extended.labelTertiary,
                    modifier = Modifier.weight(1f),
                )
                MapFilterEscBadge(onClose)
            }
        }
        Spacer(Modifier.height(MomentMarkTokens.MapFilterSearchContainerPaddingBottom))
    }
}

@Composable
private fun MapFilterEscBadge(onClose: () -> Unit) {
    val extended = LocalMmExtendedColors.current
    Box(
        Modifier
            .border(
                width = MomentMarkTokens.MapFilterEscBadgeBorderWidth,
                color = MaterialTheme.colorScheme.outline,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClose,
            )
            .semantics { contentDescription = "退出筛选" },
    ) {
        Text(
            text = "ESC",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = MomentMarkTokens.MapFilterEscBadgeFontSize,
                lineHeight = MomentMarkTokens.MapFilterEscBadgeLineHeight,
                platformStyle = MmNoFontPadding,
            ),
            color = extended.labelTertiary,
            modifier = Modifier.padding(
                vertical = MomentMarkTokens.MapFilterEscBadgePaddingVertical,
                horizontal = MomentMarkTokens.MapFilterEscBadgePaddingHorizontal,
            ),
        )
    }
}

/**
 * 抽屉背后 35% 透明度的时刻列表（design.html `opacity:.35` 的 `.px` 行）。
 * 真实模式投影首页 Moment；fixture 模式注入设计稿冻结行。
 */
@Composable
fun MapFilterBackdropList(
    cards: List<MapFilterBackdropCard>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .alpha(MomentMarkTokens.MapFilterBackdropAlpha)
            .verticalScroll(rememberScrollState())
            .padding(
                top = MomentMarkTokens.MapFilterBackdropPaddingTop,
                start = MomentMarkTokens.MapFilterBackdropPaddingHorizontal,
                end = MomentMarkTokens.MapFilterBackdropPaddingHorizontal,
                bottom = MomentMarkTokens.MapFilterBackdropPaddingTop,
            ),
        verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.MapFilterBackdropGap),
    ) {
        cards.forEach { card ->
            MapFilterBackdropCardRow(card)
        }
    }
}

@Composable
private fun MapFilterBackdropCardRow(card: MapFilterBackdropCard) {
    val extended = LocalMmExtendedColors.current
    PixelPanel(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = MomentMarkTokens.MapFilterBackdropCardPaddingVertical,
                    horizontal = MomentMarkTokens.MapFilterBackdropCardPaddingHorizontal,
                ),
        ) {
            Text(
                text = card.title,
                style = TextStyle(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = MomentMarkTokens.MapFilterBackdropTitleFontSize,
                    lineHeight = MomentMarkTokens.MapFilterBackdropTitleLineHeight,
                    platformStyle = MmNoFontPadding,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = card.meta,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = MomentMarkTokens.MapFilterBackdropMetaFontSize,
                    lineHeight = MomentMarkTokens.MapFilterBackdropMetaLineHeight,
                    platformStyle = MmNoFontPadding,
                ),
                color = extended.labelTertiary,
                modifier = Modifier.padding(top = MomentMarkTokens.MapFilterBackdropMetaTopGap),
            )
        }
    }
}

/** 压暗层 `rgba(43,38,32,.45)`：点击关闭抽屉，并给出中文语义。 */
@Composable
fun MapFilterScrim(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .background(MaterialTheme.colorScheme.outline.copy(alpha = MomentMarkTokens.MapFilterScrimAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClose,
            )
            .semantics { contentDescription = "关闭地图筛选" },
    )
}

/**
 * 左侧抽屉面板（`.px4`，left 10 / top 6 / bottom 6 / width 222）：
 * 标题 → 状态段（全部/倒数中/正数中，选中=墨底金字）→ 分组线路行 → 底部「管理分组」。
 * 状态与分组行是纯展示（筛选属 P1）；「⚙ 管理分组 →」进入系统设置的队伍编成。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapFilterDrawerPanel(
    state: MapFilterDrawerState,
    onManageGroups: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PixelPanel(
        modifier = modifier,
        variant = com.cch.momentmark.ui.components.PixelPanelVariant.Emphasized,
        // 抽屉高度由外层 top/bottom 锚定；内容可能更矮，描边必须撑满整块。
        fillContent = true,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(MomentMarkTokens.MapFilterDrawerPadding)
                .semantics { contentDescription = "地图筛选" },
            verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.MapFilterDrawerGap),
        ) {
            Text(
                text = "▸ 地图筛选 FILTER",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = MomentMarkTokens.MapFilterDrawerTitleFontSize,
                    lineHeight = MomentMarkTokens.MapFilterDrawerTitleLineHeight,
                    letterSpacing = MomentMarkTokens.MapFilterDrawerTitleLetterSpacing,
                    platformStyle = MmNoFontPadding,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Column {
                MapFilterSectionLabel(text = "状态 STATUS")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.MapFilterChipRowGap),
                    verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.MapFilterChipRowGap),
                ) {
                    MapFilterStatusChip(text = "全部 ×${state.totalCount}", selected = true)
                    MapFilterStatusChip(text = "倒数中", selected = false)
                    MapFilterStatusChip(text = "正数中", selected = false)
                }
            }
            Column {
                MapFilterSectionLabel(text = "分组 PARTY LINES")
                Column(
                    verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.MapFilterPartyRowGap),
                ) {
                    state.partyRows.forEach { row ->
                        MapFilterPartyLineRow(
                            row = row,
                            selected = state.selectedPartyRowId == row.id,
                            prominent = state.prominentPartyRowId == row.id,
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            MapFilterManageButton(onClick = onManageGroups)
        }
    }
}

@Composable
private fun MapFilterSectionLabel(text: String) {
    Text(
        text = text,
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold,
            fontSize = MomentMarkTokens.MapFilterSectionLabelFontSize,
            lineHeight = MomentMarkTokens.MapFilterSectionLabelLineHeight,
            letterSpacing = MomentMarkTokens.MapFilterSectionLabelLetterSpacing,
            platformStyle = MmNoFontPadding,
        ),
        color = LocalMmExtendedColors.current.labelTertiary,
        modifier = Modifier.padding(bottom = MomentMarkTokens.MapFilterSectionLabelBottomGap),
    )
}

/**
 * 状态 chip：mono 9px/800、2.5dp 描边、4px 8px 内留白，无硬影（设计稿 span 无 box-shadow）。
 * 选中 = 墨底金字（与底部导航选中项同语义）；纯展示，不接筛选（P1）。
 */
@Composable
private fun MapFilterStatusChip(text: String, selected: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Text(
        text = text,
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold,
            fontSize = MomentMarkTokens.MapFilterChipFontSize,
            lineHeight = MomentMarkTokens.MapFilterChipLineHeight,
            platformStyle = MmNoFontPadding,
        ),
        color = if (selected) scheme.tertiary else scheme.onSurfaceVariant,
        modifier = Modifier
            .then(
                if (selected) {
                    Modifier.background(scheme.inverseSurface)
                } else {
                    Modifier
                },
            )
            .border(
                width = MomentMarkTokens.MapFilterChipBorderWidth,
                color = scheme.outline,
            )
            .padding(
                vertical = MomentMarkTokens.MapFilterChipPaddingVertical,
                horizontal = MomentMarkTokens.MapFilterChipPaddingHorizontal,
            ),
    )
}

/**
 * 分组线路行：emoji + 名称 + ×N 计数，2.5dp 描边，SpaceBetween 布局。
 * 真实模式恒为未选中（无筛选状态）；fixture 用 selected/prominent 还原设计稿稿面。
 */
@Composable
private fun MapFilterPartyLineRow(
    row: MapFilterPartyRow,
    selected: Boolean,
    prominent: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    val rowStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.ExtraBold,
        fontSize = MomentMarkTokens.MapFilterPartyRowFontSize,
        lineHeight = MomentMarkTokens.MapFilterPartyRowLineHeight,
        platformStyle = MmNoFontPadding,
    )
    Row(
        Modifier
            .fillMaxWidth()
            .border(
                width = MomentMarkTokens.MapFilterPartyRowBorderWidth,
                color = if (selected) scheme.error else scheme.outline,
            )
            .padding(
                vertical = MomentMarkTokens.MapFilterPartyRowPaddingVertical,
                horizontal = MomentMarkTokens.MapFilterPartyRowPaddingHorizontal,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "${pixelGroupEmoji(row.iconKey)} ${row.name}",
            style = rowStyle,
            color = when {
                selected -> scheme.error
                prominent -> scheme.onSurface
                else -> scheme.onSurfaceVariant
            },
        )
        Text(
            text = "×${row.count}",
            style = rowStyle,
            color = if (selected) scheme.error else LocalMmExtendedColors.current.labelTertiary,
        )
    }
}

/** 「⚙ 管理分组 →」ghost 按钮：panel 面 + 墨线 + 3dp 硬影 + 按压位移（DESIGN_SYSTEM §3.4）。 */
@Composable
private fun MapFilterManageButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    PixelPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = "管理分组" },
        pressInteractionSource = interactionSource,
    ) {
        Text(
            text = "⚙ 管理分组 →",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = MomentMarkTokens.MapFilterManageButtonFontSize,
                lineHeight = MomentMarkTokens.MapFilterManageButtonLineHeight,
                platformStyle = MmNoFontPadding,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MomentMarkTokens.MapFilterManageButtonPaddingVertical),
        )
    }
}

/**
 * 首页内容区任意位置右滑呼出抽屉：水平累计位移达到
 * [MomentMarkTokens.MapFilterOpenDragDistance] 时立即触发 [onOpen]，不必等手指抬起。
 * 只在抽屉关闭时挂载，避免与 scrim/抽屉自身的手势竞争。
 *
 * 手势在触及 touch slop 前不消费事件。达到 slop 后，只有横向位移大于纵向位移的右滑才
 * 认领事件，因此正常的纵向滚动和卡片点按不受影响。它直接累加每一帧的原始位移，避免
 * `detectHorizontalDragGestures` 只从 slop 后开始统计、或等到抬手才触发带来的漏判。
 * [excludeDrawerEdgeSystemGesture] 仍仅用于左侧系统返回区域；从屏幕中部起手不受该区域影响。
 */
fun Modifier.mapFilterEdgeSwipeToOpen(
    enabled: Boolean,
    onOpen: () -> Unit,
): Modifier = this.pointerInput(enabled) {
    if (!enabled) return@pointerInput
    val thresholdPx = MomentMarkTokens.MapFilterOpenDragDistance.toPx()
    val touchSlopPx = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var accumulatedX = 0f
        var accumulatedY = 0f
        var horizontalRightDrag = false

        while (true) {
            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
            val delta = change.positionChange()
            accumulatedX += delta.x
            accumulatedY += delta.y

            if (!horizontalRightDrag) {
                val hasPassedSlop = kotlin.math.abs(accumulatedX) >= touchSlopPx ||
                    kotlin.math.abs(accumulatedY) >= touchSlopPx
                if (hasPassedSlop) {
                    if (accumulatedX <= 0f || kotlin.math.abs(accumulatedX) <= kotlin.math.abs(accumulatedY)) {
                        break
                    }
                    horizontalRightDrag = true
                }
            }

            if (horizontalRightDrag) {
                change.consume()
                if (accumulatedX >= thresholdPx) {
                    onOpen()
                    break
                }
            }

            if (!change.pressed) break
        }
    }
}

/**
 * 抽屉关闭时把左缘条带从**系统手势**（返回手势）中排除。
 *
 * 手势导航下系统在左缘保留一块优先手势区（`WindowInsets.systemGestures`，实机约 24~32dp），
 * 该区域内的滑动会先被系统当作「返回」吃掉。这里把固定的左缘兼容条带从系统手势中排除
 * （[Modifier.systemGestureExclusion] → `setSystemGestureExclusionRects`），
 * 左缘滑动交还应用内手势；抽屉展开时保持排除关闭，左缘返回手势仍可正常关闭抽屉。
 */
@Composable
fun Modifier.excludeDrawerEdgeSystemGesture(enabled: Boolean): Modifier {
    if (!enabled) return this
    val density = LocalDensity.current
    val zonePx = with(density) { MomentMarkTokens.MapFilterSystemGestureExclusionWidth.toPx() }
    val systemLeftPx = WindowInsets.systemGestures.getLeft(density, LocalLayoutDirection.current).toFloat()
    val exclusionWidthPx = maxOf(zonePx, systemLeftPx)
    return this.systemGestureExclusion { coordinates ->
        Rect(0f, 0f, exclusionWidthPx, coordinates.size.height.toFloat())
    }
}
