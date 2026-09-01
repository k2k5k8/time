package com.cch.momentmark.ui.moment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import com.cch.momentmark.ui.components.PageTitlePanel
import com.cch.momentmark.ui.components.PixelPanel
import com.cch.momentmark.ui.components.PixelTextInput
import com.cch.momentmark.domain.model.MomentDirection
import com.cch.momentmark.data.repository.Group
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MomentMarkTokens

/**
 * 铭刻时刻表单（J ②③）：新建或预填编辑同一条 Moment，保存后由导航决定返回首页/详情。
 */
@Composable
fun MomentFormScreen(
    viewModel: MomentFormViewModel,
    momentId: String?,
    onSaved: (savedMomentId: String, wasEditing: Boolean) -> Unit,
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
    modifier: Modifier = Modifier,
) {
    val extended = LocalMmExtendedColors.current
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = MomentMarkTokens.SpacePage,
                vertical = MomentMarkTokens.SpaceCard,
            ),
        verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCard),
    ) {
        PageTitlePanel(
            title = if (state.editingMomentId == null) "✦ 铭刻时刻" else "✎ 编辑时刻",
            hud = if (state.editingMomentId == null) "首页大事件 · MARK" else "MOMENT · EDIT",
        )
        PixelPanel(modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(MomentMarkTokens.SpaceInner),
                verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit * 2),
            ) {
                Text(
                    text = "▸ 这个时刻 DIRECTION",
                    style = MaterialTheme.typography.labelSmall,
                    color = extended.labelTertiary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit * 2)) {
                    DirectionChoice(
                        label = "⧖ 未来 · 倒数",
                        selected = state.direction == MomentDirection.FUTURE_COUNTDOWN,
                        achievement = false,
                        onSelect = { onDirectionSelected(MomentDirection.FUTURE_COUNTDOWN) },
                        modifier = Modifier.weight(1f),
                    )
                    DirectionChoice(
                        label = "🏆 过去 · 正数",
                        selected = state.direction == MomentDirection.PAST_ACHIEVEMENT,
                        achievement = true,
                        onSelect = { onDirectionSelected(MomentDirection.PAST_ACHIEVEMENT) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    text = if (state.direction == MomentDirection.FUTURE_COUNTDOWN) {
                        "▸ 记录一个期待的日子，倒数前往。"
                    } else {
                        "▸ 记录一段已经开始的成就，每天 +1。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.direction == MomentDirection.FUTURE_COUNTDOWN) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        extended.goldInk
                    },
                )
            }
        }
        FormField(
            label = "▸ 时刻名称 NAME ★",
            value = state.title,
            hint = "例如：毕业旅行 · 东京",
            contentDescription = "时刻名称",
            onValueChange = onTitleChanged,
        )
        DateTripleSelector(
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
        )
        FormField(
            label = if (state.direction == MomentDirection.FUTURE_COUNTDOWN) {
                "▸ 任务背景 LORE（可选）"
            } else {
                "▸ 成就故事 STORY（可选）"
            },
            value = state.note,
            hint = "把这段故事也铭刻下来",
            contentDescription = "时刻故事，可选",
            onValueChange = onNoteChanged,
        )
        GroupSelector(
            groups = state.groups,
            selectedGroupId = state.groupId,
            onGroupChanged = onGroupChanged,
        )
        RaritySelector(
            rarity = state.rarity,
            onRaritySelected = onRaritySelected,
        )
        state.errorMessage?.let { message ->
            Text(
                text = "⚠ $message",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        PixelPanel(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "确认铭刻时刻" }
                .clickable(
                    enabled = !state.isSaving,
                    role = Role.Button,
                    onClick = onSave,
                ),
            backgroundColor = if (state.direction == MomentDirection.FUTURE_COUNTDOWN) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.tertiary
            },
            borderColor = MaterialTheme.colorScheme.outline,
            shadowColor = MaterialTheme.colorScheme.outline,
        ) {
            Text(
                text = when {
                    state.isSaving -> "保存中…"
                    state.editingMomentId != null -> "✎ 保存修改 · 返回时刻详情"
                    state.direction == MomentDirection.FUTURE_COUNTDOWN -> "⧖ 铭刻 · 开始倒数！"
                    else -> "🏆 铭刻 · 开始珍藏！"
                },
                style = MaterialTheme.typography.titleSmall,
                color = if (state.direction == MomentDirection.FUTURE_COUNTDOWN) {
                    MaterialTheme.colorScheme.onSecondary
                } else {
                    MaterialTheme.colorScheme.onTertiary
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MomentMarkTokens.SpaceInner),
            )
        }
    }
}

@Composable
private fun DirectionChoice(
    label: String,
    selected: Boolean,
    achievement: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            backgroundColor = if (selected) {
                if (achievement) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
            } else MaterialTheme.colorScheme.surface,
            borderColor = MaterialTheme.colorScheme.outline,
            shadowColor = MaterialTheme.colorScheme.outline,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) {
                    if (achievement) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSecondary
                } else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceInner),
            )
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
    singleLine: Boolean = false,
) {
    PixelTextInput(
        label = label,
        value = value,
        placeholder = hint,
        contentDescription = contentDescription,
        onValueChange = onValueChange,
        singleLine = singleLine || label.contains("日期"),
    )
}

@Composable
private fun RaritySelector(rarity: Int?, onRaritySelected: (Int?) -> Unit) {
    val extended = LocalMmExtendedColors.current
    PixelPanel(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceInner),
            verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit * 2),
        ) {
            Text("▸ 重要度 RARITY（可选）", style = MaterialTheme.typography.labelSmall, color = extended.labelTertiary)
            Row(horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit * 2)) {
                (1..3).forEach { value ->
                    PixelPanel(
                        modifier = Modifier.weight(1f).semantics { contentDescription = "$value 星重要度" }
                            .clickable(role = Role.RadioButton, onClick = { onRaritySelected(if (rarity == value) null else value) }),
                        backgroundColor = if (rarity == value) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surface,
                        shadowColor = MaterialTheme.colorScheme.outline,
                    ) {
                        Text(
                            text = "★".repeat(value),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (rarity == value) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceUnit * 2),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupSelector(groups: List<Group>, selectedGroupId: String, onGroupChanged: (String) -> Unit) {
    val extended = LocalMmExtendedColors.current
    PixelPanel(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceInner), verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit * 2)) {
            Text("▸ 分组线路 PARTY（可选）", style = MaterialTheme.typography.labelSmall, color = extended.labelTertiary)
            if (groups.isEmpty()) {
                FormField("", selectedGroupId, "例如：旅行", "时刻分组，可选", onGroupChanged, singleLine = true)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit * 2)) {
                    GroupChip("无阵营", selectedGroupId.isBlank(), { onGroupChanged("") }, Modifier.weight(1f))
                    groups.forEach { group ->
                        GroupChip(group.name, selectedGroupId == group.id, { onGroupChanged(group.id) }, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    PixelPanel(
        modifier = modifier.semantics { contentDescription = "分组 $label" }.clickable(role = Role.RadioButton, onClick = onClick),
        backgroundColor = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
        shadowColor = MaterialTheme.colorScheme.outline,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceCompact))
    }
}

@Composable
private fun DateTripleSelector(
    label: String,
    dateText: String,
    year: Int?,
    month: Int?,
    day: Int?,
    onYearSelected: (Int) -> Unit,
    onMonthSelected: (Int) -> Unit,
    onDaySelected: (Int) -> Unit,
) {
    val fallback = runCatching { java.time.LocalDate.parse(dateText) }.getOrNull()
    val selectedYear = year ?: fallback?.year ?: 2000
    val selectedMonth = month ?: fallback?.monthValue ?: 1
    val selectedDay = day ?: fallback?.dayOfMonth ?: 1
    PixelPanel(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceInner),
            verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit * 2),
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = LocalMmExtendedColors.current.labelTertiary)
            // Moment 当前固定为全天；该标识是事实语义的一部分，不提供取消入口。
            Text("[x] 全天", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit * 2),
            ) {
                DatePartDropdown("年", selectedYear.toString(), (selectedYear - 100..selectedYear + 100).toList(), onYearSelected, Modifier.weight(1f))
                DatePartDropdown("月", "%02d".format(selectedMonth), (1..12).toList(), onMonthSelected, Modifier.weight(1f))
                DatePartDropdown("日", "%02d".format(selectedDay), (1..31).toList(), onDaySelected, Modifier.weight(1f))
            }
            Text(
                text = "已选择 $dateText",
                style = MaterialTheme.typography.labelSmall,
                color = LocalMmExtendedColors.current.labelTertiary,
            )
        }
    }
}

@Composable
private fun DatePartDropdown(
    label: String,
    value: String,
    options: List<Int>,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Box(modifier) {
        PixelPanel(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "${label}选择器，当前$value" }
                .clickable { expanded = !expanded },
            backgroundColor = MaterialTheme.colorScheme.surface,
            shadowColor = MaterialTheme.colorScheme.outline,
        ) {
            Text(
                text = "$label\n$value",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceInner),
            )
        }
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(if (label == "年") option.toString() else "%02d".format(option)) },
                    onClick = { onSelected(option); expanded = false },
                )
            }
        }
    }
}
