package com.cch.momentmark.ui.home.edit

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * Decides when the dragged card should swap places with another card during a
 * board edit session (see 首页卡片拖拽布局编辑系统设计方案 §四).
 *
 * A swap is only accepted when the drag center enters the *core rect* (the
 * central half) of another card — the "overlap > 50%" rule — and three
 * anti-thrash guards pass:
 *
 *  1. confirmation: the same candidate must be hit on two consecutive probes
 *     (≈ two frames) before it counts;
 *  2. time debounce: accepted swaps are at least [minIntervalMs] apart;
 *  3. hysteresis: after a swap with card T, no new swap is accepted until the
 *     drag center has left T's (old) bounds, so a card resting over the spot
 *     it just claimed cannot re-trigger itself.
 */
internal class DragSwapGovernor(
    private val minIntervalMs: Long = 50,
) {
    private var pendingTargetId: String? = null
    private var pendingHits: Int = 0
    private var lastAcceptedAtMs: Long? = null
    private var blockedRect: Rect? = null

    fun begin() {
        pendingTargetId = null
        pendingHits = 0
        lastAcceptedAtMs = null
        blockedRect = null
    }

    /**
     * The card currently under the drag center's core rect, regardless of the
     * debounce guards. Used for the "displaced card" highlight only.
     */
    fun candidateFor(
        dragCenter: Offset,
        bounds: Map<String, Rect>,
        draggedId: String?,
    ): String? = bounds.entries
        .asSequence()
        .filter { it.key != draggedId }
        .filter { coreRectOf(it.value).contains(dragCenter) }
        .minByOrNull { distanceSquared(it.value.center, dragCenter) }
        ?.key

    /** Returns the card id to swap with now, or null when no swap should happen. */
    fun targetFor(
        dragCenter: Offset,
        bounds: Map<String, Rect>,
        draggedId: String?,
        nowMs: Long,
    ): String? {
        if (draggedId == null) return null

        val blocked = blockedRect
        if (blocked != null) {
            if (blocked.contains(dragCenter)) return null
            blockedRect = null
        }

        val candidate = candidateFor(dragCenter, bounds, draggedId) ?: run {
            pendingTargetId = null
            pendingHits = 0
            return null
        }
        if (candidate == pendingTargetId) {
            pendingHits += 1
        } else {
            pendingTargetId = candidate
            pendingHits = 1
        }
        if (pendingHits < ConfirmationHits) return null
        val lastAccepted = lastAcceptedAtMs
        if (lastAccepted != null && nowMs - lastAccepted < minIntervalMs) return null

        lastAcceptedAtMs = nowMs
        blockedRect = bounds[candidate]
        pendingTargetId = null
        pendingHits = 0
        return candidate
    }

    private companion object {
        const val ConfirmationHits = 2

        /** Central half of [rect]: inset by a quarter of the size per side. */
        fun coreRectOf(rect: Rect): Rect {
            val insetX = rect.width / 4f
            val insetY = rect.height / 4f
            return Rect(
                left = rect.left + insetX,
                top = rect.top + insetY,
                right = rect.right - insetX,
                bottom = rect.bottom - insetY,
            )
        }

        fun distanceSquared(a: Offset, b: Offset): Float {
            val dx = a.x - b.x
            val dy = a.y - b.y
            return dx * dx + dy * dy
        }
    }
}

/**
 * Lightweight velocity estimator for the drop moment. Keeps the last
 * [windowMs] of drag-offset samples and derives px/ms, so a fast fling can
 * land the card where the finger was heading instead of where it stopped
 * (see 设计方案 §7.5).
 */
internal class DragMotionTracker(
    private val windowMs: Long = 100,
    private val maxSamples: Int = 16,
) {
    private var samples: ArrayDeque<Sample> = ArrayDeque()

    fun begin() {
        samples = ArrayDeque()
    }

    fun addSample(timeMs: Long, offset: Offset) {
        samples.addLast(Sample(timeMs, offset))
        while (samples.size > maxSamples) samples.removeFirst()
        while (samples.size > 1 && timeMs - samples.first().timeMs > windowMs) {
            samples.removeFirst()
        }
    }

    /** Average velocity in px per ms over the sample window. */
    fun velocity(): Offset {
        val first = samples.firstOrNull() ?: return Offset.Zero
        val last = samples.lastOrNull() ?: return Offset.Zero
        val dt = last.timeMs - first.timeMs
        if (dt <= 0) return Offset.Zero
        return Offset(
            x = (last.offset.x - first.offset.x) / dt,
            y = (last.offset.y - first.offset.y) / dt,
        )
    }

    /** True when the release deserves fling extrapolation. */
    fun isFling(thresholdPxPerMs: Float = 1.2f): Boolean =
        velocity().getDistance() > thresholdPxPerMs

    private data class Sample(val timeMs: Long, val offset: Offset)
}

/**
 * Undo/redo history for the board layout with one entry per committed drop
 * (see 设计方案 §9). [record] snapshots the state *before* a change; the
 * current state is passed into [undo]/[redo] so it can be pushed onto the
 * opposite stack.
 */
internal class BoardUndoStack<T>(private val limit: Int = 50) {
    private val undoStack = ArrayDeque<T>()
    private val redoStack = ArrayDeque<T>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun record(state: T) {
        undoStack.addLast(state)
        if (undoStack.size > limit) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo(current: T): T? {
        val previous = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(current)
        return previous
    }

    fun redo(current: T): T? {
        val next = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(current)
        return next
    }
}
