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
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.cch.momentmark.ui.components.PixelCanvas
import com.cch.momentmark.ui.components.PixelUndoToast
import com.cch.momentmark.ui.daybook.DaybookScreen
import com.cch.momentmark.ui.daybook.DaybookViewModel
import com.cch.momentmark.ui.daybook.DaybookViewModelFactory
import com.cch.momentmark.ui.daybook.DaybookVisualFixture
import com.cch.momentmark.ui.moment.MomentFormScreen
import com.cch.momentmark.ui.moment.MomentFormViewModel
import com.cch.momentmark.ui.moment.MomentFormViewModelFactory
import com.cch.momentmark.ui.moment.detail.MomentDetailScreen
import com.cch.momentmark.ui.moment.detail.MomentDetailViewModel
import com.cch.momentmark.ui.moment.detail.MomentDetailViewModelFactory
import com.cch.momentmark.ui.moment.detail.MomentDetailVisualFixture
import com.cch.momentmark.ui.moment.home.MomentHomeScreen
import com.cch.momentmark.ui.moment.home.MomentHomeViewModel
import com.cch.momentmark.ui.moment.home.MomentHomeViewModelFactory
import com.cch.momentmark.ui.moment.home.MomentHomeVisualFixture
import com.cch.momentmark.ui.moment.home.buildMapFilterPartyRows
import com.cch.momentmark.ui.task.TaskFormScreen
import com.cch.momentmark.ui.task.TaskFormViewModel
import com.cch.momentmark.ui.task.TaskFormViewModelFactory
import com.cch.momentmark.ui.recyclebin.RecycleBinScreen
import com.cch.momentmark.ui.recyclebin.RecycleBinViewModel
import com.cch.momentmark.ui.recyclebin.RecycleBinViewModelFactory
import com.cch.momentmark.ui.recyclebin.RecycleBinVisualFixture
import com.cch.momentmark.domain.model.RecycleBinItemType
import com.cch.momentmark.ui.system.SystemSettingsScreen
import com.cch.momentmark.ui.system.ThemeSettingsViewModel
import com.cch.momentmark.ui.system.ThemeSettingsViewModelFactory
import com.cch.momentmark.ui.system.GroupViewModel
import com.cch.momentmark.ui.system.GroupViewModelFactory
import com.cch.momentmark.ui.theme.MomentMarkTheme
import com.cch.momentmark.ui.theme.MomentMarkTokens
import com.cch.momentmark.ui.theme.ThemeMode
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

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
 * 仅 DEBUG：读取 `debug.momentmark.nav` 视觉回归 fixture（取值 `task` / `daybook`，其它值 = 正常首页）。
 * 用反射访问 SystemProperties，避免把 `android.os.SystemProperties` 暴露到编译期 API。
 */
private fun readDebugNav(): String = try {
    val clazz = Class.forName("android.os.SystemProperties")
    val get = clazz.getMethod("get", String::class.java, String::class.java)
    (get.invoke(null, "debug.momentmark.nav", "") as? String).orEmpty()
} catch (_: Throwable) {
    ""
}

/**
 * 仅 DEBUG：读取 `debug.momentmark.seed` 首页视觉回归 fixture。
 * 取值 `samples` 时注入 design.html 第①屏示例时刻并冻结时钟到 2026-08-28；
 * 正常启动（未设置）不注入任何示例，首页数据完全来自用户自己的持久化内容。
 */
private fun readDebugSeed(): String = try {
    val clazz = Class.forName("android.os.SystemProperties")
    val get = clazz.getMethod("get", String::class.java, String::class.java)
    (get.invoke(null, "debug.momentmark.seed", "") as? String).orEmpty()
} catch (_: Throwable) {
    ""
}

/**
 * 仅 DEBUG：读取 `debug.momentmark.daybook` 视觉回归 fixture。
 * 取值 `aug12` 时使用固定日期 2026-08-12 与 design.html 第 ⑤ 屏示例任务。
 */
private fun readDebugDaybook(): String = try {
    val clazz = Class.forName("android.os.SystemProperties")
    val get = clazz.getMethod("get", String::class.java, String::class.java)
    (get.invoke(null, "debug.momentmark.daybook", "") as? String).orEmpty()
} catch (_: Throwable) {
    ""
}

/**
 * 仅 DEBUG：读取 `debug.momentmark.detail` 视觉回归 fixture。
 * 取值 `quest` 时启动即进入第 ⑥ 屏任务详情，并注入冻结的 QUEST 展示帧。
 */
private fun readDebugDetail(): String = try {
    val clazz = Class.forName("android.os.SystemProperties")
    val get = clazz.getMethod("get", String::class.java, String::class.java)
    (get.invoke(null, "debug.momentmark.detail", "") as? String).orEmpty()
} catch (_: Throwable) {
    ""
}

/**
 * 仅 DEBUG：读取 `debug.momentmark.drawer` 视觉回归 fixture。
 * 取值 `filter` 时首页直接展开第 ⑧ 屏分类抽屉的冻结画面。
 */
private fun readDebugDrawer(): String = try {
    val clazz = Class.forName("android.os.SystemProperties")
    val get = clazz.getMethod("get", String::class.java, String::class.java)
    (get.invoke(null, "debug.momentmark.drawer", "") as? String).orEmpty()
} catch (_: Throwable) {
    ""
}

/**
 * 仅 DEBUG：读取 `debug.momentmark.settings` 视觉回归 fixture。
 * 取值 `system` 时启动即进入第 ⑨ 屏系统设置（日子簿页签 + SETTINGS 叠加页）。
 */
private fun readDebugSettings(): String = try {
    val clazz = Class.forName("android.os.SystemProperties")
    val get = clazz.getMethod("get", String::class.java, String::class.java)
    (get.invoke(null, "debug.momentmark.settings", "") as? String).orEmpty()
} catch (_: Throwable) {
    ""
}

/**
 * 仅 DEBUG：读取 `debug.momentmark.recyclebin` 视觉回归 fixture。
 * 取值 `graveyard` 时启动即进入第 ⑩ 屏封印之地（日子簿页签 + RECYCLE_BIN 叠加页），
 * 并注入 design.html 第⑩屏示例封印数据。
 */
private fun readDebugRecycleBin(): String = try {
    val clazz = Class.forName("android.os.SystemProperties")
    val get = clazz.getMethod("get", String::class.java, String::class.java)
    (get.invoke(null, "debug.momentmark.recyclebin", "") as? String).orEmpty()
} catch (_: Throwable) {
    ""
}

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
    // 仅 DEBUG 回收站视觉回归 fixture：`setprop debug.momentmark.recyclebin graveyard`
    val debugRecycleBin = if (BuildConfig.DEBUG) readDebugRecycleBin() else ""
    val nav = rememberSaveable(saver = MomentMarkNavState.Saver) {
        // 仅 DEBUG 视觉回归 fixture：`setprop debug.momentmark.nav task` 启动即打开第 ④ 屏表单，
        // 避免截图脚本依赖坐标点击。生产构建始终为首页 + 无叠加页。
        val debugNav = if (BuildConfig.DEBUG) readDebugNav() else ""
        val debugSettings = if (BuildConfig.DEBUG) readDebugSettings() else ""
        val initialTab = when {
            debugNav == "task" || debugNav == "daybook" -> MainTab.DAYBOOK
            debugSettings == "system" -> MainTab.DAYBOOK
            debugRecycleBin == "graveyard" -> MainTab.DAYBOOK
            else -> MainTab.HOME
        }
        val initialOverlay = when {
            debugNav == "task" -> OverlayScreen.TASK_FORM
            debugSettings == "system" -> OverlayScreen.SETTINGS
            debugRecycleBin == "graveyard" -> OverlayScreen.RECYCLE_BIN
            else -> null
        }
        MomentMarkNavState(initialTab = initialTab, initialOverlay = initialOverlay)
    }
    // 默认首页事件（新安装首次启动注入；由 DataStore 一次性标记 + 空库守卫控制，
    // 注入后用户可自由增删改，不会在后续启动被清理或回填）。
    // DEBUG 视觉回归 fixture：`setprop debug.momentmark.seed samples` 时冻结时钟到
    // 2026-08-28，用于 design.html 第①屏截图。
    val debugSeed = if (BuildConfig.DEBUG) readDebugSeed() else ""
    val homeViewModel = remember(activity) {
        val seedFixture = debugSeed == "samples"
        ViewModelProvider(
            activity,
            MomentHomeViewModelFactory(
                context = context.applicationContext,
                seedMoments = SampleMoments.all,
                clockOverride = if (seedFixture) {
                    Clock.fixed(Instant.parse("2026-08-28T00:00:00Z"), ZoneId.of("Asia/Shanghai"))
                } else {
                    null
                },
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
    val debugDaybook = if (BuildConfig.DEBUG) readDebugDaybook() else ""
    val daybookViewModel = remember(activity) {
        ViewModelProvider(
            activity,
            DaybookViewModelFactory(
                context.applicationContext,
                fixtureToday = if (debugDaybook == "aug12") DaybookVisualFixture.today else null,
                fixtureTasks = if (debugDaybook == "aug12") DaybookVisualFixture.tasks else null,
            )
        )[DaybookViewModel::class.java]
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
    // 仅 DEBUG 视觉回归 fixture：
    // `setprop debug.momentmark.detail quest` → 直开第 ⑥ 屏任务详情（QUEST）
    // `setprop debug.momentmark.detail achievement` → 直开第 ⑦ 屏成就详情（ACHIEVEMENT）
    val debugDetail = if (BuildConfig.DEBUG) readDebugDetail() else ""
    // 仅 DEBUG 视觉回归 fixture：`setprop debug.momentmark.drawer filter` →
    // 首页直接展开第 ⑧ 屏「分类抽屉 · 搜索」冻结帧（design.html 第⑧屏数据）。
    val debugDrawer = if (BuildConfig.DEBUG) readDebugDrawer() else ""
    LaunchedEffect(debugDetail) {
        if (debugDetail == "quest" || debugDetail == "achievement") {
            val fixtureId = if (debugDetail == "achievement") {
                MomentDetailVisualFixture.achievementMomentId
            } else {
                MomentDetailVisualFixture.momentId
            }
            nav.openMomentDetail(fixtureId)
        }
    }

    BackHandler(enabled = nav.overlay != null) { nav.back() }

    // 首页抽屉的返回处理统一放在应用层（而非 MomentHomeScreen 内部）：
    // 1) 左缘右滑与系统返回手势竞争时，紧随其后的返回事件被宽限期吞掉，抽屉保持展开；
    // 2) 抽屉已展开 → 关闭抽屉；3) 否则执行默认返回（结束应用）。
    BackHandler(enabled = nav.overlay == null && nav.selectedTab == MainTab.HOME) {
        when {
            homeViewModel.consumeDrawerOpenBack() -> Unit
            homeViewModel.uiState.value.drawerOpen -> homeViewModel.closeDrawer()
            else -> activity.finish()
        }
    }

    // 当前设计稿所有页面均使用昼间任务板配色（DESIGN_SYSTEM §5）。
    MomentMarkTheme(themeMode = themeMode) {
        // design.html 的 .phone 内容区是 314×648 的固定画布；设备宽度不等于 314dp 时
        // 整页等比缩放，避免卡片外框随 weight(1f) 拉伸而字号/内距仍是固定 dp（DESIGN_SYSTEM §2.6）。
        PixelCanvas {
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
                            onBack = nav::back,
                            modifier = Modifier.fillMaxSize(),
                        )
                        OverlayScreen.MOMENT_DETAIL -> MomentDetailScreen(
                            momentId = nav.selectedMomentId ?: "",
                            viewModel = momentDetailViewModel,
                            onBack = nav::back,
                            onEdit = nav::openMomentEdit,
                            onSealed = { moment ->
                                undoSeal = UndoSeal(RecycleBinItemType.MOMENT, moment.id, moment.title)
                                nav.back()
                            },
                            modifier = Modifier.fillMaxSize(),
                            fixtureState = when (debugDetail) {
                                "quest" -> MomentDetailVisualFixture.uiState
                                "achievement" -> MomentDetailVisualFixture.achievementUiState
                                else -> null
                            },
                        )
                        OverlayScreen.TASK_FORM -> TaskFormScreen(
                            viewModel = taskFormViewModel,
                            taskId = nav.editingTaskId,
                            onSaved = { dueDate ->
                                daybookViewModel.selectDate(dueDate)
                                nav.back()
                            },
                            onBack = nav::back,
                            modifier = Modifier.fillMaxSize(),
                        )
                        OverlayScreen.SETTINGS -> SystemSettingsScreen(
                            recycleBinState = recycleBinUiState,
                            onBack = nav::back,
                            onOpenRecycleBin = nav::openRecycleBin,
                            themeMode = themeMode,
                            onThemeModeChanged = themeSettingsViewModel::setThemeMode,
                            selectedTab = nav.selectedTab,
                            onTabSelected = nav::selectTab,
                            onNewMoment = nav::openMomentForm,
                            modifier = Modifier.fillMaxSize(),
                        )
                        OverlayScreen.RECYCLE_BIN -> RecycleBinScreen(
                            uiState = recycleBinUiState,
                            clock = java.time.Clock.systemDefaultZone(),
                            onBack = nav::openSettings,
                            onRestore = recycleBinViewModel::restore,
                            onPermanentlyDelete = recycleBinViewModel::permanentlyDelete,
                            onPurgeAll = recycleBinViewModel::purgeAll,
                            selectedTab = nav.selectedTab,
                            onTabSelected = nav::selectTab,
                            onNewMoment = nav::openMomentForm,
                            fixtureState = if (debugRecycleBin == "graveyard") RecycleBinVisualFixture.uiState else null,
                            fixtureClock = if (debugRecycleBin == "graveyard") RecycleBinVisualFixture.clock else null,
                            modifier = Modifier.fillMaxSize(),
                        )
                        null -> when (destination.tab) {
                            MainTab.HOME -> MomentHomeScreen(
                                uiState = homeUiState,
                                onMomentSelected = nav::openMomentDetail,
                                onPinnedMove = homeViewModel::movePinned,
                                onDrawerOpen = homeViewModel::openDrawer,
                                onDrawerClose = homeViewModel::closeDrawer,
                                onOpenGroupManagement = nav::openSettings,
                                partyRows = buildMapFilterPartyRows(
                                    groups = groupUiState.groups,
                                    counts = homeUiState.filterCounts,
                                ),
                                drawerFixture = if (debugDrawer == "filter") {
                                    MomentHomeVisualFixture.mapFilter
                                } else {
                                    null
                                },
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
                    PixelUndoToast(
                        title = sealed.title,
                        onUndo = {
                            recycleBinViewModel.restore(sealed.type, sealed.id)
                            undoSeal = null
                        },
                        onDismiss = { undoSeal = null },
                    )
                }
                if (nav.overlay == null) {
                    PixelBottomNav(
                        selectedTab = nav.selectedTab,
                        onTabSelected = nav::selectTab,
                        onNewMoment = nav::openMomentForm,
                    )
                }
            }
        }
    }
}
/*
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
*/
