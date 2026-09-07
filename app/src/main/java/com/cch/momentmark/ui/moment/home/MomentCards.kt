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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
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
import java.time.LocalDate

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
    HomeAchievementCard(
        title = card.title,
        days = card.days,
        anchorDate = card.anchorDate,
        milestoneLabel = card.nextMilestone?.label,
        milestoneDaysRemaining = card.nextMilestone?.daysRemaining,
        milestoneProgress = card.nextMilestone?.progressFraction,
        isPinned = card.isPinned,
        onClick = onClick,
        modifier = modifier,
        semanticDescription = card.accessibilityDescription(),
    )
}

/**
 * 第①屏 `.ach` 的等值组件。元素顺序严格保持：HUD → 主信息 → HP → 元数据。
 */
@Composable
fun HomeAchievementCard(
    title: String = "和小满在一起 💞",
    days: Long = 1196,
    anchorDate: LocalDate = LocalDate.of(2023, 5, 20),
    milestoneLabel: String? = "1200 天",
    milestoneDaysRemaining: Long? = 4,
    milestoneProgress: Float? = 0.99f,
    isPinned: Boolean = true,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    semanticDescription: String? = null,
) {
    val extended = LocalMmExtendedColors.current
    val frameModifier = modifier
        .fillMaxWidth()
        .clickable(role = Role.Button, onClick = onClick)
        .semantics(mergeDescendants = true) {
            contentDescription = semanticDescription
                ?: "$title，已 $days 天，${anchorDate.format(CardDateFormatter)}${if (isPinned) "，已置顶" else ""}"
        }
    val content: @Composable () -> Unit = {
        Column(
            Modifier.fillMaxWidth().padding(
                start = MomentMarkTokens.HomeAchievementPaddingHorizontal,
                top = MomentMarkTokens.HomeAchievementPaddingTop,
                end = MomentMarkTokens.HomeAchievementPaddingHorizontal,
                bottom = MomentMarkTokens.HomeAchievementPaddingBottom,
            ),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "🏆 ACHIEVEMENT 成就 · 正数",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold, fontSize = MomentMarkTokens.HomeAchievementHudFontSize, lineHeight = 11.sp),
                    color = extended.goldInk,
                )
                if (isPinned) {
                    Text(
                        text = "★ 置顶",
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold, fontSize = MomentMarkTokens.HomeAchievementHudFontSize, lineHeight = 11.sp),
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(top = MomentMarkTokens.HomeAchievementMainGap),
            ) {
                Column {
                    Text(
                        text = title,
                        style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = MomentMarkTokens.HomeAchievementTitleFontSize, lineHeight = 17.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = days.toString(),
                            style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = MomentMarkTokens.HomeAchievementNumberFontSize, lineHeight = MomentMarkTokens.HomeAchievementNumberLineHeight),
                            color = extended.goldInk,
                        )
                        Text(
                            text = " 天",
                            style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = MomentMarkTokens.HomeAchievementUnitFontSize, lineHeight = 14.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = MomentMarkTokens.HomeAchievementUnitBaselineOffset),
                        )
                    }
                }
                Text(
                    text = "SINCE\n${anchorDate.format(CardDateFormatter)}",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 9.sp, lineHeight = 11.sp),
                    color = extended.labelTertiary,
                    textAlign = TextAlign.End,
                )
            }
            if (milestoneLabel != null && milestoneDaysRemaining != null && milestoneProgress != null) {
                PixelHpBar(
                    fraction = milestoneProgress,
                    fillColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = MomentMarkTokens.HomeAchievementHpGap),
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = MomentMarkTokens.HomeAchievementMetaGap),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "下一里程碑 $milestoneLabel · 还有 $milestoneDaysRemaining 天",
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold, fontSize = MomentMarkTokens.HomeAchievementMetaFontSize, lineHeight = 10.sp),
                        color = extended.labelTertiary,
                    )
                    Text(
                        text = "${(milestoneProgress * 100).toInt()}%",
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold, fontSize = MomentMarkTokens.HomeAchievementMetaFontSize, lineHeight = 10.sp),
                        color = extended.labelTertiary,
                    )
                }
            }
        }
    }
    AchievementFrame(modifier = frameModifier) { content() }
}

/**
 * 第①屏「主线任务横幅卡」（MAIN QUEST）的等值组件。
 * 元素顺序严格保持：HUD → 主信息行；数字与单位共用原 HTML 的 line-height 1.05。
 */
@Composable
fun HomeMainQuestCard(
    title: String = "春节 · 回家的日子",
    days: Long = 148,
    unitLabel: String = " 天后",
    anchorDate: LocalDate = LocalDate.of(2027, 2, 6),
    questLabel: String = "◆ MAIN QUEST 主线 · 倒数",
    expLabel: String? = "EXP +148",
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    semanticDescription: String? = null,
) {
    val extended = LocalMmExtendedColors.current
    val hudStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = MomentMarkTokens.HomeMainQuestHudFontSize,
        lineHeight = MomentMarkTokens.HomeMainQuestHudLineHeight,
    )
    val titleStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = MomentMarkTokens.HomeMainQuestTitleFontSize,
        lineHeight = MomentMarkTokens.HomeMainQuestTitleLineHeight,
    )
    val numberStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = MomentMarkTokens.HomeMainQuestNumberFontSize,
        lineHeight = MomentMarkTokens.HomeMainQuestNumberLineHeight,
    )
    val unitStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = MomentMarkTokens.HomeMainQuestUnitFontSize,
        lineHeight = MomentMarkTokens.HomeMainQuestUnitLineHeight,
    )
    val unlockStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = MomentMarkTokens.HomeMainQuestHudFontSize,
        lineHeight = MomentMarkTokens.HomeMainQuestHudLineHeight,
    )

    PixelPanel(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = semanticDescription
                    ?: "$title，还有 $days 天，${anchorDate.format(CardDateFormatter)}"
            },
        variant = com.cch.momentmark.ui.components.PixelPanelVariant.Emphasized,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MomentMarkTokens.HomeMainQuestPaddingHorizontal,
                    vertical = MomentMarkTokens.HomeMainQuestPaddingVertical,
                ),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = questLabel,
                    style = hudStyle,
                    color = extended.manaPurple,
                )
                expLabel?.let {
                    Text(
                        text = it,
                        style = hudStyle,
                        color = extended.labelTertiary,
                    )
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = MomentMarkTokens.HomeMainQuestMainGap),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = title,
                        style = titleStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = days.toString(),
                            style = numberStyle,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = unitLabel,
                            style = unitStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = "UNLOCK\n${anchorDate.format(CardDateFormatter)}",
                    style = unlockStyle,
                    color = extended.labelTertiary,
                )
            }
        }
    }
}

/**
 * 首页①双列小卡的唯一视觉骨架。它对应 HTML `.px` 的 DOM 顺序：
 * Header(title + badge) → value → hpbar（可选）→ DUE/SINCE 元数据。
 *
 * Moment 与 Task 只通过参数提供内容，不能在这里互换领域语义；进度条颜色
 * 由 [PixelHpBar] 按剩余比例实时选择绿/橙/红，长度始终是 [progressFraction]。
 */
@Composable
fun HomeSmallCard(
    title: String,
    value: String,
    unitLabel: String,
    badgeLabel: String,
    badgeColor: Color,
    metadata: String,
    progressFraction: Float? = null,
    progressColor: Color? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    semanticDescription: String? = null,
) {
    PixelPanel(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = semanticDescription ?: "$title，$value$unitLabel，$metadata"
            },
        variant = com.cch.momentmark.ui.components.PixelPanelVariant.Raised,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(
                    start = MomentMarkTokens.HomeSmallCardPaddingHorizontal,
                    top = MomentMarkTokens.HomeSmallCardPaddingTop,
                    end = MomentMarkTokens.HomeSmallCardPaddingHorizontal,
                    bottom = MomentMarkTokens.HomeSmallCardPaddingBottom,
                ),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = MomentMarkTokens.HomeSmallCardTitleFontSize,
                        lineHeight = MomentMarkTokens.HomeSmallCardTitleLineHeight,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = badgeLabel,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 8.sp,
                        lineHeight = 10.sp,
                    ),
                    color = badgeColor,
                    modifier = Modifier
                        .border(MomentMarkTokens.PxThinBorderWidth, badgeColor)
                        .padding(
                            horizontal = MomentMarkTokens.BadgePaddingHorizontal,
                            vertical = MomentMarkTokens.BadgePaddingVertical,
                        ),
                )
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(top = MomentMarkTokens.HomeSmallCardTitleToNumberGap),
            ) {
                Text(
                    text = value,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = MomentMarkTokens.HomeSmallCardNumberFontSize,
                        lineHeight = MomentMarkTokens.HomeSmallCardNumberLineHeight,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = unitLabel,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = MomentMarkTokens.HomeSmallCardUnitFontSize,
                        lineHeight = 12.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            progressFraction?.let { fraction ->
                PixelHpBar(
                    fraction = fraction,
                    fillColor = progressColor,
                    modifier = Modifier.padding(top = MomentMarkTokens.HomeSmallCardNumberToBarGap),
                )
            }
            Text(
                text = metadata,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = MomentMarkTokens.HomeSmallCardMetaFontSize,
                    lineHeight = MomentMarkTokens.HomeSmallCardMetaLineHeight,
                ),
                color = LocalMmExtendedColors.current.labelTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = MomentMarkTokens.HomeSmallCardBarToMetaGap),
            )
        }
    }
}

/** 双列网格 · 未来时刻卡：领域投影适配到共享小卡骨架。 */
@Composable
fun MomentQuestCard(
    card: MomentCardState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val badgeColor = if (card.isLimited) LocalMmExtendedColors.current.amber
    else MaterialTheme.colorScheme.secondary
    HomeSmallCard(
        title = card.title,
        value = if (card.status == EventTimeStatus.TODAY) "就是今天" else card.days.toString(),
        unitLabel = if (card.status == EventTimeStatus.TODAY) "" else " 天",
        badgeLabel = when (card.badgeKind()) {
            MomentBadgeKind.MAIN -> "◆"
            MomentBadgeKind.LIMITED -> "⧗"
            else -> "◇"
        },
        badgeColor = badgeColor,
        metadata = "${if (card.status == EventTimeStatus.TODAY) "TODAY" else "DUE"} ${card.anchorDate.format(CardDateFormatter)}" +
            (card.groupId?.let { " · $it" } ?: ""),
        progressFraction = card.hpFraction,
        onClick = onClick,
        modifier = modifier,
        semanticDescription = card.accessibilityDescription(),
    )
}

/**
 * 首页右列的待办卡。它是 Task 的只读投影，不提供 Moment 详情入口；编辑/完成仍在日子簿。
 */
@Composable
fun HomeTaskCard(
    card: TaskCardState,
    modifier: Modifier = Modifier,
) {
    HomeSmallCard(
        title = card.title,
        value = when {
            card.isCompleted -> "CLEAR"
            card.daysUntilDue == 0L -> "就是今天"
            else -> card.daysUntilDue.toString()
        },
        unitLabel = if (card.isCompleted || card.daysUntilDue == 0L) "" else " 天后",
        badgeLabel = card.taskType.badgeLabel(),
        badgeColor = MaterialTheme.colorScheme.secondary,
        metadata = "${if (card.daysUntilDue == 0L) "TODAY" else "DUE"} ${card.dueLocalDate.format(CardDateFormatter)}" +
            (card.groupId?.let { " · $it" } ?: ""),
        onClick = {},
        modifier = modifier,
        semanticDescription = card.accessibilityDescription(),
    )
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
