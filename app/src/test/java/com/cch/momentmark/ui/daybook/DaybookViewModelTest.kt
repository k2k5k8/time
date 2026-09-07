package com.cch.momentmark.ui.daybook

import com.cch.momentmark.data.repository.TaskRepositoryPort
import com.cch.momentmark.domain.model.Task
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class DaybookViewModelTest {
    @get:Rule val mainDispatcherRule = DaybookMainDispatcherRule()
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun selectedDateQueriesOnlyItsTasksAndDoesNotMigratePastTasks() = runTest {
        val today = LocalDate.of(2026, 8, 29)
        val yesterday = today.minusDays(1)
        val repository = FakeDaybookRepository(
            mapOf(
                yesterday to listOf(task("yesterday", yesterday)),
                today to listOf(task("today", today)),
            ),
        )
        val viewModel = DaybookViewModel(
            repository = repository,
            clock = Clock.fixed(Instant.parse("2026-08-29T00:00:00Z"), ZoneOffset.UTC),
        )
        advanceUntilIdle()

        assertEquals(listOf("today"), viewModel.uiState.value.tasks.map(Task::id))
        viewModel.selectDate(yesterday)
        advanceUntilIdle()
        assertEquals(listOf("yesterday"), viewModel.uiState.value.tasks.map(Task::id))
        assertEquals(yesterday, repository.lastObservedDate)
    }

    @Test
    fun completionToggleDelegatesDurableStateChange() = runTest {
        val date = LocalDate.of(2026, 8, 29)
        val repository = FakeDaybookRepository(mapOf(date to listOf(task("quest", date))))
        val viewModel = DaybookViewModel(
            repository,
            Clock.fixed(Instant.parse("2026-08-29T00:00:00Z"), ZoneOffset.UTC),
        )
        advanceUntilIdle()

        viewModel.toggleCompleted(requireNotNull(viewModel.uiState.value.tasks.singleOrNull()))
        advanceUntilIdle()

        assertEquals("quest" to true, repository.lastCompletion)
        assertFalse(viewModel.uiState.value.tasks.single().isCompleted)
    }

    @Test
    fun sealingTaskDelegatesSoftDeleteThenReportsTheOriginalFactsForUndo() = runTest {
        val date = LocalDate.of(2026, 8, 29)
        val original = task("quest", date)
        val repository = FakeDaybookRepository(mapOf(date to listOf(original)))
        val viewModel = DaybookViewModel(
            repository,
            Clock.fixed(Instant.parse("2026-08-29T00:00:00Z"), ZoneOffset.UTC),
        )
        advanceUntilIdle()
        var callbackTask: Task? = null

        viewModel.seal(original) { callbackTask = it }
        advanceUntilIdle()

        assertEquals("quest", repository.lastSoftDeletedId)
        assertEquals(original, callbackTask)
    }
}

private class FakeDaybookRepository(initial: Map<LocalDate, List<Task>>) : TaskRepositoryPort {
    private val records = initial.mapValues { MutableStateFlow(it.value) }.toMutableMap()
    var lastObservedDate: LocalDate? = null
    var lastCompletion: Pair<String, Boolean>? = null
    var lastSoftDeletedId: String? = null
    override fun observeForDate(date: LocalDate): Flow<List<Task>> {
        lastObservedDate = date
        return records.getOrPut(date) { MutableStateFlow(emptyList()) }
    }
    override suspend fun findById(id: String): Task? = records.values.flatMap { it.value }.firstOrNull { it.id == id }
    override suspend fun save(task: Task) = Unit
    override suspend fun setCompleted(id: String, completed: Boolean) { lastCompletion = id to completed }
    override suspend fun softDelete(id: String) { lastSoftDeletedId = id }
}

private fun task(id: String, date: LocalDate) = Task(
    id = id,
    title = id,
    dueLocalDate = date,
    dueInstant = null,
    zoneId = null,
    note = "",
    taskType = null,
    difficulty = null,
    groupId = null,
    isCompleted = false,
    completedAt = null,
    showOnHome = false,
    deletedAt = null,
    createdAt = Instant.EPOCH,
    updatedAt = Instant.EPOCH,
)

@OptIn(ExperimentalCoroutinesApi::class)
class DaybookMainDispatcherRule : TestWatcher() {
    private val dispatcher = UnconfinedTestDispatcher()
    override fun starting(description: Description) { Dispatchers.setMain(dispatcher) }
}
