# Moment Mark HTML → Android UI 等效重构计划

> 日期：2026-08-30  
> 目的：把 `docs/design/proposal_J_final.html` 的像素视觉组件逐一复刻到 Android Compose。  
> 适用范围：仅处理已有 P0 页面（首页、铭刻时刻、新任务、日子簿、详情、设置、封印之地、确认框、底部导航）；不借此加入 P1/P2 功能。

## 1. 先定义“完全一致”

HTML 与 Android 不共享浏览器字体栅格、系统状态栏和物理像素，因此不能用“看起来差不多”作为完成标准，也不能承诺两张原始截图的物理像素完全相同。本项目采用可重复的等效标准：

- 以 HTML `.phone` 的 **330 × 664 CSS px** 为构图基准；Android 使用固定基准宽度和固定 density 截图，再按内容区域比例比较。
- Android 不伪造 `LV.07`、`GOLD` 或 iOS 状态栏；系统栏保留原生显示，应用内容从同一内边距开始。
- 所有 HTML 几何值先进入 `ui/theme/` token，再由共享组件消费；页面中不得自行写 `Color(0x...)`、裸 `dp/sp`、圆角或 Material 默认阴影。
- 每个 P0 页面都必须有“HTML 参考图 / Android 截图 / 差异记录”三件证据；只有代码通过或单张截图好看不能标记完成。

## 2. HTML 组件到 Compose 组件的逐项映射

| HTML/CSS | 精确语义 | Compose 实现要求 | 验收重点 |
| --- | --- | --- | --- |
| `.px` | 3px 墨线 + 3px 右下墨色实体层 | `PixelFrame(variant=Raised)`；用独立 offset layer 绘制，不用 `Modifier.shadow()` | 右边、下边均出现连续且独立的黑色台阶 |
| `.px4` | 3px 墨线 + 4px 右下实体层 | `PixelFrame(variant=Emphasized)` | 主卡/CTA 的影子比普通卡明显 |
| `.ach::before` | 外框内缩 3px，2px 金线 | `AchievementFrame` 内层 inset token | 金线不覆盖外框，不挤压文字 |
| `.phone` | 8px 外框、昼/夜底色、底部固定 nav | `PixelViewport` 页面骨架 | 内容、底栏、滚动区比例稳定 |
| `.mono` | HUD 等宽、粗体 | `MmHudFont` + `labelMedium/Small` | DUE/SINCE/UNLOCK、百分比统一字形 |
| `.hpbar` | 10px 高、2px 边框、内填充 | `PixelHpBar` | 填充不覆盖边框；颜色只由真实 fraction 决定 |
| `.badge-tag` | 2px currentColor 边框、紧凑内边距 | `PixelBadge` | 徽章不变成圆角 chip，图标不依赖 emoji 字形 |
| `.notebox` | 白色内表面、3px 墨线、固定最小高度 | `PixelTextInput(single/multiline)` | 聚焦方块光标、选区、错误态可见 |
| `.nav` | 面板底、顶部分隔线、三入口、加号 1.4 倍宽 | `PixelBottomNav` | 选中项 2px 影；夜间成对 cave token |
| `.dg` | 直角洞窟日格；today 有金底和硬影 | `DungeonDayCell` | 今天/选中/有任务/其他月五态可区分 |

## 3. 当前两张图暴露的根因

1. **阴影不是颜色问题而是层级问题**：必须将影子作为面板后方的独立矩形层；不能只给外层 Box 背景或把 `surface` 当 shadowColor。影子要在裁剪前绘制，且由组件保证右/下连续。
2. **卡片不是同一种面板**：HTML 的主线、成就、普通任务有不同 padding、数字层级和 badge 位置。当前若全部套 `SpaceInner`，会得到同质大白框。
3. **字体角色未落到每一段文本**：标题/正文使用中文 sans，大数字使用 ExtraBold，HUD 使用 mono；单位、日期、星级不能继续跟随同一个 Material TextStyle。
4. **输入框缺少第二层**：HTML 的外层字段面板与白色输入内框是两层，光标是闪烁方块，不是系统细线 caret。
5. **截图比较基准不一致**：HTML 参考图包含 330dp 画布和固定底栏；Android 截图还包含设备状态栏/导航栏，必须先裁出应用内容区域再比较。

## 4. 重构顺序（每阶段独立验收）

### 阶段 A：像素基座

- 重写 `PixelPanel` 为真正的 `PixelFrame`：面、边、影三层明确分离；支持 Raised/Emphasized/Flat，默认影色永远是语义 outline。
- 固化 `MmTypography` 的 display/body/HUD 三套角色，并为数字 + 单位、HUD 日期、badge 提供专用组合组件。
- 统一 `AchievementFrame`、`PixelBadge`、`PixelHpBar`、`PixelTextInput` 和 `PixelBottomNav` 的 token。
- 加入组件级 screenshot fixture：同一组件在昼/夜、普通/强调、聚焦/错误状态各一张。

### 阶段 B：首页“大事件”

- 按 HTML ① 的顺序重排：主线宽卡 → 成就金框卡 → 两列普通卡；不恢复旧 Hero/自由拖拽墙。
- 将排序箭头收进紧凑操作组，不能再形成横跨整行的“第二张卡”。
- 主线/成就/普通 Moment/Task 使用独立卡片排版，但保留 PRD 的列归属、日期排序和 Task 只读投影。
- 固定 fixture 截图检查：外框、右下黑影、内金线、数字/单位基线、DUE/SINCE、血条和底栏。

### 阶段 C：表单与光标

- 用同一个 `PixelTextInput` 替换 Moment/Task 所有字段；方向选择、快捷日期、日期输入、备注和错误态不得分叉。
- 方向/CTA 复刻 HTML 的蓝/金实体按钮和按压位移；输入内层保持白色（夜间为 cave input token）。
- 用中文长标题、空值、错误日期、聚焦/取消聚焦录制对照图；验证方块 caret 不被 placeholder、padding 或裁剪吞掉。

### 阶段 D：日子簿与剩余 P0 页

- 日格、任务行、checkbox、月切换按钮按 HTML 紧凑密度重排；宝箱/有任务标记只来自真实 Task 日期。
- 详情、设置、封印之地、确认框全部只调用共享像素组件；昼/夜不能混用 surface/cave token。
- 每个危险操作仍保留二次确认、撤销、复活和净化的数据行为。

### 阶段 E：视觉回归与可访问性

- 在固定 Clock/fixture 下逐页生成截图，按应用内容区域做像素差异检查并记录剩余差异。
- 跑 JVM、assemble、lint、connected UI tests；另行手工检查真实设备、TalkBack、130%/200% 字体、系统栏 inset、长中文和重启后布局。
- 未完成上述人工证据前，状态只能写“代码/模拟器已验证”，不能写“与 HTML 一模一样”。

## 5. 这次明确不改的产品行为

- 不改变 Moment/Task 独立实体、创建入口、首页列归属、`EventTimeCalculator` 日期派生和回收站生命周期。
- 不实现 HTML 中的装备库、搜索筛选、分组管理、里程碑、周年提醒、通知或重复任务。
- 不把 HTML 示例中的 `EXP`、`GOLD`、假进度和样例标题写入真实业务数据；它们只能作为已有字段的视觉位置参考。

## 6. 完成门槛

只有同时满足以下条件，才可标记“HTML 等效 UI 完成”：

1. 所有 P0 页面均使用共享像素基座，卡片右/下黑色实体影在昼夜截图中连续可见；
2. 成就外框、内金线、字体角色、数字/单位基线、HUD、血条、输入方块光标与 HTML 结构一一对应；
3. 固定 fixture 的页面截图完成差异记录，且无未解释的布局/裁剪/对比度问题；
4. 自动化测试、模拟器截图、真实设备/TalkBack/大字体检查分别有明确结果；
5. P0 数据行为和导航回归通过。

本计划替代“只增加阴影 dp 值”的零散修补；后续实现必须按 A → B → C → D → E 顺序推进。
