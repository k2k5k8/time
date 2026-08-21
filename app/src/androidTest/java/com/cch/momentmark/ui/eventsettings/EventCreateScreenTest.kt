package com.cch.momentmark.ui.eventsettings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cch.momentmark.ui.theme.MomentMarkTheme
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventCreateScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun formShowsSharedFieldsAndRejectsEmptyTitle() {
        var saved = false
        composeRule.setContent {
            MomentMarkTheme(dynamicColor = false) {
                EventCreateFeature(
                    onBack = {},
                    onSave = { saved = true },
                )
            }
        }

        composeRule.onNodeWithText("新增日期").assertExists()
        composeRule.onNodeWithText("大标题").assertExists()
        composeRule.onNodeWithText("小标题").assertExists()
        composeRule.onNodeWithText("所属分组").assertExists()
        composeRule.onNodeWithText("开始时间 / 到达时间").assertExists()
        composeRule.onNodeWithText("进阶设置").assertExists()

        composeRule.onNodeWithContentDescription("保存事件").performClick()
        assertFalse(saved)
    }
}
