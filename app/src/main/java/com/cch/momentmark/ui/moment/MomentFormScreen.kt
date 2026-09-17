package com.cch.momentmark.ui.moment

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cch.momentmark.ui.components.GroupChipItem
import com.cch.momentmark.ui.components.GroupChipRail
import com.cch.momentmark.ui.components.PageTitlePanel
import com.cch.momentmark.ui.components.PixelDateSelector
import com.cch.momentmark.ui.components.PixelFieldLabel
import com.cch.momentmark.ui.components.PixelPanel
import com.cch.momentmark.ui.components.PixelPanelVariant
import com.cch.momentmark.ui.components.PixelStarRating
import com.cch.momentmark.ui.components.PixelStatusHud
import com.cch.momentmark.ui.components.PixelTextInput
import com.cch.momentmark.domain.model.MomentDirection
import com.cch.momentmark.data.repository.Group
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MomentMarkTokens

/**
 * 铭刻时刻表单（design.html 第 ②③ 屏）：新建或预填编辑同一条 Moment，保存后由导航决定返回首页/详情。
 *
 * 视觉与构造对齐 design.html `.phone` 内 form-scroll；同一组件由 MomentDirection 切换未来 / 过去两种文案与配色。
 */
@Composable
fun MomentFormScreen(
    viewModel: MomentFormViewModel,
    momentId: String?,
    onSaved: (savedMomentId: String, wasEditing: Boolean) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(momentId) { viewModel.start(momentId) }
    LaunchedEffect(state.savedMomentId) {
        state.savedMomentId?.let { savedMomentId ->
            val wasEditing = state.savedWasEditing
            viewModel.consumeSaved()
            onSaved(savedMomentId, wasEditing)
        }
    }
    MomentFormContent(
        state = state,
        onDirectionSelected = viewModel::selectDirection,
        onTitleChanged = viewModel::updateTitle,
        onNoteChanged = viewModel::updateNote,
        onGroupChanged = viewModel::updateGroup,
        onRaritySelected = viewModel::selectRarity,
        onDateChanged = viewModel::updateAnchorDate,
        onYearSelected = viewModel::selectYear,
        onMonthSelected = viewModel::selectMonth,
        onDaySelected = viewModel::selectDay,
        onSave = viewModel::submit,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
internal fun MomentFormContent(
    state: MomentFormUiState,
    onDirectionSelected: (MomentDirection) -> Unit,
    onTitleChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onGroupChanged: (String) -> Unit,
    onRaritySelected: (Int?) -> Unit,
    onDateChanged: (String) -> Unit,
    onYearSelected: (Int) -> Unit = {},
    onMonthSelected: (Int) -> Unit = {},
    onDaySelected: (Int) -> Unit = {},
    onSave: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val extended = LocalMmExtendedColors.current
    // design.html 结构：`.statusbar` 与 `.pagetitle` 固定，只有 `.form-scroll` 滚动。
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding(),
    ) {
        // `.statusbar{padding:11px 18px 5px}`：左右 18dp，由组件自身的水平 padding 提供。
        PixelStatusHud(
            leftText = "LV.07",
            rightText = "NEW MOMENT",
        )
        // `.pagetitle{margin:0 12px 10px}`：左右 12dp、下 10dp；其余 `margin:0`。
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                // design.html `.form-scroll`：左右 12dp + 字段间 10dp。
                // 底部另加 FormScrollPaddingBottom：HTML 里内容直接贴屏幕底部，
                // App 内为提交按钮留出余量，避免按钮紧贴屏幕边缘/手势条。
                .padding(
                    start = MomentMarkTokens.HomePagePaddingHorizontal,
                    end = MomentMarkTokens.HomePagePaddingHorizontal,
                    bottom = MomentMarkTokens.FormScrollPaddingBottom,
                ),
            // 字段间距 = SpaceCard(10dp)，与 design.html `.form-scroll` 各 `.px` 之间的 10px 一致。
            verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCard),
        ) {
            PageTitlePanel(
                title = if (state.editingMomentId == null) "✦ 铭刻时刻" else "✎ 编辑时刻",
                hud = if (state.editingMomentId == null) "首页大事件 · MARK" else "MOMENT · EDIT",
                onBack = onBack,
            )
        // 这个时刻 DIRECTION：design.html `.px4`（4dp 硬影）+ `padding:11px 12px`。
        // Compose 的 border 内嵌不占位，故 padding 取 11+3 / 12+3。
        PixelPanel(modifier = Modifier.fillMaxWidth(), variant = PixelPanelVariant.Emphasized) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MomentMarkTokens.FormPanelPaddingHorizontal,
                        vertical = MomentMarkTokens.FormPanelEmphasizedPaddingVertical,
                    ),
                verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit * 2),
            ) {
                PixelFieldLabel("▸ 这个时刻 DIRECTION")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DirectionChoice(
                        icon = "⧖",
                        label = "未来 · 倒数",
                        selected = state.direction == MomentDirection.FUTURE_COUNTDOWN,
                        achievement = false,
                        onSelect = { onDirectionSelected(MomentDirection.FUTURE_COUNTDOWN) },
                        modifier = Modifier.weight(1f),
                    )
                    DirectionChoice(
                        icon = "🏆",
                        label = "过去 · 正数",
                        selected = state.direction == MomentDirection.PAST_ACHIEVEMENT,
                        achievement = true,
                        onSelect = { onDirectionSelected(MomentDirection.PAST_ACHIEVEMENT) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    text = if (state.direction == MomentDirection.FUTURE_COUNTDOWN) {
                        "▸ 记录一个期待的日子，倒数前往"
                    } else {
                        "▸ 珍藏一段已经开始的时光，正数相伴"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                    ),
                    color = if (state.direction == MomentDirection.FUTURE_COUNTDOWN) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        extended.goldInk
                    },
                )
            }
        }
        // 时刻名称
        FormField(
            label = "▸ 时刻名称 NAME ★",
            value = state.title,
            hint = "例如：毕业旅行 · 东京",
            contentDescription = "时刻名称",
            onValueChange = onTitleChanged,
        )
        // 日期选择（标签随方向切换，左 helperLabel 跟随过去/未来指示）
        PixelDateSelector(
            label = if (state.direction == MomentDirection.FUTURE_COUNTDOWN) {
                "▸ 目标日期 TARGET DATE ★"
            } else {
                "▸ 开始日期 SINCE DATE ★"
            },
            dateText = state.anchorDateText,
            year = state.selectedYear,
            month = state.selectedMonth,
            day = state.selectedDay,
            onYearSelected = onYearSelected,
            onMonthSelected = onMonthSelected,
            onDaySelected = onDaySelected,
            helperLabel = if (state.direction == MomentDirection.FUTURE_COUNTDOWN) {
                "[x] 全天"
            } else {
                "从这天起 · 每天 +1"
            },
            helperColor = if (state.direction == MomentDirection.FUTURE_COUNTDOWN) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.tertiary
            },
        )
        // 任务背景 / 成就故事（label 右侧 mono 提示见 design.html `.px` 内第二行）
        // placeholder 是「空输入框的引导语」，不是 design.html 里那段示例正文 ——
        // 后者只是 HTML 稿的展示效果，产品中不应作为提示复现（用户 2026-09-14 要求）。
        FormField(
            label = if (state.direction == MomentDirection.FUTURE_COUNTDOWN) {
                "▸ 任务背景 LORE"
            } else {
                "▸ 成就故事 STORY"
            },
            value = state.note,
            hint = if (state.direction == MomentDirection.FUTURE_COUNTDOWN) {
                "写下令你期待这段日子的理由…"
            } else {
                "写下这段成就背后的故事…"
            },
            contentDescription = "时刻故事，可选",
            onValueChange = onNoteChanged,
            isMultiline = true,
            helperLabel = if (state.direction == MomentDirection.FUTURE_COUNTDOWN) {
                "选填 · 将显示在详情页"
            } else {
                "选填 · 将显示在成就页"
            },
        )
        // 分组线路 + 重要度：design 把两个分组在同一 `.px` panel 内排版。
        GroupAndRarityPanel(
            groups = state.groups,
            selectedGroupId = state.groupId,
            rarity = state.rarity,
            isPast = state.direction == MomentDirection.PAST_ACHIEVEMENT,
            onGroupChanged = onGroupChanged,
            onRaritySelected = onRaritySelected,
        )
        // 过去模式专有：周年提醒行（design.html `.px` flex between）。仅视觉复刻，未接入提醒逻辑。
        if (state.direction == MomentDirection.PAST_ACHIEVEMENT) {
            AnniversaryReminderPanel(anchorDateText = state.anchorDateText)
        }
        state.errorMessage?.let { message ->
            Text(
                text = "⚠ $message",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        SubmitButton(
            direction = state.direction,
            isSaving = state.isSaving,
            isEditing = state.editingMomentId != null,
            onSave = onSave,
        )
        }
    }
}

/**
 * design.html 第 ③ 屏 `.px` 周年提醒行：左 `🔔 周年提醒`，右 `[x] 每年 MM.DD 提醒我`。
 *
 * 目前只复刻视觉；通知/提醒属于 P1，未接入。文案里的 MM.DD 由锚定日期派生。
 */
@Composable
private fun AnniversaryReminderPanel(anchorDateText: String) {
    val md = runCatching {
        val date = java.time.LocalDate.parse(anchorDateText)
        "%02d.%02d".format(date.monthValue, date.dayOfMonth)
    }.getOrElse { "--.--" }
    PixelPanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MomentMarkTokens.FormPanelPaddingHorizontal,
                    vertical = MomentMarkTokens.FormPanelPaddingVertical,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "🔔 周年提醒",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "[x] 每年 $md 提醒我",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.8.sp,
                    lineHeight = 11.sp,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun DirectionChoice(
    icon: String,
    label: String,
    selected: Boolean,
    achievement: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = when {
        !selected -> MaterialTheme.colorScheme.surface
        achievement -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.secondary
    }
    val fg = when {
        !selected -> MaterialTheme.colorScheme.onSurface
        achievement -> MaterialTheme.colorScheme.onTertiary
        else -> MaterialTheme.colorScheme.onSecondary
    }
    Box(
        modifier = modifier
            .clickable(role = Role.RadioButton, onClick = onSelect)
            .semantics(mergeDescendants = true) {
                contentDescription = label
                role = Role.RadioButton
            },
    ) {
        PixelPanel(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = bg,
            borderColor = MaterialTheme.colorScheme.outline,
            shadowColor = MaterialTheme.colorScheme.outline,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MomentMarkTokens.FormDirectionButtonHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = icon,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 17.sp, lineHeight = 20.sp),
                    color = fg,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                    ),
                    color = fg,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    hint: String,
    contentDescription: String,
    onValueChange: (String) -> Unit,
    isMultiline: Boolean = false,
    helperLabel: String? = null,
) {
    PixelTextInput(
        label = label,
        value = value,
        placeholder = hint,
        contentDescription = contentDescription,
        onValueChange = onValueChange,
        singleLine = !isMultiline,
        helperLabel = helperLabel,
    )
}

@Composable
private fun GroupAndRarityPanel(
    groups: List<Group>,
    selectedGroupId: String,
    rarity: Int?,
    isPast: Boolean,
    onGroupChanged: (String) -> Unit,
    onRaritySelected: (Int?) -> Unit,
) {
    val selectedBorder = if (isPast) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
    val selectedBg = if (isPast) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
    PixelPanel(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MomentMarkTokens.FormPanelPaddingHorizontal,
                    vertical = MomentMarkTokens.FormPanelPaddingVertical,
                ),
        ) {
            PixelFieldLabel("▸ 分组线路 PARTY")
            val chipItems = groups.map { GroupChipItem(it.id, it.name, it.iconKey) }
            // P0 当前还没有真实分组时，给出一组示例 chip 与设计稿对应（仅在 debug 截屏时启用）。
            val items = if (chipItems.isEmpty()) defaultGroupChips else chipItems
            // design.html `.party-rail{...;margin-top:8px}`；首项就是「🎓 学业」，没有「无阵营」chip。
            GroupChipRail(
                items = items,
                selectedGroupId = selectedGroupId,
                onGroupSelected = onGroupChanged,
                selectedBorderColor = selectedBorder,
                selectedBackgroundColor = selectedBg,
                includeNone = false,
                modifier = Modifier.padding(top = MomentMarkTokens.SpaceCompact),
            )
            // design.html 该行 `margin-top:10px`：左 label、右 3 星触摸行（13/17/21px 递进字号）。
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MomentMarkTokens.FormRarityRowGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PixelFieldLabel("▸ 重要度 RARITY", modifier = Modifier.weight(1f))
                PixelStarRating(
                    value = rarity,
                    onValueChange = onRaritySelected,
                    progressive = true,
                    contentDescription = { "$it 星重要度" },
                )
            }
        }
    }
}

@Composable
private fun SubmitButton(direction: MomentDirection, isSaving: Boolean, isEditing: Boolean, onSave: () -> Unit) {
    val bg = when (direction) {
        MomentDirection.FUTURE_COUNTDOWN -> MaterialTheme.colorScheme.secondary
        MomentDirection.PAST_ACHIEVEMENT -> MaterialTheme.colorScheme.tertiary
    }
    val fg = when (direction) {
        MomentDirection.FUTURE_COUNTDOWN -> MaterialTheme.colorScheme.onSecondary
        MomentDirection.PAST_ACHIEVEMENT -> MaterialTheme.colorScheme.onTertiary
    }
    val text = when {
        isSaving -> "保存中…"
        isEditing -> "✎ 保存修改 · 返回时刻详情"
        direction == MomentDirection.FUTURE_COUNTDOWN -> "⧖ 铭刻 · 开始倒数！"
        else -> "🏆 铭刻 · 开始珍藏！"
    }
    PixelPanel(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "确认铭刻时刻" }
            .clickable(enabled = !isSaving, role = Role.Button, onClick = onSave),
        backgroundColor = bg,
        borderColor = MaterialTheme.colorScheme.outline,
        shadowColor = MaterialTheme.colorScheme.outline,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 20.sp,
            ),
            color = fg,
            modifier = Modifier
                .fillMaxWidth()
                // design.html `.btn.px{padding:12px 0;border:3px}` → Compose 取 12+3。
                .padding(vertical = MomentMarkTokens.FormSubmitPaddingVertical),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

/** 截屏 fixture：design.html 第 ②③ 屏的预置分组 chip。 */
private val defaultGroupChips: List<GroupChipItem> = listOf(
    GroupChipItem("study", "学业", "study"),
    GroupChipItem("heart", "生活", "heart"),
    GroupChipItem("travel", "旅行", "travel"),
    GroupChipItem("work", "工作", "work"),
    GroupChipItem("family", "家庭", "family"),
    GroupChipItem("health", "健康", "health"),
    GroupChipItem("memory", "纪念", "memory"),
    GroupChipItem("inspire", "灵感", "inspire"),
)
