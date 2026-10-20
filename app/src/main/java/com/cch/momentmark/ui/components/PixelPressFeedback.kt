package com.cch.momentmark.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.cch.momentmark.ui.theme.MomentMarkTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Visual state only. Foundation still owns activation, scrolling and accessibility. */
internal class PixelPressFeedback(private val scope: CoroutineScope) {
    var pressed by mutableStateOf(false)
        private set
    private var startedAt = 0L
    private var releaseJob: Job? = null

    fun begin(time: Long) {
        releaseJob?.cancel()
        startedAt = time
        pressed = true
    }

    fun end(time: Long, cancelled: Boolean) {
        releaseJob?.cancel()
        val remaining = if (cancelled) 0L else
            (MomentMarkTokens.MotionPressMs - (time - startedAt)).coerceAtLeast(0L)
        if (remaining == 0L) pressed = false
        else releaseJob = scope.launch {
            delay(remaining)
            pressed = false
        }
    }

    fun cancel() {
        releaseJob?.cancel()
        pressed = false
    }
}

@Composable
internal fun rememberPixelPressFeedback(enabled: Boolean): PixelPressFeedback {
    val scope = rememberCoroutineScope()
    val feedback = remember(scope) { PixelPressFeedback(scope) }
    DisposableEffect(feedback, enabled) {
        if (!enabled) feedback.cancel()
        onDispose { feedback.cancel() }
    }
    return feedback
}

/** Observe down before the scroll container's delayed PressInteraction, without consuming input. */
internal fun Modifier.pixelPressFeedback(feedback: PixelPressFeedback, enabled: Boolean): Modifier =
    pointerInput(feedback, enabled) {
        if (!enabled) return@pointerInput
        try {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                feedback.begin(down.uptimeMillis)
                awaitPointerEvent(PointerEventPass.Final)
                while (true) {
                    val initial = awaitPointerEvent(PointerEventPass.Initial)
                    // A synthetic cancellation arrives with its up already consumed. A normal
                    // release is consumed later by clickable during Main, so inspect Initial.
                    val cancelledRelease = initial.changes.firstOrNull { it.id == down.id }
                        ?.let { !it.pressed && it.isConsumed } == true
                    val event = awaitPointerEvent(PointerEventPass.Final)
                    val change = event.changes.firstOrNull { it.id == down.id }
                    if (change == null) {
                        feedback.cancel()
                        break
                    }
                    val outside = change.position.x < 0 || change.position.y < 0 ||
                        change.position.x > size.width || change.position.y > size.height
                    val scrollConsumed = change.isConsumed && change.position != change.previousPosition
                    if (outside || scrollConsumed) {
                        feedback.end(change.uptimeMillis, cancelled = true)
                        break
                    }
                    if (!change.pressed) {
                        feedback.end(change.uptimeMillis, cancelled = cancelledRelease)
                        break
                    }
                }
            }
        } finally {
            feedback.cancel()
        }
    }
