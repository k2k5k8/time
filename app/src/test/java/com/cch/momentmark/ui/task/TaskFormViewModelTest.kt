package com.cch.momentmark.ui.task

import com.cch.momentmark.data.repository.TaskRepositoryPort
import com.cch.momentmark.domain.model.Task
import com.cch.momentmark.domain.model.TaskType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class TaskFormViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-08-29T08:00:00Z"), ZoneOffset.UTC)

    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun defaultTaskUsesTodayAndSavesAsIndependentTask() = runTest {
        val repository = RecordingTaskRepository()
        val viewModel = TaskFormViewModel(repository, clock, idGenerator = { "quest-1" })

        assertEquals("2026-08-29", viewModel.uiState.value.dueDateText)
        assertEquals("2026-08-29", viewModel.uiState.value.quickTodayDateText)
        assertEquals("2026-08-30", viewModel.uiState.value.quickTomorrowDateText)
        assertEquals("2026-08-29", viewModel.uiState.value.quickSaturdayDateText)
        assertFalse(viewModel.uiState.value.showOnHome)
        viewModel.updateTitle("提交项目周报")
        viewModel.updateTime("09:30")
        viewModel.selectType(TaskType.MAIN)
        viewModel.submit()
        advanceUntilIdle()

        val saved = requireNotNull(repository.saved)
        assertEquals("quest-1", saved.id)
        assertEquals(LocalDate.of(2026, 8, 29), saved.dueLocalDate)
        assertEquals(Instant.parse("2026-08-29T09:30:00Z"), saved.dueInstant)
        assertEquals(TaskType.MAIN, saved.taskType)
        assertFalse(saved.isCompleted)
        assertFalse(saved.showOnHome)
        assertEquals(LocalDate.of(2026, 8, 29), viewModel.uiState.value.savedDueDate)
    }

    @Test
    fun invalidTimeDoesNotWriteTask() = runTest {
        val repository = RecordingTaskRepository()
        val viewModel = TaskFormViewModel(repository, clock)
        viewModel.updateTitle("错误时间")
        viewModel.updateTime("9:3")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals("截止时间格式为 HH:MM。", viewModel.uiState.value.errorMessage)
        assertTrue(repository.saved == null)
    }

    @Test
    fun editPreFillsAndPreservesCompletionAndLifecycleFacts() = runTest {
        val repository = RecordingTaskRepository()
        val completedAt = Instant.parse("2026-08-28T10:00:00Z")
        repository.saved = Task(
            id = "quest-existing",
            title = "旧任务",
            dueLocalDate = LocalDate.of(2026, 8, 28),
            dueInstant = null,
            zoneId = null,
            note = "旧备注",
            taskType = null,
            difficulty = 3,
            groupId = "工作",
            isCompleted = true,
            completedAt = completedAt,
            showOnHome = false,
            deletedAt = null,
            createdAt = Instant.ofEpochMilli(123L),
            updatedAt = Instant.ofEpochMilli(456L),
        )
        val viewModel = TaskFormViewModel(repository, clock, idGenerator = { "must-not-create" })

        viewModel.start("quest-existing")
        advanceUntilIdle()
        assertEquals("旧任务", viewModel.uiState.value.title)
        assertEquals(null, viewModel.uiState.value.taskType)

        viewModel.updateTitle("更新后的任务")
        viewModel.updateDate("2026-08-29")
        viewModel.toggleShowOnHome()
        viewModel.selectDifficulty(2)
        viewModel.submit()
        advanceUntilIdle()

        val saved = requireNotNull(repository.saved)
        assertEquals("quest-existing", saved.id)
        assertEquals("更新后的任务", saved.title)
        assertEquals(LocalDate.of(2026, 8, 29), saved.dueLocalDate)
        assertEquals(2, saved.difficulty)
        assertTrue(saved.isCompleted)
        assertEquals(completedAt, saved.completedAt)
        assertEquals(Instant.ofEpochMilli(123L), saved.createdAt)
        assertTrue(saved.showOnHome)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    private val dispatcher = UnconfinedTestDispatcher()
    override fun starting(description: Description) { Dispatchers.setMain(dispatcher) }
}

private class RecordingTaskRepository : TaskRepositoryPort {
    var saved: Task? = null
    override fun observeForDate(date: LocalDate): Flow<List<Task>> = emptyFlow()
    override suspend fun findById(id: String): Task? = saved?.takeIf { it.id == id }
    override suspend fun save(task: Task) { saved = task }
    override suspend fun setCompleted(id: String, completed: Boolean) = Unit
}
