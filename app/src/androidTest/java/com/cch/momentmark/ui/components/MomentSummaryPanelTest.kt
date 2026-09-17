package com.cch.momentmark.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cch.momentmark.ui.theme.MomentMarkTheme
import org.junit.Rule
import org.junit.Test

class MomentSummaryPanelTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun rendersQuestSummaryPresentation() {
        composeRule.setContent { MomentMarkTheme { MomentSummaryPanel(MomentSummaryPresentation("论文答辩", "12 天后开启", "TARGET 2026.09.21", false)) } }
        composeRule.onNodeWithText("论文答辩").fetchSemanticsNode()
        composeRule.onNodeWithText("12 天后开启").fetchSemanticsNode()
        composeRule.onNodeWithText("TARGET 2026.09.21").fetchSemanticsNode()
    }
}
