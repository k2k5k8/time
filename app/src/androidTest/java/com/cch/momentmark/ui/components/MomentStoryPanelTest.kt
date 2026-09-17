package com.cch.momentmark.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cch.momentmark.ui.theme.MomentMarkTheme
import org.junit.Rule
import org.junit.Test

class MomentStoryPanelTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun rendersStoryOnlyWhenNoteIsPresent() {
        composeRule.setContent { MomentMarkTheme { MomentStoryPanel("答辩准备完成", true) } }
        composeRule.onNodeWithText("▸ 成就故事 STORY").fetchSemanticsNode()
        composeRule.onNodeWithText("答辩准备完成").fetchSemanticsNode()
    }
}
