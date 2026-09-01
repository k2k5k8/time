package com.cch.momentmark.ui.system

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cch.momentmark.ui.recyclebin.RecycleBinUiState
import com.cch.momentmark.ui.theme.MomentMarkTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SystemSettingsScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun groupManagementIsReachableAndCreateButtonHasBehavior() {
        var created = ""
        composeRule.setContent {
            MomentMarkTheme {
                SystemSettingsScreen(
                    recycleBinState = RecycleBinUiState(),
                    onBack = {}, onOpenRecycleBin = {}, onAutoPurgeChanged = {},
                    onCreateGroup = { created = it },
                )
            }
        }
        composeRule.onNodeWithContentDescription("队伍编成管理，当前 0 组").assertExists()
        composeRule.onNodeWithContentDescription("新建分组名称").performTextInput("生活")
        composeRule.onNodeWithText("＋ 新建").performClick()
        assertEquals("生活", created)
    }
}
