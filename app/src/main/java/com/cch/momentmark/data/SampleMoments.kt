package com.cch.momentmark.data

import com.cch.momentmark.domain.model.Moment
import com.cch.momentmark.domain.model.MomentDirection
import java.time.LocalDate
import java.time.ZoneId

/**
 * 首页默认事件：新安装首次启动即注入的初始内容（镜像 design.html 第①屏首页排布），
 * 同时兼作 DEBUG 视觉回归 fixture（`debug.momentmark.seed samples` + 冻结时钟 2026-08-28）。
 *
 * 注入仅发生一次：首页 ViewModel 在 DataStore 一次性标记 `default_moments_seeded`
 * 未置位且数据库为空时才写入，随后落标记。此后这些事件完全等同用户自己创建的数据，
 * 可自由编辑标题/故事/日期、置顶排序、封印删除；删除（含全部净化）后不会在下次启动复活，
 * 也不会被每次启动的清理逻辑抹掉。
 * 这些 id 使用保留的 `sample-` 前缀命名空间：仅首次注入前做一次性清理，
 * 用于清除历史开发期残留的旧示例行（用户数据 id 为 UUID，不会命中）。
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
            id = "sample-tiantian",
            title = "和甜甜在一起 💞",
            note = "西安➡️天水➡️宝鸡➡️爱情",
            creationDirection = MomentDirection.PAST_ACHIEVEMENT,
            anchorDate = LocalDate.of(2026, 5, 4),
            groupId = "生活",
            rarity = 3,
            isPinned = true,
            pinnedOrder = 1,
            deletedAt = null,
            createdAt = millisOf(LocalDate.of(2026, 5, 4)),
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
            id = "sample-paper-opening",
            title = "论文开题",
            note = "",
            creationDirection = MomentDirection.PAST_ACHIEVEMENT,
            anchorDate = LocalDate.of(2026, 8, 15),
            groupId = "学业",
            rarity = 1,
            isPinned = false,
            pinnedOrder = null,
            deletedAt = null,
            createdAt = millisOf(LocalDate.of(2026, 8, 15)),
            updatedAt = 0,
        ),
        Moment(
            id = "sample-enrollment", title = "入学报到", note = "",
            creationDirection = MomentDirection.PAST_ACHIEVEMENT,
            anchorDate = LocalDate.of(2026, 7, 1), groupId = "学业", rarity = 2,
            isPinned = false, pinnedOrder = null, deletedAt = null,
            createdAt = millisOf(LocalDate.of(2026, 7, 1)), updatedAt = 0,
        ),
        Moment(
            id = "sample-offer", title = "拿到 offer", note = "",
            creationDirection = MomentDirection.PAST_ACHIEVEMENT,
            anchorDate = LocalDate.of(2026, 5, 8), groupId = "工作", rarity = 3,
            isPinned = false, pinnedOrder = null, deletedAt = null,
            createdAt = millisOf(LocalDate.of(2026, 5, 8)), updatedAt = 0,
        ),
        Moment(
            id = "sample-defense", title = "论文答辩", note = "",
            creationDirection = MomentDirection.FUTURE_COUNTDOWN,
            anchorDate = LocalDate.of(2026, 11, 8), groupId = "学业", rarity = 2,
            isPinned = false, pinnedOrder = null, deletedAt = null,
            createdAt = millisOf(LocalDate.of(2026, 8, 9)), updatedAt = 0,
        ),
        Moment(
            id = "sample-trip", title = "毕业旅行", note = "",
            creationDirection = MomentDirection.FUTURE_COUNTDOWN,
            anchorDate = LocalDate.of(2027, 2, 6), groupId = "旅行", rarity = 2,
            isPinned = false, pinnedOrder = null, deletedAt = null,
            createdAt = millisOf(LocalDate.of(2026, 7, 15)), updatedAt = 0,
        ),
    )
}
