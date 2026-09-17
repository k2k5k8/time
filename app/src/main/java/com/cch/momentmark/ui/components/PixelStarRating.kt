package com.cch.momentmark.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.cch.momentmark.ui.theme.MomentMarkTokens

/**
 * Shared 1..N star control for Moment rarity and Task difficulty.
 *
 * @param minRowHeight 行盒最小高度。默认 null = 按 progressive 规则取值
 *   （递进星串 21dp / 等宽星串 44dp 触摸目标）。第 ④ 屏「难度 DIFFICULTY」设计稿行高仅 21dp，
 *   传 [MomentMarkTokens.FormRarityRowHeight] 可避免 44dp 触摸目标把整行撑高 ——
 *   与 `FormRarityRow` 相同的显式取舍（触摸高度 < 44dp），已记录在 `组件改造.md`。
 */
@Composable
fun PixelStarRating(
    value: Int?,
    onValueChange: ((Int?) -> Unit)? = null,
    modifier: Modifier = Modifier,
    max: Int = 3,
    /**
     * design.html `.rarity-stars`：名词形态的递进星串（13/17/21px 基线对齐、gap 2px）。
     * 为 false 时使用等宽星串（任务难度等场景）。
     */
    progressive: Boolean = false,
    minRowHeight: Dp? = null,
    contentDescription: (Int) -> String = { "$it 星" },
) {
    require(max > 0) { "max must be positive" }
    val selectedValue = value?.coerceIn(0, max)
    // 递进星串严格对齐 HTML 行高 21px；等宽星串默认保留 44dp 可访问触摸目标，
    // 但允许调用方按设计稿行高显式收紧（第 ④ 屏难度行 = 21dp）。
    val controlMinHeight = minRowHeight ?: if (progressive) {
        MomentMarkTokens.FormRarityRowHeight
    } else {
        MomentMarkTokens.TouchTargetMin
    }
    Box(modifier = modifier.defaultMinSize(minHeight = controlMinHeight)) {
        // J 稿使用内联星串；透明的 44dp 语义层覆盖在上方，保留可访问触摸目标。
        if (progressive) {
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.FormRarityStarGap),
            ) {
                repeat(max) { index ->
                    val size = MomentMarkTokens.FormRarityStarSizes.getOrElse(index) {
                        MomentMarkTokens.FormRarityStarSizes.last()
                    }
                    val filled = selectedValue != null && index < selectedValue
                    Text(
                        text = "★",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                            fontSize = size,
                            // HTML `line-height:1`：避免递进字号撑高行盒。
                            lineHeight = size,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = if (filled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        } else {
            Text(
                text = buildAnnotatedString {
                    repeat(max) { index ->
                        withStyle(SpanStyle(
                            color = if (selectedValue != null && index < selectedValue) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant,
                            fontWeight = FontWeight.Bold,
                        )) { append("★") }
                    }
                },
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                    fontSize = 17.sp,
                    letterSpacing = 3.sp,
                ),
                // 触摸层（3×44dp 透明格子）会把 Box 撑宽到 132dp；星串必须右对齐到
                // Box 右缘（= 面板内容右缘），否则会被顶到左侧 —— design.html 星串右对齐。
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
        Row(modifier = Modifier.fillMaxHeight()) {
            (1..max).forEach { option ->
                val selected = selectedValue == option
                val label = contentDescription(option)
                val optionModifier = Modifier
                    .width(MomentMarkTokens.TouchTargetMin)
                    .defaultMinSize(minHeight = controlMinHeight)
                    .semantics {
                        this.contentDescription = label
                        this.selected = selected
                        if (selected) stateDescription = "已选择"
                    }
                val clickableModifier = onValueChange?.let { callback ->
                    optionModifier.clickable(role = Role.RadioButton) { callback(if (selected) null else option) }
                } ?: optionModifier
                Box(clickableModifier)
            }
        }
    }
}
