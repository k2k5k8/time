package com.cch.momentmark.data.repository

import com.cch.momentmark.data.local.MomentDao
import com.cch.momentmark.data.local.MomentEntity
import com.cch.momentmark.data.repository.MomentMapper.toDomain
import com.cch.momentmark.data.repository.MomentMapper.toEntity
import com.cch.momentmark.domain.model.Moment
import com.cch.momentmark.domain.model.MomentDirection
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

/** 时刻仓储端口：首页/表单/详情 ViewModel 依赖此接口，测试用假实现。 */
interface MomentRepositoryPort {
    fun observeActive(): Flow<List<Moment>>
    fun observeDeleted(): Flow<List<Moment>> = emptyFlow()
    suspend fun findById(id: String): Moment?
    suspend fun save(moment: Moment)
    suspend fun seedIfEmpty(moments: List<Moment>)
    suspend fun setPinned(id: String, pinned: Boolean)
    /** 按用户给出的完整顺序持久化置顶排序（AC-05a）。 */
    suspend fun reorderPinned(orderedIds: List<String>)
    suspend fun softDelete(id: String) = Unit
    suspend fun restore(id: String) = Unit
    suspend fun permanentlyDelete(id: String) = Unit
    suspend fun purgeDeletedBefore(cutoffMillis: Long) = Unit
    suspend fun purgeAllDeleted() = Unit
}

class MomentRepository(
    private val dao: MomentDao,
    private val clock: Clock,
) : MomentRepositoryPort {

    override fun observeActive(): Flow<List<Moment>> =
        dao.observeActive().map { entities -> entities.map { it.toDomain() } }

    override fun observeDeleted(): Flow<List<Moment>> =
        dao.observeDeleted().map { entities -> entities.map { it.toDomain() } }

    override suspend fun findById(id: String): Moment? = dao.findById(id)?.toDomain()

    /**
     * 内容编辑只改事实字段：createdAt、置顶状态与顺序、deletedAt
     * 必须沿用已存行，避免 REPLACE 静默改变生命周期字段。
     */
    override suspend fun save(moment: Moment) {
        val now = clock.millis()
        val previous = dao.findById(moment.id)
        dao.upsert(moment.toEntity(nowMillis = now, previous = previous))
    }

    override suspend fun seedIfEmpty(moments: List<Moment>) {
        if (dao.countAll() == 0 && moments.isNotEmpty()) {
            val now = clock.millis()
            dao.upsertAll(
                moments.map { moment ->
                    // 种子条目可能携带自定的 createdAt（供血条跨度演示）；缺省用当前时间
                    if (moment.createdAt > 0L) {
                        moment.toEntity(nowMillis = now)
                    } else {
                        moment.copy(createdAt = now).toEntity(nowMillis = now)
                    }
                },
            )
        }
    }

    /** 置顶追加到现有置顶序列末尾；取消置顶清空顺序。 */
    override suspend fun setPinned(id: String, pinned: Boolean) {
        val now = clock.millis()
        if (!pinned) {
            dao.setPinned(id, pinned = false, pinnedOrder = null, updatedAt = now)
            return
        }
        val nextOrder = (dao.maxPinnedOrder() ?: -1) + 1
        dao.setPinned(id, pinned = true, pinnedOrder = nextOrder, updatedAt = now)
    }

    override suspend fun reorderPinned(orderedIds: List<String>) {
        val now = clock.millis()
        orderedIds.forEachIndexed { index, id ->
            dao.setPinnedOrder(id, pinnedOrder = index, updatedAt = now)
        }
    }

    override suspend fun softDelete(id: String) {
        val now = clock.millis()
        dao.softDelete(id, deletedAt = now, updatedAt = now)
    }

    override suspend fun restore(id: String) {
        dao.restore(id, updatedAt = clock.millis())
    }

    override suspend fun permanentlyDelete(id: String) {
        dao.permanentlyDelete(id)
    }

    override suspend fun purgeDeletedBefore(cutoffMillis: Long) {
        dao.purgeDeletedBefore(cutoffMillis)
    }

    override suspend fun purgeAllDeleted() {
        dao.purgeAllDeleted()
    }
}

/** Entity ↔ Domain 映射：日期用 ISO 文本，方向用枚举名，未知值直接失败。 */
object MomentMapper {

    fun Moment.toEntity(
        nowMillis: Long,
        previous: MomentEntity? = null,
    ): MomentEntity = MomentEntity(
        id = id,
        title = title,
        note = note,
        creationDirection = creationDirection.name,
        anchorDateIso = anchorDate.toString(),
        groupId = groupId,
        rarity = rarity,
        isPinned = isPinned,
        pinnedOrder = if (isPinned) (pinnedOrder ?: previous?.pinnedOrder) else null,
        deletedAt = deletedAt ?: previous?.deletedAt,
        createdAt = if (createdAt > 0L) createdAt else (previous?.createdAt ?: nowMillis),
        updatedAt = nowMillis,
    )

    fun MomentEntity.toDomain(): Moment = Moment(
        id = id,
        title = title,
        note = note,
        creationDirection = requireNotNull(
            MomentDirection.entries.firstOrNull { it.name == creationDirection },
        ) { "Unknown creationDirection: $creationDirection" },
        anchorDate = LocalDate.parse(anchorDateIso),
        groupId = groupId,
        rarity = rarity,
        isPinned = isPinned,
        pinnedOrder = pinnedOrder,
        deletedAt = deletedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
