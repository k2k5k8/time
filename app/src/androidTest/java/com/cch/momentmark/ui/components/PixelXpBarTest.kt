package com.cch.momentmark.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.cch.momentmark.ui.theme.MomentMarkTheme
import org.junit.Rule
import org.junit.Test

class PixelXpBarTest {
    @get:Rule val composeRule = createComposeRule()
    @Test fun exposesFilledCellDescription() {
        composeRule.setContent { MomentMarkTheme { PixelXpBar(5, contentDescription = "经验 5/16") } }
        composeRule.onNodeWithContentDescription("经验 5/16").fetchSemanticsNode()
    }
}
