package com.cch.momentmark.ui.system

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cch.momentmark.ui.app.MainTab
import com.cch.momentmark.ui.recyclebin.RecycleBinUiState
import com.cch.momentmark.ui.theme.MomentMarkTheme
import com.cch.momentmark.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SystemSettingsScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun settingsScreenShowsAllSections() {
        composeRule.setContent {
            MomentMarkTheme {
                SystemSettingsScreen(
                    recycleBinState = RecycleBinUiState(),
                    onBack = {}, onOpenRecycleBin = {},
                )
            }
        }
        composeRule.onNodeWithText("系统设置").assertExists()
        composeRule.onNodeWithText("冒险者 · cch").assertExists()
        composeRule.onNodeWithText("LV.07 · 连续打卡 12 天 🔥").assertExists()
        composeRule.onNodeWithText("EXP 61% → LV.08").assertExists()
        composeRule.onNodeWithText("▸ 显示模式 DISPLAY").assertExists()
        composeRule.onNodeWithText("昼间地图").assertExists()
        composeRule.onNodeWithText("洞窟").assertExists()
        composeRule.onNodeWithText("跟随系统 ✓").assertExists()
        composeRule.onNodeWithText("🔊 战斗音效").assertExists()
        composeRule.onNodeWithText("📳 命中震动").assertExists()
        composeRule.onNodeWithText("🗑 封印之地（回收站）").assertExists()
        composeRule.onNodeWithText("ℹ 关于刻间").assertExists()
        composeRule.onNodeWithText("MOMENT MARK · MADE WITH ♥ · SAVE FILE LOCAL").assertExists()
    }

    @Test
    fun recycleBinCountIsDisplayed() {
        val state = RecycleBinUiState(
            items = listOf(
                com.cch.momentmark.domain.model.RecycleBinItem(
                    type = com.cch.momentmark.domain.model.RecycleBinItemType.MOMENT,
                    id = "1", title = "test",
                    deletedAt = java.time.Instant.now(),
                ),
            ),
            isLoading = false,
        )
        composeRule.setContent {
            MomentMarkTheme {
                SystemSettingsScreen(
                    recycleBinState = state,
                    onBack = {}, onOpenRecycleBin = {},
                )
            }
        }
        composeRule.onNodeWithText("1 件 ›").assertExists()
    }

    @Test
    fun displayModeButtonsCallCallback() {
        var selected: ThemeMode? = null
        composeRule.setContent {
            MomentMarkTheme {
                SystemSettingsScreen(
                    recycleBinState = RecycleBinUiState(),
                    onBack = {}, onOpenRecycleBin = {},
                    themeMode = ThemeMode.SYSTEM,
                    onThemeModeChanged = { selected = it },
                )
            }
        }
        composeRule.onNodeWithText("昼间地图").performClick()
        assertEquals(ThemeMode.LIGHT, selected)
        composeRule.onNodeWithText("洞窟").performClick()
        assertEquals(ThemeMode.DARK, selected)
        composeRule.onNodeWithText("跟随系统 ✓").performClick()
        assertEquals(ThemeMode.SYSTEM, selected)
    }

    @Test
    fun bottomNavShowsDaybookSelected() {
        var selectedTab: MainTab? = null
        composeRule.setContent {
            MomentMarkTheme {
                SystemSettingsScreen(
                    recycleBinState = RecycleBinUiState(),
                    onBack = {}, onOpenRecycleBin = {},
                    selectedTab = MainTab.DAYBOOK,
                    onTabSelected = { selectedTab = it },
                )
            }
        }
        composeRule.onNodeWithContentDescription("大事件").performClick()
        assertEquals(MainTab.HOME, selectedTab)
    }
}
