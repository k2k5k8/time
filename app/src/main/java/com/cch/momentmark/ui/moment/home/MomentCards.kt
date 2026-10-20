package com.cch.momentmark.ui.moment.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import com.cch.momentmark.domain.model.MomentCardState
import com.cch.momentmark.domain.model.TaskCardState
import com.cch.momentmark.domain.model.TaskType
import com.cch.momentmark.domain.time.EventTimeStatus
import com.cch.momentmark.ui.components.AchievementFrame
import com.cch.momentmark.ui.components.PixelHpBar
import com.cch.momentmark.ui.components.resolveHpAccentColor
import com.cch.momentmark.ui.components.PixelIconSlot
import com.cch.momentmark.ui.components.PixelBadge
import com.cch.momentmark.ui.components.PixelBadgeVariant
import com.cch.momentmark.ui.components.PixelPanel
import com.cch.momentmark.ui.components.PixelStarRating
import com.cch.momentmark.ui.components.MomentMetaRow
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MmHomeDisplayFont
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
    title: String = "和甜甜在一起 💞",
    days: Long = 136,
    anchorDate: LocalDate = LocalDate.of(2026, 5, 4),
    milestoneLabel: String? = "一周年",
    milestoneDaysRemaining: Long? = 229,
    milestoneProgress: Float? = 0.37f,
    isPinned: Boolean = true,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    semanticDescription: String? = null,
) {
    val extended = LocalMmExtendedColors.current
    val pressInteraction = remember { MutableInteractionSource() }
    val frameModifier = modifier
        .fillMaxWidth()
        // aspectRatio 已按兰亭黑实际行高调整为 290/150，比 DESIGN 142dp 多 8dp 容纳数字行，
        // meta 行（下一里程碑 X 天）距 bottom border 12.5dp，与 DESIGN 一致。
        .aspectRatio(MomentMarkTokens.HomeAchievementAspectRatio)
        .clickable(interactionSource = pressInteraction, indication = null, role = Role.Button, onClick = onClick)
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
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(top = MomentMarkTokens.HomeAchievementMainGap),
            ) {
                Column {
                    Text(
                        text = title,
                        style = TextStyle(fontFamily = MmHomeDisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = MomentMarkTokens.HomeAchievementTitleFontSize, lineHeight = 17.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row {
                        Text(
                            text = days.toString(),
                            style = TextStyle(fontFamily = MmHomeDisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = MomentMarkTokens.HomeAchievementNumberFontSize, lineHeight = MomentMarkTokens.HomeAchievementNumberLineHeight),
                            color = extended.goldInk,
                            modifier = Modifier.alignByBaseline(),
                        )
                        Text(
                            text = " 天",
                            style = TextStyle(fontFamily = MmHomeDisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = MomentMarkTokens.HomeAchievementUnitFontSize, lineHeight = 14.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .alignByBaseline()
                                .padding(bottom = MomentMarkTokens.HomeAchievementUnitBaselineOffset),
                        )
                    }
                }
                Text(
                    text = "SINCE\n${anchorDate.format(CardDateFormatter)}",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold, fontSize = 9.sp, lineHeight = 11.sp),
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
    AchievementFrame(modifier = frameModifier, pressInteractionSource = pressInteraction) { content() }
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
    val pressInteraction = remember { MutableInteractionSource() }
    val hudStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.ExtraBold,
        fontSize = MomentMarkTokens.HomeMainQuestHudFontSize,
        lineHeight = MomentMarkTokens.HomeMainQuestHudLineHeight,
    )
    val titleStyle = TextStyle(
        fontFamily = MmHomeDisplayFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = MomentMarkTokens.HomeMainQuestTitleFontSize,
        lineHeight = MomentMarkTokens.HomeMainQuestTitleLineHeight,
    )
    val numberStyle = TextStyle(
        fontFamily = MmHomeDisplayFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = MomentMarkTokens.HomeMainQuestNumberFontSize,
        lineHeight = MomentMarkTokens.HomeMainQuestNumberLineHeight,
    )
    val unitStyle = TextStyle(
        fontFamily = MmHomeDisplayFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = MomentMarkTokens.HomeMainQuestUnitFontSize,
        lineHeight = MomentMarkTokens.HomeMainQuestUnitLineHeight,
    )
    val unlockStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.ExtraBold,
        fontSize = MomentMarkTokens.HomeMainQuestHudFontSize,
        lineHeight = MomentMarkTokens.HomeMainQuestHudLineHeight,
    )

    PixelPanel(
        modifier = modifier
            .fillMaxWidth()
            // aspectRatio 调整为 290/116，容纳兰亭黑 44sp 数字实际行高。
            .aspectRatio(MomentMarkTokens.HomeMainQuestAspectRatio)
            .clickable(interactionSource = pressInteraction, indication = null, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = semanticDescription
                    ?: "$title，还有 $days 天，${anchorDate.format(CardDateFormatter)}"
            },
        variant = com.cch.momentmark.ui.components.PixelPanelVariant.Emphasized,
        pressInteractionSource = pressInteraction,
        // 描边盒必须撑满 aspectRatio 外框（DESIGN_SYSTEM §3.3）：边框若按内容高度 wrap，
        // 硬影仍按外框全高绘制，不同屏宽/字体渲染下会出现「主体过小 + 悬空阴影」。
        fillContent = true,
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
                verticalAlignment = Alignment.Top,
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
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    progressFraction: Float? = null,
    progressColor: Color? = null,
    iconKey: String? = null,
    showBadgeAlongsideIcon: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    semanticDescription: String? = null,
) {
    val pressInteraction = remember { MutableInteractionSource() }
    PixelPanel(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(MomentMarkTokens.HomeSmallCardAspectRatio)
            .clickable(interactionSource = pressInteraction, indication = null, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = semanticDescription ?: "$title，$value$unitLabel，$metadata"
            },
        variant = com.cch.momentmark.ui.components.PixelPanelVariant.Raised,
        pressInteractionSource = pressInteraction,
        // 描边盒撑满 aspectRatio 外框，边框与硬影在任何屏宽/字体下严格对齐。
        fillContent = true,
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
                        fontFamily = MmHomeDisplayFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = MomentMarkTokens.HomeSmallCardTitleFontSize,
                        lineHeight = MomentMarkTokens.HomeSmallCardTitleLineHeight,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (iconKey == null) {
                    PixelBadge(label = badgeLabel, color = badgeColor)
                } else {
                    PixelIconSlot(
                        iconKey = iconKey,
                        iconColor = valueColor,
                        showBackground = false,
                        boxSize = MomentMarkTokens.HomeSmallCardIconSize,
                        iconFontSize = MomentMarkTokens.HomeSmallCardIconFontSize,
                    )
                    if (showBadgeAlongsideIcon) {
                        PixelBadge(label = badgeLabel, color = badgeColor)
                    }
                }
            }
            Row(modifier = Modifier.padding(top = MomentMarkTokens.HomeSmallCardTitleToNumberGap)) {
                Text(
                    text = value,
                    style = TextStyle(
                        fontFamily = MmHomeDisplayFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = MomentMarkTokens.HomeSmallCardNumberFontSize,
                        lineHeight = MomentMarkTokens.HomeSmallCardNumberLineHeight,
                    ),
                    color = valueColor,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    text = unitLabel,
                    style = TextStyle(
                        fontFamily = MmHomeDisplayFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = MomentMarkTokens.HomeSmallCardUnitFontSize,
                        lineHeight = 12.sp,
                    ),
                    color = valueColor,
                    modifier = Modifier.alignByBaseline(),
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
    val valueColor = card.hpFraction?.let { resolveHpAccentColor(it) }
        ?: MaterialTheme.colorScheme.onSurface
    HomeSmallCard(
        title = card.title,
        value = if (card.status == EventTimeStatus.TODAY) "就是今天" else card.days.toString(),
        unitLabel = if (card.status == EventTimeStatus.TODAY) "" else " 天后",
        badgeLabel = when (card.badgeKind()) {
            MomentBadgeKind.MAIN -> "◆"
            MomentBadgeKind.LIMITED -> "⧗"
            else -> "◇"
        },
        badgeColor = badgeColor,
        valueColor = valueColor,
        metadata = "${if (card.status == EventTimeStatus.TODAY) "TODAY" else "DUE"} ${card.anchorDate.format(CardDateFormatter)}" +
            (card.groupId?.let { " · $it" } ?: ""),
        progressFraction = card.hpFraction,
        onClick = onClick,
        modifier = modifier,
        semanticDescription = card.accessibilityDescription(),
        iconKey = card.groupIconKey(),
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
        iconKey = card.taskGroupIconKey(),
        showBadgeAlongsideIcon = true,
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
    // 血条与数字同用「下一里程碑进度」的三段色（design.html .is-short/.is-medium/.is-long）。
    val progress = card.nextMilestone?.progressFraction
    val valueColor = progress?.let { resolveHpAccentColor(it) }
        ?: MaterialTheme.colorScheme.onSurface
    HomeSmallCard(
        title = card.title,
        value = card.days.toString(),
        unitLabel = " 天了",
        badgeLabel = "",
        badgeColor = valueColor,
        valueColor = valueColor,
        metadata = "SINCE ${card.anchorDate.format(CardDateFormatter)}" + (card.groupId?.let { " · $it" } ?: ""),
        progressFraction = progress,
        iconKey = card.groupIconKey(),
        onClick = onClick,
        modifier = modifier,
        semanticDescription = card.accessibilityDescription(),
    )
}

private fun MomentCardState.groupIconKey(): String = when (groupId) {
    "旅行" -> "travel"
    "工作" -> "work"
    "生活", "家庭" -> "heart"
    "学业" -> "study"
    else -> "default"
}

private fun TaskCardState.taskGroupIconKey(): String = when (groupId) {
    "旅行" -> "travel"
    "工作" -> "work"
    "生活", "家庭" -> "heart"
    "学业" -> "study"
    else -> "default"
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
    PixelBadge(
        label = kind.label,
        variant = when (kind) {
            MomentBadgeKind.MAIN -> PixelBadgeVariant.Main
            MomentBadgeKind.SIDE -> PixelBadgeVariant.Side
            MomentBadgeKind.LIMITED -> PixelBadgeVariant.Limited
            MomentBadgeKind.ACHIEVEMENT -> PixelBadgeVariant.Achievement
        },
        color = color,
    )
}

/** 重要度星级：填充金色、空星弱化色，最多 3 星；null 不渲染。 */
@Composable
private fun RarityStars(rarity: Int?) {
    if (rarity == null) return
    PixelStarRating(value = rarity, max = 3)
}
