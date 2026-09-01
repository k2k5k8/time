package com.cch.momentmark.ui.system

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.cch.momentmark.ui.components.PageTitlePanel
import com.cch.momentmark.ui.components.PixelPanel
import com.cch.momentmark.ui.components.PixelTextInput
import com.cch.momentmark.ui.components.PixelConfirmationDialog
import com.cch.momentmark.ui.recyclebin.RecycleBinUiState
import com.cch.momentmark.ui.theme.MomentMarkTokens
import com.cch.momentmark.data.repository.Group
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/** J ⑪ 的轻量系统设置入口；只承载 P0 回收站与自动净化设置。 */
@Composable
fun SystemSettingsScreen(
    recycleBinState: RecycleBinUiState,
    onBack: () -> Unit,
    onOpenRecycleBin: () -> Unit,
    onAutoPurgeChanged: (Boolean) -> Unit,
    groupState: GroupUiState = GroupUiState(),
    onCreateGroup: (String) -> Unit = {},
    onRenameGroup: (String, String) -> Unit = { _, _ -> },
    onDissolveGroup: (String) -> Unit = {},
    onReorderGroups: (List<String>) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MomentMarkTokens.SpacePage, vertical = MomentMarkTokens.SpaceCard),
        verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCard),
    ) {
        PageTitlePanel("◂ 系统设置", "OPTIONS")
        SettingsRow("◂ 返回日子簿", "返回日子簿", onBack)
        PixelPanel(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth()) {
                SettingsListRow(
                    label = "🗑 封印之地（回收站）",
                    description = "打开封印之地",
                    trailing = "${recycleBinState.items.size} 件 ›",
                    onClick = onOpenRecycleBin,
                    divider = true,
                )
                SettingsListRow(
                    label = "30 天后自动净化",
                    description = "切换 30 天后自动净化",
                    trailing = if (recycleBinState.autoPurgeEnabled) "[x] ON" else "[ ] OFF",
                    onClick = { onAutoPurgeChanged(!recycleBinState.autoPurgeEnabled) },
                )
            }
        }
        GroupManagementPanel(groupState, onCreateGroup, onRenameGroup, onDissolveGroup, onReorderGroups)
        Text(
            "▸ 自动净化默认关闭。开启后，仅在下一次打开 App 时清理已封存满 30 天的条目；不会后台删除。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = MomentMarkTokens.SpaceUnit),
        )
        recycleBinState.errorMessage?.let { Text("⚠ $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun GroupManagementPanel(state: GroupUiState, onCreate: (String) -> Unit, onRename: (String, String) -> Unit, onDissolve: (String) -> Unit, onReorder: (List<String>) -> Unit) {
    var draft by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<String?>(null) }
    var editingDraft by remember { mutableStateOf("") }
    var confirmGroup by remember { mutableStateOf<Group?>(null) }
    PixelPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceInner), verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCompact)) {
            Text("▸ 队伍编成 GROUPS · ${state.groups.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.semantics { contentDescription = "队伍编成管理，当前 ${state.groups.size} 组" })
            state.groups.forEachIndexed { index, group ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("▰ ${group.name}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    if (index > 0) Text("↑", modifier = Modifier.clickable { onReorder(state.groups.map { it.id }.toMutableList().apply { add(index - 1, removeAt(index)) }) }.padding(MomentMarkTokens.SpaceCompact))
                    if (index < state.groups.lastIndex) Text("↓", modifier = Modifier.clickable { onReorder(state.groups.map { it.id }.toMutableList().apply { add(index + 1, removeAt(index)) }) }.padding(MomentMarkTokens.SpaceCompact))
                    Text("✎", style = MaterialTheme.typography.labelMedium, modifier = Modifier.semantics { contentDescription = "改名 ${group.name}" }.clickable { editingId = group.id; editingDraft = group.name }.padding(MomentMarkTokens.SpaceCompact))
                    Text("✕", style = MaterialTheme.typography.labelMedium, modifier = Modifier.semantics { contentDescription = "解散 ${group.name}" }.clickable { confirmGroup = group }.padding(MomentMarkTokens.SpaceCompact))
                }
                if (editingId == group.id) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCompact)) {
                        PixelTextInput("", editingDraft, "分组名称", "改名输入", { editingDraft = it }, Modifier.weight(1f), singleLine = true)
                        PixelPanel(Modifier.clickable { onRename(group.id, editingDraft); editingId = null }) { Text("保存", modifier = Modifier.padding(MomentMarkTokens.SpaceCompact)) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCompact), verticalAlignment = Alignment.CenterVertically) {
                PixelTextInput(label = "", value = draft, placeholder = "新建分组", contentDescription = "新建分组名称", onValueChange = { draft = it }, singleLine = true, modifier = Modifier.weight(1f))
                PixelPanel(Modifier.weight(0.5f).clickable { onCreate(draft); draft = "" }) { Text("＋ 新建", modifier = Modifier.padding(MomentMarkTokens.SpaceCompact)) }
            }
            state.errorMessage?.let { Text("⚠ $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
        }
    }
    confirmGroup?.let { group ->
        PixelConfirmationDialog(
            title = "解散「${group.name}」？",
            message = "只解除条目归属，Moment/Task 不会删除。",
            confirmLabel = "确认解散",
            onConfirm = { onDissolve(group.id); confirmGroup = null },
            onDismiss = { confirmGroup = null },
        )
    }
}

@Composable
private fun SettingsRow(
    label: String,
    description: String,
    onClick: () -> Unit,
    trailing: String? = null,
) {
    PixelPanel(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick),
        shadowColor = MaterialTheme.colorScheme.outline,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceInner),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            trailing?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary) }
        }
    }
}

@Composable
private fun SettingsListRow(
    label: String,
    description: String,
    trailing: String,
    onClick: () -> Unit,
    divider: Boolean = false,
) {
    val separatorColor = MaterialTheme.colorScheme.outlineVariant
    Row(
        Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = MomentMarkTokens.SpaceInner, vertical = MomentMarkTokens.SpaceCompact)
            .drawBehind {
                if (divider) {
                    drawLine(
                        color = separatorColor,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = MomentMarkTokens.PxThinBorderWidth.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(MomentMarkTokens.SpaceUnit.toPx(), MomentMarkTokens.SpaceUnit.toPx()),
                        ),
                    )
                }
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(trailing, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
    }
}
