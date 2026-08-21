package com.cch.momentmark.ui.home

import com.cch.momentmark.domain.model.TimeEvent

/** Search is intentionally in-memory until the Room-backed event source phase. */
fun filterEventsByTitle(events: List<TimeEvent>, query: String): List<TimeEvent> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return events
    return events.filter { it.title.contains(normalizedQuery, ignoreCase = true) }
}
