package com.cch.momentmark.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MomentMarkTokens
import kotlinx.coroutines.delay

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
) {
    val scheme = MaterialTheme.colorScheme
    val extended = LocalMmExtendedColors.current
    var fieldValue by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    var focused by remember { mutableStateOf(false) }
    var cursorVisible by remember { mutableStateOf(true) }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    LaunchedEffect(value) {
        if (value != fieldValue.text) {
            fieldValue = TextFieldValue(value, TextRange(value.length))
        }
    }
    LaunchedEffect(focused) {
        if (!focused) {
            cursorVisible = false
            return@LaunchedEffect
        }
        while (true) {
            cursorVisible = true
            delay(MomentMarkTokens.MotionStepMs.toLong() * 2)
            cursorVisible = false
            delay(MomentMarkTokens.MotionStepMs.toLong() * 2)
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
    PixelPanel(
        modifier = modifier.fillMaxWidth(),
        // HTML 表单字段本身也是 .px：外框右下保留实体黑影，内层再放白色输入框。
        variant = PixelPanelVariant.Raised,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(MomentMarkTokens.SpaceCompact),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isError) scheme.error else extended.labelTertiary,
            )
            CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = MomentMarkTokens.InputMinHeight)
                        .background(extended.inputSurface)
                        .border(MomentMarkTokens.PxBorderWidth, borderColor)
                        .onFocusChanged { focused = it.isFocused }
                        .drawWithContent {
                            drawContent()
                            // 输入回调与文本布局会在相邻帧更新；以当前布局长度钳制偏移，
                            // 避免新 selection 对旧 layout 取 cursorRect 越界。
                            val cursor = layoutResult?.let { result ->
                                result.getCursorRect(
                                    fieldValue.selection.end.coerceIn(0, result.layoutInput.text.length),
                                )
                            }
                            if (focused && cursorVisible && cursor != null) {
                                drawRect(
                                    color = scheme.outline,
                                    topLeft = Offset(
                                        cursor.left + MomentMarkTokens.SpaceInput.toPx(),
                                        cursor.top + MomentMarkTokens.SpaceInput.toPx(),
                                    ),
                                    size = Size(
                                        MomentMarkTokens.CursorWidth.toPx(),
                                        cursor.height.coerceAtLeast(MomentMarkTokens.CursorWidth.toPx()),
                                    ),
                                )
                            }
                        },
                ) {
                    BasicTextField(
                        value = fieldValue,
                        onValueChange = { next ->
                            fieldValue = next
                            onValueChange(next.text)
                        },
                        singleLine = singleLine,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                        cursorBrush = SolidColor(androidx.compose.ui.graphics.Color.Transparent),
                        onTextLayout = { layoutResult = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MomentMarkTokens.SpaceInput)
                            .semantics { this.contentDescription = contentDescription },
                        decorationBox = { innerTextField ->
                            if (fieldValue.text.isEmpty() && !focused) {
                                Text(
                                    text = placeholder,
                                    style = MaterialTheme.typography.bodyLarge,
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
