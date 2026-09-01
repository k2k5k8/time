package com.cch.momentmark.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import com.cch.momentmark.BuildConfig
import com.cch.momentmark.data.SampleMoments
import com.cch.momentmark.ui.app.MainTab
import com.cch.momentmark.ui.app.MomentMarkNavState
import com.cch.momentmark.ui.app.OverlayScreen
import com.cch.momentmark.ui.components.PixelBottomNav
import com.cch.momentmark.ui.daybook.DaybookScreen
import com.cch.momentmark.ui.daybook.DaybookViewModel
import com.cch.momentmark.ui.daybook.DaybookViewModelFactory
import com.cch.momentmark.ui.moment.MomentFormScreen
import com.cch.momentmark.ui.moment.MomentFormViewModel
import com.cch.momentmark.ui.moment.MomentFormViewModelFactory
import com.cch.momentmark.ui.moment.detail.MomentDetailScreen
import com.cch.momentmark.ui.moment.detail.MomentDetailViewModel
import com.cch.momentmark.ui.moment.detail.MomentDetailViewModelFactory
import com.cch.momentmark.ui.moment.home.MomentHomeScreen
import com.cch.momentmark.ui.moment.home.MomentHomeViewModel
import com.cch.momentmark.ui.moment.home.MomentHomeViewModelFactory
import com.cch.momentmark.ui.task.TaskFormScreen
import com.cch.momentmark.ui.task.TaskFormViewModel
import com.cch.momentmark.ui.task.TaskFormViewModelFactory
import com.cch.momentmark.ui.recyclebin.RecycleBinScreen
import com.cch.momentmark.ui.recyclebin.RecycleBinViewModel
import com.cch.momentmark.ui.recyclebin.RecycleBinViewModelFactory
import com.cch.momentmark.domain.model.RecycleBinItemType
import com.cch.momentmark.ui.system.SystemSettingsScreen
import com.cch.momentmark.ui.system.ThemeSettingsViewModel
import com.cch.momentmark.ui.system.ThemeSettingsViewModelFactory
import com.cch.momentmark.ui.system.GroupViewModel
import com.cch.momentmark.ui.system.GroupViewModelFactory
import com.cch.momentmark.ui.theme.MomentMarkTheme
import com.cch.momentmark.ui.theme.MomentMarkTokens
import com.cch.momentmark.ui.theme.ThemeMode

private fun Context.requireComponentActivity(): ComponentActivity {
    var current: Context = this
    while (true) {
        if (current is ComponentActivity) return current
        val base = (current as? ContextWrapper)?.baseContext ?: break
        if (base === current) break
        current = base
    }
    error("MomentMarkApp must be hosted by a ComponentActivity")
}

/** 骨架目的地 = 常驻页签 + 可选叠加表单（供 AnimatedContent 按整体切换）。 */
private data class SkeletonDestination(
    val tab: MainTab,
    val overlay: OverlayScreen?,
)

private data class UndoSeal(val type: RecycleBinItemType, val id: String, val title: String)

/**
 * J 最终设计三入口导航骨架（AGENTS.md §2.1）：
 * `◉ 大事件` ｜ `＋ 新时刻`（全局铭刻表单） ｜ `▤ 日子簿`。
 * 返回栈：先关闭叠加表单，再交还系统；页签是根目的地，切换页签会关闭表单。
 */
@Composable
fun MomentMarkApp() {
    val context = LocalContext.current
    val activity = remember(context) { context.requireComponentActivity() }
    val themeSettingsViewModel = remember(activity) {
        ViewModelProvider(
            activity,
            ThemeSettingsViewModelFactory(context.applicationContext),
        )[ThemeSettingsViewModel::class.java]
    }
    val themeMode by themeSettingsViewModel.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val groupViewModel = remember(activity) { ViewModelProvider(activity, GroupViewModelFactory(context.applicationContext))[GroupViewModel::class.java] }
    val groupUiState by groupViewModel.uiState.collectAsState()
    val nav = rememberSaveable(saver = MomentMarkNavState.Saver) { MomentMarkNavState() }
    val homeViewModel = remember(activity) {
        ViewModelProvider(
            activity,
            MomentHomeViewModelFactory(
                context = context.applicationContext,
                seedMoments = if (BuildConfig.DEBUG) SampleMoments.all else emptyList(),
            ),
        )[MomentHomeViewModel::class.java]
    }
    val homeUiState by homeViewModel.uiState.collectAsState()
    val momentFormViewModel = remember(activity) {
        ViewModelProvider(
            activity,
            MomentFormViewModelFactory(context.applicationContext),
        )[MomentFormViewModel::class.java]
    }
    val momentDetailViewModel = remember(activity) {
        ViewModelProvider(
            activity,
            MomentDetailViewModelFactory(context.applicationContext),
        )[MomentDetailViewModel::class.java]
    }
    val daybookViewModel = remember(activity) {
        ViewModelProvider(activity, DaybookViewModelFactory(context.applicationContext))[DaybookViewModel::class.java]
    }
    val daybookUiState by daybookViewModel.uiState.collectAsState()
    val taskFormViewModel = remember(activity) {
        ViewModelProvider(activity, TaskFormViewModelFactory(context.applicationContext))[TaskFormViewModel::class.java]
    }
    // 随 App 创建一次：自动净化只有开启时才执行，且只在本次启动路径运行。
    val recycleBinViewModel = remember(activity) {
        ViewModelProvider(activity, RecycleBinViewModelFactory(context.applicationContext))[RecycleBinViewModel::class.java]
    }
    val recycleBinUiState by recycleBinViewModel.uiState.collectAsState()
    var undoSeal by remember { mutableStateOf<UndoSeal?>(null) }

    BackHandler(enabled = nav.overlay != null) { nav.back() }

    // 日子簿是夜间「副本地图」空间：无论全局主题为何，该页签固定洞窟配色（PRD §3.1）；
    // 叠加表单（J ②③④）按设计使用昼间配色，返回时切回来源页签的配色。
    val screenThemeMode = when {
        nav.selectedTab == MainTab.DAYBOOK && nav.overlay in setOf(null, OverlayScreen.SETTINGS, OverlayScreen.RECYCLE_BIN) -> ThemeMode.DARK
        else -> themeMode
    }

    MomentMarkTheme(themeMode = screenThemeMode) {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            AnimatedContent(
                targetState = SkeletonDestination(nav.selectedTab, nav.overlay),
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    // 8-bit 式快速硬切过渡：不做位移动画
                    (fadeIn(tween(MomentMarkTokens.MotionPressMs)) togetherWith
                        fadeOut(tween(MomentMarkTokens.MotionPressMs)))
                },
                label = "skeleton-destination",
            ) { destination ->
                when (destination.overlay) {
                    OverlayScreen.MOMENT_FORM -> MomentFormScreen(
                        viewModel = momentFormViewModel,
                        momentId = nav.editingMomentId,
                        onSaved = nav::completeMomentForm,
                        modifier = Modifier.fillMaxSize(),
                    )
                    OverlayScreen.MOMENT_DETAIL -> MomentDetailScreen(
                        momentId = requireNotNull(nav.selectedMomentId) {
                            "Moment detail requires a selected Moment ID"
                        },
                        viewModel = momentDetailViewModel,
                        onBack = nav::back,
                        onEdit = nav::openMomentEdit,
                        onSealed = { moment ->
                            undoSeal = UndoSeal(RecycleBinItemType.MOMENT, moment.id, moment.title)
                            nav.back()
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    OverlayScreen.TASK_FORM -> TaskFormScreen(
                        viewModel = taskFormViewModel,
                        taskId = nav.editingTaskId,
                        onSaved = { dueDate ->
                            daybookViewModel.selectDate(dueDate)
                            nav.back()
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    OverlayScreen.SETTINGS -> SystemSettingsScreen(
                        recycleBinState = recycleBinUiState,
                        onBack = nav::back,
                        onOpenRecycleBin = nav::openRecycleBin,
                        onAutoPurgeChanged = recycleBinViewModel::setAutoPurgeEnabled,
                        groupState = groupUiState,
                        onCreateGroup = groupViewModel::create,
                        onRenameGroup = groupViewModel::rename,
                        onDissolveGroup = groupViewModel::dissolve,
                        onReorderGroups = groupViewModel::reorder,
                        modifier = Modifier.fillMaxSize(),
                    )
                    OverlayScreen.RECYCLE_BIN -> RecycleBinScreen(
                        uiState = recycleBinUiState,
                        clock = java.time.Clock.systemDefaultZone(),
                        onBack = nav::openSettings,
                        onRestore = recycleBinViewModel::restore,
                        onPermanentlyDelete = recycleBinViewModel::permanentlyDelete,
                        onPurgeAll = recycleBinViewModel::purgeAll,
                        modifier = Modifier.fillMaxSize(),
                    )
                    null -> when (destination.tab) {
                        MainTab.HOME -> MomentHomeScreen(
                            uiState = homeUiState,
                            onMomentSelected = nav::openMomentDetail,
                            onPinnedMove = homeViewModel::movePinned,
                            modifier = Modifier.fillMaxSize(),
                        )
                        MainTab.DAYBOOK -> DaybookScreen(
                            uiState = daybookUiState,
                            onAcceptNewTask = nav::openTaskForm,
                            onOpenSettings = nav::openSettings,
                            onDateSelected = daybookViewModel::selectDate,
                            onMonthChanged = daybookViewModel::changeMonth,
                            onTaskCompletionToggled = daybookViewModel::toggleCompleted,
                            onTaskEditRequested = nav::openTaskEdit,
                            onTaskSealRequested = { task ->
                                daybookViewModel.seal(task) {
                                    undoSeal = UndoSeal(RecycleBinItemType.TASK, task.id, task.title)
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            undoSeal?.let { sealed ->
                UndoSealBanner(
                    title = sealed.title,
                    onUndo = {
                        recycleBinViewModel.restore(sealed.type, sealed.id)
                        undoSeal = null
                    },
                    onDismiss = { undoSeal = null },
                )
            }
            PixelBottomNav(
                selectedTab = nav.selectedTab,
                onTabSelected = nav::selectTab,
                onNewMoment = nav::openMomentForm,
            )
        }
    }
}

@Composable
private fun UndoSealBanner(title: String, onUndo: () -> Unit, onDismiss: () -> Unit) {
    com.cch.momentmark.ui.components.PixelPanel(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.inverseSurface,
        borderColor = MaterialTheme.colorScheme.outline,
        shadowColor = MaterialTheme.colorScheme.outline,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceInner),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            androidx.compose.material3.Text("⚔「$title」已封印", color = MaterialTheme.colorScheme.tertiary)
            Row(horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCard)) {
                androidx.compose.material3.Text("↩ 撤销", color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.clickable(onClick = onUndo))
                androidx.compose.material3.Text("×", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clickable(onClick = onDismiss))
            }
        }
    }
}
