package com.cch.momentmark.ui.moment.detail

import com.cch.momentmark.data.repository.MomentRepositoryPort
import com.cch.momentmark.domain.model.Moment
import com.cch.momentmark.domain.model.MomentDirection
import com.cch.momentmark.domain.time.EventTimeStatus
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MomentDetailViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-08-28T02:00:00Z"), ZoneId.of("Asia/Shanghai"))
    private lateinit var repository: FakeMomentRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeMomentRepository(
            Moment(
                id = "future",
                title = "答辩",
                note = "准备材料",
                creationDirection = MomentDirection.FUTURE_COUNTDOWN,
                anchorDate = LocalDate.of(2026, 8, 30),
                groupId = "学业",
                rarity = 2,
                isPinned = false,
                pinnedOrder = null,
                deletedAt = null,
                createdAt = clock.millis(),
                updatedAt = clock.millis(),
            ),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `详情用固定时钟派生未来天数而不改变事实字段`() = runTest {
        val viewModel = MomentDetailViewModel(repository, clock)

        viewModel.load("future")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(EventTimeStatus.FUTURE, state.status)
        assertEquals(2L, state.days)
        assertEquals("答辩", state.moment?.title)
        assertFalse(state.isLoading)
    }

    @Test
    fun `置顶切换委托给仓储并重新加载详情`() = runTest {
        val viewModel = MomentDetailViewModel(repository, clock)
        viewModel.load("future")
        advanceUntilIdle()

        viewModel.togglePinned()
        advanceUntilIdle()

        assertEquals("future" to true, repository.pinnedRequest)
    }

    private class FakeMomentRepository(private val moment: Moment) : MomentRepositoryPort {
        var pinnedRequest: Pair<String, Boolean>? = null

        override fun observeActive(): Flow<List<Moment>> = emptyFlow()
        override suspend fun findById(id: String): Moment? = moment.takeIf { it.id == id }
        override suspend fun save(moment: Moment) = Unit
        override suspend fun seedIfEmpty(moments: List<Moment>) = Unit
        override suspend fun setPinned(id: String, pinned: Boolean) {
            pinnedRequest = id to pinned
        }
        override suspend fun reorderPinned(orderedIds: List<String>) = Unit
    }
}
