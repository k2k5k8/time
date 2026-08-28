# DESIGN_SYSTEM — 刻间「像素冒险 Pixel Quest」

> **本文档是设计风格的唯一权威来源。** 任何 UI 改动的颜色、字号、间距、圆角、动效取值必须来自本表的 token（代码位于 `ui/theme/`），禁止凭感觉新造值。修改风格 = 「token + 本文档」同步改。AI 协作规则见 `CLAUDE.md`。
>
> 视觉基准（HTML 预览）：[proposal_J.html](docs/design/proposal_J.html)（首页 + 日子簿）· [proposal_J_app.html](docs/design/proposal_J_app.html)（全页面扩展）。其余备选风格存档见 `docs/design/` 目录。

## 1. 设计定位

**一句话定位：人生是一场冒险，每个日子都是一张任务卡。**

- 关键词：任务卡 · HP 血条 · 经验格 · 宝箱 · 8-bit 直角 · 硬偏移阴影 · mono HUD 标签
- 双模式叙事：**昼 = 任务板（冒险地图）**，**夜 = 洞窟（副本）**
- 数字永远是第一视觉层级（display 全部 extrabold）
- 游戏元素必须承载真实数据（血条=紧迫度、格数=进度、徽章=类型），**禁止纯装饰**

**三个"不要"**：不要圆角（全局 0dp）；不要柔和阴影/渐变/模糊（只用硬偏移影）；不要低对比灰字（正文 Medium 起步）。

## 2. 游戏化语义映射表（命名与文案的强制词表）

| 产品功能 | 游戏化术语 | 备注 |
|---|---|---|
| 事件 | 任务 Quest | 卡片右上角类型徽章 |
| 置顶 | 主线 MAIN QUEST（◆ 紫） | |
| 普通 | 支线 SIDE QUEST（◇ 蓝） | |
| 临近（<30% 剩余） | 限时 LIMITED（⧗ 橙）+ 血条变红 | |
| 分组 | 队伍/线路 Party Line | 颜色 = 线路色 |
| 删除（软删除） | 封印（移入封印之地） | 确认弹窗：「放弃这个任务？」 |
| 回收站 | 封印之地 GRAVEYARD（夜模式） | 30 天后永久净化 |
| 恢复 | 复活 REVIVE（金按钮） | |
| 永久删除 | 净化 | 危险红 |
| 模板 | 装备/皮肤 OUTFIT | 模板浏览页 = 装备库 |
| 日子簿 | 副本地图 DUNGEON MAP | 事件日 = 宝箱 🎁 |
| 待办/当日记录 | 今日掉落 TODAY'S LOOT | 可勾选 |
| 新建 | ＋ 新任务 / 接受任务 | |
| 空分组 | 空线 | 置 AlphaDisabled |

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
| **危险红**：删除/放弃/血条<30% | error | `#E5484D` | `#FF6B6F` |
| **限时橙**：血条 30–60% | MmAmber | `#F5A623` | `#FFB84D` |
| **魔法紫**：主线徽章/节日 | MmManaPurple | `#9B6DFF` | `#B79AFF` |
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
| PxShadowOffset | 3dp 硬偏移影 | 卡片/按钮（普通） |
| PxShadowOffsetLarge | 4dp | 主卡/弹窗 |
| PressOffset | 2dp | 按压时位移+阴影缩短（实体感） |
| SpacePage / SpaceCard / SpaceInner | 14 / 10 / 12dp | 页面边距/卡间距/卡内边距 |
| HpBarHeight | 10dp | 血条 |
| XpCellCount × XpCellHeight | 16 格 × 12dp（gap 3dp） | 经验条（全年/总进度） |
| CheckboxSize | 15dp | 目标复选框（2.5dp 描边） |

### 3.4 动效与材质

| 场景 | 规格 |
|---|---|
| 按钮按压 | 位移 2dp + 阴影缩短，100ms（不用透明度） |
| 血条变化 | 阶梯式减血 200ms，**不做平滑补间** |
| 任务完成 | ✦ 闪光 + 卡片弹出消散 |
| 删除动效 | 卡片像素化碎裂飞散 |
| 数字变化 | **直接跳变**（8-bit 无补间） |
| 装备库切卡 | 相邻卡 38% 透明度 + 透视变换（沿用既有拖拽 governor 交互） |
| 装备库相邻卡 | AlphaStackedCard 0.38 |
| 压暗层 | AlphaScrim 0.5 |

## 4. 组件规格

- **任务卡**：panel 面 + 3dp 墨线 + 3dp 硬影；行 1 = 名称 + 类型徽章（◆主线紫/◇支线蓝/⧗限时橙，2dp currentColor 描边）；行 2 = 大数字 + 难度星级（★金）；血条；mono 元数据行（HP% · DUE · GROUP）。
- **主线横幅（首页 Hero）**：EXP+ 奖励预告、16 格经验条、PROGRESS/UNLOCK 角标。
- **按钮**：直角 + 3dp 墨线 + 硬影；绿色=主操作、蓝色=编辑/次级、金色=奖励/复活、红=危险、ghost=取消。文字一律 labelLarge。
- **确认弹窗（放弃任务）**：⚠️ + 标题 + 说明（"将被封存进回收站，30 天内可复活"）+ [继续冒险(ghost)] [✕ 确认放弃(红)]。
- **Toast（系统消息）**：墨底金字，"⚔「xx」已封印入封印之地 ↩ 撤销"。
- **装备库（模板浏览）**：保留现有透视堆叠交互，文案改为装备/皮肤语义，当前卡带 "EQUIPPED ✓" 金标。
- **抽屉（地图筛选）**：状态段（全部/进行中/已完成，选中=墨底金字）+ 分组线路列表（色块 + ×N 计数，选中=线路色描边）。
- **设置页（夜模式建议）**：冒险者卡（头像块 + LV + 连续天数🔥 + EXP 条）→ 显示模式三态（沿用 SYSTEM/LIGHT/DARK）→ 开关行 `[x]/[ ]` mono → 关于（VERSION）。
- **日子簿（副本地图）**：夜=洞窟面板；今日=金色块；事件日=宝箱标记 🎁；当日列表=「今日掉落」可勾选行。

## 5. 深浅色规则

- 昼（任务板）为默认模式；夜（洞窟）不是简单反色：面板 `#262138` 紫黑、描边用更深的 `#0E0C16`、金色在夜间提亮为 `#FFC53D`。
- 同一屏幕禁止混用两套模式的表面色。
- 游戏色昼/夜成对取值（见 3.1），禁止只调亮度。
- 主题模式持久化沿用 DataStore `moment_mark_settings` 的 `SYSTEM/LIGHT/DARK`。

## 6. 交互契约（沿用既有实现，不因换风格改变）

1. 长按卡片 500ms 进入布局编辑；拖拽避让/跨槽位换位沿用防抖 governor（确认两帧 + 50ms 去抖 + 滞回）；布局持久化到 DataStore。
2. 搜索五状态完整：焦点、清空、关闭、有结果、无结果；返回键先关搜索再退页面。
3. 删除必须二次确认；Snackbar 撤销只恢复最近一次软删除，且避开底部加号触控区。
4. 无障碍：卡片合并为"标题 + 时间状态 + 日期"完整中文语义；装饰 icon 隐藏；导航节点有中文 content description。
5. 字体缩放 130%/200% 不裁剪主信息；Small/Wide 两列网格规则不变。
6. 事件数据统一走 `EventTimeCalculator` 派生显示，不落库冗余；模板通过统一内容契约读取。
7. 纯规划项不可点击，不给空 onClick。

## 7. 存量迁移状态

当前代码中存在旧「暖纸风格」的硬编码颜色（约 186 处 `Color(0x…)`）与散落的 dp/sp 值，集中在 `ui/home/EventCard.kt`、`HomeHero.kt`、`AdaptiveBackground.kt`、`CategoryDrawer.kt`、`eventdetail/`、`eventsettings/`、`ui/daybook/` 等文件。**迁移策略：渐进式**——

- 新写的 UI 一律用 token；
- 修改旧文件时，顺手把**触碰到的**硬编码值替换为最近的 token，不要求一次性全量迁移；
- 每完成一个页面的迁移，在本节登记（页面 → 日期）。

迁移登记（初始为空）：
- （暂无）

## 8. 修改流程（任何风格调整必须走）

1. 改 `ui/theme/` 中对应 token（或新增成对 token）；
2. 同步更新本文档 §3 对应行；
3. 若影响组件结构，更新 §4 与 HTML 视觉基准（`docs/design/proposal_J*.html`）；
4. 跑 `./gradlew assembleDebug` 验证。
