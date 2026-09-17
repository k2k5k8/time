package com.cch.momentmark.ui.recyclebin

import com.cch.momentmark.domain.model.RecycleBinItem
import com.cch.momentmark.domain.model.RecycleBinItemType
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * J ⑩ 封印之地视觉回归 fixture（design.html 第⑩屏 GRAVEYARD · 昼）。
 *
 * 3 条封印记录：
 *   1. 🔗 演唱会（任务）— 封存于 3 天前，剩余 27 天
 *   2. 🔗 实习答辩（任务）— 封存于 12 天前，剩余 18 天
 *   3. 🔗 旧手机退役 🏆（成就）— 封存于 21 天前，剩余 9 天
 *
 * 固定时钟到 2026-09-16T00:00:00Z，deletedAt 据此回推，保证截图中天数与设计稿一致。
 */
object RecycleBinVisualFixture {
    val clock: Clock = Clock.fixed(Instant.parse("2026-09-16T00:00:00Z"), ZoneOffset.UTC)

    val items: List<RecycleBinItem> = listOf(
        RecycleBinItem(
            type = RecycleBinItemType.TASK,
            id = "fixture-task-concert",
            title = "演唱会",
            deletedAt = Instant.parse("2026-09-13T00:00:00Z"), // 3 天前
        ),
        RecycleBinItem(
            type = RecycleBinItemType.TASK,
            id = "fixture-task-internship",
            title = "实习答辩",
            deletedAt = Instant.parse("2026-09-04T00:00:00Z"), // 12 天前
        ),
        RecycleBinItem(
            type = RecycleBinItemType.MOMENT,
            id = "fixture-moment-old-phone",
            title = "旧手机退役 🏆",
            deletedAt = Instant.parse("2026-08-26T00:00:00Z"), // 21 天前
        ),
    )

    val uiState: RecycleBinUiState = RecycleBinUiState(
        items = items,
        autoPurgeEnabled = false,
        isLoading = false,
        errorMessage = null,
    )
}
