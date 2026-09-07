package com.cch.momentmark.ui.moment

import com.cch.momentmark.data.repository.MomentRepositoryPort
import com.cch.momentmark.domain.model.Moment
import com.cch.momentmark.domain.model.MomentDirection
import java.time.Clock
import java.time.Instant
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MomentFormViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-08-28T02:00:00Z"), ZoneId.of("Asia/Shanghai"))
    private lateinit var repository: FakeMomentRepository

    @Before fun setUp() { repository = FakeMomentRepository() }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `未来方向拒绝过去日期`() {
        val vm = viewModel()
        vm.updateTitle("毕业旅行")
        vm.updateAnchorDate("2026-08-27")

        vm.submit()

        assertEquals("未来·倒数只能选择今天或未来日期。", vm.uiState.value.errorMessage)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `过去方向拒绝未来日期`() {
        val vm = viewModel()
        vm.selectDirection(MomentDirection.PAST_ACHIEVEMENT)
        vm.updateTitle("入学日")
        vm.updateAnchorDate("2026-08-29")

        vm.submit()

        assertEquals("过去·正数只能选择今天或过去日期。", vm.uiState.value.errorMessage)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `有效未来时刻保存事实字段并发出完成事件`() = runTest {
        val vm = viewModel()
        vm.updateTitle("  毕业旅行  ")
        vm.updateNote("  东京见  ")
        vm.updateAnchorDate("2027-03-28")

        vm.submit()
        advanceUntilIdle()

        val saved = repository.saved.single()
        assertEquals("fixed-id", saved.id)
        assertEquals("毕业旅行", saved.title)
        assertEquals("东京见", saved.note)
        assertEquals(MomentDirection.FUTURE_COUNTDOWN, saved.creationDirection)
        assertEquals("2027-03-28", saved.anchorDate.toString())
        assertEquals(clock.millis(), saved.createdAt)
        assertNull(saved.pinnedOrder)
        assertEquals("fixed-id", vm.uiState.value.savedMomentId)
    }

    @Test
    fun `编辑会预填并保留同一身份和非表单事实字段`() = runTest {
        val original = Moment(
            id = "existing-id",
            title = "旧标题",
            note = "旧故事",
            creationDirection = MomentDirection.PAST_ACHIEVEMENT,
            anchorDate = java.time.LocalDate.of(2025, 8, 28),
            groupId = "生活",
            rarity = 3,
            isPinned = true,
            pinnedOrder = 1,
            deletedAt = null,
            createdAt = 123L,
            updatedAt = 456L,
        )
        repository.saved += original
        val vm = viewModel()

        vm.start(original.id)
        advanceUntilIdle()
        vm.updateTitle("新标题")
        vm.updateAnchorDate("2025-08-27")
        vm.updateGroup("  纪念日  ")
        vm.selectRarity(2)
        vm.submit()
        advanceUntilIdle()

        val updated = repository.saved.single()
        assertEquals(original.id, updated.id)
        assertEquals("新标题", updated.title)
        assertEquals("纪念日", updated.groupId)
        assertEquals(2, updated.rarity)
        assertEquals(original.pinnedOrder, updated.pinnedOrder)
        assertEquals(original.createdAt, updated.createdAt)
        assertTrue(vm.uiState.value.savedWasEditing)
    }

    @Test
    fun `年 月 日独立选择后合并为单一自然日并自动修正月底`() {
        val vm = viewModel()
        vm.selectYear(2024)
        vm.selectMonth(2)
        vm.selectDay(29)

        assertEquals("2024-02-29", vm.uiState.value.anchorDateText)
        assertEquals(2024, vm.uiState.value.selectedYear)
        assertEquals(2, vm.uiState.value.selectedMonth)
        assertEquals(29, vm.uiState.value.selectedDay)

        vm.selectYear(2023)
        assertEquals("2023-02-28", vm.uiState.value.anchorDateText)
    }

    private fun viewModel() = MomentFormViewModel(repository, clock, idGenerator = { "fixed-id" })

    private class FakeMomentRepository : MomentRepositoryPort {
        val saved = mutableListOf<Moment>()
        override fun observeActive(): Flow<List<Moment>> = MutableStateFlow(emptyList())
        override suspend fun findById(id: String): Moment? = saved.firstOrNull { it.id == id }
        override suspend fun save(moment: Moment) {
            saved.removeAll { it.id == moment.id }
            saved += moment
        }
        override suspend fun seedIfEmpty(moments: List<Moment>) = Unit
        override suspend fun setPinned(id: String, pinned: Boolean) = Unit
        override suspend fun reorderPinned(orderedIds: List<String>) = Unit
    }

    class MainDispatcherRule : TestWatcher() {
        private val dispatcher = UnconfinedTestDispatcher()
        override fun starting(description: Description) { Dispatchers.setMain(dispatcher) }
    }
}
