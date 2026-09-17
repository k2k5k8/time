package com.cch.momentmark.ui.task

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cch.momentmark.ui.components.PixelFieldLabel
import com.cch.momentmark.ui.components.PixelPanel
import com.cch.momentmark.ui.components.PixelPanelVariant
import com.cch.momentmark.ui.components.PixelStarRating
import com.cch.momentmark.ui.components.pixelBlockCursor
import com.cch.momentmark.ui.components.rememberPixelCursorVisible
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MomentMarkTokens

/**
 * 第 ④ 屏「▸ 截止日期 DUE ★」面板 + 右上「[ ] 全天」helper + 一行两白框（日期 1.4 + 时间 1）。
 *
 * 与 design.html 完全一致：`.px`（内距 10/12，border 3） + 顶行 `flex-between` label/helper
 * + 第二行 `flex` 双白框 `gap:8px` `margin-top:8px`。
 */
@Composable
internal fun DueDateTimePanel(
    dateText: String,
    timeText: String,
    onDateChanged: (String) -> Unit,
    onTimeChanged: (String) -> Unit,
) {
    PixelPanel(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MomentMarkTokens.FormPanelPaddingHorizontal,
                    vertical = MomentMarkTokens.FormPanelPaddingVertical,
                ),
            verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.FormTaskDateTimeLabelToRowGap),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PixelFieldLabel(
                    label = "▸ 截止日期 DUE ★",
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "[ ] 全天",
                    style = TextStyle(
                        fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        letterSpacing = 1.8.sp,
                    ),
                    color = LocalMmExtendedColors.current.labelTertiary,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.FormTaskDateTimeCellRowGap),
            ) {
                DueCell(
                    value = dateText,
                    onValueChange = onDateChanged,
                    modifier = Modifier.weight(MomentMarkTokens.FormTaskDateTimeCellWeightDate),
                )
                DueCell(
                    value = timeText,
                    onValueChange = onTimeChanged,
                    modifier = Modifier.weight(MomentMarkTokens.FormTaskDateTimeCellWeightTime),
                )
            }
        }
    }
}

/**
 * 单格白框——与第 ②③ 屏 `.px` 内 DatePart 同款外观，但实现为可编辑 BasicTextField。
 * 文本格式 `text` 由 ViewModel 负责（日期 `YYYY.MM.DD` / 时间 `HH:MM`）。
 */
@Composable
private fun DueCell(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    var fieldValue by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    var focused by remember { mutableStateOf(false) }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    // 与新时刻表单（PixelTextInput）共用同一组方块光标与闪烁相位：4dp × 8.3dp、1s 硬切换。
    val cursorVisible = rememberPixelCursorVisible(focused)
    LaunchedEffect(value) {
        if (value != fieldValue.text) {
            fieldValue = TextFieldValue(value, TextRange(value.length))
        }
    }
    // design.html 白框是裸 `border:3px`（无 box-shadow）——必须用 Flat 变体，
    // 否则 Raised 的 3dp 硬影会在白框右/下多出墨色带（与设计不符且挤压相邻 gap 视觉）。
    PixelPanel(modifier = modifier.fillMaxWidth(), variant = PixelPanelVariant.Flat) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // design.html 白框 `background:#fff`：与 PixelTextInput 一致使用输入内表面。
                .background(LocalMmExtendedColors.current.inputSurface)
                .padding(
                    horizontal = MomentMarkTokens.FormDateCellPaddingHorizontal,
                    vertical = MomentMarkTokens.FormDateCellPaddingVertical,
                )
                .onFocusChanged { focused = it.isFocused }
                // 方块光标画在 padding 之后，绘制原点即文本区左上角，无需偏移补偿。
                .pixelBlockCursor(
                    layoutResult = layoutResult,
                    selectionEnd = fieldValue.selection.end,
                    visible = focused && cursorVisible,
                    color = scheme.outline,
                ),
            contentAlignment = Alignment.Center,
        ) {
            BasicTextField(
                value = fieldValue,
                onValueChange = { next ->
                    fieldValue = next
                    onValueChange(next.text)
                },
                singleLine = true,
                // design.html 白框 `text-align:center`：日期与时间均居中显示。
                textStyle = TextStyle(
                    fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = MomentMarkTokens.FormTaskCellValueSize,
                    lineHeight = MomentMarkTokens.FormDateCellValueLineHeight,
                    textAlign = TextAlign.Center,
                ).copy(color = scheme.onSurface),
                // 光标由 pixelBlockCursor 手绘；不置透明会出现系统细线与方块重叠的双光标。
                cursorBrush = SolidColor(Color.Transparent),
                onTextLayout = { layoutResult = it },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * 第 ④ 屏「▸ 难度 DIFFICULTY」行：label + 等宽星串 17sp letterSpacing 3sp。
 *
 * design.html 结构：`.px{padding:10px 12px}` 内唯一子行 `margin-top:10px`（星串行高 ≈20px），
 * 实测面板总高 56dp。Compose 侧：border 补偿 padding 13 + 顶部额外 10dp + 行高 21dp。
 * 星串行触摸高度 21dp < TouchTargetMin，与 FormRarityRow 同一显式取舍。
 */
@Composable
internal fun DifficultyPanel(difficulty: Int?, onDifficultySelected: (Int?) -> Unit) {
    PixelPanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = MomentMarkTokens.FormPanelPaddingHorizontal,
                    end = MomentMarkTokens.FormPanelPaddingHorizontal,
                    top = MomentMarkTokens.FormPanelPaddingVertical +
                        MomentMarkTokens.FormTaskDifficultyLabelTopGap,
                    bottom = MomentMarkTokens.FormPanelPaddingVertical,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PixelFieldLabel(
                label = "▸ 难度 DIFFICULTY",
                modifier = Modifier.weight(1f),
            )
            PixelStarRating(
                value = difficulty,
                onValueChange = onDifficultySelected,
                progressive = false,
                minRowHeight = MomentMarkTokens.FormRarityRowHeight,
                contentDescription = { "$it 星难度" },
            )
        }
    }
}
