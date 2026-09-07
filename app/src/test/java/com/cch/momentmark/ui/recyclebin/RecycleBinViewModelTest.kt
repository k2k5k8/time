package com.cch.momentmark.ui.recyclebin

import com.cch.momentmark.data.repository.MomentRepositoryPort
import com.cch.momentmark.data.repository.TaskRepositoryPort
import com.cch.momentmark.data.settings.MomentMarkSettingsStorePort
import com.cch.momentmark.domain.model.Moment
import com.cch.momentmark.domain.model.MomentDirection
import com.cch.momentmark.domain.model.RecycleBinItemType
import com.cch.momentmark.domain.model.Task
import com.cch.momentmark.domain.model.toRecycleBinItem
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecycleBinViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-08-29T00:00:00Z"), ZoneOffset.UTC)

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `启动时仅在自动净化开启后清理满三十天的封存物`() = runTest {
        val moments = FakeMoments(listOf(moment("old", "旧时刻", "2026-07-30T00:00:00Z")))
        val tasks = FakeTasks(listOf(task("recent", "新任务", "2026-08-01T00:00:00Z")))
        RecycleBinViewModel(moments, tasks, Settings(enabled = true), clock)
        advanceUntilIdle()

        assertEquals(Instant.parse("2026-07-30T00:00:00Z").toEpochMilli(), moments.purgeCutoff)
        assertEquals(Instant.parse("2026-07-30T00:00:00Z").toEpochMilli(), tasks.purgeCutoff)
    }

    @Test
    fun `复活保留实体类型并委托正确仓储`() = runTest {
        val moments = FakeMoments(listOf(moment("m", "时刻", "2026-08-10T00:00:00Z")))
        val tasks = FakeTasks(listOf(task("t", "任务", "2026-08-10T00:00:00Z")))
        val vm = RecycleBinViewModel(moments, tasks, Settings(enabled = false), clock)
        advanceUntilIdle()

        vm.restore(RecycleBinItemType.TASK, "t")
        advanceUntilIdle()

        assertEquals(listOf("t"), tasks.restored)
        assertEquals(emptyList<String>(), moments.restored)
    }

    @Test
    fun `手动净化委托 Moment 仓储`() = runTest {
        val moments = FakeMoments(emptyList())
        val vm = RecycleBinViewModel(
            moments,
            FakeTasks(emptyList()),
            Settings(enabled = false),
            clock,
        )
        advanceUntilIdle()

        vm.permanentlyDelete(moment("m", "待净化时刻", "2026-08-10T00:00:00Z").toRecycleBinItem())
        advanceUntilIdle()

        assertEquals(listOf("m"), moments.permanentlyDeleted)
    }

    private fun moment(id: String, title: String, deletedAt: String) = Moment(
        id, title, "", MomentDirection.PAST_ACHIEVEMENT, LocalDate.of(2026, 1, 1), null, null,
        false, null, Instant.parse(deletedAt).toEpochMilli(), 1L, 1L,
    )

    private fun task(id: String, title: String, deletedAt: String) = Task(
        id, title, LocalDate.of(2026, 1, 1), null, null, "", null, null, null,
        false, null, false, Instant.parse(deletedAt), Instant.EPOCH, Instant.EPOCH,
    )

    private class FakeMoments(
        initial: List<Moment>,
    ) : MomentRepositoryPort {
        private val deleted = MutableStateFlow(initial)
        val restored = mutableListOf<String>()
        val permanentlyDeleted = mutableListOf<String>()
        var purgeCutoff: Long? = null
        override fun observeActive() = MutableStateFlow(emptyList<Moment>())
        override fun observeDeleted() = deleted
        override suspend fun findById(id: String) = null
        override suspend fun save(moment: Moment) = Unit
        override suspend fun seedIfEmpty(moments: List<Moment>) = Unit
        override suspend fun setPinned(id: String, pinned: Boolean) = Unit
        override suspend fun reorderPinned(orderedIds: List<String>) = Unit
        override suspend fun restore(id: String) { restored += id }
        override suspend fun permanentlyDelete(id: String) {
            permanentlyDeleted += id
        }
        override suspend fun purgeDeletedBefore(cutoffMillis: Long) { purgeCutoff = cutoffMillis }
    }

    private class FakeTasks(initial: List<Task>) : TaskRepositoryPort {
        private val deleted = MutableStateFlow(initial)
        val restored = mutableListOf<String>()
        val permanentlyDeleted = mutableListOf<String>()
        var purgeCutoff: Long? = null
        override fun observeForDate(date: LocalDate) = MutableStateFlow(emptyList<Task>())
        override fun observeDeleted() = deleted
        override suspend fun findById(id: String) = null
        override suspend fun save(task: Task) = Unit
        override suspend fun setCompleted(id: String, completed: Boolean) = Unit
        override suspend fun restore(id: String) { restored += id }
        override suspend fun permanentlyDelete(id: String) { permanentlyDeleted += id }
        override suspend fun purgeDeletedBefore(cutoffMillis: Long) { purgeCutoff = cutoffMillis }
    }

    private class Settings(enabled: Boolean) : MomentMarkSettingsStorePort {
        override val themeMode = MutableStateFlow(com.cch.momentmark.ui.theme.ThemeMode.SYSTEM)
        override val autoPurgeEnabled = MutableStateFlow(enabled)
        override suspend fun setThemeMode(mode: com.cch.momentmark.ui.theme.ThemeMode) = Unit
        override suspend fun setAutoPurgeEnabled(enabled: Boolean) = Unit
    }
}
