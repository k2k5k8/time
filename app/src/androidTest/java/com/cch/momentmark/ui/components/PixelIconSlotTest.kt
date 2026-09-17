package com.cch.momentmark.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.cch.momentmark.ui.theme.MomentMarkTheme
import org.junit.Rule
import org.junit.Test

class PixelIconSlotTest {
    @get:Rule val composeRule = createComposeRule()
    @Test fun exposesMeaningfulIconWhenNotDecorative() {
        composeRule.setContent { MomentMarkTheme { PixelIconSlot("travel", decorative = false) } }
        composeRule.onNodeWithContentDescription("分组图标 travel").fetchSemanticsNode()
    }
}
