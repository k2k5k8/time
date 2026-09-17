package com.cch.momentmark.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MomentMarkTokens

/**
 * J 页面顶部 mono HUD 行（design.html `.statusbar{padding:11px 18px 5px}`）。
 * 左右两端各一条 mono 弱注释；纯展示、无交互，装饰性内容不进入无障碍语义树。
 */
@Composable
fun PixelStatusHud(
    leftText: String,
    rightText: String,
    modifier: Modifier = Modifier,
    contentColor: Color = LocalMmExtendedColors.current.labelTertiary,
) {
    Row(
        modifier
            .fillMaxWidth()
            // 显式锁高 29dp：左侧 ♥ 字形的兜底字体行盒比 mono 高 2dp，只靠 padding 会让整页下移。
            .height(MomentMarkTokens.HomeStatusHudHeight)
            .padding(
                start = MomentMarkTokens.HomeStatusHudPadding,
                end = MomentMarkTokens.HomeStatusHudPadding,
                top = MomentMarkTokens.HomeStatusHudPaddingTop,
                bottom = MomentMarkTokens.HomeStatusHudPaddingBottom,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = leftText,
            style = MomentMarkTokens.HomeStatusHudStyle,
            color = contentColor,
        )
        Text(
            text = rightText,
            style = MomentMarkTokens.HomeStatusHudStyle,
            color = contentColor,
        )
    }
}
