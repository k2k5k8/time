package com.cch.momentmark.ui.moment.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
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

    @Test
    fun filterDrawerRendersChipsPartyRowsAndManageEntry() {
        val uiState = MomentHomeUiState(
            isLoading = false,
            drawerOpen = true,
            filterTotal = 6,
            projection = HomeMomentProjection(
                pinned = listOf(card("春节主线", LocalDate.of(2027, 2, 6), EventTimeStatus.FUTURE, 162, isPinned = true)),
                past = emptyList(),
                future = emptyList(),
            ),
        )
        val partyRows = listOf(
            MapFilterPartyRow("学业", "学业", "study", 3),
            MapFilterPartyRow("工作", "工作", "work", 0),
        )

        composeRule.setContent {
            MomentMarkTheme {
                MomentHomeScreen(
                    uiState = uiState,
                    onMomentSelected = {},
                    onPinnedMove = { _, _ -> },
                    partyRows = partyRows,
                )
            }
        }

        composeRule.onNodeWithText("MAP FILTER").assertExists()
        composeRule.onNodeWithText("▸ 地图筛选 FILTER").assertExists()
        composeRule.onNodeWithText("全部 ×6").assertExists()
        composeRule.onNodeWithText("倒数中").assertExists()
        composeRule.onNodeWithText("正数中").assertExists()
        composeRule.onNodeWithText("🎓 学业").assertExists()
        composeRule.onNodeWithText("×3").assertExists()
        composeRule.onNodeWithText("💼 工作").assertExists()
        composeRule.onNodeWithText("⚙ 管理分组 →").assertExists()
        composeRule.onNodeWithContentDescription("地图筛选").assertExists()
    }

    @Test
    fun filterDrawerScrimAndEscCloseDrawer() {
        val uiState = MomentHomeUiState(isLoading = false, drawerOpen = true)
        var closeCalls = 0

        composeRule.setContent {
            MomentMarkTheme {
                MomentHomeScreen(
                    uiState = uiState,
                    onMomentSelected = {},
                    onPinnedMove = { _, _ -> },
                    onDrawerClose = { closeCalls++ },
                )
            }
        }

        composeRule.onNodeWithContentDescription("关闭地图筛选").performClick()
        composeRule.onNodeWithContentDescription("退出筛选").performClick()
        composeRule.waitForIdle()

        org.junit.Assert.assertEquals(2, closeCalls)
    }

    @Test
    fun filterDrawerManageEntryInvokesCallback() {
        val uiState = MomentHomeUiState(isLoading = false, drawerOpen = true)
        var openSettings = false

        composeRule.setContent {
            MomentMarkTheme {
                MomentHomeScreen(
                    uiState = uiState,
                    onMomentSelected = {},
                    onPinnedMove = { _, _ -> },
                    onOpenGroupManagement = { openSettings = true },
                )
            }
        }

        composeRule.onNodeWithContentDescription("管理分组").performClick()

        org.junit.Assert.assertTrue(openSettings)
    }

    @Test
    fun leftEdgeSwipeOpensDrawer() {
        val uiState = MomentHomeUiState(isLoading = false)
        var opened = false

        composeRule.setContent {
            MomentMarkTheme {
                MomentHomeScreen(
                    uiState = uiState,
                    onMomentSelected = {},
                    onPinnedMove = { _, _ -> },
                    onDrawerOpen = { opened = true },
                )
            }
        }

        composeRule.onRoot()
            .performTouchInput { swipeRight(startX = 5f, endX = width * 0.8f, durationMillis = 400) }
        composeRule.waitForIdle()

        org.junit.Assert.assertTrue(opened)
    }

    @Test
    fun continuousMiddleSwipeOpensDrawerWithoutEdgeStart() {
        // 从屏幕中部一气呵成右滑；不需要先在左缘停顿，也不需要等抬手后才打开。
        val uiState = MomentHomeUiState(isLoading = false)
        var opened = false

        composeRule.setContent {
            MomentMarkTheme {
                MomentHomeScreen(
                    uiState = uiState,
                    onMomentSelected = {},
                    onPinnedMove = { _, _ -> },
                    onDrawerOpen = { opened = true },
                )
            }
        }

        composeRule.onRoot().performTouchInput {
            swipe(
                start = Offset(width * 0.5f, height / 2f),
                end = Offset(width * 0.9f, height / 2f),
                durationMillis = 100,
            )
        }
        composeRule.waitForIdle()

        org.junit.Assert.assertTrue(opened)
    }

    @Test
    fun shortMiddleSwipeKeepsDrawerClosed() {
        // 中部轻微横移仍按点按处理，不能误开抽屉。
        val uiState = MomentHomeUiState(isLoading = false)
        var opened = false

        composeRule.setContent {
            MomentMarkTheme {
                MomentHomeScreen(
                    uiState = uiState,
                    onMomentSelected = {},
                    onPinnedMove = { _, _ -> },
                    onDrawerOpen = { opened = true },
                )
            }
        }

        composeRule.onRoot().performTouchInput {
            swipe(
                start = Offset(width * 0.5f, height / 2f),
                end = Offset(width * 0.5f + 16.dp.toPx(), height / 2f),
                durationMillis = 100,
            )
        }
        composeRule.waitForIdle()

        org.junit.Assert.assertFalse(opened)
    }

    @Test
    fun verticalSwipeDoesNotOpenDrawer() {
        // 上下滚动优先，避免任意位置的抽屉手势劫持首页浏览。
        val uiState = MomentHomeUiState(isLoading = false)
        var opened = false

        composeRule.setContent {
            MomentMarkTheme {
                MomentHomeScreen(
                    uiState = uiState,
                    onMomentSelected = {},
                    onPinnedMove = { _, _ -> },
                    onDrawerOpen = { opened = true },
                )
            }
        }

        composeRule.onRoot().performTouchInput {
            swipe(
                start = Offset(width * 0.5f, height * 0.35f),
                end = Offset(width * 0.5f, height * 0.75f),
                durationMillis = 100,
            )
        }
        composeRule.waitForIdle()

        org.junit.Assert.assertFalse(opened)
    }

    @Test
    fun filterDrawerFixtureRendersDesignFrame() {
        composeRule.setContent {
            MomentMarkTheme {
                MomentHomeScreen(
                    uiState = MomentHomeUiState(isLoading = false),
                    onMomentSelected = {},
                    onPinnedMove = { _, _ -> },
                    drawerFixture = MomentHomeVisualFixture.mapFilter,
                )
            }
        }

        composeRule.onNodeWithText("考研初试").assertExists()
        composeRule.onNodeWithText("96 DAYS · DUE 12.19").assertExists()
        composeRule.onNodeWithText("演唱会").assertExists()
        composeRule.onNodeWithText("32 DAYS · DUE 09.20").assertExists()
        composeRule.onNodeWithText("全部 ×6").assertExists()
        composeRule.onNodeWithText("💞 生活").assertExists()
        composeRule.onNodeWithText("搜索任务与成就…").assertExists()
    }
}
