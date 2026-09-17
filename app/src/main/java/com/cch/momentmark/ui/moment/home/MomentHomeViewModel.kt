package com.cch.momentmark.ui.moment.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.os.SystemClock
import com.cch.momentmark.data.AppContainer
import com.cch.momentmark.data.repository.MomentRepositoryPort
import com.cch.momentmark.data.repository.TaskRepositoryPort
import com.cch.momentmark.data.settings.MomentMarkSettingsStorePort
import com.cch.momentmark.domain.model.HomeMomentProjector
import com.cch.momentmark.domain.model.HomeMomentProjection
import com.cch.momentmark.domain.model.Moment
import com.cch.momentmark.domain.model.Task
import java.time.Clock
import java.time.LocalDate
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 大事件首页 UI 状态：置顶全宽 / 过去左列 / 未来右列 三段投影。 */
data class MomentHomeUiState(
    val isLoading: Boolean = true,
    val projection: HomeMomentProjection = HomeMomentProjection(emptyList(), emptyList(), emptyList()),
    val errorMessage: String? = null,
    /** 第⑧屏分类抽屉是否展开（首页局部 UI 状态，非路由）。 */
    val drawerOpen: Boolean = false,
    /** 抽屉「分组 PARTY LINES」的活动 Moment 计数，按分组名聚合（Moment.groupId 存名称）。 */
    val filterCounts: Map<String, Int> = emptyMap(),
    /** 抽屉状态段「全部 ×N」的活动 Moment 总数。 */
    val filterTotal: Int = 0,
) {
    val isEmpty: Boolean
        get() = !isLoading && projection.pinned.isEmpty() &&
            projection.past.isEmpty() && projection.rightItems.isEmpty()
}

/**
 * 大事件首页 ViewModel：只读投影 Moment 事实，不落库任何派生值；
 * 相对天数与展示状态由 EventTimeCalculator 经 HomeMomentProjector 派生。
 */
class MomentHomeViewModel(
    private val repository: MomentRepositoryPort,
    private val taskRepository: TaskRepositoryPort = EmptyTaskRepository,
    private val clock: Clock,
    seedMoments: List<Moment> = emptyList(),
    private val settingsStore: MomentMarkSettingsStorePort = NeverSeededSettingsStore,
    private val onClearedCallback: (() -> Unit)? = null,
    private val uptimeMillis: () -> Long = SystemClock::uptimeMillis,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MomentHomeUiState())
    val uiState = _uiState.asStateFlow()

    /** [openDrawer] 的时刻（uptimeMillis），0 = 本会话尚未呼出过抽屉。 */
    private var drawerOpenRequestedAtMillis = 0L

    init {
        viewModelScope.launch {
            try {
                // 默认首页事件只注入一次：仅当 DataStore 一次性标记未置位时，先清理
                // 历史开发期 sample- 示例行，再在空库时写入默认事件并落标记。
                // 此后用户对首页事件的增删改完全自由：不再清理、不再回填，
                // 即使全部封印净化，默认事件也不会在下次启动复活。
                if (!settingsStore.defaultMomentsSeeded.first()) {
                    repository.removeSampleRows()
                    repository.seedIfEmpty(seedMoments)
                    settingsStore.markDefaultMomentsSeeded()
                }
                combine(
                    repository.observeActive(),
                    taskRepository.observeActive(),
                ) { moments, tasks -> moments to tasks }.collect { (moments, tasks) ->
                    _uiState.value = MomentHomeUiState(
                        isLoading = false,
                        projection = project(moments, tasks),
                        filterCounts = moments.groupingBy { it.groupId.orEmpty() }.eachCount(),
                        filterTotal = moments.size,
                    )
                }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: throwable::class.simpleName ?: "加载失败",
                    )
                }
            }
        }
    }

    /** 供测试与未来「跨午夜刷新」复用的同口径投影。 */
    internal fun project(moments: List<Moment>, tasks: List<Task> = emptyList()): HomeMomentProjection =
        HomeMomentProjector.project(
            moments = moments,
            today = LocalDate.now(clock),
            zoneId = clock.zone,
            tasks = tasks,
        )

    fun setPinned(id: String, pinned: Boolean) {
        viewModelScope.launch {
            runCatching { repository.setPinned(id, pinned) }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    _uiState.update { state ->
                        state.copy(errorMessage = throwable.message ?: "操作失败")
                    }
                }
        }
    }

    /**
     * 调整置顶主线的相邻顺序。排序事实只通过 Repository 一次写入，
     * HomeMomentProjector 仍是唯一的展示排序来源（PRD AC-05a）。
     */
    fun movePinned(id: String, offset: Int) {
        val ids = _uiState.value.projection.pinned.map { it.id }
        val currentIndex = ids.indexOf(id)
        val targetIndex = currentIndex + offset
        if (currentIndex < 0 || targetIndex !in ids.indices) return
        val orderedIds = ids.toMutableList().apply {
            this[currentIndex] = this[targetIndex]
            this[targetIndex] = id
        }
        viewModelScope.launch {
            runCatching { repository.reorderPinned(orderedIds) }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    _uiState.update { state ->
                        state.copy(errorMessage = throwable.message ?: "置顶排序保存失败")
                    }
                }
        }
    }

    /**
     * 第⑧屏分类抽屉：首页左缘右滑呼出，scrim/ESC/返回键关闭。
     *
     * 记录呼出时刻用于「竞争返回」判定：手势导航下系统返回手势与左缘右滑几乎同时完成，
     * 部分设备（如 Android 16+ 预测性返回）会无视手势排除矩形，紧随其后向应用派发一次返回
     * 事件——若直接关闭抽屉/结束应用，左缘右滑将永远无法稳定呼出。该事件由
     * [consumeDrawerOpenBack] 在宽限期内吞掉。
     */
    fun openDrawer() {
        drawerOpenRequestedAtMillis = uptimeMillis()
        _uiState.update { it.copy(drawerOpen = true) }
    }

    fun closeDrawer() {
        _uiState.update { it.copy(drawerOpen = false) }
    }

    /**
     * 判定并消耗一次「抽屉刚被左缘右滑呼出后的竞争返回事件」。
     * 仅在 [openDrawer] 后的 [DrawerOpenBackGraceMillis] 内返回 true（且只消耗一次），
     * 之后恢复普通返回语义（关闭抽屉 / 结束应用）。
     */
    fun consumeDrawerOpenBack(nowMillis: Long = uptimeMillis()): Boolean {
        val requestedAt = drawerOpenRequestedAtMillis
        if (requestedAt == 0L) return false
        val withinGrace = nowMillis - requestedAt <= DrawerOpenBackGraceMillis
        if (withinGrace) drawerOpenRequestedAtMillis = 0L
        return withinGrace
    }

    private companion object {
        /**
         * 竞争返回的宽限期：覆盖「手势完成 → 预测性返回动画 → 返回事件派发到应用」的全程
         * （实测 Android 16+ 约 1.5~2.5s）。正常设备上该窗口内不会出现返回事件，
         * 因此不影响真实返回操作。
         */
        const val DrawerOpenBackGraceMillis = 2_500L
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        onClearedCallback?.invoke()
        super.onCleared()
    }
}

/** 让仅验证 Moment 的旧调用仍能立即产生空 Task 流，而非永久停在加载态。 */
private object EmptyTaskRepository : TaskRepositoryPort {
    override fun observeActive() = flowOf(emptyList<Task>())
    override fun observeForDate(date: LocalDate) = flowOf(emptyList<Task>())
    override suspend fun findById(id: String): Task? = null
    override suspend fun save(task: Task) = Unit
    override suspend fun setCompleted(id: String, completed: Boolean) = Unit
}

/** 未显式注入设置存储时的兜底：视为「尚未注入默认事件」，保持旧有的注入语义。 */
private object NeverSeededSettingsStore : MomentMarkSettingsStorePort {
    override val themeMode = flowOf(com.cch.momentmark.ui.theme.ThemeMode.SYSTEM)
    override val defaultMomentsSeeded = flowOf(false)
    override suspend fun setThemeMode(mode: com.cch.momentmark.ui.theme.ThemeMode) = Unit
}

/** 与 MomentMarkAppViewModelFactory 相同的装配边界：每 VM 独立容器，清除时关闭。 */
class MomentHomeViewModelFactory(
    context: Context,
    private val seedMoments: List<Moment> = emptyList(),
    private val clockOverride: Clock? = null,
) : ViewModelProvider.Factory {
    private val applicationContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MomentHomeViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        val container = AppContainer.get(applicationContext)
        return MomentHomeViewModel(
            repository = container.momentRepository,
            taskRepository = container.taskRepository,
            clock = clockOverride ?: container.clock,
            seedMoments = seedMoments,
            settingsStore = container.settingsStore,
        ) as T
    }
}
