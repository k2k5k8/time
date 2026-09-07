package com.cch.momentmark.ui.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue

/**
 * 底部导航的两个常驻页签（AGENTS.md §2.1 固定三入口）。
 * 「＋ 新时刻」不是页签，而是叠加在当前页签之上的全局表单入口。
 */
enum class MainTab {
    HOME,
    DAYBOOK,
}

/** 叠加在页签之上的推入页；系统返回键先关闭它，再考虑退出 App。 */
enum class OverlayScreen {
    MOMENT_FORM,
    MOMENT_DETAIL,
    TASK_FORM,
    SETTINGS,
    RECYCLE_BIN,
}

/**
 * J 三入口导航状态：
 * - 页签切换会关闭叠加表单（页签是根目的地）；
 * - 返回键优先关闭叠加表单，其次交还系统；
 * - 「＋ 新时刻」保持来源页签不变，用于导航高亮与返回落点。
 */
class MomentMarkNavState(initialTab: MainTab = MainTab.HOME) {

    var selectedTab: MainTab by mutableStateOf(initialTab)
        private set

    var overlay: OverlayScreen? by mutableStateOf(null)
        private set

    /** 详情页必须携带 Moment 身份；表单与页签切换时立即清除，避免回到旧详情。 */
    var selectedMomentId: String? by mutableStateOf(null)
        private set

    /** 编辑表单携带的 Moment 身份；新建表单始终为 null。 */
    var editingMomentId: String? by mutableStateOf(null)
        private set

    /** 编辑任务表单携带的 Task 身份；新建任务始终为 null。 */
    var editingTaskId: String? by mutableStateOf(null)
        private set

    /** 打开全局「＋ 新时刻」表单；不改变来源页签。 */
    fun openMomentForm() {
        selectedMomentId = null
        editingMomentId = null
        editingTaskId = null
        overlay = OverlayScreen.MOMENT_FORM
    }

    /** 从大事件卡进入统一详情页；详情仍属于当前首页根页签。 */
    fun openMomentDetail(momentId: String) {
        editingMomentId = null
        editingTaskId = null
        selectedMomentId = momentId
        overlay = OverlayScreen.MOMENT_DETAIL
    }

    /** 编辑结束后回到同一条时刻详情，而不是新建一条记录或落回旧页面。 */
    fun openMomentEdit(momentId: String) {
        selectedMomentId = null
        editingMomentId = momentId
        editingTaskId = null
        overlay = OverlayScreen.MOMENT_FORM
    }

    /** 新建回首页；编辑保存后回到刚编辑的详情（PRD §5.2）。 */
    fun completeMomentForm(savedMomentId: String, wasEditing: Boolean) {
        editingMomentId = null
        editingTaskId = null
        if (wasEditing) {
            selectedTab = MainTab.HOME
            selectedMomentId = savedMomentId
            overlay = OverlayScreen.MOMENT_DETAIL
        } else {
            selectedMomentId = null
            selectedTab = MainTab.HOME
            overlay = null
        }
    }

    /** 打开日子簿内的「＋ 接受新任务」表单。 */
    fun openTaskForm() {
        selectedMomentId = null
        editingMomentId = null
        editingTaskId = null
        overlay = OverlayScreen.TASK_FORM
    }

    /** 日子簿任务的编辑仍使用唯一的 Task 表单入口。 */
    fun openTaskEdit(taskId: String) {
        selectedMomentId = null
        editingMomentId = null
        editingTaskId = taskId
        overlay = OverlayScreen.TASK_FORM
    }

    fun openSettings() {
        selectedMomentId = null
        editingMomentId = null
        editingTaskId = null
        overlay = OverlayScreen.SETTINGS
    }

    fun openRecycleBin() {
        selectedMomentId = null
        editingMomentId = null
        editingTaskId = null
        overlay = OverlayScreen.RECYCLE_BIN
    }

    fun selectTab(tab: MainTab) {
        selectedMomentId = null
        editingMomentId = null
        editingTaskId = null
        overlay = null
        selectedTab = tab
    }

    /** 处理系统返回：有关叠加表单时关闭并返回 true，否则交还系统。 */
    fun back(): Boolean {
        if (overlay == null) return false
        selectedMomentId = null
        editingMomentId = null
        editingTaskId = null
        overlay = null
        return true
    }

    private fun restore(
        tab: MainTab,
        overlayScreen: OverlayScreen?,
        momentId: String?,
        editingId: String?,
        editingTaskId: String?,
    ) {
        selectedTab = tab
        overlay = overlayScreen
        selectedMomentId = momentId
        editingMomentId = editingId
        this.editingTaskId = editingTaskId
    }

    companion object {
        /** 保存格式：[页签名, 叠加页面名或空串, Moment 详情 ID, Moment 编辑 ID, Task 编辑 ID]。 */
        val Saver: Saver<MomentMarkNavState, Any> = listSaver(
            save = { state ->
                listOf(
                    state.selectedTab.name,
                    state.overlay?.name ?: "",
                    state.selectedMomentId ?: "",
                    state.editingMomentId ?: "",
                    state.editingTaskId ?: "",
                )
            },
            restore = { values ->
                MomentMarkNavState(MainTab.valueOf(values[0])).apply {
                    restore(
                        tab = selectedTab,
                        overlayScreen = values[1].takeIf { it.isNotEmpty() }
                            ?.let(OverlayScreen::valueOf),
                        momentId = values.getOrNull(2)?.takeIf { it.isNotEmpty() },
                        editingId = values.getOrNull(3)?.takeIf { it.isNotEmpty() },
                        editingTaskId = values.getOrNull(4)?.takeIf { it.isNotEmpty() },
                    )
                }
            },
        )
    }
}
