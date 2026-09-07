package com.cch.momentmark.ui.moment

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cch.momentmark.data.AppContainer
import com.cch.momentmark.data.repository.MomentRepositoryPort
import com.cch.momentmark.data.repository.Group
import com.cch.momentmark.data.repository.GroupRepositoryPort
import com.cch.momentmark.domain.model.Moment
import com.cch.momentmark.domain.model.MomentDirection
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 「铭刻时刻」表单状态。日期仍是全日 LocalDate 的 ISO 输入，避免把展示状态写回数据库。
 */
data class MomentFormUiState(
    /** null = 新建；非空 = 编辑已有 Moment，保存必须保留其身份与生命周期字段。 */
    val editingMomentId: String? = null,
    val direction: MomentDirection = MomentDirection.FUTURE_COUNTDOWN,
    val title: String = "",
    val note: String = "",
    /** 空串代表未归入线路；落库时转换为 null。 */
    val groupId: String = "",
    /** null 代表未选择重要度；有效值为 1..3。 */
    val rarity: Int? = null,
    val anchorDateText: String,
    val selectedYear: Int? = null,
    val selectedMonth: Int? = null,
    val selectedDay: Int? = null,
    val groups: List<Group> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    /** 非空表示保存已提交；UI 消费后关闭表单并回到来源页签。 */
    val savedMomentId: String? = null,
    val savedWasEditing: Boolean = false,
)

/**
 * Moment 的创建边界：只生成 Moment，且在写入前验证方向与日期的一致性（PRD AC-02）。
 */
class MomentFormViewModel(
    private val repository: MomentRepositoryPort,
    private val clock: Clock,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
    private val groupRepository: GroupRepositoryPort = EmptyGroupRepository,
    private val onClearedCallback: (() -> Unit)? = null,
) : ViewModel() {

    private fun dateParts(date: LocalDate) = Triple(date.year, date.monthValue, date.dayOfMonth)
    private fun initialState(): MomentFormUiState {
        val date = LocalDate.now(clock)
        val (year, month, day) = dateParts(date)
        return MomentFormUiState(
            anchorDateText = date.toString(),
            selectedYear = year,
            selectedMonth = month,
            selectedDay = day,
        )
    }

    private val _uiState = MutableStateFlow(initialState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            groupRepository.observeAll().collect { groups -> _uiState.update { it.copy(groups = groups) } }
        }
    }

    /**
     * 每次进入表单都初始化：新建清空旧输入；编辑从 Repository 读取事实字段。
     * 这使编辑不会依赖详情页 remember 状态，也不会把展示派生字段写入 Room。
     */
    fun start(momentId: String?) {
        if (momentId == null) {
            _uiState.value = initialState().copy(groups = _uiState.value.groups)
            return
        }
        _uiState.value = MomentFormUiState(
            editingMomentId = momentId,
            anchorDateText = LocalDate.now(clock).toString(),
            selectedYear = LocalDate.now(clock).year,
            selectedMonth = LocalDate.now(clock).monthValue,
            selectedDay = LocalDate.now(clock).dayOfMonth,
            groups = _uiState.value.groups,
            isLoading = true,
        )
        viewModelScope.launch {
            try {
                val moment = repository.findById(momentId)
                if (moment == null || moment.deletedAt != null) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "这个时刻不存在，或已被封印。")
                    }
                    return@launch
                }
                _uiState.value = MomentFormUiState(
                    editingMomentId = moment.id,
                    direction = moment.creationDirection,
                    title = moment.title,
                    note = moment.note,
                    groupId = moment.groupId.orEmpty(),
                    rarity = moment.rarity,
                    anchorDateText = moment.anchorDate.toString(),
                    selectedYear = moment.anchorDate.year,
                    selectedMonth = moment.anchorDate.monthValue,
                    selectedDay = moment.anchorDate.dayOfMonth,
                    groups = _uiState.value.groups,
                )
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = throwable.message ?: "时刻加载失败，请重试")
                }
            }
        }
    }

    fun selectDirection(direction: MomentDirection) {
        _uiState.update { it.copy(direction = direction, errorMessage = null) }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title, errorMessage = null) }
    }

    fun updateNote(note: String) {
        _uiState.update { it.copy(note = note, errorMessage = null) }
    }

    fun updateGroup(groupId: String) {
        _uiState.update { it.copy(groupId = groupId, errorMessage = null) }
    }

    fun selectRarity(rarity: Int?) {
        require(rarity == null || rarity in 1..3) { "rarity must be null or within 1..3" }
        _uiState.update { it.copy(rarity = rarity, errorMessage = null) }
    }

    fun updateAnchorDate(text: String) {
        val parsed = runCatching { LocalDate.parse(text.trim()) }.getOrNull()
        _uiState.update {
            it.copy(
                anchorDateText = text,
                selectedYear = parsed?.year,
                selectedMonth = parsed?.monthValue,
                selectedDay = parsed?.dayOfMonth,
                errorMessage = null,
            )
        }
    }

    fun selectYear(year: Int) = updateDatePart(year = year)
    fun selectMonth(month: Int) = updateDatePart(month = month)
    fun selectDay(day: Int) = updateDatePart(day = day)

    private fun updateDatePart(year: Int? = null, month: Int? = null, day: Int? = null) {
        val state = _uiState.value
        val base = runCatching { LocalDate.parse(state.anchorDateText.trim()) }
            .getOrElse { LocalDate.now(clock) }
        val targetYear = year ?: state.selectedYear ?: base.year
        val targetMonth = month ?: state.selectedMonth ?: base.monthValue
        val maxDay = LocalDate.of(targetYear, targetMonth, 1).lengthOfMonth()
        val targetDay = (day ?: state.selectedDay ?: base.dayOfMonth).coerceIn(1, maxDay)
        val target = LocalDate.of(targetYear, targetMonth, targetDay)
        _uiState.update {
            it.copy(
                anchorDateText = target.toString(),
                selectedYear = target.year,
                selectedMonth = target.monthValue,
                selectedDay = target.dayOfMonth,
                errorMessage = null,
            )
        }
    }

    fun submit() {
        val state = _uiState.value
        val validation = validate(state, LocalDate.now(clock))
        if (validation != null) {
            _uiState.update { it.copy(errorMessage = validation) }
            return
        }
        val anchorDate = LocalDate.parse(state.anchorDateText.trim())
        val editingId = state.editingMomentId
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val existing = if (editingId == null) null else repository.findById(editingId)
                if (editingId != null && (existing == null || existing.deletedAt != null)) {
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = "这个时刻不存在，或已被封印。")
                    }
                    return@launch
                }
                val id = existing?.id ?: idGenerator()
                repository.save(
                    Moment(
                        id = id,
                        title = state.title.trim(),
                        note = state.note.trim(),
                        creationDirection = state.direction,
                        anchorDate = anchorDate,
                        groupId = state.groupId.trim().ifEmpty { null },
                        rarity = state.rarity,
                        isPinned = existing?.isPinned ?: false,
                        pinnedOrder = existing?.pinnedOrder,
                        deletedAt = existing?.deletedAt,
                        createdAt = existing?.createdAt ?: clock.millis(),
                        updatedAt = existing?.updatedAt ?: clock.millis(),
                    ),
                )
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        savedMomentId = id,
                        savedWasEditing = existing != null,
                    )
                }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = throwable.message ?: "铭刻失败，请重试",
                    )
                }
            }
        }
    }

    fun consumeSaved() {
        _uiState.update { it.copy(savedMomentId = null, savedWasEditing = false) }
    }

    private fun validate(state: MomentFormUiState, today: LocalDate): String? {
        if (state.title.isBlank()) return "请填写时刻名称。"
        val anchorDate = runCatching { LocalDate.parse(state.anchorDateText.trim()) }.getOrNull()
            ?: return "日期格式为 YYYY-MM-DD。"
        return when (state.direction) {
            MomentDirection.FUTURE_COUNTDOWN ->
                if (anchorDate.isBefore(today)) "未来·倒数只能选择今天或未来日期。" else null
            MomentDirection.PAST_ACHIEVEMENT ->
                if (anchorDate.isAfter(today)) "过去·正数只能选择今天或过去日期。" else null
        }
    }

    override fun onCleared() {
        onClearedCallback?.invoke()
        super.onCleared()
    }
}

/** 单独装配 Moment 表单，避免让 Composable 直接触碰 Room。 */
class MomentFormViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val applicationContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MomentFormViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        val container = AppContainer(applicationContext)
        return MomentFormViewModel(
            repository = container.momentRepository,
            clock = container.clock,
            onClearedCallback = container::close,
            groupRepository = container.groupRepository,
        ) as T
    }
}

private object EmptyGroupRepository : GroupRepositoryPort
