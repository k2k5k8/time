package com.cch.momentmark.ui.app

import com.cch.momentmark.data.local.TimeEventEntity
import com.cch.momentmark.data.repository.TimeEventRepositoryPort
import com.cch.momentmark.data.settings.MomentMarkGroupStorePort
import com.cch.momentmark.data.settings.MomentMarkSettingsStorePort
import com.cch.momentmark.domain.model.EventCardPaletteKey
import com.cch.momentmark.domain.model.EventCardTemplateKey
import com.cch.momentmark.domain.model.EventColorRole
import com.cch.momentmark.domain.model.EventTimeType
import com.cch.momentmark.domain.model.TimeEvent
import com.cch.momentmark.ui.theme.ThemeMode
import java.time.LocalDate
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MomentMarkAppViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeEventRepository
    private lateinit var settings: FakeSettingsStore
    private lateinit var groups: FakeGroupStore

    @Before
    fun setUp() {
        repository = FakeEventRepository()
        settings = FakeSettingsStore()
        groups = FakeGroupStore()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoadSeedsAndPublishesCombinedState() = runTest {
        val vm = newViewModel(seedEvents = listOf(sampleEvent("seeded")))

        advanceUntilIdle()

        assertEquals(1, repository.seedCalls)
        assertEquals(listOf("seeded"), vm.uiState.value.events.map { it.id })
        assertEquals(ThemeMode.SYSTEM, vm.uiState.value.themeMode)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun saveDelegatesToRepository() = runTest {
        val vm = newViewModel()
        val event = sampleEvent("saved")

        vm.saveEvent(event)
        advanceUntilIdle()

        assertEquals(event, repository.savedEvent)
    }

    @Test
    fun softDeleteDelegatesWithEventId() = runTest {
        val vm = newViewModel()

        vm.softDeleteEvent("deleted")
        advanceUntilIdle()

        assertEquals("deleted", repository.softDeletedId)
    }

    @Test
    fun restoreDelegatesWithEventId() = runTest {
        val vm = newViewModel()

        vm.restoreDeleted("restored")
        advanceUntilIdle()

        assertEquals("restored", repository.restoredId)
    }

    @Test
    fun permanentDeleteDelegatesWithEventId() = runTest {
        val vm = newViewModel()

        vm.permanentlyDelete("permanent")
        advanceUntilIdle()

        assertEquals("permanent", repository.permanentlyDeletedId)
    }

    @Test
    fun purgeDeletedDelegatesToRepository() = runTest {
        val vm = newViewModel()

        vm.purgeDeleted()
        advanceUntilIdle()

        assertEquals(1, repository.purgeCalls)
    }

    @Test
    fun groupChangesUpdateStoreAndEventAssignments() = runTest {
        groups = FakeGroupStore(listOf("旅行"))
        val vm = newViewModel()

        vm.createGroup("工作")
        advanceUntilIdle()
        vm.renameGroup("旅行", "出行")
        advanceUntilIdle()

        assertEquals(listOf("出行", "工作"), groups.groups.value)
        assertEquals("旅行", repository.renamedFrom)
        assertEquals("出行", repository.renamedTo)
    }

    @Test
    fun themeChangeUpdatesStoreAndUiState() = runTest {
        val vm = newViewModel()

        vm.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, settings.lastSetMode)
        assertEquals(ThemeMode.DARK, vm.uiState.value.themeMode)
    }

    private fun newViewModel(seedEvents: List<TimeEvent> = emptyList()) =
        MomentMarkAppViewModel(
            eventRepository = repository,
            settingsStore = settings,
            groupStore = groups,
            seedEvents = seedEvents,
        )

    class MainDispatcherRule : TestWatcher() {
        private val dispatcher = UnconfinedTestDispatcher()

        override fun starting(description: Description) {
            Dispatchers.setMain(dispatcher)
        }
    }

    private class FakeEventRepository(
        initialEvents: List<TimeEventEntity> = emptyList(),
    ) : TimeEventRepositoryPort {
        private val active = MutableStateFlow(initialEvents)
        private val deleted = MutableStateFlow(emptyList<TimeEventEntity>())
        var seedCalls = 0
        var savedEvent: TimeEvent? = null
        var softDeletedId: String? = null
        var restoredId: String? = null
        var permanentlyDeletedId: String? = null
        var purgeCalls = 0
        var renamedFrom: String? = null
        var renamedTo: String? = null

        override fun observeActive(): Flow<List<TimeEventEntity>> = active

        override fun observeDeleted(): Flow<List<TimeEventEntity>> = deleted

        override suspend fun seedIfEmpty(events: List<TimeEvent>) {
            seedCalls += 1
            if (active.value.isEmpty()) {
                active.value = events.map { event -> entity(event.id, event.title) }
            }
        }

        override suspend fun save(event: TimeEvent) {
            savedEvent = event
        }

        override suspend fun setArchived(id: String, archived: Boolean, updatedAt: Long) = Unit

        override suspend fun setPinned(id: String, pinned: Boolean, updatedAt: Long) = Unit

        override suspend fun renameGroup(oldGroup: String, newGroup: String, updatedAt: Long) {
            renamedFrom = oldGroup
            renamedTo = newGroup
        }

        override suspend fun clearGroup(group: String, updatedAt: Long) = Unit

        override suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long) {
            softDeletedId = id
        }

        override suspend fun restoreDeleted(id: String, updatedAt: Long) {
            restoredId = id
        }

        override suspend fun permanentlyDelete(id: String) {
            permanentlyDeletedId = id
        }

        override suspend fun purgeDeleted() {
            purgeCalls += 1
        }
    }

    private class FakeSettingsStore(
        initialMode: ThemeMode = ThemeMode.SYSTEM,
    ) : MomentMarkSettingsStorePort {
        override val themeMode = MutableStateFlow(initialMode)
        var lastSetMode: ThemeMode? = null

        override suspend fun setThemeMode(mode: ThemeMode) {
            lastSetMode = mode
            themeMode.value = mode
        }
    }

    private class FakeGroupStore(
        initialGroups: List<String> = emptyList(),
    ) : MomentMarkGroupStorePort {
        override val groups = MutableStateFlow(initialGroups)

        override suspend fun saveGroups(groups: List<String>) {
            this.groups.value = groups
        }
    }

    private companion object {
        fun sampleEvent(id: String) = TimeEvent(
            id = id,
            title = id,
            timeType = EventTimeType.ALL_DAY,
            dateLabel = "2026.09.01",
            relativeLabel = "还有 1 天",
            icon = "●",
            colorRole = EventColorRole.FUTURE,
            cardPaletteKey = EventCardPaletteKey.BLUE_WHITE,
            cardTemplateKey = EventCardTemplateKey.CLASSIC,
            localDate = LocalDate.of(2026, 9, 1),
        )

        fun entity(id: String, title: String) = TimeEventEntity(
            id = id,
            title = title,
            subtitle = "",
            timeType = "ALL_DAY",
            localDateIso = "2026-09-01",
            instantEpochMillis = null,
            zoneId = null,
            note = "",
            iconKey = "●",
            paletteKey = "BLUE_WHITE",
            templateKey = "CLASSIC",
            templateConfigJson = "",
            advancedConfigJson = "",
            groupId = null,
            isPinned = false,
            isArchived = false,
            sortOrder = 0,
            deletedAt = null,
            createdAt = 0,
            updatedAt = 0,
        )
    }
}
