package com.cch.momentmark.ui.moment.home

/**
 * 仅 DEBUG 视觉回归 fixture：`setprop debug.momentmark.drawer filter` 启动即
 * 打开第⑧屏「分类抽屉 · 搜索」的冻结画面（design.html 第 ⑧ 屏）。
 * 生产构建不触发；示例数据只用于视觉对齐，不写入业务数据库。
 */
object MomentHomeVisualFixture {

    /** design.html 第⑧屏整帧：35% 背景列表 + 抽屉内容。 */
    val mapFilter: MapFilterDrawerFixture = MapFilterDrawerFixture(
        backdrop = listOf(
            MapFilterBackdropCard(title = "考研初试", meta = "96 DAYS · DUE 12.19"),
            MapFilterBackdropCard(title = "演唱会", meta = "32 DAYS · DUE 09.20"),
        ),
        drawerState = MapFilterDrawerState(
            totalCount = 6,
            partyRows = listOf(
                MapFilterPartyRow(id = "学业", name = "学业", iconKey = "study", count = 3),
                MapFilterPartyRow(id = "生活", name = "生活", iconKey = "heart", count = 2),
                MapFilterPartyRow(id = "旅行", name = "旅行", iconKey = "travel", count = 1),
                MapFilterPartyRow(id = "工作", name = "工作", iconKey = "work", count = 0),
            ),
            // 设计稿稿面：生活为 danger 线路色选中态，学业为墨色文字首行。
            selectedPartyRowId = "生活",
            prominentPartyRowId = "学业",
        ),
    )
}
