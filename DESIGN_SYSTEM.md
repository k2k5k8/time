# DESIGN_SYSTEM — 刻间「像素冒险 Pixel Quest」

> **本文档是设计风格的唯一权威来源。** 任何 UI 改动的颜色、字号、间距、圆角、动效取值必须来自本表的 token（代码位于 `ui/theme/`），禁止凭感觉新造值。修改风格 = 「token + 本文档」同步改。AI 协作规则见 `CLAUDE.md`。
>
> 视觉基准（HTML 预览）：[proposal_J_final.html](docs/design/proposal_J_final.html)（J 最终完整稿）。`proposal_J.html` 与 `proposal_J_app.html` 仅作历史参考，不得反向覆盖最终稿。

## 1. 设计定位

**一句话定位：人生是一场冒险，每个日子都是一张任务卡。**

- 关键词：任务卡 · HP 血条 · 经验格 · 宝箱 · 8-bit 直角 · 硬偏移阴影 · mono HUD 标签
- 双模式叙事：**昼 = 任务板（冒险地图）**，**夜 = 洞窟（副本）**
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
| 分组 | 队伍/线路 Party Line | 颜色 = 线路色 |
| 删除（软删除） | 封印（移入封印之地） | 任务确认弹窗「放弃这个任务？」；成就为「封印此成就」 |
| 回收站 | 封印之地 GRAVEYARD（夜模式） | 30 天后永久净化 |
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

### 3.3 形状 / 间距 / 像素元素（代码：`ui/theme/Shapes.kt` + `MomentMarkTokens.kt`）

| Token | 值 | 用途 |
|---|---|---|
| radius（全局） | **0dp 直角** | 一切卡片/按钮/输入框 |
| PxBorderWidth | 3dp 墨色实线 | 所有元素描边 |
| PxThinBorderWidth | 2dp 细描边 | 血条 / 徽章 / 成就卡内嵌金线 |
| PxShadowOffset | 3dp 硬偏移影 | 卡片/按钮（与 HTML `.px` 的 3px 对应） |
| PxShadowOffsetLarge | 4dp | 主卡/弹窗（与 HTML `.px4` 的 4px 对应） |
| PxShadowOffsetSmall | 2dp | 底部导航选中项/加号等小元素硬影 |
| PressOffset | 2dp | 按压时位移+阴影缩短（实体感） |
| SpacePage / SpaceCard / SpaceInner | 14 / 10 / 12dp | 页面边距/卡间距/卡内边距 |
| SpaceCompact / SpaceInput | 8 / 10dp | 紧凑行与输入文本留白 |
| AchievementInset / InputMinHeight / CursorWidth | 3 / 48 / 2dp | 成就金线留白、输入触摸高度、方块光标 |
| HpBarHeight | 10dp | 血条 |
| XpCellCount × XpCellHeight | 16 格 × 12dp（gap 3dp） | 经验条（全年/总进度） |
| CheckboxSize | 15dp | 目标复选框 |
| TouchTargetMin | 44dp | 日期切换、复选框等关键触摸目标最小尺寸 |
| CompactIconSize | 28dp | 紧凑像素箭头/图标视觉盒（外层触摸目标仍为 44dp） |
| CheckboxStrokeWidth | 2.5dp | 复选框/虚线新增框描边 |
| BadgePadding | 2dp 垂直 / 6dp 水平 | `.badge-tag` 徽章内留白，避免描边贴字 |

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
- **PixelDateSelector（Moment）**：年/月/日为三个独立可操作选择格，等宽比例由父容器 `weight`/约束决定；提交时合并为单个 `LocalDate`。左上角固定显示 `[x] 全天`，本轮不提供取消全天或时分字段。
- **PixelHpBar**：10dp 高、2dp 墨线，填充从边框内开始，沿用真实时间派生的绿/橙/红阈值。
- **AchievementFrame**：`Emphasized` 墨线框和硬影内，距离外框 3dp 的 2dp 金线；仅表达过去 Moment 的成就语义。
- **任务卡（未来方向）**：panel 面 + 3dp 墨线 + 3dp 硬影；行 1 = 名称 + 类型徽章（◆主线紫/◇支线蓝/⧗限时橙，2dp currentColor 描边）；行 2 = 大数字 + 难度星级（★金）；血条；mono 元数据行（HP% · DUE · GROUP）。
- **成就卡（过去方向，首页）**：通过 `AchievementFrame` 内嵌 2dp 金线（inset 3dp）；徽章=「🏆 成就 · 正数」（金）；大数字用 gold 深色（昼 #B98A00）文案「已 X 天」+ SINCE 角标。当前下一固定里程碑可显示真实金色时间条，不得伪造进度。
- **铭刻时刻页（新时刻表单）**：顶部**方向开关**（⧖未来·倒数 ⇄ 🏆过去·正数，两个等宽大直角块，纵向图标+文字，选中侧填色：蓝/金，下方一行方向说明文案）；字段顺序：名称 → 日期（标签随方向：目标日期/开始日期，使用年/月/日三个独立选择格；左上角固定显示 `[x] 全天`，不提供时分）→ **备注**（未来=「任务背景 LORE」/ 过去=「成就故事 STORY」，均选填，白底 3dp 墨线输入框，未来显示在任务详情页、过去显示在成就详情页）→ 分组线路（HTML 风格线路 chip）→ 重要度 RARITY（★金）；CTA 随方向换色换文案（蓝「⧖ 铭刻 · 开始倒数！」/ 金「🏆 铭刻 · 开始珍藏！」）。周年提醒仍不在表单出现。
- **新任务页（待办表单，仅副本地图入口）**：与铭刻页区分——顶部**快捷日期 chips**（今日/明日/本周六/自选）；默认带时刻（日期+时间两框）；**备注=「任务描述 LORE」（选填，提示文案"攻略/备注/链接"）**；类型/难度徽章；页脚说明「完成后记入今日掉落，不占首页卡位」；绿色 CTA「▶ 接受任务！」。
- **成就详情页**：金框横幅（🏆 · 成就珍藏中 · 已 X 天 · SINCE）+ 成就故事 + 固定里程碑只读面板 + [✎ 编辑][🏆 珍藏中]。里程碑行显示已达成/下一项/远征中；当前项使用金色时间条，不提供可点击复选框。
- **成就详情扩展（J ⑦，本轮）**：里程碑由 `anchorDate + Clock` 派生固定节点（100、365、1000、1200、2000…天）和周年节点；2 月 29 日开始的 Moment 在非闰年不生成周年项。周年提醒只有通知链路交付后才显示。
- **分组管理（J ⑩，本轮）**：`队伍编成` 从系统设置进入，不新增底部导航项。分组卡使用线路色 token 色块、名称、活动 Moment/Task 类型计数，以及独立的改名/解散像素按钮；表单使用等宽线路 chip；解散说明必须明确“变为无阵营、不删除条目”。颜色、间距、描边、阴影和触摸目标均复用本表 token，禁止新增裸 `Color(0x...)`。
- **置顶主线宽卡（首页）**：置顶 Moment 独占首页首部整行，显示真实标题、日期、正数/倒数状态与必要字段；不恢复旧首页 Hero、EXP 预告或自由卡片墙。
- **按钮**：直角 + 3dp 墨线 + 硬影；绿色=主操作、蓝色=编辑/次级、金色=奖励/复活/新时刻、红=危险、ghost=取消。文字一律 labelLarge。
- **底部导航（固定三入口，`ui/components/PixelBottomNav`）**：panel 底 + 顶部 3dp 墨线；普通项 = 页面底色 + 3dp 墨线 + labelMedium(mono) 次级文字；选中项 昼 = 墨底金字（inverseSurface + tertiary）/ 夜 = 金底洞窟墨字（tertiary + outline），带 2dp 40% 墨影；「＋ 新时刻」金底墨字、宽约普通项 1.4 倍 + 2dp 实心墨影；按压 = 位移 2dp + 影缩至 0（100ms，不用透明度）。导航项带中文 content description 与 selected 语义；夜态由 `LocalMomentMarkNight` 提供。
- **底部导航加号**：金色「＋ 新时刻」，两页一致；「＋ 新任务」仅作为副本地图内今日掉落列表末尾的虚线框行（绿色虚线 + 绿字）。
- **确认弹窗（封印/净化）**：共享 `PixelConfirmationDialog`；⚠️ + 标题 + 说明（"将被封存进回收站，30 天内可复活"）+ [继续冒险(ghost)] [✕ 确认放弃/确认封印/确认净化(红)]，全程使用直角像素面板、3dp 描边与硬偏移影。
- **Toast（系统消息）**：墨底金字，"⚔「xx」已封印入封印之地 ↩ 撤销"。
- **装备库（模板浏览）**：保留现有透视堆叠交互，文案改为装备/皮肤语义，当前卡带 "EQUIPPED ✓" 金标。
- **抽屉（地图筛选）**：状态段（全部/进行中/已完成，选中=墨底金字）+ 分组线路列表（色块 + ×N 计数，选中=线路色描边）。
- **设置页（夜模式建议）**：冒险者卡（头像块 + LV + 连续天数🔥 + EXP 条）→ 显示模式三态（沿用 SYSTEM/LIGHT/DARK）→ 开关行 `[x]/[ ]` mono → 关于（VERSION）。
- **日子簿（副本地图）**：夜=洞窟面板；今日=金色块；有任务日=宝箱/内容标记 🎁；当日列表=「今日掉落」紧凑可勾选行。完成行必须标题删除线、checkbox 填充、右侧 `CLEAR +5 EXP`；未完成全天显示 `TODO`，有截止时间显示时间。日格、方向块、日期三格和任务行的宽度/间距均按父容器百分比或权重布局，不能写死屏幕尺寸。

## 5. 深浅色规则

- 昼（任务板）为默认模式；夜（洞窟）不是简单反色：面板 `#262138` 紫黑、描边用更深的 `#0E0C16`、金色在夜间提亮为 `#FFC53D`。
- 同一屏幕禁止混用两套模式的表面色。
- 游戏色昼/夜成对取值（见 3.1），禁止只调亮度。
- 主题模式持久化沿用 DataStore `moment_mark_settings` 的 `SYSTEM/LIGHT/DARK`。

## 6. 交互契约（以 PRD.md 为准，本节为摘要）

产品行为以 `PRD.md` 与 `AGENTS.md` §1 为准，本节仅摘录视觉实现必须遵守的部分。**旧版契约（长按 500ms 布局编辑、拖拽 governor、Small/Wide 网格、搜索五状态、旧模板内容契约）已随旧首页退场，属 `AGENTS.md` §2.3 授权可删除的遗留实现。**

1. **Moment 时间规则**：未来模式仅可选今天/未来日期，过去模式仅可选今天/过去日期；目标日显示「就是今天」，次日起同一记录自动转为成就展示（不改 ID/日期/字段，不生成重复记录）；相对天数与展示状态只由 `EventTimeCalculator` 派生，不落库。
2. **Task 规则**：默认归属今天（快捷：今日/明日/本周六/自选）；勾选完成显示 `CLEAR` 且可取消；跨日未完成不迁移不删除、仅不出现在当日；首页右列仅在 `showOnHome` 开启时显示（日期近→远），到期日「就是今天」，过期从首页隐藏但保留在日子簿。
3. **首页排序**：置顶 Moment 每条独占一行置顶，可手动排序且持久化；未置顶过去 Moment 仅左列、未来仅右列，均由近到远。
4. **封印 / 复活 / 净化**：删除先二次确认 + 即时撤销；复活需恢复原始字段与状态；永久净化与清空回收站二次确认；自动净化默认关闭，开启后仅在下一次启动时经 `purgeDeletedBefore(cutoff)` 清理满 30 天项。
5. **无障碍与适配**：卡片合并为完整中文语义节点，装饰 icon 隐藏，导航有中文 content description；大字体缩放不裁剪主信息；系统栏 inset 正确。
6. **纯规划项**不可点击，不给空 onClick。

## 7. 存量迁移与清理状态

当前代码中存在旧「暖纸风格」的硬编码颜色（约 186 处 `Color(0x…)`）与散落的 dp/sp 值，集中在 `ui/home/EventCard.kt`、`HomeHero.kt`、`AdaptiveBackground.kt`、`CategoryDrawer.kt`、`eventdetail/`、`eventsettings/`、`ui/daybook/` 等文件。**迁移策略：渐进式**——

- 新写的 UI 一律用 token；
- 修改旧文件时，顺手把**触碰到的**硬编码值替换为最近的 token，不要求一次性全量迁移；
- 每完成一个页面的迁移，在本节登记；
- **删除授权**：旧 UI / 逻辑满足「J 最终设计等价替代完成 + 不再承担 P0 功能 + 数据安全核查通过」时，可在同一任务内删除；规则全文与前置约束见 `AGENTS.md` §2.3。删除须在下方登记表记录。

迁移登记（页面 → 日期）：
- 三入口导航骨架（`ui/components/PixelBottomNav` + `ui/moment`/`ui/task`/`ui/daybook` 占位页）→ 2026-08-28（新建即全 token，无硬编码色值/dp）
- 大事件首页时刻卡（`ui/moment/home/`：置顶全宽主线卡、过去成就卡、未来任务卡 + 血条）→ 2026-08-28（全 token；血条三段色 primary/amber/error）
- 大事件首页待办投影（`ui/moment/home/`：已开启首页显示且未过期的 Task 与未来 Moment 按日期混排右列）→ 2026-08-30（全 token；只读展示，任务编辑/完成仍在日子簿）
- 铭刻/接受任务表单（`ui/moment/MomentFormScreen` + `ui/task/TaskFormScreen`：新建/编辑共用，线路、重要度/难度、首页显示开关均写入各自实体）→ 2026-08-30（全 token；Moment 与 Task 不互相转换）
- 时刻统一详情（`ui/moment/detail/`：同一组件按派生状态展示 QUEST / ACHIEVEMENT，支持置顶切换）→ 2026-08-29（全 token；不接回旧详情页）
- 日子簿任务链路（`ui/daybook/DaybookScreen` + `ui/task/TaskFormScreen`：夜间月历、CLEAR 任务行、虚线新任务入口）→ 2026-08-29（全 token；未接入原型日子簿数据）
- 封印之地（`ui/recyclebin/` + `ui/system/SystemSettingsScreen`：Moment/Task 合并投影、复活、二次确认净化、启动时可选 30 天净化）→ 2026-08-30（全 token；封印/净化确认统一为 `PixelConfirmationDialog`，不接回旧 TimeEvent 回收站路径）

删除登记（旧文件 → 替代屏 → 删除日期）：
- `ui/recyclebin/RecycleBinFeature.kt`（旧 `TimeEvent` 回收站包装层）→ `ui/recyclebin/RecycleBinViewModel.kt` + `RecycleBinScreen.kt`（Moment/Task 独立表投影）→ 2026-08-29
- `ui/home/`、`ui/eventdetail/`、`ui/eventsettings/`、`ui/settings/`、旧 `TimeEvent`/模板仓储与自由拖拽测试 → `ui/moment/`、`ui/task/`、`ui/daybook/`、`ui/system/`（J 三入口及独立 Moment/Task 数据层）→ 2026-08-30
- `ui/components/TimelineNavigation.kt`（无路由/测试引用的暖纸圆角时间线控件）→ 无替代；J P0 无对应入口 → 2026-08-30

## 8. 修改流程（任何风格调整必须走）

1. 改 `ui/theme/` 中对应 token（或新增成对 token）；
2. 同步更新本文档 §3 对应行；
3. 若影响组件结构，更新 §4 与 HTML 视觉基准（`docs/design/proposal_J*.html`）；
4. 跑 `./gradlew assembleDebug` 验证。
