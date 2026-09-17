package com.cch.momentmark.ui.moment.detail

import com.cch.momentmark.domain.model.Moment
import com.cch.momentmark.domain.model.MomentDirection
import com.cch.momentmark.domain.time.EventTimeStatus
import com.cch.momentmark.domain.time.Milestone
import com.cch.momentmark.domain.time.MilestoneStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * DEBUG 视觉回归 fixture：仅供 `adb shell setprop debug.momentmark.detail quest`
 * 启动时使用，冻结 design.html 第 ⑥ 屏「任务详情 · QUEST DETAIL · 未来 · 昼」的示例数据。
 *
 * 这些值只用于生成与设计稿一致的截图，不写入生产数据库；release 不应触发
 * （调用方必须检查 `BuildConfig.DEBUG`）。
 *
 * 与 design.html 第 ⑥ 屏逐项对应：
 * - 标题「春节 · 回家的日子」，目标日 2027.02.06；
 * - 冻结 today=2026-09-11，距目标日 148 天（与设计稿「148 天后开启」一致）；
 * - 经验格 16 格填 11 = 69%（PROGRESS 69%）；
 * - LORE 文案、前置目标两行（一条 CLEAR、一条 TODO）、GROUP=家庭、RARITY=★★☆。
 */
object MomentDetailVisualFixture {

    val momentId: String = "fixture-quest-detail"

    val moment: Moment = Moment(
        id = momentId,
        title = "春节 · 回家的日子",
        note = "农历新年的团圆时刻。回家前需完成前置任务：期末考试、实习答辩、抢到车票。",
        creationDirection = MomentDirection.FUTURE_COUNTDOWN,
        anchorDate = LocalDate.of(2027, 2, 6),
        groupId = "家庭",
        rarity = 2,
        isPinned = true,
        pinnedOrder = 0,
        deletedAt = null,
        createdAt = Instant.parse("2026-09-11T00:00:00Z").toEpochMilli(),
        updatedAt = 0L,
    )

    /** 与设计稿「148 天后开启」对应的冻结自然日。 */
    val today: LocalDate = LocalDate.of(2026, 9, 11)

    val objectives: List<QuestObjectiveUi> = listOf(
        QuestObjectiveUi(title = "抢到回家的车票", completed = true),
        QuestObjectiveUi(title = "给家人带礼物", completed = false),
    )

    /** 16 格填 11 = 68.75% ≈ 设计稿 PROGRESS 69%。 */
    const val FILLED_XP_CELLS = 11
    const val PROGRESS_PERCENT = 69

    /** 直接喂给 [MomentDetailContent] 的冻结 UI 状态。 */
    val uiState: MomentDetailUiState = MomentDetailUiState(
        isLoading = false,
        moment = moment,
        status = EventTimeStatus.FUTURE,
        days = 148L,
        today = today,
        filledXpCells = FILLED_XP_CELLS,
        progressPercent = PROGRESS_PERCENT,
        questObjectives = objectives,
    )

    @Suppress("unused")
    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

    // ── 第 ⑦ 屏 成就详情（ACHIEVEMENT · 过去 · 昼）fixture ──────────────────

    /**
     * DEBUG 视觉回归 fixture：`adb shell setprop debug.momentmark.detail achievement`
     * 冻结首页默认成就「和甜甜在一起」的详情帧。
     *
     * 与默认首页事件（SampleMoments `sample-tiantian`）同源：
     * - 标题「和甜甜在一起 💞」，SINCE 2026.05.04 · 星期一 · 生活线；
     * - 冻结 today=2026-08-28，已 116 天；
     * - STORY 文案「西安➡️天水➡️宝鸡➡️爱情」；
     * - 里程碑 3 项：100 天（已达成）、一周年（下一项，约 32%）、1000 天（远征中）。
     *   里程碑日期由 MilestoneCalculator 从锚定日真实推算（100 天=2026.08.12，一周年=2027.05.04，
     *   1000 天=2029.01.28）。
     */
    val achievementMomentId: String = "fixture-achievement-detail"

    val achievementMoment: Moment = Moment(
        id = achievementMomentId,
        title = "和甜甜在一起 💞",
        note = "西安➡️天水➡️宝鸡➡️爱情",
        creationDirection = MomentDirection.PAST_ACHIEVEMENT,
        anchorDate = LocalDate.of(2026, 5, 4),
        groupId = "生活线",
        rarity = 3,
        isPinned = true,
        pinnedOrder = 0,
        deletedAt = null,
        createdAt = Instant.parse("2026-05-04T00:00:00Z").toEpochMilli(),
        updatedAt = 0L,
    )

    /** 与默认事件「已 116 天」对应的冻结自然日。 */
    val achievementToday: LocalDate = LocalDate.of(2026, 8, 28)

    /** 冻结的 3 项里程碑：100 天（已达成）+ 一周年（下一项）+ 1000 天（远征中）。 */
    val achievementMilestones: List<Milestone> = listOf(
        Milestone(
            label = "100 天",
            targetDate = LocalDate.of(2026, 8, 12),
            dayCount = 100L,
            status = MilestoneStatus.ACHIEVED,
        ),
        Milestone(
            label = "一周年",
            targetDate = LocalDate.of(2027, 5, 4),
            dayCount = 365L,
            status = MilestoneStatus.NEXT,
            daysRemaining = 249L,
            progressFraction = 0.32f,
        ),
        Milestone(
            label = "1000 天",
            targetDate = LocalDate.of(2029, 1, 28),
            dayCount = 1000L,
            status = MilestoneStatus.EXPEDITION,
        ),
    )

    val achievementUiState: MomentDetailUiState = MomentDetailUiState(
        isLoading = false,
        moment = achievementMoment,
        status = EventTimeStatus.PAST,
        days = 116L,
        today = achievementToday,
        milestones = achievementMilestones,
    )
}
