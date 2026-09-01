package com.cch.momentmark.ui.moment.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.cch.momentmark.domain.model.MomentCardState
import com.cch.momentmark.domain.model.TaskCardState
import com.cch.momentmark.domain.model.TaskType
import com.cch.momentmark.domain.time.EventTimeStatus
import com.cch.momentmark.ui.components.AchievementFrame
import com.cch.momentmark.ui.components.PixelHpBar
import com.cch.momentmark.ui.components.PixelPanel
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MomentMarkTokens
import java.time.format.DateTimeFormatter

/** J 卡片的日期角标格式：DUE/SINCE 用 2026.12.19 风格。 */
private val CardDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

/** 卡片徽章类型：主线=置顶，限时=临近，其余按展示状态。 */
private enum class MomentBadgeKind(val label: String) {
    MAIN("◆ 主线"),
    SIDE("◇ 支线"),
    LIMITED("⧗ 限时"),
    ACHIEVEMENT("🏆 成就"),
}

private fun MomentCardState.badgeKind(): MomentBadgeKind = when {
    status == EventTimeStatus.PAST -> MomentBadgeKind.ACHIEVEMENT
    isLimited -> MomentBadgeKind.LIMITED
    isPinned -> MomentBadgeKind.MAIN
    else -> MomentBadgeKind.SIDE
}

/** 语义节点合并为完整中文描述（DESIGN_SYSTEM §6.5）。 */
private fun MomentCardState.accessibilityDescription(): String {
    val countText = when (status) {
        EventTimeStatus.TODAY -> "就是今天"
        EventTimeStatus.FUTURE -> "还有 $days 天"
        EventTimeStatus.PAST -> "已经 $days 天"
    }
    val groupText = groupId?.let { "，分组 $it" } ?: ""
    return "$title，$countText，${anchorDate.format(CardDateFormatter)}$groupText"
}

/**
 * 置顶主线卡（首页最前方，独占一整行）：px4 面板；成就方向内嵌 2dp 金线。
 */
@Composable
fun MomentPinnedCard(
    card: MomentCardState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extended = LocalMmExtendedColors.current
    val isAchievement = card.status == EventTimeStatus.PAST
    val badgeColor = if (isAchievement) {
        extended.goldInk
    } else {
        extended.manaPurple
    }
    val numberColor = if (isAchievement) extended.goldInk else MaterialTheme.colorScheme.onSurface
    val frameModifier = modifier
        .fillMaxWidth()
        .clickable(role = Role.Button, onClick = onClick)
        .semantics(mergeDescendants = true) {
            contentDescription = card.accessibilityDescription()
        }
    val content: @Composable () -> Unit = {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(MomentMarkTokens.SpaceInner),
            verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // ① 的主线/成就 HUD 是一行完整语义；EXP/GOLD 等示例数据不落入业务。
                Text(
                    text = if (isAchievement) {
                        "🏆 ACHIEVEMENT 成就 · 正数"
                    } else {
                        "◆ MAIN QUEST 主线 · 倒数"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                )
                if (isAchievement) {
                    Text(
                        text = "★ 置顶",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(end = MomentMarkTokens.TouchTargetMin),
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit * 2),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = card.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                MomentNumberText(card = card, big = true, color = numberColor)
                if (isAchievement) {
                    card.nextMilestone?.let { milestone ->
                        PixelHpBar(
                            fraction = milestone.progressFraction ?: 0f,
                            fillColor = extended.goldInk,
                            modifier = Modifier.padding(top = MomentMarkTokens.SpaceUnit),
                        )
                        Text(
                            text = "下一里程碑 ${milestone.label} · 还有 ${milestone.daysRemaining} 天",
                            style = MaterialTheme.typography.labelSmall,
                            color = extended.labelTertiary,
                        )
                    }
                }
                }
                Text(
                    text = if (isAchievement) {
                        "SINCE\n${card.anchorDate.format(CardDateFormatter)}"
                    } else {
                        "UNLOCK\n${card.anchorDate.format(CardDateFormatter)}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = extended.labelTertiary,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
    if (isAchievement) {
        AchievementFrame(modifier = frameModifier) { content() }
    } else PixelPanel(
        modifier = frameModifier,
        variant = com.cch.momentmark.ui.components.PixelPanelVariant.Emphasized,
    ) {
        content()
    }
}

/** 双列网格 · 未来任务卡：大数字 + 血条 + mono 元数据行。 */
@Composable
fun MomentQuestCard(
    card: MomentCardState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extended = LocalMmExtendedColors.current
    PixelPanel(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = card.accessibilityDescription()
            },
        // HTML ① 的普通未来卡是 `.px`：3dp 墨线 + 3dp 实体影；
        // 只有置顶主线才使用 `.px4` 的强调级 4dp 影。
        variant = com.cch.momentmark.ui.components.PixelPanelVariant.Raised,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(MomentMarkTokens.SpaceInner),
            verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                MomentBadge(
                    kind = card.badgeKind(),
                    color = if (card.isLimited) extended.amber else MaterialTheme.colorScheme.secondary,
                )
            }
            MomentNumberText(card = card, big = false, color = MaterialTheme.colorScheme.onSurface)
            card.hpFraction?.let { PixelHpBar(fraction = it) }
            MomentMetaRow(
                prefix = if (card.status == EventTimeStatus.TODAY) "TODAY" else "DUE",
                dateText = card.anchorDate.format(CardDateFormatter),
                groupId = card.groupId,
            )
        }
    }
}

/**
 * 首页右列的待办卡。它是 Task 的只读投影，不提供 Moment 详情入口；编辑/完成仍在日子簿。
 */
@Composable
fun HomeTaskCard(
    card: TaskCardState,
    modifier: Modifier = Modifier,
) {
    val extended = LocalMmExtendedColors.current
    PixelPanel(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = card.accessibilityDescription()
            },
        backgroundColor = if (card.isCompleted) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surface
        },
        // Task 首页投影同样是普通 `.px` 卡，避免与置顶主线争夺视觉层级。
        variant = com.cch.momentmark.ui.components.PixelPanelVariant.Raised,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(MomentMarkTokens.SpaceInner),
            verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = card.taskType.badgeLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.border(
                        MomentMarkTokens.PxThinBorderWidth,
                        MaterialTheme.colorScheme.secondary,
                    ).padding(
                        horizontal = MomentMarkTokens.BadgePaddingHorizontal,
                        vertical = MomentMarkTokens.BadgePaddingVertical,
                    ),
                )
            }
            Text(
                text = when {
                    card.isCompleted -> "CLEAR"
                    card.daysUntilDue == 0L -> "就是今天"
                    else -> "${card.daysUntilDue} 天后"
                },
                style = MaterialTheme.typography.titleLarge,
                color = if (card.isCompleted) extended.labelTertiary else MaterialTheme.colorScheme.onSurface,
            )
            MomentMetaRow(
                prefix = if (card.daysUntilDue == 0L) "TODAY" else "DUE",
                dateText = card.dueLocalDate.format(CardDateFormatter),
                groupId = card.groupId,
            )
        }
    }
}

private fun TaskCardState.accessibilityDescription(): String {
    val status = when {
        isCompleted -> "已完成 CLEAR"
        daysUntilDue == 0L -> "就是今天"
        else -> "还有 $daysUntilDue 天"
    }
    val groupText = groupId?.let { "，分组 $it" } ?: ""
    return "待办 $title，$status，${dueLocalDate.format(CardDateFormatter)}$groupText"
}

private fun TaskType?.badgeLabel(): String = when (this) {
    TaskType.MAIN -> "◆ 待办主线"
    TaskType.LIMITED -> "⧗ 限时待办"
    TaskType.SIDE, null -> "◇ 待办"
}

/** 双列网格 · 过去成就卡：金框 + 金墨数字 + SINCE。 */
@Composable
fun MomentAchievementCard(
    card: MomentCardState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extended = LocalMmExtendedColors.current
    AchievementFrame(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = card.accessibilityDescription()
            },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(MomentMarkTokens.SpaceInner),
            verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MomentBadge(kind = MomentBadgeKind.ACHIEVEMENT, color = extended.goldInk)
                RarityStars(rarity = card.rarity)
            }
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            MomentNumberText(card = card, big = false, color = extended.goldInk)
            card.nextMilestone?.let { milestone ->
                PixelHpBar(
                    fraction = milestone.progressFraction ?: 0f,
                    fillColor = extended.goldInk,
                    modifier = Modifier.padding(top = MomentMarkTokens.SpaceUnit),
                )
                Text(
                    text = "下一里程碑 ${milestone.label} · 还有 ${milestone.daysRemaining} 天",
                    style = MaterialTheme.typography.labelSmall,
                    color = extended.labelTertiary,
                )
            }
            MomentMetaRow(
                prefix = "SINCE",
                dateText = card.anchorDate.format(CardDateFormatter),
                groupId = card.groupId,
            )
        }
    }
}

/** 大数字行：目标日当天显示「就是今天」，否则数字 + 单位文案。 */
@Composable
private fun MomentNumberText(card: MomentCardState, big: Boolean, color: Color) {
    if (card.status == EventTimeStatus.TODAY) {
        Text(
            text = "就是今天",
            style = if (big) {
                MaterialTheme.typography.headlineSmall
            } else {
                MaterialTheme.typography.titleLarge
            },
            color = color,
        )
        return
    }
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = card.days.toString(),
            style = if (big) {
                MaterialTheme.typography.displayMedium
            } else {
                MaterialTheme.typography.displaySmall
            },
            color = color,
        )
        Text(
            text = if (card.status == EventTimeStatus.PAST) " 天" else " 天后",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = MomentMarkTokens.SpaceUnit * 2),
        )
    }
}

/** mono 小徽章：2dp currentColor 描边（proposal_J_final .badge-tag）。 */
@Composable
private fun MomentBadge(kind: MomentBadgeKind, color: Color) {
    Text(
        text = kind.label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .border(MomentMarkTokens.PxThinBorderWidth, color)
            .padding(
                horizontal = MomentMarkTokens.BadgePaddingHorizontal,
                vertical = MomentMarkTokens.BadgePaddingVertical,
            ),
    )
}

/** mono 元数据行：DUE/SINCE 日期 + 分组。 */
@Composable
private fun MomentMetaRow(prefix: String, dateText: String, groupId: String?) {
    val meta = buildString {
        append(prefix)
        append(' ')
        append(dateText)
        if (!groupId.isNullOrBlank()) {
            append(" · ")
            append(groupId)
        }
    }
    Text(
        text = meta,
        style = MaterialTheme.typography.labelSmall,
        color = LocalMmExtendedColors.current.labelTertiary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** 重要度星级：填充金色、空星弱化色，最多 3 星；null 不渲染。 */
@Composable
private fun RarityStars(rarity: Int?) {
    if (rarity == null) return
    val filled = rarity.coerceIn(0, 3)
    Row {
        Text(
            text = "★".repeat(filled),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.tertiary,
        )
        if (filled < 3) {
            Text(
                text = "★".repeat(3 - filled),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}
