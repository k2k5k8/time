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
class NewQuestEntryTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun exposesDaybookOnlyNewTaskAction() {
        var clicked = false
        composeRule.setContent { MomentMarkTheme { NewQuestEntry(onClick = { clicked = true }) } }
        composeRule.onNodeWithContentDescription("接受新任务（待办）").assertHasClickAction().performClick()
        composeRule.runOnIdle { assertTrue(clicked) }
    }
}
