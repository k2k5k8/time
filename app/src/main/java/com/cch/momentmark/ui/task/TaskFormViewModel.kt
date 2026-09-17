package com.cch.momentmark.ui.task

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cch.momentmark.BuildConfig
import com.cch.momentmark.data.AppContainer
import com.cch.momentmark.data.repository.TaskRepositoryPort
import com.cch.momentmark.domain.model.Task
import com.cch.momentmark.domain.model.TaskType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 「接受新任务」表单状态。日期 = 本地自然日 LocalDate；保存后可空的时间 Instant 由 dueTimeText 派生。
 *
 * UI 可见的 dueDateText / dueTimeText 与 design.html 第 ④ 屏示例（"2026.09.18" + "10:00"）保持同款
 * 文本格式，但提交时统一解析成 ISO / HH:MM 输入；日期真实字段仍以本地自然日落库，与时间 Instant 独立。
 */
data class TaskFormUiState(
    /** null = 新建；非空 = 编辑已有 Task，保存必须保留任务完成与生命周期事实。 */
    val editingTaskId: String? = null,
    val title: String = "",
    /** UI 可见日期（"2026.09.18"），提交时解析回 LocalDate；与时间字段独立。 */
    val dueDateText: String,
    /** 由注入 Clock 派生，仅用于快捷日期选中态；不写入 Task。 */
    val quickTodayDateText: String,
    val quickTomorrowDateText: String,
    val quickSaturdayDateText: String,
    /** UI 可见时间（"10:00"），提交时解析回 LocalTime；空串 = 全天。 */
    val dueTimeText: String = "",
    val note: String = "",
    /** null = 未选难度；有效值 1..3。其它字段（任务类型 / 分组 / 显示在首页）由设计稿第 ④ 屏未展示，
     *  本轮仅保留在数据层，由 ViewModel 用默认值落库，UI 不展示。 */
    val taskType: TaskType? = TaskType.SIDE,
    val difficulty: Int? = null,
    val groupId: String = "",
    val showOnHome: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedDueDate: LocalDate? = null,
)

/** 唯一的新任务写入边界；默认今天，不允许从首页/新时刻调用。 */
class TaskFormViewModel(
    private val repository: TaskRepositoryPort,
    private val clock: Clock,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
    private val onClearedCallback: (() -> Unit)? = null,
) : ViewModel() {
    /**
     * 初始状态。生产路径永远只新建 + 今天；DEBUG 下可被 [debugSeedFixture] 切换为 `sample`
     * （预置与 `design.html` 第 ④ 屏一致的**结构**：日期 2026.09.18 / 时间 10:00 / 难度 ★☆☆）
     * 或 `empty`（与生产等价的空表单）。
     */
    private fun initialState(): TaskFormUiState {
        val today = LocalDate.now(clock)
        val thisSaturday = today.plusDays(
            ((java.time.DayOfWeek.SATURDAY.value - today.dayOfWeek.value + 7) % 7).toLong(),
        )
        val base = TaskFormUiState(
            dueDateText = formatUiDate(today),
            quickTodayDateText = formatUiDate(today),
            quickTomorrowDateText = formatUiDate(today.plusDays(1)),
            quickSaturdayDateText = formatUiDate(thisSaturday),
        )
        // fixture 由 TaskFormViewModelFactory 在 DEBUG 构建下读取 setprop 后注入；
        // 这里只判断「是否注入过」，让 JVM 单测（BuildConfig.DEBUG=false）也能覆盖 fixture 行为。
        return when (debugSeedFixture) {
            "sample" -> base.copy(
                dueDateText = "2026.09.18",
                dueTimeText = "10:00",
                difficulty = 1,
            )
            // "empty" 与任何其他值都走默认（生产等价）。
            else -> base
        }
    }

    /**
     * 设计稿截屏 fixture。生产构建下保持 ""（生产行为）；仅当 [BuildConfig.DEBUG] 时由
     * [TaskFormViewModelFactory] 在创建时一次性写入。
     *
     * 取值：
     * - `empty`  —— 空表单（今天日期 + 空时间 + 难度 ★☆☆），与生产等价
     * - `sample` —— 预置 design.html 第 ④ 屏的**结构**：`2026.09.18` / `10:00` / 难度 1
     *
     * ⚠ 任务名称「抢演唱会门票」与任务描述段落只是 HTML 稿的展示效果，**不在 fixture 中预填**
     * （用户 2026-09-14 明确要求删除）；`title` / `note` 恒为数据类默认的空串。
     */
    var debugSeedFixture: String = ""

    private val _uiState = MutableStateFlow(initialState())
    val uiState = _uiState.asStateFlow()

    /** 每次进入表单都清空新建状态或按 ID 预填编辑事实字段。 */
    fun start(taskId: String?) {
        if (taskId == null) {
            _uiState.value = initialState()
            return
        }
        _uiState.value = initialState().copy(editingTaskId = taskId, isLoading = true)
        viewModelScope.launch {
            try {
                val task = repository.findById(taskId)
                if (task == null || task.deletedAt != null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "这个任务不存在，或已被封印。") }
                    return@launch
                }
                _uiState.value = initialState().copy(
                    editingTaskId = task.id,
                    title = task.title,
                    dueDateText = formatUiDate(task.dueLocalDate),
                    dueTimeText = task.dueInstant?.atZone(task.zoneId ?: clock.zone)?.toLocalTime()?.toString().orEmpty(),
                    note = task.note,
                    taskType = task.taskType,
                    difficulty = task.difficulty,
                    groupId = task.groupId.orEmpty(),
                    showOnHome = task.showOnHome,
                )
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message ?: "任务加载失败，请重试") }
            }
        }
    }

    fun updateTitle(value: String) = _uiState.update { it.copy(title = value, errorMessage = null) }
    fun updateDate(value: String) = _uiState.update { it.copy(dueDateText = value, errorMessage = null) }
    fun updateTime(value: String) = _uiState.update { it.copy(dueTimeText = value, errorMessage = null) }
    fun updateNote(value: String) = _uiState.update { it.copy(note = value, errorMessage = null) }
    fun selectDifficulty(value: Int?) {
        require(value == null || value in 1..3) { "difficulty must be null or within 1..3" }
        _uiState.update { it.copy(difficulty = value, errorMessage = null) }
    }

    fun submit() {
        val state = _uiState.value
        val date = parseUiDate(state.dueDateText.trim())
        val error = validate(state, date)
        if (error != null || date == null) {
            _uiState.update { it.copy(errorMessage = error ?: "日期格式为 YYYY.MM.DD。") }
            return
        }
        val dueInstant = state.dueTimeText.trim().takeIf(String::isNotEmpty)?.let { raw ->
            runCatching { LocalDateTime.of(date, LocalTime.parse(raw)).atZone(clock.zone).toInstant() }.getOrNull()
        }
        val editingId = state.editingTaskId
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val existing = if (editingId == null) null else repository.findById(editingId)
                if (editingId != null && (existing == null || existing.deletedAt != null)) {
                    _uiState.update { it.copy(isSaving = false, errorMessage = "这个任务不存在，或已被封印。") }
                    return@launch
                }
                val now = Instant.ofEpochMilli(clock.millis())
                repository.save(
                    Task(
                        id = existing?.id ?: idGenerator(),
                        title = state.title.trim(),
                        dueLocalDate = date,
                        dueInstant = dueInstant,
                        zoneId = dueInstant?.let { clock.zone } ?: existing?.zoneId,
                        note = state.note.trim(),
                        taskType = state.taskType,
                        difficulty = state.difficulty,
                        groupId = state.groupId.trim().ifEmpty { null },
                        isCompleted = existing?.isCompleted ?: false,
                        completedAt = existing?.completedAt,
                        showOnHome = state.showOnHome,
                        deletedAt = existing?.deletedAt,
                        createdAt = existing?.createdAt ?: now,
                        updatedAt = existing?.updatedAt ?: now,
                    ),
                )
                _uiState.update { it.copy(isSaving = false, savedDueDate = date) }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                _uiState.update { it.copy(isSaving = false, errorMessage = throwable.message ?: "接受任务失败，请重试") }
            }
        }
    }

    fun consumeSaved() {
        if (_uiState.value.savedDueDate != null) _uiState.value = initialState()
    }

    private fun validate(state: TaskFormUiState, date: LocalDate?): String? {
        if (state.title.isBlank()) return "请填写任务名称。"
        if (date == null) return "日期格式为 YYYY.MM.DD。"
        if (state.dueTimeText.isNotBlank() && runCatching { LocalTime.parse(state.dueTimeText.trim()) }.isFailure) {
            return "截止时间格式为 HH:MM。"
        }
        return null
    }

    override fun onCleared() {
        onClearedCallback?.invoke()
        super.onCleared()
    }

    /**
     * design.html 第 ④ 屏示例可见的日期格式 `YYYY.MM.DD`（如 `2026.09.18`）。
     * 与 `LocalDate.parse` 的 ISO 格式 `YYYY-MM-DD` 略有差异，需要独立解析路径。
     */
    private fun formatUiDate(date: LocalDate): String =
        "%04d.%02d.%02d".format(date.year, date.monthValue, date.dayOfMonth)

    private fun parseUiDate(text: String): LocalDate? {
        // 先按 design 的点分格式解析；点分隔失败时回落到 ISO，方便用户混输。
        val dotted = Regex("""(\d{4})\.(\d{1,2})\.(\d{1,2})""")
        dotted.matchEntire(text)?.let { m ->
            val (y, mo, d) = m.destructured
            return runCatching { LocalDate.of(y.toInt(), mo.toInt(), d.toInt()) }.getOrNull()
        }
        return runCatching { LocalDate.parse(text) }.getOrNull()
    }
}

class TaskFormViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val applicationContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TaskFormViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        val container = AppContainer.get(applicationContext)
        val viewModel = TaskFormViewModel(
            repository = container.taskRepository,
            clock = container.clock,
        )
        // 设计稿截屏 fixture：`adb shell setprop debug.momentmark.taskform empty|sample`。
        // 仅 debug 构建生效；release 构建始终保持 ""（生产行为）。
        if (BuildConfig.DEBUG) {
            viewModel.debugSeedFixture = readDebugSeed(applicationContext)
        }
        return viewModel as T
    }

    private fun readDebugSeed(context: Context): String = try {
        val clazz = Class.forName("android.os.SystemProperties")
        val get = clazz.getMethod("get", String::class.java, String::class.java)
        (get.invoke(null, "debug.momentmark.taskform", "") as? String).orEmpty()
    } catch (_: Throwable) {
        ""
    }
}
