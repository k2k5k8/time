package com.cch.momentmark.ui.moment.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cch.momentmark.data.AppContainer
import com.cch.momentmark.data.repository.MomentRepositoryPort
import com.cch.momentmark.domain.model.Moment
import com.cch.momentmark.domain.time.EventTimeCalculator
import com.cch.momentmark.domain.time.EventTimeStatus
import com.cch.momentmark.domain.time.Milestone
import com.cch.momentmark.domain.time.MilestoneCalculator
import com.cch.momentmark.domain.time.MilestoneStatus
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 前置目标（QUEST OBJECTIVES）行：P1 才接数据；P0 详情只渲染展示壳。 */
data class QuestObjectiveUi(
    val title: String,
    val completed: Boolean,
)

/** 详情页只保存 Moment 事实；天数与过去/未来展示在读取时用统一时间内核推导。 */
data class MomentDetailUiState(
    val isLoading: Boolean = true,
    val moment: Moment? = null,
    val status: EventTimeStatus? = null,
    val days: Long = 0L,
    /** Natural day used for this projection; retained only in UI state. */
    val today: LocalDate? = null,
    val isPinUpdating: Boolean = false,
    val isSealing: Boolean = false,
    val errorMessage: String? = null,
    /** QUEST 横幅经验格已填格数：由创建日→目标日的时间轴实时推导（fixture 直接覆盖）。 */
    val filledXpCells: Int = 0,
    /** QUEST 横幅 PROGRESS 百分比：同上实时推导；null 时 UI 显示占位。 */
    val progressPercent: Int? = null,
    /** 前置目标行（P1 关联目标后端未定；P0 空列表，只渲染面板壳）。 */
    val questObjectives: List<QuestObjectiveUi> = emptyList(),
    /** 成就里程碑：由 `MilestoneCalculator` 派生，仅成就展示模式使用；QUEST 模式为空。 */
    val milestones: List<Milestone> = emptyList(),
)

class MomentDetailViewModel(
    private val repository: MomentRepositoryPort,
    private val clock: Clock,
    private val onClearedCallback: (() -> Unit)? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MomentDetailUiState())
    val uiState = _uiState.asStateFlow()

    fun load(momentId: String) {
        _uiState.value = MomentDetailUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val moment = repository.findById(momentId)
                if (moment == null || moment.deletedAt != null) {
                    _uiState.value = MomentDetailUiState(
                        isLoading = false,
                        errorMessage = "这个时刻不存在，或已被封印。",
                    )
                    return@launch
                }
                val result = EventTimeCalculator.allDay(moment.anchorDate, clock, clock.zone)
                val today = EventTimeCalculator.today(clock)
                val (filledCells, progressPct) = questProgress(moment, today)
                val milestones = if (result.status == EventTimeStatus.PAST ||
                    (result.status == EventTimeStatus.TODAY && moment.creationDirection == com.cch.momentmark.domain.model.MomentDirection.PAST_ACHIEVEMENT)
                ) {
                    trimMilestones(MilestoneCalculator.calculate(moment.anchorDate, today))
                } else {
                    emptyList()
                }
                _uiState.value = MomentDetailUiState(
                    isLoading = false,
                    moment = moment,
                    status = result.status,
                    days = result.amount,
                    today = today,
                    filledXpCells = filledCells,
                    progressPercent = progressPct,
                    milestones = milestones,
                )
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                _uiState.value = MomentDetailUiState(
                    isLoading = false,
                    errorMessage = throwable.message ?: "时刻详情加载失败。",
                )
            }
        }
    }

    /**
     * 里程碑面板只展示最近 3 个已达成 + 下一项 + 1 个远征中（共 5 项），
     * 避免早期节点把面板撑得过长。MilestoneCalculator 返回按日期升序，
     * 已达成取末尾 3 个即最近达成的节点。
     */
    private fun trimMilestones(all: List<Milestone>): List<Milestone> {
        val achieved = all.filter { it.status == MilestoneStatus.ACHIEVED }
        val upcoming = all.filter { it.status != MilestoneStatus.ACHIEVED }
        return achieved.takeLast(3) + upcoming.take(2)
    }

    fun togglePinned() {
        val moment = _uiState.value.moment ?: return
        _uiState.update { it.copy(isPinUpdating = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                repository.setPinned(moment.id, !moment.isPinned)
                load(moment.id)
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                _uiState.update {
                    it.copy(
                        isPinUpdating = false,
                        errorMessage = throwable.message ?: "置顶状态保存失败。",
                    )
                }
            }
        }
    }

    fun seal(onSealed: (Moment) -> Unit) {
        val moment = _uiState.value.moment ?: return
        _uiState.update { it.copy(isSealing = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                repository.softDelete(moment.id)
                onSealed(moment)
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                _uiState.update { it.copy(isSealing = false, errorMessage = throwable.message ?: "封印时刻失败。") }
            }
        }
    }

    override fun onCleared() {
        onClearedCallback?.invoke()
        super.onCleared()
    }

    /**
     * QUEST 横幅经验格/百分比：以时刻创建日为起点、目标日为终点，
     * 按「今天在这段时间轴上的位置」实时计算。随时间推移逐渐填满，到目标日满格。
     * 总跨度 ≤0（创建即到目标）或已过目标日时夹取到 0/16。
     */
    private fun questProgress(moment: Moment, today: LocalDate): Pair<Int, Int> {
        val startDay = Instant.ofEpochMilli(moment.createdAt).atZone(clock.zone).toLocalDate()
        val total = ChronoUnit.DAYS.between(startDay, moment.anchorDate)
        if (total <= 0L) return 0 to 0
        val elapsed = ChronoUnit.DAYS.between(startDay, today)
        val fraction = (elapsed.toDouble() / total.toDouble()).coerceIn(0.0, 1.0)
        val cells = Math.round(fraction * 16).toInt().coerceIn(0, 16)
        val pct = Math.round(fraction * 100).toInt().coerceIn(0, 100)
        return cells to pct
    }
}

class MomentDetailViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val applicationContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MomentDetailViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        val container = AppContainer.get(applicationContext)
        return MomentDetailViewModel(
            repository = container.momentRepository,
            clock = container.clock,
        ) as T
    }
}
