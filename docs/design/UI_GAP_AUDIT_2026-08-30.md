# Moment Mark UI 视觉差距审计与补齐规范

> 状态：**后续 UI 实施的唯一工作清单**；本文件不改变 P0 产品范围或数据行为。
>
> 日期：2026-08-30  
> 视觉权威顺序：`PRD.md` → `proposal_J_final.html` → `DESIGN_SYSTEM.md` → `ARCHITECTURE.md` → 当前 Compose 源码。  
> 产品范围：Moment/Task 的 P0 已独立实现。本文件只处理“同一行为的视觉与交互呈现”，不得借此加入 P1/P2。

> **严格等效重构入口：** 需要按 HTML 组件层级逐项实现时，先执行 [`UI_PARITY_REPLAN_2026-08-30.md`](UI_PARITY_REPLAN_2026-08-30.md) 的 A→B→C→D→E 顺序；本审计记录差距与历史实施证据，不能用“增加几 dp 阴影”替代该计划。

## 1. 审计范围、证据与结论

### 1.1 设计基准

最终视觉基准是 `docs/design/proposal_J_final.html` 的 13 屏 HTML。它明确给出：

- 昼间任务板和夜间洞窟的成对颜色；
- `.px`（3px 描边 + 3px 右下硬影）、`.px4`（4px 硬影）、`.ach`（距外框 3px 的 2px 金色内框）；
- 卡片内部的字体层级、血条、HUD、badge、虚线任务行、表单输入框和底部导航；
- 页面不是“任意直角白框的集合”，而是有主/次层级、信息密度和空间节奏的像素冒险界面。

`DESIGN_SYSTEM.md` 的 token 是 Android 唯一取值来源；若 HTML 示例中有 P1/P2 内容（例如里程碑、周年提醒、装备库、筛选、分组管理），只能借鉴视觉语言，**不能**因本审计而实现其行为。

### 1.2 当前 App 的实际证据

本次在连接的 Pixel_9_Pro 模拟器上查看了以下实际画面：

| 画面 | 观察到的事实 |
| --- | --- |
| 首页“大事件” | 置顶卡、成就卡、两列卡、导航均已使用直角描边；但置顶排序显示为两条横跨整行的“上/下移主线”框，卡片高度、阴影可见度、内部层级和 HTML 示例差异明显。 |
| 日子簿 | 已是夜间洞窟色板；但月历、空状态和“接受新任务”都像通用大面板，缺少最终稿的紧凑任务行、虚线分隔、宝箱事件标记和内容密度。 |
| 铭刻时刻表单 | 方向块与表单字段存在；但实际可编辑字段只是外层 `PixelPanel` 内的裸 `BasicTextField`，没有 HTML 中白色输入内框、像素光标或聚焦态。 |

源码进一步证实：`MomentCards.kt`、`MomentFormScreen.kt`、`TaskFormScreen.kt`、`DaybookScreen.kt`、`MomentDetailScreen.kt`、`RecycleBinScreen.kt` 均已具备 P0 行为和 token 色板；视觉问题主要来自组件结构和排版契约未被实现，而非缺少数据。

### 1.3 总结判断

当前 App 是“**功能 P0 + 初步像素 token 化**”，不是最终稿级 UI。尤其需要纠正下列误区：

1. 有 `PixelPanel` 不等于有最终稿的硬影层次；大量调用传入 `shadowColor = surface`，视觉上等同于取消右下黑影。
2. 有大号数字不等于与 HTML 的信息层级一致；当前 Android 默认字体、字号组合、基线和 emoji fallback 与 HTML 的系统/PingFang 组合不一致。
3. 有 `BasicTextField` 不等于像素输入框；当前没有可见的输入内框、方块光标、选择色或聚焦边框。
4. HTML 是视觉基准，不是产品功能清单；不得为了“更像网页”恢复旧 Hero、模板墙、搜索抽屉或实现 P1 的目标/里程碑。

## 2. 必须保持不变的 P0 边界

后续 Agent 在做任何视觉改造前必须先保留以下行为：

- 底部仅有“大事件 / ＋ 新时刻 / 日子簿”三个入口；“新时刻”绝不创建 Task。
- Moment 和 Task 仍是独立实体；首页右列的 Task 仍是只读投影，编辑/完成只能留在日子簿。
- 相对日期仍只由 `EventTimeCalculator` 派生；不可把“X 天后”、血条百分比或显示状态写回 Room。
- 置顶 Moment 的顺序仍需可调整、可持久化；可改变排序控件的**视觉位置与形态**，不可删除能力。
- 回收站确认、即时撤销、复活、永久净化、启动时可选自动净化均必须继续可达。
- 里程碑、周年提醒、前置目标、模板装备库、搜索筛选、分组管理、通知和重复任务仍为 P1/P2，不在本轮补齐。

## 3. 视觉基础层：先修组件，再修页面

以下项目为最高优先级。页面不得各自复制一套描边、字号、输入框或阴影实现。

| ID | 设计基准 | 当前差距（源码/画面事实） | 必须补齐 | 验收标准 |
| --- | --- | --- | --- | --- |
| V-01 | `.px` = 3px 描边 + 3px 黑色右下硬影；`.px4` = 4px 硬影。 | `PixelPanel` 有硬影能力，但表单、任务行、回收站按钮等大量调用传入 `shadowColor = surface`，实际只剩双层边框或无可辨识投影。 | 将 `PixelPanel` 拆为语义变体：普通面板、强调面板、输入外框、平面容器。默认业务卡/按钮必须使用 outline 右下硬影；只有明确的嵌套内容可用 `Flat` 变体。 | 昼/夜两套界面中，普通卡和 CTA 的右、下边均存在独立、连续、与描边同色的硬影；不可再以 surface 色伪装为影子。 |
| V-02 | `.ach::before` 为距外框 3px 的金色 2px 内框。 | `MomentAchievementCard` 与置顶成就卡直接对 `matchParentSize()` 画金框，内框贴住或覆盖外描边，不等同于 HTML inset。 | 新建 `AchievementFrame`：外 3dp 墨线、右下硬影、内缩 token、2dp 金线，所有首页/详情成就共用。 | 金线四边都与墨色外框保留相等留白；不遮挡标题、星级、日期，也不超出阴影。 |
| V-03 | HTML 正文采用系统中文 sans，HUD 使用 monospace；数字粗而紧凑，中文标题与数字基线经过分别排版。 | `MmTypography` 只指定 mono HUD；其余依赖 Android 默认字体。仓库已有中文字体资源但未进入 `Type.kt`，导致 Android 字形、粗细和 emoji fallback 与网页显著不同。 | 在不改变字号 token 名称的前提下，建立 `MmDisplayFont`、`MmBodyFont`、`MmHudFont` 三个字体族；先检查现有字体的字重、中文覆盖和再发布许可，再决定是否使用。禁止把所有文字替换成衬线体。 | 360dp 宽度截图中：中文标题不发虚、不被切字；大数字、中文单位、HUD 日期基线稳定；HUD 均为同一等宽族；emoji 不承担关键 badge 形状。 |
| V-04 | HTML 输入框为白色内表面 + 3px 墨线；示例光标为闪烁方块 `▮`。 | `FormField`/`TaskField` 直接把 `BasicTextField` 放在外层 panel 内，无内白框、cursorBrush、selectionColors、focus frame 或固定高度。 | 抽取 `PixelTextInput`，承载 label、内白框、3dp 边、方块 cursor、step-blink、选区色、placeholder、单/多行高度和错误态。日期/时间仍用相同组件，不引入 M3 圆角输入框。 | 聚焦时光标为主题色或墨色 2dp 级方块，不显示系统细线；输入框内外层清楚分离；文本、placeholder、选择和错误提示均不溢出。 |
| V-05 | 血条是 10px 高、2px 墨线、内填色；颜色只表示真实紧迫度。 | `HpBar` 已有颜色阈值，但为简单 `drawBehind`；缺少统一的内缩、HUD 百分比/语义和在成就/任务的差异变体。网页示例的手工填色也不能覆盖 `DESIGN_SYSTEM` 的阈值规则。 | 抽取 `PixelHpBar`：保留当前由 `HomeMomentProjector` 派生的 fraction 与绿/橙/红阈值；统一边框、内填充裁剪、最小可见宽度和可选 HUD 文案。 | 满/中/低三档在昼夜均清晰；填充不覆盖边框；不通过伪造进度或 P1 里程碑来增加装饰。 |
| V-06 | HTML 采用 10/12px 级内部节奏，卡片不同层级有不同 padding。 | 当前多数 UI 直接套 `SpaceInner`，导致表单大框、卡片、chip、任务行和 CTA 的密度趋同。 | 为卡片、紧凑行、输入框、选择 chip、CTA、页面标题补齐语义 spacing token；禁止在页面中新增裸 `dp`。 | 同屏内可一眼区分“可点击卡”“字段容器”“输入内框”“HUD”四级；页面不再呈现重复的大白框堆叠。 |

## 4. 页面差距与实施任务

### 4.1 首页“大事件”（P0-UI-01，最高优先级）

HTML ① 的核心不是旧 Hero，而是：置顶主线的**一张高信息密度宽卡**、置顶成就的金色内框、两列中紧凑而有节奏的任务/成就卡、DUE/SINCE HUD 和具备实体感的血条。

| 现象 | 当前实现 | 后续任务 | 约束/验收 |
| --- | --- | --- | --- |
| 置顶排序控件破坏首页节奏 | `PinnedOrderControls` 在每张置顶卡后插入完整横向“上移/下移主线”面板；模拟器画面中该面板的视觉权重接近一张卡。 | 将排序操作收敛为置顶卡右上角或底部的紧凑像素 icon/button 组；必要时仅在“调整主线顺序”显式模式显示。 | 仍可相邻上/下移动，仍有中文 content description；常规浏览首页不出现占满整行的蓝色排序框。 |
| 置顶卡缺少最终稿层级 | 当前 `MomentPinnedCard` 只显示 badge、日期、标题、数字、星级；HTML 的主线卡有更清晰的顶部 HUD、数字/单位基线、强调层。 | 以真实字段重排为“badge + HUD/date → title → digit/unit + rarity”；主线/成就分别复用同一框架。 | 不添加不存在的 EXP、金币、假进度；卡片点击区域和详情路由不变。 |
| 成就金框不正确 | 见 V-02。 | 首页和详情统一使用 `AchievementFrame`。 | 过去 Moment 的金框、金墨大数字、SINCE 在两处一致。 |
| 两列高度和节奏杂乱 | 当前使用两条独立 `Column`，卡片高度由内容决定，左列空白与右列连堆的视觉效果缺乏设计意图。 | 在不改变“过去仅左列、未来/Task 仅右列”规则下，定义统一的卡片最小高度、行内对齐和单列空状态；先在固定 fixture 上比较两列。 | 不把 Task 放到左列；不为视觉整齐改变日期排序；长标题可省略但不能盖住 badge/数字。 |
| Task 首页卡与 Moment 卡不够区分 | `HomeTaskCard` 是同样的通用面板，只靠文案区分。 | 为 Task 投影定义只读任务卡变体（棋盘/checkbox/`CLEAR`/DUE），与 Moment 的倒数语义明显区分。 | 不出现 Moment 详情点击；已完成、今天、未来三态都可读。 |
| 血条与 HUD显得粗糙 | 当前条形和 metadata 行的占位、基线、星级对齐未按卡型分别设计。 | 使用 V-05 组件，给未来 Moment、限时 Moment、成就和 Task 设定合法变体。 | 颜色阈值仍以真实日期派生；小屏不裁切 DUE 与分组。 |

### 4.2 铭刻时刻与新任务表单（P0-UI-02）

HTML ②③④ 的重点是“方向/快捷日期是可感知的游戏选择，字段是内白框，CTA 是有硬影的实体按钮”，而非连续叠加尺寸相同的容器。

1. 先实现 V-04 的 `PixelTextInput`，统一替换 `FormField` 和 `TaskField`；光标、聚焦、日期/时间输入与错误状态不得分叉实现。
2. 方向块应是大面积双色直角块：未来为蓝，过去为金；未选中项保持面板色和明显墨线。当前 `DirectionChoice` 的未选中项将 shadow 设为 surface，需改为正式按钮变体。
3. 日期需要视觉上拆为“日期输入层”，但 P0 数据仍保存 `LocalDate`；不能为了视觉拆成三份状态或改变日期校验。
4. 分组、重要度、任务类型、难度应使用紧凑 chip/星级行，不能和普通字段拥有同等大面板高度。
5. 保存 CTA 需要标准硬影与按压位移；当前 Moment CTA 已显式使用 outline shadow，Task CTA 仍依赖默认调用，必须统一。

验收：未来/过去表单、任务表单各录制一张“未聚焦”和一张“聚焦输入”的截图；输入任意中文、长中文、日期错误文本均不得裁切/跳动。

### 4.3 日子簿“副本地图”（P0-UI-03）

HTML ⑤ 是紧凑的夜间月历 + 今日掉落清单：日期格之间有可读间隔、今日块明确、含内容的日期有宝箱标记、任务行使用虚线分隔和 CLEAR/DUE HUD。

当前模拟器画面已正确进入夜色，但月历和空任务区都过于“整块面板化”。后续须：

- 为日期格定义 `DungeonDayCell`，包含普通/今天/选中/其他月/有任务五种视觉状态；宝箱仅根据真实 Task 数量显示。
- 将任务列表改为紧凑行而不是每项通用卡；使用 2dp 虚线分隔、15dp checkbox、完成删除线和右侧 CLEAR/DUE HUD。
- 空状态保持简短，将“接受新任务”作为列表最后一行，而不是让空白页与大 CTA 竞争视觉焦点。
- 保持夜间的 cave panel/cave line 成对色，不混入昼间 `surface`、`outline`。

验收：一个固定月份 fixture 覆盖今天、选中日、有任务日、其他月日期、完成任务、未完成任务和空列表；每个日期格都至少 44dp 触摸目标，但视觉格可更紧凑。

### 4.4 时刻详情（P0-UI-04）

HTML ⑥⑦给出未来 QUEST 和过去 ACHIEVEMENT 的信息层级。P0 只实现真实存在的标题、日期、备注、编辑、置顶、封印；HTML 的前置目标、里程碑和周年提醒只作为后续视觉占位参考。

- 未来详情：`QUEST DETAIL` 顶部横幅应包含 title、数字、单位、TARGET/DUE HUD、真实可计算的血条；不要添加没有 Task 关系的“前置目标”。
- 过去详情：使用 `AchievementFrame`、金墨数字、SINCE、STORY；不伪造里程碑列表。
- 编辑、置顶、封印三个操作应成为明确层级的像素按钮：蓝/紫或面板/红，统一硬影与按压反馈；不使用 Material 文本按钮视觉。
- 返回入口、内容面板和危险操作要有合理间距，长备注可滚动但不与底部导航重叠。

### 4.5 设置、封印之地与确认框（P0-UI-05）

HTML ⑪⑫⑬的夜间系统页、回收站和确认框可用于视觉补齐，但不得添加 HTML 示例中的战斗音效/震动等 P1 外功能。

- `SystemSettingsScreen` 需把“回收站”和“自动净化”处理为夜间像素行：前者有数量/箭头，后者有明确 `[x]/[ ]` 状态；说明文字为次级 HUD，不做一整块同权重说明卡。
- `RecycleBinScreen` 应将条目类型、封存剩余天数、复活/净化按钮做成紧凑层级；每个操作必须保留二次确认。
- `PixelConfirmationDialog` 已摆脱 Material 默认圆角，但后续要与 `.px4`、按钮高度、警告 icon、文案行距作截图对齐；scrim 固定使用 token `AlphaScrim`。

### 4.6 底部导航与全局页面骨架（P0-UI-06）

`PixelBottomNav` 已满足三入口和基本硬影，但需要在所有昼/夜页面统一检查：

- 导航容器、普通项、选中项、加号的描边/影子必须与 HTML `.nav` 对齐；夜间不能出现浅色面板残留。
- 加号是行动入口，不是普通 tab；保持 1.4 倍宽、金色表面、实体黑影。
- 内容区滚动时，底部导航必须稳定，不能用异常大底部留白来“避开”导航。
- 系统状态栏可保留 Android 原生显示；不可为了模仿 HTML 的 `LV.07` 假数据覆盖系统状态栏。

## 5. 明确不纳入本轮的 HTML 屏幕

下列 HTML 屏幕只能用于提取颜色、描边、字体、动效等语言，不能作为本轮功能目标：

| HTML 屏幕 | 产品状态 | 本轮处理 |
| --- | --- | --- |
| ⑧ 装备库 | P2 | 不恢复模板/皮肤/透视堆叠交互。 |
| ⑨ 搜索与筛选 | P1 | 不恢复搜索抽屉和旧筛选逻辑。 |
| ⑩ 分组管理 | P1 | 仅沿用 Party 色彩和 chip 语言，不新增管理页。 |
| ⑥ 前置目标、⑦ 里程碑/周年提醒 | P1 | 不建立虚假数据或可点击空功能。 |

## 6. 后续 Agent 的实施顺序与提交边界

按下列顺序完成，每个阶段可独立验证；不要跨阶段顺手改数据、导航或 P1 功能：

1. **UI 基座**：V-01～V-06，增加 `PixelSurface`/`AchievementFrame`/`PixelTextInput`/`PixelHpBar`/字体 token；更新 `DESIGN_SYSTEM.md` 的 token 与组件节。
2. **首页**：只改 `ui/moment/home/` 的卡片与排序控件呈现；固定数据 fixture 截图对比。
3. **表单**：只改 `ui/moment/` 与 `ui/task/` 的表单视觉，先覆盖焦点、光标、错误和长文本。
4. **日子簿**：只改 `ui/daybook/` 的日格、任务行和空状态；不改变 `TaskRepository` 查询语义。
5. **详情/回收站/设置**：共享组件应用到剩余 P0 页面，并做昼夜回归。
6. **全局回归**：导航、系统栏、大字体、TalkBack、真实重启持久化。

每阶段前先执行 `git status --short`。当前工作区有大规模未提交的 Moment/Task 迁移改动；不得重置、覆盖或把视觉重构混入数据库迁移。

## 7. 视觉验收协议

### 7.1 基准与夹具

- HTML 的 `.phone` 是 330 × 664 CSS px 的视觉构图参考；Android 需先选定固定 dp 宽度和同一设备密度，再比较**相对比例**，不得把 CSS 像素直接等同于物理屏幕像素。
- 使用固定 Clock 和固定 Moment/Task fixture：置顶未来、置顶过去、普通过去、普通未来、首页 Task、今日 Task、完成 Task、已封存 Moment/Task。
- 视觉截图不使用用户真实数据，不依赖当天日期或调试 seed 的偶然内容。

### 7.2 每阶段必做检查

1. `./gradlew testDebugUnitTest assembleDebug lintDebug`。
2. 对修改页面增加/更新 Compose UI 测试：中文 content description、点击区域、焦点输入、错误文案、深浅色。
3. 在模拟器保存昼间首页、夜间日子簿、未来表单聚焦态、过去详情、回收站确认框截图；人工与 HTML 对照检查描边、右下硬影、内框、字体层级、输入光标、血条、底部导航。
4. 至少手工检查 130% 和 200% 字体缩放、系统栏 inset、中文长标题、TalkBack 焦点顺序。
5. 明确报告“自动化/模拟器/真机/TalkBack/人工视觉”的各自覆盖，禁止只凭 build 成功宣称完成。

### 7.3 完成定义

仅当以下均满足时，才可称“UI 与 J 最终稿的 P0 视觉补齐完成”：

- 每张 P0 页面都不再以无影/同质大面板堆叠呈现；
- 昼/夜卡片、按钮、表单、确认框都具备一致的像素描边和可辨识右下硬影；
- 字体、数字、HUD、中文标题、badge、星级和血条满足本文件的层级契约；
- 输入框的焦点、方块光标、选择与错误态都符合像素语言；
- 首页排序、Task 独立性、回收站和所有 P0 数据行为没有回归；
- 模拟器截图和人工大字体/TalkBack 检查有独立证据。 

## 8. 当前不可宣称的事项

首轮基座与核心页面已经实施，但本 App 仍不能宣称已与 HTML 最终稿像素级一致，也不能以 JVM/Room 测试代替字体、真实设备、输入焦点或无障碍验证；剩余页面和人工验收边界见 §9。

## 9. 2026-08-30 首轮实施记录

本轮按第 6 节顺序完成了 UI 基座及首页/表单/日子簿的第一批 P0 补齐：

- `PixelPanelVariant` 统一 Raised/Emphasized/Flat 层级；普通卡、主卡、按钮和系统页不再用 `surface` 伪装硬影。阴影现按 HTML `.px/.px4` 使用 3/4dp 基准偏移，并由独立后方矩形绘制。
- 新增 `AchievementFrame`、`PixelHpBar`、`PixelTextInput`。成就金线改为距墨色外框 3dp 的 2dp 内框；血条填充从边框内开始；文本输入统一内表面、聚焦描边、选择色和阶梯闪烁方块光标。
- 首页所有事件卡使用实体硬影；置顶移动按钮改为卡片下方紧凑箭头，保持相邻移动和中文无障碍语义；Task 投影保持普通卡，不与 Moment 成就金框混用。
- 铭刻时刻/新任务表单统一使用 `PixelTextInput`；方向、快捷日期、重要度/难度和 CTA 使用可见墨色硬影。日子簿日期格增加直角状态块，任务行增加虚线分隔；详情成就摘要复用 `AchievementFrame`。
- 第二轮补齐设置/封印之地/详情的剩余 P0 层级：设置页说明降为 HUD 文案并补系统栏 inset；回收站复活/净化改为金色/危险实体按钮；详情封印按钮使用危险底色；撤销横幅与全部 P0 面板统一使用墨色硬影。
- 第三轮补齐日子簿月历真实日期标记：`DaybookViewModel` 从现有 active Task 流派生当前月份的 `taskDates`，日期格仅在真实日期有任务时绘制 `◆` 标记并扩展中文无障碍描述；未改变按选中日期查询、跨日隐藏或完成状态语义。
- 第四轮补齐日子簿时钟与触摸契约：`today` 由 ViewModel 的固定 `Clock` 进入 UI state，日期高亮和“今日掉落”文案不再直接读取系统时钟；月份切换按钮与任务复选框采用 44dp 触摸区域，保留 15dp 像素视觉框。
- 第五轮修复 HTML 阴影的实际绘制层级：`PixelPanel` 改为“完整布局 → 右下偏移墨色矩形 → 左上面层”的三层绘制；此前 `padding().background()` 会让右/下区域透明，导致截图看不到黑影。普通/强调偏移按 HTML `.px/.px4` 校正为 3/4dp；置顶箭头改为 44dp 触摸目标 + 28dp 紧凑视觉盒。视觉核验必须启动 `com.cch.momentmark.app`，旧包 `com.cch.momentmark` 仍是历史原型，不能作为当前 App 证据。
- 第六轮进入表单视觉：`PixelTextInput` 外层改用 Raised（复刻 HTML 字段 `.px` 的右下实体影），内层白色输入框默认使用墨色 3dp 边框，错误态才切换危险色；保持 `TextFieldValue`、方块光标、选区和 IME 行为不变。
- 第七轮收敛日子簿/详情结构：月历补齐前后月份暗色日期和 HTML 对应的行列间距，移除月历整块外框；系统设置入口改为右上角紧凑齿轮（44dp 触摸目标 + 28dp 视觉盒）；未来/成就详情继续复用带实体黑影的摘要、故事和操作面板。
- 第八轮微调任务表单对比度：快捷日期/任务类型被选中时改用 `onSecondary` 前景色，保存 CTA 显式指定 outline 边框与实体黑影，确保蓝/金色块上的文字不沿用昼间正文色而失去层级。
- 第九轮以正确包 `com.cch.momentmark.app` 的实机模拟器画面复核并修正：置顶相邻排序控件由横跨整行的空白条收敛为右侧 44dp 触摸目标内的 28dp 像素箭头；未来 Moment 的方向说明/CTA 使用 XP 蓝，过去模式使用金币金；任务表单补状态栏 inset，快捷日期的“今日/明日/本周六”选中态由 ViewModel 注入的 `Clock` 派生，默认“今日”按 HTML 使用 HP 绿。上述快捷日期仅是展示派生值，不写入 Task。

验证证据：`testDebugUnitTest`、`assembleDebug`、`lintDebug` 均通过；`connectedDebugAndroidTest` 在 Pixel_9_Pro API 35 模拟器 **15/15** 通过（含表单中文输入、首页 Task/Moment 语义、置顶移动、日子簿编辑入口、日期格“有任务”语义）。第五轮重新安装正确 applicationId `com.cch.momentmark.app` 后人工查看 `/private/tmp/moment-mark-ui-shadow-html-offset.png` 与 `/private/tmp/moment-mark-ui-home-stage-b.png`，首页卡片右/下黑色实体层已连续可见，成就金线保持内缩；第七轮查看 `/private/tmp/moment-mark-ui-daybook-no-frame.png` 与 `/private/tmp/moment-mark-ui-detail-stage-d.png`，确认日子簿结构和详情实体层。设置/封印之地本轮已编译并完成代码级检查，但未获得稳定的独立运行截图；真实设备、TalkBack、130%/200% 字体和跨重启视觉仍未验证；仓库字体资源的再发布许可也仍待确认，因此本轮使用系统中文 sans 与 mono HUD 角色。

第八轮验证：任务表单修改后 `:app:compileDebugKotlin`、`testDebugUnitTest`、`connectedDebugAndroidTest` 均通过；Pixel_9_Pro API 35 **15/15**，并完成 `git diff --check`。本轮未新增业务行为或数据字段。

第九轮截图证据：`/private/tmp/moment-mark-home-pinned-controls-fixed.png`（首页紧凑排序箭头）、`/private/tmp/moment-mark-form-blue-cta.png`（未来蓝 CTA）、`/private/tmp/moment-mark-form-past-gold-cta.png`（过去金色方向）、`/private/tmp/moment-mark-form-focused-cursor.png` 与 `/private/tmp/moment-mark-task-form-focused-cursor.png`（两类表单聚焦光标）、`/private/tmp/moment-mark-delete-confirmation.png`（封印二次确认）、`/private/tmp/moment-mark-task-form-fixed.png`（任务状态栏和今日选中态）。`testDebugUnitTest`、`assembleDebug`、`lintDebug` 与 Pixel_9_Pro API 35 `connectedDebugAndroidTest` **15/15** 均通过，且 `git diff --check` 无输出。

## 11. 2026-08-31 阶段 B-补齐：首页普通卡层级与徽章节奏

- `MomentQuestCard` 与首页只读 `HomeTaskCard` 改用 `PixelPanelVariant.Raised`，对应 HTML 普通 `.px` 的 3dp 实体影；置顶主线与成就仍使用 `.px4` 的 4dp 影。
- 首页 Moment/Task 徽章补齐 HTML `.badge-tag` 的 2dp 垂直、6dp 水平内留白，并将数值提升为 `MomentMarkTokens` 统一 token；不改变卡片投影、日期排序或 Task 只读边界。

本轮待验证：重新生成首页固定夹具截图，确认普通卡与主线的影子层级差异及长标题下徽章不遮挡；继续保留真机、TalkBack、大字体和像素差异量化未验证状态。

## 10. 2026-08-31 阶段 B：首页 ① 等效重排

本轮只改首页的视觉结构，没有改变 Moment/Task 数据、日期计算、路由或置顶排序事实：

- `MomentCards.kt` 的置顶卡按 HTML ① 重排为“方向 HUD → 标题/大数字 + UNLOCK/SINCE 日期”两级结构。未来卡显示 `◆ MAIN QUEST 主线 · 倒数`，过去卡显示 `🏆 ACHIEVEMENT 成就 · 正数`；没有把 HTML 示例中的 `EXP +148`、`GOLD` 或里程碑进度写进业务 UI。
- `MomentHomeScreen.kt` 将置顶相邻移动箭头收进主卡右上角叠加层，仍保留每个箭头 44dp 触摸目标、中文 content description 和 `onPinnedMove(id, ±1)` 回调；普通浏览不再在两张主卡之间插入整行排序面板。
- 置顶卡仍按 `HomeMomentProjector` 的持久化顺序渲染；过去 Moment 仍只在左列，未来 Moment 与开启首页显示的 Task 仍只在右列，点击/编辑边界不变。

验证：`JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:compileDebugKotlin testDebugUnitTest` 通过；Pixel_9_Pro API 35 `connectedDebugAndroidTest` **15/15** 通过。重新安装 `com.cch.momentmark.app` 后取得首页截图 `/private/tmp/moment-mark-home-stage-b-2026-08-31-final.png`，确认主线宽卡、成就金框、双列卡、实体右下影和底部三入口均可见。该截图包含原生 Android 状态栏，符合等效重构计划，不伪造 HTML 的 `LV.07/GOLD`。

仍未闭合：尚未做 HTML 330×664 内容区域与 Android 截图的像素差异量化；原生系统字体与网页字体栅格差异、真实设备、TalkBack、130%/200% 字体和重启后视觉仍未验证。置顶箭头叠加层是功能性控件，需在下一轮小屏/长标题夹具中确认不遮挡 HUD。

### 下一步方案（按 A→B→C→D→E）

1. **B-验收补强（首页）**：用固定 Clock/fixture 生成 HTML ① 与 Android 内容区域对照图，检查主卡高度、箭头遮挡、双列最小高度、DUE/SINCE 截断和底栏稳定性；必要时只调整首页 spacing token。
2. **C-表单视觉**：复核 `PixelTextInput` 在未来/过去 Moment 与 Task 表单的未聚焦、聚焦、中文长文本、错误日期四态；统一方向块与 CTA 的按压位移，禁止引入新业务字段。
3. **D/E 回归**：完成日子簿/详情/设置/封印之地的同一套内容区域截图，再分别记录模拟器、真机、TalkBack、大字体和重启持久化证据；证据齐全前状态仍写“代码/模拟器已验证”，不写“与 HTML 一模一样”。
