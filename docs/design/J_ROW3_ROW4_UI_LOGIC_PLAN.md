# J 最终稿第 3/4 行首卡：UI 与逻辑规划

> 规划对象：`docs/design/proposal_J_final.html` 在桌面三列网格中的第三行第一张（⑦ 成就详情）与第四行第一张（⑩ 分组管理）。
> 本文件是本轮（2026-09-01）已授权的实施契约；代码尚未由 Agent 执行前，不代表功能已经交付。

## 1. 屏幕定位与现状边界

`proposal_J_final.html` 的 `.grid` 在 1160px 容器内可排出三列，因此行首卡映射为：

| 网格位置 | J 屏 | 产品职责 | 当前状态 |
| --- | --- | --- | --- |
| 第三行第一张 | ⑦ 成就详情 · `ACHIEVEMENT` | 展示过去 Moment 的成就摘要、故事和日期派生里程碑 | 本轮实现固定里程碑与金色时间条；周年提醒仍等待通知链路 |
| 第四行第一张 | ⑩ 分组管理 · `PARTY` | 管理 Moment/Task 的线路归属、颜色和解散语义 | 本轮实现 Room 分组、线路 chip 与管理页 |

两屏必须继续遵守以下边界：

- Moment 与 Task 仍是两张独立的 Room 表；分组只是可选组织维度，不把 Task 转成 Moment。
- 固定底部导航仍只有「大事件 / ＋ 新时刻 / 日子簿」；两屏通过时刻详情或系统设置进入，不新增底部入口。
- HTML 中的示例标题、数字、日期、数量和颜色仅用于构图，运行时必须来自真实数据或 token。
- 本轮不实现通知、重复规则、照片/地点、模板装备库、自定义里程碑或周年提醒开关；里程碑与分组管理按本契约实施，但完成状态仍需 Agent 的代码、测试和截图证据确认。

## 2. ⑦ 成就详情：UI 契约

### 2.1 页面结构

按 J 稿从上到下保留以下层级：

1. `PageTitlePanel`：`时刻详情`，HUD 为 `ACHIEVEMENT`。
2. `AchievementFrame` 摘要横幅：奖杯徽章、真实标题、`已 X 天`、`SINCE yyyy.MM.dd`、可选 PARTY 标签。
3. `成就故事 STORY` 面板：仅在 `Moment.note` 非空时显示；空值不渲染空框。
4. `里程碑 MILESTONES` 面板：显示固定节点只读行；当前下一项配金色时间条，不放置可点击复选框或假进度。
5. `周年提醒`：只有提醒能力和权限链路完成后才显示；未实现时隐藏，而不是展示无效开关。
6. 操作区：`✎ 编辑`、`🏆 珍藏中/设为珍藏`（置顶事实）、`✕ 封印此成就`。封印沿用共享二次确认弹窗和回收站语义。

`AchievementFrame`、`PixelPanel`、按钮颜色、硬偏移阴影、44dp 触摸目标和中文 content description 均复用现有 token/组件；不在页面内新增裸色值、圆角或自定义阴影。

### 2.2 状态与数据流

```text
MomentRepository.findById
        │
        ├─ EventTimeCalculator(anchorDate, Clock) → PAST/TODAY/FUTURE
        ├─ PAST 或「过去·正数 + TODAY」→ AchievementPresentation
        └─ 其他状态 → 现有 QUEST 详情表现

AchievementPresentation
        ├─ summary（标题、已 X 天、SINCE、group）
        ├─ story（note）
        └─ milestones（本轮：由 anchorDate + Clock 派生，不落库）
```

- `creationDirection`、`anchorDate`、`note`、`groupId`、`isPinned` 等事实继续来自 `Moment`。
- `days`、成就/任务文案、里程碑状态和下一目标全部是读取时派生值；不写回 Room。
- ViewModel 注入 `Clock`，在跨午夜或测试时固定日期，避免 UI 自己调用 `LocalDate.now()`。
- 未来 Moment 在目标日次日起进入同一成就表现；不得复制记录或修改原始方向字段。

### 2.3 本轮固定里程碑算法

第一版只读、确定性生成，不允许用户手工勾选“已达成”：

- 日数里程碑固定为 `100、365、1000、1200、2000…`；已达到的显示达成日期，最近未达到的一项显示剩余天数、目标日期和金色时间条，更远目标显示“远征中”。不开放自定义节点或手工勾选。
- 年度里程碑按 `anchorDate` 的月日生成；2 月 29 日开始的 Moment 在非闰年不生成周年项。
- 里程碑排序按目标日期升序；同一天先显示日数项，再显示周年项。
- 没有真实目标时不显示占位行；没有里程碑时显示一行解释性空状态。

### 2.4 详情页验收

- 固定 `Clock` 覆盖开始日前后、目标日当天和次日：摘要数字与状态正确，里程碑不改变 Moment 事实字段；另覆盖 2 月 29 日在非闰年不生成周年项。
- 重启后标题、故事、置顶、分组和日期保持；里程碑可重新计算且无需迁移数据。
- 页面不得出现可点击但无行为的里程碑复选框或周年提醒开关。
- 大字体下标题、数字、日期和封印按钮不裁剪；TalkBack 能读出“成就、已 X 天、开始日期、编辑、置顶、封印”。

## 3. ⑩ 分组管理：UI 契约

### 3.1 页面结构

入口建议放在「系统设置 → 队伍编成」，不改变三入口底栏。页面按 J 稿保留：

1. 页面标题：`队伍编成`，HUD 显示 `GROUPS · 当前数量/总量`；总量只有在产品确认上限后才展示，否则只显示当前数量。
2. 分组列表卡：线路色块、名称、活动内容计数、`✎ 改名`、`✕` 解散。
3. 空线使用禁用透明度表达，但仍可改名或解散；颜色来自 token 调色板，不使用硬编码 `Color(0x...)`。
4. `＋ 新建分组` 绿色主按钮；名称校验失败时在输入面板内显示中文错误。
5. 说明面板：明确“解散只解除归属，Moment/Task 不会删除；原有条目变为无阵营”。

列表卡是可访问的组合节点：卡片本体可进入分组筛选（P1 搜索/筛选能力就绪后），改名和解散按钮保持独立 44dp 触摸目标；装饰色块隐藏于语义树。

### 3.2 数据模型与迁移决策

当前 `Moment.groupId` / `Task.groupId` 已存在，但 `MomentMarkGroupStore` 仍以 DataStore 的字符串列表保存分组。本轮迁移为 Room `groups` 表：

```text
Group
  id: String (稳定 UUID)
  name: String (trim 后唯一，不区分大小写)
  colorToken: String (来自预定义线路色 token 名)
  sortOrder: Int
  createdAt: Long
  updatedAt: Long
```

- `moments.groupId` 与 `tasks.groupId` 指向 `Group.id`；P0 的 null 继续表示“无阵营”。
- 从旧 DataStore/名称型 `groupId` 迁移时，先按规范化名称创建/合并 Group，再在同一事务中重写两张表引用；无法匹配的值保留为无阵营并记录诊断信息。
- 迁移必须提升 Room 版本、导出 schema JSON、补 migration test；禁止 `fallbackToDestructiveMigration()`。
- 删除 Group 时先在同一事务把所有 Moment/Task 的 `groupId` 置空，再删除 Group 行；软删除项目也要解除归属，确保复活后不会指向不存在的分组。

### 3.3 操作逻辑

| 操作 | 规则 | 失败恢复 |
| --- | --- | --- |
| 新建 | 名称 trim 后不能为空；同名（不区分大小写）拒绝；自动选择未占用的线路色 token | 保留输入，显示错误，不写半成品 |
| 改名 | 只改 Group.name，引用使用稳定 ID，因此 Moment/Task 无需逐行改写 | 事务失败时维持旧名称并显示可重试错误 |
| 解散 | 二次确认；清空两张表中的 groupId；删除 Group；条目变为“无阵营”且不删除、不改变完成/置顶/封印状态 | 任一步失败整体回滚 |
| 排序 | 仅影响 Group.sortOrder；不改变首页 Moment/Task 日期排序或 pinnedOrder | 保存后重新收集 Flow，重启保持 |

计数默认只统计 `deletedAt == null` 的活动条目，并同时给出 Moment/Task 的类型拆分；封印之地不因分组删除而丢失历史字段。

### 3.4 分组页验收

- 新建、重名校验、改名、解散、重启恢复均有 JVM + Room 测试。
- 解散后首页、日子簿和回收站投影都显示“无阵营”，条目本身仍可用。
- 组内计数随 Moment/Task 新增、封印、复活和永久净化更新；不把已删除项计入活动计数。
- 颜色只引用 `MomentMarkTokens`/Material theme 语义 token；夜间模式使用成对 token，保持 J 的洞窟/任务板对比。
- TalkBack 能读出“分组名称、活动条目数、改名、解散”；44dp 触摸目标和 130%/200% 字体不重叠。

## 4. 本轮实施顺序与非目标

1. **里程碑/成就详情**：实现固定节点、下一项金色时间条、只读状态和 `AchievementFrame` 视觉。
2. **三格日期选择**：实现年/月/日独立选择格、合并 `LocalDate`、固定 `[x] 全天`，不新增时分字段。
3. **分组线路**：实现 Room `groups`、HTML 风格等宽 chip、稳定 ID、颜色 token、管理页和解散事务。
4. **日历/完成态**：按 HTML 放大日格与字号层级；完成任务显示删除线、填充 checkbox、`CLEAR +5 EXP`，未完成显示 `TODO` 或具体时间。
5. 所有布局尺寸使用父容器百分比、`weight` 或相对约束；44dp 仅作为触摸下限。每阶段完成 JVM/Room/Compose、截图和重启检查。
6. 周年提醒、通知权限、自定义里程碑、分组筛选和跨实体关联仍作为后续独立任务，不在本轮隐式实现。

## 5. 2026-09-01 实施记录

- 阶段 1：已实现固定里程碑计算、成就详情只读面板、首页成就金色时间条；2 月 29 日在非闰年不生成周年项。
- 阶段 2：已实现 Moment 年/月/日独立选择格，合并保存为单一 `LocalDate`，固定显示 `[x] 全天`。
- 阶段 3：已实现 Room `groups`（数据库 v8、`MIGRATION_7_8`）、稳定 ID、线路色、表单 chip、管理操作和跨表解散事务。
- 阶段 4：已实现日历相对尺寸与日期字号调整、任务完成删除线、填充 checkbox、`CLEAR +5 EXP`，未完成任务显示 `TODO` 或具体时间。
- 自动化证据：`testDebugUnitTest`、`assembleDebug`、`lintDebug` 通过；Pixel_9_Pro API 35 模拟器 `connectedDebugAndroidTest` 18/18 通过。
- 尚未完成：真实物理设备、TalkBack、130%/200% 字体人工检查、HTML/Android 截图像素差异量化，以及通知链路（周年提醒仍隐藏）。
