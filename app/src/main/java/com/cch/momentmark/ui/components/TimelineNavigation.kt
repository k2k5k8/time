package com.cch.momentmark.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal enum class TimelineDestination {
    BIG_EVENT,
    DAYBOOK,
}

private val TimelineSurface = Color(0xFFFFFCF9).copy(alpha = 0.94f)
private val TimelineText = Color(0xFF4F4036)
private val TimelineMuted = Color(0xFF9E8D7D)
private val TimelineLine = Color(0xFFE9DCD0)
private val TimelineAccent = Color(0xFFE9B18F)

/** 两个时间入口共用的轻量胶囊导航，避免首页和日子簿出现两套导航语言。 */
@Composable
internal fun TimelineNavigation(
    selectedDestination: TimelineDestination,
    onSelectBigEvents: () -> Unit,
    onSelectDaybook: (() -> Unit)?,
    onCreateEvent: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(30.dp),
        color = TimelineSurface,
        border = BorderStroke(1.dp, TimelineLine.copy(alpha = 0.72f)),
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimelineItem(
                title = "大事件",
                selected = selectedDestination == TimelineDestination.BIG_EVENT,
                description = if (selectedDestination == TimelineDestination.BIG_EVENT) {
                    "大事件，当前主页入口"
                } else {
                    "打开大事件"
                },
                onClick = onSelectBigEvents,
            )
            Spacer(
                modifier = Modifier
                    .width(1.dp)
                    .height(22.dp)
                    .background(TimelineLine),
            )
            TimelineItem(
                title = "日子簿",
                selected = selectedDestination == TimelineDestination.DAYBOOK,
                description = if (selectedDestination == TimelineDestination.DAYBOOK) {
                    "日子簿，当前页面"
                } else {
                    "日子簿，打开日子簿页面"
                },
                onClick = onSelectDaybook,
            )
            if (onCreateEvent != null) {
                Spacer(
                    modifier = Modifier
                        .width(1.dp)
                        .height(22.dp)
                        .background(TimelineLine),
                )
                TimelineAddButton(onClick = onCreateEvent)
            }
        }
    }
}

@Composable
private fun TimelineAddButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = "新建事件"
                role = Role.Button
            },
        shape = CircleShape,
        color = TimelineAccent.copy(alpha = 0.86f),
        contentColor = TimelineText,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Add, contentDescription = null)
        }
    }
}

@Composable
private fun TimelineItem(
    title: String,
    selected: Boolean,
    description: String,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .semantics {
                contentDescription = description
                if (onClick != null) role = Role.Button
            }
            .padding(horizontal = 18.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            modifier = Modifier
                .size(if (selected) 10.dp else 7.dp)
                .clip(CircleShape)
                .background(if (selected) TimelineAccent else TimelineMuted.copy(alpha = 0.45f)),
        )
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) TimelineText else TimelineMuted,
        )
    }
}
