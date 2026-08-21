package com.cch.momentmark.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.cch.momentmark.domain.model.EventTimeType
import com.cch.momentmark.domain.model.TimeCardFields
import com.cch.momentmark.domain.model.TimeEvent
import com.cch.momentmark.domain.model.cardFields
import com.cch.momentmark.domain.time.CountdownResult
import com.cch.momentmark.domain.time.EventTimeCalculator
import java.time.Clock
import java.time.ZoneId
import kotlinx.coroutines.isActive

/**
 * 模板唯一的展示数据入口：统一内容字段与实时倒计时必须从同一事件事实派生。
 */
data class TimeCardPresentation(
    val fields: TimeCardFields,
    val countdown: CountdownResult,
)

@Composable
fun rememberTimeCardPresentation(event: TimeEvent): TimeCardPresentation {
    val countdown = rememberEventCountdown(event)
    val unit = event.travelCardConfig?.countdownUnit?.takeIf { it.isNotBlank() } ?: "天"
    return TimeCardPresentation(
        fields = event.cardFields().copy(
            countdownLabel = "${countdown.statusLabel} ${countdown.amount} $unit",
        ),
        countdown = countdown,
    )
}

/**
 * The only Compose-side time refresh loop. Cards receive a derived domain
 * result and never calculate relative time locally.
 */
@Composable
fun rememberEventCountdown(event: TimeEvent): CountdownResult {
    val clock = remember { Clock.systemDefaultZone() }
    val zoneId = remember(event.zoneId) {
        event.zoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
    }
    val localDate = event.localDate ?: event.travelCardConfig?.targetDate
    val targetInstant = event.targetInstant

    return produceState(
        initialValue = calculate(event, localDate, targetInstant, clock, zoneId),
        key1 = event.id,
        key2 = localDate,
        key3 = targetInstant,
    ) {
        val refreshMillis = if (event.timeType == EventTimeType.TIMED) 1_000L else 60_000L
        while (isActive) {
            value = calculate(event, localDate, targetInstant, clock, zoneId)
            kotlinx.coroutines.delay(refreshMillis)
        }
    }.value
}

private fun calculate(
    event: TimeEvent,
    localDate: java.time.LocalDate?,
    targetInstant: java.time.Instant?,
    clock: Clock,
    zoneId: ZoneId,
): CountdownResult = EventTimeCalculator.calculate(
    timeType = event.timeType,
    localDate = localDate,
    targetInstant = targetInstant,
    repeatRule = event.repeatRule.takeIf { event.isRepeat },
    clock = clock,
    zoneId = zoneId,
)
