package com.cch.momentmark.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cch.momentmark.ui.app.MainTab
import com.cch.momentmark.ui.theme.MomentMarkTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PixelBottomNavTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun fixedThreeEntriesExistWithChineseDescriptions() {
        composeRule.setContent {
            MomentMarkTheme {
                PixelBottomNav(
                    selectedTab = MainTab.HOME,
                    onTabSelected = {},
                    onNewMoment = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("大事件").assertExists()
        composeRule.onNodeWithContentDescription("新时刻").assertExists()
        composeRule.onNodeWithContentDescription("日子簿").assertExists()
    }

    @Test
    fun selectedTabIsExposedAsSelectedSemantics() {
        composeRule.setContent {
            MomentMarkTheme {
                PixelBottomNav(
                    selectedTab = MainTab.HOME,
                    onTabSelected = {},
                    onNewMoment = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("大事件").assertIsSelected()
        composeRule.onNodeWithContentDescription("日子簿").assertIsNotSelected()
    }

    @Test
    fun tabClicksSwitchTabAndPlusOpensNewMoment() {
        var selected by mutableStateOf(MainTab.HOME)
        var newMomentRequested = false
        composeRule.setContent {
            MomentMarkTheme {
                PixelBottomNav(
                    selectedTab = selected,
                    onTabSelected = { selected = it },
                    onNewMoment = { newMomentRequested = true },
                )
            }
        }

        composeRule.onNodeWithContentDescription("日子簿").performClick()
        composeRule.runOnIdle { assertEquals(MainTab.DAYBOOK, selected) }

        composeRule.onNodeWithContentDescription("新时刻").performClick()
        composeRule.runOnIdle {
            assertEquals(MainTab.DAYBOOK, selected)
            assertTrue(newMomentRequested)
        }
    }
}
