package com.cch.momentmark.domain.model

import java.time.Instant

/** 封印之地的跨实体只读投影；事实仍各自存于 Moment / Task 表。 */
data class RecycleBinItem(
    val type: RecycleBinItemType,
    val id: String,
    val title: String,
    val deletedAt: Instant,
)

enum class RecycleBinItemType(val label: String) {
    MOMENT("时刻"),
    TASK("任务"),
}

fun Moment.toRecycleBinItem(): RecycleBinItem = RecycleBinItem(
    type = RecycleBinItemType.MOMENT,
    id = id,
    title = title,
    deletedAt = requireNotNull(deletedAt) { "Only sealed moments belong in the recycle bin." }.let(Instant::ofEpochMilli),
)

fun Task.toRecycleBinItem(): RecycleBinItem = RecycleBinItem(
    type = RecycleBinItemType.TASK,
    id = id,
    title = title,
    deletedAt = requireNotNull(deletedAt) { "Only sealed tasks belong in the recycle bin." },
)
