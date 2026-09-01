package com.cch.momentmark.ui.recyclebin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.cch.momentmark.domain.model.RecycleBinItem
import com.cch.momentmark.ui.components.PageTitlePanel
import com.cch.momentmark.ui.components.PixelConfirmationDialog
import com.cch.momentmark.ui.components.PixelPanel
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MomentMarkTokens
import java.time.Clock
import java.time.Duration

private val RecycleDays = Duration.ofDays(30)

/** J ⑫ 夜间封印之地：只展示已软删除条目，复活与净化均由 VM 落库。 */
@Composable
fun RecycleBinScreen(
    uiState: RecycleBinUiState,
    clock: Clock,
    onBack: () -> Unit,
    onRestore: (RecycleBinItem) -> Unit,
    onPermanentlyDelete: (RecycleBinItem) -> Unit,
    onPurgeAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDelete by remember { mutableStateOf<RecycleBinItem?>(null) }
    var confirmPurgeAll by remember { mutableStateOf(false) }
    val extended = LocalMmExtendedColors.current
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MomentMarkTokens.SpacePage, vertical = MomentMarkTokens.SpaceCard),
        verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCard),
    ) {
        PageTitlePanel("◂ 封印之地", "${uiState.items.size} 件封存 · 30 天后净化")
        PixelActionRow("◂ 返回系统设置", "返回系统设置", onBack)
        when {
            uiState.isLoading -> Text("▸ LOADING…", style = MaterialTheme.typography.labelSmall, color = extended.labelTertiary)
            uiState.items.isEmpty() -> PixelPanel(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "封印之地暂时空无一物。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(MomentMarkTokens.SpaceInner),
                )
            }
            else -> {
                uiState.items.forEach { item ->
                    RecycleBinRow(
                        item = item,
                        clock = clock,
                        onRestore = { onRestore(item) },
                        onPermanentlyDelete = { pendingDelete = item },
                    )
                }
                PixelActionRow(
                    label = "⚔ 全部净化（清空回收站）",
                    description = "清空回收站",
                    onClick = { confirmPurgeAll = true },
                    danger = true,
                )
            }
        }
        uiState.errorMessage?.let { Text("⚠ $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) }
    }
    pendingDelete?.let { item ->
        ConfirmDialog(
            title = "永久净化「${item.title}」？",
            message = "净化后将永远消失，无法复活。",
            confirmLabel = "确认净化",
            onConfirm = { onPermanentlyDelete(item); pendingDelete = null },
            onDismiss = { pendingDelete = null },
        )
    }
    if (confirmPurgeAll) {
        ConfirmDialog(
            title = "净化全部封存物？",
            message = "回收站中的时刻与任务将永远消失，无法复活。",
            confirmLabel = "全部净化",
            onConfirm = { onPurgeAll(); confirmPurgeAll = false },
            onDismiss = { confirmPurgeAll = false },
        )
    }
}

@Composable
private fun RecycleBinRow(
    item: RecycleBinItem,
    clock: Clock,
    onRestore: () -> Unit,
    onPermanentlyDelete: () -> Unit,
) {
    val remainingDays = (RecycleDays.minus(Duration.between(item.deletedAt, clock.instant())).toDays()).coerceAtLeast(0)
    PixelPanel(modifier = Modifier.fillMaxWidth(), shadowColor = MaterialTheme.colorScheme.outline) {
        Column(
            Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceInner),
            verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit * 2),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("🔗 ${item.title}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "${item.type.label} · 已封印",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalMmExtendedColors.current.labelTertiary,
                )
            }
            Text(
                "封存中 · 剩余 $remainingDays 天后永久净化",
                style = MaterialTheme.typography.labelSmall,
                color = LocalMmExtendedColors.current.labelTertiary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCard)) {
                PixelActionRow("✚ 复活", "复活 ${item.title}", onRestore, Modifier.weight(1f), emphasize = true)
                PixelActionRow("永久净化", "永久净化 ${item.title}", onPermanentlyDelete, danger = true, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PixelActionRow(
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false,
    danger: Boolean = false,
) {
    val color = when {
        danger -> MaterialTheme.colorScheme.onError
        emphasize -> MaterialTheme.colorScheme.onTertiary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val background = when {
        danger -> MaterialTheme.colorScheme.error
        emphasize -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.surface
    }
    PixelPanel(
        modifier = modifier
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick),
        backgroundColor = background,
        borderColor = MaterialTheme.colorScheme.outline,
        shadowColor = MaterialTheme.colorScheme.outline,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            modifier = Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceInner),
        )
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    PixelConfirmationDialog(
        title = title,
        message = message,
        confirmLabel = confirmLabel,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
