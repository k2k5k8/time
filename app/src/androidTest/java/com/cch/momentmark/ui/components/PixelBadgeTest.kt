package com.cch.momentmark.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cch.momentmark.ui.theme.MomentMarkTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PixelBadgeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun badgeExposesLabelAndRendersCustomTone() {
        composeRule.setContent {
            TestTheme {
                PixelBadge(label = "◆ 主线", variant = PixelBadgeVariant.Main)
            }
        }

        composeRule.onNodeWithContentDescription("◆ 主线")
            .assertContentDescriptionEquals("◆ 主线")
            .assertTextContains("◆ 主线")
    }

    @Test
    fun allSemanticVariantsRenderTheirLabels() {
        composeRule.setContent {
            TestTheme {
                Column {
                    PixelBadge(label = "主线", variant = PixelBadgeVariant.Main)
                    PixelBadge(label = "支线", variant = PixelBadgeVariant.Side)
                    PixelBadge(label = "限时", variant = PixelBadgeVariant.Limited)
                    PixelBadge(label = "成就", variant = PixelBadgeVariant.Achievement)
                }
            }
        }

        listOf("主线", "支线", "限时", "成就").forEach { label ->
            composeRule.onNodeWithContentDescription(label).assertTextContains(label)
        }
    }
}

@Composable
private fun TestTheme(content: @Composable () -> Unit) {
    MomentMarkTheme(content = content)
}
