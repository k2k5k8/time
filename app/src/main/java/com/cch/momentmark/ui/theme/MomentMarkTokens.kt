package com.cch.momentmark.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * 设计 token：像素冒险的间距、像素元素与动效规格。
 * 颜色见 Color.kt，字号见 Type.kt，形状见 Shapes.kt（全局直角）。
 * 完整规范与使用规则见 DESIGN_SYSTEM.md。
 */
object MomentMarkTokens {

    // ── 设计画布基准 ─────────────────────────────────────
    /**
     * design.html `.phone{width:330px;border:8px solid}` 的**内容区**尺寸：330 − 2×8 = 314、
     * 664 − 2×8 = 648。本文件所有字号 / 间距 / 描边 token 都是在这个宽度下标定的，
     * 例如首页双列小卡宽 `(314 − 2×12 − 10) ÷ 2 = 140`。
     *
     * 设备逻辑宽度不等于 314dp 时（Pixel 9 Pro 384dp、主流 360dp 机等），如果不做处理，
     * 卡片外框会随 `weight(1f)` 被横向拉伸，而字号 / 内距仍是固定 dp，导致宽高比失真。
     * [com.cch.momentmark.ui.components.PixelCanvas] 用这两个值做整页等比缩放的基准：
     * 卡片宽高比、内距与字号的比例在任何设备上都与设计稿一致。
     */
    val DesignCanvasWidth = 314.dp
    val DesignCanvasHeight = 648.dp

    // ── 间距（4dp 栅格）──────────────────────────────────
    val SpaceUnit = 4.dp
    val SpacePage = 14.dp        // 页面水平边距
    val SpaceCard = 10.dp        // 卡片/任务卡之间
    val SpaceInner = 12.dp       // 卡片内边距
    val SpaceSection = 18.dp     // 区块之间
    val SpaceCompact = 8.dp      // 紧凑任务行 / chip
    val SpaceInput = 10.dp       // 输入内框文字留白

    // ── 像素元素 ─────────────────────────────────────────
    val PxBorderWidth = 3.dp     // 全局墨线描边
    val PxThinBorderWidth = 2.dp // 细描边：血条 / 徽章 / 选中项金线
    // 与 proposal_J_final.html 的 .px/.px4 一一对应：3/4 CSS px → 3/4dp 基准构图。
    val PxShadowOffset = 3.dp    // 硬偏移阴影（普通卡/按钮）
    val PxShadowOffsetLarge = 4.dp // 硬偏移阴影（主卡/弹窗）
    val PxShadowOffsetSmall = 2.dp // 硬偏移阴影（底部导航/小元素）
    val PressOffset = 2.dp       // 按压时位移 + 阴影缩短量

    // ── 游戏化组件 ───────────────────────────────────────
    const val XpCellCount = 16   // 经验条总格数
    val XpCellHeight = 12.dp
    val XpCellGap = 3.dp
    val HpBarHeight = 10.dp      // 血条高度
    val CheckboxSize = 15.dp     // 目标复选框
    val TouchTargetMin = 44.dp   // 关键触摸目标最小尺寸
    val CompactIconSize = 28.dp  // 紧凑像素箭头/图标的视觉盒尺寸
    val CheckboxStrokeWidth = 2.5.dp // 复选框/虚线新增框描边
    val AchievementInset = 5.dp  // 成就金线距外框留白：内容 padding 10dp，金线 inset 5dp，内容在金线内 5dp（与 CSS 13-8=5px 一致）
    // ── 表单版面（design.html 第 ②③ 屏实测，见 docs/design/visual/screen-02-add/design-geometry-*.json）──
    // ⚠ 统一规则：PixelPanel 的 `Modifier.border` 画在盒子**内部**、不占布局尺寸，
    // 而 HTML 的 `border` 会让盒子变大。因此 Compose 侧 padding 一律取
    // 「设计稿 padding + 设计稿 border-width」，保证内容与边框的绝对位置一致。
    // `.pagetitle{padding:10px 12px;border:3px}` → 13 / 15。
    val PageTitlePaddingVertical = 13.dp
    val PageTitlePaddingHorizontal = 15.dp
    // `.pagetitle .back{width:25px;height:25px;margin-right:12px;box-shadow:2px 2px 0}`
    val PageTitleBackSize = 25.dp
    val PageTitleBackGap = 12.dp
    /**
     * `.back` 内的 `‹`（HTML `font-size:18px`）在 design.html 中实测的**字形墨迹**：
     * 5.5dp 宽 × 7dp 高、水平笔画厚 2.5dp（见 `design-geometry-*.json` 与
     * `docs/design/visual/screen-02-add/visual-report-round2.md` §2）。
     *
     * Android 兜底字体（Noto Sans CJK）的 U+2039 只有约 3dp 宽，仅为设计稿的一半，
     * 无法通过字号调大补齐（字号一大会同时把字形拉高）。因此这里改为
     * [PixelBackChevron] 用 Canvas 填充多边形自绘，几何完全可控。
     */
    val PageTitleBackChevronWidth = 5.5.dp
    val PageTitleBackChevronHeight = 7.dp
    /**
     * `.back` 内字形在 25dp 方块中的垂直落点：实测字形中心比方块中心低 1.5dp
     * （HTML 行盒的基线留白所致），自绘时显式补回该偏移。
     */
    val PageTitleBackChevronOffsetY = 1.5.dp
    /**
     * `.pagetitle` 实测内容行高 25dp（= `.back` 方块高度）。
     * 显式锁定以避免 Row 被标题文字行高压缩。
     */
    val PageTitleMinHeight = 25.dp
    // `.px{padding:10px 12px;border:3px}` → 13 / 15。
    val FormPanelPaddingVertical = 13.dp
    val FormPanelPaddingHorizontal = 15.dp
    // 首块 `.px4{padding:11px 12px;border:3px}` → 14 / 15。
    val FormPanelEmphasizedPaddingVertical = 14.dp
    // `.party-rail{gap:7px;margin-top:8px}`：分组 chip 间距。
    val FormPartyRailGap = 7.dp
    // `.party-rail` 与 `▸ 重要度 RARITY` 行之间 `margin-top:10px`。
    val FormRarityRowGap = 10.dp
    // `.rarity-stars{gap:2px}` + `.s1/.s2/.s3{font-size:13/17/21px}` 递进星字号。
    val FormRarityStarGap = 2.dp
    val FormRarityStarSizes = listOf(13.sp, 17.sp, 21.sp)
    /**
     * `▸ 重要度 RARITY` 行的行高 = 最大星字号 21px（HTML `line-height:1`，行盒即 21px）。
     * 注意：该行触摸目标因此为 21dp 高，低于 [TouchTargetMin]；这是为对齐设计稿
     * 行高所做的显式取舍，已记录在 `组件改造.md`。
     */
    val FormRarityRowHeight = 21.dp
    // 字段 label 行与白框之间 HTML 固定为 `margin-top:7px`。
    val FormLabelToInputGap = 7.dp
    // `.notebox{padding:8px 10px;border:3px}` → 11 / 13。
    val FormInputPaddingVertical = 11.dp
    val FormInputPaddingHorizontal = 13.dp
    // NAME 单行：实测白框 42dp = 8+20+8 + 6(border)，14px 文字行高 20px。
    val FormInputTextSize = 14.sp
    val FormInputLineHeight = 20.sp
    // LORE `.notebox`：实测 61.6dp = 3+8+39.6+8+3，即 12px 文字两行 × 19.8。
    val FormNoteTextSize = 12.sp
    val FormNoteLineHeight = 19.8.sp
    val FormNoteMinHeight = 58.dp
    // 年/月/日三格：`.px` 内 `padding:7px 8px;border:3px` → 10 / 11；实测格高 38dp。
    val FormDateCellPaddingVertical = 10.dp
    val FormDateCellPaddingHorizontal = 11.dp
    val FormDateCellValueSize = 15.sp
    val FormDateCellValueLineHeight = 18.sp
    // `.btn{padding:9px 0}` 的方向按钮：design.html 渲染高度实测 ≈66dp（截图边框对齐标定）。
    val FormDirectionButtonHeight = 66.dp
    // `▸ 分组线路 PARTY` chip：`.party-rail > *{padding:4px 9px;border:2.5px}` → 6.5 / 11.5；实测 chip 高 29dp。
    val FormChipPaddingVertical = 6.5.dp
    val FormChipPaddingHorizontal = 11.5.dp
    val FormChipLineHeight = 16.sp
    // 提交按钮：`.btn.px{padding:12px 0;border:3px}` → 15；实测 50dp。
    val FormSubmitPaddingVertical = 15.dp
    // ── 第 ④ 屏（接受新任务）表单版面 ──────────────────────────────
    // 截止日期双格面板：`.px` 顶行 label + 右上 helperLabel「[ ] 全天」；
    // 第二行两白框（design `flex:1.4` 日期 + `flex:1` 时间），`gap:8px`、`margin-top:8px`。
    val FormTaskDateTimeLabelToRowGap = 8.dp
    val FormTaskDateTimeCellRowGap = 8.dp
    val FormTaskDateTimeCellWeightDate = 1.4f
    val FormTaskDateTimeCellWeightTime = 1f
    // 任务描述 LORE 与第 ②③ 屏的 notebox 共享 FormNote*；helperLabel `选填 · 攻略/备注/链接`。
    // 难度 DIFFICULTY 行：等宽星串 17sp / letterSpacing 3sp；行盒 21dp（design 实测面板高 56dp）。
    // ⚠ 触摸高度 21dp < TouchTargetMin，与 FormRarityRow 同一显式取舍，见 `组件改造.md`。
    val FormTaskDifficultyLabelTopGap = 10.dp
    // 截止日期/时间白框内文字：design `font-size:14px`（第 ②③ 屏日期格是 15px，④ 屏为 14px）。
    val FormTaskCellValueSize = 14.sp
    // 难度等宽星串：design `font-size:17px;letter-spacing:3px`。
    val FormTaskDifficultyStarSize = 17.sp
    val FormTaskDifficultyStarSpacing = 3.sp
    // ── 首页 ① 版面（逐项来自 design.html 第 ① 屏实测，单位 = 逻辑 px/dp）──
    // .home-scroll{padding:0 12px 12px}：页面左右边距 12，滚动区底部留白 12。
    val HomePagePaddingHorizontal = 12.dp
    val HomeScrollPaddingBottom = 12.dp
    // .statusbar{padding:11px 18px 5px}
    val HomeStatusHudPaddingTop = 11.dp
    val HomeStatusHudPaddingBottom = 5.dp
    // HTML 第①屏 `.ach` 置顶成就卡的逐像素还原值。
    val HomeAchievementPaddingHorizontal = 13.dp
    val HomeAchievementPaddingTop = 11.dp
    val HomeAchievementPaddingBottom = 13.dp // 10→13：兰亭黑数字行实际占高 ~60dp，把 meta 行挤到 border 旁；与 DESIGN 视觉留白 12.5dp 对齐
    val HomeAchievementMainGap = 4.dp
    val HomeAchievementHpGap = 6.dp
    val HomeAchievementMetaGap = 3.dp
    val HomeAchievementUnitBaselineOffset = 2.dp
    val HomeAchievementHudFontSize = 9.sp
    val HomeAchievementTitleFontSize = 14.sp
    val HomeAchievementNumberFontSize = 44.sp
    val HomeAchievementNumberLineHeight = 42.sp // 44px × 原 HTML line-height 1.05；因兰亭黑内建行高偏高而收紧到 42sp，避免置顶卡底部裁切
    val HomeAchievementUnitFontSize = 12.sp
    val HomeAchievementMetaFontSize = 8.sp
    // 实测：置顶成就卡外框 290×150 逻辑（容纳兰亭黑 SC Heavy 实际行高 1.4×font-size；
    // DESIGN PingFang 仅 142，但 IMPL 数字行撑高约 8dp，超出 lineHeight 1.05 的 46dp 设计值）。
    // 直接锁 aspectRatio 让布局节奏与 DESIGN 对齐；高度 150dp 比 DESIGN 142dp 多 8dp，
    // 整体页面布局与设计稿一一对应（nav 与第三行小卡间距不缩小）。
    val HomeAchievementAspectRatio = 290f / 150f
    // HTML 首页①双列小卡（.px）：两种内容共享同一骨架。
    val HomeSmallCardPaddingHorizontal = 11.dp
    val HomeSmallCardPaddingTop = 10.dp
    val HomeSmallCardPaddingBottom = 9.dp
    val HomeSmallCardNumberFontSize = 26.sp
    val HomeSmallCardNumberLineHeight = 31.sp // 浏览器 normal 行高的固定基线
    val HomeSmallCardUnitFontSize = 10.sp
    val HomeSmallCardTitleFontSize = 12.sp
    val HomeSmallCardTitleLineHeight = 15.sp
    val HomeSmallCardMetaFontSize = 8.sp
    val HomeSmallCardMetaLineHeight = 10.sp
    val HomeSmallCardTitleToNumberGap = 3.dp
    val HomeSmallCardNumberToBarGap = 5.dp
    val HomeSmallCardBarToMetaGap = 4.dp
    // 实测：双列小卡 140x114 逻辑 → 1.228
    val HomeSmallCardAspectRatio = 1.228f
    val HomeSmallCardIconSize = 25.dp
    val HomeSmallCardIconFontSize = 17.sp
    val HomeStatusHudPadding = 18.dp
    val HomeStatusHudVerticalPadding = 5.dp
    val HomeStatusHudStyle = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, lineHeight = 13.sp)
    // 顶部 mono HUD 行**总高**（design `.statusbar{padding:11px 18px 5px}` + 11px mono 行盒 13px = 29px）。
    // 必须显式锁高：⑤ 屏左侧文案含 ♥ 字形，Android 兜底字体的该字形行盒比 mono 高 2dp，
    // 只靠 padding 会让 statusbar 变成 31dp、并让整页内容整体下移 2dp（实测 5px @2x）。
    val HomeStatusHudHeight = 29.dp
    // 底部导航（HTML `.nav .item{padding:7px 0 6px;border:3px solid}`）：普通项内留白。
    // border 补偿：HTML 的 3px 边框占布局尺寸，Compose 的 border 画在盒内 → 本应取 7+3 / 6+3。
    // 但 Android 的 mono 字体 11sp 自然行盒是 16dp（设计稿 10px 字体行盒 14px），
    // 若仍按 10/9 会让项高变成 35dp；这里把多出的 2dp 从 padding 里让出来（9/8），
    // 使项高仍为设计的 33dp，同时文字行盒中心与设计稿保持同一位置（17px 处）。
    val NavItemPaddingTop = 9.dp
    val NavItemPaddingBottom = 8.dp
    // 底部导航容器（HTML `.nav{padding:10px 12px 16px}` + `border-top:3px`）：border 画在盒内，故顶部补偿 +3。
    val NavPaddingTop = 13.dp
    val NavPaddingHorizontal = 12.dp
    val NavPaddingBottom = 16.dp
    // HTML 第①屏「主线任务横幅卡」（.px4 + 春节·回家的日子）的逐像素还原值。
    val HomeMainQuestPaddingHorizontal = 13.dp
    val HomeMainQuestPaddingVertical = 11.dp      // top & bottom 同为 11px
    val HomeMainQuestMainGap = 4.dp               // HUD → 主信息行 margin-top
    val HomeMainQuestHudFontSize = 9.sp
    val HomeMainQuestHudLineHeight = 13.sp        // 9px mono 的 normal 行高（Chrome 实测）
    val HomeMainQuestTitleFontSize = 14.sp
    val HomeMainQuestTitleLineHeight = 20.sp      // 14px sans 的 normal 行高（Chrome 实测）
    val HomeMainQuestNumberFontSize = 44.sp
    val HomeMainQuestNumberLineHeight = 46.2.sp   // 44px × 1.05
    val HomeMainQuestUnitFontSize = 12.sp
    val HomeMainQuestUnitLineHeight = 12.6.sp     // 12px × 原 HTML line-height 1.05（Chrome 实测）
    // 实测：置顶主线宽卡 290×116 逻辑（兰亭黑 44sp 数字行实际占 ~60dp，比 DESIGN 107 多 ~9dp）。
    val HomeMainQuestAspectRatio = 290f / 116f
    val InputMinHeight = 48.dp   // 像素输入框的最小触摸高度
    /**
     * 输入框的方块光标。design.html 用内联 `<span>▮</span>`（U+25AE）+ `@keyframes blink`
     * 表达"待输入"状态；Chrome 实测该字形在 14px mono 下墨迹为 4dp 宽 × 8.3dp 高。
     * 本实现用 `drawRect` 绘制等价矩形（不依赖兜底字体是否含 U+25AE）。
     */
    val FormInputCursorWidth = 4.dp
    val FormInputCursorHeight = 8.3.dp
    /** `@keyframes blink{50%{opacity:0}}` + `animation:blink 1s steps(1) infinite`：1s 硬切换。 */
    const val CursorBlinkPeriodMs = 1000
    /** `.form-scroll` 在 HTML 中内容直接贴到屏幕底部；App 内额外留白，避免提交按钮压在手势条上。 */
    val FormScrollPaddingBottom = 28.dp
    // HTML `.badge-tag` 的紧凑内留白（2px 6px），避免徽章描边贴住文字。
    val BadgePaddingVertical = 2.dp
    val BadgePaddingHorizontal = 6.dp

    // ── 日子簿 ⑤ 版面（design.html `.dg`/`.pagetitle`/任务行实测）──
    // `.pagetitle{padding:10px 12px;border:3px;box-shadow:3px}` + 底部 margin 10。
    val DaybookTitlePanelMarginHorizontal = 12.dp
    val DaybookTitlePanelMarginBottom = 10.dp
    val DaybookTitlePanelPaddingVertical = 13.dp   // 10 + 3 边框补偿
    val DaybookTitlePanelPaddingHorizontal = 15.dp // 12 + 3 边框补偿
    val DaybookTitleArrowFontSize = 12.sp
    val DaybookTitleArrowLineHeight = 15.sp
    val DaybookTitleLabelFontSize = 15.sp
    val DaybookTitleLabelLineHeight = 21.sp
    // 15sp sans 的字体自然行盒 = 22dp（ascent+descent），比设计稿的 21px 多 1dp；
    // 行盒只能用显式高度约束压回 21dp，否则月标题面板会比设计稿高 1dp、并让整页内容下移。
    val DaybookTitleLabelBoxHeight = 21.dp
    val DaybookTitleHudFontSize = 9.sp
    val DaybookTitleHudLineHeight = 13.sp
    val DaybookTitleHudLetterSpacing = 0.72.sp
    // `.form-scroll > div{padding:0 14px}`
    val DaybookScrollHorizontalPadding = 14.dp
    // design `.form-scroll` 与 `.nav` 之间有一段 `<div style="height:10px">`：
    // 它决定「今日掉落」面板在可视区里的截断位置（设计稿截图在 5 条任务处被裁掉）。
    val DaybookScrollToNavGap = 10.dp
    // `.weekday_row{padding:11px 18px 5px;margin-bottom:6px}`
    val DaybookWeekdayHeaderPaddingTop = 0.dp
    val DaybookWeekdayHeaderPaddingBottom = 0.dp
    val DaybookWeekdayHeaderMarginBottom = 6.dp
    // design `.mono` 星期表头 9px 字体的 normal 行盒实测 10px（Chrome）。
    val DaybookWeekdayHeaderFontSize = 9.sp
    val DaybookWeekdayHeaderLineHeight = 10.sp
    // `.cal-grid{gap:4px}` / `.dg{aspect-ratio:1;border:2px;font:13px/800 mono}`
    val DaybookCalendarGap = 4.dp
    val DaybookCellFontSize = 13.sp
    val DaybookCellLineHeight = 18.sp
    val DaybookCellMarkerFontSize = 8.sp
    val DaybookCellMarkerLineHeight = 11.sp
    // design `.fx{bottom:3px}` 相对边框内的内边距盒定位 → Compose 需 +2dp 边框补偿。
    val DaybookCellMarkerBottomInset = 5.dp
    val DaybookTodayShadowOffset = 2.dp
    // 任务面板
    val DaybookTaskPanelMarginTop = 12.dp
    val DaybookTaskPanelPaddingVertical = 14.dp   // 11 + 3 边框补偿
    val DaybookTaskPanelPaddingHorizontal = 16.dp  // 13 + 3 边框补偿
    val DaybookTaskPanelTitleFontSize = 9.sp
    val DaybookTaskPanelTitleLineHeight = 13.sp
    val DaybookTaskPanelTitleLetterSpacing = 2.25.sp
    // design 任务行 = 8(pad) + 18.5(13px sans 的 normal 行盒) + 8(pad) + 2(border-bottom) = 36.5px。
    // Chrome 实测 5 条虚线的间距恒为 73 设备像素 @2x = 36.5 CSS px；取 36dp 会让每行少 1px、
    // 5 行累积 5px（实测第 5 条虚线偏移 -4px），故必须用 36.5dp。
    val DaybookTaskRowHeight = 36.5.dp
    val DaybookTaskRowPaddingVertical = 8.dp
    val DaybookTaskRowBorderWidth = 2.dp
    // design `border-bottom:2px dashed` 的 Chrome 实测 dash 模式：4px 实 / 2px 虚（周期 6px）。
    val DaybookTaskRowDashOn = 4.dp
    val DaybookTaskRowDashOff = 2.dp
    val DaybookTaskTitleFontSize = 13.sp
    val DaybookTaskTitleLineHeight = 18.sp
    val DaybookTaskStatusFontSize = 8.sp
    val DaybookTaskStatusLineHeight = 11.sp
    // design `.mono{letter-spacing:normal}`：⑤ 屏的 mono 文本只有页面标题 `.r`（0.08em）与任务面板
    // 标题（0.25em）带字距，其余（星期表头 / CLEAR +5 / 10:00 截止 / 全天）都是 normal。
    // `labelSmall` 默认带 0.5sp 字距，会让「CLEAR +5」比设计稿宽 10px（实测 85 vs 75）、
    // 星期表头每格右移 1px，因此这些位置必须显式归零。
    val MonoLetterSpacingNone = 0.sp
    // design `.checkbox.on::after{inset:-3px 0 0 0;font-size:11px;font-weight:800;color:#fff}`：
    // ::after 盒比复选框高 3px（顶端溢出）且垂直居中 → 勾号视觉中心比复选框中心高 1.5px；
    // Android 实测勾号中心反而低 0.75dp，故需上移 2dp（实测 4px @2x）。
    val CheckboxCheckMarkFontSize = 11.sp
    val CheckboxCheckMarkOffsetY = (-2).dp
    val DaybookNewTaskRowPaddingTop = 9.dp
    val DaybookNewTaskRowPaddingBottom = 2.dp
    val DaybookNewTaskTitleFontSize = 13.sp
    val DaybookNewTaskTitleLineHeight = 18.sp

    // ── 第 ⑥ 屏 任务详情（QUEST DETAIL · 未来 · 昼，design.html `.phone` 314×648）──
    // 内容列 `<div style="padding:0 12px">`：与首页同一左右边距。
    val DetailPagePaddingHorizontal = 12.dp
    // `.px4{padding:12px 13px;border:3px;box-shadow:4px}` → border 补偿 15 / 16。
    val DetailBannerPaddingVertical = 15.dp
    val DetailBannerPaddingHorizontal = 16.dp
    // 横幅 HUD 行（9px mono 800）→ 标题 margin-top 5px。
    val DetailBannerHudFontSize = 9.sp
    val DetailBannerHudLineHeight = 12.sp
    val DetailBannerTitleTopGap = 5.dp
    // 标题 17px / 800。
    val DetailBannerTitleFontSize = 17.sp
    val DetailBannerTitleLineHeight = 22.sp
    // 大数字 48px / 800 / line-height:1；单位「天后开启」13px。
    val DetailBannerNumberTopGap = 4.dp
    val DetailBannerNumberFontSize = 48.sp
    val DetailBannerNumberLineHeight = 48.sp
    val DetailBannerUnitFontSize = 13.sp
    // XP 格 margin-top 9；格下 PROGRESS/UNLOCK 行 margin-top 4、8px mono 800。
    // 经验格顶距：实测 HTML `.big` 行盒基线与格顶间距折算后为 4dp（原 9dp 使横幅整体偏高 5dp）。
    val DetailBannerXpTopGap = 4.dp
    val DetailBannerXpMetaTopGap = 4.dp
    val DetailBannerXpMetaFontSize = 8.sp
    val DetailBannerXpMetaLineHeight = 11.sp
    // 通用 `.px` 内容面板（LORE / 前置目标）：padding 11px 13px + border 3 → 14 / 16。
    val DetailPanelMarginTop = 10.dp
    val DetailPanelPaddingVertical = 14.dp
    val DetailPanelPaddingHorizontal = 16.dp
    // LORE 正文 13px / 600 / line-height 1.7，label 后 margin-top 6px。
    val DetailLoreBodyTopGap = 6.dp
    val DetailLoreBodyFontSize = 13.sp
    val DetailLoreBodyLineHeight = 22.1.sp
    // 前置目标行：flex gap 9px、padding 8px 0、行底 2px 虚线 surfaceVariant。
    val DetailObjectiveRowGap = 9.dp
    val DetailObjectiveRowPaddingVertical = 8.dp
    val DetailObjectiveTitleFontSize = 13.sp
    val DetailObjectiveTitleLineHeight = 18.sp
    val DetailObjectiveStatusFontSize = 8.sp
    val DetailObjectiveStatusLineHeight = 11.sp
    // 24×24 蓝色「＋」方块。
    val DetailObjectiveAddButtonSize = 24.dp
    val DetailObjectiveAddButtonFontSize = 16.sp
    // DUE/GROUP/RARITY 元数据条：padding 10px 13px + border 3 → 13 / 16；
    // label 8px 800、value 12px 800。
    val DetailMetaPaddingVertical = 13.dp
    val DetailMetaPaddingHorizontal = 16.dp
    val DetailMetaLabelFontSize = 8.sp
    val DetailMetaLabelLineHeight = 11.sp
    val DetailMetaValueFontSize = 12.sp
    val DetailMetaValueLineHeight = 16.sp
    val DetailMetaStarFontSize = 12.sp
    // 按钮行：margin-top 12、gap 10；编辑 flex 1 / 继续冒险 flex 1.3；padding 10px 0、13px 字。
    val DetailButtonRowTopGap = 12.dp
    val DetailButtonRowGap = 10.dp
    val DetailButtonPaddingVertical = 10.dp
    val DetailButtonFontSize = 13.sp
    val DetailButtonLineHeight = 18.sp
    // 放弃链接：居中、margin-top 9、9px mono 800 危险红、底部 2px 实线下划线。
    val DetailDeleteTopGap = 9.dp
    val DetailDeleteFontSize = 9.sp
    val DetailDeleteLineHeight = 13.sp

    // ── 第 ⑦ 屏 成就详情（ACHIEVEMENT · 过去 · 昼，design.html `.ach.px4`）──
    // 成就横幅 `.ach.px4{padding:13px 14px;border:3px;box-shadow:4px}` → border 补偿 16 / 17。
    // 横幅整体 text-align:center；外框 + 金线由 AchievementFrame 承载。
    val AchievementBannerPaddingVertical = 10.dp
    val AchievementBannerPaddingHorizontal = 17.dp
    // 🏆 奖杯 34px；ACHIEVEMENT 标签 9px mono 800 gold-ink letter-spacing .15em，margin-top 2px。
    val AchievementBannerTrophyFontSize = 34.sp
    val AchievementBannerLabelFontSize = 9.sp
    val AchievementBannerLabelLineHeight = 12.sp
    val AchievementBannerLabelLetterSpacing = 1.35.sp  // 9px × .15em ≈ 1.35px
    val AchievementBannerLabelTopGap = 2.dp
    // 标题 16px / 800，margin-top 5px。Chrome 默认行高约 19px。
    val AchievementBannerTitleFontSize = 16.sp
    val AchievementBannerTitleLineHeight = 19.sp
    val AchievementBannerTitleTopGap = 5.dp
    // 大数字 50px / 800 / line-height 1.05 gold-ink；单位「天」13px ink-2。
    val AchievementBannerNumberTopGap = 0.dp
    val AchievementBannerNumberFontSize = 50.sp
    val AchievementBannerNumberLineHeight = 52.5.sp
    val AchievementBannerNumberUnitFontSize = 13.sp
    // SINCE 行 9px mono 800 ink-3；与大数字之间留 2dp 呼吸间距。
    val AchievementBannerSinceFontSize = 9.sp
    val AchievementBannerSinceLineHeight = 12.sp
    val AchievementBannerSinceTopGap = 2.dp

    // 里程碑行（MILESTONES 面板内）：flex gap 9px；普通行 padding 7px 0 + 行底 2px 虚线。
    val MilestoneRowGap = 9.dp
    val MilestoneRowPaddingVertical = 7.dp
    // 下一项行 padding 8px 0 4px，血条 margin-top 7px；最后一行 padding 8px 0 2px 无虚线。
    val MilestoneRowNextPaddingTop = 8.dp
    val MilestoneRowNextPaddingBottom = 4.dp
    val MilestoneRowLastPaddingTop = 8.dp
    val MilestoneRowLastPaddingBottom = 2.dp
    val MilestoneHpBarTopGap = 7.dp
    // checkbox 15×15 / border 2.5px；标签固定宽 56px / 12px 800；详情 mono 9px；状态 ✓ / % mono 9px。
    val MilestoneCheckboxSize = 15.dp
    val MilestoneLabelWidth = 56.dp
    val MilestoneLabelFontSize = 12.sp
    val MilestoneLabelLineHeight = 16.sp
    val MilestoneDetailFontSize = 9.sp
    val MilestoneDetailLineHeight = 12.sp
    val MilestoneStatusFontSize = 9.sp
    val MilestoneStatusLineHeight = 12.sp
    // 已达成 checkbox 内的 ✓ 标记：11px / 800 / 白色。
    val MilestoneCheckMarkFontSize = 11.sp
    val MilestoneCheckMarkOffsetY = (-2).dp

    // ── 第 ⑧ 屏 分类抽屉 · 搜索（MAP FILTER · design.html 第⑧屏，逐项对应 HTML 内联样式）──
    // 搜索栏容器 `<div style="padding:0 12px 8px">`：左右 12、面板下方 8。
    val MapFilterSearchContainerPaddingHorizontal = 12.dp
    val MapFilterSearchContainerPaddingBottom = 8.dp
    // 搜索白框 `.px{padding:9px 12px;border:3px}` → 12 / 15（边框补偿）；行内 gap 9。
    val MapFilterSearchPanelPaddingVertical = 12.dp
    val MapFilterSearchPanelPaddingHorizontal = 15.dp
    val MapFilterSearchRowGap = 9.dp
    val MapFilterSearchIconFontSize = 14.sp
    val MapFilterSearchIconLineHeight = 17.sp
    val MapFilterSearchPlaceholderFontSize = 13.sp
    val MapFilterSearchPlaceholderLineHeight = 18.sp
    // ESC 徽章：mono 8px、2px 墨线、`padding:1px 6px` → 3 / 8（边框补偿）。
    val MapFilterEscBadgePaddingVertical = 3.dp
    val MapFilterEscBadgePaddingHorizontal = 8.dp
    val MapFilterEscBadgeBorderWidth = 2.dp
    val MapFilterEscBadgeFontSize = 8.sp
    val MapFilterEscBadgeLineHeight = 10.sp
    // 背景列表 `opacity:.35;padding:14px 12px;gap:9px`。
    const val MapFilterBackdropAlpha = 0.35f
    val MapFilterBackdropPaddingTop = 14.dp
    val MapFilterBackdropPaddingHorizontal = 12.dp
    val MapFilterBackdropGap = 9.dp
    // 背景行 `.px{padding:10px 12px;border:3px}` → 13 / 15；标题 13px/800，meta mono 9px mt3。
    val MapFilterBackdropCardPaddingVertical = 13.dp
    val MapFilterBackdropCardPaddingHorizontal = 15.dp
    val MapFilterBackdropTitleFontSize = 13.sp
    val MapFilterBackdropTitleLineHeight = 18.sp
    val MapFilterBackdropMetaFontSize = 9.sp
    val MapFilterBackdropMetaLineHeight = 11.sp
    val MapFilterBackdropMetaTopGap = 3.dp
    // 压暗层 `rgba(43,38,32,.45)`（区别于通用 AlphaScrim .5）。
    const val MapFilterScrimAlpha = 0.45f
    // 抽屉 `.px4`：left 10 / top 6 / bottom 6 / width 222；`padding:12px 12px;border:3px` → 15。
    val MapFilterDrawerOffsetStart = 10.dp
    val MapFilterDrawerOffsetTop = 6.dp
    val MapFilterDrawerOffsetBottom = 6.dp
    val MapFilterDrawerWidth = 222.dp
    val MapFilterDrawerPadding = 15.dp
    val MapFilterDrawerGap = 11.dp
    // 抽屉标题 `▸ 地图筛选 FILTER` mono 11px/800/.15em。
    val MapFilterDrawerTitleFontSize = 11.sp
    val MapFilterDrawerTitleLineHeight = 13.sp
    val MapFilterDrawerTitleLetterSpacing = 1.65.sp
    // 区段 label（状态 STATUS / 分组 PARTY LINES）mono 8px/800/.2em，下间距 6。
    val MapFilterSectionLabelFontSize = 8.sp
    val MapFilterSectionLabelLineHeight = 10.sp
    val MapFilterSectionLabelLetterSpacing = 1.6.sp
    val MapFilterSectionLabelBottomGap = 6.dp
    // 状态 chips：`gap:5px;padding:4px 8px;border:2.5px` → 6.5 / 10.5；mono 9px/800。
    val MapFilterChipRowGap = 5.dp
    val MapFilterChipPaddingVertical = 6.5.dp
    val MapFilterChipPaddingHorizontal = 10.5.dp
    val MapFilterChipBorderWidth = 2.5.dp
    val MapFilterChipFontSize = 9.sp
    val MapFilterChipLineHeight = 11.sp
    // 分组行：`gap:6px;padding:5px 8px;border:2.5px` → 7.5 / 10.5；mono 10px/800。
    val MapFilterPartyRowGap = 6.dp
    val MapFilterPartyRowPaddingVertical = 7.5.dp
    val MapFilterPartyRowPaddingHorizontal = 10.5.dp
    val MapFilterPartyRowBorderWidth = 2.5.dp
    val MapFilterPartyRowFontSize = 10.sp
    val MapFilterPartyRowLineHeight = 12.sp
    // 「⚙ 管理分组 →」ghost 按钮 `padding:7px 0` → 10（边框补偿）；mono 10px。
    val MapFilterManageButtonPaddingVertical = 10.dp
    val MapFilterManageButtonFontSize = 10.sp
    val MapFilterManageButtonLineHeight = 12.sp
    // 分类抽屉可从首页内容区任意位置向右滑出。32dp 足以区分点按，又无需停顿或长距离拖动。
    val MapFilterOpenDragDistance = 32.dp
    // 左缘系统返回兼容条带；仅为了让用户仍可从左缘起手，不限制中部右滑。
    val MapFilterSystemGestureExclusionWidth = 48.dp

    // ── 设置 ⑨ 版面（design.html 第⑨屏 SYSTEM · 昼实测）──
    // 内容容器 `padding:0 12px;gap:10px`。
    val SettingsContentPaddingHorizontal = 12.dp
    val SettingsContentGap = 10.dp
    // 冒险者卡 `.px{padding:12px 13px;gap:12px}` → 15/16（border 补偿）。
    val SettingsAdventurerPaddingVertical = 15.dp
    val SettingsAdventurerPaddingHorizontal = 16.dp
    val SettingsAdventurerGap = 12.dp
    // 头像 `width:52px;height:52px;border:3px;font-size:24px`。
    val SettingsAvatarSize = 52.dp
    val SettingsAvatarFontSize = 24.sp
    // 冒险者标题 `15px/800`；LV 行 mono `9px/800` margin-top 2；XP bar margin-top 7；EXP 文本 mono 8px margin-top 4。
    val SettingsAdventurerTitleFontSize = 15.sp
    val SettingsAdventurerTitleLineHeight = 18.sp
    val SettingsLvFontSize = 9.sp
    val SettingsLvLineHeight = 12.sp
    val SettingsLvTopGap = 2.dp
    val SettingsXpBarTopGap = 7.dp
    val SettingsXpCellCount = 8
    val SettingsExpFontSize = 8.sp
    val SettingsExpLineHeight = 10.sp
    val SettingsExpTopGap = 4.dp
    // 显示模式面板 `border:3px;padding:11px 13px`（无 box-shadow）→ 14/16；label mono 9px/.2em；按钮行 gap 7 margin-top 8。
    val SettingsDisplayPaddingVertical = 14.dp
    val SettingsDisplayPaddingHorizontal = 16.dp
    val SettingsDisplayLabelFontSize = 9.sp
    val SettingsDisplayLabelLineHeight = 11.sp
    val SettingsDisplayLabelLetterSpacing = 1.8.sp
    val SettingsDisplayRowGap = 7.dp
    val SettingsDisplayRowTopGap = 8.dp
    // 显示模式按钮 `padding:7px 0;border:3px;font-size:10px mono/800` → padding 10（border 补偿）；选中项 flex 1.2。
    val SettingsDisplayButtonPaddingVertical = 10.dp
    val SettingsDisplayButtonFontSize = 10.sp
    val SettingsDisplayButtonLineHeight = 12.sp
    val SettingsDisplaySelectedWeight = 1.2f
    // 设置列表面板无 HTML padding，PixelPanel 内容加 3dp border 补偿；
    // 行 `padding:11px 13px;border-bottom:2px dashed`（行本身无 border，不做 border 补偿）。
    val SettingsListPanelPadding = 3.dp
    val SettingsRowPaddingVertical = 11.dp
    val SettingsRowPaddingHorizontal = 13.dp
    val SettingsRowLabelFontSize = 13.sp
    val SettingsRowLabelLineHeight = 18.sp
    val SettingsRowTrailingFontSize = 10.sp
    val SettingsRowTrailingLineHeight = 12.sp
    // 底部 footer mono 8px/800/.15em 居中。
    val SettingsFooterFontSize = 8.sp
    val SettingsFooterLineHeight = 10.sp
    val SettingsFooterLetterSpacing = 1.2.sp
    val SettingsFooterTopGap = 0.dp
    // 内容与底部导航之间的 spacer `height:10px`。
    val SettingsBottomSpacer = 10.dp

    // ── 回收站 ⑩ 版面（design.html 第⑩屏 GRAVEYARD · 昼实测）──
    // 内容区 `padding:0 12px;gap:10px`。
    val RecycleBinContentPaddingHorizontal = 12.dp
    val RecycleBinContentGap = 10.dp
    // 封印条目卡 `background:panel;border:3px;padding:11px 12px`（无 box-shadow，Flat 变体）。
    val RecycleBinCardPaddingVertical = 11.dp
    val RecycleBinCardPaddingHorizontal = 12.dp
    // 标题 `14px/800;color:ink-3`；徽章 `mono 8px/800;2px border;padding:1px 6px`。
    val RecycleBinCardTitleFontSize = 14.sp
    val RecycleBinCardTitleLineHeight = 17.sp
    val RecycleBinBadgeFontSize = 8.sp
    val RecycleBinBadgeLineHeight = 10.sp
    val RecycleBinBadgePaddingVertical = 1.dp
    val RecycleBinBadgePaddingHorizontal = 6.dp
    // 元数据行 `mono 9px/800;color:ink-3;margin-top:4px`。
    val RecycleBinMetaFontSize = 9.sp
    val RecycleBinMetaLineHeight = 11.sp
    val RecycleBinMetaTopGap = 4.dp
    // 按钮行 `gap:8px;margin-top:9px`；复活=gold flex:1 `padding:7px 0;11px`；
    // 永久净化=ghost `padding:7px 12px;11px;color:ink-3`。按钮均带 .px（3dp 硬影）。
    val RecycleBinButtonRowGap = 8.dp
    val RecycleBinButtonRowTopGap = 9.dp
    val RecycleBinButtonPaddingVertical = 7.dp
    val RecycleBinButtonFontSize = 11.sp
    val RecycleBinButtonLineHeight = 13.sp
    // 底部提示 `mono 9px/800;color:ink-3;居中;letter-spacing:.1em;margin-top:4px`。
    val RecycleBinFooterFontSize = 9.sp
    val RecycleBinFooterLineHeight = 11.sp
    val RecycleBinFooterLetterSpacing = 0.9.sp
    val RecycleBinFooterTopGap = 4.dp
    // 全部净化按钮容器 `margin-top:auto;padding:0 12px 12px`；
    // 按钮 ghost `width:100%;padding:10px 0;12px;color:danger-n(#FF6B6F)`。
    val RecycleBinPurgeAllContainerPaddingBottom = 12.dp
    val RecycleBinPurgeAllPaddingVertical = 10.dp
    val RecycleBinPurgeAllFontSize = 12.sp
    val RecycleBinPurgeAllLineHeight = 14.sp

    // ── 动效 ────────────────────────────────────────────
    const val MotionStepMs = 200          // 阶梯式变化（减血/落位）
    const val MotionPressMs = 100         // 按压位移响应
    const val MotionLongPressEditMs = 500L // 长按进入布局编辑（沿用既有交互契约）

    // ── 状态透明度 ───────────────────────────────────────
    const val AlphaStackedCard = 0.38f    // 装备库相邻卡
    const val AlphaDisabled = 0.6f        // 空线/不可用分组
    const val AlphaScrim = 0.5f           // 抽屉/弹窗压暗层
}
