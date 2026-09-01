package com.cch.momentmark.data

import com.cch.momentmark.domain.model.Moment
import com.cch.momentmark.domain.model.MomentDirection
import java.time.LocalDate
import java.time.ZoneId

/**
 * 开发期种子时刻，镜像 proposal_J_final ① 首页排布，仅 debug 构建空库时写入。
 * 不是业务真相；仅供 debug 空库演示，release 不写入。
 */
object SampleMoments {

    private val zone = ZoneId.of("Asia/Shanghai")

    private fun millisOf(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    val all: List<Moment> = listOf(
        Moment(
            id = "sample-spring-festival",
            title = "春节 · 回家的日子",
            note = "农历新年的团圆时刻，抢到车票记得截图留念。",
            creationDirection = MomentDirection.FUTURE_COUNTDOWN,
            anchorDate = LocalDate.of(2027, 2, 6),
            groupId = "家庭",
            rarity = 2,
            isPinned = true,
            pinnedOrder = 0,
            deletedAt = null,
            createdAt = millisOf(LocalDate.of(2026, 8, 1)),
            updatedAt = 0,
        ),
        Moment(
            id = "sample-xiaoman",
            title = "和小满在一起 💞",
            note = "2023.05.20，在学校樱花树下告白成功 🌸 从这天起，每天 +1。",
            creationDirection = MomentDirection.PAST_ACHIEVEMENT,
            anchorDate = LocalDate.of(2023, 5, 20),
            groupId = "生活",
            rarity = 3,
            isPinned = true,
            pinnedOrder = 1,
            deletedAt = null,
            createdAt = millisOf(LocalDate.of(2023, 5, 20)),
            updatedAt = 0,
        ),
        Moment(
            id = "sample-kaoyan",
            title = "考研初试",
            note = "",
            creationDirection = MomentDirection.FUTURE_COUNTDOWN,
            anchorDate = LocalDate.of(2026, 12, 19),
            groupId = "学业",
            rarity = 2,
            isPinned = false,
            pinnedOrder = null,
            deletedAt = null,
            createdAt = millisOf(LocalDate.of(2026, 3, 18)),
            updatedAt = 0,
        ),
        Moment(
            id = "sample-concert",
            title = "演唱会",
            note = "",
            creationDirection = MomentDirection.FUTURE_COUNTDOWN,
            anchorDate = LocalDate.of(2026, 9, 20),
            groupId = "生活",
            rarity = 1,
            isPinned = false,
            pinnedOrder = null,
            deletedAt = null,
            createdAt = millisOf(LocalDate.of(2026, 3, 1)),
            updatedAt = 0,
        ),
        Moment(
            id = "sample-old-phone",
            title = "旧手机退役 🏆",
            note = "陪了四年的老伙计功成身退。",
            creationDirection = MomentDirection.PAST_ACHIEVEMENT,
            anchorDate = LocalDate.of(2024, 3, 2),
            groupId = "生活",
            rarity = 1,
            isPinned = false,
            pinnedOrder = null,
            deletedAt = null,
            createdAt = millisOf(LocalDate.of(2024, 3, 2)),
            updatedAt = 0,
        ),
    )
}
