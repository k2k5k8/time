# DESIGN_SYSTEM — 刻间「像素冒险 Pixel Quest」（design.html 当前版）

> **本文档是实现 token 与组件规格的权威来源。** 页面结构和视觉比例以根目录 [`design.html`](design.html) 为准；任何 UI 改动的颜色、字号、间距、圆角、动效取值必须来自本表的 token（代码位于 `ui/theme/`）。修改风格 = 「token + 本文档」同步改。AI 协作规则见 `AGENTS.md`。
>
> 视觉基准（HTML 预览）：[design.html](design.html)（当前 11 屏完整稿）。旧 proposal 页面仅作历史参考，不参与当前验收。

## 1. 设计定位

**一句话定位：人生是一场冒险，每个日子都是一张任务卡。**

- 关键词：任务卡 · HP 血条 · 经验格 · 宝箱 · 8-bit 直角 · 硬偏移阴影 · mono HUD 标签
- 当前版本统一使用**昼 = 任务板（冒险地图）**；`design.html` 底部夜间区为与昼间逐屏对应的 11 屏洞窟参考稿（仅配色 token 不同、内容同源克隆），仍只作为后续主题适配方向，不能混入当前页面。
- 数字永远是第一视觉层级（display 全部 extrabold）
- 游戏元素必须承载真实数据（血条=紧迫度、格数=进度、徽章=类型），**禁止纯装饰**

**三个"不要"**：不要圆角（全局 0dp）；不要柔和阴影/渐变/模糊（只用硬偏移影）；不要低对比灰字（正文 Medium 起步）。

## 2. 内容体系与游戏化语义词表（命名与文案的强制词表）

**双体系**：首页大事件 = **时刻 MOMENT**（底部导航「＋ 新时刻」创建，分两个方向）；日子簿事项 = **待办 TODO**（只有副本地图内的入口能创建，叫「＋ 新任务」）。

| 产品概念 | 游戏化术语 | 视觉/入口 |
|---|---|---|
| 首页重要日期 · 未来方向 | 任务 QUEST（⧖ 倒数） | 卡片文案「X 天后」；横幅=kicker◆主线 |
| 首页重要日期 · 过去方向 | 成就 ACHIEVEMENT（🏆 正数） | 金框卡（内嵌 2dp 金线）；文案「已 X 天」+ SINCE 日期 |
| 铭刻时刻表单 · 未来模式 | ⧖ 未来 · 倒数（蓝强调） | 目标日期 TARGET DATE；备注=任务背景 LORE；CTA「⧖ 铭刻 · 开始倒数！」 |
| 铭刻时刻表单 · 过去模式 | 🏆 过去 · 正数（金强调） | 开始日期 SINCE DATE（标签随方向切换）；备注=成就故事 STORY；CTA「🏆 铭刻 · 开始珍藏！」 |
| 日历待办事项 | 待办 / 新任务 NEW QUEST | 仅副本地图入口；默认带时刻；备注=任务描述 LORE；完成记入当日「今日掉落」 |
| 置顶 | 主线 MAIN QUEST（◆ 紫） | |
| 普通 | 支线 SIDE QUEST（◇ 蓝） | |
| 临近（剩余<30%） | 限时 LIMITED（⧗ 橙）+ 血条变红 | |
| 成就里程碑 | 里程碑 MILESTONE | 固定 100/365/1000/1200/2000…天；进行中=金色时间条；不允许自定义 |
| 分组 | 队伍/线路 Party Line | 颜色 = 线路色；图标 = 分组内置图标 |
| 删除（软删除） | 封印（移入封印之地） | 任务确认弹窗「放弃这个任务？」；成就为「封印此成就」 |
| 回收站 | 封印之地 GRAVEYARD（当前主稿为昼间） | 30 天后永久净化 |
| 恢复 | 复活 REVIVE（金按钮） | |
| 永久删除 | 净化 | 危险红 |
| 模板 | 装备/皮肤 OUTFIT | 模板浏览页 = 装备库 |
| 日子簿 | 副本地图 DUNGEON MAP | 有内容日 = 宝箱 🎁 |
| 空分组 | 空线 | 置 AlphaDisabled |

**加号命名规则（强制）**：底部导航加号固定为「＋ 新时刻」（NEW MOMENT，金色），全局打开铭刻时刻页；「＋ 新任务」只出现在日子簿/副本地图内部（今日掉落列表末尾、选中日详情），只创建待办。两词严禁混用。

## 3. Token 速查表

### 3.1 色彩（代码：`ui/theme/Color.kt`）

| 语义 | Token | 昼（浅） | 夜（洞窟） |
|---|---|---|---|
| 页面底 | background | `#E8E0CC` | `#1B1826` |
| 卡片/面板面 | surface | `#F6F1E3` | `#262138` |
| 次级面/轨道底 | surfaceVariant | `#DFD5BB` | `#37304E` |
| 文字 · 主 | onSurface | `#2B2620` | `#E4DEF2` |
| 文字 · 次 | onSurfaceVariant | `#6B6152` | `#9C93B8` |
| 文字 · 弱（HUD 注释） | labelTertiary | `#A79B85` | `#5E5680` |
| 墨线（全局描边） | outline | `#2B2620` | `#0E0C16` |
| 描边弱化/虚线底 | outlineVariant | `#C9BFA6` | `#37304E` |
| **HP 绿**：主操作/进度正常 | primary | `#46C168` | `#5BD385` |
| **XP 蓝**：经验/链接/支线 | secondary | `#3E9BFF` | `#66B0FF` |
| **金币金**：奖励/选中/复活 | tertiary | `#F5B301` | `#FFC53D` |
| **金墨**：成就大数字（昼深金） | MmGoldInk | `#B98A00` | `#FFC53D` |
| **危险红**：删除/放弃/血条<30% | error | `#E5484D` | `#FF6B6F` |
| **限时橙**：血条 30–60% | MmAmber | `#F5A623` | `#FFB84D` |
| **魔法紫**：主线徽章/节日 | MmManaPurple | `#9B6DFF` | `#B79AFF` |
| 输入内表面 | MmInputSurface | `#FFFFFF` | `#201B30` |
| 各色浅底（选中态） | *Container | 见 Color.kt | 见 Color.kt |

**血条三段变色规则（核心语义）**：剩余 >60% 绿 / 30–60% 橙 / <30% 红。紧迫度 = 血量，颜色变化即数据。

**昼→夜 token 映射的实现基准（design.html）**：`design.html` `:root` 已携带全部夜间 token（`--cave*`、`--hp-n/--xp-n/--gold-n/--danger-n`、`--amber-n`、`--mana-n`、`--cave-variant #37304E`、`--cave-input #201B30`，逐项对齐本表夜列）。页面底部夜间 11 屏由脚本实时克隆昼间 `.phone` 原稿（内容/结构/文案零差异），仅通过两层 CSS 重映射颜色：`.night:not(.light-preview)` 类级覆盖（面板/墨线/血条/经验格/checkbox/按钮/首页小卡 accent）与 `.night-override` 内联色重映射（`--panel→--cave-panel`、`#fff→--cave-input`、`ink 系→cave 系`、`gold/hp/xp/danger/mana/amber→-n 夜变体`、`gold-ink→gold-n`、虚线 `#E0D8C2→#37304E`、scrim→黑）。新增夜间视觉一律走此映射，不得为夜间单独发明结构或颜色。

### 3.2 字号（代码：`ui/theme/Type.kt`）

| 角色 | 规格 | 用途 |
|---|---|---|
| displayLarge | 64sp / ExtraBold / -1sp 字距 | 主线横幅大数字 |
| displayMedium | 48sp / ExtraBold | 详情页数字 |
| displaySmall | 34sp / ExtraBold | 任务卡数字 |
| headlineSmall | 19sp / ExtraBold | 页面标题（队伍编成/装备库） |
| titleMedium | 16sp / Bold | 任务名称 |
| bodyMedium | 15sp / Medium | 正文 |
| labelLarge | 14sp / ExtraBold / +0.5sp | 按钮 |
| labelMedium | 11sp / Bold / **Monospace** | HUD 标签 |
| labelSmall | 9sp / Bold / **Monospace** / +0.5sp | HP% · DUE · GROUP 等角标 |

首页①显示字体：HTML 使用 `-apple-system` / `PingFang SC`；Android 首页使用仓库现有 `lantinghei_tc_heavy.ttc` 的简体 Heavy 字形，并通过 `MmHomeDisplayFont` 统一引用。全局 `MmDisplayFont` 与其他页面保持 Android `SansSerif`。HUD 与日期元数据继续使用 `Monospace`。该字体资源的再发布许可证尚未在仓库中找到，需在发布前核验。

### 3.3 形状 / 间距 / 像素元素（代码：`ui/theme/Shapes.kt` + `MomentMarkTokens.kt`）

| Token | 值 | 用途 |
|---|---|---|
| radius（全局） | **0dp 直角** | 一切卡片/按钮/输入框 |
| PxBorderWidth | 3dp 墨色实线 | 所有元素描边 |
| PxThinBorderWidth | 2dp 细描边 | 血条 / 徽章 / 成就卡内嵌金线 |
| PxShadowOffset | 3dp 硬偏移影（右侧竖带含右下角 + 底部横带） | 卡片/按钮（与 HTML `.px` 的 3px 对应）；HTML box-shadow 是整块平移 (N,N)，右下角 offset×offset 必须填满，不得留缺角 |
| PxShadowOffsetLarge | 4dp | 主卡/弹窗（与 HTML `.px4` 的 4px 对应） |
| PxShadowOffsetSmall | 2dp | 底部导航选中项/加号等小元素硬影 |
| PressOffset | 2dp | 按压时位移+阴影缩短（实体感） |
| SpacePage / SpaceCard / SpaceInner | 14 / 10 / 12dp | 页面边距/卡间距/卡内边距 |
| SpaceCompact / SpaceInput | 8 / 10dp | 紧凑行与输入文本留白 |
| AchievementInset / InputMinHeight | 3 / 48dp | 成就金线留白、输入触摸高度 |
| FormInputCursor | 方块 **4dp × 8.3dp**、`1s` 硬切换闪烁（亮/灭各 0.5s）、行盒内垂直居中、仅聚焦时显示 | `design.html` 内联 `<span>▮</span>`（U+25AE）+ `@keyframes blink{50%{opacity:0}}` / `animation:blink 1s steps(1) infinite`；Chrome `measureText` 实测该字形墨迹 3.99×8.34px → 4.0×8.3dp。用 `drawRect` 绘制，不依赖兜底字体是否含 U+25AE |
| FormScrollPaddingBottom | 28dp | `.form-scroll` 内容底部留白。HTML 里提交按钮直接贴屏幕底边；App 内额外留白，避免按钮紧贴屏幕边缘/手势条 |
| HomeAchievementCard | padding `11/13/13dp`、间距 `4/6/3dp`、数字 `44sp/42sp`（兰亭黑内建行高偏高，padding bottom 从 10 加到 13 让 meta 行距 bottom border ≥10dp）、日期 `SINCE 9sp` 顶对齐标题、宽高比 `290:150`（1.933） | 首页①置顶成就卡；逐项对应 HTML `.ach` |
| HomeMainQuestCard | padding `11/13/11dp`、间距 `4dp`、HUD `9sp/13sp`、标题 `14sp/20sp`、数字 `44sp/46.2sp`、单位 `12sp/12.6sp`、日期 `UNLOCK 9sp` 顶对齐标题、宽高比 `290:116`（2.5，容纳兰亭黑 SC Heavy 实际行高） | 首页①置顶主线横幅卡；逐项对应 HTML `.px4` + 春节·回家的日子 |
| HomeSmallCard | padding `10/11/9dp`、标题 `12sp`、数字 `26sp`、数字/单位基线对齐、HP `10dp`、元数据 `8sp`、宽高比 `1.228`（140×114 实测）；右上角图标槽渲染真实 emoji（17sp），图标不求与设计稿逐一对齐；数字/单位同用血条 accent 三段色 | 首页①两列小卡共享骨架；进度比例与颜色实时由时间投影驱动 |
| NavPadding | 顶部 10dp / 水平 12dp / 底部 16dp | 底部导航容器内边距，逐项对应 HTML `.nav{padding:10px 12px 16px}` |
| HpBarHeight | 10dp | 血条 |
| XpCellCount × XpCellHeight | 16 格 × 12dp（gap 3dp） | 经验条（全年/总进度） |
| CheckboxSize | 15dp | 目标复选框 |
| TouchTargetMin | 44dp | 日期切换、复选框等关键触摸目标最小尺寸 |
| CompactIconSize | 28dp | 紧凑像素箭头/图标视觉盒（外层触摸目标仍为 44dp） |
| CheckboxStrokeWidth | 2.5dp | 复选框/虚线新增框描边 |
| BadgePadding | 2dp 垂直 / 6dp 水平 | `.badge-tag` 徽章内留白，避免描边贴字 |
| FormPanel | `.px` → padding `13/15dp`、`.px4` → `14/15dp`、`.pagetitle` → `13/15dp`、`.notebox` → `11/13dp`、日期格 → `10/11dp`、party chip → `6.5/11.5dp`；`.party-rail` gap 7dp；label→白框 7dp；RARITY 行与 chip 间距 10dp | ②③ 铭刻时刻表单面板内距，逐项由 `docs/design/visual/screen-02-add/design-geometry-*.json` 实测反推 |
| FormPanel 顶部骨架 | 返回方块 25dp（右间距 12dp、硬影 2dp）；方块内 `‹` 为 **Canvas 自绘多边形**（5.5×7dp、垂直下偏 1.5dp，顶点取自 design.html 逐行墨迹几何边界 —— Android 兜底字体的 U+2039 只有 3dp 宽，且放大字号会同时把高度拉过 7dp）；`.pagetitle` 内容行高 25dp；提交按钮 padding 15dp | 同上；`.back` 与 `.btn.px` 逐项对应 |
| FormPanel 文本 | NAME `14sp/20sp`（mono，字重 700）、notebox 正文 `12sp/19.8sp`、日期格 `15sp/18sp`、party chip `10sp/16sp`（emoji 与文字同为 10sp）、方向按钮高 66dp | 同上 |
| FormRarityRow | 行高 **21dp**；星串 `13/17/21sp` 递进、gap 2dp、基线对齐 | `.rarity-stars`。⚠ 该行触摸高度 21dp < `TouchTargetMin`，为对齐设计稿行高做的显式取舍，见 `组件改造.md` |
| FormTaskDateTimeCellRowGap / FormTaskDateTimeLabelToRowGap | `8dp` / `8dp` | design `.gap:8px` `.margin-top:8px`：截止日期面板 label 与两白框之间的间距 |
| FormTaskDateTimeCellWeightDate / FormTaskDateTimeCellWeightTime | `1.4f / 1f` | design 双白框 `flex:1.4` + `flex:1`；容纳 4 位年份 `2026.09.18` |
| FormTaskDifficultyRowMinHeight | `TouchTargetMin` (44dp) | 第 ④ 屏「▸ 难度 DIFFICULTY」行沿用 44dp 触摸目标 |
| DaybookTitlePanel | margin 0/12/10dp、padding 13/15dp（10+3 border 补偿）、box-shadow 3dp | 第 ⑤ 屏月份标题面板，对应 HTML `.pagetitle` |
| DaybookWeekdayHeader | 0dp 垂直 padding、margin-bottom 6dp、9sp mono ExtraBold | 第 ⑤ 屏 SU–SA 星期表头 |
| DaybookCalendarGap / DaybookCellFontSize | 4dp / 13sp ExtraBold | 第 ⑤ 屏日历格间距与字号；单元格 `aspectRatio(1)`、2dp 描边 |
| DaybookTodayShadowOffset | 2dp | 第 ⑤ 屏 today 格硬影，对应 `.dg.today{box-shadow:2px 2px 0 rgba(43,38,32,.4)}` |
| DaybookTaskPanel | margin-top 12dp、padding 14/16dp（11+3 / 13+3 border 补偿） | 第 ⑤ 屏「今日掉落」面板 |
| DaybookTaskRow | height 36dp、padding 8/0、border-bottom 2dp dashed surfaceVariant | 第 ⑤ 屏任务行；完成态标题 `onSurfaceVariant`、未勾选边框 `labelTertiary` |
| DaybookNewQuestRow | padding-top 9dp / padding-bottom 2dp、15dp 虚线框、primary 标题 | 第 ⑤ 屏「＋ 接受新任务（待办）」入口 |
| DetailBanner（.px4） | padding 15/16dp（12+3 / 13+3 border 补偿）、HUD 9sp mono、标题 17sp/800、大数字 48sp/800 lineHeight=48（须 `MmNoFontPadding`）、经验格顶距 4dp、PROGRESS/UNLOCK 8sp mono | 第 ⑥ 屏任务详情主横幅 |
| DetailPanel（LORE/OBJECTIVES） | margin-top 10dp、padding 14/16dp（11+3 / 13+3 border 补偿）、LORE 正文 13sp/600 lh22.1、目标行 padding 8/0 gap 9、行底 2dp 虚线 | 第 ⑥ 屏内容面板 |
| DetailMeta（DUE/GROUP/RARITY） | padding 13/16dp（10+3）、label 8sp mono、value 12sp mono、RARITY 星 12sp tertiary letterSpacing 2 | 第 ⑥ 屏元数据条 |
| DetailButtonRow | margin-top 12dp、gap 10dp、编辑 weight1 / 继续冒险 weight1.3、padding 10/0、13sp/800 | 第 ⑥ 屏双操作按钮 |
| DetailDeleteLink | margin-top 9dp、居中、9sp mono 800 error、下划线 | 第 ⑥ 屏「放弃任务（移入回收站）」 |
| AchievementBanner（.ach.px4） | padding **10/17dp**（vertical 10dp 使内容距金线 5dp，与 CSS 13px padding - 8px inset = 5px 一致；horizontal 14+3 border 补偿）、奖杯 34sp（MmNoFontPadding）、标签 9sp mono 800 gold letterSpacing 1.35sp topGap 2dp、标题 16sp/800 lh19 topGap 5dp、大数字 50sp/800 lh52.5（MmNoFontPadding，「已 N 天」同 Text buildAnnotatedString）、单位「天」13sp ink-2、SINCE 9sp mono ink-3 topGap 2dp；金线 inset **5dp** + 2dp gold border（AchievementFrame，内容 padding 10dp > 金线 inset 5dp，内容在金线内 5dp） | 第 ⑦ 屏成就详情主横幅；对应 HTML `.ach.px4` + `::before` 金线 |
| MilestonePanel | margin-top 10dp、padding 14/16dp（11+3 / 13+3 border 补偿，同 DetailPanel）、label 9sp mono | 第 ⑦ 屏里程碑面板 |
| MilestoneRow | gap 9dp、普通行 padding 7/0 + 行底 2dp dashed surfaceVariant、下一项行 padding 8/0/4 + 血条 topGap 7dp、最后一行 padding 8/0/2 无虚线；展示规则：最近 3 已达成 + 下一项 + 1 远征中（共 5 项，ViewModel.trimMilestones 截断） | 第 ⑦ 屏里程碑行；对应 HTML `.ml` |
| MilestoneCheckbox | 15dp、已达成=绿填充墨边白✓、下一项=金半格渐变(135°)金边、远征中=空框 ink-3 边 | 第 ⑦ 屏里程碑 checkbox；对应 HTML `.cb` |
| Milestone 文本 | label 12sp/800（已达成 ink / 下一项 gold / 远征中 ink-2）、detail 9sp mono ink-3（已达成「日期 达成」或周年「日期 · N 天」、下一项「还有 N 天 · 日期」、远征中「日期 · 远征中」）、status 9sp mono（已达成绿✓ / 下一项金%） | 第 ⑦ 屏里程碑行文本 |
| MapFilter（第 ⑧ 屏分类抽屉·搜索） | 搜索白框 `.px{padding:9px 12px}` → 12/15dp、行 gap 9dp、⌕ 14sp 蓝、placeholder 13sp/700 ink-3、ESC 徽章 mono 8sp + 2dp 边 + 3/8dp 内距、容器 12dp 边距 + 底 8dp；背景列表 opacity .35、padding 14/12dp、gap 9dp、行卡 13/15dp + 标题 13sp/800 + meta mono 9sp mt3；scrim `rgba(43,38,32,.45)`（MapFilterScrimAlpha，区别于通用 AlphaScrim .5）；抽屉 `.px4` left10/top6/bottom6/宽222dp、padding 15dp、列 gap 11dp、标题 mono 11sp/800/.15em、区段 label mono 8sp/800/.2em mb6、chips mono 9sp/800 + 2.5dp 边 + 6.5/10.5dp、分组行 mono 10sp/800 + 2.5dp 边 + 7.5/10.5dp + gap 6dp、管理按钮 mono 10sp/800 + 10dp 内距；内容区任意位置右滑、开阈 32dp | 第 ⑧ 屏「分类抽屉 · 搜索」全部版面值（`MomentMarkTokens.MapFilter*`）；左缘额外保留 48dp 系统返回手势排除条带，但不限制中部右滑 |
| Settings（第 ⑨ 屏设置 SYSTEM · 昼） | 内容容器 padding 0/12dp + gap 10dp；自定义状态栏 padding 11/18/5dp + mono 11sp/800 ink-2；冒险者卡 `.px` padding 15/16dp（12+3/13+3 border 补偿）+ row gap 12dp、头像 52dp 方块 `MmGoldDark`(#FFC53D) 背景 + 3dp 墨边 + 🧙 24sp、标题 15sp/800、LV 行 mono 9sp goldInk(#B98A00) mt2dp、XP 条 8 格×12dp 高 gap3dp + 2dp 边（空格 background 色、填格 secondary 蓝）mt7dp、EXP 文字 mono 8sp labelTertiary mt4dp；显示模式面板无硬影 padding 14/16dp（11+3/13+3）、label mono 9sp/800/.2em labelTertiary、按钮行 gap7dp mt8dp、按钮 flex 1/1/1.2 + padding 10dp（7+3 border 补偿）+ mono 10sp/800，选中项 `MmGoldDark` 背景+边+onSurface 墨色字；设置列表面板无硬影无内边距、内容加 3dp border 补偿 padding、行 padding 11/13dp（行本身无 border 不做补偿）+ space-between + 2dp 虚线分隔（横跨全宽）、label 13sp/700 onSurface、trailing mono 10sp/800（OFF=labelTertiary / ON=primary 绿 / 链接=onSurfaceVariant）；footer mono 8sp/800/.15em labelTertiary 居中；底部 spacer 10dp | 第 ⑨ 屏「设置 SYSTEM · 昼」全部版面值（`MomentMarkTokens.Settings*`）；头像与选中按钮用 `MmGoldDark`(#FFC53D) 而非昼间 tertiary(#F5B301)，因 design.html `background:var(--gold-n)` 不被 `.light-preview` 覆盖 |
| **DesignCanvasWidth / Height** | **314 / 648 dp** | `design.html` `.phone{width:330px;height:664px;border:8px}` 的**内容区**。本表所有字号 / 间距 / 描边都在这块画布上标定，例如首页双列小卡宽 = `(314 − 2×12 − 10) ÷ 2 = 140dp` |
| **PixelCanvas**（`ui/components/PixelCanvas.kt`） | 缩放系数 `s = 设备可用宽度 ÷ 314dp`；**只覆盖 `LocalDensity.density`，`fontScale` 保持不变**；无上下界 | 整页等比缩放容器，包在 `MomentMarkApp` 主题内、含底部导航。基准设备（可用宽 314dp，即 628px @ density 320）`s` 恰为 `1.0f`，`Density` 数值不变、逐像素零回归；384dp 设备 → `1.223×` |

> **边框补偿规则（强制）**：Compose 的 `Modifier.border` 绘制在盒子内部、**不占布局尺寸**，而 HTML 的 `border` 会让盒子变大。因此凡是用 `PixelPanel` 复刻带 `border` 的 HTML 元素，Compose 侧 padding 必须取 **「HTML padding + HTML border-width」**，否则内容会比设计稿外扩一个 border 宽度、整页节奏逐块累积偏移。②③ 屏首次系统性应用该规则（修正前有 11–13px 累积偏移）。
>
> **描边盒必须撑满固定尺寸（强制）**：`PixelPanel` 默认按内容高度绘制描边；当外层由 `Modifier.size(...)` 固定了高度、而内容比它矮时（典型如 `.back` 返回方块：外层 25dp、内部字形仅 7dp），**必须传 `fillContent = true`**。否则 HTML 的 `border` 包住整块 25dp、而 Compose 只包住 7dp，硬影仍按 25dp 计算，方块下方会出现一段与边框错位的「悬空阴影」（②③ 屏实测：方块矮 4dp + 右下多出 8px 孤立阴影条）。
>
> **横竖比例必须用等比缩放对齐，不得只锁 `aspectRatio`（强制）**：卡片外框走 `weight(1f)` / `fillMaxWidth` 时宽度随屏宽线性变化，而字号 / 内距 / 描边是固定 dp —— 在非 314dp 设备上宽高比必然失真（384dp 实测：小卡 175×114dp、比例 1.535，设计值 1.228）。**唯一正确的做法是 `PixelCanvas` 整页等比缩放**，让整棵树落在 314×648 的设计坐标系里。
>
> 两个已踩过的坑：
> 1. **不能只缩放 `fontScale`+`density`**。`sp` 换算像素用的是 `sp × fontScale × density`，两者都乘 `s` 会让字号被放大 `s²`（384dp 实测标题墨迹带 20.03dp vs 基准 17.00dp，大了 18%）。只乘 `density` 才能让字号与 dp 同比例。
> 2. **缩放基准是「画布内容区宽度 314dp」，不是屏宽**。`.phone` 的 8px 边框不在内容区里；若误用 330，所有几何会整体偏小 5%。

### 3.4 动效与材质

| 场景 | 规格 |
|---|---|
| 按钮按压 | 位移 2dp + 阴影缩短，100ms（不用透明度） |
| 血条变化 | 阶梯式减血 200ms，**不做平滑补间** |
| 任务完成 | 标题删除线 + 填充 checkbox + `CLEAR +5 EXP`；可选 ✦ 闪光/卡片反馈 |
| 删除动效 | 卡片像素化碎裂飞散 |
| 数字变化 | **直接跳变**（8-bit 无补间） |
| 装备库切卡 | 相邻卡 38% 透明度 + 透视变换（沿用既有拖拽 governor 交互） |
| 装备库相邻卡 | AlphaStackedCard 0.38 |
| 压暗层 | AlphaScrim 0.5 |

## 4. 组件规格

- **PixelPanel**：`Raised` 为普通卡/按钮的 3dp 硬影，`Emphasized` 为主卡/弹窗的 4dp 硬影，`Flat` 只用于被外层描边承载的内嵌容器；不得再以 surface 色充当不可见阴影。
- **PixelTextInput**：标签位于字段外层，输入区使用输入内表面 + 3dp 墨线；聚焦显示主题墨色 2dp 阶梯闪烁方块光标，并统一 selection/error 语义。普通文本仍按字段语义保存；Moment 日期由 `PixelDateSelector` 的年/月/日三格合并为单个 `LocalDate`。
- **PixelCursor（全 App 统一方块光标）**：`rememberPixelCursorVisible` + `Modifier.pixelBlockCursor`（4×8.3dp 方块、1s 硬切换、行盒内垂直居中）。所有可输入文本的框——`PixelTextInput`（时刻/任务名称与描述）和第 ④ 屏日期/时间白框（`DueCell`）——都必须用它，**禁止**让任何输入框回落到系统默认细线光标；使用处必须把 `cursorBrush` 置透明避免双光标。输入框内一律不预填 design.html 的示例文案（那只是 HTML 稿展示效果），空态只显示 placeholder 引导语。
- **PixelDateSelector（Moment）**：年/月/日为三个独立可操作选择格，等宽比例由父容器 `weight`/约束决定；提交时合并为单个 `LocalDate`。左上角固定显示 `[x] 全天`，本轮不提供取消全天或时分字段。
- **PixelHpBar**：10dp 高、2dp 墨线，填充从边框内开始，沿用真实时间派生的绿/橙/红阈值。
- **AchievementFrame**：`Emphasized` 墨线框和硬影内，距离外框 3dp 的 2dp 金线；仅表达过去 Moment 的成就语义。
- **任务卡（未来方向）**：panel 面 + 3dp 墨线 + 3dp 硬影；行 1 = 名称 + 类型徽章（◆主线紫/◇支线蓝/⧗限时橙，2dp currentColor 描边）；行 2 = 大数字 + 难度星级（★金）；血条；mono 元数据行（HP% · DUE · GROUP）。
- **成就卡（过去方向，首页）**：通过 `AchievementFrame` 内嵌 2dp 金线（inset 3dp）；徽章=「🏆 成就 · 正数」（金）；大数字用 gold 深色（昼 #B98A00）文案「已 X 天」+ SINCE 角标。当前下一固定里程碑可显示真实金色时间条，不得伪造进度。
- **首页双列小卡**：过去/未来共用同一组件和相同网格节奏：固定 25px 分组图标格、等高标题行、对齐的大数字行、血条和元数据行；图标由 `groupId → Group.iconKey` 解析为**真实 emoji**（旅行 ✈️ / 工作 💼 / 生活·家庭 💞 / 学业 🎓 / 无分组 ✨），无分组显示统一默认 emoji。过去语义使用「X 天了」，其中「天了」与数字同色，不在标题行另放状态字。血条仅按时间数字的展示紧迫度取绿（长）/橙（中）/红（短）和不同长度；阈值逻辑后续另定。
- **铭刻时刻页（新时刻表单）**：顶部**方向开关**（⧖未来·倒数 ⇄ 🏆过去·正数，两个等宽大直角块，纵向图标+文字，选中侧填色：蓝/金，下方一行方向说明文案）；字段顺序：名称 → 日期（标签随方向：目标日期/开始日期，使用年/月/日三个独立选择格；左上角固定显示 `[x] 全天`，不提供时分）→ **备注**（未来=「任务背景 LORE」/ 过去=「成就故事 STORY」，均选填，白底 3dp 墨线输入框，未来显示在任务详情页、过去显示在成就详情页）→ 分组线路（HTML 风格线路 chip）→ 重要度 RARITY（★金）；CTA 随方向换色换文案（蓝「⧖ 铭刻 · 开始倒数！」/ 金「🏆 铭刻 · 开始珍藏！」）。周年提醒仍不在表单出现。
- **新任务页（待办表单，仅副本地图入口）**：顶部使用与铭刻页一致的返回像素按钮，按钮与标题保留间隔；此表单、两种铭刻表单均不显示底部导航。默认带时刻（日期+时间两框）；**备注=「任务描述 LORE」（选填，提示文案"攻略/备注/链接"）**；难度徽章；绿色 CTA「▶ 开始执行」。
- **成就详情页**：金框横幅（🏆 · 成就珍藏中 · 已 X 天 · SINCE）+ 成就故事 + 固定里程碑只读面板 + [✎ 编辑][🏆 珍藏中]。里程碑行显示已达成/下一项/远征中；当前项使用金色时间条，不提供可点击复选框。
- **成就详情扩展（J ⑦，本轮）**：里程碑由 `anchorDate + Clock` 派生固定节点（100、365、1000、1200、2000…天）和周年节点；2 月 29 日开始的 Moment 在非闰年不生成周年项。周年提醒只有通知链路交付后才显示。
- **分组管理（J ⑩，本轮）**：`队伍编成` 从系统设置进入，不新增底部导航项。每个分组拥有一个内置图标，创建/编辑时与线路色一并选择；分组卡使用图标、线路色 token 色块、名称、活动 Moment/Task 类型计数，以及独立的改名/解散像素按钮；表单使用等宽线路 chip；解散说明必须明确“变为无阵营、不删除条目”。颜色、间距、描边、阴影和触摸目标均复用本表 token，禁止新增裸 `Color(0x...)`。
- **置顶主线宽卡（首页）**：未来方向置顶 Moment 使用 `HomeMainQuestCard`（`.px4` 面板、3dp 墨线、4dp 硬影、紫 ◆ MAIN QUEST 徽章、倒数大数字、UNLOCK 日期）；过去方向置顶 Moment 仍用 `AchievementFrame` + `HomeAchievementCard` 金框成就卡。两者均独占首页首部整行，显示真实标题、日期、状态与必要字段；不恢复旧首页 Hero、EXP 预告或自由卡片墙。
- **按钮**：直角 + 3dp 墨线 + 硬影；绿色=主操作、蓝色=编辑/次级、金色=奖励/复活/新时刻、红=危险、ghost=取消。文字一律 labelLarge。
- **底部导航（固定三入口，`ui/components/PixelBottomNav`）**：panel 底 + 顶部 3dp 墨线；普通项 = 页面底色 + 3dp 墨线 + labelMedium(mono) 次级文字；选中项 昼 = 墨底金字（inverseSurface + tertiary）/ 夜 = 金底洞窟墨字（tertiary + outline），带 2dp 40% 墨影；「＋ 新时刻」金底墨字、宽约普通项 1.4 倍 + 2dp 实心墨影；按压 = 位移 2dp + 影缩至 0（100ms，不用透明度）。导航项带中文 content description 与 selected 语义；夜态由 `LocalMomentMarkNight` 提供。
- **底部导航加号**：金色「＋ 新时刻」，两页一致；「＋ 新任务」仅作为副本地图内今日掉落列表末尾的虚线框行（绿色虚线 + 绿字）。
- **确认弹窗（封印/净化）**：共享 `PixelConfirmationDialog`；⚠️ + 标题 + 说明（"将被封存进回收站，30 天内可复活"）+ [继续冒险(ghost)] [✕ 确认放弃/确认封印/确认净化(红)]，全程使用直角像素面板、3dp 描边与硬偏移影。
- **Toast（系统消息）**：墨底金字，"⚔「xx」已封印入封印之地 ↩ 撤销"。
- **装备库（模板浏览）**：保留现有透视堆叠交互，文案改为装备/皮肤语义，当前卡带 "EQUIPPED ✓" 金标。
- **抽屉（地图筛选，第 ⑧ 屏，`ui/moment/home/MomentFilterDrawer.kt`）**：首页内容区任意位置右滑呼出，scrim/ESC/返回键关闭；累计横向 32dp 即展开，无需左缘停顿或等待抬手；横向位移必须大于纵向位移，避免干扰滚动。展开时 HUD 右槽切为 `MAP FILTER`。结构 = 顶部搜索白框（⌕ + placeholder「搜索任务与成就…」+ ESC 徽章；搜索行为 P1 未设计，仅占位）→ 45% 墨色 scrim + 35% 透明背景时刻列表 → 左侧 `.px4` 抽屉：状态段（全部 ×N / 倒数中 / 正数中，选中=墨底金字）+ 分组线路行（emoji + 名称 + ×N 计数，选中=线路色描边，真实模式恒为未选中态）+「⚙ 管理分组 →」（进系统设置·队伍编成）。分组行数据 = Room 分组（sortOrder）+ 按分组名聚合的活动 Moment 计数；筛选/搜索逻辑属 P1，chips 与分组行纯展示不可点。
- **设置页（当前昼间主稿）**：冒险者卡（头像块 + LV + 连续天数🔥 + EXP 条）→ 显示模式三态（沿用 SYSTEM/LIGHT/DARK）→ 开关行 `[x]/[ ]` mono → 关于（VERSION）；夜间样式暂存而不作为主稿。
- **日子簿（副本地图）**：当前主稿使用昼间任务板面板；今日=金色块；有任务日=宝箱/内容标记 🎁；当日列表=「今日掉落」紧凑可勾选行。未完成标题使用高对比墨色；完成行必须标题危险红删除线、checkbox 填充、右侧 `CLEAR +5 EXP`；未完成项始终显示截止信息，非当日显示日期加时间，当日无精确时间显示「全天」。日格、方向块、日期三格和任务行的宽度/间距均按父容器百分比或权重布局，不能写死屏幕尺寸。

## 5. 主题规则

- 昼间是当前默认主题，所有当前页面必须完整使用昼间表面、描边和文字 token。
- 当前页面禁止混用昼间和夜间表面色；不得因页面上下文强制切换夜间主题。
- 夜间 token 可以保留在主题层，但只作为后续适配，不进入当前视觉验收。
- 夜间参考稿（`design.html` 底部）与昼间 11 屏逐屏对应：只允许按 §3.1 的 昼→夜 映射替换颜色，不得增删结构或文案；昼间主稿像素不得因夜间适配而变化（`design.html` 内由 `:not(.light-preview)` 与 `.night-override` 作用域保证）。
- 主题切换按钮可以暂留在设置页面作为非最终设计占位，不得宣称夜间主题已经完成，也不得让它影响当前默认页面复刻验收。

## 6. 交互契约（以 PRD.md 为准，本节为摘要）

产品行为以 `PRD.md` 与 `AGENTS.md` §1 为准，本节仅摘录视觉实现必须遵守的部分。**旧版契约（长按 500ms 布局编辑、拖拽 governor、Small/Wide 网格、搜索五状态、旧模板内容契约）已随旧首页退场，属 `AGENTS.md` §2.3 授权可删除的遗留实现。**

1. **Moment 时间规则**：未来模式仅可选今天/未来日期，过去模式仅可选今天/过去日期；目标日显示「就是今天」，次日起同一记录自动转为成就展示（不改 ID/日期/字段，不生成重复记录）；相对天数与展示状态只由 `EventTimeCalculator` 派生，不落库。
2. **Task 规则**：默认归属今天（快捷：今日/明日/本周六/自选）；勾选完成显示 `CLEAR` 且可取消；跨日未完成不迁移不删除、仅不出现在当日；首页右列仅在 `showOnHome` 开启时显示（日期近→远），到期日「就是今天」，过期从首页隐藏但保留在日子簿。
3. **首页排序**：置顶 Moment 每条独占一行置顶，可手动排序且持久化；未置顶过去 Moment 仅左列、未来仅右列，均由近到远。双列小卡右上角始终保留同尺寸图标槽，显示所属分组的内置图标；修改分组图标会同步更新组内卡片。图标不同不得改变图标槽尺寸、标题可用宽度或两列卡片的对称性。
4. **封印 / 复活 / 净化**：删除先二次确认 + 即时撤销；复活需恢复原始字段与状态；永久净化与清空回收站二次确认；自动净化默认关闭，开启后仅在下一次启动时经 `purgeDeletedBefore(cutoff)` 清理满 30 天项。
5. **无障碍与适配**：卡片合并为完整中文语义节点，装饰 icon 隐藏，导航有中文 content description；大字体缩放不裁剪主信息；系统栏 inset 正确。
6. **纯规划项**不可点击，不给空 onClick。

## 7. 存量迁移与清理状态

当前代码中存在旧「暖纸风格」的硬编码颜色（约 186 处 `Color(0x…)`）与散落的 dp/sp 值，集中在 `ui/home/EventCard.kt`、`HomeHero.kt`、`AdaptiveBackground.kt`、`CategoryDrawer.kt`、`eventdetail/`、`eventsettings/`、`ui/daybook/` 等文件。**迁移策略：渐进式**——

- 新写的 UI 一律用 token；
- 修改旧文件时，顺手把**触碰到的**硬编码值替换为最近的 token，不要求一次性全量迁移；
- 每完成一个页面的迁移，在本节登记；
- **删除授权**：旧 UI / 逻辑满足「`design.html` 当前设计等价替代完成 + 不再承担 P0 功能 + 数据安全核查通过」时，可在同一任务内删除；规则全文与前置约束见 `AGENTS.md` §2.3。删除须在下方登记表记录。

迁移登记（页面 → 日期）：
- 三入口导航骨架（`ui/components/PixelBottomNav` + `ui/moment`/`ui/task`/`ui/daybook` 占位页）→ 2026-08-28（新建即全 token，无硬编码色值/dp）
- 大事件首页时刻卡（`ui/moment/home/`：置顶全宽主线卡、过去成就卡、未来任务卡 + 血条）→ 2026-08-28（全 token；血条三段色 primary/amber/error）
- 大事件首页待办投影（`ui/moment/home/`：已开启首页显示且未过期的 Task 与未来 Moment 按日期混排右列）→ 2026-08-30（全 token；只读展示，任务编辑/完成仍在日子簿）
- 铭刻/接受任务表单（`ui/moment/MomentFormScreen` + `ui/task/TaskFormScreen`：新建/编辑共用，线路、重要度/难度、首页显示开关均写入各自实体）→ 2026-08-30（全 token；Moment 与 Task 不互相转换）
- 时刻统一详情（`ui/moment/detail/`：同一组件按派生状态展示 QUEST / ACHIEVEMENT，支持置顶切换）→ 2026-08-29（全 token；不接回旧详情页）
- 日子簿任务链路（`ui/daybook/DaybookScreen` + `ui/task/TaskFormScreen`：昼间月历、CLEAR 任务行、虚线新任务入口）→ 2026-08-29（全 token；未接入原型日子簿数据）
- 封印之地（`ui/recyclebin/` + `ui/system/SystemSettingsScreen`：Moment/Task 合并投影、复活、二次确认净化、启动时可选 30 天净化）→ 2026-08-30（全 token；封印/净化确认统一为 `PixelConfirmationDialog`，不接回旧 TimeEvent 回收站路径）
- 铭刻时刻表单 ②③屏（`ui/moment/MomentFormScreen` + `PixelPageHeader`/`PixelStatusHud`/`PixelTextInput`/`PixelDateSelector`/`GroupChipRail`/`PixelStarRating` 的表单变体）→ 2026-09-11（全 token；新增 FormPanel/FormPanel 顶部骨架/FormPanel 文本/FormRarityRow 四组 token；建立「border 补偿规则」；视觉产物见 `docs/design/visual/screen-02-add/`）
- 分类抽屉 ⑧屏（`ui/moment/home/MomentFilterDrawer.kt` + `MomentHomeScreen` 抽屉态：搜索栏、scrim、35% 背景列表、左抽屉、内容区右滑手势）→ 2026-09-15（全 token；新增 MapFilter* 系列 token；分组行接 Room 分组 + Moment 计数；搜索/筛选为 P1 纯展示；视觉产物见 `docs/design/visual/screen-08/`）

删除登记（旧文件 → 替代屏 → 删除日期）：
- `ui/recyclebin/RecycleBinFeature.kt`（旧 `TimeEvent` 回收站包装层）→ `ui/recyclebin/RecycleBinViewModel.kt` + `RecycleBinScreen.kt`（Moment/Task 独立表投影）→ 2026-08-29
- `ui/home/`、`ui/eventdetail/`、`ui/eventsettings/`、`ui/settings/`、旧 `TimeEvent`/模板仓储与自由拖拽测试 → `ui/moment/`、`ui/task/`、`ui/daybook/`、`ui/system/`（J 三入口及独立 Moment/Task 数据层）→ 2026-08-30
- `ui/components/TimelineNavigation.kt`（无路由/测试引用的暖纸圆角时间线控件）→ 无替代；J P0 无对应入口 → 2026-08-30

## 8. 修改流程（任何风格调整必须走）

1. 改 `ui/theme/` 中对应 token（或新增成对 token）；
2. 同步更新本文档 §3 对应行；
3. 若影响组件结构，更新 §4 与根目录 `design.html` 的对应屏幕说明；
4. 按 `PRD.md` §12 和 `组件改造.md` §9 生成两组截图、差异图和量化报告；
5. 跑 `./gradlew assembleDebug`，并单独报告逻辑、模拟器、真机和无障碍验证范围。
### 6.7 分类抽屉手势

- 首页内容区任意位置的右滑均可呼出分类抽屉；累计横向位移达到 32dp 时立即展开，无需在左缘停顿或等待抬手。
- 手势在超过 touch slop 前不接管事件；超过后仅在横向位移大于纵向位移时接管，保证上下滚动与卡片点按保持原行为。
- 左缘仍保留 48dp 系统返回手势排除条带以兼容手势导航，但这不是抽屉的触发范围。
