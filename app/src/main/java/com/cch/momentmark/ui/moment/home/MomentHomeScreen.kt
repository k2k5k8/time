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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import com.cch.momentmark.domain.model.HomeCardItem
import androidx.compose.ui.semantics.semantics
import com.cch.momentmark.domain.model.MomentCardState
import com.cch.momentmark.ui.components.PixelPanel
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MomentMarkTokens

/**
 * 大事件首页（J ① · 昼间任务板）：置顶全宽 → 过去成就左列 → 未来 Moment/Task 右列。
 * 数据来自 MomentHomeViewModel 的只读投影；本页不读写 Room/DataStore。
 */
@Composable
fun MomentHomeScreen(
    uiState: MomentHomeUiState,
    onMomentSelected: (String) -> Unit,
    onPinnedMove: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extended = LocalMmExtendedColors.current
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = MomentMarkTokens.SpacePage,
                vertical = MomentMarkTokens.SpaceCard,
            ),
        verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCard),
    ) {
        when {
            uiState.isLoading -> Text(
                text = "▸ LOADING…",
                style = MaterialTheme.typography.labelSmall,
                color = extended.labelTertiary,
            )

            uiState.isEmpty -> EmptyMomentHomePanel()

            else -> {
                uiState.projection.pinned.forEachIndexed { index, card ->
                    // 排序操作仍可用，但不再在主卡之间形成一张横跨整行的“按钮卡”。
                    // 叠加层保留 44dp 触摸目标，常规浏览时只占主卡右上角的紧凑像素区域。
                    Box(Modifier.fillMaxWidth()) {
                        MomentPinnedCard(card = card, onClick = { onMomentSelected(card.id) })
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
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        PixelPanel(
            modifier = Modifier
                .align(Alignment.Center)
                .size(MomentMarkTokens.CompactIconSize),
            backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
            shadowColor = MaterialTheme.colorScheme.outline,
            shadowOffset = MomentMarkTokens.PxShadowOffsetSmall,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(MomentMarkTokens.SpaceUnit * 2),
            )
        }
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
