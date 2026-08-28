package com.cch.momentmark.ui.recyclebin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cch.momentmark.domain.model.TimeEvent
import com.cch.momentmark.ui.components.DeleteConfirmationDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecycleBinScreen(
    deletedEvents: List<TimeEvent>,
    onBack: () -> Unit,
    onRestore: (String) -> Unit,
    onPermanentlyDelete: (String) -> Unit,
    onPurge: () -> Unit,
) {
    var eventToPermanentlyDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var showPurgeConfirmation by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("回收站") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (deletedEvents.isNotEmpty()) {
                        TextButton(onClick = { showPurgeConfirmation = true }) {
                            Text("清空")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (deletedEvents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        text = "回收站是空的",
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "删除的事件会在这里保留，直到你手动清空。",
                        modifier = Modifier.padding(top = 6.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        text = "${deletedEvents.size} 个已删除事件",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    )
                }
                items(deletedEvents, key = { it.id }) { event ->
                    DeletedEventRow(
                        event = event,
                        onRestore = { onRestore(event.id) },
                        onPermanentlyDelete = { eventToPermanentlyDelete = event.id },
                    )
                }
            }
        }
    }

    val pendingEvent = deletedEvents.firstOrNull { it.id == eventToPermanentlyDelete }
    if (pendingEvent != null) {
        DeleteConfirmationDialog(
            title = "永久删除这个事件？",
            message = "删除后将无法恢复。",
            confirmLabel = "永久删除",
            onConfirm = {
                onPermanentlyDelete(pendingEvent.id)
                eventToPermanentlyDelete = null
            },
            onDismiss = { eventToPermanentlyDelete = null },
        )
    }

    if (showPurgeConfirmation) {
        DeleteConfirmationDialog(
            title = "清空回收站？",
            message = "所有已删除事件都会永久删除，且无法恢复。",
            confirmLabel = "永久删除",
            onConfirm = {
                onPurge()
                showPurgeConfirmation = false
            },
            onDismiss = { showPurgeConfirmation = false },
        )
    }
}

@Composable
private fun DeletedEventRow(
    event: TimeEvent,
    onRestore: () -> Unit,
    onPermanentlyDelete: () -> Unit,
) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
            if (event.subtitle.isNotBlank()) {
                Text(
                    text = event.subtitle,
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                )
            }
            Text(
                text = event.dateLabel.ifBlank { "未设置日期" },
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onRestore,
                    modifier = Modifier.semantics {
                        contentDescription = "恢复 ${event.title}"
                        role = Role.Button
                    },
                ) {
                    Text("恢复")
                }
                OutlinedButton(
                    onClick = onPermanentlyDelete,
                    modifier = Modifier.semantics {
                        contentDescription = "永久删除 ${event.title}"
                        role = Role.Button
                    },
                ) {
                    Text("永久删除")
                }
            }
        }
    }
}
