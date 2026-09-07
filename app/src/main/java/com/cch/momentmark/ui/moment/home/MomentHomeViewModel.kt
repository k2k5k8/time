package com.cch.momentmark.ui.moment.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.cch.momentmark.data.AppContainer
import com.cch.momentmark.data.repository.MomentRepositoryPort
import com.cch.momentmark.data.repository.TaskRepositoryPort
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 大事件首页 UI 状态：置顶全宽 / 过去左列 / 未来右列 三段投影。 */
data class MomentHomeUiState(
    val isLoading: Boolean = true,
    val projection: HomeMomentProjection = HomeMomentProjection(emptyList(), emptyList(), emptyList()),
    val errorMessage: String? = null,
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
    private val onClearedCallback: (() -> Unit)? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MomentHomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repository.seedIfEmpty(seedMoments)
                combine(
                    repository.observeActive(),
                    taskRepository.observeActive(),
                ) { moments, tasks -> moments to tasks }.collect { (moments, tasks) ->
                    _uiState.value = MomentHomeUiState(
                        isLoading = false,
                        projection = project(moments, tasks),
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

/** 与 MomentMarkAppViewModelFactory 相同的装配边界：每 VM 独立容器，清除时关闭。 */
class MomentHomeViewModelFactory(
    context: Context,
    private val seedMoments: List<Moment> = emptyList(),
) : ViewModelProvider.Factory {
    private val applicationContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MomentHomeViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        val container = AppContainer(applicationContext)
        return MomentHomeViewModel(
            repository = container.momentRepository,
            taskRepository = container.taskRepository,
            clock = container.clock,
            seedMoments = seedMoments,
            onClearedCallback = container::close,
        ) as T
    }
}
