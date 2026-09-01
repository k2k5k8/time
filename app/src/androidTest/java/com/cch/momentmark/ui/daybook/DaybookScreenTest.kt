package com.cch.momentmark.ui.daybook

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cch.momentmark.domain.model.Task
import com.cch.momentmark.domain.model.TaskType
import com.cch.momentmark.ui.theme.MomentMarkTheme
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DaybookScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun taskRowOpensTheSharedEditTaskFormWithItsIdentity() {
        val date = LocalDate.of(2026, 8, 29)
        val task = Task(
            id = "task-42",
            title = "提交项目周报",
            dueLocalDate = date,
            dueInstant = null,
            zoneId = null,
            note = "",
            taskType = TaskType.SIDE,
            difficulty = null,
            groupId = null,
            isCompleted = false,
            completedAt = null,
            showOnHome = false,
            deletedAt = null,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
        )
        var editingId: String? = null

        composeRule.setContent {
            MomentMarkTheme {
                DaybookScreen(
                    uiState = DaybookUiState(
                        selectedDate = date,
                        today = date,
                        tasks = listOf(task),
                        taskDates = setOf(date),
                        isLoading = false,
                    ),
                    onAcceptNewTask = {},
                    onOpenSettings = {},
                    onDateSelected = {},
                    onMonthChanged = {},
                    onTaskCompletionToggled = {},
                    onTaskEditRequested = { editingId = it },
                    onTaskSealRequested = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("$date 已选择，有任务").assertExists()
        composeRule.onNodeWithContentDescription("编辑 提交项目周报").performClick()

        assertEquals("task-42", editingId)
    }

    @Test
    fun completedTaskUsesClearExpStatus() {
        val date = LocalDate.of(2026, 8, 29)
        val task = Task("done", "完成测试", date, null, null, "", TaskType.SIDE, null, null, true, Instant.EPOCH, false, null, Instant.EPOCH, Instant.EPOCH)
        composeRule.setContent {
            MomentMarkTheme {
                DaybookScreen(
                    uiState = DaybookUiState(selectedDate = date, today = date, tasks = listOf(task), taskDates = setOf(date), isLoading = false),
                    onAcceptNewTask = {}, onOpenSettings = {}, onDateSelected = {}, onMonthChanged = {},
                    onTaskCompletionToggled = {}, onTaskEditRequested = {}, onTaskSealRequested = {},
                )
            }
        }
        composeRule.onNodeWithText("CLEAR +5 EXP").assertExists()
    }
}
