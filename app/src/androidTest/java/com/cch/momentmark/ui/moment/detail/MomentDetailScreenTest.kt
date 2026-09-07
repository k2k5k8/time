package com.cch.momentmark.ui.moment.detail

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cch.momentmark.domain.model.Moment
import com.cch.momentmark.domain.model.MomentDirection
import com.cch.momentmark.domain.time.EventTimeStatus
import com.cch.momentmark.ui.theme.MomentMarkTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MomentDetailScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun editActionCarriesTheDisplayedMomentIdentity() {
        val moment = Moment(
            id = "moment-42",
            title = "答辩",
            note = "准备材料",
            creationDirection = MomentDirection.FUTURE_COUNTDOWN,
            anchorDate = LocalDate.of(2026, 8, 30),
            groupId = null,
            rarity = null,
            isPinned = false,
            pinnedOrder = null,
            deletedAt = null,
            createdAt = 0L,
            updatedAt = 0L,
        )
        var editedId: String? = null

        composeRule.setContent {
            MomentMarkTheme {
                MomentDetailContent(
                    state = MomentDetailUiState(
                        isLoading = false,
                        moment = moment,
                        status = EventTimeStatus.FUTURE,
                        days = 2,
                    ),
                    onBack = {},
                    onEdit = { editedId = it },
                    onTogglePinned = {},
                    onSeal = {},
                    onSealed = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("编辑 答辩").performClick()

        assertEquals("moment-42", editedId)
    }
}
