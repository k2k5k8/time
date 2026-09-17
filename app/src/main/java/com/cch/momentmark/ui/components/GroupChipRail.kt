package com.cch.momentmark.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cch.momentmark.ui.theme.MomentMarkTokens

data class GroupChipItem(val id: String, val label: String, val iconKey: String? = null)

/**
 * 分组线路选择轨道：横向溢出、内容尺寸 chip，对应 design.html `.party-rail`。
 *
 * 不再使用 weight(1f) 把所有 chip 拉伸等宽；每个 chip 自适应内容宽度。
 * 「无阵营」以 includeNone 控制；当前选中的 chip 可传入高亮的 borderColor（默认 secondary）。
 */
@Composable
fun GroupChipRail(
    items: List<GroupChipItem>,
    selectedGroupId: String,
    onGroupSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    includeNone: Boolean = true,
    selectedBorderColor: Color = MaterialTheme.colorScheme.secondary,
    unselectedBorderColor: Color = MaterialTheme.colorScheme.outline,
    selectedBackgroundColor: Color = MaterialTheme.colorScheme.secondary,
    unselectedBackgroundColor: Color = MaterialTheme.colorScheme.surface,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.FormPartyRailGap),
    ) {
        if (includeNone) {
            GroupChipItemView(
                item = GroupChipItem("", "无阵营"),
                selected = selectedGroupId.isBlank(),
                onSelected = { onGroupSelected("") },
                selectedBorderColor = selectedBorderColor,
                unselectedBorderColor = unselectedBorderColor,
                selectedBackgroundColor = selectedBackgroundColor,
                unselectedBackgroundColor = unselectedBackgroundColor,
            )
        }
        items.forEach { item ->
            GroupChipItemView(
                item = item,
                selected = selectedGroupId == item.id,
                onSelected = { onGroupSelected(item.id) },
                selectedBorderColor = selectedBorderColor,
                unselectedBorderColor = unselectedBorderColor,
                selectedBackgroundColor = selectedBackgroundColor,
                unselectedBackgroundColor = unselectedBackgroundColor,
            )
        }
    }
}

@Composable
private fun GroupChipItemView(
    item: GroupChipItem,
    selected: Boolean,
    onSelected: () -> Unit,
    selectedBorderColor: Color,
    unselectedBorderColor: Color,
    selectedBackgroundColor: Color,
    unselectedBackgroundColor: Color,
) {
    val border = if (selected) selectedBorderColor else unselectedBorderColor
    val bg = if (selected) selectedBackgroundColor else unselectedBackgroundColor
    val text = if (selected) {
        when {
            selectedBackgroundColor == MaterialTheme.colorScheme.tertiary -> MaterialTheme.colorScheme.onTertiary
            selectedBackgroundColor == MaterialTheme.colorScheme.secondary -> MaterialTheme.colorScheme.onSecondary
            selectedBackgroundColor == MaterialTheme.colorScheme.error -> MaterialTheme.colorScheme.onError
            selectedBackgroundColor == MaterialTheme.colorScheme.primary -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurface
        }
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    PixelPanel(
        modifier = Modifier.semantics {
            contentDescription = "分组 ${item.label}"
            this.selected = selected
        }.clickable(role = Role.RadioButton) { onSelected() },
        backgroundColor = bg,
        borderColor = border,
        borderWidth = MomentMarkTokens.CheckboxStrokeWidth, // 对应 HTML `.party-rail > *` 的 2.5px 实边
        shadowColor = MaterialTheme.colorScheme.outline,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = MomentMarkTokens.FormChipPaddingHorizontal,
                vertical = MomentMarkTokens.FormChipPaddingVertical,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item.iconKey?.let { key ->
                Text(
                    text = resolveChipIcon(key),
                    // design.html `.party-rail > *{font-size:10px}`：emoji 与文字同为 10px。
                    style = TextStyle(
                        fontSize = 10.sp,
                        lineHeight = MomentMarkTokens.FormChipLineHeight,
                    ),
                )
            }
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp,
                    lineHeight = MomentMarkTokens.FormChipLineHeight,
                ),
                color = text,
            )
        }
    }
}

private fun resolveChipIcon(key: String): String = when (key) {
    "travel" -> "✈️"
    "work" -> "💼"
    "heart" -> "💞"
    "study" -> "🎓"
    "family" -> "🏠"
    "health" -> "🍎"
    "memory" -> "🎁"
    "inspire" -> "💡"
    else -> "✨"
}
