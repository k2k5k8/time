package com.cch.momentmark.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.cch.momentmark.ui.theme.LocalMmExtendedColors

/** 首页 Moment/Task 卡片共用的只读日期与分组元数据行。 */
@Composable
fun MomentMetaRow(prefix: String, dateText: String, groupId: String?, modifier: Modifier = Modifier) {
    val meta = buildString {
        append(prefix); append(' '); append(dateText)
        if (!groupId.isNullOrBlank()) append(" · ").append(groupId)
    }
    Text(meta, style = MaterialTheme.typography.labelSmall, color = LocalMmExtendedColors.current.labelTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier.fillMaxWidth())
}
