package com.cch.momentmark.ui.components

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cch.momentmark.ui.theme.MomentMarkTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GroupChipRailTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun exposesStableGroupSelectionSemantics() {
        val selected = mutableStateOf("travel")
        composeRule.setContent {
            MomentMarkTheme {
                GroupChipRail(
                    items = listOf(GroupChipItem("travel", "旅行"), GroupChipItem("work", "工作")),
                    selectedGroupId = selected.value,
                    onGroupSelected = { selected.value = it },
                )
            }
        }
        composeRule.onNodeWithContentDescription("分组 旅行").assertIsSelected().assertHasClickAction()
        composeRule.onNodeWithContentDescription("分组 工作").performClick()
        composeRule.runOnIdle { assertEquals("work", selected.value) }
    }

    @Test
    fun noneChipUsesBlankStableId() {
        val selected = mutableStateOf("travel")
        composeRule.setContent {
            MomentMarkTheme {
                GroupChipRail(listOf(GroupChipItem("travel", "旅行")), selected.value, { selected.value = it })
            }
        }
        composeRule.onNodeWithContentDescription("分组 无阵营").performClick()
        composeRule.runOnIdle { assertEquals("", selected.value) }
    }
}
