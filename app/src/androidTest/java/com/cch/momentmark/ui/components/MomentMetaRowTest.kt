package com.cch.momentmark.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cch.momentmark.ui.theme.MomentMarkTheme
import org.junit.Rule
import org.junit.Test

class MomentMetaRowTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun rendersDateAndOptionalGroupWithoutChangingCallerText() {
        composeRule.setContent { MomentMarkTheme { MomentMetaRow("DUE", "2026.09.09", "旅行") } }
        composeRule.onNodeWithText("DUE 2026.09.09 · 旅行").fetchSemanticsNode()
    }
}
