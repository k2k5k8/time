package com.cch.momentmark.ui.home.edit

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DragSwapGovernorTest {

    private val bounds = mapOf(
        "a" to Rect(0f, 0f, 100f, 100f),
        "b" to Rect(110f, 0f, 210f, 100f),
        "c" to Rect(0f, 110f, 100f, 210f),
    )

    @Test
    fun candidateFor_requiresEnteringTheCoreRect() {
        val governor = DragSwapGovernor()
        // Inside b's bounds but outside its central half.
        assertNull(governor.candidateFor(Offset(115f, 50f), bounds, draggedId = "a"))
        // Deep inside b's core rect.
        assertEquals("b", governor.candidateFor(Offset(160f, 50f), bounds, draggedId = "a"))
        // Never the dragged card itself.
        assertNull(governor.candidateFor(Offset(50f, 50f), bounds, draggedId = "a"))
    }

    @Test
    fun candidateFor_picksTheNearestCoreWhenTwoOverlap() {
        val overlapping = mapOf(
            "dragged" to Rect(0f, 300f, 100f, 400f),
            "near" to Rect(0f, 0f, 100f, 200f),
            "far" to Rect(0f, 100f, 100f, 300f),
        )
        val governor = DragSwapGovernor()
        // Point sits in both cores; the closer center wins.
        assertEquals("near", governor.candidateFor(Offset(50f, 140f), overlapping, draggedId = "dragged"))
    }

    @Test
    fun targetFor_needsTwoConsecutiveHits() {
        val governor = DragSwapGovernor()
        val center = Offset(160f, 50f)
        assertNull(governor.targetFor(center, bounds, "a", nowMs = 0))
        assertEquals("b", governor.targetFor(center, bounds, "a", nowMs = 16))
    }

    @Test
    fun targetFor_resetsConfirmationWhenCandidateChanges() {
        val governor = DragSwapGovernor()
        assertNull(governor.targetFor(Offset(160f, 50f), bounds, "a", nowMs = 0))
        // Move to c before confirming b: b's pending hit is discarded and c
        // needs its own two consecutive hits.
        assertNull(governor.targetFor(Offset(50f, 160f), bounds, "a", nowMs = 16))
        assertEquals("c", governor.targetFor(Offset(50f, 160f), bounds, "a", nowMs = 32))
    }

    @Test
    fun targetFor_enforcesTheTimeDebounce() {
        val governor = DragSwapGovernor()
        assertNull(governor.targetFor(Offset(160f, 50f), bounds, "a", nowMs = 0))
        assertEquals("b", governor.targetFor(Offset(160f, 50f), bounds, "a", nowMs = 10))
        // Leave b's bounds so hysteresis releases, then hit c's core twice
        // within the debounce window: rejected.
        assertNull(governor.targetFor(Offset(50f, 160f), bounds, "a", nowMs = 20))
        assertNull(governor.targetFor(Offset(50f, 160f), bounds, "a", nowMs = 30))
        assertNull(governor.targetFor(Offset(50f, 160f), bounds, "a", nowMs = 40))
        // Past the 50ms interval: accepted.
        assertEquals("c", governor.targetFor(Offset(50f, 160f), bounds, "a", nowMs = 70))
    }

    @Test
    fun targetFor_hysteresisBlocksRetriggerWhileStillOverTheOldBounds() {
        val governor = DragSwapGovernor()
        val bCore = Offset(160f, 50f)
        assertNull(governor.targetFor(bCore, bounds, "a", nowMs = 0))
        assertEquals("b", governor.targetFor(bCore, bounds, "a", nowMs = 100))
        // Still inside b's old bounds: nothing can trigger, not even c's core
        // reaching into b's rect is required — the point simply stays blocked.
        assertNull(governor.targetFor(bCore, bounds, "a", nowMs = 200))
        assertNull(governor.targetFor(bCore, bounds, "a", nowMs = 300))
    }

    @Test
    fun targetFor_releasesHysteresisAfterLeavingTheBlockedRect() {
        val governor = DragSwapGovernor()
        assertNull(governor.targetFor(Offset(160f, 50f), bounds, "a", nowMs = 0))
        assertEquals("b", governor.targetFor(Offset(160f, 50f), bounds, "a", nowMs = 100))
        // Move fully away from b's bounds and settle onto c's core…
        assertNull(governor.targetFor(Offset(50f, 160f), bounds, "a", nowMs = 120))
        assertEquals("c", governor.targetFor(Offset(50f, 160f), bounds, "a", nowMs = 160))
        // …then back onto b: swapping back is allowed again.
        assertNull(governor.targetFor(Offset(160f, 50f), bounds, "a", nowMs = 200))
        assertEquals("b", governor.targetFor(Offset(160f, 50f), bounds, "a", nowMs = 240))
    }

    @Test
    fun targetFor_ignoresNullDraggedId() {
        val governor = DragSwapGovernor()
        assertNull(governor.targetFor(Offset(160f, 50f), bounds, draggedId = null, nowMs = 0))
    }
}

class DragMotionTrackerTest {

    @Test
    fun velocity_averagesSamplesWithinTheWindow() {
        val tracker = DragMotionTracker()
        tracker.begin()
        tracker.addSample(0, Offset.Zero)
        tracker.addSample(50, Offset(100f, 0f))
        assertEquals(2f, tracker.velocity().x, 0.001f)
    }

    @Test
    fun velocity_isZeroWithoutEnoughHistory() {
        val tracker = DragMotionTracker()
        tracker.begin()
        assertEquals(Offset.Zero, tracker.velocity())
        tracker.addSample(0, Offset.Zero)
        assertEquals(Offset.Zero, tracker.velocity())
    }

    @Test
    fun isFling_requiresSpeedAboveTheThreshold() {
        val tracker = DragMotionTracker()
        tracker.begin()
        tracker.addSample(0, Offset.Zero)
        tracker.addSample(50, Offset(30f, 0f)) // 0.6 px/ms
        assertFalse(tracker.isFling())
        tracker.addSample(100, Offset(180f, 0f)) // 1.8 px/ms
        assertTrue(tracker.isFling())
    }
}

class BoardUndoStackTest {

    @Test
    fun record_thenUndo_returnsTheSnapshotBeforeTheChange() {
        val stack = BoardUndoStack<String>()
        stack.record("before")
        assertTrue(stack.canUndo)
        assertEquals("before", stack.undo("after"))
        assertTrue(stack.canRedo)
        assertFalse(stack.canUndo)
    }

    @Test
    fun redo_restoresTheUndoneState() {
        val stack = BoardUndoStack<String>()
        stack.record("v1")
        val restored = stack.undo("v2")
        assertEquals("v1", restored)
        assertEquals("v2", stack.redo(restored!!))
        assertFalse(stack.canRedo)
    }

    @Test
    fun record_clearsTheRedoStack() {
        val stack = BoardUndoStack<String>()
        stack.record("v1")
        stack.undo("v2")
        stack.record("v3")
        assertFalse(stack.canRedo)
    }

    @Test
    fun undo_onAnEmptyStackReturnsNull() {
        val stack = BoardUndoStack<String>()
        assertNull(stack.undo("current"))
    }
}
