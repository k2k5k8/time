package com.cch.momentmark.ui.eventsettings

import androidx.compose.runtime.Composable
import com.cch.momentmark.domain.model.EventCardTemplateKey
import com.cch.momentmark.domain.model.TimeEvent
import com.cch.momentmark.domain.model.TravelCardConfig
import com.cch.momentmark.domain.model.TravelCardSize
import com.cch.momentmark.ui.EventSettingsScreen

fun eventTemplateLabel(template: EventCardTemplateKey): String = when (template) {
    EventCardTemplateKey.CLASSIC -> "经典蓝白 / 橙白"
    EventCardTemplateKey.TRAVEL_MINIMAL_EDITORIAL -> "奶油极简编辑式"
    EventCardTemplateKey.TRAVEL_SUNSET_GLASS -> "黄昏玻璃风景"
    EventCardTemplateKey.TRAVEL_SCRAPBOOK,
    EventCardTemplateKey.TRAVEL_COUNTDOWN -> "旅行手账贴纸风"
}

fun eventSizeLabel(size: TravelCardSize?): String = when (size) {
    TravelCardSize.SMALL -> "Small · 普通网格槽位"
    TravelCardSize.WIDE -> "Wide · 跨两列"
    null -> "经典卡片尺寸"
}

/** Feature boundary for editing the in-memory prototype event appearance. */
@Composable
fun EventSettingsFeature(
    event: TimeEvent,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onSubtitleChange: (String) -> Unit,
    onGroupLabelChange: (String) -> Unit,
    onTemplateChange: (EventCardTemplateKey) -> Unit,
    onTravelConfigChange: (TravelCardConfig) -> Unit,
    onTogglePinned: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    EventSettingsScreen(
        event = event,
        onBack = onBack,
        onTitleChange = onTitleChange,
        onSubtitleChange = onSubtitleChange,
        onGroupLabelChange = onGroupLabelChange,
        onTemplateChange = onTemplateChange,
        onTravelConfigChange = onTravelConfigChange,
        onTogglePinned = onTogglePinned,
        onArchive = onArchive,
        onDelete = onDelete,
    )
}
