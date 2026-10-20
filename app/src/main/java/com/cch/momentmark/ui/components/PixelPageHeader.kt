package com.cch.momentmark.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MomentMarkTokens

/** J 页面标题骨架：可选返回方块、标题和 mono HUD。 */
@Composable
fun PixelPageHeader(
    title: String,
    hud: String? = null,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backContentDescription: String = "返回",
    backTouchTarget: Dp = MomentMarkTokens.PageTitleBackSize,
    compact: Boolean = false,
    /** 紧凑页头的 HUD 紧随标题；空间不足时与标题左缘对齐。默认保留尾部布局。 */
    hudFollowsTitle: Boolean = false,
) {
    PixelPanel(modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = if (compact) MomentMarkTokens.TaskHeaderMinHeight else MomentMarkTokens.PageTitleMinHeight)
                .padding(
                    horizontal = if (compact) MomentMarkTokens.TaskHeaderPaddingHorizontal else MomentMarkTokens.PageTitlePaddingHorizontal,
                    vertical = if (compact) MomentMarkTokens.TaskHeaderPaddingVertical else MomentMarkTokens.PageTitlePaddingVertical,
                ),
            horizontalArrangement = Arrangement.spacedBy(if (compact) MomentMarkTokens.SpaceUnit else MomentMarkTokens.PageTitleBackGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            onBack?.let { back ->
                if (compact) PixelButton(
                    label = backContentDescription,
                    onClick = back,
                    variant = PixelButtonVariant.Ghost,
                    layout = PixelButtonLayout.Icon,
                    icon = PixelButtonIcon.Back,
                    modifier = Modifier.size(backTouchTarget).clearAndSetSemantics {
                        contentDescription = backContentDescription
                        role = Role.Button
                        onClick(backContentDescription) { back(); true }
                    },
                ) else {
                    val backInteraction = remember { MutableInteractionSource() }
                    // design.html `.back{width:25px;height:25px;margin-right:12px;border:3px;
                    // box-shadow:2px 2px 0;background:var(--bg);font-size:18px;line-height:1}`
                    // fillContent 必须为 true：HTML 的 border 包住整块 25px，而 Compose 的
                    // Modifier.border 只包住内容高度（`‹` 仅 7dp），否则边框会塌成 7dp 高、
                    // 硬影却仍按 25dp 画，方块下方出现一段错位的"悬空阴影"。
                    Box(
                        modifier = Modifier
                            .size(backTouchTarget)
                            .semantics {
                                contentDescription = backContentDescription
                                role = Role.Button
                            }
                            .clickable(interactionSource = backInteraction, indication = null, role = Role.Button, onClick = back),
                        contentAlignment = Alignment.Center,
                    ) {
                        PixelPanel(
                            modifier = Modifier.size(MomentMarkTokens.PageTitleBackSize),
                            backgroundColor = MaterialTheme.colorScheme.background,
                            borderColor = MaterialTheme.colorScheme.outline,
                            shadowOffset = MomentMarkTokens.PxShadowOffsetSmall,
                            shadowColor = MaterialTheme.colorScheme.outline,
                            fillContent = true,
                            pressInteractionSource = backInteraction,
                        ) {
                            PixelBackChevron(
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .offset(y = MomentMarkTokens.PageTitleBackChevronOffsetY),
                            )
                        }
                    }
                }
            }
            if (compact && hud != null) {
                val titleStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, lineHeight = 18.sp)
                val dateStyle = MaterialTheme.typography.labelSmall
                val measurer = androidx.compose.ui.text.rememberTextMeasurer()
                androidx.compose.foundation.layout.BoxWithConstraints(Modifier.weight(1f)) {
                    val required = measurer.measure(title, titleStyle).size.width + measurer.measure(hud, dateStyle).size.width + with(androidx.compose.ui.platform.LocalDensity.current) { MomentMarkTokens.SpaceInner.toPx() }
                    @Composable fun date(modifier: Modifier = Modifier) {
                        Text(hud, modifier.semantics { contentDescription = "计划日 $hud" }, style = dateStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (required <= constraints.maxWidth) Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = if (hudFollowsTitle) Arrangement.spacedBy(MomentMarkTokens.SpaceInner) else Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(title, Modifier.semantics { heading() }, style = titleStyle, color = MaterialTheme.colorScheme.onSurface)
                        date()
                    } else androidx.compose.foundation.layout.Column(
                        Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(if (hudFollowsTitle) MomentMarkTokens.SpaceUnit else 0.dp),
                    ) {
                        Text(title, Modifier.semantics { heading() }, style = titleStyle, color = MaterialTheme.colorScheme.onSurface)
                        date(Modifier.align(if (hudFollowsTitle) Alignment.Start else Alignment.End))
                    }
                }
            } else {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).then(if (compact) Modifier.semantics { heading() } else Modifier),
            )
            }
            if (!compact && hud != null) Text(
                text = hud,
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 0.72.sp,
                ),
                color = LocalMmExtendedColors.current.labelTertiary,
            )
        }
    }
}

/**
 * `.back` 方块内的左尖括号，按 design.html 实测字形自绘。
 *
 * 为什么不直接用 `Text("‹")`：设计稿是 PingFang SC 渲染的 U+2039，墨迹 5.5dp×7dp、
 * 水平笔画厚 2.5dp；Android 兜底字体（Noto Sans CJK）的同名字形只有约 3dp 宽，
 * 且放大字号会同时把高度拉过 7dp，无法只补宽度。自绘多边形可以同时锁死宽/高/笔画厚度。
 *
 * 多边形顶点取自 `design-geometry-*.json` 中 `.back` 的逐行墨迹**几何边界**
 * （外接框 11 × 14 @2x 设备像素，即 5.5dp × 7dp）：
 * 内边界（左下）(5,0) → (0,7) → (5,14)；外边界（右上）(11,14) → (6,7) → (11,0)。
 */
@Composable
private fun PixelBackChevron(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val width = MomentMarkTokens.PageTitleBackChevronWidth
    val height = MomentMarkTokens.PageTitleBackChevronHeight
    Canvas(modifier.size(width = width, height = height)) {
        val w = size.width
        val h = size.height
        // 外接框 11 × 14（@2x px）→ 等比映射到 canvas 尺寸。
        fun px(v: Float) = v / 11f * w
        fun py(v: Float) = v / 14f * h
        val path = Path().apply {
            moveTo(px(5f), py(0f))
            lineTo(px(0f), py(7f))
            lineTo(px(5f), py(14f))
            lineTo(px(11f), py(14f))
            lineTo(px(6f), py(7f))
            lineTo(px(11f), py(0f))
            close()
        }
        drawPath(path = path, color = color)
    }
}
