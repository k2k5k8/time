package com.cch.momentmark.ui.moment

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertHasClickAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cch.momentmark.domain.model.MomentDirection
import com.cch.momentmark.ui.theme.MomentMarkTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MomentFormScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun formExposesMomentOnlyFieldsAndDirectionChoice() {
        var selectedDirection = MomentDirection.FUTURE_COUNTDOWN
        var selectedGroup = ""
        var selectedRarity: Int? = null
        composeRule.setContent {
            var state by remember {
                mutableStateOf(MomentFormUiState(anchorDateText = "2026-08-28"))
            }
            MomentMarkTheme {
                MomentFormContent(
                    state = state,
                    onDirectionSelected = {
                        selectedDirection = it
                        state = state.copy(direction = it)
                    },
                    onTitleChanged = { state = state.copy(title = it) },
                    onNoteChanged = { state = state.copy(note = it) },
                    onGroupChanged = {
                        selectedGroup = it
                        state = state.copy(groupId = it)
                    },
                    onRaritySelected = {
                        selectedRarity = it
                        state = state.copy(rarity = it)
                    },
                    onDateChanged = { state = state.copy(anchorDateText = it) },
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithText("✦ 铭刻时刻").assertExists()
        composeRule.onNodeWithText("[x] 全天").assertExists()
        composeRule.onNodeWithContentDescription("年选择器，当前2026").assertExists()
        composeRule.onNodeWithContentDescription("月选择器，当前08").assertExists()
        composeRule.onNodeWithContentDescription("日选择器，当前28").assertExists()
        composeRule.onNodeWithContentDescription("时刻名称").performTextInput("毕业旅行")
        composeRule.onNodeWithContentDescription("时刻分组，可选").performTextInput("旅行")
        composeRule.onNodeWithContentDescription("2 星重要度").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("🏆 过去 · 正数")
            .performScrollTo()
            .assertHasClickAction()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(MomentDirection.PAST_ACHIEVEMENT, selectedDirection)
            assertEquals("旅行", selectedGroup)
            assertEquals(2, selectedRarity)
        }
    }
}
