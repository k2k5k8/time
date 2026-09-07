package com.cch.momentmark.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cch.momentmark.ui.theme.MomentMarkTokens

/**
 * P0 封印/净化确认弹窗。
 *
 * 不使用 Material 默认圆角 Dialog：所有实际删除路径共享直角像素面板、硬偏移影和
 * DESIGN_SYSTEM 指定的危险/取消文案，避免不同页面出现两套确认体验。
 */
@Composable
fun PixelConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = "继续冒险",
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = MomentMarkTokens.AlphaScrim))
                .padding(MomentMarkTokens.SpaceSection),
            contentAlignment = Alignment.Center,
        ) {
            PixelPanel(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = MaterialTheme.colorScheme.outline,
                shadowOffset = MomentMarkTokens.PxShadowOffsetLarge,
                shadowColor = MaterialTheme.colorScheme.outline,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MomentMarkTokens.SpaceInner),
                    verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCard),
                ) {
                    Text(
                        text = "⚠",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCard),
                    ) {
                        PixelDialogAction(
                            label = dismissLabel,
                            description = dismissLabel,
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                        )
                        PixelDialogAction(
                            label = confirmLabel,
                            description = confirmLabel,
                            onClick = onConfirm,
                            danger = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PixelDialogAction(
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
) {
    val background = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surface
    val foreground = if (danger) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurface
    val border = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
    PixelPanel(
        modifier = modifier
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick),
        backgroundColor = background,
        borderColor = border,
        shadowColor = border,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = foreground,
            modifier = Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceInner),
        )
    }
}
