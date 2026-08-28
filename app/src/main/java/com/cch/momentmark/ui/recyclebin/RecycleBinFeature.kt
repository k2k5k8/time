package com.cch.momentmark.ui.recyclebin

import androidx.compose.runtime.Composable
import com.cch.momentmark.domain.model.TimeEvent

@Composable
internal fun RecycleBinFeature(
    deletedEvents: List<TimeEvent>,
    onBack: () -> Unit,
    onRestore: (String) -> Unit,
    onPermanentlyDelete: (String) -> Unit,
    onPurge: () -> Unit,
) {
    RecycleBinScreen(
        deletedEvents = deletedEvents,
        onBack = onBack,
        onRestore = onRestore,
        onPermanentlyDelete = onPermanentlyDelete,
        onPurge = onPurge,
    )
}
