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
import java.time.Clock
import java.time.LocalDate
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
                _uiState.value = MomentDetailUiState(
                    isLoading = false,
                    moment = moment,
                    status = result.status,
                    days = result.amount,
                    today = EventTimeCalculator.today(clock),
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
}

class MomentDetailViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val applicationContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MomentDetailViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        val container = AppContainer(applicationContext)
        return MomentDetailViewModel(
            repository = container.momentRepository,
            clock = container.clock,
            onClearedCallback = container::close,
        ) as T
    }
}
