package com.cch.momentmark.ui.daybook

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cch.momentmark.domain.model.DaybookEvent
import com.cch.momentmark.domain.model.DaybookEventSource
import com.cch.momentmark.domain.model.DaybookEventType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val recordDateFormatter = DateTimeFormatter.ofPattern("MM/dd")

/**
 * 当日记录卡片：虚线边框 + 暖白底。
 *
 * 顶部展示当天的系统节日/节气记录（橙色标记条），下方为用户自定义记录。
 * 空状态显示插画 + 引导文案；有数据时展示记录列表（日期/标题/类型/备注）。
 * 数据全部来自现有 [DaybookDataSource]。
 */
@Composable
internal fun CustomRecordCard(
    selectedDate: LocalDate,
    events: List<DaybookEvent>,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenEvent: (DaybookEvent) -> Unit = {},
) {
    val holidayRecords = events.filter { it.source == DaybookEventSource.SYSTEM }
    val userRecords = events.filter { it.source == DaybookEventSource.USER }
    val hasRecords = userRecords.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .border(
                width = 1.5.dp,
                color = DaybookDashedBorder,
                shape = RoundedCornerShape(30.dp),
            )
            .background(DaybookSurfaceTranslucent)
            .then(
                // 空状态整块承担新增动作；有记录时不响应整卡点击，避免误触。
                if (!hasRecords && holidayRecords.isEmpty()) {
                    Modifier.clickable(role = Role.Button, onClick = onAdd)
                } else {
                    Modifier
                },
            )
            .semantics {
                contentDescription = buildString {
                    if (holidayRecords.isNotEmpty()) {
                        append("法定节日，共${holidayRecords.size}条；")
                    }
                    if (hasRecords) {
                        append("自定义记录，共${userRecords.size}条")
                    } else {
                        append("自定义记录，点击添加你的日程、纪念日或想法")
                    }
                }
                if (!hasRecords && holidayRecords.isEmpty()) role = Role.Button
            }
            .padding(14.dp),
    ) {
        // 顶部：插画 + 标题 + 有记录时的显式新增按钮
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NotebookIllustration()
            Spacer(Modifier.width(12.dp))
            Text(
                text = "当日记录",
                fontSize = DaybookCardTitleFontSize,
                fontWeight = DaybookTitleFontWeight,
                color = DaybookText,
                modifier = Modifier.weight(1f),
            )
            if (hasRecords || holidayRecords.isNotEmpty()) {
                ExplicitAddButton(
                    selectedDate = selectedDate,
                    onClick = onAdd,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // 节日记录：系统色标记条，展示在自定义记录上方（系统节日无详情页，不可点击）
        holidayRecords.forEachIndexed { index, record ->
            RecordRow(record = record, markColor = DaybookSystemMark)
            if (index != holidayRecords.lastIndex) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(DaybookLine),
                )
                Spacer(Modifier.height(8.dp))
            }
        }
        if (holidayRecords.isNotEmpty() && hasRecords) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DaybookLine),
            )
            Spacer(Modifier.height(8.dp))
        }

        if (!hasRecords) {
            // 空状态
            Text(
                text = if (holidayRecords.isEmpty()) "还没有自定义记录" else "还没有自定义记录，点击 + 添加",
                fontSize = DaybookCardBodyFontSize,
                color = DaybookText,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(4.dp))
            if (holidayRecords.isEmpty()) {
                Text(
                    text = "点击这里，添加你的日程、纪念日或想法",
                    fontSize = DaybookCardCaptionFontSize,
                    color = DaybookMuted,
                )
            }
        } else {
            // 记录列表：点击进入该事件的详情页（与首页卡片一致）
            userRecords.forEachIndexed { index, record ->
                RecordRow(
                    record = record,
                    markColor = DaybookUserMark,
                    onClick = { onOpenEvent(record) },
                )
                if (index != userRecords.lastIndex) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(DaybookLine),
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun RecordRow(
    record: DaybookEvent,
    markColor: Color,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 左侧类型色条
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(50))
                .background(markColor),
        )

        Spacer(Modifier.width(12.dp))

        // 左侧日期
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(44.dp),
        ) {
            Text(
                text = record.date.format(recordDateFormatter),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = DaybookPrimary,
            )
        }

        Spacer(Modifier.width(12.dp))

        // 中间：标题 + 类型/备注
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = record.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = DaybookText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = listOf(typeLabel(record.eventType), record.subtitle)
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                fontSize = 13.sp,
                color = DaybookMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun NotebookIllustration() {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DaybookPrimaryTint),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = null,
            tint = DaybookPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** 有记录时的显式新增按钮，保留原有无障碍语义。 */
@Composable
private fun ExplicitAddButton(
    selectedDate: LocalDate,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(DaybookPrimary.copy(alpha = 0.14f))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription =
                    "为${selectedDate.monthValue}月${selectedDate.dayOfMonth}日新增日期记录"
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null,
            tint = DaybookPrimary,
            modifier = Modifier.size(18.dp),
        )
    }
}

private fun typeLabel(type: DaybookEventType): String = when (type) {
    DaybookEventType.FESTIVAL -> "节日"
    DaybookEventType.SOLAR_TERM -> "节气"
    DaybookEventType.ANNIVERSARY -> "纪念日"
    DaybookEventType.PERSONAL -> "日常"
    DaybookEventType.REMINDER -> "提醒"
}
