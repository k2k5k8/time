package com.cch.momentmark.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cch.momentmark.domain.model.EventCardPaletteKey
import com.cch.momentmark.domain.model.EventColorRole
import com.cch.momentmark.domain.model.EventTimeType
import com.cch.momentmark.domain.model.TimeEvent
import com.cch.momentmark.ui.theme.MomentMarkTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun primaryActionsAndEventCardRemainDiscoverableAtLargeFontScale() {
        val event = TimeEvent(
            id = "accessibility-event",
            title = "可访问性检查",
            timeType = EventTimeType.ALL_DAY,
            dateLabel = "2026.09.01",
            relativeLabel = "还有",
            icon = "●",
            colorRole = EventColorRole.FUTURE,
            cardPaletteKey = EventCardPaletteKey.BLUE_WHITE,
            localDate = LocalDate.of(2026, 9, 1),
        )

        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides androidx.compose.ui.unit.Density(
                    density = 1f,
                    fontScale = 2f,
                ),
            ) {
                MomentMarkTheme(dynamicColor = false) {
                    HomeScreen(
                        events = listOf(event),
                        selectedFilter = EventFilter.ALL,
                        selectedGroup = null,
                        templateOverrides = emptyMap(),
                        travelConfigOverrides = emptyMap(),
                        onOpenGroups = {},
                        onOpenSettings = {},
                        onOpenCreateEvent = {},
                        onOpenDaybook = {},
                        onOpenEventSettings = {},
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription("搜索").assertExists()
        composeRule.onNodeWithContentDescription("设置").assertExists()
        composeRule.onNodeWithContentDescription("新建事件").assertExists()
        composeRule.onNodeWithContentDescription("大事件，当前主页入口").assertExists()
        composeRule.onNodeWithContentDescription("日子簿，打开日子簿页面").assertExists()
        composeRule
            .onNodeWithContentDescription("可访问性检查，还有，2026.09.01")
            .assertExists()

        composeRule.onNodeWithContentDescription("日子簿，打开日子簿页面").performClick()
        composeRule.onNodeWithContentDescription("大事件，当前主页入口").performClick()

        composeRule.onNodeWithContentDescription("搜索").performClick()
        composeRule.onNodeWithContentDescription("关闭搜索").assertExists()
    }
}
