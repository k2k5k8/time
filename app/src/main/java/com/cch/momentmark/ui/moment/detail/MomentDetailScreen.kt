package com.cch.momentmark.ui.moment.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.cch.momentmark.domain.model.Moment
import com.cch.momentmark.domain.model.MomentDirection
import com.cch.momentmark.domain.time.EventTimeStatus
import com.cch.momentmark.ui.components.PageTitlePanel
import com.cch.momentmark.ui.components.AchievementFrame
import com.cch.momentmark.ui.components.PixelConfirmationDialog
import com.cch.momentmark.ui.components.PixelPanel
import com.cch.momentmark.ui.components.PixelHpBar
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MomentMarkTokens
import com.cch.momentmark.domain.time.Milestone
import com.cch.momentmark.domain.time.MilestoneCalculator
import com.cch.momentmark.domain.time.MilestoneStatus
import java.time.format.DateTimeFormatter

private val DetailDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

/** J ⑥⑦ 的同一 Moment 详情组件：状态只由 `EventTimeCalculator` 的派生结果切换。 */
@Composable
fun MomentDetailScreen(
    momentId: String,
    viewModel: MomentDetailViewModel,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onSealed: (Moment) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(momentId) { viewModel.load(momentId) }
    MomentDetailContent(
        state = state,
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MomentMarkTokens.SpacePage, vertical = MomentMarkTokens.SpaceCard),
        verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCard),
    ) {
        PageTitlePanel(
            title = "◂ 时刻详情",
            hud = if (isAchievement) "ACHIEVEMENT" else "QUEST DETAIL",
        )
        PixelPanel(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "返回大事件" }
                .clickable(role = Role.Button, onClick = onBack),
            backgroundColor = MaterialTheme.colorScheme.surface,
            shadowColor = MaterialTheme.colorScheme.outline,
        ) {
            Text(
                text = "◂ 返回大事件",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(MomentMarkTokens.SpaceInner),
            )
        }

        when {
            state.isLoading -> DetailMessage("▸ LOADING…", extended.labelTertiary)
            moment == null -> DetailMessage("⚠ ${state.errorMessage ?: "没有可展示的时刻。"}", MaterialTheme.colorScheme.error)
            else -> {
                MomentSummaryPanel(
                    moment = moment,
                    status = requireNotNull(state.status),
                    days = state.days,
                    isAchievement = isAchievement,
                )
                MomentStoryPanel(moment = moment, isAchievement = isAchievement)
                if (isAchievement) {
                    MilestonesPanel(
                        anchorDate = moment.anchorDate,
                        today = state.today ?: moment.anchorDate,
                    )
                }
                PixelPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "编辑 ${moment.title}" }
                        .clickable(role = Role.Button, onClick = { onEdit(moment.id) }),
                    backgroundColor = MaterialTheme.colorScheme.secondary,
                    borderColor = MaterialTheme.colorScheme.outline,
                    shadowColor = MaterialTheme.colorScheme.outline,
                ) {
                    Text(
                        text = "✎ 编辑时刻",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceInner),
                    )
                }
                PixelPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = if (moment.isPinned) "取消置顶 ${moment.title}" else "置顶 ${moment.title}" }
                        .clickable(enabled = !state.isPinUpdating, role = Role.Button, onClick = onTogglePinned),
                    backgroundColor = if (moment.isPinned) extended.manaPurple else MaterialTheme.colorScheme.surface,
                    borderColor = if (moment.isPinned) extended.manaPurple else MaterialTheme.colorScheme.outline,
                    shadowColor = MaterialTheme.colorScheme.outline,
                ) {
                    Text(
                        text = when {
                            state.isPinUpdating -> "主线状态保存中…"
                            moment.isPinned -> "◆ 已置顶主线 · 点按取消置顶"
                            else -> "◇ 设为首页置顶主线"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (moment.isPinned) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceInner),
                    )
                }
                PixelPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "封印 ${moment.title}" }
                        .clickable(enabled = !state.isSealing, role = Role.Button, onClick = { confirmSeal = true }),
                    borderColor = MaterialTheme.colorScheme.error,
                    backgroundColor = MaterialTheme.colorScheme.errorContainer,
                    shadowColor = MaterialTheme.colorScheme.outline,
                ) {
                    Text(
                        text = if (state.isSealing) "封印中…" else "✕ 封印此${if (isAchievement) "成就" else "时刻"}（移入回收站）",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceInner),
                    )
                }
                state.errorMessage?.let { DetailMessage("⚠ $it", MaterialTheme.colorScheme.error) }
            }
        }
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

@Composable
private fun MilestonesPanel(anchorDate: java.time.LocalDate, today: java.time.LocalDate) {
    val milestones = MilestoneCalculator.calculate(anchorDate, today)
    val extended = LocalMmExtendedColors.current
    PixelPanel(modifier = Modifier.fillMaxWidth(), shadowColor = MaterialTheme.colorScheme.outline) {
        Column(
            Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceInner),
            verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit),
        ) {
            Text(
                text = "▸ 里程碑 MILESTONES",
                style = MaterialTheme.typography.labelSmall,
                color = extended.labelTertiary,
            )
            if (milestones.isEmpty()) {
                Text("尚未生成里程碑。", style = MaterialTheme.typography.bodySmall, color = extended.labelTertiary)
            } else {
                milestones.forEach { milestone -> MilestoneRow(milestone) }
            }
        }
    }
}

@Composable
private fun MilestoneRow(milestone: Milestone) {
    val extended = LocalMmExtendedColors.current
    val isNext = milestone.status == MilestoneStatus.NEXT
    val color = when (milestone.status) {
        MilestoneStatus.ACHIEVED -> MaterialTheme.colorScheme.primary
        MilestoneStatus.NEXT -> extended.goldInk
        MilestoneStatus.EXPEDITION -> extended.labelTertiary
    }
    Column(
        Modifier.fillMaxWidth().padding(vertical = MomentMarkTokens.SpaceUnit),
        verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit),
    ) {
        androidx.compose.foundation.layout.Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCompact),
        ) {
            Text(
                text = when (milestone.status) {
                    MilestoneStatus.ACHIEVED -> "☑"
                    MilestoneStatus.NEXT -> "◈"
                    MilestoneStatus.EXPEDITION -> "□"
                },
                style = MaterialTheme.typography.labelMedium,
                color = color,
            )
            Text(milestone.label, style = MaterialTheme.typography.labelMedium, color = color)
            Text(
                text = when (milestone.status) {
                    MilestoneStatus.ACHIEVED -> "${milestone.targetDate.format(DetailDateFormatter)} 达成"
                    MilestoneStatus.NEXT -> "还有 ${milestone.daysRemaining} 天 · ${milestone.targetDate.format(DetailDateFormatter)}"
                    MilestoneStatus.EXPEDITION -> "${milestone.targetDate.format(DetailDateFormatter)} · 远征中"
                },
                style = MaterialTheme.typography.labelSmall,
                color = color,
                modifier = Modifier.weight(1f),
            )
        }
        if (isNext) {
            PixelHpBar(
                fraction = milestone.progressFraction ?: 0f,
                modifier = Modifier.fillMaxWidth(),
                fillColor = extended.goldInk,
            )
        }
    }
}

@Composable
private fun MomentSummaryPanel(
    moment: Moment,
    status: EventTimeStatus,
    days: Long,
    isAchievement: Boolean,
) {
    val extended = LocalMmExtendedColors.current
    val accent = if (isAchievement) extended.goldInk else MaterialTheme.colorScheme.secondary
    val summaryContent: @Composable () -> Unit = {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(MomentMarkTokens.SpaceInner),
            verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit),
        ) {
            if (isAchievement) {
                Text(
                    text = "🏆",
                    style = MaterialTheme.typography.displaySmall,
                    color = extended.goldInk,
                )
            }
            Text(
                text = if (isAchievement) "🏆 ACHIEVEMENT · 成就珍藏中" else "⧖ QUEST · 倒数前往",
                style = MaterialTheme.typography.labelSmall,
                color = accent,
            )
            Text(moment.title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = when (status) {
                    EventTimeStatus.TODAY -> "就是今天"
                    EventTimeStatus.FUTURE -> "$days 天后开启"
                    EventTimeStatus.PAST -> "已 $days 天"
                },
                style = MaterialTheme.typography.displayMedium,
                color = accent,
            )
            Text(
                text = "${if (isAchievement) "SINCE" else "TARGET"} ${moment.anchorDate.format(DetailDateFormatter)}" +
                    (moment.groupId?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.labelSmall,
                color = extended.labelTertiary,
            )
        }
    }
    if (isAchievement) {
        AchievementFrame(modifier = Modifier.fillMaxWidth()) { summaryContent() }
    } else {
        PixelPanel(
            modifier = Modifier.fillMaxWidth(),
            shadowOffset = MomentMarkTokens.PxShadowOffsetLarge,
        ) { summaryContent() }
    }
}

@Composable
private fun MomentStoryPanel(moment: Moment, isAchievement: Boolean) {
    if (moment.note.isBlank()) return
    PixelPanel(modifier = Modifier.fillMaxWidth(), shadowColor = MaterialTheme.colorScheme.outline) {
        Column(Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceInner)) {
            Text(
                text = if (isAchievement) "▸ 成就故事 STORY" else "▸ 任务背景 LORE",
                style = MaterialTheme.typography.labelSmall,
                color = LocalMmExtendedColors.current.labelTertiary,
            )
            Text(
                text = moment.note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = MomentMarkTokens.SpaceUnit * 2),
            )
        }
    }
}

@Composable
private fun DetailMessage(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = color)
}

private fun EventTimeStatus?.isAchievementDisplay(moment: Moment): Boolean =
    this == EventTimeStatus.PAST ||
        (this == EventTimeStatus.TODAY && moment.creationDirection == MomentDirection.PAST_ACHIEVEMENT)
