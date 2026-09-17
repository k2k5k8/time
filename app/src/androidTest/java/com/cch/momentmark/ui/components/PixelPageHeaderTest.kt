package com.cch.momentmark.ui.components

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cch.momentmark.ui.theme.MomentMarkTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PixelPageHeaderTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersTitleHudAndOptionalBackAction() {
        var backClicks by mutableIntStateOf(0)
        composeRule.setContent {
            MomentMarkTheme {
                PixelPageHeader(
                    title = "时刻详情",
                    hud = "QUEST DETAIL",
                    onBack = { backClicks += 1 },
                    backContentDescription = "返回大事件",
                )
            }
        }

        composeRule.onNodeWithText("时刻详情").fetchSemanticsNode()
        composeRule.onNodeWithText("QUEST DETAIL").fetchSemanticsNode()
        composeRule.onNodeWithContentDescription("返回大事件")
            .assertHasClickAction()
            .performClick()
        composeRule.runOnIdle { assertEquals(1, backClicks) }
    }

    @Test
    fun omitsBackActionWhenNotProvided() {
        composeRule.setContent {
            MomentMarkTheme {
                PixelPageHeader(title = "日子簿", hud = "DUNGEON")
            }
        }

        composeRule.onNodeWithText("日子簿").fetchSemanticsNode()
        check(composeRule.onAllNodesWithContentDescription("返回").fetchSemanticsNodes().isEmpty())
    }
}
