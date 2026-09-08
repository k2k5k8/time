package com.cch.momentmark.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cch.momentmark.ui.theme.MomentMarkTheme
import com.cch.momentmark.ui.theme.MomentMarkTokens
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PixelButtonTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun buttonExposesActionAndMinimumTouchHeight() {
        var clicks by mutableIntStateOf(0)
        composeRule.setContent {
            MomentMarkTheme {
                PixelButton(label = "保存", onClick = { clicks += 1 })
            }
        }

        composeRule.onNodeWithContentDescription("保存")
            .assertHasClickAction()
            .assertIsEnabled()
            .assertHeightIsAtLeast(MomentMarkTokens.TouchTargetMin)
            .performClick()

        composeRule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun disabledAndLoadingStatesBlockClicks() {
        var clicks by mutableIntStateOf(0)
        composeRule.setContent {
            MomentMarkTheme {
                Column {
                    PixelButton(
                        label = "不可用",
                        onClick = { clicks += 1 },
                        enabled = false,
                    )
                    PixelButton(
                        label = "同步",
                        onClick = { clicks += 1 },
                        loading = true,
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription("不可用")
            .assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("同步")
            .assertIsNotEnabled()
            .assertTextContains("处理中…")

        composeRule.runOnIdle { assertEquals(0, clicks) }
    }

    @Test
    fun allSemanticVariantsRenderAsButtons() {
        composeRule.setContent {
            MomentMarkTheme {
                Column {
                    PixelButtonVariant.entries.forEach { variant ->
                        PixelButton(
                            label = variant.name,
                            onClick = {},
                            variant = variant,
                        )
                    }
                }
            }
        }

        PixelButtonVariant.entries.forEach { variant ->
            composeRule.onNodeWithContentDescription(variant.name)
                .assertHasClickAction()
                .assertIsEnabled()
        }
    }
}
