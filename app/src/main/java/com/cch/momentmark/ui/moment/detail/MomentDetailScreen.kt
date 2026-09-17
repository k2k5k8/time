package com.cch.momentmark.ui.moment.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cch.momentmark.domain.model.Moment
import com.cch.momentmark.domain.model.MomentDirection
import com.cch.momentmark.domain.time.EventTimeStatus
import com.cch.momentmark.ui.components.PixelFieldLabel
import com.cch.momentmark.ui.components.PixelPanel
import com.cch.momentmark.ui.components.PixelPanelVariant
import com.cch.momentmark.ui.components.PixelStatusHud
import com.cch.momentmark.ui.components.PixelXpBar
import com.cch.momentmark.ui.components.AchievementFrame
import com.cch.momentmark.ui.components.PixelHpBar
import com.cch.momentmark.ui.components.MomentSummaryPanel
import com.cch.momentmark.ui.components.MomentSummaryPresentation
import com.cch.momentmark.ui.components.MomentStoryPanel
import com.cch.momentmark.ui.components.MilestonePanel
import com.cch.momentmark.ui.components.MilestonePresentation
import com.cch.momentmark.ui.components.PixelConfirmationDialog
import com.cch.momentmark.ui.components.PixelPageHeader
import com.cch.momentmark.domain.time.Milestone
import com.cch.momentmark.domain.time.MilestoneCalculator
import com.cch.momentmark.domain.time.MilestoneStatus
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MomentMarkTokens
import com.cch.momentmark.ui.theme.MmNoFontPadding
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTimeTextStyle
import java.util.Locale

private val DetailDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

/**
 * J ⑥⑦ 的同一 Moment 详情组件：状态只由 `EventTimeCalculator` 的派生结果切换。
 * 第 ⑥ 屏「任务详情 · QUEST DETAIL（未来）」按 design.html 复刻；第 ⑦ 屏成就详情沿用既有面板。
 *
 * [fixtureState] 仅 DEBUG 视觉回归使用（`debug.momentmark.detail quest`）：直接渲染冻结帧，
 * 不读取 Repository；release 始终为 null。
 */
@Composable
fun MomentDetailScreen(
    momentId: String,
    viewModel: MomentDetailViewModel,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onSealed: (Moment) -> Unit,
    modifier: Modifier = Modifier,
    fixtureState: MomentDetailUiState? = null,
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(momentId, fixtureState) { if (fixtureState == null) viewModel.load(momentId) }
    MomentDetailContent(
        state = fixtureState ?: state,
        onBack = onBack,
        onEdit = onEdit,
        onTogglePinned = viewModel::togglePinned,
        onSeal = viewModel::seal,
        onSealed = onSealed,
        modifier = modifier,
    )
}

@Composable
internal fun MomentDetailContent(
    state: MomentDetailUiState,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onTogglePinned: () -> Unit,
    onSeal: ((Moment) -> Unit) -> Unit,
    onSealed: (Moment) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extended = LocalMmExtendedColors.current
    val moment = state.moment
    var confirmSeal by remember { mutableStateOf(false) }
    val isAchievement = moment != null && state.status.isAchievementDisplay(moment)
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        when {
            state.isLoading -> DetailMessage("▸ LOADING…", extended.labelTertiary)
            moment == null -> DetailMessage("⚠ ${state.errorMessage ?: "没有可展示的时刻。"}", MaterialTheme.colorScheme.error)
            moment != null && state.status != null -> {
                if (isAchievement) {
                    AchievementBranch(
                        state = state,
                        moment = moment,
                        onBack = onBack,
                        onEdit = onEdit,
                        onTogglePinned = onTogglePinned,
                        onRequestSeal = { confirmSeal = true },
                    )
                } else {
                    QuestBranch(
                        state = state,
                        moment = moment,
                        onBack = onBack,
                        onEdit = onEdit,
                        onSeal = { confirmSeal = true },
                    )
                }
            }
            moment != null -> DetailMessage("⚠ 时刻状态暂不可用。", MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(MomentMarkTokens.FormScrollPaddingBottom))
    }
    if (confirmSeal && moment != null) {
        PixelConfirmationDialog(
            title = "封印「${moment.title}」？",
            message = "它将被移入封印之地，30 天内可复活。",
            confirmLabel = "确认封印",
            onConfirm = {
                confirmSeal = false
                onSeal { onSealed(it) }
            },
            onDismiss = { confirmSeal = false },
        )
    }
}

/**
 * 第 ⑦ 屏「成就详情 · ACHIEVEMENT（过去）」：
 * statusbar → AchievementFrame 居中横幅（🏆 / ACHIEVEMENT·成就珍藏中 / 标题 / 已 N 天 / SINCE）
 * → STORY → MILESTONES（三种状态行 + 下一项金色血条）→ [✎ 编辑][🏆 珍藏中] → 封印链。
 * 周年提醒按 PRD §5.7 / ARCHITECTURE §7.1 保持隐藏，不接入本屏。
 */
@Composable
private fun AchievementBranch(
    state: MomentDetailUiState,
    moment: Moment,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onTogglePinned: () -> Unit,
    onRequestSeal: () -> Unit,
) {
    val extended = LocalMmExtendedColors.current
    val ink = MaterialTheme.colorScheme.onSurface
    val ink2 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink3 = extended.labelTertiary
    val goldInk = extended.goldInk
    val gold = MaterialTheme.colorScheme.tertiary
    val monoHud = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold)
    val tokens = MomentMarkTokens

    PixelStatusHud(leftText = "LV.07", rightText = "ACHIEVEMENT", contentColor = ink2)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = tokens.DetailPagePaddingHorizontal),
    ) {
        // ── 成就横幅 .ach.px4（居中）──────────────────────────────
        AchievementFrame(modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = tokens.AchievementBannerPaddingHorizontal,
                        vertical = tokens.AchievementBannerPaddingVertical,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "🏆",
                    fontSize = tokens.AchievementBannerTrophyFontSize,
                    lineHeight = tokens.AchievementBannerTrophyFontSize,
                    style = TextStyle(platformStyle = MmNoFontPadding),
                )
                Text(
                    "ACHIEVEMENT · 成就珍藏中",
                    style = monoHud.copy(
                        fontSize = tokens.AchievementBannerLabelFontSize,
                        lineHeight = tokens.AchievementBannerLabelLineHeight,
                        letterSpacing = tokens.AchievementBannerLabelLetterSpacing,
                    ),
                    color = goldInk,
                    modifier = Modifier.padding(top = tokens.AchievementBannerLabelTopGap),
                )
                Text(
                    text = moment.title,
                    style = TextStyle(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = tokens.AchievementBannerTitleFontSize,
                        lineHeight = tokens.AchievementBannerTitleLineHeight,
                        platformStyle = MmNoFontPadding,
                    ),
                    color = ink,
                    maxLines = 1,
                    modifier = Modifier.padding(top = tokens.AchievementBannerTitleTopGap),
                )
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = goldInk)) {
                            append("已 ${state.days}")
                        }
                        withStyle(
                            style = SpanStyle(
                                color = ink2,
                                fontSize = tokens.AchievementBannerNumberUnitFontSize,
                                fontWeight = FontWeight.Normal,
                            ),
                        ) {
                            append(" 天")
                        }
                    },
                    style = TextStyle(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = tokens.AchievementBannerNumberFontSize,
                        lineHeight = tokens.AchievementBannerNumberLineHeight,
                        platformStyle = MmNoFontPadding,
                    ),
                    modifier = Modifier.padding(top = tokens.AchievementBannerNumberTopGap),
                )
                Text(
                    text = "SINCE ${moment.anchorDate.format(DetailDateFormatter)} · " +
                        "${moment.anchorDate.dayOfWeek.getDisplayName(JavaTimeTextStyle.FULL, Locale.CHINA)} · " +
                        (moment.groupId ?: "无阵营"),
                    style = monoHud.copy(
                        fontSize = tokens.AchievementBannerSinceFontSize,
                        lineHeight = tokens.AchievementBannerSinceLineHeight,
                    ),
                    color = ink3,
                    modifier = Modifier.padding(top = tokens.AchievementBannerSinceTopGap),
                )
            }
        }

        // ── 成就故事 STORY ────────────────────────────────────────
        Spacer(Modifier.height(tokens.DetailPanelMarginTop))
        PixelPanel(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(
                    horizontal = tokens.DetailPanelPaddingHorizontal,
                    vertical = tokens.DetailPanelPaddingVertical,
                ),
            ) {
                PixelFieldLabel("▸ 成就故事 STORY")
                Text(
                    text = moment.note.ifBlank { "暂无成就故事。" },
                    style = TextStyle(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = tokens.DetailLoreBodyFontSize,
                        lineHeight = tokens.DetailLoreBodyLineHeight,
                    ),
                    color = ink,
                    modifier = Modifier.padding(top = tokens.DetailLoreBodyTopGap),
                )
            }
        }

        // ── 里程碑 MILESTONES ─────────────────────────────────────
        Spacer(Modifier.height(tokens.DetailPanelMarginTop))
        PixelPanel(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(
                    horizontal = tokens.DetailPanelPaddingHorizontal,
                    vertical = tokens.DetailPanelPaddingVertical,
                ),
            ) {
                PixelFieldLabel("▸ 里程碑 MILESTONES")
                val milestones = state.milestones
                if (milestones.isEmpty()) {
                    Text(
                        "尚未生成里程碑。",
                        style = MaterialTheme.typography.bodySmall,
                        color = ink3,
                        modifier = Modifier.padding(top = tokens.DetailLoreBodyTopGap),
                    )
                } else {
                    milestones.forEachIndexed { index, milestone ->
                        AchievementMilestoneRow(
                            milestone = milestone,
                            isLast = index == milestones.lastIndex,
                        )
                    }
                }
            }
        }

        // ── 操作按钮 ─────────────────────────────────────────────
        Spacer(Modifier.height(tokens.DetailButtonRowTopGap))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.DetailButtonRowGap),
        ) {
            DetailActionButton(
                label = "✎ 编辑",
                container = MaterialTheme.colorScheme.secondary,
                content = MaterialTheme.colorScheme.onSecondary,
                contentDescription = "编辑 ${moment.title}",
                onClick = { onEdit(moment.id) },
                modifier = Modifier.weight(1f),
            )
            DetailActionButton(
                label = if (moment.isPinned) "🏆 珍藏中" else "◇ 设为珍藏",
                container = if (moment.isPinned) gold else MaterialTheme.colorScheme.surface,
                content = if (moment.isPinned) MaterialTheme.colorScheme.onTertiary else ink,
                contentDescription = "返回首页",
                onClick = onBack,
                modifier = Modifier.weight(1.3f),
            )
        }

        // ── 封印（移入回收站）──────────────────────────────────────
        Spacer(Modifier.height(tokens.DetailDeleteTopGap))
        Text(
            text = "✕ 封印此成就（移入回收站）",
            style = monoHud.copy(
                fontSize = tokens.DetailDeleteFontSize,
                lineHeight = tokens.DetailDeleteLineHeight,
                textDecoration = TextDecoration.Underline,
            ),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .semantics { contentDescription = "封印此成就，移入回收站" }
                .clickable(role = Role.Button, onClick = onRequestSeal)
                .padding(bottom = 1.dp),
        )
    }
}

/**
 * 第 ⑥ 屏「任务详情 · QUEST DETAIL（未来）」：
 * statusbar → px4 主横幅(MAIN QUEST/EXP/标题/倒数大数字/经验格/PROGRESS·UNLOCK)
 * → LORE → 前置目标 → DUE/GROUP/RARITY → [编辑][继续冒险] → 放弃链。
 */
@Composable
private fun QuestBranch(
    state: MomentDetailUiState,
    moment: Moment,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onSeal: () -> Unit,
) {
    val extended = LocalMmExtendedColors.current
    val ink = MaterialTheme.colorScheme.onSurface
    val ink2 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink3 = extended.labelTertiary
    val monoHud = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold)

    PixelStatusHud(leftText = "LV.07", rightText = "QUEST DETAIL", contentColor = ink2)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = MomentMarkTokens.DetailPagePaddingHorizontal),
    ) {
        // ── 主横幅 .px4 ────────────────────────────────────────────
        PixelPanel(variant = PixelPanelVariant.Emphasized, modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(
                    horizontal = MomentMarkTokens.DetailBannerPaddingHorizontal,
                    vertical = MomentMarkTokens.DetailBannerPaddingVertical,
                ),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = if (moment.isPinned) "◆ MAIN QUEST 主线" else "◇ SIDE QUEST 支线",
                        style = monoHud.copy(
                            fontSize = MomentMarkTokens.DetailBannerHudFontSize,
                            lineHeight = MomentMarkTokens.DetailBannerHudLineHeight,
                        ),
                        color = extended.manaPurple,
                    )
                    Text(
                        text = "EXP +${state.days}",
                        style = monoHud.copy(
                            fontSize = MomentMarkTokens.DetailBannerHudFontSize,
                            lineHeight = MomentMarkTokens.DetailBannerHudLineHeight,
                        ),
                        color = ink3,
                    )
                }
                Text(
                    text = moment.title,
                    style = TextStyle(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = MomentMarkTokens.DetailBannerTitleFontSize,
                        lineHeight = MomentMarkTokens.DetailBannerTitleLineHeight,
                        platformStyle = MmNoFontPadding,
                    ),
                    color = ink,
                    maxLines = 1,
                    modifier = Modifier.padding(top = MomentMarkTokens.DetailBannerTitleTopGap),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = MomentMarkTokens.DetailBannerNumberTopGap),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    if (state.status == EventTimeStatus.TODAY) {
                        Text("就是今天", style = MaterialTheme.typography.displayMedium, color = ink)
                    } else {
                        Text(
                            text = state.days.toString(),
                            style = TextStyle(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = MomentMarkTokens.DetailBannerNumberFontSize,
                                lineHeight = MomentMarkTokens.DetailBannerNumberLineHeight,
                                platformStyle = MmNoFontPadding,
                            ),
                            color = ink,
                        )
                        Text(
                            text = " 天后开启",
                            style = TextStyle(
                                fontSize = MomentMarkTokens.DetailBannerUnitFontSize,
                                fontWeight = FontWeight.Normal,
                                platformStyle = MmNoFontPadding,
                            ),
                            color = ink2,
                        )
                    }
                }
                PixelXpBar(
                    filledCells = state.filledXpCells,
                    contentDescription = "任务进度 ${state.filledXpCells}/16",
                    modifier = Modifier.padding(top = MomentMarkTokens.DetailBannerXpTopGap),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = MomentMarkTokens.DetailBannerXpMetaTopGap),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = state.progressPercent?.let { "PROGRESS $it%" } ?: "PROGRESS --",
                        style = monoHud.copy(
                            fontSize = MomentMarkTokens.DetailBannerXpMetaFontSize,
                            lineHeight = MomentMarkTokens.DetailBannerXpMetaLineHeight,
                        ),
                        color = ink3,
                    )
                    Text(
                        text = "UNLOCK ${moment.anchorDate.format(DetailDateFormatter)}",
                        style = monoHud.copy(
                            fontSize = MomentMarkTokens.DetailBannerXpMetaFontSize,
                            lineHeight = MomentMarkTokens.DetailBannerXpMetaLineHeight,
                        ),
                        color = ink3,
                    )
                }
            }
        }

        // ── 任务背景 LORE ─────────────────────────────────────────
        Spacer(Modifier.height(MomentMarkTokens.DetailPanelMarginTop))
        PixelPanel(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(
                    horizontal = MomentMarkTokens.DetailPanelPaddingHorizontal,
                    vertical = MomentMarkTokens.DetailPanelPaddingVertical,
                ),
            ) {
                PixelFieldLabel("▸ 任务背景 LORE")
                Text(
                    text = moment.note.ifBlank { "暂无任务背景。" },
                    style = TextStyle(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = MomentMarkTokens.DetailLoreBodyFontSize,
                        lineHeight = MomentMarkTokens.DetailLoreBodyLineHeight,
                    ),
                    color = ink,
                    modifier = Modifier.padding(top = MomentMarkTokens.DetailLoreBodyTopGap),
                )
            }
        }

        // ── 前置目标 OBJECTIVES（P1 展示壳）─────────────────────────
        Spacer(Modifier.height(MomentMarkTokens.DetailPanelMarginTop))
        PixelPanel(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(
                    horizontal = MomentMarkTokens.DetailPanelPaddingHorizontal,
                    vertical = MomentMarkTokens.DetailPanelPaddingVertical,
                ),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PixelFieldLabel("▸ 前置目标 OBJECTIVES")
                    // 24×24 蓝色「＋」方块
                    PixelPanel(
                        modifier = Modifier.size(MomentMarkTokens.DetailObjectiveAddButtonSize),
                        backgroundColor = MaterialTheme.colorScheme.secondary,
                        borderColor = MaterialTheme.colorScheme.outline,
                        shadowColor = MaterialTheme.colorScheme.outline,
                    ) {
                        Text(
                            "＋",
                            style = TextStyle(
                                fontSize = MomentMarkTokens.DetailObjectiveAddButtonFontSize,
                                fontWeight = FontWeight.ExtraBold,
                            ),
                            color = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
                val separatorColor = MaterialTheme.colorScheme.surfaceVariant
                state.questObjectives.forEachIndexed { index, objective ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                if (index < state.questObjectives.lastIndex) {
                                    val w = MomentMarkTokens.PxThinBorderWidth.toPx()
                                    val y = size.height - w / 2f
                                    drawLine(
                                        color = separatorColor,
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = w,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 3f)),
                                    )
                                }
                            }
                            .padding(vertical = MomentMarkTokens.DetailObjectiveRowPaddingVertical),
                        horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.DetailObjectiveRowGap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ObjectiveCheckbox(checked = objective.completed)
                        Text(
                            text = objective.title,
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = MomentMarkTokens.DetailObjectiveTitleFontSize,
                                lineHeight = MomentMarkTokens.DetailObjectiveTitleLineHeight,
                            ),
                            color = if (objective.completed) ink3 else ink,
                            textDecoration = if (objective.completed) TextDecoration.LineThrough else TextDecoration.None,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = if (objective.completed) "CLEAR" else "TODO",
                            style = monoHud.copy(
                                fontSize = MomentMarkTokens.DetailObjectiveStatusFontSize,
                                lineHeight = MomentMarkTokens.DetailObjectiveStatusLineHeight,
                            ),
                            color = if (objective.completed) MaterialTheme.colorScheme.primary else ink3,
                        )
                    }
                }
            }
        }

        // ── DUE / GROUP / RARITY ───────────────────────────────────
        Spacer(Modifier.height(MomentMarkTokens.DetailPanelMarginTop))
        PixelPanel(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MomentMarkTokens.DetailMetaPaddingHorizontal,
                        vertical = MomentMarkTokens.DetailMetaPaddingVertical,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetaCell("DUE", moment.anchorDate.format(DetailDateFormatter))
                MetaCell("GROUP", moment.groupId ?: "无阵营")
                Column {
                    Text(
                        "RARITY",
                        style = monoHud.copy(
                            fontSize = MomentMarkTokens.DetailMetaLabelFontSize,
                            lineHeight = MomentMarkTokens.DetailMetaLabelLineHeight,
                        ),
                        color = ink3,
                    )
                    Text(
                        text = rarityStars(moment.rarity),
                        style = TextStyle(
                            fontSize = MomentMarkTokens.DetailMetaStarFontSize,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }

        // ── 操作按钮 ─────────────────────────────────────────────
        Spacer(Modifier.height(MomentMarkTokens.DetailButtonRowTopGap))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.DetailButtonRowGap),
        ) {
            DetailActionButton(
                label = "✎ 编辑任务",
                container = MaterialTheme.colorScheme.secondary,
                content = MaterialTheme.colorScheme.onSecondary,
                contentDescription = "编辑 ${moment.title}",
                onClick = { onEdit(moment.id) },
                modifier = Modifier.weight(1f),
            )
            DetailActionButton(
                label = "◆ 继续冒险",
                container = MaterialTheme.colorScheme.primary,
                content = MaterialTheme.colorScheme.onPrimary,
                contentDescription = "继续冒险",
                onClick = onBack,
                modifier = Modifier.weight(1.3f),
            )
        }

        // ── 放弃（移入回收站）──────────────────────────────────────
        Spacer(Modifier.height(MomentMarkTokens.DetailDeleteTopGap))
        Text(
            text = "✕ 放弃任务（移入回收站）",
            style = monoHud.copy(
                fontSize = MomentMarkTokens.DetailDeleteFontSize,
                lineHeight = MomentMarkTokens.DetailDeleteLineHeight,
                textDecoration = TextDecoration.Underline,
            ),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .semantics { contentDescription = "放弃任务，移入回收站" }
                .clickable(role = Role.Button, onClick = onSeal)
                .padding(bottom = 1.dp),
        )
    }
}

@Composable
private fun MetaCell(label: String, value: String) {
    val extended = LocalMmExtendedColors.current
    Column {
        Text(
            label,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = MomentMarkTokens.DetailMetaLabelFontSize,
                lineHeight = MomentMarkTokens.DetailMetaLabelLineHeight,
            ),
            color = extended.labelTertiary,
        )
        Text(
            value,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = MomentMarkTokens.DetailMetaValueFontSize,
                lineHeight = MomentMarkTokens.DetailMetaValueLineHeight,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun rarityStars(rarity: Int?): String {
    val filled = rarity?.coerceIn(0, 3) ?: 0
    return "★".repeat(filled) + "☆".repeat(3 - filled)
}

@Composable
private fun ObjectiveCheckbox(checked: Boolean) {
    val outline = MaterialTheme.colorScheme.outline
    val uncheckedBorder = LocalMmExtendedColors.current.labelTertiary
    Box(
        Modifier
            .size(MomentMarkTokens.CheckboxSize)
            .background(if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            .drawBehind {
                val stroke = MomentMarkTokens.CheckboxStrokeWidth.toPx()
                drawRect(
                    color = if (checked) outline else uncheckedBorder,
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Text(
                "✓",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = MomentMarkTokens.CheckboxCheckMarkFontSize,
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.offset(y = MomentMarkTokens.CheckboxCheckMarkOffsetY),
            )
        }
    }
}

@Composable
private fun DetailActionButton(
    label: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PixelPanel(
        modifier = modifier
            .semantics(mergeDescendants = true) { this.contentDescription = contentDescription }
            .clickable(role = Role.Button, onClick = onClick),
        backgroundColor = container,
        borderColor = MaterialTheme.colorScheme.outline,
        shadowColor = MaterialTheme.colorScheme.outline,
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontWeight = FontWeight.ExtraBold,
                fontSize = MomentMarkTokens.DetailButtonFontSize,
                lineHeight = MomentMarkTokens.DetailButtonLineHeight,
            ),
            color = content,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MomentMarkTokens.DetailButtonPaddingVertical),
        )
    }
}

@Composable
private fun DetailMessage(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = color, modifier = Modifier.padding(MomentMarkTokens.SpacePage))
}

/**
 * 第 ⑦ 屏里程碑行：三种状态（已达成 / 下一项 / 远征中）。
 * 已达成 = 绿填充 checkbox + ✓ + 墨色标签 + ink-3 详情 + 绿色 ✓；
 * 下一项 = 金色半格 checkbox + gold-ink 标签/详情 + 金色百分比 + 金色血条；
 * 远征中 = 空 checkbox(ink-3 边框) + ink-2 标签 + ink-3 详情。
 */
@Composable
private fun AchievementMilestoneRow(
    milestone: Milestone,
    isLast: Boolean,
) {
    val extended = LocalMmExtendedColors.current
    val ink = MaterialTheme.colorScheme.onSurface
    val ink2 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink3 = extended.labelTertiary
    val goldInk = extended.goldInk
    val gold = MaterialTheme.colorScheme.tertiary
    val hp = MaterialTheme.colorScheme.primary
    val tokens = MomentMarkTokens
    val mono = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold)

    val isNext = milestone.status == MilestoneStatus.NEXT
    val isAchieved = milestone.status == MilestoneStatus.ACHIEVED
    val separatorColor = MaterialTheme.colorScheme.surfaceVariant

    val labelColor = when {
        isAchieved -> ink
        isNext -> goldInk
        else -> ink2
    }
    val detailColor = when {
        isAchieved -> ink3
        isNext -> goldInk
        else -> ink3
    }

    val paddingTop = when {
        isNext -> tokens.MilestoneRowNextPaddingTop
        isLast -> tokens.MilestoneRowLastPaddingTop
        else -> tokens.MilestoneRowPaddingVertical
    }
    val paddingBottom = when {
        isNext -> tokens.MilestoneRowNextPaddingBottom
        isLast -> tokens.MilestoneRowLastPaddingBottom
        else -> tokens.MilestoneRowPaddingVertical
    }

    Column(
        Modifier
            .fillMaxWidth()
            .then(
                if (!isLast) {
                    Modifier.drawBehind {
                        val w = tokens.PxThinBorderWidth.toPx()
                        val y = size.height - w / 2f
                        drawLine(
                            color = separatorColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = w,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 3f)),
                        )
                    }
                } else Modifier
            )
            .padding(top = paddingTop, bottom = paddingBottom),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.MilestoneRowGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MilestoneCheckbox(status = milestone.status)
            Text(
                milestone.label,
                style = TextStyle(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = tokens.MilestoneLabelFontSize,
                    lineHeight = tokens.MilestoneLabelLineHeight,
                ),
                color = labelColor,
                modifier = Modifier.width(tokens.MilestoneLabelWidth),
            )
            Text(
                text = when (milestone.status) {
                    MilestoneStatus.ACHIEVED -> {
                        val dateStr = milestone.targetDate.format(DetailDateFormatter)
                        if ("周年" in milestone.label && milestone.dayCount != null) {
                            "$dateStr · ${milestone.dayCount} 天"
                        } else {
                            "$dateStr 达成"
                        }
                    }
                    MilestoneStatus.NEXT -> "还有 ${milestone.daysRemaining} 天 · ${milestone.targetDate.format(DetailDateFormatter)}"
                    MilestoneStatus.EXPEDITION -> "${milestone.targetDate.format(DetailDateFormatter)} · 远征中"
                },
                style = mono.copy(
                    fontSize = tokens.MilestoneDetailFontSize,
                    lineHeight = tokens.MilestoneDetailLineHeight,
                ),
                color = detailColor,
                modifier = Modifier.weight(1f),
            )
            when (milestone.status) {
                MilestoneStatus.ACHIEVED -> Text(
                    "✓",
                    style = mono.copy(
                        fontSize = tokens.MilestoneStatusFontSize,
                        lineHeight = tokens.MilestoneStatusLineHeight,
                    ),
                    color = hp,
                )
                MilestoneStatus.NEXT -> Text(
                    "${Math.round((milestone.progressFraction ?: 0f) * 100)}%",
                    style = mono.copy(
                        fontSize = tokens.MilestoneStatusFontSize,
                        lineHeight = tokens.MilestoneStatusLineHeight,
                    ),
                    color = gold,
                )
                MilestoneStatus.EXPEDITION -> {}
            }
        }
        if (isNext) {
            PixelHpBar(
                fraction = milestone.progressFraction ?: 0f,
                fillColor = gold,
                modifier = Modifier.padding(top = tokens.MilestoneHpBarTopGap),
            )
        }
    }
}

/** 里程碑 checkbox：已达成=绿填充墨边白✓；下一项=金半格渐变金边；远征中=空框 ink-3 边。 */
@Composable
private fun MilestoneCheckbox(status: MilestoneStatus) {
    val outline = MaterialTheme.colorScheme.outline
    val ink3 = LocalMmExtendedColors.current.labelTertiary
    val gold = MaterialTheme.colorScheme.tertiary
    val hp = MaterialTheme.colorScheme.primary
    val tokens = MomentMarkTokens

    val borderColor = when (status) {
        MilestoneStatus.ACHIEVED -> outline
        MilestoneStatus.NEXT -> gold
        MilestoneStatus.EXPEDITION -> ink3
    }

    Box(
        Modifier
            .size(tokens.MilestoneCheckboxSize)
            .then(
                when (status) {
                    MilestoneStatus.ACHIEVED -> Modifier.background(hp)
                    MilestoneStatus.NEXT -> Modifier.drawBehind {
                        // linear-gradient(135deg, gold 50%, transparent 50%)
                        drawRect(
                            brush = Brush.linearGradient(
                                colorStops = arrayOf(
                                    0f to gold,
                                    0.5f to gold,
                                    0.5f to Color.Transparent,
                                    1f to Color.Transparent,
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(size.width, size.height),
                            ),
                            size = size,
                        )
                    }
                    MilestoneStatus.EXPEDITION -> Modifier.background(MaterialTheme.colorScheme.surface)
                }
            )
            .drawBehind {
                val stroke = tokens.CheckboxStrokeWidth.toPx()
                drawRect(
                    color = borderColor,
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (status == MilestoneStatus.ACHIEVED) {
            Text(
                "✓",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = tokens.MilestoneCheckMarkFontSize,
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = Color.White,
                modifier = Modifier.offset(y = tokens.MilestoneCheckMarkOffsetY),
            )
        }
    }
}

private fun EventTimeStatus?.isAchievementDisplay(moment: Moment): Boolean =
    this == EventTimeStatus.PAST ||
        (this == EventTimeStatus.TODAY && moment.creationDirection == MomentDirection.PAST_ACHIEVEMENT)
