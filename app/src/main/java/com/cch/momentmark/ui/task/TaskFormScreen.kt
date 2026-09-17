package com.cch.momentmark.ui.task

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.cch.momentmark.ui.components.PageTitlePanel
import com.cch.momentmark.ui.components.PixelPanel
import com.cch.momentmark.ui.components.PixelStatusHud
import com.cch.momentmark.ui.components.PixelTextInput
import com.cch.momentmark.ui.theme.MomentMarkTokens
import java.time.LocalDate

/**
 * J 第 ④ 屏「接受新任务」表单：仅从日子簿入口进入，保存后回到任务所属日期。
 *
 * 字段顺序、面板结构、辅助文字与 design.html 第 ④ 屏（`4 新任务 · 待办`）一一对应：
 *  - 顶部 `.statusbar mono`（LV.07 / NEW QUEST）
 *  - `.pagetitle.px` 带返回方块 + 「▶ 接受新任务」/ HUD「待办 · 来自日子簿」
 *  - ▸ 任务名称 NAME ★（单行白框）
 *  - ▸ 截止日期 DUE ★ + 右上 [ ] 全天 + 两白框（日期 1.4 + 时间 1）
 *  - ▸ 任务描述 LORE（多行 notebox + helper `选填 · 攻略/备注/链接`）
 *  - ▸ 难度 DIFFICULTY（等宽 17sp 星串）
 *  - `.btn.green.px` 提交按钮 `▶ 开始执行`
 *
 * 表单不显示底部导航，由 [MomentMarkApp] 在 overlay 模式下整体替换骨架接管。
 */
@Composable
fun TaskFormScreen(
    viewModel: TaskFormViewModel,
    taskId: String?,
    onSaved: (LocalDate) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(taskId) { viewModel.start(taskId) }
    LaunchedEffect(state.savedDueDate) {
        state.savedDueDate?.let { dueDate ->
            viewModel.consumeSaved()
            onSaved(dueDate)
        }
    }
    TaskFormContent(
        state = state,
        onTitleChanged = viewModel::updateTitle,
        onDateChanged = viewModel::updateDate,
        onTimeChanged = viewModel::updateTime,
        onNoteChanged = viewModel::updateNote,
        onDifficultySelected = viewModel::selectDifficulty,
        onSave = viewModel::submit,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
internal fun TaskFormContent(
    state: TaskFormUiState,
    onTitleChanged: (String) -> Unit,
    onDateChanged: (String) -> Unit,
    onTimeChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onDifficultySelected: (Int?) -> Unit,
    onSave: () -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    // design.html 结构：`.statusbar` 与 `.pagetitle` 固定，只有 `.form-scroll` 滚动。
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding(),
    ) {
        // design.html `.statusbar{padding:11px 18px 5px}`：左右 18dp。
        PixelStatusHud(
            leftText = "LV.07",
            rightText = "NEW QUEST",
        )
        // 设计稿表单容器：`<div style="padding:0 12px;...gap:10px">` —— 左右 12dp、面板间 10dp。
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MomentMarkTokens.HomePagePaddingHorizontal)
                // design.html `.form-scroll` 内容直接贴屏幕底部；
                // App 内为提交按钮留出余量，避免按钮紧贴屏幕边缘/手势条。
                .padding(bottom = MomentMarkTokens.FormScrollPaddingBottom),
            // 字段间距 = SpaceCard(10dp)，与 design `.form-scroll` 各 `.px` 之间的 10px 一致。
            verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCard),
        ) {
            PageTitlePanel(
                title = if (state.editingTaskId == null) "▶ 接受新任务" else "✎ 编辑任务",
                hud = if (state.editingTaskId == null) "待办 · 来自日子簿" else "TASK · EDIT",
                onBack = onBack,
            )

            // 1) ▸ 任务名称 NAME ★
            PixelTextInput(
                label = "▸ 任务名称 NAME ★",
                value = state.title,
                placeholder = "例如：提交项目周报",
                contentDescription = "任务名称",
                onValueChange = onTitleChanged,
                singleLine = true,
            )

            // 2) ▸ 截止日期 DUE ★ + 右上 [ ] 全天 + 两白框（日期 1.4 + 时间 1）
            DueDateTimePanel(
                dateText = state.dueDateText,
                timeText = state.dueTimeText,
                onDateChanged = onDateChanged,
                onTimeChanged = onTimeChanged,
            )

            // 3) ▸ 任务描述 LORE（多行；helper `选填 · 攻略/备注/链接`）
            PixelTextInput(
                label = "▸ 任务描述 LORE",
                value = state.note,
                placeholder = "攻略、备注或链接",
                contentDescription = "任务描述，可选",
                onValueChange = onNoteChanged,
                singleLine = false,
                helperLabel = "选填 · 攻略/备注/链接",
            )

            // 4) ▸ 难度 DIFFICULTY —— 等宽星串（17sp letterSpacing 3sp）
            DifficultyPanel(
                difficulty = state.difficulty,
                onDifficultySelected = onDifficultySelected,
            )

            // 错误提示
            state.errorMessage?.let {
                Text(
                    text = "⚠ $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            // 5) 提交按钮 ▶ 开始执行（`.btn.green.px`）
            SubmitButton(
                isSaving = state.isSaving,
                isEditing = state.editingTaskId != null,
                onSave = onSave,
            )
        }
    }
}

/**
 * design.html `.btn.green.mono.px` 提交按钮：`.px` 内 padding 12/0, font-size 14px, font-weight 800,
 * `color:#fff` 背景 `var(--hp)` (`#46C168` = primary)，全宽。
 *
 * 同时承载编辑态文案「✎ 保存修改 · 返回日子簿」（与第 ②③ 屏保持一致）。
 */
@Composable
private fun SubmitButton(
    isSaving: Boolean,
    isEditing: Boolean,
    onSave: () -> Unit,
) {
    val text = when {
        isSaving -> "保存中…"
        isEditing -> "✎ 保存修改 · 返回日子簿"
        else -> "▶ 开始执行"
    }
    PixelPanel(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "确认接受任务" }
            .clickable(enabled = !isSaving, role = Role.Button, onClick = onSave),
        backgroundColor = MaterialTheme.colorScheme.primary,
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
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MomentMarkTokens.FormSubmitPaddingVertical),
            textAlign = TextAlign.Center,
        )
    }
}
