package com.cch.momentmark.ui.task

import com.cch.momentmark.ui.theme.MmSemanticColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.state.ToggleableState
import com.cch.momentmark.ui.components.*

/** Task form controls share pixel feedback and one native Chinese action node. */
@Composable
internal fun TaskFormAction(
    label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    icon: PixelButtonIcon? = null, enabled: Boolean = true, selected: Boolean? = null,
    expanded: Boolean? = null, checked: Boolean? = null, field: Boolean = false,
    selectedColors: MmSemanticColor? = null,
    editing: Boolean? = null,
    shortLabel: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    // Disclosure is a visual state, not a business selection or an enabled feature.
    // Explicit choice/switch state takes priority over an accompanying disclosure.
    val sunken = if (editing != null) selected == true || editing else selected ?: checked ?: expanded
    val selectionColors = selectedColors ?: MmSemanticColor(
        scheme.secondary, scheme.secondaryContainer, scheme.onSecondaryContainer,
    )
    val suffix = when { expanded == true -> " ▾"; expanded == false || field -> " ▸"; else -> "" }
    val visibleLabel = if (shortLabel) label else if (checked == null || editing != null) label + suffix else "$label · ${if (checked) "开" else "关"}"
    PixelButton(visibleLabel, onClick, modifier.clearAndSetSemantics {
        contentDescription = label
        role = if (checked != null) Role.Switch else Role.Button
        selected?.let { this.selected = it; stateDescription = if (it) "已选择" else "未选择" }
        expanded?.let { open ->
            stateDescription = listOfNotNull(selected?.let { if (it) "已选择" else "未选择" },
                if (open) "已展开" else "已收起").joinToString("，")
        }
        checked?.let { toggleableState = if (it) ToggleableState.On else ToggleableState.Off; stateDescription = if (it) "已开启" else "已关闭" }
        editing?.let { stateDescription = "${if (selected == true) "已启用" else "未启用"}，${if (it) "正在编辑" else "已收起"}" }
        if (enabled) onClick(label) { onClick(); true } else disabled()
    }, PixelButtonVariant.Ghost, enabled, icon = icon, selected = sunken,
        selectedColors = selectionColors, layout = if (field) PixelButtonLayout.Regular else PixelButtonLayout.Compact)
}

internal fun TaskFormUiState.advancedSummary(): String = listOfNotNull(
    "重要".takeIf { important }, "有截止".takeIf { deadlineEnabled },
    (if (weekdays.size == 7) "每天" else "每周").takeIf { repeating },
    "自动顺延".takeIf { autoDefer },
).joinToString(" · ").let { if (it.isEmpty()) "更多设置" else "更多设置 · $it" }

internal fun TaskFormUiState.extrasSummary(): String = listOfNotNull(
    "有备注".takeIf { note.isNotBlank() },
    difficulty?.let { listOf("轻量", "适中", "费力")[it - 1] },
).joinToString(" · ").let { if (it.isEmpty()) "备注与难度" else "备注与难度 · $it" }
