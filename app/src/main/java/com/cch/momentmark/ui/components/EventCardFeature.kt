package com.cch.momentmark.ui.components

import androidx.compose.runtime.Composable
import com.cch.momentmark.domain.model.TimeEvent
import com.cch.momentmark.ui.EventCard

/** Card rendering boundary; all four existing visual families remain unchanged. */
@Composable
fun EventCardFeature(
    event: TimeEvent,
    onClick: (() -> Unit)? = null,
) {
    EventCard(event = event, onClick = onClick)
}
