package com.cch.momentmark.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cch.momentmark.ui.app.MainTab
import com.cch.momentmark.ui.theme.LocalMomentMarkNight
import com.cch.momentmark.ui.theme.MomentMarkTokens

/** 选中项硬影透明度：proposal_J_final .nav .item.on 的 rgba(43,38,32,.4)。 */
private const val SelectedShadowAlpha = 0.4f

/** 加号宽度约为普通项的 1.4 倍（.nav .plus flex:1.4）。 */
private const val PlusWeight = 1.4f

/**
 * J 最终设计的固定三入口底部导航（AGENTS.md §2.1 / DESIGN_SYSTEM §4）：
 * `◉ 大事件` ｜ `＋ 新时刻`（金） ｜ `▤ 日子簿`。
 * 普通项 = 页面底色 + 3dp 墨线 + 次级文字；选中项 昼=墨底金字 / 夜=金底洞窟墨字；
 * 加号 = 金底 + 2dp 实心墨影；按压 = 位移 2dp + 影缩至 0（不用透明度）。
 */
@Composable
fun PixelBottomNav(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onNewMoment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val outline = MaterialTheme.colorScheme.outline
    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .drawBehind {
                // 顶部 3dp 墨线（.nav border-top）
                drawRect(
                    color = outline,
                    topLeft = Offset.Zero,
                    size = Size(size.width, MomentMarkTokens.PxBorderWidth.toPx()),
                )
            },
    ) {
        Row(
            Modifier
                .navigationBarsPadding()
                .padding(
                    top = MomentMarkTokens.SpaceCard,
                    start = MomentMarkTokens.SpaceInner,
                    end = MomentMarkTokens.SpaceInner,
                ),
            horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit * 2),
        ) {
            PixelNavTabItem(
                label = "◉ 大事件",
                contentDescription = "大事件",
                selected = selectedTab == MainTab.HOME,
                onClick = { onTabSelected(MainTab.HOME) },
                modifier = Modifier.weight(1f),
            )
            PixelNavPlusItem(
                label = "＋ 新时刻",
                contentDescription = "新时刻",
                onClick = onNewMoment,
                modifier = Modifier.weight(PlusWeight),
            )
            PixelNavTabItem(
                label = "▤ 日子簿",
                contentDescription = "日子簿",
                selected = selectedTab == MainTab.DAYBOOK,
                onClick = { onTabSelected(MainTab.DAYBOOK) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PixelNavTabItem(
    label: String,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val night = LocalMomentMarkNight.current
    PixelNavItem(
        label = label,
        contentDescription = contentDescription,
        selected = selected,
        containerColor = if (selected) {
            if (night) scheme.tertiary else scheme.inverseSurface
        } else {
            scheme.background
        },
        contentColor = if (selected) {
            if (night) scheme.outline else scheme.tertiary
        } else {
            scheme.onSurfaceVariant
        },
        shadowColor = if (selected) {
            scheme.outline.copy(alpha = SelectedShadowAlpha)
        } else {
            Color.Transparent
        },
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun PixelNavPlusItem(
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    PixelNavItem(
        label = label,
        contentDescription = contentDescription,
        selected = false,
        containerColor = scheme.tertiary,
        contentColor = scheme.outline,
        shadowColor = scheme.outline,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun PixelNavItem(
    label: String,
    contentDescription: String,
    selected: Boolean,
    containerColor: Color,
    contentColor: Color,
    shadowColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // 按压 = 位移 2dp + 影子缩至 0 的实体感（DESIGN_SYSTEM §3.4；不做透明度）
    val shadowOffset by animateDpAsState(
        targetValue = if (pressed) 0.dp else MomentMarkTokens.PxShadowOffsetSmall,
        animationSpec = tween(MomentMarkTokens.MotionPressMs),
        label = "nav-item-shadow",
    )
    val pressShift = MomentMarkTokens.PxShadowOffsetSmall - shadowOffset
    PixelPanel(
        modifier = modifier
            .graphicsLayer {
                translationX = pressShift.toPx()
                translationY = pressShift.toPx()
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                this.contentDescription = contentDescription
                // 明确暴露 true/false；未选中页签也可被读屏/测试辨识为非选中。
                this.selected = selected
            },
        backgroundColor = containerColor,
        shadowOffset = shadowOffset,
        shadowColor = shadowColor,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MomentMarkTokens.SpaceUnit * 2),
        )
    }
}
