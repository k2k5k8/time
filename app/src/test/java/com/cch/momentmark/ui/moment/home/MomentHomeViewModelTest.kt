package com.cch.momentmark.ui.moment.home

import com.cch.momentmark.data.repository.Group
import com.cch.momentmark.data.repository.MomentRepositoryPort
import com.cch.momentmark.data.repository.TaskRepositoryPort
import com.cch.momentmark.data.settings.MomentMarkSettingsStorePort
import com.cch.momentmark.domain.model.Moment
import com.cch.momentmark.domain.model.MomentDirection
import com.cch.momentmark.domain.model.Task
import com.cch.momentmark.domain.model.TaskType
import com.cch.momentmark.domain.time.EventTimeStatus
import com.cch.momentmark.ui.theme.ThemeMode
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
import org.junit.Assert.assertFalse
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
    private lateinit var settings: FakeSettingsStore

    private val fixedClock = Clock.fixed(
        Instant.parse("2026-08-28T02:00:00Z"),
        ZoneId.of("Asia/Shanghai"),
    )

    @Before
    fun setUp() {
        repository = FakeMomentRepository()
        taskRepository = FakeTaskRepository()
        settings = FakeSettingsStore(seeded = false)
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
            settingsStore = settings,
        )
        advanceUntilIdle()

        assertEquals(1, repository.seedCalls)
        assertEquals(1, repository.removeSampleCalls)
        val state = vm.uiState.value
        assertEquals(listOf("pinned"), state.projection.pinned.map { it.id })
        assertEquals(listOf("past"), state.projection.past.map { it.id })
        assertEquals(listOf("future"), state.projection.future.map { it.id })
        assertTrue(!state.isEmpty)
    }

    @Test
    fun `首次启动先清理历史示例且空种子不注入`() = runTest {
        repository.moments.value = listOf(sampleMoment("sample-old", LocalDate.of(2026, 1, 10), MomentDirection.PAST_ACHIEVEMENT))

        val vm = MomentHomeViewModel(
            repository = repository,
            clock = fixedClock,
            seedMoments = emptyList(),
            settingsStore = settings,
        )
        advanceUntilIdle()

        // 首次启动（一次性标记未置位）先清理历史 sample- 行；空种子不写任何内容，首页投影为空。
        assertEquals(1, repository.removeSampleCalls)
        assertEquals(emptyList<Moment>(), repository.moments.value)
        assertTrue(vm.uiState.value.isEmpty)
        assertTrue(settings.seeded)
    }

    @Test
    fun `已注入过默认事件后不再清理也不再回填`() = runTest {
        repository.moments.value = listOf(
            sampleMoment("mine", LocalDate.of(2026, 1, 10), MomentDirection.PAST_ACHIEVEMENT),
        )
        val vm = MomentHomeViewModel(
            repository = repository,
            clock = fixedClock,
            seedMoments = listOf(sampleMoment("default", LocalDate.of(2026, 5, 4), MomentDirection.PAST_ACHIEVEMENT)),
            settingsStore = FakeSettingsStore(seeded = true),
        )
        advanceUntilIdle()

        // 一次性标记已置位：不清理、不注入，用户数据原样保留（删除后默认事件也不会复活）。
        assertEquals(0, repository.removeSampleCalls)
        assertEquals(0, repository.seedCalls)
        assertEquals(listOf("mine"), repository.moments.value.map { it.id })
    }

    @Test
    fun `空库无种子时展示空状态`() = runTest {
        val vm = MomentHomeViewModel(repository = repository, clock = fixedClock, settingsStore = settings)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isEmpty)
    }

    @Test
    fun `目标日当天的未来时刻在右列显示就是今天`() = runTest {
        repository.moments.value = listOf(sampleMoment("today", LocalDate.of(2026, 8, 28)))

        val vm = MomentHomeViewModel(repository = repository, clock = fixedClock, settingsStore = settings)
        advanceUntilIdle()

        val card = vm.uiState.value.projection.future.single()
        assertEquals(EventTimeStatus.TODAY, card.status)
        assertEquals(0L, card.days)
    }

    @Test
    fun `置顶调用透传到仓储`() = runTest {
        repository.moments.value = listOf(sampleMoment("m", LocalDate.of(2026, 12, 19)))

        val vm = MomentHomeViewModel(repository = repository, clock = fixedClock, settingsStore = settings)
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
        val vm = MomentHomeViewModel(repository = repository, clock = fixedClock, settingsStore = settings)
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
            settingsStore = settings,
        )
        advanceUntilIdle()

        assertEquals(
            listOf("today-task", "moment", "later-task"),
            vm.uiState.value.projection.rightItems.map { it.id },
        )
        assertTrue(!vm.uiState.value.isEmpty)
    }

    @Test
    fun `抽屉开关状态在打开与关闭间切换`() = runTest {
        val vm = MomentHomeViewModel(
            repository = repository,
            clock = fixedClock,
            settingsStore = settings,
            uptimeMillis = { 1_000_000L },
        )
        advanceUntilIdle()

        vm.openDrawer()
        assertTrue(vm.uiState.value.drawerOpen)
        vm.closeDrawer()
        assertTrue(!vm.uiState.value.drawerOpen)
    }

    @Test
    fun `抽屉呼出后的竞争返回只在宽限期内被消耗一次`() {
        val vm = MomentHomeViewModel(
            repository = repository,
            clock = fixedClock,
            settingsStore = settings,
            uptimeMillis = { 1_000_000L },
        )
        val t0 = 1_000_000L

        // 未呼出过抽屉：任何时刻都不消耗。
        assertFalse(vm.consumeDrawerOpenBack(nowMillis = t0))

        vm.openDrawer()
        // 宽限期内：返回被消耗（吞掉），且只消耗一次。
        assertTrue(vm.consumeDrawerOpenBack(nowMillis = t0 + 1_000L))
        assertFalse(vm.consumeDrawerOpenBack(nowMillis = t0 + 1_000L))

        // 重新呼出：新宽限期重新生效（临界点 2500ms 内仍算宽限）。
        vm.openDrawer()
        assertTrue(vm.consumeDrawerOpenBack(nowMillis = t0 + 2_500L))

        // 超出宽限期后不再消耗。
        vm.openDrawer()
        assertFalse(vm.consumeDrawerOpenBack(nowMillis = t0 + 2_501L))
    }

    @Test
    fun `抽屉按分组名聚合活动计数并给出总数`() = runTest {
        repository.moments.value = listOf(
            sampleMoment("a", LocalDate.of(2026, 12, 19)).withGroup("学业"),
            sampleMoment("b", LocalDate.of(2026, 9, 20)).withGroup("学业"),
            sampleMoment("c", LocalDate.of(2026, 9, 20)).withGroup("生活"),
            sampleMoment("d", LocalDate.of(2023, 5, 20), MomentDirection.PAST_ACHIEVEMENT),
        )

        val vm = MomentHomeViewModel(repository = repository, clock = fixedClock, settingsStore = settings)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(mapOf("学业" to 2, "生活" to 1, "" to 1), state.filterCounts)
        assertEquals(4, state.filterTotal)
    }

    @Test
    fun `分组行按 Room 排序输出并追加未被管理的组名`() {
        val groups = listOf(
            Group(id = "g2", name = "生活", colorToken = "hpRed", sortOrder = 1, iconKey = "heart"),
            Group(id = "g1", name = "学业", colorToken = "manaPurple", sortOrder = 0, iconKey = "study"),
        )
        val counts = mapOf("学业" to 3, "生活" to 2, "旅行" to 1, "" to 4)

        val rows = buildMapFilterPartyRows(groups, counts)

        assertEquals(listOf("学业", "生活", "旅行"), rows.map { it.name })
        assertEquals(listOf(3, 2, 1), rows.map { it.count })
        assertEquals(listOf("study", "heart", "travel"), rows.map { it.iconKey })
    }

    private fun Moment.withGroup(name: String?): Moment = copy(groupId = name)

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
        var removeSampleCalls = 0
        var pinnedRequest: Pair<String, Boolean>? = null
        var reorderedIds: List<String>? = null

        override fun observeActive(): Flow<List<Moment>> = moments

        override suspend fun findById(id: String): Moment? = moments.value.firstOrNull { it.id == id }

        override suspend fun removeSampleRows() {
            removeSampleCalls++
            moments.value = moments.value.filterNot { it.id.startsWith("sample-") }
        }

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

    private class FakeSettingsStore(seeded: Boolean) : MomentMarkSettingsStorePort {
        var seeded = seeded
            private set
        private val seededFlow = MutableStateFlow(seeded)

        override val themeMode = MutableStateFlow(ThemeMode.SYSTEM)
        override val defaultMomentsSeeded: Flow<Boolean> = seededFlow

        override suspend fun setThemeMode(mode: ThemeMode) = Unit
        override suspend fun markDefaultMomentsSeeded() {
            seeded = true
            seededFlow.value = true
        }
    }
}
