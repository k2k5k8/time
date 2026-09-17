package com.cch.momentmark.ui.components

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cch.momentmark.ui.theme.MomentMarkTheme
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PixelStarRatingTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun interactiveRatingExposesRadioSemanticsAndTogglesSelection() {
        val selected = mutableStateOf<Int?>(null)
        composeRule.setContent {
            MomentMarkTheme {
                PixelStarRating(
                    value = selected.value,
                    onValueChange = { selected.value = it },
                    contentDescription = { "$it 星重要度" },
                )
            }
        }

        composeRule.onNodeWithContentDescription("2 星重要度")
            .assertHasClickAction()
            .performClick()
        composeRule.runOnIdle { assertEquals(2, selected.value) }
        composeRule.onNodeWithContentDescription("2 星重要度").assertIsSelected()
            .performClick()
        composeRule.runOnIdle { assertNull(selected.value) }
    }

    @Test
    fun readOnlyRatingDoesNotExposeClickAction() {
        composeRule.setContent {
            MomentMarkTheme { PixelStarRating(value = 2, onValueChange = null) }
        }
        val node = composeRule.onNodeWithContentDescription("2 星").fetchSemanticsNode()
        check(!node.config.contains(SemanticsActions.OnClick)) { "read-only rating must not be clickable" }
    }
}
