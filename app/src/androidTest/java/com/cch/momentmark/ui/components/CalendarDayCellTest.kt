package com.cch.momentmark.ui.components

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cch.momentmark.ui.theme.MomentMarkTheme
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarDayCellTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun selectedTaskDayExposesChineseStateAndClick() {
        var clicked = false
        composeRule.setContent {
            MomentMarkTheme {
                CalendarDayCell(LocalDate.of(2026, 9, 9), true, true, false, true, { clicked = true })
            }
        }
        composeRule.onNodeWithContentDescription("2026-09-09，已选择，有任务")
            .assertHasClickAction().performClick()
        composeRule.runOnIdle { assertTrue(clicked) }
    }

    @Test
    fun outsideMonthDayIsNotClickable() {
        composeRule.setContent {
            MomentMarkTheme {
                CalendarDayCell(LocalDate.of(2026, 8, 30), false, false, false, false, {})
            }
        }
        val node = composeRule.onNodeWithContentDescription("2026-08-30").fetchSemanticsNode()
        check(!node.config.contains(androidx.compose.ui.semantics.SemanticsActions.OnClick))
    }
}
