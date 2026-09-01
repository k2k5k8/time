package com.cch.momentmark.ui.recyclebin

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cch.momentmark.domain.model.RecycleBinItem
import com.cch.momentmark.domain.model.RecycleBinItemType
import com.cch.momentmark.ui.theme.MomentMarkTheme
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecycleBinScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private val item = RecycleBinItem(
        type = RecycleBinItemType.TASK,
        id = "task-1",
        title = "提交项目周报",
        deletedAt = Instant.parse("2026-08-20T00:00:00Z"),
    )

    @Test
    fun sealedItemExposesReviveAndDoubleConfirmedPurification() {
        var restored: RecycleBinItem? = null
        var permanentlyDeleted: RecycleBinItem? = null
        composeRule.setContent {
            MomentMarkTheme {
                RecycleBinScreen(
                    uiState = RecycleBinUiState(items = listOf(item), isLoading = false),
                    clock = Clock.fixed(Instant.parse("2026-08-29T00:00:00Z"), ZoneOffset.UTC),
                    onBack = {},
                    onRestore = { restored = it },
                    onPermanentlyDelete = { permanentlyDeleted = it },
                    onPurgeAll = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("复活 提交项目周报").performClick()
        composeRule.runOnIdle { assertEquals(item, restored) }
        composeRule.onNodeWithContentDescription("永久净化 提交项目周报").performClick()
        composeRule.onNodeWithText("确认净化").performClick()
        composeRule.runOnIdle { assertEquals(item, permanentlyDeleted) }
    }
}
