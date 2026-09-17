package com.cch.momentmark.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.cch.momentmark.ui.theme.MomentMarkTokens

/**
 * iconKey → 内联 emoji 的统一映射（分组 chip、地图筛选抽屉行等文字流内使用；
 * 卡片右上角固定图标槽仍走 [PixelIconSlot] 的盒子版本）。
 */
fun pixelGroupEmoji(iconKey: String?): String = when (iconKey) {
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

/**
 * 分组名 → iconKey：与首页小卡 `groupIconKey()` 同一名称映射，供抽屉行
 * 解析「未被 Room 分组管理覆盖、但被 Moment 引用」的组名。
 */
fun homeGroupIconKeyForName(groupId: String?): String = when (groupId) {
    "旅行" -> "travel"
    "工作" -> "work"
    "生活", "家庭" -> "heart"
    "学业" -> "study"
    else -> "default"
}

/**
 * 首页卡片右上角 / 分组芯片的图标槽。
 * iconKey 由 Group 映射为真实 emoji（design.html `.home-card-icon` 直接内联 emoji），
 * 图标不求与设计稿逐一对齐，但必须能渲染彩色 emoji 而不是黑白符号。
 * 缺省使用通用宝箱 emoji。
 */
@Composable
fun PixelIconSlot(
    iconKey: String?,
    modifier: Modifier = Modifier,
    decorative: Boolean = true,
    iconColor: Color = MaterialTheme.colorScheme.tertiary,
    showBackground: Boolean = true,
    boxSize: Dp = MomentMarkTokens.CompactIconSize,
    iconFontSize: TextUnit = 17.sp,
) {
    val icon = when (iconKey) {
        "travel" -> "✈️"
        "work" -> "💼"
        "heart" -> "💞"
        "study" -> "🎓"
        else -> "✨"
    }
    val base = if (showBackground) Modifier.background(MaterialTheme.colorScheme.surfaceVariant) else Modifier
    Box(
        modifier.size(boxSize).then(base)
            .then(if (decorative) Modifier else Modifier.semantics { contentDescription = "分组图标 $iconKey" }),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = icon,
            style = TextStyle(fontSize = iconFontSize, lineHeight = iconFontSize),
            color = iconColor,
        )
    }
}
