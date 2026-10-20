package com.cch.momentmark.ui.moment.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import com.cch.momentmark.domain.model.HomeCardItem
import com.cch.momentmark.domain.model.HomeMomentProjection
import androidx.compose.ui.semantics.semantics
import com.cch.momentmark.domain.model.MomentCardState
import com.cch.momentmark.domain.time.EventTimeStatus
import com.cch.momentmark.ui.components.PixelPanel
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MomentMarkTokens
import java.time.format.DateTimeFormatter

/** 第⑧屏背景列表元数据的日期格式：DUE/SINCE 12.19（同设计稿，不带年份）。 */
private val BackdropDateFormatter = DateTimeFormatter.ofPattern("MM.dd")

/**
 * 大事件首页（J ① · 昼间任务板）：置顶全宽 → 过去成就左列 → 未来 Moment/Task 右列。
 * 数据来自 MomentHomeViewModel 的只读投影；本页不读写 Room/DataStore。
 *
 * 第⑧屏分类抽屉（MAP FILTER）叠加在本页：左缘右滑呼出，scrim / ESC / 返回键关闭；
 * 搜索与筛选行为属 P1（PRD §7），当前只复刻视觉，不接筛选逻辑。
 */
@Composable
fun MomentHomeScreen(
    uiState: MomentHomeUiState,
    onMomentSelected: (String) -> Unit,
    onPinnedMove: (String, Int) -> Unit,
    onDrawerOpen: () -> Unit = {},
    onDrawerClose: () -> Unit = {},
    onOpenGroupManagement: () -> Unit = {},
    modifier: Modifier = Modifier,
    /** 真实模式的分组线路行（Room 分组 + Moment 计数，由 App 层装配）。 */
    partyRows: List<MapFilterPartyRow> = emptyList(),
    /** 仅 DEBUG 视觉回归：design.html 第⑧屏冻结帧；生产恒为 null。 */
    drawerFixture: MapFilterDrawerFixture? = null,
) {
    val drawerOpen = drawerFixture != null || uiState.drawerOpen
    // 抽屉的返回键关闭统一由 MomentMarkApp 的应用层 BackHandler 处理（需读 ViewModel 当前值
    // 以消化「左缘右滑 vs 系统返回手势」的竞争事件）；本页只处理 scrim / ESC 关闭。
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // 抽屉展开时 HUD 右槽随上下文切换为 MAP FILTER（design.html 第⑧屏 .statusbar）。
        HomeStatusHud(rightText = if (drawerOpen) "MAP FILTER" else "GOLD ×1,280")
        if (drawerOpen) {
            MapFilterSearchBar(onClose = onDrawerClose, modifier = Modifier.fillMaxWidth())
        }
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .excludeDrawerEdgeSystemGesture(enabled = !drawerOpen)
                .mapFilterEdgeSwipeToOpen(enabled = !drawerOpen, onOpen = onDrawerOpen)
        ) {
            if (drawerOpen) {
                MapFilterBackdropList(
                    cards = drawerFixture?.backdrop ?: backdropCardsFrom(uiState.projection),
                )
                MapFilterScrim(onClose = onDrawerClose, modifier = Modifier.matchParentSize())
                Box(
                    Modifier
                        .matchParentSize()
                        .padding(
                            start = MomentMarkTokens.MapFilterDrawerOffsetStart,
                            top = MomentMarkTokens.MapFilterDrawerOffsetTop,
                            bottom = MomentMarkTokens.MapFilterDrawerOffsetBottom,
                        )
                ) {
                    MapFilterDrawerPanel(
                        state = drawerFixture?.drawerState ?: MapFilterDrawerState(
                            totalCount = uiState.filterTotal,
                            partyRows = partyRows,
                        ),
                        onManageGroups = onOpenGroupManagement,
                        modifier = Modifier
                            .width(MomentMarkTokens.MapFilterDrawerWidth)
                            .fillMaxSize(),
                    )
                }
            } else {
                HomeCardContent(
                    uiState = uiState,
                    onMomentSelected = onMomentSelected,
                    onPinnedMove = onPinnedMove,
                )
            }
        }
    }
}

/**
 * 真实模式下抽屉背后的 35% 时刻列表：按首页阅读顺序（置顶 → 过去 → 未来）
 * 投影为 design.html 第⑧屏的整行卡（标题 + mono 元数据），纯展示派生。
 */
private fun backdropCardsFrom(projection: HomeMomentProjection): List<MapFilterBackdropCard> =
    (projection.pinned + projection.past + projection.future).map { card ->
        val date = card.anchorDate.format(BackdropDateFormatter)
        when (card.status) {
            EventTimeStatus.FUTURE -> MapFilterBackdropCard(card.title, "${card.days} DAYS · DUE $date")
            EventTimeStatus.TODAY -> MapFilterBackdropCard(card.title, "TODAY · DUE $date")
            EventTimeStatus.PAST -> MapFilterBackdropCard(card.title, "${card.days} DAYS · SINCE $date")
        }
    }

/**
 * 关闭态的滚动内容：原「HUD + 卡片列」中的卡片列（HUD 已上移为固定行，
 * 与 design.html 第①屏 `.statusbar` 在 `.home-scroll` 之外的结构一致）。
 */
@Composable
private fun HomeCardContent(
    uiState: MomentHomeUiState,
    onMomentSelected: (String) -> Unit,
    onPinnedMove: (String, Int) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            // 对应 HTML `.home-scroll{padding:0 12px 12px}`：顶部不留白，
            // 首卡紧接 HUD（状态栏），左右 12dp，底部 12dp。
            .padding(
                start = MomentMarkTokens.HomePagePaddingHorizontal,
                end = MomentMarkTokens.HomePagePaddingHorizontal,
                bottom = MomentMarkTokens.HomeScrollPaddingBottom,
            ),
    ) {
        when {
            uiState.isLoading -> Text(
                text = "▸ LOADING…",
                style = MaterialTheme.typography.labelSmall,
                color = LocalMmExtendedColors.current.labelTertiary,
            )

            uiState.isEmpty -> EmptyMomentHomePanel()

            else -> Column(
                verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCard),
            ) {
                uiState.projection.pinned.forEachIndexed { index, card ->
                    // 排序操作仍可用，但不再在主卡之间形成一张横跨整行的“按钮卡”。
                    // 叠加层保留 44dp 触摸目标，常规浏览时只占主卡右上角的紧凑像素区域。
                    Box(Modifier.fillMaxWidth()) {
                        if (card.status == EventTimeStatus.FUTURE) {
                            HomeMainQuestCard(
                                title = card.title,
                                days = card.days,
                                anchorDate = card.anchorDate,
                                semanticDescription = card.accessibilityDescriptionForHome(),
                                onClick = { onMomentSelected(card.id) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            MomentPinnedCard(card = card, onClick = { onMomentSelected(card.id) })
                        }
                        if (uiState.projection.pinned.size > 1) {
                            PinnedOrderControls(
                                card = card,
                                canMoveUp = index > 0,
                                canMoveDown = index < uiState.projection.pinned.lastIndex,
                                onMove = onPinnedMove,
                                modifier = Modifier.align(Alignment.TopEnd),
                            )
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCard),
                ) {
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCard),
                    ) {
                        uiState.projection.past.forEach { card ->
                            MomentAchievementCard(card = card, onClick = { onMomentSelected(card.id) })
                        }
                    }
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCard),
                    ) {
                        uiState.projection.rightItems.forEach { item ->
                            when (item) {
                                is HomeCardItem.FutureMoment -> MomentQuestCard(
                                    card = item.card,
                                    onClick = { onMomentSelected(item.card.id) },
                                )
                                is HomeCardItem.HomeTask -> HomeTaskCard(card = item.card)
                            }
                        }
                    }
                }
            }
        }
        uiState.errorMessage?.let { message ->
            Text(
                text = "⚠ $message",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

private fun MomentCardState.accessibilityDescriptionForHome(): String {
    val count = when (status) {
        EventTimeStatus.TODAY -> "就是今天"
        EventTimeStatus.FUTURE -> "还有 $days 天"
        EventTimeStatus.PAST -> "已经 $days 天"
    }
    return "$title，$count，${anchorDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd"))}${groupId?.let { "，分组 $it" } ?: ""}"
}

/** 置顶之间只允许相邻移动，避免把旧自由拖拽布局重新带回 P0 首页。 */
@Composable
private fun PinnedOrderControls(
    card: MomentCardState,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCompact, Alignment.End),
    ) {
        if (canMoveUp) {
            PinnedMoveButton(
                label = "↑",
                contentDescription = "上移 ${card.title}",
                onClick = { onMove(card.id, -1) },
            )
        }
        if (canMoveDown) {
            PinnedMoveButton(
                label = "↓",
                contentDescription = "下移 ${card.title}",
                onClick = { onMove(card.id, 1) },
            )
        }
    }
}

@Composable
private fun PinnedMoveButton(
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 保留 44dp 触摸目标，但把视觉按钮收敛为 HTML 风格的紧凑像素图标。
    Box(
        modifier = modifier
            .size(MomentMarkTokens.TouchTargetMin)
            .semantics { this.contentDescription = contentDescription }
            .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onClick),
    ) {
        Text(text = label, color = Color.Transparent)
    }
}

/** 对应 HTML `.statusbar{padding:11px 18px 5px}`：左右 18dp、上 11dp、下 5dp。 */
@Composable
private fun HomeStatusHud(rightText: String = "GOLD ×1,280") {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(
                start = MomentMarkTokens.HomeStatusHudPadding,
                end = MomentMarkTokens.HomeStatusHudPadding,
                top = MomentMarkTokens.HomeStatusHudPaddingTop,
                bottom = MomentMarkTokens.HomeStatusHudPaddingBottom,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("LV.07", style = MomentMarkTokens.HomeStatusHudStyle, color = LocalMmExtendedColors.current.labelTertiary)
        Text(rightText, style = MomentMarkTokens.HomeStatusHudStyle, color = LocalMmExtendedColors.current.labelTertiary)
    }
}

@Composable
private fun EmptyMomentHomePanel() {
    val extended = LocalMmExtendedColors.current
    PixelPanel(
        modifier = Modifier.fillMaxWidth(),
        shadowOffset = MomentMarkTokens.PxShadowOffsetLarge,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(MomentMarkTokens.SpaceInner),
            verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit * 2),
        ) {
            Text(
                text = "▸ BIG EVENTS · 昼间任务板",
                style = MaterialTheme.typography.labelSmall,
                color = extended.labelTertiary,
            )
            Text(
                text = "还没有铭刻的时刻",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "点按底部「＋ 新时刻」，选择未来倒数或过去正数，开始第一笔铭刻。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "▸ 置顶主线全宽 · 过去成就居左 · 未来任务/待办居右",
                style = MaterialTheme.typography.labelSmall,
                color = extended.labelTertiary,
            )
        }
    }
}
