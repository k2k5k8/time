package com.cch.momentmark.ui.components

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cch.momentmark.ui.theme.MomentMarkTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PixelDateSelectorTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun rendersThreeAccessibleDatePartsAndAllDayFact() {
        composeRule.setContent {
            MomentMarkTheme {
                PixelDateSelector("▸ 目标日期 TARGET DATE ★", "2026-09-09", 2026, 9, 9, {}, {}, {})
            }
        }
        composeRule.onNodeWithText("▸ 目标日期 TARGET DATE ★").fetchSemanticsNode()
        composeRule.onNodeWithText("[x] 全天").fetchSemanticsNode()
        composeRule.onNodeWithContentDescription("年选择器，当前2026").assertHasClickAction()
        composeRule.onNodeWithContentDescription("月选择器，当前09").assertHasClickAction()
        composeRule.onNodeWithContentDescription("日选择器，当前09").assertHasClickAction()
    }

    @Test
    fun selectingYearUsesCallerCallbackAndPreservesDateText() {
        val selectedYear = mutableStateOf(2026)
        composeRule.setContent {
            MomentMarkTheme {
                PixelDateSelector("日期", "2026-09-09", selectedYear.value, 9, 9, { selectedYear.value = it }, {}, {}, yearRange = 2026..2027)
            }
        }
        composeRule.onNodeWithContentDescription("年选择器，当前2026").performClick()
        composeRule.onNodeWithText("2027").performClick()
        composeRule.runOnIdle { assertEquals(2027, selectedYear.value) }
        composeRule.onNodeWithText("已选择 2026-09-09").fetchSemanticsNode()
    }
}
