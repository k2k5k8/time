package com.cch.momentmark.ui.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MomentMarkNavStateTest {

    @Test
    fun `启动默认进入大事件页签且无叠加表单`() {
        val nav = MomentMarkNavState()

        assertEquals(MainTab.HOME, nav.selectedTab)
        assertNull(nav.overlay)
    }

    @Test
    fun `加号新时刻叠加在来源页签之上，返回键先关闭表单`() {
        val nav = MomentMarkNavState()
        nav.selectTab(MainTab.DAYBOOK)

        nav.openMomentForm()

        assertEquals(OverlayScreen.MOMENT_FORM, nav.overlay)
        assertEquals(MainTab.DAYBOOK, nav.selectedTab)
        assertTrue(nav.back())
        assertNull(nav.overlay)
        assertEquals(MainTab.DAYBOOK, nav.selectedTab)
    }

    @Test
    fun `无叠加表单时返回键交还系统`() {
        val nav = MomentMarkNavState()

        assertFalse(nav.back())
    }

    @Test
    fun `切换页签会关闭叠加表单`() {
        val nav = MomentMarkNavState()
        nav.openMomentForm()

        nav.selectTab(MainTab.DAYBOOK)

        assertNull(nav.overlay)
        assertEquals(MainTab.DAYBOOK, nav.selectedTab)
    }

    @Test
    fun `日子簿接受新任务返回后仍停留在日子簿`() {
        val nav = MomentMarkNavState()
        nav.selectTab(MainTab.DAYBOOK)

        nav.openTaskForm()
        assertTrue(nav.back())

        assertEquals(MainTab.DAYBOOK, nav.selectedTab)
        assertNull(nav.overlay)
    }

    @Test
    fun `日子簿任务编辑携带身份并在返回时清除`() {
        val nav = MomentMarkNavState()
        nav.selectTab(MainTab.DAYBOOK)

        nav.openTaskEdit("task-42")

        assertEquals(OverlayScreen.TASK_FORM, nav.overlay)
        assertEquals("task-42", nav.editingTaskId)
        assertTrue(nav.back())
        assertNull(nav.editingTaskId)
        assertEquals(MainTab.DAYBOOK, nav.selectedTab)
    }

    @Test
    fun `大事件卡详情保存身份并在返回时清除`() {
        val nav = MomentMarkNavState()

        nav.openMomentDetail("moment-42")

        assertEquals(OverlayScreen.MOMENT_DETAIL, nav.overlay)
        assertEquals("moment-42", nav.selectedMomentId)
        assertTrue(nav.back())
        assertNull(nav.overlay)
        assertNull(nav.selectedMomentId)
    }

    @Test
    fun `编辑时刻保存后返回同一详情，新建保存后回首页`() {
        val nav = MomentMarkNavState()
        nav.openMomentDetail("moment-42")
        nav.openMomentEdit("moment-42")

        assertEquals(OverlayScreen.MOMENT_FORM, nav.overlay)
        assertEquals("moment-42", nav.editingMomentId)

        nav.completeMomentForm("moment-42", wasEditing = true)
        assertEquals(OverlayScreen.MOMENT_DETAIL, nav.overlay)
        assertEquals("moment-42", nav.selectedMomentId)
        assertNull(nav.editingMomentId)

        nav.openMomentForm()
        nav.completeMomentForm("new-moment", wasEditing = false)
        assertEquals(MainTab.HOME, nav.selectedTab)
        assertNull(nav.overlay)
    }

    @Test
    fun `Saver 保存并恢复页签与叠加表单`() {
        // 保存格式见 MomentMarkNavState.Saver 注释：[页签名, 叠加页面名或空串, Moment ID 或空串]
        val restored = MomentMarkNavState.Saver.restore(listOf("DAYBOOK", "TASK_FORM"))

        assertEquals(MainTab.DAYBOOK, restored?.selectedTab)
        assertSame(OverlayScreen.TASK_FORM, restored?.overlay)
    }

    @Test
    fun `Saver 恢复无叠加表单的状态`() {
        val restored = MomentMarkNavState.Saver.restore(listOf("HOME", ""))

        assertEquals(MainTab.HOME, restored?.selectedTab)
        assertNull(restored?.overlay)
    }

    @Test
    fun `Saver 恢复时刻详情与对应身份`() {
        val restored = MomentMarkNavState.Saver.restore(listOf("HOME", "MOMENT_DETAIL", "moment-42"))

        assertEquals(OverlayScreen.MOMENT_DETAIL, restored?.overlay)
        assertEquals("moment-42", restored?.selectedMomentId)
    }

    @Test
    fun `Saver 恢复编辑中的时刻表单`() {
        val restored = MomentMarkNavState.Saver.restore(listOf("HOME", "MOMENT_FORM", "", "moment-42"))

        assertEquals(OverlayScreen.MOMENT_FORM, restored?.overlay)
        assertEquals("moment-42", restored?.editingMomentId)
    }

    @Test
    fun `Saver 恢复编辑中的任务表单`() {
        val restored = MomentMarkNavState.Saver.restore(listOf("DAYBOOK", "TASK_FORM", "", "", "task-42"))

        assertEquals(OverlayScreen.TASK_FORM, restored?.overlay)
        assertEquals("task-42", restored?.editingTaskId)
    }
}
