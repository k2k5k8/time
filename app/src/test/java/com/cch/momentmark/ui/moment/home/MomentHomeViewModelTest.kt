package com.cch.momentmark.ui.moment.home

import com.cch.momentmark.data.repository.MomentRepositoryPort
import com.cch.momentmark.data.repository.TaskRepositoryPort
import com.cch.momentmark.domain.model.Moment
import com.cch.momentmark.domain.model.MomentDirection
import com.cch.momentmark.domain.model.Task
import com.cch.momentmark.domain.model.TaskType
import com.cch.momentmark.domain.time.EventTimeStatus
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MomentHomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeMomentRepository
    private lateinit var taskRepository: FakeTaskRepository

    private val fixedClock = Clock.fixed(
        Instant.parse("2026-08-28T02:00:00Z"),
        ZoneId.of("Asia/Shanghai"),
    )

    @Before
    fun setUp() {
        repository = FakeMomentRepository()
        taskRepository = FakeTaskRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `启动空库时写入种子并投影三段布局`() = runTest {
        val seed = listOf(
            sampleMoment("pinned", LocalDate.of(2023, 5, 20), MomentDirection.PAST_ACHIEVEMENT, isPinned = true),
            sampleMoment("past", LocalDate.of(2026, 1, 10), MomentDirection.PAST_ACHIEVEMENT),
            sampleMoment("future", LocalDate.of(2026, 12, 19)),
        )

        val vm = MomentHomeViewModel(
            repository = repository,
            clock = fixedClock,
            seedMoments = seed,
        )
        advanceUntilIdle()

        assertEquals(1, repository.seedCalls)
        val state = vm.uiState.value
        assertEquals(listOf("pinned"), state.projection.pinned.map { it.id })
        assertEquals(listOf("past"), state.projection.past.map { it.id })
        assertEquals(listOf("future"), state.projection.future.map { it.id })
        assertTrue(!state.isEmpty)
    }

    @Test
    fun `空库无种子时展示空状态`() = runTest {
        val vm = MomentHomeViewModel(repository = repository, clock = fixedClock)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isEmpty)
    }

    @Test
    fun `目标日当天的未来时刻在右列显示就是今天`() = runTest {
        repository.moments.value = listOf(sampleMoment("today", LocalDate.of(2026, 8, 28)))

        val vm = MomentHomeViewModel(repository = repository, clock = fixedClock)
        advanceUntilIdle()

        val card = vm.uiState.value.projection.future.single()
        assertEquals(EventTimeStatus.TODAY, card.status)
        assertEquals(0L, card.days)
    }

    @Test
    fun `置顶调用透传到仓储`() = runTest {
        repository.moments.value = listOf(sampleMoment("m", LocalDate.of(2026, 12, 19)))

        val vm = MomentHomeViewModel(repository = repository, clock = fixedClock)
        advanceUntilIdle()

        vm.setPinned("m", true)
        advanceUntilIdle()

        assertEquals("m" to true, repository.pinnedRequest)
    }

    @Test
    fun `置顶相邻移动提交完整的新顺序`() = runTest {
        repository.moments.value = listOf(
            sampleMoment("first", LocalDate.of(2026, 12, 19), isPinned = true),
            sampleMoment("second", LocalDate.of(2026, 12, 20), isPinned = true),
        )
        val vm = MomentHomeViewModel(repository = repository, clock = fixedClock)
        advanceUntilIdle()

        vm.movePinned("second", -1)
        advanceUntilIdle()

        assertEquals(listOf("second", "first"), repository.reorderedIds)
    }

    @Test
    fun `开启首页显示的任务与未来时刻按日期投影到右列`() = runTest {
        repository.moments.value = listOf(sampleMoment("moment", LocalDate.of(2026, 8, 30)))
        taskRepository.tasks.value = listOf(
            sampleTask("today-task", LocalDate.of(2026, 8, 28)),
            sampleTask("later-task", LocalDate.of(2026, 9, 1)),
            sampleTask("hidden-task", LocalDate.of(2026, 8, 29), showOnHome = false),
            sampleTask("expired-task", LocalDate.of(2026, 8, 27)),
        )

        val vm = MomentHomeViewModel(
            repository = repository,
            taskRepository = taskRepository,
            clock = fixedClock,
        )
        advanceUntilIdle()

        assertEquals(
            listOf("today-task", "moment", "later-task"),
            vm.uiState.value.projection.rightItems.map { it.id },
        )
        assertTrue(!vm.uiState.value.isEmpty)
    }

    private fun sampleMoment(
        id: String,
        anchor: LocalDate,
        direction: MomentDirection = MomentDirection.FUTURE_COUNTDOWN,
        isPinned: Boolean = false,
    ): Moment = Moment(
        id = id,
        title = id,
        note = "",
        creationDirection = direction,
        anchorDate = anchor,
        groupId = null,
        rarity = null,
        isPinned = isPinned,
        pinnedOrder = if (isPinned) 0 else null,
        deletedAt = null,
        createdAt = anchor.minusDays(30).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli(),
        updatedAt = 0L,
    )

    private fun sampleTask(
        id: String,
        dueDate: LocalDate,
        showOnHome: Boolean = true,
    ) = Task(
        id = id,
        title = id,
        dueLocalDate = dueDate,
        dueInstant = null,
        zoneId = null,
        note = "",
        taskType = TaskType.SIDE,
        difficulty = null,
        groupId = null,
        isCompleted = false,
        completedAt = null,
        showOnHome = showOnHome,
        deletedAt = null,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    class MainDispatcherRule : TestWatcher() {
        private val dispatcher = UnconfinedTestDispatcher()

        override fun starting(description: Description) {
            Dispatchers.setMain(dispatcher)
        }
    }

    private class FakeMomentRepository : MomentRepositoryPort {
        val moments = MutableStateFlow<List<Moment>>(emptyList())
        var seedCalls = 0
        var pinnedRequest: Pair<String, Boolean>? = null
        var reorderedIds: List<String>? = null

        override fun observeActive(): Flow<List<Moment>> = moments

        override suspend fun findById(id: String): Moment? = moments.value.firstOrNull { it.id == id }

        override suspend fun save(moment: Moment) {
            moments.value = moments.value.filterNot { it.id == moment.id } + moment
        }

        override suspend fun seedIfEmpty(seeds: List<Moment>) {
            seedCalls++
            if (moments.value.isEmpty()) moments.value = seeds
        }

        override suspend fun setPinned(id: String, pinned: Boolean) {
            pinnedRequest = id to pinned
        }

        override suspend fun reorderPinned(orderedIds: List<String>) {
            reorderedIds = orderedIds
        }
    }

    private class FakeTaskRepository : TaskRepositoryPort {
        val tasks = MutableStateFlow<List<Task>>(emptyList())

        override fun observeActive(): Flow<List<Task>> = tasks
        override fun observeForDate(date: LocalDate): Flow<List<Task>> = tasks
        override suspend fun findById(id: String): Task? = tasks.value.firstOrNull { it.id == id }
        override suspend fun save(task: Task) = Unit
        override suspend fun setCompleted(id: String, completed: Boolean) = Unit
    }
}
