package com.cch.momentmark.data.repository

import com.cch.momentmark.data.local.MomentEntity
import com.cch.momentmark.data.repository.MomentMapper.toDomain
import com.cch.momentmark.data.repository.MomentMapper.toEntity
import com.cch.momentmark.domain.model.Moment
import com.cch.momentmark.domain.model.MomentDirection
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MomentMapperTest {

    private fun sampleMoment(
        isPinned: Boolean = false,
        pinnedOrder: Int? = null,
        createdAt: Long = 100L,
    ) = Moment(
        id = "m-1",
        title = "春节 · 回家的日子",
        note = "团圆时刻",
        creationDirection = MomentDirection.FUTURE_COUNTDOWN,
        anchorDate = LocalDate.of(2027, 2, 6),
        groupId = "家庭",
        rarity = 2,
        isPinned = isPinned,
        pinnedOrder = pinnedOrder,
        deletedAt = null,
        createdAt = createdAt,
        updatedAt = 0L,
    )

    @Test
    fun `领域模型与实体字段完整往返`() {
        val entity = sampleMoment(isPinned = true, pinnedOrder = 3).toEntity(nowMillis = 999L)

        assertEquals("FUTURE_COUNTDOWN", entity.creationDirection)
        assertEquals("2027-02-06", entity.anchorDateIso)
        assertEquals(999L, entity.updatedAt)

        val restored = entity.toDomain()
        assertEquals(sampleMoment(isPinned = true, pinnedOrder = 3).copy(updatedAt = 999L), restored)
    }

    @Test
    fun `编辑保存沿用已存的创建时间与置顶顺序`() {
        val previous = MomentEntity(
            id = "m-1",
            title = "旧标题",
            note = "",
            creationDirection = "PAST_ACHIEVEMENT",
            anchorDateIso = "2023-05-20",
            groupId = null,
            rarity = null,
            isPinned = true,
            pinnedOrder = 7,
            deletedAt = null,
            createdAt = 111L,
            updatedAt = 112L,
        )
        // 编辑后的领域对象缺失生命周期字段（updatedAt=0、createdAt=0、未置顶）
        val edited = sampleMoment().copy(
            title = "新标题",
            isPinned = false,
            pinnedOrder = null,
            createdAt = 0L,
        )

        val saved = edited.toEntity(nowMillis = 500L, previous = previous)

        assertEquals("新标题", saved.title)
        assertEquals(111L, saved.createdAt)          // 沿用已存创建时间
        assertEquals(500L, saved.updatedAt)
        assertEquals(false, saved.isPinned)
        assertNull(saved.pinnedOrder)                  // 取消置顶清空顺序
    }

    @Test
    fun `未知方向值直接失败而非静默猜测`() {
        val broken = MomentEntity(
            id = "bad",
            title = "t",
            note = "",
            creationDirection = "SOMEDAY",
            anchorDateIso = "2026-08-28",
            groupId = null,
            rarity = null,
            isPinned = false,
            pinnedOrder = null,
            deletedAt = null,
            createdAt = 0L,
            updatedAt = 0L,
        )

        val error = runCatching { broken.toDomain() }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }
}
