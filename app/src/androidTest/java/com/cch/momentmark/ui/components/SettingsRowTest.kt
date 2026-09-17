package com.cch.momentmark.ui.components

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cch.momentmark.ui.theme.MomentMarkTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsRowTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun exposesDescriptionTrailingTextAndClick() {
        var clicked = false
        composeRule.setContent { MomentMarkTheme { SettingsRow("封印之地", "打开封印之地", { clicked = true }, "2 件 ›") } }
        composeRule.onNodeWithContentDescription("打开封印之地").assertHasClickAction().performClick()
        composeRule.runOnIdle { assertTrue(clicked) }
    }
}
