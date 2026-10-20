package com.cch.momentmark.ui.task

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.cch.momentmark.ui.theme.MomentMarkTokens
import kotlinx.coroutines.flow.first

/** Page-owned viewport; reveal only the new heading, never request input focus. */
internal class TaskFormRevealViewport(val scroll: ScrollState) {
    var bounds by mutableStateOf(Rect.Zero)
    var imeVisible by mutableStateOf(false)
    suspend fun reveal(top: Float, headingHeight: Float, animate: Boolean) {
        if (imeVisible || scroll.isScrollInProgress || bounds.height <= 0 || top < bounds.bottom) return
        val delta = (top + headingHeight - bounds.bottom).toInt().coerceAtLeast(0)
        val target = (scroll.value + delta).coerceAtMost(scroll.maxValue)
        if (animate) scroll.animateScrollTo(target, tween(MomentMarkTokens.MotionStepMs)) else scroll.scrollTo(target)
    }
}
internal val LocalTaskFormRevealViewport = staticCompositionLocalOf<TaskFormRevealViewport?> { null }

/** Observe the same system animator setting as existing task feedback, including live changes. */
@Composable
private fun formMotionEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    fun enabled() = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) >= 1f
    var motion by remember(resolver) { mutableStateOf(enabled()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { motion = enabled() }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return motion
}

/** Existing/edit-restored sections start settled; subsequent targets interrupt one transition. */
@Composable
internal fun TaskFormReveal(visible: Boolean, tag: String, headingBottom: Float? = null, content: @Composable ColumnScope.() -> Unit) {
    val transition = remember { MutableTransitionState(visible) }
    var previousVisible by remember { mutableStateOf(visible) }
    val viewport = LocalTaskFormRevealViewport.current
    val motion = formMotionEnabled()
    val latestMotion by rememberUpdatedState(motion)
    val latestHeadingBottom by rememberUpdatedState(headingBottom)
    val duration = if (motion) MomentMarkTokens.MotionStepMs else 0
    val headingHeight = with(LocalDensity.current) { 32.sp.toPx() }
    var top by remember { mutableFloatStateOf(0f) }
    transition.targetState = visible
    LaunchedEffect(visible) {
        val newlyVisible = visible && !previousVisible
        previousVisible = visible
        if (newlyVisible) {
            snapshotFlow { transition.isIdle && transition.currentState }.first { it }
            withFrameNanos { }
            val height = latestHeadingBottom?.takeIf { it > top }?.let { it - top } ?: headingHeight
            viewport?.reveal(top, height, latestMotion)
        }
    }
    AnimatedVisibility(
        modifier = Modifier.testTag(tag).then(if (visible) Modifier else Modifier
            .clearAndSetSemantics { }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                }
            }),
        visibleState = transition,
        enter = expandVertically(tween(duration), expandFrom = Alignment.Top) + fadeIn(tween(duration)),
        exit = shrinkVertically(tween(duration), shrinkTowards = Alignment.Top) + fadeOut(tween(duration)),
    ) {
        // Hard shadows draw outside button bounds. Reserve their edges inside the animated clip.
        Column(
            Modifier.fillMaxWidth().onGloballyPositioned { top = it.positionInRoot().y }
                .padding(end = MomentMarkTokens.PxShadowOffset, bottom = MomentMarkTokens.PxShadowOffset),
            verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCard),
            content = content,
        )
    }
}
