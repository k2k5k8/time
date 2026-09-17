package com.cch.momentmark.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.sp
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MomentMarkTokens

/**
 * P0 统一像素输入框：字段标签、内白框、像素选择色和阶梯闪烁方块光标。
 * 使用 TextFieldValue 保留原生选择、粘贴和 IME 行为；外部仍只接收文本事实值。
 */
@Composable
fun PixelTextInput(
    label: String,
    value: String,
    placeholder: String,
    contentDescription: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    /** design.html 字段行右侧的 mono 提示（如 `选填 · 将显示在详情页`）。 */
    helperLabel: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val extended = LocalMmExtendedColors.current
    var fieldValue by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    var focused by remember { mutableStateOf(false) }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    // 闪烁相位与方块光标由 PixelCursor 统一提供，避免各表单各写一套（与 DueCell 共用）。
    val cursorVisible = rememberPixelCursorVisible(focused)

    LaunchedEffect(value) {
        if (value != fieldValue.text) {
            fieldValue = TextFieldValue(value, TextRange(value.length))
        }
    }

    val borderColor = when {
        isError -> scheme.error
        // HTML .notebox 始终使用 3px panel-line；焦点由方块光标表达，
        // 不切换成 Material 的浅色 focus/outlineVariant。
        else -> scheme.outline
    }
    val selectionColors = TextSelectionColors(
        handleColor = scheme.outline,
        backgroundColor = scheme.secondary.copy(alpha = MomentMarkTokens.AlphaDisabled),
    )
    // design.html：NAME 为 `.mono{font-size:14px;font-weight:700}`；LORE 为
    // `.notebox{font-size:12px;font-weight:600;line-height:1.65;min-height:58px}`。
    val fieldTextStyle = if (singleLine) {
        MaterialTheme.typography.bodyLarge.copy(
            fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = MomentMarkTokens.FormInputTextSize,
            lineHeight = MomentMarkTokens.FormInputLineHeight,
        )
    } else {
        MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.Medium,
            fontSize = MomentMarkTokens.FormNoteTextSize,
            lineHeight = MomentMarkTokens.FormNoteLineHeight,
        )
    }
    val fieldHeightModifier = if (singleLine) {
        // 单行白框不设下限，由 8+10 padding 与 14sp 文字自然撑高（HTML 无 min-height）。
        Modifier
    } else {
        Modifier.heightIn(min = MomentMarkTokens.FormNoteMinHeight)
    }
    PixelPanel(
        modifier = modifier.fillMaxWidth(),
        // HTML 表单字段本身也是 .px：外框右下保留实体黑影，内层再放白色输入框。
        variant = PixelPanelVariant.Raised,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                // design.html `.px{padding:10px 12px}`。
                .padding(
                    horizontal = MomentMarkTokens.FormPanelPaddingHorizontal,
                    vertical = MomentMarkTokens.FormPanelPaddingVertical,
                ),
        ) {
            // label 行：左 fieldlabel、右可选 mono 提示（HTML `justify-content:space-between`）。
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isError) scheme.error else extended.labelTertiary,
                    modifier = Modifier.weight(1f),
                )
                helperLabel?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 8.sp,
                            lineHeight = 10.sp,
                        ),
                        color = extended.labelTertiary,
                    )
                }
            }
            CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = MomentMarkTokens.FormLabelToInputGap)
                        .then(fieldHeightModifier)
                        .background(extended.inputSurface)
                        .border(MomentMarkTokens.PxBorderWidth, borderColor)
                        .onFocusChanged { focused = it.isFocused }
                        // 方块光标画在父 Box 上（BasicTextField 自身 cursorBrush 已置透明），
                        // 内白框的 padding 由 offset 补回。
                        .pixelBlockCursor(
                            layoutResult = layoutResult,
                            selectionEnd = fieldValue.selection.end,
                            visible = focused && cursorVisible,
                            color = scheme.outline,
                            offsetX = MomentMarkTokens.FormInputPaddingHorizontal,
                            offsetY = MomentMarkTokens.FormInputPaddingVertical,
                        ),
                ) {
                    BasicTextField(
                        value = fieldValue,
                        onValueChange = { next ->
                            fieldValue = next
                            onValueChange(next.text)
                        },
                        singleLine = singleLine,
                        textStyle = fieldTextStyle.copy(color = scheme.onSurface),
                        cursorBrush = SolidColor(androidx.compose.ui.graphics.Color.Transparent),
                        onTextLayout = { layoutResult = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = MomentMarkTokens.FormInputPaddingHorizontal,
                                vertical = MomentMarkTokens.FormInputPaddingVertical,
                            )
                            .semantics { this.contentDescription = contentDescription },
                        decorationBox = { innerTextField ->
                            if (fieldValue.text.isEmpty() && !focused) {
                                Text(
                                    text = placeholder,
                                    style = fieldTextStyle.copy(
                                        fontWeight = FontWeight.Normal,
                                    ),
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                            innerTextField()
                        },
                    )
                }
            }
            errorMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.error,
                    modifier = Modifier.padding(top = MomentMarkTokens.SpaceUnit),
                )
            }
        }
    }
}
