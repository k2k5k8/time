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
class PixelUndoToastTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun exposesUndoAndDismissActions() {
        var undone = false; var dismissed = false
        composeRule.setContent { MomentMarkTheme { PixelUndoToast("论文", { undone = true }, { dismissed = true }) } }
        composeRule.onNodeWithContentDescription("撤销封印 论文").assertHasClickAction().performClick()
        composeRule.onNodeWithContentDescription("关闭撤销提示").assertHasClickAction().performClick()
        composeRule.runOnIdle { assertTrue(undone && dismissed) }
    }
}
