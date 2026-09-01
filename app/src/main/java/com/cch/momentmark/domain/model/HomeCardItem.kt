package com.cch.momentmark.domain.model

import com.cch.momentmark.domain.time.EventTimeCalculator
import com.cch.momentmark.domain.time.EventTimeStatus
import com.cch.momentmark.domain.time.Milestone
import com.cch.momentmark.domain.time.MilestoneCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 首页卡片投影：面向 J 卡片的单一展示状态（ARCHITECTURE §5.3）。
 * 全部字段由 Moment/Task 事实 + 今天派生，不把派生结果写回 Room。
 */
data class MomentCardState(
    val id: String,
    val title: String,
    val anchorDate: LocalDate,
    val groupId: String?,
    val rarity: Int?,
    val isPinned: Boolean,
    /** 置顶手动顺序；null 排在已有顺序之后。 */
    val pinnedOrder: Int?,
    val creationDirection: MomentDirection,
    /** EventTimeCalculator.countdown 的状态：FUTURE / TODAY / PAST。 */
    val status: EventTimeStatus,
    /** 未来 = 剩余天数；过去 = 已过天数；TODAY = 0。 */
    val days: Long,
    /**
     * 未来卡血条：目标日前剩余占「创建日→目标日」总跨度的比例；null = 不显示。
     * 三段变色（>0.6 绿 / 0.3–0.6 橙 / <0.3 红）由 UI 按 token 执行。
     */
    val hpFraction: Float?,
    /** 徽章语义：置顶=主线◆紫；未来且 hpFraction<0.3=限时⧗橙；其余=支线◇蓝。 */
    val isLimited: Boolean,
    /** Current next fixed milestone for achievement cards; null for future/today. */
    val nextMilestone: Milestone? = null,
)

/** 首页右列中的独立待办展示状态；Task 仍可只在日子簿中编辑与完成。 */
data class TaskCardState(
    val id: String,
    val title: String,
    val dueLocalDate: LocalDate,
    val dueInstant: Instant?,
    val taskType: TaskType?,
    val difficulty: Int?,
    val groupId: String?,
    val isCompleted: Boolean,
    /** 所属日相对今天的天数；首页只投影大于等于 0 的 Task。 */
    val daysUntilDue: Long,
)

/**
 * 首页右列的只读混合项。Task 只改变展示位置，绝不获得 Moment 的时间语义或身份。
 */
sealed interface HomeCardItem {
    val id: String
    val date: LocalDate

    data class FutureMoment(val card: MomentCardState) : HomeCardItem {
        override val id: String = card.id
        override val date: LocalDate = card.anchorDate
    }

    data class HomeTask(val card: TaskCardState) : HomeCardItem {
        override val id: String = card.id
        override val date: LocalDate = card.dueLocalDate
    }
}

/** 首页三段布局：置顶全宽 → 过去左列 → 未来 Moment/Task 混合右列。 */
data class HomeMomentProjection(
    val pinned: List<MomentCardState>,
    val past: List<MomentCardState>,
    val future: List<MomentCardState>,
    /**
     * 右列的最终顺序。保留 [future] 供 Moment 专属测试/逻辑复用；UI 必须使用本字段。
     */
    val rightItems: List<HomeCardItem> = future.map(HomeCardItem::FutureMoment),
)

/** 纯函数投影器：排序与分列规则在此集中，便于固定 Clock 测试。 */
object HomeMomentProjector {

    fun project(
        moments: List<Moment>,
        today: LocalDate,
        zoneId: ZoneId,
        tasks: List<Task> = emptyList(),
    ): HomeMomentProjection {
        val cards = moments.map { it.toCardState(today, zoneId) }
        val (pinnedCards, normalCards) = cards.partition { it.isPinned }
        // 置顶之间按用户手动顺序（未排序者靠后）；同序时按日期由近到远稳定排列
        val pinnedSorted = pinnedCards.sortedWith(
            compareBy<MomentCardState> { it.pinnedOrder ?: Int.MAX_VALUE }
                .thenBy { it.anchorDate.toEpochDay() }
                .thenBy { it.id },
        )
        // 左列 = 过去成就（含「过去·正数」方向锚定今天的条目）；右列 = 未来倒数与目标日当天
        val (pastCards, futureCards) = normalCards.partition { card ->
            card.status == EventTimeStatus.PAST ||
                (card.status == EventTimeStatus.TODAY &&
                    card.creationDirection == MomentDirection.PAST_ACHIEVEMENT)
        }
        val futureSorted = futureCards.sortedWith(
            compareBy<MomentCardState> { it.anchorDate.toEpochDay() }
                .thenBy { it.id },
        )
        val activeHomeTasks = tasks
            .asSequence()
            .filter { task -> task.showOnHome && task.deletedAt == null && !task.dueLocalDate.isBefore(today) }
            .map { task -> task.toCardState(today) }
            .toList()
        val rightItems = (
            futureSorted.map(HomeCardItem::FutureMoment) +
                activeHomeTasks.map(HomeCardItem::HomeTask)
            ).sortedWith(
                compareBy<HomeCardItem> { it.date.toEpochDay() }
                    .thenBy { item -> if (item is HomeCardItem.FutureMoment) 0 else 1 }
                    .thenBy(HomeCardItem::id),
            )
        return HomeMomentProjection(
            pinned = pinnedSorted,
            // 过去由近到远 = 日期降序；未来由近到远 = 日期升序（AC-06）
            past = pastCards.sortedWith(
                compareByDescending<MomentCardState> { it.anchorDate.toEpochDay() }
                    .thenBy { it.id },
            ),
            future = futureSorted,
            rightItems = rightItems,
        )
    }

    private fun Moment.toCardState(today: LocalDate, zoneId: ZoneId): MomentCardState {
        val result = EventTimeCalculator.countdown(targetDate = anchorDate, today = today)
        val hpFraction = if (result.status == EventTimeStatus.FUTURE) {
            remainingFraction(today = today, zoneId = zoneId)
        } else {
            null
        }
        return MomentCardState(
            id = id,
            title = title,
            anchorDate = anchorDate,
            groupId = groupId,
            rarity = rarity,
            isPinned = isPinned,
            pinnedOrder = pinnedOrder,
            creationDirection = creationDirection,
            status = result.status,
            days = result.amount,
            hpFraction = hpFraction,
            isLimited = result.status == EventTimeStatus.FUTURE &&
                hpFraction != null && hpFraction < LIMITED_FRACTION,
            nextMilestone = if (result.status == EventTimeStatus.PAST) {
                MilestoneCalculator.calculate(anchorDate, today)
                    .firstOrNull { it.status == com.cch.momentmark.domain.time.MilestoneStatus.NEXT }
            } else null,
        )
    }

    private fun Task.toCardState(today: LocalDate): TaskCardState = TaskCardState(
        id = id,
        title = title,
        dueLocalDate = dueLocalDate,
        dueInstant = dueInstant,
        taskType = taskType,
        difficulty = difficulty,
        groupId = groupId,
        isCompleted = isCompleted,
        daysUntilDue = dueLocalDate.toEpochDay() - today.toEpochDay(),
    )

    /** 剩余跨度占比：创建日=今天 或 目标日=创建日时防零。 */
    private fun Moment.remainingFraction(today: LocalDate, zoneId: ZoneId): Float {
        val createdDay = Instant.ofEpochMilli(createdAt).atZone(zoneId).toLocalDate()
        val total = anchorDate.toEpochDay() - createdDay.toEpochDay()
        if (total <= 0L) return 1f
        val left = anchorDate.toEpochDay() - today.toEpochDay()
        return (left.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
    }

    /** DESIGN_SYSTEM：剩余 <30% 视为限时 LIMITED。 */
    const val LIMITED_FRACTION = 0.3f
}
