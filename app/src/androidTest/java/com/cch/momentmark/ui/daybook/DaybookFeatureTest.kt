package com.cch.momentmark.ui.daybook

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cch.momentmark.ui.theme.MomentMarkTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DaybookFeatureTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectedDateWithEventsKeepsAnExplicitCreateAction() {
        val today = LocalDate.now()
        var requestedDate: LocalDate? = null
        val createDescription = "为${today.monthValue}月${today.dayOfMonth}日新增日期记录"

        composeRule.setContent {
            MomentMarkTheme(dynamicColor = false) {
                DaybookFeature(
                    userEvents = emptyList(),
                    onOpenCreateEvent = { requestedDate = it },
                    onOpenBigEvents = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription(createDescription).performClick()

        assertEquals(today, requestedDate)
    }
}
