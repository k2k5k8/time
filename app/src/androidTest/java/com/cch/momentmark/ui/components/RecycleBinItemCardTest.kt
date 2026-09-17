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
class RecycleBinItemCardTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun exposesRestoreAndPermanentPurgeActions() {
        var restored = false; var purged = false
        composeRule.setContent { MomentMarkTheme { RecycleBinItemCard(RecycleBinItemPresentation("论文", "时刻", 12), { restored = true }, { purged = true }) } }
        composeRule.onNodeWithContentDescription("复活 论文").assertHasClickAction().performClick()
        composeRule.onNodeWithContentDescription("永久净化 论文").assertHasClickAction().performClick()
        composeRule.runOnIdle { assertTrue(restored && purged) }
    }
}
