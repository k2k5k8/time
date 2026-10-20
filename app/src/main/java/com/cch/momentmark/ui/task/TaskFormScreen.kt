package com.cch.momentmark.ui.task

import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.core.view.WindowCompat
import android.app.Activity
import android.view.WindowManager
import android.content.ContextWrapper
import com.cch.momentmark.ui.theme.LocalMomentMarkNight
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cch.momentmark.ui.components.PageTitlePanel
import com.cch.momentmark.ui.components.PixelButton
import com.cch.momentmark.ui.components.PixelButtonIcon
import com.cch.momentmark.ui.components.PixelButtonVariant
import androidx.compose.ui.platform.testTag
import com.cch.momentmark.ui.components.PixelTextInput
import com.cch.momentmark.ui.theme.MomentMarkTokens
import java.time.LocalDate
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

/** 日子簿规划表单；保存与取消均返回入口浏览日，独立于任务所属日。 */
@Composable
fun TaskFormScreen(
    viewModel: TaskFormViewModel,
    taskId: String?,
    onSaved: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    selectedDate: LocalDate? = null,
    onAccepted: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val viewModelContext = LocalView.current.context
    var started by rememberSaveable(taskId) { mutableStateOf(false) }
    val latestSaved by androidx.compose.runtime.rememberUpdatedState(onSaved)
    val latestAccepted by androidx.compose.runtime.rememberUpdatedState(onAccepted)
    val activity = remember(viewModelContext) {
        generateSequence(viewModelContext) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<androidx.activity.ComponentActivity>().first()
    }
    val lifecycle = activity.lifecycle
    LaunchedEffect(lifecycle, viewModel) {
        lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            while (true) { viewModel.refreshDateShortcuts(); kotlinx.coroutines.delay(1000) }
        }
    }
    val finish = {
        val saved = viewModel.uiState.value
        viewModel.consumeSaved()?.let { date ->
            if (saved.editingTaskId == null && saved.savedDueDate == date) saved.savedTaskId?.let(latestAccepted)
            latestSaved(date)
        }
    }
    val latestFinish by androidx.compose.runtime.rememberUpdatedState(finish)
    val returnToDaybook: () -> Unit = {
        if (viewModel.uiState.value.savedDueDate != null) latestFinish() else onBack?.invoke()
    }
    androidx.activity.compose.BackHandler(enabled = onBack != null) { returnToDaybook() }
    DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP && !activity.isChangingConfigurations) latestFinish()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    var feedbackStarted by rememberSaveable(taskId) { mutableStateOf(false) }
    LaunchedEffect(viewModel, taskId) {
        // 先初始化本次表单，再监听成功，不能把上次退出后的事件送给新表单。
        if (!started || !viewModel.hasSession) {
            viewModel.start(taskId, selectedDate ?: LocalDate.parse(state.quickTodayDateText.replace('.', '-')))
            started = true
        }
        viewModel.uiState.map { it.savedDueDate }.filterNotNull().collect {
            val scale = android.provider.Settings.Global.getFloat(
                viewModelContext.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            if (!feedbackStarted && scale > 0f && lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                feedbackStarted = true
                kotlinx.coroutines.delay(((if (taskId == null) MomentMarkTokens.TaskAcceptFeedbackMs else MomentMarkTokens.TaskEditFeedbackMs) * scale).toLong())
            }
            if (!activity.isChangingConfigurations && lifecycle.currentState != androidx.lifecycle.Lifecycle.State.DESTROYED) latestFinish()
        }
    }
    androidx.compose.foundation.layout.Box(modifier) {
        TaskFormContent(
            state = state,
            onTitleChanged = viewModel::updateTitle,
            onNoteChanged = viewModel::updateNote,
            onDifficultySelected = viewModel::selectDifficulty,
            onSave = viewModel::submit,
            onBack = returnToDaybook,
            modifier = Modifier,
            planning = { extras -> TaskPlanningPanel(state, viewModel, extras) },
            onExtrasToggle = viewModel::toggleExtras,
        )
        if (state.savedDueDate != null) TaskSaveFeedback(state.editingTaskId != null)
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun TaskFormContent(
    state: TaskFormUiState,
    onTitleChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onDifficultySelected: (Int?) -> Unit,
    onSave: () -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    planning: @Composable (@Composable () -> Unit) -> Unit,
    onExtrasToggle: (() -> Unit)? = null,
) {
    var localExtrasOpen by rememberSaveable { mutableStateOf(false) }
    val extrasOpen = if (onExtrasToggle != null) state.extrasOpen else localExtrasOpen
    var extrasHeadingBottom by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val view = LocalView.current
    val density = LocalDensity.current
    val backTouchTarget = with(density) { (48f * view.resources.displayMetrics.density).toDp() }
        .coerceAtLeast(MomentMarkTokens.FormPlanningTouchTarget)
    val window = remember(view) { generateSequence(view.context) { (it as? ContextWrapper)?.baseContext }
        .filterIsInstance<Activity>().firstOrNull()?.window }
    DisposableEffect(window) {
        // With an unspecified adjustment Android can pan the whole window for lower fields,
        // in addition to Compose's imePadding. Keep the edge-to-edge form on resize insets.
        val originalSoftInputMode = window?.attributes?.softInputMode
        if (originalSoftInputMode != null) window.setSoftInputMode(
            (originalSoftInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST.inv()) or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE,
        )
        onDispose {
            if (originalSoftInputMode != null) {
                window.setSoftInputMode(originalSoftInputMode)
                // Window.setSoftInputMode(0) only clears its explicit-mode flag; it leaves
                // LayoutParams unchanged. Restore that value too when returning to default.
                window.attributes = window.attributes.apply { softInputMode = originalSoftInputMode }
            }
        }
    }
    val controller = remember(window, view) { window?.let { WindowCompat.getInsetsController(it, view) } }
    val night = LocalMomentMarkNight.current
    DisposableEffect(controller) {
        val status = controller?.isAppearanceLightStatusBars
        val navigation = controller?.isAppearanceLightNavigationBars
        onDispose { if (status != null) controller.isAppearanceLightStatusBars = status; if (navigation != null) controller.isAppearanceLightNavigationBars = navigation }
    }
    SideEffect { controller?.isAppearanceLightStatusBars = !night; controller?.isAppearanceLightNavigationBars = !night }
    val scroll = rememberScrollState()
    val revealViewport = remember(scroll) { TaskFormRevealViewport(scroll) }
    val imeVisible = WindowInsets.isImeVisible
    SideEffect { revealViewport.imeVisible = imeVisible }
    CompositionLocalProvider(LocalTaskFormRevealViewport provides revealViewport) {
        // Task keeps one compact title inside the scroll area; submit stays above the IME.
        Column(
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            // 设计稿表单容器：`<div style="padding:0 12px;...gap:10px">` —— 左右 12dp、面板间 10dp。
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onGloballyPositioned {
                        val position = it.positionInRoot()
                        revealViewport.bounds = Rect(position.x, position.y, position.x + it.size.width, position.y + it.size.height)
                    }
                    .verticalScroll(scroll)
                    .padding(horizontal = MomentMarkTokens.HomePagePaddingHorizontal)
                    // design.html `.form-scroll` 内容直接贴屏幕底部；
                    // App 内为提交按钮留出余量，避免按钮紧贴屏幕边缘/手势条。
                    .padding(bottom = MomentMarkTokens.FormScrollPaddingBottom),
                // 字段间距 = SpaceCard(10dp)，与 design `.form-scroll` 各 `.px` 之间的 10px 一致。
                verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCard),
            ) {
                PageTitlePanel(
                    title = if (state.editingTaskId == null) "接受新任务" else "编辑任务",
                    onBack = onBack,
                    backTouchTarget = backTouchTarget,
                    compact = true,
                    hudFollowsTitle = true,
                    hud = runCatching { LocalDate.parse(state.dueDateText.replace('.', '-')) }.getOrNull()?.let { date ->
                        if (date.year == LocalDate.parse(state.quickTodayDateText.replace('.', '-')).year) "%02d.%02d".format(date.monthValue, date.dayOfMonth) else state.dueDateText
                    },
                    backContentDescription = "返回日子簿",
                    modifier = Modifier.testTag("task-form-header"),
                )

                // Task title remains the only required input.
                PixelTextInput(
                    label = "任务名称",
                    value = state.title,
                    placeholder = "",
                    contentDescription = "任务名称",
                    onValueChange = onTitleChanged,
                    singleLine = true,
                )

                planning {
                    TaskFormReveal(state.extrasAvailable || state.editingTaskId != null, "form-extras") {
                        TaskFormSection("task-section-extras") {
                            TaskFormSectionLabel("附加信息")
                            TaskFormAction(state.extrasSummary(),
                                { if (onExtrasToggle != null) onExtrasToggle() else localExtrasOpen = !localExtrasOpen },
                                Modifier.fillMaxWidth(), PixelButtonIcon.Note,
                                enabled = !state.isLoading && !state.isSaving && !state.isSaved, expanded = extrasOpen, field = true)
                            TaskFormReveal(extrasOpen, "form-note", headingBottom = extrasHeadingBottom) {
                                // Notes stay in state when their disclosure is collapsed.
                                PixelTextInput(
                                    label = "备注",
                                    value = state.note,
                                    placeholder = "",
                                    contentDescription = "任务描述，可选",
                                    onValueChange = onNoteChanged,
                                    singleLine = false,
                                    framed = false,
                                    followCursor = imeVisible,
                                    labelModifier = Modifier.onGloballyPositioned {
                                        extrasHeadingBottom = it.positionInRoot().y + it.size.height
                                    },
                                )

                                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                DifficultyPanel(
                                    framed = false,
                                    difficulty = state.difficulty,
                                    onDifficultySelected = onDifficultySelected,
                                    enabled = !state.isLoading && !state.isSaving && !state.isSaved,
                                )
                            }
                        }
                    }

                }

                // 错误提示
                state.errorMessage?.let {
                    Text(
                        text = "⚠ $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

            }
            Column(Modifier.fillMaxWidth().padding(horizontal = MomentMarkTokens.HomePagePaddingHorizontal)
                .padding(vertical = MomentMarkTokens.SpaceInner)) {
                SubmitButton(
                    isSaving = state.isSaving,
                    isSaved = state.isSaved,
                    enabled = !state.isLoading && !state.isSaving && !state.isSaved,
                    isEditing = state.editingTaskId != null,
                    onSave = onSave,
                )
            }
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
    isSaved: Boolean,
    enabled: Boolean,
    isEditing: Boolean,
    onSave: () -> Unit,
) {
    val text = when {
        isSaved -> "已保存"
        isSaving -> "保存中…"
        isEditing -> "✎ 保存修改 · 返回日子簿"
        else -> "开始执行"
    }
    PixelButton(
        label = text,
        onClick = onSave,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        contentDescription = "确认接受任务",
        icon = if (!isEditing && !isSaving && !isSaved) PixelButtonIcon.Play else PixelButtonIcon.Check,
        textStyle = MaterialTheme.typography.labelLarge.copy(
            fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 20.sp,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = MomentMarkTokens.SpaceInner,
            vertical = MomentMarkTokens.FormSubmitPaddingVertical,
        ),
    )
}
