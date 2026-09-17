package com.cch.momentmark.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cch.momentmark.ui.theme.MomentMarkTheme
import org.junit.Rule
import org.junit.Test

class MilestonePanelTest {
    @get:Rule val composeRule = createComposeRule()
    @Test fun rendersReadonlyMilestoneStates() {
        composeRule.setContent { MomentMarkTheme { MilestonePanel(listOf(MilestonePresentation("1200 天", "下一项", "还有 4 天", .99f, true))) } }
        composeRule.onNodeWithText("1200 天").fetchSemanticsNode(); composeRule.onNodeWithText("还有 4 天").fetchSemanticsNode()
    }
}
