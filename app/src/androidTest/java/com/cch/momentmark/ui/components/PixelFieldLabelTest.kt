package com.cch.momentmark.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import com.cch.momentmark.ui.theme.MomentMarkTheme
import org.junit.Rule
import org.junit.Test

class PixelFieldLabelTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersCallerProvidedLabelAndHelper() {
        composeRule.setContent {
            MomentMarkTheme {
                PixelFieldLabel(
                    label = "▸ 任务描述 LORE（可选）",
                    helper = "最多 200 字",
                )
            }
        }

        composeRule.onNodeWithText("▸ 任务描述 LORE（可选）").fetchSemanticsNode()
        composeRule.onNodeWithText("最多 200 字").fetchSemanticsNode()
    }

    @Test
    fun omitsHelperWhenItIsNotProvided() {
        composeRule.setContent {
            MomentMarkTheme {
                PixelFieldLabel(label = "▸ 重要度 RARITY")
            }
        }

        composeRule.onNodeWithText("▸ 重要度 RARITY").fetchSemanticsNode()
        check(composeRule.onAllNodesWithText("最多 200 字").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun allTonesRenderWithoutChangingTextSemantics() {
        composeRule.setContent {
            MomentMarkTheme {
                PixelFieldLabel(label = "默认", tone = PixelFieldLabelTone.Default)
                PixelFieldLabel(label = "强调", tone = PixelFieldLabelTone.Accent)
                PixelFieldLabel(label = "错误", tone = PixelFieldLabelTone.Error)
            }
        }

        composeRule.onNodeWithText("默认").fetchSemanticsNode()
        composeRule.onNodeWithText("强调").fetchSemanticsNode()
        composeRule.onNodeWithText("错误").fetchSemanticsNode()
    }
}
