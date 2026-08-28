# AGENTS.md — Moment Mark AI 员工手册

本文件约束在此仓库工作的所有 AI/Agent。目标不是维护旧原型，而是按当前 `PRD.md` 与 J 最终设计稿完成 Moment Mark 的新版本。

## 0. 开工前必读

每次开始功能、UI、数据或重构任务前，按以下顺序阅读：

1. `PRD.md`：产品行为、P0/P1/P2、页面入口、验收标准。
2. `docs/design/proposal_J_final.html`：最终页面结构、布局和交互表达。
3. `DESIGN_SYSTEM.md`：视觉 token、组件规格和像素冒险词表。
4. `ARCHITECTURE.md`：目标数据模型、目录边界、迁移方案与技术债。
5. 当前任务涉及的源码和测试。

冲突优先级：**PRD → `proposal_J_final.html` → DESIGN_SYSTEM → ARCHITECTURE → 现有代码**。

现有代码只是技术栈、可迁移数据和可复用底层能力的参考。若旧逻辑与前四项冲突，修改或删除旧逻辑；不要为了“少改代码”而恢复旧产品行为。

## 1. 产品不变量

### 1.1 两个独立体系

| 体系 | 用户可见名称 | 创建入口 | 主页面 |
| --- | --- | --- | --- |
| Moment | 时刻 | 底部 `＋ 新时刻` | 大事件首页 |
| Task | 待办 / 新任务 | 仅日子簿内 `＋ 接受新任务` | 日子簿 / 副本地图 |

- 不得在“新时刻”里创建普通待办。
- 不得在日子簿“新任务”里创建首页时刻。
- Moment 与 Task 是独立实体，不能互相伪装或共用一个万能 Event 表。

### 1.2 Moment 的时间规则

- 新建 Moment 时，用户选择：`未来·倒数` 或 `过去·正数`。
- 未来模式只可选今天或未来日期；过去模式只可选今天或过去日期。
- 未来 Moment：目标日前显示倒数，目标日显示“就是今天”，从次日起自动转为过去成就。
- 自动转换是同一条记录的展示状态改变：不得改变 ID、日期、标题、备注、分组、重要度或置顶，不得生成重复记录。
- 过去 Moment 显示“已 X 天”；未来 Moment 显示“还有 X 天 / X 天后开启”。
- 全日日期一律用 `LocalDate`；相对天数和显示状态只由 `EventTimeCalculator` 派生。

### 1.3 Task 的规则

- 新 Task 默认归属今天；用户可选今日、明日、本周六或自选日期。
- Task 可勾选完成，完成后显示 `CLEAR`，再次点击可取消完成。
- 跨过归属日的未完成 Task 不自动迁移、不自动删除，只是不出现在今天的日子簿中。
- Task 默认不显示在首页；用户明确开启后显示在首页右列，按日期由近到远排序。到期日显示“就是今天”，日期过去后从首页隐藏，但任务仍保留在日子簿。

### 1.4 首页规则

1. 置顶 Moment 在最前方，每条独占一整行。
2. 多个置顶 Moment 可由用户手动调整顺序，顺序必须持久化。
3. 未置顶过去 Moment 仅在左列，日期从近到远。
4. 未置顶未来 Moment 仅在右列，日期从近到远。
5. 不要把旧 Hero、旧模板池、自由卡片墙或长按拖拽布局带回 P0 首页。

### 1.5 回收站规则

- 删除等于“封印”：先二次确认，再移入“封印之地”，并提供即时撤销。
- Moment 与 Task 都可在回收站复活；复活需恢复原始字段和状态。
- 手动永久净化、清空回收站必须二次确认。
- 自动净化默认关闭。只有用户打开此设置后，App 在**下一次启动时**清理已封存满 30 天的项目；不实现后台定时删除。

## 2. 页面与视觉规则

### 2.1 固定三入口

底部导航只表达以下三个入口：

- `◉ 大事件`：首页。
- `＋ 新时刻`：铭刻时刻表单。
- `▤ 日子簿`：副本地图、月历与任务。

不要从旧页面继承多余抽屉、旧 Hero 控件或不在 J 最终设计稿中的入口。

### 2.2 J 最终设计是视觉基准

- 保持像素冒险世界观，不改成通用 Dashboard、极简白卡或 Material 默认示例界面。
- 首页是“大事件”，日子簿是夜间“副本地图”。
- 文案使用既定词表：时刻、成就、铭刻、日子簿、封印之地、复活、净化、装备等。
- `DESIGN_SYSTEM.md` 的 token 是唯一视觉值来源。若 J 最终设计与该文档存在差异，先同步更新 token/规范，再写 UI；不能用旧规范反向覆盖新设计。
- 新 UI 禁止新增硬编码 `Color(0x...)`、裸 `dp/sp`、圆角、柔和阴影、玻璃拟态或无业务意义的装饰。

### 2.3 旧 UI 的处理

以下内容是可删除或重写的遗留实现，不构成兼容承诺：

- 旧首页 Hero、搜索抽屉、旧筛选逻辑。
- 旧模板池、旅行卡、Small/Wide 卡片选择器。
- `CardBoard` 自由排版、长按拖拽和其 DataStore 布局数据。
- `PrototypeDaybookDataSource` 的样例记录、硬编码节日和原型日子簿。
- 旧 `TimeEvent` 自动按日期决定未来/过去的页面语义。

**已获用户授权：** 在同一任务已完成 J 最终设计的等价替代、且确认不再承担新 P0 功能后，AI 可以直接删除旧 UI、旧路由分支、旧模板和旧交互逻辑，不需要再次逐项确认。删除前仍必须完成数据安全确认：不误删真实用户数据、迁移所需代码或当前任务之外的功能；用 `git status` / `git diff` 核对待删文件归属，不得删除用户并行开发中未提交的修改。删除后运行相关构建与测试，并在 `DESIGN_SYSTEM.md` §7 的删除登记表记录（旧文件 → 替代屏 → 日期）。

## 3. 目标技术边界

### 3.1 结构

- 保持 Android 原生、Kotlin、Compose、单 Activity、本地优先。
- 使用 ViewModel + Repository + Room；Composable 不直接读写 Room 或 DataStore。
- 手工 `AppContainer` 可继续使用。手工 `AppScreen` 路由可逐步替换；新页面流不必迁就旧路由。
- 新 Feature 放入清晰的目录，例如 `ui/moment/`、`ui/task/`、`ui/daybook/`；不要继续膨胀 `MomentMarkApp.kt`。

### 3.2 数据模型

目标模型至少包括：

```text
Moment
  id, title, note, creationDirection, anchorDate,
  groupId?, rarity?, isPinned, pinnedOrder?,
  deletedAt?, createdAt, updatedAt

Task
  id, title, dueLocalDate, dueInstant?, zoneId?, note,
  taskType?, difficulty?, groupId?, isCompleted, completedAt?,
  showOnHome=false, deletedAt?, createdAt, updatedAt
```

- `creationDirection` 是用户创建 Moment 时的选择；`displayState` 是 `creationDirection + anchorDate + Clock` 的派生结果，不能落库。
- Moment、Task 需要分别建 Room 表和 Repository。
- 首页应使用 `HomeCardItem` 投影组合 Moment 和 `showOnHome=true` 的 Task；不要把 Task 保存为 Moment。
- P0 不要求 Moment 和 Task 有外键；`relatedMomentId` 只在 P1 有明确需求时再加入。

### 3.3 迁移与持久化

- 当前 `time_events`、`TimeEvent`、模板 JSON 和多份 DataStore 是遗留数据形状，不是新模型的强制约束。
- 有真实用户数据时，写明确迁移并测试；只有样例/开发数据时，可以在开发阶段清空数据库后启用新模型（已确认：当前无真实用户数据，走此路径，详见 ARCHITECTURE §5.1）。
- 不得静默清除用户真实数据，也不得用 `fallbackToDestructiveMigration()`。
- Room schema 改动必须同时更新 Database 版本、Migration、schema JSON 与迁移测试。
- 自动净化实现为 `purgeDeletedBefore(cutoff)`；仅在 `autoPurgeEnabled=true` 的启动路径调用。
- 永久删除 Moment 后，要清理或迁移其关联的详情/布局遗留数据，避免孤儿数据。

## 4. 实施流程

### 4.1 动手前

1. 说明你准备修改的页面、数据和文档。
2. 对照 PRD 的 P0/P1/P2，拒绝顺手加入未授权功能。
3. 找到旧实现与新设计的冲突点，决定“复用底层能力 / 迁移数据 / 删除旧 UI”。
4. 如果以下产品规则缺失，先问用户，不能自行发明：展示位置、排序方向、删除恢复语义、日期/时区语义、数据迁移范围。

### 4.2 实施中

- 先完成数据模型、Repository 与时间内核，再接页面；不要只做静态 UI。
- 每个页面通过明确事件回调与 ViewModel 通信，不把业务状态藏在 `remember` 中。
- 保持 Moment/Task 事实字段与派生展示字段分离。
- 旧卡片模板或旧自由排版未被 J 最终设计采用时，不接入新路径。
- 新增 P1/P2 能力前先更新 PRD/ARCHITECTURE，并等待用户确认。

### 4.3 实施后

1. 运行最小相关单元测试。
2. 运行 `./gradlew testDebugUnitTest assembleDebug lintDebug`。
3. 涉及 Room、Compose 交互、系统栏或无障碍时，运行相关 `connectedDebugAndroidTest` 或明确说明未运行原因。
4. 对时间功能使用固定 `Clock` 覆盖：未来、当天、次日自动转过去、月末、闰年与跨年。
5. 对新增/删除/恢复检查重启后的持久化状态。
6. 同步更新受影响文档：PRD（行为变更）、ARCHITECTURE（数据/边界变更）、DESIGN_SYSTEM（视觉 token/组件变更）。

## 5. 测试与质量红线

### 必测行为

- 未来 Moment：目标日前、目标日、目标日次日的倒数/今天/过去转换。
- 表单日期限制：未来模式拒绝过去日期；过去模式拒绝未来日期。
- 多个置顶 Moment 的手动排序和重启恢复。
- Task 的创建、勾选、取消勾选、跨日隐藏、主页右列显示与到期隐藏。
- 回收站撤销、复活、手动净化，以及开启/关闭自动净化后的启动清理。
- Room migration：旧版本数据库升级后不丢失真实数据。
- 大字体、中文内容描述、系统栏 inset、卡片点击和关键触摸目标。

### 不得宣称

- 不得只因 `assembleDebug` 成功就称“功能完成”。
- 不得把 JVM 测试说成真机或无障碍验证。
- 不得把原型节日、样例数据、静态截图当作真实持久化或真实日期计算验证。
- 不得把未实现的提醒、重复、系统通知、Widget、共享、同步说成已支持。

## 6. 最终汇报格式

完成一个任务时，简洁说明：

1. 改了什么（文件与用户可见行为）。
2. 为什么符合 PRD/J 最终设计。
3. 运行了什么验证，以及结果。
4. 哪些验证尚未进行（如真机、TalkBack、通知）。
5. 若有数据迁移、删除、未决产品规则或风险，必须单独说明。

不要用“构建通过”代替上述结论。
