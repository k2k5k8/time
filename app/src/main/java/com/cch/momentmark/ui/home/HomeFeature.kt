package com.cch.momentmark.ui.home

import androidx.compose.runtime.Composable
import com.cch.momentmark.domain.model.EventCardTemplateKey
import com.cch.momentmark.domain.model.TimeEvent
import com.cch.momentmark.domain.model.TravelCardConfig
import com.cch.momentmark.ui.EventFilter
import com.cch.momentmark.ui.HomeScreen

/** Feature boundary for the home feed; the visual implementation stays stable during the prototype phase. */
@Composable
internal fun HomeFeature(
    events: List<TimeEvent>,
    selectedFilter: EventFilter,
    selectedGroup: String?,
    templateOverrides: Map<String, EventCardTemplateKey>,
    travelConfigOverrides: Map<String, TravelCardConfig>,
    isLayoutEditing: Boolean,
    onLayoutEditingChange: (Boolean) -> Unit,
    onOpenGroups: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCreateEvent: () -> Unit,
    onOpenDaybook: () -> Unit,
    onOpenEventSettings: (TimeEvent) -> Unit,
) {
    HomeScreen(
        events = events,
        selectedFilter = selectedFilter,
        selectedGroup = selectedGroup,
        templateOverrides = templateOverrides,
        travelConfigOverrides = travelConfigOverrides,
        isLayoutEditing = isLayoutEditing,
        onLayoutEditingChange = onLayoutEditingChange,
        onOpenGroups = onOpenGroups,
        onOpenSettings = onOpenSettings,
        onOpenCreateEvent = onOpenCreateEvent,
        onOpenDaybook = onOpenDaybook,
        onOpenEventSettings = onOpenEventSettings,
    )
}
