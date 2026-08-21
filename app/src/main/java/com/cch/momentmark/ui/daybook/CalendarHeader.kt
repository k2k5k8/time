package com.cch.momentmark.ui.daybook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cch.momentmark.R
import java.time.YearMonth

private val NotoSerifSc = FontFamily(
    Font(R.font.noto_serif_sc_vf, FontWeight.Normal),
)

/**
 * 顶部标题区域：月份 Serif 大字 + 年份装饰 + 左右圆形导航按钮。
 *
 * 不包含手机状态栏高度（由外层 [DaybookFeature] 的 statusBarsPadding 处理）。
 */
@Composable
internal fun CalendarHeader(
    visibleMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // 左侧：上一月按钮
        NavIconButton(
            icon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
            description = "上个月",
            onClick = onPreviousMonth,
        )

        // 中间：月份 + 年份
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "${visibleMonth.monthValue}月",
                fontSize = DaybookMonthFontSize,
                fontWeight = DaybookMonthFontWeight,
                color = DaybookText,
                fontFamily = NotoSerifSc,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "— ${visibleMonth.year} —",
                fontSize = DaybookYearFontSize,
                color = DaybookMuted,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
            )
        }

        // 右侧：下一月按钮
        NavIconButton(
            icon = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            description = "下个月",
            onClick = onNextMonth,
        )
    }
}

/** 64x64 白色半透明圆角按钮，内部黑色箭头。 */
@Composable
private fun NavIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(DaybookCardRadiusSmall))
            .clip(RoundedCornerShape(DaybookCardRadiusSmall))
            .background(DaybookSurfaceTranslucent)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = description
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DaybookText,
            modifier = Modifier.size(20.dp),
        )
    }
}
