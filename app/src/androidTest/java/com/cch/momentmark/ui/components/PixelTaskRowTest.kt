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
class PixelTaskRowTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun rowExposesCompletionAndEditActions() {
        var completed = false
        var edited = false
        composeRule.setContent {
            MomentMarkTheme {
                PixelTaskRow(TaskRowPresentation("提交周报", false, "TODO", "提交周报 TODO"), { completed = true }, { edited = true }, {})
            }
        }
        composeRule.onNodeWithContentDescription("提交周报 未完成；复选框").assertHasClickAction().performClick()
        composeRule.onNodeWithContentDescription("提交周报，提交周报 TODO，点击编辑，长按放弃").performClick()
        composeRule.runOnIdle { assertTrue(completed && edited) }
    }
}
