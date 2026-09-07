package com.cch.momentmark.domain.model

import com.cch.momentmark.domain.time.EventTimeStatus
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeMomentProjectorTest {

    private val zone = ZoneId.of("Asia/Shanghai")
    private val today = LocalDate.of(2026, 8, 28)

    private fun moment(
        id: String,
        anchorDate: LocalDate,
        direction: MomentDirection = MomentDirection.FUTURE_COUNTDOWN,
        isPinned: Boolean = false,
        pinnedOrder: Int? = null,
        createdAtDay: LocalDate = LocalDate.of(2026, 8, 1),
        groupId: String? = null,
    ): Moment {
        val createdAt = createdAtDay.atStartOfDay(zone).toInstant().toEpochMilli()
        return Moment(
            id = id,
            title = id,
            note = "",
            creationDirection = direction,
            anchorDate = anchorDate,
            groupId = groupId,
            rarity = null,
            isPinned = isPinned,
            pinnedOrder = pinnedOrder,
            deletedAt = null,
            createdAt = createdAt,
            updatedAt = createdAt,
        )
    }

    private fun task(
        id: String,
        dueDate: LocalDate,
        showOnHome: Boolean = true,
        isCompleted: Boolean = false,
    ) = Task(
        id = id,
        title = id,
        dueLocalDate = dueDate,
        dueInstant = null,
        zoneId = null,
        note = "",
        taskType = TaskType.SIDE,
        difficulty = null,
        groupId = null,
        isCompleted = isCompleted,
        completedAt = null,
        showOnHome = showOnHome,
        deletedAt = null,
        createdAt = java.time.Instant.EPOCH,
        updatedAt = java.time.Instant.EPOCH,
    )

    @Test
    fun `置顶时刻独占最前方并按手动顺序排列`() {
        val projection = HomeMomentProjector.project(
            moments = listOf(
                moment("a", LocalDate.of(2027, 2, 6), isPinned = true, pinnedOrder = 1),
                moment("b", LocalDate.of(2023, 5, 20), isPinned = true, pinnedOrder = 0),
                moment("c", LocalDate.of(2026, 12, 19), isPinned = true, pinnedOrder = null),
            ),
            today = today,
            zoneId = zone,
        )

        assertEquals(listOf("b", "a", "c"), projection.pinned.map { it.id })
        assertTrue(projection.past.isEmpty())
        assertTrue(projection.future.isEmpty())
    }

    @Test
    fun `过去左列按日期由近到远，未来右列按日期由近到远`() {
        val projection = HomeMomentProjector.project(
            moments = listOf(
                moment("past-old", LocalDate.of(2024, 2, 29), direction = MomentDirection.PAST_ACHIEVEMENT),
                moment("past-new", LocalDate.of(2026, 1, 10), direction = MomentDirection.PAST_ACHIEVEMENT),
                moment("past-mid", LocalDate.of(2025, 12, 31), direction = MomentDirection.PAST_ACHIEVEMENT),
                moment("future-far", LocalDate.of(2027, 2, 6)),
                moment("future-near", LocalDate.of(2026, 8, 30)),
                moment("future-mid", LocalDate.of(2026, 12, 31)),
            ),
            today = today,
            zoneId = zone,
        )

        assertEquals(listOf("past-new", "past-mid", "past-old"), projection.past.map { it.id })
        assertEquals(listOf("future-near", "future-mid", "future-far"), projection.future.map { it.id })
    }

    @Test
    fun `未来时刻目标日当天显示就是今天且仍在右列`() {
        val projection = HomeMomentProjector.project(
            moments = listOf(moment("today-future", today)),
            today = today,
            zoneId = zone,
        )

        val card = projection.future.single()
        assertEquals(EventTimeStatus.TODAY, card.status)
        assertEquals(0L, card.days)
        assertNull(card.hpFraction)
    }

    @Test
    fun `未来时刻目标日次日起自动转为过去成就且不生成新记录`() {
        // 同一条记录：8-28 当天在右列「就是今天」；8-29 起进入左列「已 1 天」
        val record = moment("auto-convert", LocalDate.of(2026, 8, 28))

        val todayView = HomeMomentProjector.project(listOf(record), today, zone)
        val nextDayView = HomeMomentProjector.project(
            listOf(record),
            today = LocalDate.of(2026, 8, 29),
            zoneId = zone,
        )

        assertEquals("auto-convert", todayView.future.single().id)
        val converted = nextDayView.past.single()
        assertEquals("auto-convert", converted.id)
        assertEquals(EventTimeStatus.PAST, converted.status)
        assertEquals(1L, converted.days)
        assertEquals(record.anchorDate, converted.anchorDate)
        assertTrue(nextDayView.future.isEmpty())
    }

    @Test
    fun `过去方向锚定今天进入左列并显示今天状态`() {
        val projection = HomeMomentProjector.project(
            moments = listOf(moment("past-today", today, direction = MomentDirection.PAST_ACHIEVEMENT)),
            today = today,
            zoneId = zone,
        )

        val card = projection.past.single()
        assertEquals(EventTimeStatus.TODAY, card.status)
        assertEquals(0L, card.days)
    }

    @Test
    fun `血条占比按创建日到目标日跨度计算且临近判定生效`() {
        // created 03-01 → anchor 09-20 共 203 天；today 08-28 剩 23 天 → 23/203 ≈ 0.113 < 0.3 限时
        val limited = HomeMomentProjector.project(
            moments = listOf(
                moment("limited", LocalDate.of(2026, 9, 20), createdAtDay = LocalDate.of(2026, 3, 1)),
            ),
            today = today,
            zoneId = zone,
        ).future.single()

        assertTrue(limited.isLimited)
        assertEquals(0.113f, limited.hpFraction!!, 0.01f)

        // created 08-01 → anchor 2027-02-06 共 159+31+30+31+... 天；剩余占比高，不限时
        val normal = HomeMomentProjector.project(
            moments = listOf(
                moment("normal", LocalDate.of(2027, 2, 6), createdAtDay = LocalDate.of(2026, 8, 1)),
            ),
            today = today,
            zoneId = zone,
        ).future.single()

        assertFalse(normal.isLimited)
        assertTrue(normal.hpFraction!! > 0.6f)
    }

    @Test
    fun `创建日晚于目标日时血条按满格兜底`() {
        val card = HomeMomentProjector.project(
            moments = listOf(
                moment("weird-span", LocalDate.of(2026, 9, 20), createdAtDay = LocalDate.of(2027, 3, 1)),
            ),
            today = today,
            zoneId = zone,
        ).future.single()

        assertEquals(1f, card.hpFraction)
        assertFalse(card.isLimited)
    }

    @Test
    fun `跨年与闰年的相对天数正确`() {
        val crossYear = HomeMomentProjector.project(
            moments = listOf(moment("new-year", LocalDate.of(2027, 1, 1))),
            today = LocalDate.of(2026, 12, 31),
            zoneId = zone,
        ).future.single()
        assertEquals(1L, crossYear.days)

        val leapPast = HomeMomentProjector.project(
            moments = listOf(
                moment("leap", LocalDate.of(2024, 2, 29), direction = MomentDirection.PAST_ACHIEVEMENT),
            ),
            today = LocalDate.of(2025, 3, 1),
            zoneId = zone,
        ).past.single()
        assertEquals(366L, leapPast.days)
    }

    @Test
    fun `首页右列按日期混排未来时刻与明确开启的待办`() {
        val projection = HomeMomentProjector.project(
            moments = listOf(
                moment("moment-near", LocalDate.of(2026, 8, 30)),
                moment("moment-far", LocalDate.of(2026, 9, 2)),
            ),
            today = today,
            zoneId = zone,
            tasks = listOf(
                task("task-today", today),
                task("task-same-day", LocalDate.of(2026, 8, 30)),
                task("task-hidden", LocalDate.of(2026, 8, 29), showOnHome = false),
                task("task-expired", LocalDate.of(2026, 8, 27)),
            ),
        )

        assertEquals(
            listOf("task-today", "moment-near", "task-same-day", "moment-far"),
            projection.rightItems.map { it.id },
        )
        val todayTask = projection.rightItems.first() as HomeCardItem.HomeTask
        assertEquals(0L, todayTask.card.daysUntilDue)
        assertEquals(listOf("moment-near", "moment-far"), projection.future.map { it.id })
    }

    @Test
    fun `首页待办保留完成状态但从不伪装为时刻`() {
        val projection = HomeMomentProjector.project(
            moments = emptyList(),
            today = today,
            zoneId = zone,
            tasks = listOf(task("done-task", LocalDate.of(2026, 8, 31), isCompleted = true)),
        )

        val item = projection.rightItems.single()
        assertTrue(item is HomeCardItem.HomeTask)
        assertTrue((item as HomeCardItem.HomeTask).card.isCompleted)
    }
}
