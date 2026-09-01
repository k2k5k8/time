package com.cch.momentmark.ui.moment.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cch.momentmark.domain.model.HomeCardItem
import com.cch.momentmark.domain.model.HomeMomentProjection
import com.cch.momentmark.domain.model.MomentCardState
import com.cch.momentmark.domain.model.MomentDirection
import com.cch.momentmark.domain.model.TaskCardState
import com.cch.momentmark.domain.model.TaskType
import com.cch.momentmark.domain.time.EventTimeStatus
import com.cch.momentmark.ui.theme.MomentMarkTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MomentHomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun card(
        id: String,
        anchorDate: LocalDate,
        status: EventTimeStatus,
        days: Long,
        isPinned: Boolean = false,
    ) = MomentCardState(
        id = id,
        title = id,
        anchorDate = anchorDate,
        groupId = "生活",
        rarity = 2,
        isPinned = isPinned,
        pinnedOrder = if (isPinned) 0 else null,
        creationDirection = MomentDirection.PAST_ACHIEVEMENT,
        status = status,
        days = days,
        hpFraction = null,
        isLimited = false,
    )

    @Test
    fun pinnedPastAndFutureCardsExposeMergedChineseSemantics() {
        val uiState = MomentHomeUiState(
            isLoading = false,
            projection = HomeMomentProjection(
                pinned = listOf(
                    card("春节主线", LocalDate.of(2027, 2, 6), EventTimeStatus.FUTURE, 162, isPinned = true),
                ),
                past = listOf(
                    card("旧手机退役", LocalDate.of(2024, 3, 2), EventTimeStatus.PAST, 909),
                ),
                future = listOf(
                    card("考研初试", LocalDate.of(2026, 12, 19), EventTimeStatus.FUTURE, 113),
                ),
            ),
        )

        composeRule.setContent {
            MomentMarkTheme {
                MomentHomeScreen(uiState = uiState, onMomentSelected = {}, onPinnedMove = { _, _ -> })
            }
        }

        composeRule.onNodeWithContentDescription("春节主线，还有 162 天，2027.02.06，分组 生活")
            .assertExists()
        composeRule.onNodeWithContentDescription("旧手机退役，已经 909 天，2024.03.02，分组 生活")
            .assertExists()
        composeRule.onNodeWithContentDescription("考研初试，还有 113 天，2026.12.19，分组 生活")
            .assertExists()
    }

    @Test
    fun todayMomentShowsExactTodayWording() {
        val uiState = MomentHomeUiState(
            isLoading = false,
            projection = HomeMomentProjection(
                pinned = emptyList(),
                past = emptyList(),
                future = listOf(
                    card("答辩日", LocalDate.of(2026, 8, 28), EventTimeStatus.TODAY, 0),
                ),
            ),
        )

        composeRule.setContent {
            MomentMarkTheme {
                MomentHomeScreen(uiState = uiState, onMomentSelected = {}, onPinnedMove = { _, _ -> })
            }
        }

        composeRule.onNodeWithContentDescription("答辩日，就是今天，2026.08.28，分组 生活")
            .assertExists()
        composeRule.onNodeWithText("就是今天").assertExists()
    }

    @Test
    fun emptyStateGuidesToNewMomentEntry() {
        composeRule.setContent {
            MomentMarkTheme {
                MomentHomeScreen(
                    uiState = MomentHomeUiState(isLoading = false),
                    onMomentSelected = {},
                    onPinnedMove = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("还没有铭刻的时刻").assertExists()
    }

    @Test
    fun pinnedCardsExposeAdjacentMoveActions() {
        val uiState = MomentHomeUiState(
            isLoading = false,
            projection = HomeMomentProjection(
                pinned = listOf(
                    card("第一主线", LocalDate.of(2026, 12, 19), EventTimeStatus.FUTURE, 113, isPinned = true),
                    card("第二主线", LocalDate.of(2026, 12, 20), EventTimeStatus.FUTURE, 114, isPinned = true),
                ),
                past = emptyList(),
                future = emptyList(),
            ),
        )
        var move: Pair<String, Int>? = null

        composeRule.setContent {
            MomentMarkTheme {
                MomentHomeScreen(
                    uiState = uiState,
                    onMomentSelected = {},
                    onPinnedMove = { id, offset -> move = id to offset },
                )
            }
        }

        composeRule.onNodeWithContentDescription("上移 第二主线").performClick()

        org.junit.Assert.assertEquals("第二主线" to -1, move)
    }

    @Test
    fun optedInTaskUsesDistinctTodayCardSemanticsInRightColumn() {
        val taskCard = TaskCardState(
            id = "task-today",
            title = "提交项目周报",
            dueLocalDate = LocalDate.of(2026, 8, 28),
            dueInstant = null,
            taskType = TaskType.SIDE,
            difficulty = null,
            groupId = "工作",
            isCompleted = false,
            daysUntilDue = 0,
        )
        val uiState = MomentHomeUiState(
            isLoading = false,
            projection = HomeMomentProjection(
                pinned = emptyList(),
                past = emptyList(),
                future = emptyList(),
                rightItems = listOf(HomeCardItem.HomeTask(taskCard)),
            ),
        )

        composeRule.setContent {
            MomentMarkTheme {
                MomentHomeScreen(uiState = uiState, onMomentSelected = {}, onPinnedMove = { _, _ -> })
            }
        }

        composeRule.onNodeWithContentDescription("待办 提交项目周报，就是今天，2026.08.28，分组 工作")
            .assertExists()
        composeRule.onNodeWithText("就是今天").assertExists()
        composeRule.onNodeWithText("◇ 待办").assertExists()
    }
}
