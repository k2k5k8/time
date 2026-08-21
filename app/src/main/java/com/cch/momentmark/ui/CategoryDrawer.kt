package com.cch.momentmark.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material.icons.outlined.Luggage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cch.momentmark.domain.model.EventColorRole
import com.cch.momentmark.domain.model.TimeEvent

private val DrawerCream = Color(0xFFFFF9F3)
private val DrawerCard = Color(0xFFFFFCF8)
private val DrawerSelected = Color(0xFFFFF0E8)
private val DrawerAccent = Color(0xFFD98C7B)
private val DrawerInk = Color(0xFF453A34)
private val DrawerMuted = Color(0xFF9A897E)
private val DrawerIcon = Color(0xFFAC8D77)
private val DrawerRule = Color(0xFFEDE1D6)

internal data class DrawerGroup(
    val name: String,
    val count: Int,
)

internal fun drawerGroups(events: List<TimeEvent>, savedGroups: List<String>): List<DrawerGroup> {
    val eventGroups = events.mapNotNull { it.groupLabel.trim().takeIf(String::isNotBlank) }
    val names = (savedGroups + eventGroups).distinct()
    return names.map { name -> DrawerGroup(name, eventGroups.count { it == name }) }
}

internal fun filterEventsByScope(
    events: List<TimeEvent>,
    selectedFilter: EventFilter,
    selectedGroup: String?,
): List<TimeEvent> = events.filter { event ->
    val statusMatches = when (selectedFilter) {
        EventFilter.ALL -> true
        EventFilter.FUTURE -> event.colorRole == EventColorRole.FUTURE
        EventFilter.PAST -> event.colorRole == EventColorRole.PAST
        EventFilter.PINNED -> event.isPinned
    }
    val groupMatches = selectedGroup == null || event.groupLabel.trim() == selectedGroup
    statusMatches && groupMatches
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoryDrawer(
    events: List<TimeEvent>,
    groups: List<DrawerGroup>,
    selectedFilter: EventFilter,
    selectedGroup: String?,
    onSelectFilter: (EventFilter) -> Unit,
    onSelectGroup: (String?) -> Unit,
    onManageGroups: () -> Unit,
    onClose: () -> Unit,
) {
    val allCount = events.size
    val futureCount = events.count { it.colorRole == EventColorRole.FUTURE }
    val pastCount = events.count { it.colorRole == EventColorRole.PAST }
    val pinnedCount = events.count { it.isPinned }

    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(.74f)
            .widthIn(max = 430.dp),
        drawerShape = RoundedCornerShape(topEnd = 34.dp, bottomEnd = 34.dp),
        drawerContainerColor = DrawerCream,
        drawerTonalElevation = 0.dp,
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(DrawerCream)
                .padding(horizontal = 20.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(top = 18.dp, bottom = 20.dp),
        ) {
            DrawerHeader()
            DrawerSectionTitle("默认筛选")
            DrawerFilterItem(
                title = "全部事件",
                count = allCount,
                icon = { Icon(Icons.Outlined.GridView, null) },
                selected = selectedFilter == EventFilter.ALL,
                onClick = { onSelectFilter(EventFilter.ALL) },
            )
            DrawerFilterItem(
                title = "未来事件",
                count = futureCount,
                icon = { Icon(Icons.Outlined.AccessTime, null) },
                selected = selectedFilter == EventFilter.FUTURE,
                onClick = { onSelectFilter(EventFilter.FUTURE) },
            )
            DrawerFilterItem(
                title = "过去事件",
                count = pastCount,
                icon = { Icon(Icons.Outlined.CalendarMonth, null) },
                selected = selectedFilter == EventFilter.PAST,
                onClick = { onSelectFilter(EventFilter.PAST) },
            )
            DrawerFilterItem(
                title = "置顶事件",
                count = pinnedCount,
                icon = { Icon(Icons.Outlined.PushPin, null) },
                selected = selectedFilter == EventFilter.PINNED,
                onClick = { onSelectFilter(EventFilter.PINNED) },
            )

            DrawerSectionTitle("我的分组", modifier = Modifier.padding(top = 20.dp))
            if (groups.isEmpty()) {
                Text(
                    "还没有创建分组\n创建分组后，可以更方便地整理重要日子",
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    color = DrawerMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                )
            } else {
                groups.forEachIndexed { index, group ->
                    DrawerFilterItem(
                        title = group.name,
                        count = group.count,
                        icon = { GroupIcon(group.name, index) },
                        selected = selectedGroup == group.name,
                        onClick = {
                            onSelectGroup(if (selectedGroup == group.name) null else group.name)
                        },
                    )
                }
            }

            DrawerTipCard()
            DrawerFooter(
                onManageGroups = onManageGroups,
                onClose = onClose,
            )
        }
    }
}

@Composable
private fun DrawerHeader() {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryLogo()
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                "分类与分组",
                color = DrawerInk,
                style = MaterialTheme.typography.headlineSmall,
                fontSize = 28.sp,
            )
            Text(
                "快速查看不同分组中的重要日子",
                color = DrawerMuted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun CategoryLogo() {
    Canvas(modifier = Modifier.size(52.dp)) {
        val unit = size.minDimension / 4f
        val triangle = Path().apply {
            moveTo(size.width / 2f, unit * .35f)
            lineTo(unit * 3.45f, unit * 2.2f)
            lineTo(unit * .55f, unit * 2.2f)
            close()
        }
        drawPath(triangle, Color(0xFFD88578))
        drawRoundRect(
            color = Color(0xFFA87E67),
            topLeft = Offset(unit * .35f, unit * 2.3f),
            size = androidx.compose.ui.geometry.Size(unit * 1.55f, unit * 1.25f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(unit * .35f),
        )
        drawCircle(Color(0xFFD98D7D), radius = unit * .7f, center = Offset(unit * 3.25f, unit * 3f))
    }
}

@Composable
private fun DrawerSectionTitle(title: String, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = DrawerInk, style = MaterialTheme.typography.titleMedium)
        androidx.compose.foundation.layout.Spacer(Modifier.width(10.dp))
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFFDCCDBF)),
        )
        androidx.compose.foundation.layout.Spacer(Modifier.width(10.dp))
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(DrawerRule),
        )
    }
}

@Composable
private fun DrawerFilterItem(
    title: String,
    count: Int,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val description = "$title，$count 个事件${if (selected) "，已选中" else ""}"
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .shadow(if (selected) 0.dp else 5.dp, RoundedCornerShape(18.dp), clip = false)
            .clip(RoundedCornerShape(18.dp))
            .semantics {
                contentDescription = description
                role = Role.Button
            }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) DrawerSelected else DrawerCard,
        border = if (selected) BorderStroke(1.dp, Color(0x4DD98C7B)) else null,
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.ProvideTextStyle(
                    value = MaterialTheme.typography.titleLarge.copy(color = if (selected) DrawerAccent else DrawerIcon),
                    content = icon,
                )
            }
            androidx.compose.foundation.layout.Spacer(Modifier.width(10.dp))
            Text(
                title,
                modifier = Modifier.weight(1f),
                color = DrawerInk,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
            Text(
                "$count 个事件",
                color = DrawerMuted,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                softWrap = false,
            )
            androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
            if (selected) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(DrawerAccent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
                }
            } else {
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = DrawerMuted, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun GroupIcon(name: String, index: Int) {
    val tint = if (index == 0) DrawerAccent else DrawerIcon
    val image = when {
        name.contains("旅行") || name.contains("生活") -> Icons.Outlined.Luggage
        name.contains("学习") || name.contains("成长") -> Icons.Outlined.MenuBook
        name.contains("家") -> Icons.Outlined.FavoriteBorder
        name.contains("工作") -> Icons.Outlined.BusinessCenter
        else -> Icons.Outlined.WorkOutline
    }
    Icon(imageVector = image, contentDescription = null, tint = tint)
}

@Composable
private fun DrawerTipCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 24.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFFF6EC),
        border = BorderStroke(1.dp, Color(0xFFEADACB)),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Outlined.Lightbulb, contentDescription = null, tint = Color(0xFFB89D88), modifier = Modifier.size(24.dp))
            Text(
                "选择分组后，首页只会展示该分组中的卡片内容。",
                modifier = Modifier.padding(start = 12.dp),
                color = DrawerMuted,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 21.sp,
            )
        }
    }
}

@Composable
private fun DrawerFooter(onManageGroups: () -> Unit, onClose: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = DrawerCard,
        border = BorderStroke(1.dp, DrawerRule),
    ) {
        androidx.compose.foundation.layout.Column {
            DrawerFooterItem("管理分组", Icons.Outlined.Settings, onManageGroups)
            androidx.compose.material3.HorizontalDivider(color = DrawerRule, modifier = Modifier.padding(horizontal = 16.dp))
            DrawerFooterItem("返回主页", Icons.Outlined.Home, onClose)
        }
    }
}

@Composable
private fun DrawerFooterItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = title
                role = Role.Button
            },
        leadingContent = { Icon(icon, contentDescription = null, tint = DrawerIcon) },
        headlineContent = { Text(title, color = DrawerInk, style = MaterialTheme.typography.titleMedium) },
        trailingContent = { Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = DrawerMuted) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroupManagementScreen(
    groups: List<DrawerGroup>,
    onBack: () -> Unit,
    onCreate: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var editingGroup by rememberSaveable { mutableStateOf<String?>(null) }
    var draftName by rememberSaveable { mutableStateOf("") }
    var deleteGroup by rememberSaveable { mutableStateOf<String?>(null) }
    var showCreate by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("管理分组") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                "分组不会删除事件，删除后事件会回到未分组。",
                color = DrawerMuted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            groups.forEachIndexed { index, group ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = DrawerCard,
                    border = BorderStroke(1.dp, DrawerRule),
                ) {
                    ListItem(
                        leadingContent = { GroupIcon(group.name, index) },
                        headlineContent = { Text(group.name) },
                        supportingContent = { Text("${group.count} 个事件", color = DrawerMuted) },
                        trailingContent = {
                            androidx.compose.foundation.layout.Row {
                                TextButton(onClick = {
                                    editingGroup = group.name
                                    draftName = group.name
                                }) { Text("编辑") }
                                TextButton(onClick = { deleteGroup = group.name }) { Text("删除") }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
            if (groups.isEmpty()) {
                Text("还没有分组，先创建一个吧。", color = DrawerMuted)
            }
            TextButton(
                onClick = {
                    draftName = ""
                    showCreate = true
                },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("＋ 创建分组")
            }
        }
    }

    if (showCreate || editingGroup != null) {
        val oldName = editingGroup
        AlertDialog(
            onDismissRequest = {
                showCreate = false
                editingGroup = null
            },
            title = { Text(if (oldName == null) "创建分组" else "编辑分组") },
            text = {
                OutlinedTextField(
                    value = draftName,
                    onValueChange = { draftName = it.take(24) },
                    singleLine = true,
                    label = { Text("分组名称") },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = draftName.trim().isNotEmpty(),
                    onClick = {
                        val value = draftName.trim()
                        if (oldName == null) onCreate(value) else if (oldName != value) onRename(oldName, value)
                        showCreate = false
                        editingGroup = null
                    },
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreate = false
                    editingGroup = null
                }) { Text("取消") }
            },
        )
    }

    deleteGroup?.let { name ->
        AlertDialog(
            onDismissRequest = { deleteGroup = null },
            title = { Text("删除分组？") },
            text = { Text("确定删除“$name”分组吗？其中的事件会保留并回到未分组。") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(name)
                    deleteGroup = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteGroup = null }) { Text("取消") }
            },
        )
    }
}
